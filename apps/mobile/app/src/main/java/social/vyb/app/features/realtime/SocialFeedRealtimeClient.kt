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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * App-scoped, content-free feed invalidation transport.
 *
 * The socket never carries post data. Each invalidation asks the authenticated
 * REST feed to reconcile, preserving visibility, block, and community rules.
 */
class SocialFeedRealtimeClient(
    private val socketUrlProvider: suspend () -> String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun observe(): Flow<SocialFeedRealtimeEvent> = callbackFlow {
        val stopped = AtomicBoolean(false)
        var reconnectAttempt = 0
        var connectJob: Job? = null
        var socket: WebSocket? = null

        fun scheduleConnect(delayMs: Long = 0L) {
            if (stopped.get() || connectJob?.isActive == true) return
            connectJob = launch {
                if (delayMs > 0) delay(delayMs)
                if (!isActive || stopped.get()) return@launch
                val socketUrl = try {
                    socketUrlProvider()
                } catch (_: Throwable) {
                    val retryDelay = retryDelayMillis(reconnectAttempt++)
                    connectJob = null
                    scheduleConnect(retryDelay)
                    return@launch
                }
                if (stopped.get()) return@launch

                socket = client.newWebSocket(
                    Request.Builder()
                        .url(socketUrl)
                        .header("Origin", "https://vybnet.app")
                        .build(),
                    object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            reconnectAttempt = 0
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            val type = runCatching {
                                json.parseToJsonElement(text).jsonObject["type"]?.jsonPrimitive?.content
                            }.getOrNull()
                            if (type == "social.feed.invalidated") {
                                trySend(SocialFeedRealtimeEvent.Invalidated)
                            }
                        }

                        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                            webSocket.close(code, reason)
                        }

                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            if (socket === webSocket) socket = null
                            reconnect()
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            if (socket === webSocket) socket = null
                            reconnect()
                        }

                        private fun reconnect() {
                            if (stopped.get()) return
                            val retryDelay = retryDelayMillis(reconnectAttempt++)
                            connectJob = null
                            scheduleConnect(retryDelay)
                        }
                    }
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
            socket?.close(1000, "Feed realtime stopped")
            socket = null
        }
    }

    private fun retryDelayMillis(attempt: Int): Long =
        (1_000L shl attempt.coerceIn(0, 5)).coerceAtMost(30_000L)
}

sealed interface SocialFeedRealtimeEvent {
    data object Invalidated : SocialFeedRealtimeEvent
}
