package social.vyb.app.features.stories

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import social.vyb.app.data.network.VybNetwork
import social.vyb.app.data.network.requireIdToken

class StoriesVibesRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val api: StoriesVibesApi = VybNetwork.create()

    private val tenantIds = TenantIdResolver()

    suspend fun loadStories(): List<StoryItem> = authenticated { bearer, tenantId ->
        api.stories(bearer, tenantId).items
    }

    suspend fun loadVibes(cursor: String? = null, limit: Int = 12): VibesEnvelope =
        authenticated { bearer, tenantId ->
            api.vibes(bearer, tenantId, limit, cursor)
        }

    internal suspend fun markStorySeen(storyId: String): StorySeenEnvelope =
        authenticated { bearer, _ -> api.markStorySeen(bearer, storyId) }

    internal suspend fun toggleStoryLike(storyId: String): StoryReactionEnvelope =
        authenticated { bearer, _ -> api.toggleStoryLike(bearer, storyId) }

    internal suspend fun toggleVibeLike(vibeId: String): VibeReactionEnvelope =
        authenticated { bearer, _ -> api.toggleVibeLike(bearer, vibeId) }

    private suspend fun <T> authenticated(
        operation: suspend (bearer: String, tenantId: String) -> T
    ): T {
        val user = auth.currentUser
            ?: throw StoriesVibesException("Your session expired. Please sign in again.")
        val bearer = "Bearer ${user.requireIdToken()}"
        val tenantId = tenantIds.resolve(user.uid) {
            api.me(bearer).membershipSummary.let { membership ->
                check(membership.verificationStatus == "verified") {
                    "Your campus membership is not verified yet."
                }
                membership.tenantId
            }
        }
        return try {
            operation(bearer, tenantId)
        } catch (error: HttpException) {
            if (error.code() == 401) tenantIds.invalidate(user.uid)
            throw StoriesVibesException(
                when (error.code()) {
                    401 -> "Your session expired. Please sign in again."
                    403 -> "Your account cannot access this campus content."
                    404 -> "This story or vibe is no longer available."
                    else -> "The Stories & Vibes service is unavailable (${error.code()})."
                },
                error
            )
        }
    }

}

internal class TenantIdResolver {
    private data class CachedTenant(val uid: String, val tenantId: String)

    private val mutex = Mutex()

    @Volatile
    private var cached: CachedTenant? = null

    suspend fun resolve(uid: String, lookup: suspend () -> String): String {
        cached?.takeIf { it.uid == uid }?.let { return it.tenantId }
        return mutex.withLock {
            cached?.takeIf { it.uid == uid }?.tenantId
                ?: lookup().also { tenantId ->
                    cached = CachedTenant(uid = uid, tenantId = tenantId)
                }
        }
    }

    suspend fun invalidate(uid: String) {
        mutex.withLock {
            if (cached?.uid == uid) cached = null
        }
    }
}

class StoriesVibesException(
    override val message: String,
    cause: Throwable? = null
) : Exception(message, cause)
