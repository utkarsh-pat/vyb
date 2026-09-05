package social.vyb.app.features.scribble

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

internal class ScribbleRealtimeClient(
    private val socketUrlProvider: suspend () -> String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    },
) {
    @Volatile
    private var activeSocket: WebSocket? = null

    fun observe(initialAction: ScribblePendingAction?): Flow<ScribbleRealtimeEvent> = callbackFlow {
        val stopped = AtomicBoolean(false)
        var reconnectAttempt = 0
        var connectJob: Job? = null
        var activeRoomId: String? = null
        var pendingAction = initialAction

        fun send(webSocket: WebSocket, type: String, payload: JsonObject = JsonObject(emptyMap())) =
            webSocket.send(
                json.encodeToString(
                    ScribbleSocketEnvelope(type = type, payload = payload)
                )
            )

        fun scheduleConnect(delayMs: Long = 0L) {
            if (stopped.get() || connectJob?.isActive == true) return
            connectJob = launch {
                if (delayMs > 0) delay(delayMs)
                if (!isActive || stopped.get()) return@launch
                trySend(ScribbleRealtimeEvent.Connecting(reconnecting = reconnectAttempt > 0))

                val url = try {
                    socketUrlProvider()
                } catch (error: Throwable) {
                    trySend(ScribbleRealtimeEvent.Error(error.message ?: "Could not authorize Scribble."))
                    trySend(ScribbleRealtimeEvent.Disconnected(retrying = true))
                    val retryDelay = retryDelayMillis(reconnectAttempt++)
                    connectJob = null
                    scheduleConnect(retryDelay)
                    return@launch
                }

                val listener = object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        activeSocket = webSocket
                        reconnectAttempt = 0
                        trySend(ScribbleRealtimeEvent.Connected)
                        send(webSocket, "scribble.catalog.subscribe")

                        val action = activeRoomId?.let { ScribblePendingAction.Join(it) } ?: pendingAction
                        when (action) {
                            is ScribblePendingAction.Create -> send(
                                webSocket,
                                "scribble.room.create",
                                buildJsonObject {
                                    put("settings", json.encodeToJsonElement(action.settings))
                                },
                            )
                            is ScribblePendingAction.Join -> send(
                                webSocket,
                                "scribble.room.join",
                                buildJsonObject { put("roomId", action.roomId) },
                            )
                            null -> Unit
                        }
                        pendingAction = null
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        val envelope = runCatching {
                            json.decodeFromString<ScribbleSocketEnvelope>(text)
                        }.getOrNull() ?: return
                        val payload = envelope.payload
                        val event = when (envelope.type) {
                            "scribble.connected" -> ScribbleRealtimeEvent.Connected
                            "scribble.state" -> runCatching {
                                json.decodeFromJsonElement<ScribbleSnapshot>(payload)
                            }.getOrNull()?.also { activeRoomId = it.roomId }?.let(
                                ScribbleRealtimeEvent::State
                            )
                            "scribble.catalog" -> runCatching {
                                json.decodeFromJsonElement<ScribbleCatalog>(payload)
                            }.getOrNull()?.rooms?.let(ScribbleRealtimeEvent::Catalog)
                            "scribble.draw.step" -> {
                                val roomId = payload["roomId"]?.jsonPrimitive?.contentOrNull
                                val steps = payload["steps"]?.let {
                                    runCatching {
                                        json.decodeFromJsonElement<List<ScribbleDrawStep>>(it)
                                    }.getOrDefault(emptyList())
                                }.orEmpty()
                                ScribbleRealtimeEvent.Draw(roomId, steps)
                            }
                            "scribble.canvas.clear" -> ScribbleRealtimeEvent.CanvasCleared
                            "scribble.notice" -> payload["message"]?.jsonPrimitive?.contentOrNull
                                ?.let(ScribbleRealtimeEvent::Notice)
                            "scribble.error" -> ScribbleRealtimeEvent.Error(
                                payload["message"]?.jsonPrimitive?.contentOrNull
                                    ?: "Scribble realtime error."
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
                        if (!stopped.get()) {
                            val retryDelay = retryDelayMillis(reconnectAttempt++)
                            trySend(ScribbleRealtimeEvent.Disconnected(retrying = true))
                            connectJob = null
                            scheduleConnect(retryDelay)
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        if (activeSocket === webSocket) activeSocket = null
                        if (!stopped.get()) {
                            // A Cloud Run/WebSocket transport can end while the room state is
                            // still perfectly valid. Rejoin from the last authoritative room
                            // snapshot without covering the game with a stale failure banner.
                            // Initial handshakes still surface an actionable error.
                            if (activeRoomId == null) {
                                trySend(ScribbleRealtimeEvent.Error(t.message ?: "Scribble connection failed."))
                            }
                            val retryDelay = retryDelayMillis(reconnectAttempt++)
                            trySend(ScribbleRealtimeEvent.Disconnected(retrying = true))
                            connectJob = null
                            scheduleConnect(retryDelay)
                        }
                    }
                }

                activeSocket = client.newWebSocket(Request.Builder().url(url).build(), listener)
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
            activeSocket?.close(1000, "Scribble closed")
            activeSocket = null
        }
    }

    fun send(type: String, payload: JsonObject = JsonObject(emptyMap())): Boolean =
        activeSocket?.send(
            json.encodeToString(ScribbleSocketEnvelope(type = type, payload = payload))
        ) ?: false

    fun startGame() = send("scribble.game.start")
    fun chooseWord(choiceId: String) = send(
        "scribble.word.choose",
        buildJsonObject { put("choiceId", choiceId) },
    )
    fun draw(steps: List<ScribbleDrawStep>) = send(
        "scribble.draw.step",
        buildJsonObject { put("steps", json.encodeToJsonElement(steps.take(120))) },
    )
    fun clearCanvas() = send("scribble.canvas.clear")
    fun guess(text: String) = send(
        "scribble.chat.guess",
        buildJsonObject { put("text", text.trim().take(120)) },
    )
    fun skipRound() = send("scribble.round.skip")
    fun leave() = send("scribble.room.leave")

    private fun retryDelayMillis(attempt: Int): Long =
        (750L * (attempt + 1)).coerceAtMost(8_000L)
}
