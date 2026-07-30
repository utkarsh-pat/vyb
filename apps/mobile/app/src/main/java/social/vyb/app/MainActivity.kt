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

class MainActivity : ComponentActivity() {
    private var pendingNotificationHref by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        pendingNotificationHref = intent.notificationHref()
    }
}

private fun Intent.notificationHref(): String? =
    getStringExtra("href")?.trim()?.takeIf(String::isNotBlank)
        ?: data?.toString()?.trim()?.takeIf(String::isNotBlank)
