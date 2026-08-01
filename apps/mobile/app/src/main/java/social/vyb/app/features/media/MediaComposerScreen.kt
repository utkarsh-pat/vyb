package social.vyb.app.features.media

import android.widget.ImageView
import android.widget.VideoView
import android.net.Uri
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybPanel
import social.vyb.app.ui.VybPanelLifted
import social.vyb.app.ui.VybResponsiveFrame
import social.vyb.app.ui.VybText
import social.vyb.app.features.social.SocialAvatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

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
    onCaptionChanged: (String) -> Unit = {},
    onIntentChanged: (MediaPublishIntent) -> Unit = {},
    onPublishWithoutMedia: (() -> Unit)? = null,
    onScheduleWithoutMedia: ((publishAtMillis: Long, draftId: String?) -> Unit)? = null,
    onCancelCreation: () -> Unit = {},
    onExternalPickerChanged: (Boolean) -> Unit = {},
    onDraftSaved: () -> Unit = {},
    onPublishingStarted: () -> Unit = {},
    onPublished: (CreatedMediaItem) -> Unit = {},
    viewModel: MediaComposerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var storyBuilderMedia by remember { mutableStateOf<SelectedMedia?>(null) }
    var editingOriginalUri by remember { mutableStateOf<Uri?>(null) }
    var draftsOpen by remember { mutableStateOf(false) }
    var editedStoryUri by remember { mutableStateOf<Uri?>(null) }
    var autoLaunchConsumed by remember { mutableStateOf(false) }
    val effectiveAnonymous = isAnonymous && state.intent != MediaPublishIntent.Story
    val hasUnsavedContent = state.caption.isNotBlank() || state.selected.isNotEmpty()

    fun saveCurrentDraft(): Boolean {
        val saved = viewModel.saveDraft(
            isAnonymous = effectiveAnonymous,
            allowAnonymousComments = allowAnonymousComments,
            visibility = visibility,
            communityId = communityId,
            announce = false
        ) != null
        if (saved) onDraftSaved()
        return saved
    }

    fun openSchedulePicker() {
        val initial = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val target = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val delayMillis = target - System.currentTimeMillis()
                        if (delayMillis <= 0L) {
                            android.widget.Toast.makeText(
                                context,
                                "Choose a future time",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            if (state.selected.isEmpty() && onScheduleWithoutMedia != null) {
                                val draftId = viewModel.saveDraft(
                                    isAnonymous = effectiveAnonymous,
                                    allowAnonymousComments = allowAnonymousComments,
                                    visibility = visibility,
                                    communityId = communityId,
                                    scheduledForMillis = target
                                )
                                onScheduleWithoutMedia(target, draftId)
                                viewModel.resetComposer(state.intent)
                                onCancelCreation()
                            } else {
                                viewModel.schedulePublish(
                                    delayMillis = delayMillis,
                                    isAnonymous = effectiveAnonymous,
                                    allowAnonymousComments = allowAnonymousComments,
                                    visibility = visibility,
                                    communityId = communityId
                                )
                            }
                        }
                    },
                    initial.get(Calendar.HOUR_OF_DAY),
                    initial.get(Calendar.MINUTE),
                    false
                ).show()
            },
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH)
        ).apply { datePicker.minDate = System.currentTimeMillis() - 1_000L }.show()
    }
    val multiplePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_POST_MEDIA_ITEMS)
    ) { uris ->
        onExternalPickerChanged(false)
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            viewModel.addSelection(uris)
        }
    }
    val singlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        onExternalPickerChanged(false)
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.addSelection(listOf(it))
        }
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
    BackHandler(enabled = hasUnsavedContent && !state.isPublishing && storyBuilderMedia == null) {
        saveCurrentDraft()
        viewModel.resetComposer(state.intent)
        onCancelCreation()
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    HorizontalDivider(color = VybBorder)
                }
                item {
                    OutlinedTextField(
                        value = state.caption,
                        onValueChange = {
                            viewModel.setCaption(it)
                            onCaptionChanged(it.take(2_000))
                        },
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
                        minLines = 3,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 104.dp),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (state.intent == MediaPublishIntent.Vibe) "Video" else "Media",
                            color = VybText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.weight(1f))
                        if (state.intent == MediaPublishIntent.Post) {
                            Text(
                                "${state.selected.size}/$MAX_POST_MEDIA_ITEMS",
                                color = if (state.selected.isEmpty()) VybMuted else MediaTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
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
                    if (state.intent == MediaPublishIntent.Post) {
                        item {
                            PostMediaCarouselEditor(
                                mediaItems = state.selected,
                                enabled = !state.isPublishing,
                                onEdit = { media ->
                                    editingOriginalUri = media.uri
                                    storyBuilderMedia = media
                                },
                                onRemove = { media -> viewModel.removeSelection(media.uri) },
                                onMove = viewModel::moveSelection
                            )
                        }
                    } else {
                        itemsIndexed(state.selected, key = { _, media -> media.uri.toString() }) { index, media ->
                            MediaPreview(
                                media = media,
                                index = index,
                                itemCount = state.selected.size,
                                removable = !state.isPublishing,
                                onEdit = {
                                    editingOriginalUri = media.uri
                                    storyBuilderMedia = media
                                },
                                onRemove = { viewModel.removeSelection(media.uri) },
                                onMove = { target -> viewModel.moveSelection(index, target) }
                            )
                        }
                    }
                    if (state.intent != MediaPublishIntent.Post || state.selected.size < MAX_POST_MEDIA_ITEMS) item {
                        Surface(
                            onClick = launchPicker,
                            enabled = !state.isPublishing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MediaIndigo.copy(alpha = .14f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MediaTeal.copy(alpha = .26f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (state.intent == MediaPublishIntent.Vibe) Icons.Default.Movie
                                    else Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MediaTeal
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (state.intent == MediaPublishIntent.Post) "Add more media" else "Replace media",
                                        color = VybText,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (state.intent == MediaPublishIntent.Post) "Swipe to preview, hold thumbnails to reorder" else "Choose a new file",
                                        color = VybMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
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
                state.notice?.let { message ->
                    item { Text(message, color = MediaTeal) }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            MediaComposerFooter(
                intent = state.intent,
                publishing = state.isPublishing,
                progress = state.progress,
                progressLabel = state.progressLabel,
                canPublish = state.canPublish || (
                    onPublishWithoutMedia != null && state.intent == MediaPublishIntent.Post &&
                        state.caption.trim().length >= 2
                ),
                drafts = state.drafts,
                onSaveDraft = {
                    if (saveCurrentDraft()) {
                        viewModel.resetComposer(state.intent)
                        onCancelCreation()
                    }
                },
                onOpenDrafts = { draftsOpen = true },
                onSchedule = if (
                    state.canPublish || (
                        onScheduleWithoutMedia != null && state.intent == MediaPublishIntent.Post &&
                            state.caption.trim().length >= 2
                    )
                ) (::openSchedulePicker) else null,
                onPublish = {
                    if (state.selected.isEmpty() && onPublishWithoutMedia != null) {
                        onPublishWithoutMedia()
                        onPublishingStarted()
                    } else {
                        viewModel.publish(
                            isAnonymous = effectiveAnonymous,
                            allowAnonymousComments = allowAnonymousComments,
                            visibility = visibility,
                            communityId = communityId
                        )
                        onPublishingStarted()
                    }
                }
            )
        }
    }

    storyBuilderMedia?.let { selected ->
        StoryBuilderScreen(
            media = selected,
            intent = state.intent,
            onApply = { edited ->
                editedStoryUri = edited.uri
                val original = editingOriginalUri
                if (original != null && state.intent == MediaPublishIntent.Post) {
                    viewModel.updateSelection(original, edited)
                } else {
                    viewModel.replaceSelection(edited)
                }
                editingOriginalUri = null
                storyBuilderMedia = null
            },
            onDismiss = {
                editedStoryUri = selected.uri
                editingOriginalUri = null
                storyBuilderMedia = null
            }
        )
    }
    if (draftsOpen) {
        AlertDialog(
            onDismissRequest = { draftsOpen = false },
            title = { Text("Drafts (${state.drafts.size})") },
            text = {
                if (state.drafts.isEmpty()) {
                    Text("No drafts saved on this device.", color = VybMuted)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.drafts, key = MediaDraftSummary::id) { draft ->
                            Surface(
                                onClick = {
                                                viewModel.loadDraft(draft.id)
                                                onIntentChanged(draft.intent)
                                                onCaptionChanged(draft.caption)
                                                draftsOpen = false
                                },
                                color = VybPanelLifted,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            draft.caption.ifBlank {
                                                "${draft.intent.name} with ${draft.mediaCount} media"
                                            },
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            buildString {
                                                append(DateFormat.getDateTimeInstance(
                                                    DateFormat.SHORT,
                                                    DateFormat.SHORT
                                                ).format(Date(draft.savedAtMillis)))
                                                draft.scheduledForMillis?.let {
                                                    append(" · scheduled ")
                                                    append(DateFormat.getDateTimeInstance(
                                                        DateFormat.SHORT,
                                                        DateFormat.SHORT
                                                    ).format(Date(it)))
                                                }
                                            },
                                            color = VybMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(onClick = { viewModel.discardDraft(draft.id) }) {
                                        Icon(Icons.Default.DeleteOutline, "Discard draft", tint = VybMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { draftsOpen = false }) { Text("Done") } }
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
    val authorName = displayName.ifBlank { "Vyb member" }
    val shownName = if (anonymous) "Anonymous Vyber" else authorName
    val shownUsername = if (anonymous) "@anonymous" else "@${username.ifBlank { "member" }}"
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SocialAvatar(
            avatarUrl = null,
            displayName = shownName,
            size = 44.dp,
            contentDescription = "$shownName avatar"
        )
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
        modifier = Modifier.fillMaxWidth().height(154.dp),
        color = VybPanelLifted.copy(alpha = .72f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MediaTeal.copy(alpha = .22f)),
        shape = RoundedCornerShape(22.dp)
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
                if (intent == MediaPublishIntent.Vibe) "Choose a video" else "Add photos or videos",
                color = VybText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (intent == MediaPublishIntent.Post) "Select up to $MAX_POST_MEDIA_ITEMS items" else "Tap to browse your device",
                color = VybMuted,
                fontSize = 11.sp
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
    drafts: List<MediaDraftSummary>,
    onSaveDraft: () -> Unit,
    onOpenDrafts: () -> Unit,
    onSchedule: (() -> Unit)?,
    onPublish: () -> Unit
) {
    val enabled = canPublish && !publishing
    var menuOpen by remember { mutableStateOf(false) }
    HorizontalDivider(color = MediaViolet.copy(alpha = .22f))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VybPanel.copy(alpha = .96f))
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
                MediaPublishIntent.Post -> "Choose up to $MAX_POST_MEDIA_ITEMS photos or videos"
            },
            color = VybMuted.copy(alpha = .75f),
            fontSize = 11.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(onClick = { menuOpen = true }, enabled = !publishing) {
                    Box {
                        Icon(Icons.Default.Menu, "Draft and schedule options", tint = VybText)
                        if (drafts.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).size(16.dp),
                                color = MediaTeal,
                                shape = CircleShape
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        drafts.size.coerceAtMost(9).toString(),
                                        color = Color.Black,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Save as draft") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { menuOpen = false; onSaveDraft() }
                    )
                    DropdownMenuItem(
                        text = { Text("Drafts (${drafts.size})") },
                        leadingIcon = { Icon(Icons.Default.DragHandle, null) },
                        onClick = { menuOpen = false; onOpenDrafts() }
                    )
                    onSchedule?.let { schedule ->
                        DropdownMenuItem(
                            text = { Text("Schedule date & time") },
                            leadingIcon = { Icon(Icons.Default.Schedule, null) },
                            onClick = { menuOpen = false; schedule() }
                        )
                    }
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
                    else if (intent == MediaPublishIntent.Post) "Share post"
                    else "Share ${intent.name.lowercase()}",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PostMediaCarouselEditor(
    mediaItems: List<SelectedMedia>,
    enabled: Boolean,
    onEdit: (SelectedMedia) -> Unit,
    onRemove: (SelectedMedia) -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { mediaItems.size })
    val scope = rememberCoroutineScope()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { page -> mediaItems[page].uri.toString() }
                ) { page ->
                    MediaPreviewContent(mediaItems[page], Modifier.fillMaxSize())
                }
                mediaItems.getOrNull(pagerState.currentPage)?.let { active ->
                    IconButton(
                        onClick = { onEdit(active) },
                        enabled = enabled,
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                            .background(Color.Black.copy(alpha = .58f), CircleShape)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit ${active.fileName}", tint = Color.White)
                    }
                    IconButton(
                        onClick = { onRemove(active) },
                        enabled = enabled,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                            .background(Color.Black.copy(alpha = .58f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove ${active.fileName}", tint = Color.White)
                    }
                }
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                    color = Color.Black.copy(alpha = .64f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        "${pagerState.currentPage + 1}/${mediaItems.size}",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Text(
                "Swipe the preview • drag a thumbnail to reorder",
                modifier = Modifier.padding(horizontal = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = VybMuted
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(mediaItems, key = { _, media -> media.uri.toString() }) { index, media ->
                    var horizontalDrag by remember(media.uri) { mutableStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                if (index == pagerState.currentPage) 2.dp else 1.dp,
                                if (index == pagerState.currentPage) MediaIndigo else VybBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = enabled) {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                            .pointerInput(media.uri, index, mediaItems.size, enabled) {
                                if (!enabled) return@pointerInput
                                detectHorizontalDragGestures(
                                    onDragStart = { horizontalDrag = 0f },
                                    onDragCancel = { horizontalDrag = 0f },
                                    onDragEnd = {
                                        val slots = (horizontalDrag / 68.dp.toPx()).roundToInt()
                                        val target = (index + slots).coerceIn(0, mediaItems.lastIndex)
                                        if (target != index) onMove(index, target)
                                        horizontalDrag = 0f
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    horizontalDrag += dragAmount
                                }
                            }
                    ) {
                        MediaPreviewContent(media, Modifier.fillMaxSize())
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp),
                            color = Color.Black.copy(alpha = .62f),
                            shape = CircleShape
                        ) {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = "Drag ${media.fileName} to reorder",
                                tint = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp).size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaPreviewContent(media: SelectedMedia, modifier: Modifier = Modifier) {
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
            modifier = modifier
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
            modifier = modifier
        )
    }
}

@Composable
private fun MediaPreview(
    media: SelectedMedia,
    index: Int,
    itemCount: Int,
    removable: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit
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
                    onClick = onEdit,
                    enabled = removable,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = .55f), CircleShape)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${media.fileName}", tint = Color.White)
                }
                IconButton(
                    onClick = onRemove,
                    enabled = removable,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = .55f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove ${media.fileName}", tint = Color.White)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Hold and drag to reorder ${media.fileName}",
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp)
                        .pointerInput(media.uri, index, itemCount) {
                            var verticalDrag = 0f
                            detectDragGesturesAfterLongPress(
                                onDragStart = { verticalDrag = 0f },
                                onDragCancel = { verticalDrag = 0f },
                                onDragEnd = {
                                    when {
                                        verticalDrag < -36f && index > 0 -> onMove(index - 1)
                                        verticalDrag > 36f && index < itemCount - 1 -> onMove(index + 1)
                                    }
                                    verticalDrag = 0f
                                }
                            ) { change, drag ->
                                change.consume()
                                verticalDrag += drag.y
                            }
                        }
                )
                Text(
                    "${index + 1}. ${media.fileName} • ${formatMediaBytes(media.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
                IconButton(onClick = { onMove(index - 1) }, enabled = removable && index > 0) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move media up")
                }
                IconButton(onClick = { onMove(index + 1) }, enabled = removable && index < itemCount - 1) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move media down")
                }
            }
        }
    }
}

private fun formatMediaBytes(bytes: Long): String =
    if (bytes >= 1024 * 1024) "%.1f MB".format(bytes / (1024f * 1024f))
    else "%.0f KB".format(bytes / 1024f)

private val MediaIndigo = Color(0xFF6366F1)
private val MediaViolet = Color(0xFFA855F7)
private val MediaTeal = Color(0xFF22D3C5)
