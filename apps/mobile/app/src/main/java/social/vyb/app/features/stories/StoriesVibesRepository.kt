package social.vyb.app.features.stories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import social.vyb.app.BuildConfig

class StoriesVibesRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val api: StoriesVibesApi = Retrofit.Builder()
        .baseUrl(normalizeBaseUrl(BuildConfig.API_BASE_URL))
        .client(
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = if (BuildConfig.DEBUG) {
                            HttpLoggingInterceptor.Level.BASIC
                        } else {
                            HttpLoggingInterceptor.Level.NONE
                        }
                    }
                )
                .build()
        )
        .addConverterFactory(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                coerceInputValues = true
            }.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(StoriesVibesApi::class.java)

    private var cachedTenantId: String? = null

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
        val bearer = "Bearer ${user.idToken()}"
        val tenantId = cachedTenantId ?: api.me(bearer).membershipSummary.let { membership ->
            check(membership.verificationStatus == "verified") {
                "Your campus membership is not verified yet."
            }
            membership.tenantId.also { cachedTenantId = it }
        }
        return try {
            operation(bearer, tenantId)
        } catch (error: HttpException) {
            if (error.code() == 401) cachedTenantId = null
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

    private suspend fun FirebaseUser.idToken(): String =
        suspendCancellableCoroutine { continuation ->
            getIdToken(false)
                .addOnSuccessListener { result ->
                    result.token?.let(continuation::resume)
                        ?: continuation.resumeWithException(
                            StoriesVibesException("Firebase returned an empty ID token.")
                        )
                }
                .addOnFailureListener(continuation::resumeWithException)
        }

    private fun normalizeBaseUrl(value: String): String =
        value.trim().let { if (it.endsWith("/")) it else "$it/" }
}

class StoriesVibesException(
    override val message: String,
    cause: Throwable? = null
) : Exception(message, cause)
