package social.vyb.app.data

import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.suspendCancellableCoroutine
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

class VybApiRepository {
    private val api: ApiService = Retrofit.Builder()
        .baseUrl(normalizeBaseUrl(BuildConfig.API_BASE_URL))
        .client(
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
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
            }.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(ApiService::class.java)

    suspend fun loadHomeFeed(): HomeFeedResult {
        val user = FirebaseAuth.getInstance().currentUser
            ?: error("Your session expired. Please sign in again.")
        val token = bootstrapBackendSession(user)
        val bearer = "Bearer $token"
        val me = api.me(bearer)
        check(me.membershipSummary.verificationStatus == "verified") {
            "Your campus membership is not verified yet."
        }
        val feed = api.feed(bearer, me.membershipSummary.tenantId)
        return HomeFeedResult(me, feed)
    }

    private suspend fun bootstrapBackendSession(
        user: com.google.firebase.auth.FirebaseUser
    ): String {
        var token = user.idToken(forceRefresh = false)
        var response = api.bootstrapSession(
            SessionBootstrapRequest(
                idToken = token,
                displayName = user.displayName
            )
        )

        if (response.code() == 409) {
            token = user.idToken(forceRefresh = true)
            response = api.bootstrapSession(
                SessionBootstrapRequest(
                    idToken = token,
                    displayName = user.displayName
                )
            )
        }

        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        return token
    }

    private suspend fun com.google.firebase.auth.FirebaseUser.idToken(
        forceRefresh: Boolean
    ): String =
        suspendCancellableCoroutine { continuation ->
            getIdToken(forceRefresh)
                .addOnSuccessListener { result ->
                    val token = result.token
                    if (token != null) continuation.resume(token)
                    else continuation.resumeWithException(
                        IllegalStateException("Firebase returned an empty ID token.")
                    )
                }
                .addOnFailureListener(continuation::resumeWithException)
        }

    private fun normalizeBaseUrl(value: String): String =
        value.trim().let { if (it.endsWith("/")) it else "$it/" }
}

data class HomeFeedResult(
    val me: MeEnvelope,
    val feed: FeedEnvelope
)
