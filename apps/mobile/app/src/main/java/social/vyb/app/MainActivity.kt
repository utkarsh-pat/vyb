package social.vyb.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import social.vyb.app.ui.VybApp
import social.vyb.app.ui.theme.ThemePreference
import social.vyb.app.ui.theme.VybTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    preferences.edit().putString("theme", selected.storedValue).apply()
                }
            ) {
                VybApp()
            }
        }
    }
}
