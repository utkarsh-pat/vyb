package social.vyb.app.data.network

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.net.URI
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import social.vyb.app.BuildConfig

/**
 * One network stack for every native feature.
 *
 * Keeping JSON rules, logging policy, and base URL normalization here prevents
 * feature repositories from silently drifting away from the web/API contract.
 */
object VybNetwork {
    private data class ClientConfig(
        val connectTimeoutSeconds: Long,
        val readTimeoutSeconds: Long,
        val writeTimeoutSeconds: Long
    )

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
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
    }
    private val retrofitCache = ConcurrentHashMap<ClientConfig, Retrofit>()

    inline fun <reified T> create(
        connectTimeoutSeconds: Long = 10,
        readTimeoutSeconds: Long = 20,
        writeTimeoutSeconds: Long = 20
    ): T = create(
        serviceClass = T::class.java,
        connectTimeoutSeconds = connectTimeoutSeconds,
        readTimeoutSeconds = readTimeoutSeconds,
        writeTimeoutSeconds = writeTimeoutSeconds
    )

    fun <T> create(
        serviceClass: Class<T>,
        connectTimeoutSeconds: Long = 10,
        readTimeoutSeconds: Long = 20,
        writeTimeoutSeconds: Long = 20
    ): T {
        val config = ClientConfig(
            connectTimeoutSeconds = connectTimeoutSeconds,
            readTimeoutSeconds = readTimeoutSeconds,
            writeTimeoutSeconds = writeTimeoutSeconds
        )
        val retrofit = retrofitCache.computeIfAbsent(config) {
            Retrofit.Builder()
                .baseUrl(normalizeBaseUrl(BuildConfig.API_BASE_URL))
                .client(
                    baseClient.newBuilder()
                        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                        .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                        .build()
                )
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
        }
        return retrofit.create(serviceClass)
    }

    fun normalizeBaseUrl(value: String): String {
        val normalized = value.trim()
        val uri = runCatching { URI(normalized) }
            .getOrElse { throw IllegalArgumentException("API base URL is invalid.", it) }
        require(
            uri.isAbsolute &&
                uri.scheme.lowercase() in setOf("http", "https") &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.rawQuery == null &&
                uri.rawFragment == null
        ) { "API base URL must be an absolute HTTP(S) origin without credentials or query data." }
        return if (normalized.endsWith("/")) normalized else "$normalized/"
    }
}

suspend fun FirebaseUser.requireIdToken(forceRefresh: Boolean = false): String =
    suspendCancellableCoroutine { continuation ->
        getIdToken(forceRefresh)
            .addOnSuccessListener { result ->
                result.token?.let(continuation::resume)
                    ?: continuation.resumeWithException(
                        IllegalStateException("Firebase returned an empty ID token.")
                    )
            }
            .addOnFailureListener(continuation::resumeWithException)
    }

suspend fun FirebaseAuth.requireBearerToken(): String {
    val user = currentUser ?: error("Your session expired. Please sign in again.")
    return "Bearer ${user.requireIdToken()}"
}
