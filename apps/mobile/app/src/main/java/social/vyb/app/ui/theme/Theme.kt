package social.vyb.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import social.vyb.app.ui.LocalVybPalette
import social.vyb.app.ui.VybDarkPalette
import social.vyb.app.ui.VybLightPalette

enum class ThemePreference(val storedValue: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromStoredValue(value: String?): ThemePreference =
            entries.firstOrNull { it.storedValue == value } ?: System
    }
}

val LocalThemePreference = staticCompositionLocalOf { ThemePreference.System }
val LocalThemePreferenceSetter =
    staticCompositionLocalOf<(ThemePreference) -> Unit> { {} }

val Ink = Color(0xFF050B18)
val Midnight = Color(0xFF071426)
val Surface = Color(0xFF0B1728)
val SurfaceLifted = Color(0xFF142238)
val Lime = Color(0xFF6366F1)
val Cyan = Color(0xFF14B8A6)
val Coral = Color(0xFFFF49A2)
val Mist = Color(0xFFE6EEFC)
val Muted = Color(0xFF94A3B8)

private val DarkColors = darkColorScheme(
    primary = Lime,
    onPrimary = Color.White,
    secondary = Cyan,
    tertiary = Coral,
    background = Midnight,
    onBackground = Mist,
    surface = Surface,
    onSurface = Mist,
    surfaceVariant = SurfaceLifted,
    onSurfaceVariant = Muted,
    outline = Color(0x1FFFFFFF),
    outlineVariant = Color(0x1FFFFFFF)
)

private val LightColors = lightColorScheme(
    primary = VybLightPalette.indigo,
    onPrimary = Color.White,
    secondary = VybLightPalette.teal,
    onSecondary = Color.White,
    tertiary = VybLightPalette.pink,
    background = VybLightPalette.background,
    onBackground = VybLightPalette.text,
    surface = VybLightPalette.panel,
    onSurface = VybLightPalette.text,
    surfaceVariant = VybLightPalette.panelLifted,
    onSurfaceVariant = VybLightPalette.muted,
    outline = VybLightPalette.border,
    outlineVariant = VybLightPalette.border
)

@Composable
fun VybTheme(
    preference: ThemePreference = ThemePreference.System,
    onPreferenceChanged: (ThemePreference) -> Unit = {},
    content: @Composable () -> Unit
) {
    val darkTheme = when (preference) {
        ThemePreference.System -> isSystemInDarkTheme()
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }
    CompositionLocalProvider(
        LocalThemePreference provides preference,
        LocalThemePreferenceSetter provides onPreferenceChanged,
        LocalVybPalette provides if (darkTheme) VybDarkPalette else VybLightPalette
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = VybTypography,
            content = content
        )
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
