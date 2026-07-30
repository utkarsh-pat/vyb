package social.vyb.app.features.realtime

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import social.vyb.app.MainActivity
import social.vyb.app.R

// Android Lint 8.13 still looks for the deprecated token callback. Firebase
// Messaging 25.1+ replaces it with the FID-based onRegistered callback below.
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class VybFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        serviceScope.launch {
            runCatching {
                NotificationDeviceRepository(applicationContext)
                    .registerInstallationId(installationId)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        showForegroundNotification(message)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showForegroundNotification(message: RemoteMessage) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val content = buildForegroundNotificationContent(
            data = message.data,
            notificationTitle = message.notification?.title,
            notificationBody = message.notification?.body
        ) ?: return
        VybNotificationChannels.ensure(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("href", content.href)
        }
        val requestCode = (message.messageId ?: content.href).hashCode()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(
            this,
            VybNotificationChannels.ACTIVITY_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification_vyb)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(requestCode, notification)
    }

}

internal object VybNotificationChannels {
    const val ACTIVITY_CHANNEL_ID = "vyb_activity"

    fun ensure(context: Context) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(ACTIVITY_CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                ACTIVITY_CHANNEL_ID,
                "Vyb activity",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Messages, campus activity, and account notifications"
            }
        )
    }
}

internal data class ForegroundNotificationContent(
    val title: String,
    val body: String,
    val href: String
)

internal fun buildForegroundNotificationContent(
    data: Map<String, String>,
    notificationTitle: String?,
    notificationBody: String?
): ForegroundNotificationContent? {
    val title = (
        notificationTitle?.trim()?.takeIf(String::isNotEmpty)
            ?: data["title"]?.trim()?.takeIf(String::isNotEmpty)
            ?: "Vyb"
        ).take(80)
    val body = (
        notificationBody?.trim()?.takeIf(String::isNotEmpty)
            ?: data["body"]?.trim()?.takeIf(String::isNotEmpty)
        )?.take(500)
        ?: return null
    val href = data["href"]?.trim()?.takeIf(String::isNotEmpty) ?: "/notifications"
    return ForegroundNotificationContent(title = title, body = body, href = href)
}
