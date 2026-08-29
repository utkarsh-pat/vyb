package social.vyb.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import social.vyb.app.ui.VybApp
import social.vyb.app.ui.theme.ThemePreference
import social.vyb.app.ui.theme.VybTheme
import social.vyb.app.features.realtime.VybNotificationChannels
import social.vyb.app.features.messages.ChatScreenshotEvents

class MainActivity : ComponentActivity() {
    private var pendingNotificationHref by mutableStateOf<String?>(null)
    private val screenCaptureCallback by lazy {
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            android.app.Activity.ScreenCaptureCallback { ChatScreenshotEvents.notifyCaptured() }
        } else null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureQaToken(intent)
        pendingNotificationHref = intent.notificationHref()
        VybNotificationChannels.ensure(this)
        enableEdgeToEdge()
        setContent {
            val preferences = remember {
                getSharedPreferences("vybnet_appearance", MODE_PRIVATE)
            }
            var themePreference by remember {
                mutableStateOf(
                    ThemePreference.fromStoredValue(
                        preferences.getString("theme", ThemePreference.System.storedValue)
                    )
                )
            }
            VybTheme(
                preference = themePreference,
                onPreferenceChanged = { selected ->
                    themePreference = selected
                    preferences.edit {
                        putString("theme", selected.storedValue)
                    }
                }
            ) {
                VybApp(
                    notificationHref = pendingNotificationHref,
                    onNotificationHrefConsumed = { pendingNotificationHref = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureQaToken(intent)
        pendingNotificationHref = intent.notificationHref()
    }

    override fun onStart() {
        super.onStart()
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            screenCaptureCallback?.let { registerScreenCaptureCallback(mainExecutor, it) }
        }
    }

    override fun onStop() {
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            screenCaptureCallback?.let { unregisterScreenCaptureCallback(it) }
        }
        super.onStop()
    }

    private fun captureQaToken(intent: Intent) {
        if (!BuildConfig.DEBUG) return
        DebugQaAuthToken.value = intent.getStringExtra("vyb.qa.id_token")
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: DebugQaAuthToken.value
    }
}

/** Debug-only escape hatch for deterministic emulator QA when the AVD has no internet. */
object DebugQaAuthToken {
    @Volatile
    var value: String? = null
}

private fun Intent.notificationHref(): String? =
    getStringExtra("href")?.trim()?.takeIf(String::isNotBlank)
        ?: data?.toString()?.trim()?.takeIf(String::isNotBlank)
