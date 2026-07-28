package social.vyb.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
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

val Ink = Color(0xFF0B1220)
val Midnight = Color(0xFF0B1220)
val Surface = Color(0xFF111A2E)
val SurfaceLifted = Color(0xFF1C2740)
val Lime = Color(0xFF6366F1)
val Cyan = Color(0xFF14B8A6)
val Coral = Color(0xFFFF49A2)
val Mist = Color(0xFFF5F7FB)
val Muted = Color(0xFF9CA9B9)

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
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1FFFFFFF)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4B7300),
    secondary = Color(0xFF00677A),
    tertiary = Color(0xFFA83A18),
    background = Color(0xFFF7F9FC),
    surface = Color.White,
    onSurface = Ink
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
