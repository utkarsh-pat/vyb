package social.vyb.app.features.media

import android.widget.ImageView
import android.widget.VideoView
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import social.vyb.app.features.social.PostUtilityDialog
import social.vyb.app.features.social.SchedulePostDialog
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybResponsiveFrame
import social.vyb.app.ui.VybText

@Composable
fun MediaComposerScreen(
    modifier: Modifier = Modifier,
    initialIntent: MediaPublishIntent = MediaPublishIntent.Post,
    initialCaption: String = "",
    autoLaunchPicker: Boolean = false,
    showIntentPicker: Boolean = true,
    displayName: String = "",
    username: String = "",
    isAnonymous: Boolean = false,
    allowAnonymousComments: Boolean = true,
    visibility: String = "public",
    communityId: String? = null,
    onCancelCreation: () -> Unit = {},
    onExternalPickerChanged: (Boolean) -> Unit = {},
    onScheduled: () -> Unit = {},
    onPublished: (CreatedMediaItem) -> Unit = {},
    viewModel: MediaComposerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var utilityDialogOpen by remember { mutableStateOf(false) }
    var scheduleDialogOpen by remember { mutableStateOf(false) }
    var storyBuilderMedia by remember { mutableStateOf<SelectedMedia?>(null) }
    var editedStoryUri by remember { mutableStateOf<Uri?>(null) }
    var autoLaunchConsumed by remember { mutableStateOf(false) }
    val effectiveAnonymous = isAnonymous && state.intent != MediaPublishIntent.Story
    val multiplePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris ->
        onExternalPickerChanged(false)
        if (uris.isNotEmpty()) viewModel.addSelection(uris)
    }
    val singlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        onExternalPickerChanged(false)
        uri?.let { viewModel.addSelection(listOf(it)) }
    }
    val launchPicker = {
        val request = PickVisualMediaRequest(
            if (state.intent == MediaPublishIntent.Vibe) {
                ActivityResultContracts.PickVisualMedia.VideoOnly
            } else {
                ActivityResultContracts.PickVisualMedia.ImageAndVideo
            }
        )
        onExternalPickerChanged(true)
        runCatching {
            if (state.intent == MediaPublishIntent.Post) {
                multiplePicker.launch(request)
            } else {
                singlePicker.launch(request)
            }
        }.onFailure {
            onExternalPickerChanged(false)
        }
        Unit
    }

    LaunchedEffect(initialIntent) {
        viewModel.setIntent(initialIntent)
    }
    LaunchedEffect(initialCaption) {
        if (initialCaption.isNotBlank() && state.caption.isBlank()) {
            viewModel.setCaption(initialCaption)
        }
    }
    LaunchedEffect(autoLaunchPicker, state.intent) {
        if (autoLaunchPicker && !autoLaunchConsumed && state.selected.isEmpty()) {
            autoLaunchConsumed = true
            launchPicker()
        }
    }
    LaunchedEffect(state.publishedItem) {
        state.publishedItem?.let(onPublished)
    }
    LaunchedEffect(state.scheduled) {
        if (state.scheduled) onScheduled()
    }
    LaunchedEffect(state.intent, state.selected) {
        val storyMedia = state.selected.singleOrNull()
        if (
            state.intent == MediaPublishIntent.Story &&
            storyMedia != null &&
            storyMedia.uri != editedStoryUri
        ) {
            storyBuilderMedia = storyMedia
        }
    }

    VybResponsiveFrame(modifier.fillMaxSize()) { layout ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = layout.horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showIntentPicker) {
                    item {
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MediaPublishIntent.entries.forEach { intent ->
                                FilterChip(
                                    selected = state.intent == intent,
                                    onClick = {
                                        if (!state.isPublishing) viewModel.setIntent(intent)
                                    },
                                    label = { Text(intent.name) }
                                )
                            }
                        }
                    }
                }
                item {
                    MediaPublisherRow(
                        displayName = displayName,
                        username = username,
                        anonymous = effectiveAnonymous,
                        modifier = Modifier.padding(top = if (showIntentPicker) 0.dp else 14.dp)
                    )
                }
                item {
                    HorizontalDivider(color = Color.White.copy(alpha = .07f))
                }
                item {
                    OutlinedTextField(
                        value = state.caption,
                        onValueChange = viewModel::setCaption,
                        placeholder = {
                            Text(
                                if (state.intent == MediaPublishIntent.Story) {
                                    "Add a caption to your story…"
                                } else {
                                    "What's on your mind? #hashtag @mention"
                                },
                                color = VybMuted.copy(alpha = .55f)
                            )
                        },
                        enabled = !state.isPublishing,
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 148.dp)
                    )
                }
                item {
                    HorizontalDivider(color = Color.White.copy(alpha = .07f))
                }
                item {
                    Text(
                        if (state.intent == MediaPublishIntent.Vibe) "Video" else "Media",
                        color = VybText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
                if (state.selected.isEmpty()) {
                    item {
                        MediaPickerTile(
                            intent = state.intent,
                            enabled = !state.isPublishing,
                            onClick = launchPicker
                        )
                    }
                } else {
                    items(state.selected, key = { it.uri.toString() }) { media ->
                        MediaPreview(media, !state.isPublishing) {
                            viewModel.removeSelection(media.uri)
                        }
                    }
                    item {
                        Button(
                            onClick = launchPicker,
                            enabled = !state.isPublishing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                if (state.intent == MediaPublishIntent.Vibe) {
                                    Icons.Default.Movie
                                } else {
                                    Icons.Default.AddPhotoAlternate
                                },
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Change media")
                        }
                    }
                    if (state.intent == MediaPublishIntent.Story) {
                        item {
                            Button(
                                onClick = { storyBuilderMedia = state.selected.singleOrNull() },
                                enabled = !state.isPublishing,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Edit story")
                            }
                        }
                    }
                }
                if (state.intent != MediaPublishIntent.Story) {
                    item {
                        OutlinedTextField(
                            value = state.location,
                            onValueChange = viewModel::setLocation,
                            label = { Text("Location (optional)") },
                            enabled = !state.isPublishing,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                state.progressLabel?.let { progressLabel ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                progressLabel,
                                color = VybMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                state.error?.let { message ->
                    item {
                        Text(message, color = MaterialTheme.colorScheme.error)
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            MediaComposerFooter(
                intent = state.intent,
                publishing = state.isPublishing,
                progress = state.progress,
                progressLabel = state.progressLabel,
                canPublish = state.canPublish,
                onOpenUtilities = { utilityDialogOpen = true },
                onPublish = {
                    viewModel.publish(
                        isAnonymous = effectiveAnonymous,
                        allowAnonymousComments = allowAnonymousComments,
                        visibility = visibility,
                        communityId = communityId
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
                onCancelCreation()
            },
            onSaveDraft = {},
            onSchedule = {
                utilityDialogOpen = false
                scheduleDialogOpen = true
            },
            creationType = state.intent.name,
            showDraft = false
        )
    }
    if (scheduleDialogOpen) {
        SchedulePostDialog(
            onDismiss = { scheduleDialogOpen = false },
            onSchedule = { publishAtMillis ->
                scheduleDialogOpen = false
                viewModel.schedule(
                    publishAtMillis = publishAtMillis,
                    isAnonymous = effectiveAnonymous,
                    allowAnonymousComments = allowAnonymousComments,
                    visibility = visibility,
                    communityId = communityId
                )
            },
            creationType = state.intent.name
        )
    }
    storyBuilderMedia?.let { selected ->
        StoryBuilderScreen(
            media = selected,
            onApply = { edited ->
                editedStoryUri = edited.uri
                viewModel.replaceSelection(edited)
                storyBuilderMedia = null
            },
            onDismiss = {
                editedStoryUri = selected.uri
                storyBuilderMedia = null
            }
        )
    }
}

@Composable
private fun MediaPublisherRow(
    displayName: String,
    username: String,
    anonymous: Boolean,
    modifier: Modifier = Modifier
) {
    val authorName = displayName.ifBlank { "Vybnet member" }
    val shownName = if (anonymous) "Anonymous Vyber" else authorName
    val shownUsername = if (anonymous) "@anonymous" else "@${username.ifBlank { "member" }}"
    val initials = authorName
        .split(" ")
        .filter(String::isNotBlank)
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { "V" }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.linearGradient(listOf(MediaIndigo, MediaViolet)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (anonymous) "A" else initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                shownName,
                color = VybText,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                shownUsername,
                color = VybMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MediaPickerTile(
    intent: MediaPublishIntent,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width = 132.dp, height = 112.dp),
        color = Color.White.copy(alpha = .04f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = .16f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                if (intent == MediaPublishIntent.Vibe) {
                    Icons.Default.Movie
                } else {
                    Icons.Default.AddPhotoAlternate
                },
                contentDescription = null,
                tint = MediaTeal,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (intent == MediaPublishIntent.Vibe) "Add video" else "Add media",
                color = VybMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MediaComposerFooter(
    intent: MediaPublishIntent,
    publishing: Boolean,
    progress: Float,
    progressLabel: String?,
    canPublish: Boolean,
    onOpenUtilities: () -> Unit,
    onPublish: () -> Unit
) {
    val enabled = canPublish && !publishing
    HorizontalDivider(color = MediaViolet.copy(alpha = .22f))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0E1A).copy(alpha = .96f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (publishing) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MediaTeal,
                trackColor = MediaViolet.copy(alpha = .16f)
            )
            Text(
                progressLabel ?: "Publishing ${intent.name.lowercase()}…",
                color = MediaTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            when (intent) {
                MediaPublishIntent.Story -> "Choose one photo or video for your story"
                MediaPublishIntent.Vibe -> "Choose one video for your Vibe"
                MediaPublishIntent.Post -> "Choose up to 4 photos or videos"
            },
            color = VybMuted.copy(alpha = .75f),
            fontSize = 11.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onOpenUtilities,
                enabled = enabled,
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = MediaViolet.copy(alpha = if (enabled) .16f else .06f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MediaViolet.copy(alpha = if (enabled) .38f else .14f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "${intent.name} utilities",
                        tint = if (enabled) Color(0xFFC4B5FD)
                        else VybMuted.copy(alpha = .35f),
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
            Button(
                onClick = onPublish,
                enabled = enabled,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediaIndigo,
                    disabledContainerColor = MediaIndigo.copy(alpha = .12f),
                    disabledContentColor = MediaViolet.copy(alpha = .45f)
                )
            ) {
                if (publishing) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (publishing) "Publishing…"
                    else "Publish ${intent.name} ✦",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MediaPreview(
    media: SelectedMedia,
    removable: Boolean,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (media.mediaType == "video") {
                    AndroidView(
                        factory = { context ->
                            VideoView(context).apply {
                                tag = media.uri
                                setVideoURI(media.uri)
                                setOnPreparedListener { player ->
                                    player.isLooping = true
                                    player.setVolume(0f, 0f)
                                    start()
                                }
                            }
                        },
                        update = {
                            if (it.tag != media.uri) {
                                it.tag = media.uri
                                it.setVideoURI(media.uri)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AndroidView(
                        factory = { context ->
                            ImageView(context).apply {
                                tag = media.uri
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                setImageURI(media.uri)
                            }
                        },
                        update = {
                            if (it.tag != media.uri) {
                                it.tag = media.uri
                                it.setImageURI(media.uri)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                IconButton(
                    onClick = onRemove,
                    enabled = removable,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove ${media.fileName}")
                }
            }
            Text(
                "${media.fileName} • ${formatMediaBytes(media.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

private fun formatMediaBytes(bytes: Long): String =
    if (bytes >= 1024 * 1024) "%.1f MB".format(bytes / (1024f * 1024f))
    else "%.0f KB".format(bytes / 1024f)

private val MediaIndigo = Color(0xFF6366F1)
private val MediaViolet = Color(0xFFA855F7)
private val MediaTeal = Color(0xFF22D3C5)
