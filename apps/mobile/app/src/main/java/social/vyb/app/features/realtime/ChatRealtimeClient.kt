package social.vyb.app.features.realtime

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Conversation-scoped realtime transport.
 *
 * Durable chat state still comes from the REST API. Socket events are only hints
 * that tell the UI when to reconcile, which keeps reconnects idempotent.
 */
class ChatRealtimeClient(
    private val socketUrlProvider: suspend (conversationId: String) -> String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    @Volatile
    private var activeSocket: WebSocket? = null

    fun observe(conversationId: String): Flow<ChatRealtimeEvent> = callbackFlow {
        val stopped = AtomicBoolean(false)
        var reconnectAttempt = 0
        var connectJob: Job? = null
        var heartbeatJob: Job? = null

        fun scheduleConnect(delayMs: Long = 0L) {
            if (stopped.get() || connectJob?.isActive == true) return
            connectJob = launch {
                if (delayMs > 0) delay(delayMs)
                if (!isActive || stopped.get()) return@launch

                trySend(ChatRealtimeEvent.Connecting)
                val socketUrl = try {
                    socketUrlProvider(conversationId)
                } catch (_: Throwable) {
                    val retryDelay = retryDelayMillis(reconnectAttempt++)
                    trySend(ChatRealtimeEvent.Disconnected(retrying = true))
                    connectJob = null
                    scheduleConnect(retryDelay)
                    return@launch
                }
                if (stopped.get()) return@launch

                val listener = object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        activeSocket = webSocket
                        reconnectAttempt = 0
                        trySend(ChatRealtimeEvent.Connected)
                        heartbeatJob?.cancel()
                        heartbeatJob = launch {
                            while (isActive && !stopped.get()) {
                                delay(25_000)
                                webSocket.send(
                                    buildJsonObject {
                                        put("type", "chat.ping")
                                        put(
                                            "payload",
                                            buildJsonObject { put("conversationId", conversationId) }
                                        )
                                    }.toString()
                                )
                            }
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        val envelope = runCatching {
                            json.decodeFromString<ChatSocketEnvelope>(text)
                        }.getOrNull() ?: return
                        if (envelope.conversationId != null &&
                            envelope.conversationId != conversationId
                        ) return

                        val payload = envelope.payload
                        val event = when (envelope.type) {
                            "chat.connected", "chat.pong" -> ChatRealtimeEvent.Connected
                            "chat.message", "chat.message.updated", "chat.delivered" ->
                                ChatRealtimeEvent.MessageChanged(
                                    payload?.get("messageId")?.jsonPrimitive?.contentOrNull
                                )
                            "chat.read" -> ChatRealtimeEvent.ReadChanged(
                                payload?.get("messageId")?.jsonPrimitive?.contentOrNull
                            )
                            "chat.sync" -> ChatRealtimeEvent.SyncRequired
                            "chat.typing" -> ChatRealtimeEvent.PeerTyping(
                                userId = payload?.get("userId")?.jsonPrimitive?.contentOrNull,
                                membershipId = payload?.get("membershipId")
                                    ?.jsonPrimitive?.contentOrNull,
                                isTyping = payload?.get("isTyping")?.jsonPrimitive
                                    ?.contentOrNull?.toBooleanStrictOrNull() ?: false
                            )
                            else -> null
                        }
                        if (event != null) trySend(event)
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        webSocket.close(code, reason)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        if (activeSocket === webSocket) activeSocket = null
                        heartbeatJob?.cancel()
                        if (!stopped.get()) {
                            val retryDelay = retryDelayMillis(reconnectAttempt++)
                            trySend(ChatRealtimeEvent.Disconnected(retrying = true))
                            connectJob = null
                            scheduleConnect(retryDelay)
                        }
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?
                    ) {
                        if (activeSocket === webSocket) activeSocket = null
                        heartbeatJob?.cancel()
                        if (!stopped.get()) {
                            val retryDelay = retryDelayMillis(reconnectAttempt++)
                            trySend(ChatRealtimeEvent.Disconnected(retrying = true))
                            connectJob = null
                            scheduleConnect(retryDelay)
                        }
                    }
                }

                activeSocket = client.newWebSocket(
                    Request.Builder().url(socketUrl).build(),
                    listener
                )
            }.also { job ->
                job.invokeOnCompletion {
                    if (connectJob === job) connectJob = null
                }
            }
        }

        scheduleConnect()

        awaitClose {
            stopped.set(true)
            connectJob?.cancel()
            heartbeatJob?.cancel()
            activeSocket?.close(1000, "Conversation closed")
            activeSocket = null
        }
    }

    fun sendTyping(conversationId: String, isTyping: Boolean): Boolean =
        activeSocket?.send(
            buildJsonObject {
                put("type", "chat.typing")
                put(
                    "payload",
                    buildJsonObject {
                        put("conversationId", conversationId)
                        put("isTyping", isTyping)
                    }
                )
            }.toString()
        ) ?: false

    fun acknowledgeDelivered(conversationId: String, messageIds: List<String>): Boolean {
        val cleanIds = messageIds.distinct().filter(String::isNotBlank).take(50)
        if (cleanIds.isEmpty()) return false
        return activeSocket?.send(
            buildJsonObject {
                put("type", "chat.delivered")
                put(
                    "payload",
                    buildJsonObject {
                        put("conversationId", conversationId)
                        put("messageIds", json.encodeToJsonElement(cleanIds))
                    }
                )
            }.toString()
        ) ?: false
    }

    private fun retryDelayMillis(attempt: Int): Long =
        (1_000L shl attempt.coerceIn(0, 5)).coerceAtMost(30_000L)
}
