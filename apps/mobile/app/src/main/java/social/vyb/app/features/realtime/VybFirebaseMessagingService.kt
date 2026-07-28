package social.vyb.app.features.realtime

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class VybFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            runCatching {
                NotificationDeviceRepository(applicationContext).registerToken(token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Notification rendering/deep links belong in the app navigation layer.
        // Firebase displays notification payloads automatically in background;
        // foreground data payloads remain available here for that integration.
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
