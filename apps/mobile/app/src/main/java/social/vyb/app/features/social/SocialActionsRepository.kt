package social.vyb.app.features.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import social.vyb.app.BuildConfig
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SocialActionsRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val api = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL.trim().let { if (it.endsWith("/")) it else "$it/" })
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
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SocialActionsApi::class.java)

    suspend fun createTextPost(
        text: String,
        isAnonymous: Boolean = false,
        allowAnonymousComments: Boolean = true,
        visibility: String = PostReach.Public.wireValue,
        communityId: String? = null
    ): SocialPost = apiCall {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Write something before publishing." }
        val bearer = bearerToken()
        val viewer = verifiedViewer(bearer)
        api.createPost(
            bearer,
            CreatePostBody(
                tenantId = viewer.tenantId,
                membershipId = viewer.id,
                body = trimmed,
                communityId = communityId,
                isAnonymous = isAnonymous,
                allowAnonymousComments = allowAnonymousComments,
                visibility = PostReach.fromWireValue(visibility).wireValue
            )
        ).item
    }

    suspend fun toggleReaction(postId: String, reactionType: String = "like"): ReactionResult =
        apiCall { api.toggleReaction(bearerToken(), postId, ReactionBody(reactionType)) }

    suspend fun toggleSave(postId: String): SaveResult =
        apiCall { api.toggleSave(bearerToken(), postId) }

    suspend fun listComments(postId: String): List<SocialComment> =
        apiCall { api.comments(bearerToken(), postId).items }

    suspend fun addComment(
        postId: String,
        text: String,
        parentCommentId: String? = null,
        isAnonymous: Boolean = false
    ): SocialComment = apiCall {
        val trimmed = text.trim()
        require(trimmed.length >= 2) { "Comment must be at least 2 characters." }
        val bearer = bearerToken()
        val viewer = verifiedViewer(bearer)
        api.addComment(
            bearer,
            postId,
            CreateCommentBody(
                membershipId = viewer.id,
                body = trimmed,
                parentCommentId = parentCommentId,
                isAnonymous = isAnonymous
            )
        ).item
    }

    private suspend fun verifiedViewer(bearer: String): ViewerMembership {
        val membership = api.viewer(bearer).membershipSummary
        check(membership.verificationStatus == "verified") {
            "Your campus membership is not verified yet."
        }
        return membership
    }

    private suspend fun bearerToken(): String {
        val user = auth.currentUser ?: error("Your session expired. Please sign in again.")
        return "Bearer ${user.idToken()}"
    }

    private suspend fun FirebaseUser.idToken(): String =
        suspendCancellableCoroutine { continuation ->
            getIdToken(false)
                .addOnSuccessListener { result ->
                    result.token?.let(continuation::resume)
                        ?: continuation.resumeWithException(
                            IllegalStateException("Firebase returned an empty ID token.")
                        )
                }
                .addOnFailureListener(continuation::resumeWithException)
        }

    private suspend fun <T> apiCall(block: suspend () -> T): T = try {
        block()
    } catch (error: HttpException) {
        val responseBody = error.response()?.errorBody()?.string()
        val backendMessage = responseBody?.let {
            runCatching { json.decodeFromString<ErrorEnvelope>(it).error?.message }.getOrNull()
        }
        throw SocialActionException(
            backendMessage ?: "Request failed (${error.code()}). Please try again.",
            error
        )
    }
}

class SocialActionException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

@Serializable
private data class ErrorEnvelope(val error: ErrorBody? = null)

@Serializable
private data class ErrorBody(val message: String? = null)
