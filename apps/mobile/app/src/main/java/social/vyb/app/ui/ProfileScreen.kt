package social.vyb.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import social.vyb.app.data.VybUiState
import social.vyb.app.ui.theme.LocalThemePreference
import social.vyb.app.ui.theme.LocalThemePreferenceSetter
import social.vyb.app.ui.theme.ThemePreference

@Composable
fun ProfileScreen(state: VybUiState, onSignOut: () -> Unit) {
    val handle = state.email.substringBefore("@").ifBlank { "vybstudent" }
    val displayName = state.displayName.ifBlank { "Vyb Student" }
    val themePreference = LocalThemePreference.current
    val setThemePreference = LocalThemePreferenceSetter.current
    val darkThemeEnabled = when (themePreference) {
        ThemePreference.Dark -> true
        ThemePreference.Light -> false
        ThemePreference.System -> VybBackground.luminance() < .5f
    }
    val toggleTheme = {
        setThemePreference(
            if (darkThemeEnabled) ThemePreference.Light else ThemePreference.Dark
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 360.dp || maxHeight < 700.dp
        val sidePadding = if (maxWidth >= 600.dp) 28.dp else 16.dp
        val coverHeight = if (compact) 126.dp else 158.dp
        val avatarSize = if (compact) 88.dp else 104.dp

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(coverHeight)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF2634A8),
                                VybPurple,
                                Color(0xFF0C7C82)
                            )
                        )
                    )
            ) {
                Box(
                    Modifier
                        .size(if (compact) 116.dp else 154.dp)
                        .offset(x = (-28).dp, y = (-42).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = .08f))
                )
                Box(
                    Modifier
                        .size(if (compact) 96.dp else 126.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 26.dp, y = 34.dp)
                        .clip(CircleShape)
                        .background(VybTeal.copy(alpha = .18f))
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = sidePadding, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VybBrandLockup(Modifier.weight(1f), compact = true)
                    IconButton(
                        onClick = toggleTheme,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = .24f))
                    ) {
                        Icon(
                            if (darkThemeEnabled) Icons.Default.LightMode else Icons.Default.DarkMode,
                            "Toggle theme",
                            tint = Color.White
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = avatarSize / 2)
                        .size(avatarSize),
                    shape = CircleShape,
                    color = VybPanel,
                    border = BorderStroke(4.dp, VybBackground),
                    shadowElevation = 10.dp
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(VybAccentBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            displayName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = if (compact) 30.sp else 38.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(Modifier.height(avatarSize / 2 + 14.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = sidePadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    displayName,
                    color = VybText,
                    fontSize = if (compact) 23.sp else 27.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("@$handle", color = VybMuted, fontSize = 14.sp)

                Surface(
                    modifier = Modifier.padding(top = 10.dp),
                    shape = RoundedCornerShape(50),
                    color = VybTeal.copy(alpha = .13f),
                    border = BorderStroke(1.dp, VybTeal.copy(alpha = .35f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.School, null, Modifier.size(16.dp), tint = VybTeal)
                        Text(
                            " Verified college member",
                            color = VybText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = VybPanel.copy(alpha = .92f),
                    border = BorderStroke(1.dp, VybBorder)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (compact) 15.dp else 19.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Stat(state.feed.size.toString(), "Posts")
                        Stat("0", "Followers")
                        Stat("0", "Following")
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = VybPanelLifted.copy(alpha = .72f),
                    border = BorderStroke(1.dp, VybIndigo.copy(alpha = .3f))
                ) {
                    Column(Modifier.padding(if (compact) 16.dp else 20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Make your profile yours",
                                    color = VybText,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Add your course, branch, bio and photo.",
                                    color = VybMuted,
                                    fontSize = 13.sp
                                )
                            }
                            Text("40%", color = VybTeal, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { .4f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .height(7.dp)
                                .clip(CircleShape),
                            color = VybTeal,
                            trackColor = VybBackground
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, VybBorder)
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Email, null, tint = VybMuted)
                        Text(
                            state.email.ifBlank { "College email unavailable" },
                            Modifier.padding(start = 12.dp),
                            color = VybText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = toggleTheme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = VybPanel,
                    border = BorderStroke(1.dp, VybBorder)
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (darkThemeEnabled) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = VybIndigo
                        )
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(
                                "Dark theme",
                                color = VybText,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Switch between light and dark appearance",
                                color = VybMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = darkThemeEnabled,
                            onCheckedChange = {
                                setThemePreference(
                                    if (it) ThemePreference.Dark else ThemePreference.Light
                                )
                            }
                        )
                    }
                }

                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(if (compact) 48.dp else 52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VybIndigo)
                ) {
                    Icon(Icons.Default.Edit, null)
                    Text("Complete profile", Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(if (compact) 46.dp else 50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, VybPink.copy(alpha = .42f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB1D8))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null)
                    Text("Sign out", Modifier.padding(start = 8.dp))
                }

                Text(
                    "Your college email is only visible to you.",
                    Modifier.padding(top = 12.dp, bottom = 22.dp),
                    color = VybMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = VybText, fontWeight = FontWeight.Black, fontSize = 21.sp)
        Text(label, color = VybMuted, fontSize = 12.sp)
    }
}
