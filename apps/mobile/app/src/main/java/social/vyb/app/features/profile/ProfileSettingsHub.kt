package social.vyb.app.features.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybIndigo
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanel
import social.vyb.app.ui.VybPanelLifted
import social.vyb.app.ui.VybResponsiveFrame
import social.vyb.app.ui.VybTeal
import social.vyb.app.ui.VybText
import social.vyb.app.ui.theme.LocalThemePreference
import social.vyb.app.ui.theme.LocalThemePreferenceSetter
import social.vyb.app.ui.theme.ThemePreference
import social.vyb.app.features.social.SocialAvatar

@Composable
internal fun ProfileSettingsHub(
    state: ProfileUiState,
    email: String,
    onBack: () -> Unit,
    onAccount: () -> Unit,
    onPrivacy: () -> Unit,
    onSecurity: () -> Unit,
    onPasswordReset: () -> Unit,
    onSignOut: () -> Unit
) {
    val profile = state.privateProfile
    VybResponsiveFrame(Modifier.fillMaxSize(), maxContentWidth = 980.dp) { layout ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = layout.horizontalPadding)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to profile", tint = VybText)
                }
                Column(Modifier.weight(1f)) {
                    Text("Settings", color = VybText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("Manage your Vyb account", color = VybMuted)
                }
            }

            if (layout.wide) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    SettingsIdentityCard(
                        name = profile?.fullName.orEmpty().ifBlank { "Vyb member" },
                        username = profile?.username.orEmpty(),
                        email = email,
                        avatarUrl = profile?.avatarUrl,
                        modifier = Modifier.weight(.72f)
                    )
                    Column(Modifier.weight(1.28f)) {
                        SettingsCategoryList(
                            onAccount = onAccount,
                            onPrivacy = onPrivacy,
                            onSecurity = onSecurity,
                            onPasswordReset = onPasswordReset
                        )
                        AppearanceSettings()
                        SettingsFeedback(state)
                        SignOutAction(onSignOut)
                    }
                }
            } else {
                SettingsIdentityCard(
                    name = profile?.fullName.orEmpty().ifBlank { "Vyb member" },
                    username = profile?.username.orEmpty(),
                    email = email,
                    avatarUrl = profile?.avatarUrl
                )
                Text(
                    "MASTER SETTINGS",
                    color = VybMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                )
                SettingsCategoryList(
                    onAccount = onAccount,
                    onPrivacy = onPrivacy,
                    onSecurity = onSecurity,
                    onPasswordReset = onPasswordReset
                )
                AppearanceSettings()
                SettingsFeedback(state)
                SignOutAction(onSignOut)
            }
            Text(
                "VYB · SETTINGS",
                color = VybMuted,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 24.dp)
            )
        }
    }
}

@Composable
private fun SettingsIdentityCard(
    name: String,
    username: String,
    email: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = VybPanel.copy(alpha = .9f),
        border = BorderStroke(1.dp, VybBorder),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            SocialAvatar(
                avatarUrl = avatarUrl,
                displayName = name,
                size = 52.dp
            )
            Text(name, color = VybText, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 12.dp))
            if (username.isNotBlank()) Text("@$username", color = VybMuted)
            Text(
                email,
                color = VybMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp)
            )
            Text(
                "Your profile, privacy, appearance, and trusted-device controls live here.",
                color = VybMuted,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

@Composable
private fun SettingsCategoryList(
    onAccount: () -> Unit,
    onPrivacy: () -> Unit,
    onSecurity: () -> Unit,
    onPasswordReset: () -> Unit
) {
    SettingsCategory(
        Icons.Default.Person,
        "Account & profile",
        "Personalization, campus badge and social links",
        onAccount
    )
    SettingsCategory(
        Icons.AutoMirrored.Filled.Chat,
        "Privacy & chat controls",
        "Last seen, read receipts and typing indicators",
        onPrivacy
    )
    SettingsCategory(
        Icons.Default.Security,
        "Security & devices",
        "Recovery and trusted-device access",
        onSecurity
    )
    SettingsCategory(
        Icons.Default.LockReset,
        "Password management",
        "Send a secure recovery link to your campus inbox",
        onPasswordReset
    )
}

@Composable
private fun SettingsCategory(
    icon: ImageVector,
    title: String,
    body: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        onClick = onClick,
        color = VybPanel.copy(alpha = .88f),
        border = BorderStroke(1.dp, VybBorder),
        shape = RoundedCornerShape(17.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                color = VybIndigo.copy(alpha = .16f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(1f))
                    Icon(icon, null, tint = VybIndigo, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.weight(1f))
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = VybText, fontWeight = FontWeight.Bold)
                Text(body, color = VybMuted, fontSize = 13.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = VybMuted)
        }
    }
}

@Composable
private fun AppearanceSettings() {
    val theme = LocalThemePreference.current
    val setTheme = LocalThemePreferenceSetter.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        color = VybPanel.copy(alpha = .88f),
        border = BorderStroke(1.dp, VybBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DarkMode, null, tint = VybIndigo)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Appearance", color = VybText, fontWeight = FontWeight.Bold)
                    Text("Match Vyb across web and Android", color = VybMuted, fontSize = 13.sp)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                ThemePreference.entries.forEach { option ->
                    val selected = theme == option
                    Surface(
                        onClick = { setTheme(option) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        color = if (selected) VybIndigo.copy(alpha = .2f) else VybPanelLifted,
                        border = BorderStroke(
                            1.dp,
                            if (selected) VybIndigo.copy(alpha = .55f) else VybBorder
                        ),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.weight(1f))
                            Text(
                                option.name,
                                color = if (selected) VybText else VybMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsFeedback(state: ProfileUiState) {
    val message = state.error ?: state.notice ?: return
    val isError = state.error != null
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        color = if (isError) MaterialTheme.colorScheme.errorContainer
        else VybTeal.copy(alpha = .14f),
        border = BorderStroke(
            1.dp,
            if (isError) MaterialTheme.colorScheme.error.copy(alpha = .32f)
            else VybTeal.copy(alpha = .34f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            message,
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else VybText,
            modifier = Modifier.padding(13.dp)
        )
    }
}

@Composable
private fun SignOutAction(onSignOut: () -> Unit) {
    OutlinedButton(
        onClick = onSignOut,
        modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 8.dp),
        border = BorderStroke(1.dp, Color(0xFFE879A9)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, null)
        Text("Sign out this device", modifier = Modifier.padding(start = 8.dp))
    }
}
