package social.vyb.app.features.realtime

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import social.vyb.app.data.network.VybNetwork
import social.vyb.app.data.network.requireIdToken

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
 * Call [registerCurrentInstallation] after sign-in as well as from
 * [VybFirebaseMessagingService.onRegistered], because FCM may issue a Firebase
 * Installation ID (FID) before Firebase Authentication has a current user.
 */
class NotificationDeviceRepository(
    context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val appContext = context.applicationContext
    private val api: NotificationDeviceApi = VybNetwork.create()

    suspend fun registerCurrentInstallation(): Boolean {
        val storedInstallationId = notificationInstallationId()
        val registeredStoredId = storedInstallationId?.let { registerInstallationId(it) } ?: false
        FirebaseMessaging.getInstance().awaitRegistration()
        return registeredStoredId
    }

    suspend fun registerInstallationId(installationId: String): Boolean {
        val normalizedInstallationId = installationId.trim()
        if (normalizedInstallationId.isEmpty()) return false
        storeNotificationInstallationId(normalizedInstallationId)
        val user = auth.currentUser ?: return false
        val idToken = user.requireIdToken()
        val response = api.register(
            authorization = "Bearer $idToken",
            body = RegisterNotificationDeviceRequest(
                deviceId = localDeviceId(),
                endpoint = normalizedInstallationId,
                pushSubscription = mapOf(
                    "provider" to "fcm-fid",
                    "fid" to normalizedInstallationId
                )
            )
        )
        return response.registered
    }

    private fun preferences() =
        appContext.getSharedPreferences(INSTALLATION_PREFERENCES, Context.MODE_PRIVATE)

    private fun localDeviceId(): String {
        val preferences = preferences()
        preferences.getString(INSTALLATION_ID_KEY, null)?.let { return it }

        return "android-${UUID.randomUUID()}".also { generatedId ->
            preferences.edit {
                putString(INSTALLATION_ID_KEY, generatedId)
            }
        }
    }

    private fun notificationInstallationId(): String? =
        preferences().getString(NOTIFICATION_INSTALLATION_ID_KEY, null)
            ?.trim()
            ?.takeIf(String::isNotEmpty)

    private fun storeNotificationInstallationId(installationId: String) {
        preferences().edit {
            putString(NOTIFICATION_INSTALLATION_ID_KEY, installationId)
        }
    }

    private companion object {
        const val INSTALLATION_PREFERENCES = "vyb_installation"
        const val INSTALLATION_ID_KEY = "notification_device_id"
        const val NOTIFICATION_INSTALLATION_ID_KEY = "fcm_installation_id"
    }
}

private suspend fun FirebaseMessaging.awaitRegistration(): Unit =
    suspendCancellableCoroutine { continuation ->
        register()
            .addOnSuccessListener { continuation.resume(Unit) }
            .addOnFailureListener { continuation.resumeWithException(it) }
    }
