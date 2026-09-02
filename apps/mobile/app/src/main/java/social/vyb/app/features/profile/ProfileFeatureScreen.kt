package social.vyb.app.features.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import social.vyb.app.features.social.SocialPost
import social.vyb.app.features.social.SocialAvatar
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybEmptyState
import social.vyb.app.ui.VybIndigo
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanel
import social.vyb.app.ui.VybPanelLifted
import social.vyb.app.ui.VybResponsiveFrame
import social.vyb.app.ui.VybTeal
import social.vyb.app.ui.VybText
import social.vyb.app.ui.VybRemoteImage
import social.vyb.app.ui.VybRemoteVideo
import social.vyb.app.ui.theme.LocalThemePreference
import social.vyb.app.ui.theme.LocalThemePreferenceSetter
import social.vyb.app.ui.theme.ThemePreference

@Composable
fun ProfileFeatureScreen(
    email: String,
    onSignOut: () -> Unit,
    onCreatePost: () -> Unit = {},
    onOpenPost: (String) -> Unit = {},
    onOpenVibe: (String) -> Unit = {},
    refreshSignal: Int = 0,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val state by profileViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(refreshSignal) {
        if (refreshSignal > 0) profileViewModel.refresh()
    }
    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VybIndigo)
        }
        state.privateProfile == null || state.publicProfile == null -> VybEmptyState(
            icon = Icons.Default.Person,
            title = "Profile unavailable",
            body = state.error ?: "Your campus profile could not be loaded.",
            actionLabel = "Try again",
            onAction = profileViewModel::refresh,
            modifier = Modifier.padding(24.dp)
        )
        else -> when (state.panel) {
            ProfilePanel.Overview -> PwaProfileSurface(
                state = state,
                onTab = profileViewModel::setTab,
                onEdit = { profileViewModel.open(ProfilePanel.Edit) },
                onSettings = { profileViewModel.open(ProfilePanel.Settings) },
                onCreatePost = onCreatePost,
                onConnections = profileViewModel::openConnections,
                onOpenLink = LocalUriHandler.current::openUri,
                onOpenContent = { post ->
                    if (post.kind == "video" || post.placement == "vibe") {
                        onOpenVibe(post.id)
                    } else {
                        onOpenPost(post.id)
                    }
                }
            )
            ProfilePanel.Edit -> EditProfile(
                state = state,
                onBack = profileViewModel::back,
                onUpdate = profileViewModel::updateDraft,
                onSave = profileViewModel::saveProfile,
                onAvatarSelected = profileViewModel::uploadAvatar
            )
            ProfilePanel.Settings -> ProfileSettingsHub(
                state = state,
                email = email,
                onBack = profileViewModel::back,
                onAccount = { profileViewModel.open(ProfilePanel.Edit) },
                onPrivacy = { profileViewModel.open(ProfilePanel.Privacy) },
                onSecurity = { profileViewModel.open(ProfilePanel.Security) },
                onPasswordReset = profileViewModel::sendPasswordReset,
                onSignOut = onSignOut
            )
            ProfilePanel.Privacy -> PrivacySettings(
                state = state,
                onBack = profileViewModel::back,
                onChange = profileViewModel::setPrivacy,
                onSave = profileViewModel::savePrivacy,
                onMeasurementChange = profileViewModel::setContentMeasurementEnabled,
                onEraseMeasurement = profileViewModel::eraseContentMeasurement,
                onUnblock = profileViewModel::unblockUser
            )
            ProfilePanel.Security -> SecuritySettings(
                state = state,
                email = email,
                onBack = profileViewModel::back,
                onResetPassword = profileViewModel::sendPasswordReset,
                onRevoke = profileViewModel::revokeDevice
            )
            ProfilePanel.Connections -> Connections(
                state = state,
                onBack = profileViewModel::back,
                onScope = profileViewModel::openConnections,
                onToggleFollow = profileViewModel::toggleConnectionFollow
            )
        }
    }
}

@Composable
private fun ProfileOverview(
    state: ProfileUiState,
    onTab: (String) -> Unit,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    onConnections: (String) -> Unit,
    onDismissMessage: () -> Unit
) {
    val profile = requireNotNull(state.privateProfile)
    val public = requireNotNull(state.publicProfile)
    val uriHandler = LocalUriHandler.current
    val safeSocialLinks = listOf("linkedin", "github", "instagram").mapNotNull { network ->
        safeSocialUrl(network, profile.socialLinks?.get(network))?.let { network to it }
    }
    val posts = if (state.activeTab == "vibes") {
        public.posts.filter { it.kind == "video" || it.placement == "vibe" }
    } else {
        public.posts.filterNot { it.kind == "video" || it.placement == "vibe" }
    }
    VybResponsiveFrame(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Profile", color = VybText, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit profile", tint = VybText)
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = VybText)
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InitialAvatar(profile.fullName, 92, profile.avatarUrl)
                    Text(
                        profile.fullName,
                        color = VybText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text("@${profile.username}", color = VybMuted)
                    Text(
                        listOf(profile.course, profile.stream).filter(String::isNotBlank).joinToString(" · "),
                        color = VybMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    profile.bio?.takeIf(String::isNotBlank)?.let {
                        Text(it, color = VybText, modifier = Modifier.padding(top = 12.dp))
                    }
                    if (safeSocialLinks.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            safeSocialLinks.forEach { (network, url) ->
                                TextButton(onClick = { uriHandler.openUri(url) }) {
                                    Text(network.replaceFirstChar { it.uppercase() })
                                }
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStat(public.stats.posts, "Posts", null)
                        ProfileStat(public.stats.followers, "Followers") { onConnections("followers") }
                        ProfileStat(public.stats.following, "Following") { onConnections("following") }
                    }
                    MessageBanner(state, onDismissMessage)
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TabButton("Posts", state.activeTab == "posts", Modifier.weight(1f)) { onTab("posts") }
                        TabButton("Vibes", state.activeTab == "vibes", Modifier.weight(1f)) { onTab("vibes") }
                    }
                }
            }
            if (posts.isEmpty()) {
                item {
                    VybEmptyState(
                        icon = Icons.Default.Person,
                        title = if (state.activeTab == "vibes") "No vibes yet" else "No posts yet",
                        body = "Your published ${state.activeTab} will appear here.",
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                items(posts, key = SocialPost::id) { post -> ProfilePost(post) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun EditProfile(
    state: ProfileUiState,
    onBack: () -> Unit,
    onUpdate: ((ProfileEditDraft) -> ProfileEditDraft) -> Unit,
    onSave: () -> Unit,
    onAvatarSelected: (android.content.ContentResolver, android.net.Uri) -> Unit
) {
    val draft = state.editDraft
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) onAvatarSelected(context.contentResolver, uri)
    }
    ProfilePage("Edit profile", onBack) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialAvatar(
                listOf(draft.firstName, draft.lastName).filter(String::isNotBlank).joinToString(" "),
                64,
                draft.avatarUrl
            )
            OutlinedButton(
                onClick = { photoPicker.launch("image/*") },
                enabled = !state.busy,
                modifier = Modifier.padding(start = 14.dp)
            ) {
                Text("Change profile photo")
            }
        }
        EditField(draft.username, { value -> onUpdate { it.copy(username = value.lowercase().take(24)) } }, "User ID")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EditField(draft.firstName, { value -> onUpdate { it.copy(firstName = value) } }, "First name", Modifier.weight(1f))
            EditField(draft.lastName, { value -> onUpdate { it.copy(lastName = value) } }, "Last name", Modifier.weight(1f))
        }
        EditField(draft.course, { value -> onUpdate { it.copy(course = value) } }, "Course")
        EditField(draft.stream, { value -> onUpdate { it.copy(stream = value) } }, "Stream")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EditField(
                draft.year,
                { value -> onUpdate { it.copy(year = value.filter(Char::isDigit).take(1)) } },
                "Year",
                Modifier.weight(1f),
                KeyboardType.Number
            )
            EditField(draft.section, { value -> onUpdate { it.copy(section = value.take(12)) } }, "Section", Modifier.weight(1f))
        }
        EditField(draft.bio, { value -> onUpdate { it.copy(bio = value.take(180)) } }, "Bio")
        EditField(draft.phoneNumber, { value -> onUpdate { it.copy(phoneNumber = value.take(19)) } }, "Phone number", keyboardType = KeyboardType.Phone)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Hosteller", color = VybText, fontWeight = FontWeight.Bold)
                Text("Campus housing details", color = VybMuted)
            }
            Switch(
                checked = draft.isHosteller,
                onCheckedChange = { value -> onUpdate { it.copy(isHosteller = value) } }
            )
        }
        if (draft.isHosteller) {
            EditField(draft.hostelName, { value -> onUpdate { it.copy(hostelName = value) } }, "Hostel name")
        }
        Text("Social links", color = VybText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
        EditField(draft.linkedin, { value -> onUpdate { it.copy(linkedin = value.take(220)) } }, "LinkedIn")
        EditField(draft.github, { value -> onUpdate { it.copy(github = value.take(220)) } }, "GitHub")
        EditField(draft.instagram, { value -> onUpdate { it.copy(instagram = value.take(220)) } }, "Instagram")
        MessageBanner(state)
        PrimaryAction("Save profile", state.busy, onSave)
    }
}

@Composable
private fun SettingsHub(
    state: ProfileUiState,
    email: String,
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    onSecurity: () -> Unit,
    onPasswordReset: () -> Unit,
    onSignOut: () -> Unit
) {
    val theme = LocalThemePreference.current
    val setTheme = LocalThemePreferenceSetter.current
    ProfilePage("Settings", onBack) {
        Text(email, color = VybMuted, modifier = Modifier.padding(bottom = 12.dp))
        SettingsRow(Icons.AutoMirrored.Filled.Chat, "Chat privacy", "Last seen, receipts and typing", onPrivacy)
        SettingsRow(Icons.Default.Security, "Security & devices", "Password recovery and trusted devices", onSecurity)
        SettingsRow(Icons.Default.LockReset, "Reset password", "Send a recovery email", onPasswordReset)
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            color = VybPanel,
            border = BorderStroke(1.dp, VybBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DarkMode, null, tint = VybIndigo)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Dark appearance", color = VybText, fontWeight = FontWeight.Bold)
                    Text("Match the Vyb web theme", color = VybMuted)
                }
                Switch(
                    checked = theme != ThemePreference.Light,
                    onCheckedChange = {
                        setTheme(if (it) ThemePreference.Dark else ThemePreference.Light)
                    }
                )
            }
        }
        MessageBanner(state)
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            border = BorderStroke(1.dp, Color(0xFFE879A9)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null)
            Text("Sign out", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun PrivacySettings(
    state: ProfileUiState,
    onBack: () -> Unit,
    onChange: (ChatPrivacySettings) -> Unit,
    onSave: () -> Unit,
    onMeasurementChange: (Boolean) -> Unit,
    onEraseMeasurement: () -> Unit,
    onUnblock: (social.vyb.app.features.search.BlockedPerson) -> Unit
) {
    val settings = state.privacy
    ProfilePage("Chat privacy", onBack) {
        Text("Who can see when you’re online", color = VybText, fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Everyone", "My Contacts", "Nobody").forEach { option ->
                TabButton(
                    option,
                    settings.lastSeenOnline == option,
                    Modifier.weight(1f)
                ) { onChange(settings.copy(lastSeenOnline = option)) }
            }
        }
        PrivacyToggle(
            "Read receipts",
            "Let people know when you read messages",
            settings.readReceipts
        ) { onChange(settings.copy(readReceipts = it)) }
        PrivacyToggle(
            "Typing indicator",
            "Show when you are typing",
            settings.typingIndicator
        ) { onChange(settings.copy(typingIndicator = it)) }
        Text(
            "Creator measurement",
            color = VybText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp)
        )
        PrivacyToggle(
            "Anonymous performance measurement",
            "Lets your own posts show privacy-preserving reach and view insights.",
            state.contentMeasurementEnabled
        ) { onMeasurementChange(it) }
        TextButton(
            onClick = onEraseMeasurement,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Erase raw measurement history", color = MaterialTheme.colorScheme.error) }
        Text(
            "Blocked accounts",
            color = VybText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
        )
        when {
            state.blockedUsersLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(12.dp),
                strokeWidth = 2.dp
            )
            state.blockedUsers.isEmpty() -> Text(
                "No blocked accounts.",
                color = VybMuted,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            else -> state.blockedUsers.forEach { user ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = VybPanel,
                    border = BorderStroke(1.dp, VybBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InitialAvatar(user.displayName, 38, user.avatarUrl)
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(user.displayName, color = VybText, fontWeight = FontWeight.SemiBold)
                            Text("@${user.username}", color = VybMuted)
                        }
                        TextButton(onClick = { onUnblock(user) }, enabled = !state.busy) {
                            Text("Unblock")
                        }
                    }
                }
            }
        }
        MessageBanner(state)
        PrimaryAction("Save privacy", state.busy, onSave)
    }
}

@Composable
private fun SecuritySettings(
    state: ProfileUiState,
    email: String,
    onBack: () -> Unit,
    onResetPassword: () -> Unit,
    onRevoke: (TrustedDevice) -> Unit
) {
    var pendingRevocation by remember { mutableStateOf<TrustedDevice?>(null) }
    ProfilePage("Security & devices", onBack) {
        Text("Account recovery", color = VybText, fontWeight = FontWeight.Bold)
        Text(email, color = VybMuted, modifier = Modifier.padding(top = 3.dp))
        PrimaryAction("Send password reset email", state.busy, onResetPassword)
        Text(
            "Trusted chat devices",
            color = VybText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)
        )
        if (state.devices.isEmpty()) {
            Text("No trusted devices are registered yet.", color = VybMuted)
        } else {
            state.devices.forEach { device ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    color = VybPanel,
                    border = BorderStroke(1.dp, VybBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Smartphone, null, tint = VybIndigo)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(device.label, color = VybText, fontWeight = FontWeight.Bold)
                            Text(
                                "${device.platform} · ${if (device.isCurrentDevice) "Current device" else "Last active ${device.lastSeenAt.take(10)}"}",
                                color = VybMuted
                            )
                        }
                        if (!device.isCurrentDevice) {
                            OutlinedButton(
                                onClick = { pendingRevocation = device },
                                enabled = !state.busy
                            ) {
                                Text("Revoke")
                            }
                        }
                    }
                }
            }
        }
        MessageBanner(state)
    }
    pendingRevocation?.let { device ->
        AlertDialog(
            onDismissRequest = { pendingRevocation = null },
            title = { Text("Revoke ${device.label}?") },
            text = {
                Text("This device will lose access to encrypted chat keys. You may need an existing trusted device to add it again.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRevocation = null
                        onRevoke(device)
                    }
                ) { Text("Revoke") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRevocation = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun Connections(
    state: ProfileUiState,
    onBack: () -> Unit,
    onScope: (String) -> Unit,
    onToggleFollow: (ProfileConnection) -> Unit
) {
    VybResponsiveFrame(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PageHeader("Connections", onBack)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabButton("Followers", state.connectionScope == "followers", Modifier.weight(1f)) { onScope("followers") }
                TabButton("Following", state.connectionScope == "following", Modifier.weight(1f)) { onScope("following") }
            }
            when {
                state.busy -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VybIndigo)
                }
                state.error != null -> VybEmptyState(
                    icon = Icons.Default.Group,
                    title = "Connections unavailable",
                    body = state.error,
                    actionLabel = "Try again",
                    onAction = { onScope(state.connectionScope) },
                    modifier = Modifier.padding(20.dp)
                )
                state.connections.isEmpty() -> VybEmptyState(
                    icon = Icons.Default.Group,
                    title = "No ${state.connectionScope} yet",
                    body = "Campus connections will appear here.",
                    modifier = Modifier.padding(20.dp)
                )
                else -> LazyColumn {
                    items(state.connections, key = ProfileConnection::userId) { person ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InitialAvatar(person.displayName, 48, person.avatarUrl)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(person.displayName, color = VybText, fontWeight = FontWeight.Bold)
                                Text("@${person.username}", color = VybMuted)
                            }
                            if (!person.isViewer) {
                                OutlinedButton(onClick = { onToggleFollow(person) }) {
                                    Text(if (person.isFollowing) "Following" else "Follow")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    VybResponsiveFrame(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            PageHeader(title, onBack)
            content()
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = VybText)
        }
        Text(title, color = VybText, fontSize = 21.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        onClick = onClick,
        color = VybPanel,
        border = BorderStroke(1.dp, VybBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = VybIndigo)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = VybText, fontWeight = FontWeight.Bold)
                Text(body, color = VybMuted)
            }
            Icon(Icons.Default.ChevronRight, null, tint = VybMuted)
        }
    }
}

@Composable
private fun EditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth().padding(bottom = 10.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = VybPanelLifted,
            unfocusedContainerColor = VybPanelLifted,
            focusedTextColor = VybText,
            unfocusedTextColor = VybText,
            focusedBorderColor = VybIndigo,
            unfocusedBorderColor = VybBorder,
            focusedLabelColor = VybMuted,
            unfocusedLabelColor = VybMuted
        )
    )
}

@Composable
private fun PrivacyToggle(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = VybText, fontWeight = FontWeight.Bold)
            Text(body, color = VybMuted)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PrimaryAction(label: String, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = VybIndigo),
        shape = RoundedCornerShape(14.dp)
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = VybText)
        else Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = if (selected) VybIndigo else VybPanel,
        border = BorderStroke(1.dp, if (selected) VybIndigo else VybBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
            Text(label, color = VybText, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun ProfileStat(value: Int, label: String, onClick: (() -> Unit)?) {
    Column(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick).padding(8.dp) else Modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value.toString(), color = VybText, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text(label, color = VybMuted)
    }
}

@Composable
private fun ProfilePost(post: SocialPost) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        color = VybPanel,
        border = BorderStroke(1.dp, VybBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (post.title.isNotBlank()) Text(post.title, color = VybText, fontWeight = FontWeight.Bold)
            Text(post.body, color = VybText, modifier = Modifier.padding(top = 4.dp))
            post.mediaUrl?.takeIf(String::isNotBlank)?.let { url ->
                if (post.kind == "video") {
                    VybRemoteVideo(
                        url = url,
                        contentDescription = "${post.author.displayName} video",
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).padding(top = 10.dp)
                    )
                } else {
                    VybRemoteImage(
                        url = url,
                        contentDescription = "${post.author.displayName} post image",
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).padding(top = 10.dp)
                    )
                }
            }
            Text(
                "${post.reactions} likes · ${post.comments} comments",
                color = VybMuted,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun InitialAvatar(name: String, size: Int, avatarUrl: String? = null) {
    SocialAvatar(
        avatarUrl = avatarUrl,
        displayName = name,
        size = size.dp
    )
}

@Composable
private fun MessageBanner(state: ProfileUiState, onDismiss: (() -> Unit)? = null) {
    val message = state.error ?: state.notice ?: return
    val isError = state.error != null
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        color = if (isError) MaterialTheme.colorScheme.errorContainer
        else VybTeal.copy(alpha = .14f),
        border = BorderStroke(
            1.dp,
            if (isError) MaterialTheme.colorScheme.error.copy(alpha = .32f)
            else VybTeal.copy(alpha = .34f)
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = { onDismiss?.invoke() }
    ) {
        Text(
            message,
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else VybText,
            modifier = Modifier.padding(12.dp)
        )
    }
}
