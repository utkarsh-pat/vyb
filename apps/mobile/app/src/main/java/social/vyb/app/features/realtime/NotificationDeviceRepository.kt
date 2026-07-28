package social.vyb.app.features.realtime

import android.content.Context
import android.provider.Settings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import social.vyb.app.BuildConfig

internal interface NotificationDeviceApi {
    @POST("v1/notifications/register-device")
    suspend fun register(
        @Header("Authorization") authorization: String,
        @Body body: RegisterNotificationDeviceRequest
    ): RegisterNotificationDeviceResponse
}

@Serializable
internal data class RegisterNotificationDeviceRequest(
    val deviceId: String,
    val platform: String = "android",
    val endpoint: String,
    val pushSubscription: Map<String, String>
)

@Serializable
internal data class RegisterNotificationDeviceResponse(
    val deviceId: String,
    val registered: Boolean,
    val updatedAt: String
)

/**
 * Registers an FCM installation against the signed-in Vyb account.
 *
 * Call [registerCurrentToken] after sign-in as well as from
 * [VybFirebaseMessagingService.onNewToken], because FCM may issue a token before
 * Firebase Authentication has a current user.
 */
class NotificationDeviceRepository(
    context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val appContext = context.applicationContext
    private val api = Retrofit.Builder()
        .baseUrl(normalizeBaseUrl(BuildConfig.API_BASE_URL))
        .addConverterFactory(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(NotificationDeviceApi::class.java)

    suspend fun registerCurrentToken(): Boolean {
        val token = FirebaseMessaging.getInstance().awaitToken()
        return registerToken(token)
    }

    suspend fun registerToken(token: String): Boolean {
        val user = auth.currentUser ?: return false
        val idToken = user.getIdToken(false).awaitResult().token ?: return false
        val deviceId = "android-${Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )}"
        val response = api.register(
            authorization = "Bearer $idToken",
            body = RegisterNotificationDeviceRequest(
                deviceId = deviceId,
                endpoint = token,
                pushSubscription = mapOf(
                    "provider" to "fcm",
                    "token" to token
                )
            )
        )
        return response.registered
    }

    private fun normalizeBaseUrl(value: String): String =
        value.trim().let { if (it.endsWith("/")) it else "$it/" }
}

private suspend fun FirebaseMessaging.awaitToken(): String =
    suspendCancellableCoroutine { continuation ->
        token
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resumeWithException(it) }
    }

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
    }
