package social.vyb.app.features.social

import com.google.firebase.auth.FirebaseAuth
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import retrofit2.HttpException
import social.vyb.app.data.network.VybNetwork
import social.vyb.app.data.network.requireBearerToken

/** Bounded, disk-backed delivery queue. The server remains authoritative for
 * thresholds, tenant scope, idempotency, and retention. */
class ContentMeasurementRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val api: SocialActionsApi = VybNetwork.create()
    private val mutex = Mutex()
    private val preferences by lazy {
        auth.app.applicationContext.getSharedPreferences("vyb_content_measurement", 0)
    }
    private val sessionKey = UUID.randomUUID().toString().replace("-", "")
    private val serializer = ListSerializer(ContentEventPayload.serializer())

    suspend fun record(
        postId: String,
        eventType: String,
        visibleMs: Int = 0,
        watchMs: Int = 0,
        progressBasisPoints: Int = 0,
        flush: Boolean = false
    ) = mutex.withLock {
        val queue = readQueue().toMutableList()
        queue += ContentEventPayload(
            eventKey = UUID.randomUUID().toString(),
            postId = postId,
            sessionKey = sessionKey,
            eventType = eventType,
            visibleMs = visibleMs.coerceIn(0, 21_600_000),
            watchMs = watchMs.coerceIn(0, 21_600_000),
            progressBasisPoints = progressBasisPoints.coerceIn(0, 10_000),
            occurredAt = Instant.now().toString()
        )
        writeQueue(queue.takeLast(200))
        if (flush || queue.size >= 10) flushLocked()
    }

    suspend fun flush() = mutex.withLock { flushLocked() }

    private suspend fun flushLocked() {
        val queue = readQueue()
        if (queue.isEmpty() || auth.currentUser == null) return
        val batch = queue.take(20)
        runCatching {
            api.contentEvents(auth.requireBearerToken(), ContentEventBatch(batch))
        }.onSuccess {
            val delivered = batch.mapTo(hashSetOf()) { it.eventKey }
            writeQueue(queue.filterNot { it.eventKey in delivered })
        }.onFailure { error ->
            // A permanently invalid/stale batch must not poison the bounded queue.
            if (error is HttpException && error.code() in setOf(400, 404, 413)) {
                val rejected = batch.mapTo(hashSetOf()) { it.eventKey }
                writeQueue(queue.filterNot { it.eventKey in rejected })
            }
        }
    }

    private fun readQueue(): List<ContentEventPayload> = preferences.getString("queue", null)?.let { raw ->
        runCatching { VybNetwork.json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    } ?: emptyList()

    private fun writeQueue(queue: List<ContentEventPayload>) {
        preferences.edit().putString("queue", VybNetwork.json.encodeToString(serializer, queue)).apply()
    }
}
