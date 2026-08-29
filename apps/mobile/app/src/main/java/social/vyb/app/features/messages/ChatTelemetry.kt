package social.vyb.app.features.messages

import android.os.SystemClock
import android.util.Log
import social.vyb.app.BuildConfig
import java.util.concurrent.ConcurrentHashMap

/** Content-free debug timings for two-device chat QA. */
object ChatTelemetry {
    private const val TAG = "VybChatLatency"
    private val acceptedAt = ConcurrentHashMap<String, Long>()

    fun apiAccepted(messageId: String, startedAt: Long) {
        acceptedAt[messageId] = startedAt
        record("send_tap_to_api_accept", SystemClock.elapsedRealtime() - startedAt, messageId)
    }

    fun receipt(kind: String, messageId: String) {
        val startedAt = acceptedAt[messageId] ?: return
        record("send_tap_to_${kind.removePrefix("chat.")}", SystemClock.elapsedRealtime() - startedAt, messageId)
        if (kind == "chat.read") acceptedAt.remove(messageId)
    }

    fun realtimeArrival(createdAtEpochMs: Long, messageId: String) {
        record("server_create_to_peer_socket", (System.currentTimeMillis() - createdAtEpochMs).coerceAtLeast(0), messageId)
    }

    fun transport(state: String, elapsedMs: Long? = null) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, "metric=socket_$state${elapsedMs?.let { " durationMs=$it" }.orEmpty()}")
    }

    fun presenceHeartbeat(startedAt: Long, serverTimestamp: String) {
        if (!BuildConfig.DEBUG) return
        val durationMs = SystemClock.elapsedRealtime() - startedAt
        Log.d(TAG, "metric=presence_heartbeat_rtt durationMs=$durationMs serverAt=$serverTimestamp")
    }

    fun presenceHeartbeatFailed(startedAt: Long) {
        if (!BuildConfig.DEBUG) return
        val durationMs = SystemClock.elapsedRealtime() - startedAt
        Log.d(TAG, "metric=presence_heartbeat_failed durationMs=$durationMs")
    }

    private fun record(metric: String, durationMs: Long, messageId: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "metric=$metric durationMs=$durationMs messageId=$messageId")
    }
}
