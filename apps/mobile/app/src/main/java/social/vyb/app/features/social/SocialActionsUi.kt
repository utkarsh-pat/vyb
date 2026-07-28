package social.vyb.app.features.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PermMedia
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val draftPreferences = remember(context) {
        context.getSharedPreferences("vybnet_post_draft", android.content.Context.MODE_PRIVATE)
    }
    var text by remember {
        mutableStateOf(draftPreferences.getString("text", "").orEmpty())
    }
    var anonymous by remember {
        mutableStateOf(draftPreferences.getBoolean("anonymous", false))
    }
    var allowAnonymousComments by remember {
        mutableStateOf(draftPreferences.getBoolean("allow_anonymous_comments", true))
    }
    var selectedCommunityId by remember(communities) {
        mutableStateOf(
            draftPreferences.getString("community_id", null)
                ?.takeIf { savedId -> communities.any { it.id == savedId } }
        )
    }
    var reach by remember {
        mutableStateOf(
            PostReach.fromWireValue(
                draftPreferences.getString("visibility", PostReach.Public.wireValue)
            )
        )
    }
    LaunchedEffect(communities) {
        if (reach == PostReach.CommunityOnly && selectedCommunityId == null) {
            selectedCommunityId = communities.firstOrNull()?.id
            if (selectedCommunityId == null) reach = PostReach.Public
        }
    }
    var selectedIntent by remember { mutableStateOf(MediaPublishIntent.Post) }
    var mediaComposerOpen by remember { mutableStateOf(false) }
    var settingsDialogOpen by remember { mutableStateOf(false) }
    var utilityDialogOpen by remember { mutableStateOf(false) }
    var scheduleDialogOpen by remember { mutableStateOf(false) }
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
                    onScheduled = ::hideThenDismiss,
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
                onSaveDraft = {
                    draftPreferences.edit()
                        .putString("text", text)
                        .putBoolean("anonymous", anonymous)
                        .putBoolean(
                            "allow_anonymous_comments",
                            allowAnonymousComments
                        )
                        .putString("visibility", reach.wireValue)
                        .putString("community_id", selectedCommunityId)
                        .putLong("saved_at", System.currentTimeMillis())
                        .apply()
                    hideThenDismiss()
                },
                onOpenSchedule = { utilityDialogOpen = true },
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

    if (utilityDialogOpen) {
        PostUtilityDialog(
            onDismiss = { utilityDialogOpen = false },
            onCancelCreation = {
                utilityDialogOpen = false
                hideThenDismiss()
            },
            onSaveDraft = {
                utilityDialogOpen = false
                draftPreferences.edit()
                    .putString("text", text)
                    .putBoolean("anonymous", anonymous)
                    .putBoolean(
                        "allow_anonymous_comments",
                        allowAnonymousComments
                    )
                    .putString("visibility", reach.wireValue)
                    .putString("community_id", selectedCommunityId)
                    .putLong("saved_at", System.currentTimeMillis())
                    .apply()
                hideThenDismiss()
            },
            onSchedule = {
                utilityDialogOpen = false
                scheduleDialogOpen = true
            }
        )
    }

    if (scheduleDialogOpen) {
        SchedulePostDialog(
            onDismiss = { scheduleDialogOpen = false },
            onSchedule = { publishAtMillis ->
                ScheduledPostWorker.schedule(
                    context = context,
                    text = text,
                    isAnonymous = anonymous,
                    allowAnonymousComments = allowAnonymousComments,
                    visibility = reach.wireValue,
                    communityId = selectedCommunityId.takeIf { reach == PostReach.CommunityOnly },
                    publishAtMillis = publishAtMillis
                )
                draftPreferences.edit().clear().apply()
                scheduleDialogOpen = false
                hideThenDismiss()
            }
        )
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
    onSaveDraft: () -> Unit,
    onOpenSchedule: () -> Unit,
    onPublish: () -> Unit
) {
    val authorName = displayName.ifBlank { "Vybnet member" }
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
            onDismiss = onDismiss,
            onSaveDraft = onSaveDraft,
            onOpenSchedule = onOpenSchedule,
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
            .size(38.dp)
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
    onDismiss: () -> Unit,
    onSaveDraft: () -> Unit,
    onOpenSchedule: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onOpenSchedule,
                enabled = canPublish && !publishing,
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = StudioViolet.copy(
                    alpha = if (canPublish && !publishing) .16f else .06f
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    StudioViolet.copy(
                        alpha = if (canPublish && !publishing) .38f else .14f
                    )
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Post utilities",
                        tint = if (canPublish && !publishing) {
                            Color(0xFFC4B5FD)
                        } else {
                            VybMuted.copy(alpha = .35f)
                        },
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
            Button(
                onClick = onPublish,
                enabled = canPublish && !publishing,
                modifier = Modifier.weight(1f).height(50.dp),
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
}

@Composable
private fun LegacyStudioFooterWithSplitActions(
    publishing: Boolean,
    canPublish: Boolean,
    onDismiss: () -> Unit,
    onSaveDraft: () -> Unit,
    onOpenSchedule: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !publishing,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(99.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = .1f)
                )
            ) {
                Text("Cancel", color = VybMuted, fontSize = 12.sp, maxLines = 1)
            }
            OutlinedButton(
                onClick = onSaveDraft,
                enabled = canPublish && !publishing,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(99.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    StudioTeal.copy(alpha = .32f)
                )
            ) {
                Icon(
                    Icons.Default.Drafts,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = StudioTeal
                )
                Spacer(Modifier.width(5.dp))
                Text("Save draft", color = StudioTeal, fontSize = 12.sp, maxLines = 1)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onOpenSchedule,
                enabled = canPublish && !publishing,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = StudioViolet.copy(
                    alpha = if (canPublish && !publishing) .18f else .06f
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    StudioViolet.copy(alpha = .38f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = "Schedule post",
                        tint = if (canPublish && !publishing) {
                            Color(0xFFC4B5FD)
                        } else {
                            VybMuted.copy(alpha = .35f)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Button(
                onClick = onPublish,
                enabled = canPublish && !publishing,
                modifier = Modifier.weight(1f).height(48.dp),
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
}

@Composable
private fun LegacyStudioFooter(
    publishing: Boolean,
    canPublish: Boolean,
    onDismiss: () -> Unit,
    onSaveDraft: () -> Unit,
    onOpenSchedule: () -> Unit,
    onPublish: () -> Unit
) {
    HorizontalDivider(color = StudioViolet.copy(alpha = .22f))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0E1A).copy(alpha = .92f))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Up to 6 photos · Text-only posts are fine too",
            modifier = Modifier.fillMaxWidth(),
            color = VybMuted.copy(alpha = .75f),
            fontSize = 11.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !publishing,
                    modifier = Modifier.weight(1f),
                    contentPadding = ButtonDefaults.ContentPadding,
                    shape = RoundedCornerShape(99.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = .1f)
                    )
                ) {
                    Text("Cancel", color = VybMuted, fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onSaveDraft,
                    enabled = canPublish && !publishing,
                    modifier = Modifier.weight(1f),
                    contentPadding = ButtonDefaults.ContentPadding,
                    shape = RoundedCornerShape(99.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        StudioTeal.copy(alpha = .28f)
                    )
                ) {
                    Icon(
                        Icons.Default.Drafts,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = StudioTeal
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("Draft", color = StudioTeal, fontSize = 11.sp)
                }
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onOpenSchedule,
                    enabled = canPublish && !publishing,
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = StudioViolet.copy(
                        alpha = if (canPublish && !publishing) .18f else .06f
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        StudioViolet.copy(alpha = .35f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = "Schedule post",
                            tint = if (canPublish && !publishing) {
                                Color(0xFFC4B5FD)
                            } else {
                                VybMuted.copy(alpha = .35f)
                            },
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                Button(
                    onClick = onPublish,
                    enabled = canPublish && !publishing,
                    modifier = Modifier.weight(1f),
                    contentPadding = ButtonDefaults.ContentPadding,
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioIndigo,
                        disabledContainerColor = StudioIndigo.copy(alpha = .12f),
                        disabledContentColor = StudioViolet.copy(alpha = .45f)
                    )
                ) {
                    if (publishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Publish ✦", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PostUtilityDialog(
    onDismiss: () -> Unit,
    onCancelCreation: () -> Unit,
    onSaveDraft: () -> Unit,
    onSchedule: () -> Unit,
    creationType: String = "Post",
    showDraft: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B1120),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(StudioViolet.copy(alpha = .16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    tint = Color(0xFFC4B5FD)
                )
            }
        },
        title = {
            Text(
                "$creationType utilities",
                color = VybText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                UtilityAction(
                    icon = Icons.Default.Close,
                    title = "Cancel creation",
                    detail = "Discard this editing session",
                    accent = Color(0xFFFCA5A5),
                    onClick = onCancelCreation
                )
                if (showDraft) {
                    UtilityAction(
                        icon = Icons.Default.Drafts,
                        title = "Save draft",
                        detail = "Continue this ${creationType.lowercase()} later",
                        accent = StudioTeal,
                        onClick = onSaveDraft
                    )
                }
                UtilityAction(
                    icon = Icons.Default.Schedule,
                    title = "Schedule ${creationType.lowercase()}",
                    detail = "Choose a future publish time",
                    accent = Color(0xFFC4B5FD),
                    onClick = onSchedule
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Back", color = VybMuted)
            }
        }
    )
}

@Composable
private fun UtilityAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = .035f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = .22f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(accent.copy(alpha = .13f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    title,
                    color = VybText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(detail, color = VybMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
internal fun SchedulePostDialog(
    onDismiss: () -> Unit,
    onSchedule: (Long) -> Unit,
    creationType: String = "Post"
) {
    val options = remember {
        val now = ZonedDateTime.now()
        val inOneHour = now.plusHours(1)
        val todayAtEight = now
            .withHour(20)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        val evening = if (todayAtEight.isAfter(now)) {
            todayAtEight
        } else {
            todayAtEight.plusDays(1)
        }
        listOf(
            ScheduleChoice("In 1 hour", inOneHour),
            ScheduleChoice("Evening", evening),
            ScheduleChoice(
                "Tomorrow morning",
                now.plusDays(1)
                    .withHour(9)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0)
            )
        )
    }
    var selected by remember { mutableStateOf(options.first()) }
    val formatter = remember {
        DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B1120),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(StudioViolet.copy(alpha = .16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFFC4B5FD)
                )
            }
        },
        title = {
            Text(
                "Schedule ${creationType.lowercase()}",
                color = VybText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    "Choose when Vybnet should publish this ${creationType.lowercase()}.",
                    color = VybMuted,
                    fontSize = 13.sp
                )
                options.forEach { option ->
                    val active = selected == option
                    Surface(
                        onClick = { selected = option },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (active) {
                            StudioViolet.copy(alpha = .14f)
                        } else {
                            Color.White.copy(alpha = .035f)
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (active) {
                                StudioViolet.copy(alpha = .4f)
                            } else {
                                Color.White.copy(alpha = .08f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .background(
                                        if (active) StudioTeal else VybMuted.copy(alpha = .25f),
                                        CircleShape
                                    )
                            )
                            Column(Modifier.padding(start = 11.dp)) {
                                Text(
                                    option.label,
                                    color = VybText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    option.time.format(formatter),
                                    color = VybMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                Text(
                    "Scheduling uses this device and waits for an internet connection.",
                    color = VybMuted.copy(alpha = .72f),
                    fontSize = 10.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Back", color = VybMuted)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSchedule(selected.time.toInstant().toEpochMilli())
                },
                colors = ButtonDefaults.buttonColors(containerColor = StudioIndigo),
                shape = RoundedCornerShape(99.dp)
            ) {
                Text("Schedule $creationType", fontWeight = FontWeight.Bold)
            }
        }
    )
}

private data class ScheduleChoice(
    val label: String,
    val time: ZonedDateTime
)

private val StudioBackground = Color(0xFF080D1A)
private val StudioIndigo = Color(0xFF6366F1)
private val StudioViolet = Color(0xFFA855F7)
private val StudioTeal = Color(0xFF22D3C5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyCreatePostComposer(
    state: SocialActionsUiState,
    onPublish: (text: String, isAnonymous: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var anonymous by remember { mutableStateOf(false) }
    var mediaComposerOpen by remember { mutableStateOf(false) }
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Create post", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Share something with your campus") },
            minLines = 4,
            maxLines = 8,
            enabled = !state.creatingPost
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { anonymous = !anonymous },
                enabled = !state.creatingPost,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (anonymous) "Anonymous ✓" else "Post anonymously")
            }
            Button(
                onClick = { onPublish(text, anonymous) },
                enabled = text.isNotBlank() && !state.creatingPost,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.creatingPost) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(18.dp).height(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Publish")
            }
        }
        state.createPostError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        OutlinedButton(
            onClick = { mediaComposerOpen = true },
            enabled = !state.creatingPost,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.PermMedia, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Create with photo or video")
        }
    }
    if (mediaComposerOpen) {
        ModalBottomSheet(onDismissRequest = { mediaComposerOpen = false }) {
            MediaComposerScreen(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(.9f),
                onPublished = { mediaComposerOpen = false }
            )
        }
    }
}

@Composable
fun PostActionsBar(
    postId: String,
    engagement: PostEngagementState,
    commentCount: Int,
    onToggleReaction: () -> Unit,
    onOpenComments: () -> Unit,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleReaction, enabled = !engagement.reactionLoading) {
            if (engagement.reactionLoading) {
                CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    if (engagement.viewerReactionType != null) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like post",
                    tint = if (engagement.viewerReactionType != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
        Text(engagement.reactionCount.toString())
        Spacer(Modifier.width(10.dp))
        IconButton(onClick = onOpenComments) {
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Open comments")
        }
        Text(commentCount.toString())
        Spacer(Modifier.weight(1f))
        if (engagement.savedCount > 0) Text(engagement.savedCount.toString())
        IconButton(onClick = onToggleSave, enabled = !engagement.saveLoading) {
            if (engagement.saveLoading) {
                CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    if (engagement.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save post"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    postId: String,
    thread: CommentThreadState,
    onLoad: () -> Unit,
    onRetry: () -> Unit,
    onAddComment: (text: String, onAdded: () -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(postId) { mutableStateOf("") }
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
                            CommentRow(comment, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            thread.error?.takeIf { thread.items.isNotEmpty() }?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
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
                        onAddComment(text) { text = "" }
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
private fun CommentRow(comment: SocialComment, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 8.dp)) {
        Text(
            comment.author?.displayName
                ?: if (comment.isAnonymous) "Anonymous" else "Vybnet member",
            style = MaterialTheme.typography.labelLarge
        )
        Text(comment.body, style = MaterialTheme.typography.bodyMedium)
    }
}
