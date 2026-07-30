package social.vyb.app.features.social

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import social.vyb.app.features.media.MediaComposerScreen
import social.vyb.app.features.media.MediaPublishIntent
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostComposer(
    state: SocialActionsUiState,
    displayName: String,
    username: String,
    communities: List<PostCommunityOption>,
    onPublish: (
        text: String,
        isAnonymous: Boolean,
        allowAnonymousComments: Boolean,
        visibility: String,
        communityId: String?,
        onPublished: () -> Unit
    ) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var anonymous by remember { mutableStateOf(false) }
    var allowAnonymousComments by remember { mutableStateOf(true) }
    var selectedCommunityId by remember(communities) {
        mutableStateOf<String?>(null)
    }
    var reach by remember { mutableStateOf(PostReach.Public) }
    LaunchedEffect(communities) {
        if (reach == PostReach.CommunityOnly && selectedCommunityId == null) {
            selectedCommunityId = communities.firstOrNull()?.id
            if (selectedCommunityId == null) reach = PostReach.Public
        }
    }
    var selectedIntent by remember { mutableStateOf(MediaPublishIntent.Post) }
    var mediaComposerOpen by remember { mutableStateOf(false) }
    var settingsDialogOpen by remember { mutableStateOf(false) }
    var externalPickerOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var dismissing by remember { mutableStateOf(false) }

    fun hideThenDismiss() {
        if (dismissing) return
        dismissing = true
        scope.launch {
            runCatching {
                if (sheetState.isVisible) sheetState.hide()
            }
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!state.creatingPost && !externalPickerOpen) {
                if (mediaComposerOpen) mediaComposerOpen = false else hideThenDismiss()
            }
        },
        modifier = modifier,
        sheetState = sheetState,
        containerColor = StudioBackground,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = .22f), RoundedCornerShape(99.dp))
            )
        }
    ) {
        if (selectedIntent != MediaPublishIntent.Post || mediaComposerOpen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(.92f)
            ) {
                StudioHeader(
                    enabled = !state.creatingPost,
                    selectedIntent = selectedIntent,
                    showSettings = true,
                    onIntentSelected = { intent ->
                        selectedIntent = intent
                        mediaComposerOpen = false
                        if (intent == MediaPublishIntent.Story) anonymous = false
                    },
                    onOpenSettings = { settingsDialogOpen = true },
                    onDismiss = ::hideThenDismiss
                )
                HorizontalDivider(color = Color.White.copy(alpha = .07f))
                MediaComposerScreen(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    initialIntent = selectedIntent,
                    initialCaption = text,
                    autoLaunchPicker = mediaComposerOpen,
                    showIntentPicker = false,
                    displayName = displayName,
                    username = username,
                    isAnonymous = anonymous && selectedIntent != MediaPublishIntent.Story,
                    allowAnonymousComments = allowAnonymousComments,
                    visibility = reach.wireValue,
                    communityId = selectedCommunityId.takeIf {
                        reach == PostReach.CommunityOnly
                    },
                    onCancelCreation = ::hideThenDismiss,
                    onExternalPickerChanged = { externalPickerOpen = it },
                    onPublished = {
                        mediaComposerOpen = false
                        hideThenDismiss()
                    }
                )
            }
        } else {
            CreationStudioPostContent(
                state = state,
                displayName = displayName,
                username = username,
                text = text,
                anonymous = anonymous,
                onTextChanged = { text = it.take(2_000) },
                onIntentSelected = { intent ->
                    selectedIntent = intent
                    mediaComposerOpen = false
                    if (intent == MediaPublishIntent.Story) anonymous = false
                },
                onOpenSettings = { settingsDialogOpen = true },
                onAddPhoto = { mediaComposerOpen = true },
                onDismiss = ::hideThenDismiss,
                onPublish = {
                    onPublish(
                        text,
                        anonymous,
                        allowAnonymousComments,
                        reach.wireValue,
                        selectedCommunityId.takeIf { reach == PostReach.CommunityOnly },
                        ::hideThenDismiss
                    )
                }
            )
        }
    }

    if (settingsDialogOpen) {
        PostSettingsDialog(
            anonymous = anonymous,
            allowAnonymousComments = allowAnonymousComments,
            reach = reach,
            communities = communities,
            selectedCommunityId = selectedCommunityId,
            enabled = !state.creatingPost,
            allowAnonymous = selectedIntent != MediaPublishIntent.Story,
            intent = selectedIntent,
            onAnonymousChanged = {
                if (selectedIntent != MediaPublishIntent.Story) anonymous = it
            },
            onAnonymousCommentsChanged = { allowAnonymousComments = it },
            onCommunityChanged = { selectedCommunityId = it },
            onReachChanged = {
                reach = it
                if (it == PostReach.CommunityOnly) {
                    selectedCommunityId = selectedCommunityId ?: communities.firstOrNull()?.id
                }
            },
            onDismiss = { settingsDialogOpen = false }
        )
    }
}

@Composable
private fun CreationStudioPostContent(
    state: SocialActionsUiState,
    displayName: String,
    username: String,
    text: String,
    anonymous: Boolean,
    onTextChanged: (String) -> Unit,
    onIntentSelected: (MediaPublishIntent) -> Unit,
    onOpenSettings: () -> Unit,
    onAddPhoto: () -> Unit,
    onDismiss: () -> Unit,
    onPublish: () -> Unit
) {
    val authorName = displayName.ifBlank { "Vyb member" }
    val initials = authorName
        .split(" ")
        .filter(String::isNotBlank)
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { "V" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(.9f)
            .imePadding()
    ) {
        StudioHeader(
            enabled = !state.creatingPost,
            selectedIntent = MediaPublishIntent.Post,
            showSettings = true,
            onIntentSelected = onIntentSelected,
            onOpenSettings = onOpenSettings,
            onDismiss = onDismiss
        )

        HorizontalDivider(color = Color.White.copy(alpha = .07f))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StudioAuthorRow(
                authorName = authorName,
                username = username,
                initials = initials,
                anonymous = anonymous
            )

            HorizontalDivider(color = Color.White.copy(alpha = .06f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 132.dp)
                    .padding(vertical = 4.dp)
            ) {
                if (text.isEmpty()) {
                    Text(
                        "What's on your mind? #hashtag @mention",
                        color = VybMuted.copy(alpha = .55f),
                        fontSize = 17.sp,
                        lineHeight = 25.sp
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    enabled = !state.creatingPost,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 132.dp),
                    textStyle = TextStyle(
                        color = VybText,
                        fontSize = 17.sp,
                        lineHeight = 25.sp
                    ),
                    cursorBrush = Brush.verticalGradient(listOf(StudioViolet, StudioIndigo))
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = .06f))

            Text(
                "Photos",
                color = VybText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .border(
                        1.dp,
                        Color.White.copy(alpha = .16f),
                        RoundedCornerShape(14.dp)
                    )
                    .background(Color.White.copy(alpha = .03f), RoundedCornerShape(14.dp))
                    .clickable(
                        enabled = !state.creatingPost,
                        onClick = onAddPhoto
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        "Add photo",
                        color = VybMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            state.createPostError?.let {
                Text(
                    it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = .1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    color = Color(0xFFFCA5A5),
                    fontSize = 13.sp
                )
            }
        }

        if (state.creatingPost) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = StudioTeal,
                    trackColor = StudioViolet.copy(alpha = .16f)
                )
                Text(
                    "Publishing your post…",
                    color = StudioTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        StudioFooter(
            publishing = state.creatingPost,
            canPublish = text.isNotBlank(),
            onPublish = onPublish
        )
    }
}

@Composable
private fun StudioHeader(
    enabled: Boolean,
    selectedIntent: MediaPublishIntent,
    showSettings: Boolean,
    onIntentSelected: (MediaPublishIntent) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Create",
            modifier = Modifier.weight(1f),
            color = VybText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Box {
            Surface(
                onClick = { if (enabled) menuExpanded = true },
                enabled = enabled,
                color = Color.White.copy(alpha = .06f),
                shape = RoundedCornerShape(99.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = .1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(start = 15.dp, end = 9.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selectedIntent.name,
                        color = VybText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = VybMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = Color(0xFF151B2D)
            ) {
                MediaPublishIntent.entries.forEach { intent ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                intent.name,
                                color = if (selectedIntent == intent) StudioTeal else VybText,
                                fontWeight = if (selectedIntent == intent) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Medium
                                }
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onIntentSelected(intent)
                        }
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        if (showSettings) {
            StudioCircleButton(
                icon = Icons.Default.Settings,
                description = "${selectedIntent.name} settings",
                enabled = enabled,
                onClick = onOpenSettings
            )
            Spacer(Modifier.width(8.dp))
        }
        StudioCircleButton(
            icon = Icons.Default.Close,
            description = "Close",
            enabled = enabled,
            onClick = onDismiss
        )
    }
}

@Composable
private fun StudioCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .border(1.dp, Color.White.copy(alpha = .1f), CircleShape)
            .background(Color.White.copy(alpha = .05f), CircleShape)
    ) {
        Icon(icon, description, tint = VybMuted)
    }
}

@Composable
private fun StudioAuthorRow(
    authorName: String,
    username: String,
    initials: String,
    anonymous: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.linearGradient(listOf(StudioIndigo, StudioViolet)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                if (anonymous) "Anonymous Vyber" else authorName,
                color = VybText,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (anonymous) "@anonymous" else "@$username",
                color = VybMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PostSettingsDialog(
    anonymous: Boolean,
    allowAnonymousComments: Boolean,
    reach: PostReach,
    communities: List<PostCommunityOption>,
    selectedCommunityId: String?,
    enabled: Boolean,
    allowAnonymous: Boolean,
    intent: MediaPublishIntent,
    onAnonymousChanged: (Boolean) -> Unit,
    onAnonymousCommentsChanged: (Boolean) -> Unit,
    onCommunityChanged: (String) -> Unit,
    onReachChanged: (PostReach) -> Unit,
    onDismiss: () -> Unit
) {
    var communityMenuOpen by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111827),
        titleContentColor = VybText,
        textContentColor = VybMuted,
        title = { Text("${intent.name} settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (allowAnonymous) {
                    SettingsToggleRow(
                        checked = anonymous,
                        title = "Publish anonymously",
                        description = "Hide your identity on this ${intent.name.lowercase()}",
                        enabled = enabled,
                        onCheckedChange = onAnonymousChanged
                    )
                } else {
                    Text(
                        "Stories always show who published them.",
                        color = VybMuted,
                        fontSize = 12.sp
                    )
                }
                if (intent != MediaPublishIntent.Story) {
                    SettingsToggleRow(
                        checked = allowAnonymousComments,
                        title = "Allow anonymous comments",
                        description = "Let people reply without showing their identity",
                        enabled = enabled,
                        onCheckedChange = onAnonymousCommentsChanged
                    )
                }
                HorizontalDivider(color = Color.White.copy(alpha = .08f))
                Text(
                    "${intent.name} reach",
                    color = VybText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                PostReach.entries.forEach { option ->
                    val optionEnabled = enabled &&
                        (option != PostReach.CommunityOnly || communities.isNotEmpty())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (reach == option) StudioViolet.copy(alpha = .1f)
                                else Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = optionEnabled) { onReachChanged(option) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = reach == option,
                            onClick = { onReachChanged(option) },
                            enabled = optionEnabled
                        )
                        Column(Modifier.padding(start = 4.dp)) {
                            Text(
                                option.label,
                                color = if (optionEnabled) VybText else VybMuted.copy(alpha = .65f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                option.description,
                                color = if (optionEnabled) VybMuted else VybMuted.copy(alpha = .55f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
                if (reach == PostReach.CommunityOnly) {
                    val selectedCommunity = communities.firstOrNull {
                        it.id == selectedCommunityId
                    }
                    Box(Modifier.fillMaxWidth()) {
                        Surface(
                            onClick = { communityMenuOpen = true },
                            enabled = enabled && communities.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White.copy(alpha = .055f),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                StudioViolet.copy(alpha = .35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    selectedCommunity?.name ?: "Choose community",
                                    modifier = Modifier.weight(1f),
                                    color = VybText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = VybMuted
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = communityMenuOpen,
                            onDismissRequest = { communityMenuOpen = false },
                            containerColor = Color(0xFF172033)
                        ) {
                            communities.forEach { community ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            community.name,
                                            color = if (community.id == selectedCommunityId) {
                                                StudioTeal
                                            } else {
                                                VybText
                                            }
                                        )
                                    },
                                    onClick = {
                                        onCommunityChanged(community.id)
                                        communityMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = StudioTeal, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SettingsToggleRow(
    checked: Boolean,
    title: String,
    description: String,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = .035f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = StudioViolet,
                checkmarkColor = Color.White
            )
        )
        Column(Modifier.padding(start = 4.dp)) {
            Text(
                title,
                color = VybText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                description,
                color = VybMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun StudioToggle(
    checked: Boolean,
    title: String,
    accent: Color,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (checked) {
        accent.copy(alpha = .38f)
    } else {
        Color.White.copy(alpha = .12f)
    }
    val backgroundColor = if (checked) {
        accent.copy(alpha = .1f)
    } else {
        Color(0xFF0F172A).copy(alpha = .52f)
    }
    Row(
        modifier = modifier
            .height(52.dp)
            .background(backgroundColor, RoundedCornerShape(99.dp))
            .border(1.dp, borderColor, RoundedCornerShape(99.dp))
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = accent,
                uncheckedColor = VybMuted.copy(alpha = .75f),
                checkmarkColor = Color(0xFF041016)
            )
        )
        Text(
            title,
            color = VybText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StudioFooter(
    publishing: Boolean,
    canPublish: Boolean,
    onPublish: () -> Unit
) {
    HorizontalDivider(color = StudioViolet.copy(alpha = .22f))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0E1A).copy(alpha = .92f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Up to 6 photos · Text-only posts are fine too",
            modifier = Modifier.fillMaxWidth(),
            color = VybMuted.copy(alpha = .75f),
            fontSize = 11.sp
        )
        Button(
                onClick = onPublish,
                enabled = canPublish && !publishing,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioIndigo,
                    disabledContainerColor = StudioIndigo.copy(alpha = .12f),
                    disabledContentColor = StudioViolet.copy(alpha = .45f)
                )
            ) {
                if (publishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(17.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Queueing…")
                } else {
                    Text("Publish Post ✦", fontWeight = FontWeight.Bold, maxLines = 1)
                }
        }
    }
}

private val StudioBackground = Color(0xFF080D1A)
private val StudioIndigo = Color(0xFF6366F1)
private val StudioViolet = Color(0xFFA855F7)
private val StudioTeal = Color(0xFF22D3C5)

@Composable
fun SocialOperationFeedback(
    state: SocialActionsUiState,
    onDismissError: () -> Unit,
    onDismissNotice: () -> Unit
) {
    state.operationError?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Action failed") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onDismissError) { Text("OK") }
            }
        )
    } ?: state.operationNotice?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissNotice,
            title = { Text("Done") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onDismissNotice) { Text("OK") }
            }
        )
    }
}

@Composable
fun PostOverflowActions(
    postId: String,
    title: String,
    body: String,
    isOwner: Boolean,
    busy: Boolean,
    reactionMembers: ReactionMembersState,
    onLoadReactionMembers: () -> Unit,
    onViewPost: (() -> Unit)? = null,
    onRepost: (quote: String, placement: String) -> Unit,
    onUpdate: (title: String, body: String) -> Unit,
    onDelete: () -> Unit,
    onReport: (reason: String) -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val context = LocalContext.current
    var menuOpen by remember(postId) { mutableStateOf(false) }
    var dialog by remember(postId) { mutableStateOf<String?>(null) }
    var draftTitle by remember(postId, title) { mutableStateOf(title) }
    var draftBody by remember(postId, body) { mutableStateOf(body) }
    var quote by remember(postId) { mutableStateOf("") }
    var repostPlacement by remember(postId) { mutableStateOf("feed") }
    var reason by remember(postId) { mutableStateOf("") }

    IconButton(onClick = { menuOpen = true }, enabled = !busy) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = iconTint)
        } else {
            Icon(Icons.Default.MoreHoriz, "More post actions", tint = iconTint)
        }
    }

    if (menuOpen) {
        AlertDialog(
            onDismissRequest = { menuOpen = false },
            title = { Text("Post actions") },
            text = {
                Column {
                    onViewPost?.let { openPost ->
                        TextButton(onClick = {
                            openPost()
                            menuOpen = false
                        }) { Text("View full post") }
                    }
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                        clipboard.setPrimaryClip(
                            android.content.ClipData.newPlainText(
                                "Vyb post",
                                postPermalink(postId)
                            )
                        )
                        Toast.makeText(context, "Post link copied", Toast.LENGTH_SHORT).show()
                        menuOpen = false
                    }) { Text("Copy link") }
                    TextButton(onClick = {
                        onLoadReactionMembers()
                        dialog = "likes"
                        menuOpen = false
                    }) { Text("View likes") }
                    TextButton(onClick = {
                        dialog = "repost"
                        menuOpen = false
                    }) { Text("Repost") }
                    if (isOwner) {
                        TextButton(onClick = {
                            dialog = "edit"
                            menuOpen = false
                        }) { Text("Edit") }
                        TextButton(onClick = {
                            dialog = "delete"
                            menuOpen = false
                        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    } else {
                        TextButton(onClick = {
                            dialog = "report"
                            menuOpen = false
                        }) { Text("Report", color = MaterialTheme.colorScheme.error) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { menuOpen = false }) { Text("Close") }
            }
        )
    }

    when (dialog) {
        "likes" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Likes") },
            text = {
                when {
                    reactionMembers.loading -> CircularProgressIndicator()
                    reactionMembers.error != null -> Text(
                        reactionMembers.error,
                        color = MaterialTheme.colorScheme.error
                    )
                    reactionMembers.items.isEmpty() -> Text("No likes yet.")
                    else -> LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(reactionMembers.items, key = { it.membershipId }) { member ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                                Text(member.displayName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "@${member.username}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Done") } }
        )
        "repost" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Repost") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = quote,
                        onValueChange = { quote = it },
                        label = { Text("Add a thought (optional)") },
                        maxLines = 4
                    )
                    Text("Share to", fontWeight = FontWeight.SemiBold)
                    Row {
                        RepostPlacements.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .clickable { repostPlacement = option.value }
                                    .padding(end = 18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = repostPlacement == option.value,
                                    onClick = { repostPlacement = option.value }
                                )
                                Text(option.label)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onRepost(quote, repostPlacement)
                    dialog = null
                }) { Text("Repost") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } }
        )
        "edit" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Edit post") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        label = { Text("Title") }
                    )
                    OutlinedTextField(
                        value = draftBody,
                        onValueChange = { draftBody = it },
                        label = { Text("Post") },
                        minLines = 3,
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = draftBody.isNotBlank(),
                    onClick = {
                        onUpdate(draftTitle, draftBody)
                        dialog = null
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } }
        )
        "delete" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Delete post?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    dialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } }
        )
        "report" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Report post") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    minLines = 2,
                    maxLines = 5
                )
            },
            confirmButton = {
                TextButton(
                    enabled = reason.isNotBlank(),
                    onClick = {
                        onReport(reason)
                        dialog = null
                    }
                ) { Text("Submit report") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } }
        )
    }
}

internal data class ReactionChoice(
    val type: String,
    val symbol: String,
    val label: String
)

internal val ReactionChoices = listOf(
    ReactionChoice("like", "👍", "Like"),
    ReactionChoice("fire", "🔥", "Fire"),
    ReactionChoice("support", "👏", "Support"),
    ReactionChoice("love", "❤️", "Love"),
    ReactionChoice("insight", "💡", "Insightful"),
    ReactionChoice("funny", "😂", "Funny")
)

internal data class RepostPlacement(
    val value: String,
    val label: String
)

internal val RepostPlacements = listOf(
    RepostPlacement("feed", "Feed"),
    RepostPlacement("vibe", "Vibes")
)

fun postPermalink(postId: String): String =
    "https://vybnet.app/post/${
        URLEncoder.encode(postId, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }"

fun postShareText(postId: String, title: String, body: String): String =
    "${body.ifBlank { title }}\n${postPermalink(postId)}".trim()

private fun sharePost(context: android.content.Context, text: String) {
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            },
            "Share post"
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostActionsBar(
    postId: String,
    engagement: PostEngagementState,
    commentCount: Int,
    title: String = "",
    body: String = "",
    onToggleReaction: (String) -> Unit,
    onOpenReactions: () -> Unit,
    onOpenComments: () -> Unit,
    onRepost: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var reactionsOpen by remember(postId) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${engagement.reactionCount} reactions",
                modifier = Modifier.clickable(onClick = onOpenReactions),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.width(16.dp))
            Text(
                "$commentCount comments",
                modifier = Modifier.clickable(onClick = onOpenComments),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${engagement.savedCount} shares",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .combinedClickable(
                            enabled = !engagement.reactionLoading,
                            onClick = {
                                onToggleReaction(engagement.viewerReactionType ?: "like")
                            },
                            onLongClick = { reactionsOpen = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (engagement.reactionLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        val choice = ReactionChoices.firstOrNull {
                            it.type == engagement.viewerReactionType
                        }
                        if (choice != null) {
                            Text(choice.symbol, fontSize = 21.sp)
                        } else {
                            Icon(Icons.Outlined.FavoriteBorder, "React to post")
                        }
                    }
                }
                DropdownMenu(
                    expanded = reactionsOpen,
                    onDismissRequest = { reactionsOpen = false }
                ) {
                    ReactionChoices.forEach { choice ->
                        DropdownMenuItem(
                            text = { Text("${choice.symbol}  ${choice.label}") },
                            onClick = {
                                reactionsOpen = false
                                onToggleReaction(choice.type)
                            }
                        )
                    }
                }
            }
            IconButton(onClick = onOpenComments) {
                Icon(Icons.Outlined.ChatBubbleOutline, "Open comments")
            }
            IconButton(
                onClick = onShare ?: {
                    sharePost(context, postShareText(postId, title, body))
                }
            ) {
                Icon(Icons.Default.Share, "Share post")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRepost) {
                Icon(Icons.Default.Repeat, "Repost")
            }
            IconButton(onClick = onToggleSave, enabled = !engagement.saveLoading) {
                if (engagement.saveLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        if (engagement.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (engagement.isSaved) "Remove saved post" else "Save post"
                    )
                }
            }
        }
    }
}

@Composable
fun PostReactionMembersDialog(
    state: ReactionMembersState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reactions") },
        text = {
            when {
                state.loading -> CircularProgressIndicator()
                state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error)
                state.items.isEmpty() -> Text("No reactions yet.")
                else -> LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(state.items, key = { it.membershipId }) { member ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                ReactionChoices.firstOrNull {
                                    it.type == member.reactionType
                                }?.symbol ?: "👍",
                                fontSize = 22.sp
                            )
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(member.displayName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "@${member.username}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
fun PostRepostDialog(
    onDismiss: () -> Unit,
    onRepost: (quote: String, placement: String) -> Unit
) {
    var quote by remember { mutableStateOf("") }
    var placement by remember { mutableStateOf("feed") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Repost") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quote,
                    onValueChange = { quote = it },
                    label = { Text("Add a thought (optional)") },
                    maxLines = 4
                )
                Text("Share to", fontWeight = FontWeight.SemiBold)
                Row {
                    RepostPlacements.forEach { option ->
                        Row(
                            Modifier
                                .clickable { placement = option.value }
                                .padding(end = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = placement == option.value,
                                onClick = { placement = option.value }
                            )
                            Text(option.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onRepost(quote, placement) }) { Text("Repost") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    postId: String,
    thread: CommentThreadState,
    onLoad: () -> Unit,
    onRetry: () -> Unit,
    onAddComment: (text: String, parentCommentId: String?, onAdded: () -> Unit) -> Unit,
    onToggleCommentReaction: (commentId: String) -> Unit,
    onUpdateComment: (commentId: String, body: String) -> Unit,
    onDeleteComment: (commentId: String) -> Unit,
    busyCommentIds: Set<String>,
    onDismiss: () -> Unit
) {
    var text by remember(postId) { mutableStateOf("") }
    var replyTo by remember(postId) { mutableStateOf<SocialComment?>(null) }
    LaunchedEffect(postId) { onLoad() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp)
        ) {
            Text("Comments", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            when {
                thread.loading -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
                thread.error != null && thread.items.isEmpty() -> {
                    Text(thread.error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
                thread.items.isEmpty() -> {
                    Text("No comments yet. Start the conversation.")
                }
                else -> {
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(thread.items, key = { it.id }) { comment ->
                            CommentRow(
                                comment = comment,
                                busy = comment.id in busyCommentIds,
                                onReply = { replyTo = comment },
                                onToggleReaction = { onToggleCommentReaction(comment.id) },
                                onUpdate = onUpdateComment,
                                onDelete = onDeleteComment,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            thread.error?.takeIf { thread.items.isNotEmpty() }?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
            replyTo?.let { target ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Replying to ${target.author?.displayName ?: "member"}",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { replyTo = null }) { Text("Cancel") }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a comment") },
                    enabled = !thread.submitting,
                    maxLines = 4
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onAddComment(text, replyTo?.id) {
                            text = ""
                            replyTo = null
                        }
                    },
                    enabled = text.trim().length >= 2 && !thread.submitting
                ) {
                    if (thread.submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(18.dp).height(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send")
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CommentRow(
    comment: SocialComment,
    busy: Boolean,
    onReply: () -> Unit,
    onToggleReaction: () -> Unit,
    onUpdate: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember(comment.id) { mutableStateOf(false) }
    var draft by remember(comment.id, comment.body) { mutableStateOf(comment.body) }
    Column(
        modifier
            .padding(start = if (comment.parentCommentId == null) 0.dp else 22.dp)
            .padding(vertical = 8.dp)
    ) {
        Text(
            comment.author?.displayName
                ?: if (comment.isAnonymous) "Anonymous" else "Vyb member",
            style = MaterialTheme.typography.labelLarge
        )
        if (editing) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(comment.body, style = MaterialTheme.typography.bodyMedium)
        }
        Row {
            TextButton(
                onClick = onToggleReaction,
                enabled = !busy,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Icon(
                    imageVector = if (comment.viewerHasLiked) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = if (comment.viewerHasLiked) {
                        "Unlike comment"
                    } else {
                        "Like comment"
                    },
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (comment.reactions > 0) comment.reactions.toString() else "Like"
                )
            }
            TextButton(onClick = onReply, enabled = !busy) { Text("Reply") }
            if (comment.viewerCanManage) {
                TextButton(
                    onClick = {
                        if (editing) {
                            onUpdate(comment.id, draft)
                            editing = false
                        } else {
                            editing = true
                        }
                    },
                    enabled = !busy && (!editing || draft.trim().length >= 2)
                ) { Text(if (editing) "Save" else "Edit") }
                TextButton(onClick = { onDelete(comment.id) }, enabled = !busy) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
