package social.vyb.app.features.messages

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybIndigo
import social.vyb.app.features.media.MediaPublishIntent
import social.vyb.app.features.media.SelectedMedia
import social.vyb.app.features.media.StoryBuilderScreen
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanelLifted
import social.vyb.app.ui.VybPurple
import social.vyb.app.ui.VybTeal
import social.vyb.app.ui.VybText
import social.vyb.app.ui.VybEmptyState
import social.vyb.app.ui.VybLoadingMark
import social.vyb.app.ui.VybResponsiveFrame
import social.vyb.app.ui.VybConnectedTab
import social.vyb.app.ui.VybConnectedTabSelector
import social.vyb.app.features.social.SocialAvatar

@Composable
fun MessagesFeatureScreen(
    modifier: Modifier = Modifier,
    initialConversationId: String? = null,
    onInitialConversationConsumed: (() -> Unit)? = null,
    communitySlug: String? = null,
    onOpenCommunity: ((String) -> Unit)? = null,
    onCloseCommunity: (() -> Unit)? = null
) {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { ChatRepository(context) }
    var selectedConversationId by rememberSaveable { mutableStateOf(initialConversationId) }
    var selectedCommunitySlug by rememberSaveable { mutableStateOf<String?>(null) }
    var communityMode by rememberSaveable { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(repository, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                val startedAt = android.os.SystemClock.elapsedRealtime()
                runCatching { repository.heartbeatPresence() }
                    .onSuccess { ChatTelemetry.presenceHeartbeat(startedAt, it.lastActiveAt) }
                    .onFailure { ChatTelemetry.presenceHeartbeatFailed(startedAt) }
                delay(30_000)
            }
        }
    }

    LaunchedEffect(initialConversationId) {
        val requested = initialConversationId?.trim()?.takeIf(String::isNotEmpty) ?: return@LaunchedEffect
        selectedConversationId = requested
        communityMode = false
        onInitialConversationConsumed?.invoke()
    }

    val activeCommunitySlug = communitySlug ?: selectedCommunitySlug
    if (activeCommunitySlug != null) {
        val communityViewModel: CommunityConversationViewModel = viewModel(
            key = "community-$activeCommunitySlug",
            factory = MessagesViewModelFactory(
                repository = repository,
                communitySlug = activeCommunitySlug
            )
        )
        val state by communityViewModel.state.collectAsStateWithLifecycle()
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(communityViewModel, lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    delay(15_000)
                    communityViewModel.refresh(silent = true)
                }
            }
        }
        CommunityConversationContent(
            state = state,
            onBack = {
                if (communitySlug != null) onCloseCommunity?.invoke()
                else selectedCommunitySlug = null
            },
            onRetry = { communityViewModel.refresh() },
            onDraftChange = communityViewModel::updateDraft,
            onSend = communityViewModel::send,
            modifier = modifier
        )
        return
    }

    val conversationId = selectedConversationId
    if (conversationId == null) {
        val inboxViewModel: MessagesInboxViewModel = viewModel(
            factory = MessagesViewModelFactory(repository)
        )
        val state by inboxViewModel.state.collectAsStateWithLifecycle()
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(inboxViewModel, lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) inboxViewModel.refresh(silent = true)
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        InboxContent(
            state = state,
            onQueryChange = inboxViewModel::updateQuery,
            onRetry = { inboxViewModel.refresh() },
            onOpen = { selectedConversationId = it },
            communityMode = communityMode,
            onCommunityModeChange = { communityMode = it },
            onOpenCommunity = { slug ->
                if (onOpenCommunity != null) onOpenCommunity(slug)
                else selectedCommunitySlug = slug
            },
            modifier = modifier
        )
    } else {
        val conversationViewModel: ConversationViewModel = viewModel(
            key = "conversation-$conversationId",
            factory = MessagesViewModelFactory(repository, conversationId)
        )
        val state by conversationViewModel.state.collectAsStateWithLifecycle()
        ConversationContent(
            state = state,
            onBack = { selectedConversationId = null },
            onRetry = conversationViewModel::refresh,
            onDraftChange = conversationViewModel::updateDraft,
            onDurationChange = conversationViewModel::updateDurationKey,
            onSend = conversationViewModel::send,
            onToggleViewOnce = conversationViewModel::toggleViewOnce,
            onSendMedia = conversationViewModel::sendMedia,
            onSendMediaBatch = conversationViewModel::sendMediaBatch,
            onOpenMedia = conversationViewModel::openMedia,
            onCloseViewOnce = conversationViewModel::closeViewOnce,
            modifier = modifier
        )
    }
}

@Composable
private fun InboxContent(
    state: InboxUiState,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onOpen: (String) -> Unit,
    communityMode: Boolean,
    onCommunityModeChange: (Boolean) -> Unit,
    onOpenCommunity: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    VybResponsiveFrame(modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().pointerInput(communityMode) {
            var drag = 0f
            detectHorizontalDragGestures(
                onDragStart = { drag = 0f },
                onDragCancel = { drag = 0f },
                onDragEnd = {
                    if (drag < -90f && !communityMode) onCommunityModeChange(true)
                    if (drag > 90f && communityMode) onCommunityModeChange(false)
                    drag = 0f
                },
                onHorizontalDrag = { change, amount ->
                    drag += amount
                    if (kotlin.math.abs(drag) > 12f) change.consume()
                }
            )
        }
    ) {
        VybConnectedTabSelector(
            tabs = listOf(
                VybConnectedTab(
                    label = "Chats",
                    badgeCount = state.items.sumOf(ChatInboxItem::unreadCount)
                ),
                VybConnectedTab("Community")
            ),
            selectedIndex = if (communityMode) 1 else 0,
            onSelected = { onCommunityModeChange(it == 1) },
            modifier = Modifier.padding(bottom = 18.dp)
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = {
                Text(if (communityMode) "Search your communities" else "Search conversations")
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            ),
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VybPanelLifted,
                unfocusedContainerColor = VybPanelLifted,
                focusedBorderColor = VybIndigo,
                unfocusedBorderColor = VybBorder,
                focusedTextColor = VybText,
                unfocusedTextColor = VybText,
                focusedPlaceholderColor = VybMuted,
                unfocusedPlaceholderColor = VybMuted
            )
        )
        val visibleError = if (communityMode) state.communityError else state.error
        val visibleEmpty = if (communityMode) {
            state.filteredCommunities.isEmpty()
        } else {
            state.filteredItems.isEmpty()
        }
        when {
            state.isLoading -> CenterStatus { VybLoadingMark(width = 96.dp) }
            visibleError != null -> ErrorStatus(visibleError, onRetry)
            visibleEmpty -> Box(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                VybEmptyState(
                    icon = if (communityMode) Icons.Default.Groups else Icons.Default.ChatBubbleOutline,
                    title = if (state.query.isBlank()) {
                        if (communityMode) "No communities yet" else "No conversations yet"
                    } else {
                        "No matches found"
                    },
                    body = if (state.query.isBlank()) {
                        if (communityMode) {
                            "Your verified campus circles will appear here."
                        } else {
                            "Your campus conversations will stay organized here."
                        }
                    } else {
                        "Try another name or clear the search."
                    }
                )
            }
            communityMode -> LazyColumn(Modifier.padding(top = 8.dp)) {
                item {
                    Surface(
                        color = VybPanelLifted,
                        border = androidx.compose.foundation.BorderStroke(1.dp, VybBorder),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "YOUR CAMPUS",
                                color = VybMuted,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "Your verified campus",
                                color = VybText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                            Text(
                                "${state.filteredCommunities.size} official circles",
                                color = VybMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                items(state.filteredCommunities, key = CommunityInboxItem::id) { item ->
                    CommunityInboxRow(item = item, onClick = { onOpenCommunity(item.slug) })
                }
            }
            else -> LazyColumn(Modifier.padding(top = 8.dp)) {
                items(state.filteredItems, key = ChatInboxItem::id) { item ->
                    InboxRow(item = item, onClick = { onOpen(item.id) })
                }
            }
        }
    }
    }
}

@Composable
private fun CommunityInboxRow(item: CommunityInboxItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape)
                .background(if (item.isOfficial) VybIndigo else VybPanelLifted),
            contentAlignment = Alignment.Center
        ) {
            Text(item.name.take(1).uppercase(), color = VybText, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.padding(start = 13.dp).weight(1f)) {
            Text(item.name, color = VybText, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                "${item.type.replaceFirstChar(Char::uppercase)} · ${item.memberCount} members",
                color = VybMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            item.membershipRole ?: "member",
            color = VybMuted,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun InboxRow(item: ChatInboxItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        color = VybPanelLifted.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, VybBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            SocialAvatar(
                avatarUrl = item.avatarUrl,
                displayName = item.peerName,
                size = 52.dp
            )
            if (item.isOnline) {
                Box(
                    Modifier.align(Alignment.BottomEnd).size(13.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Column(Modifier.padding(start = 13.dp).weight(1f)) {
            Text(item.peerName, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                item.preview,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(item.timestamp, style = MaterialTheme.typography.labelMedium)
            if (item.unreadCount > 0) {
                Badge(Modifier.padding(top = 5.dp)) { Text(item.unreadCount.toString()) }
            }
        }
    }
    }
}

@Composable
private fun CommunityConversationContent(
    state: CommunityConversationUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    VybResponsiveFrame(modifier.fillMaxSize(), maxContentWidth = 900.dp) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Surface(tonalElevation = 2.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.context?.name ?: "Community",
                            fontWeight = FontWeight.Bold,
                            color = VybText
                        )
                        Text(
                            state.context?.let {
                                "${it.memberCount} members · ${it.type.replaceFirstChar(Char::uppercase)}"
                            } ?: "Campus conversation",
                            style = MaterialTheme.typography.bodySmall,
                            color = VybMuted
                        )
                    }
                    IconButton(onClick = onRetry, enabled = !state.isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
            when {
                state.isLoading -> CenterStatus { VybLoadingMark(width = 96.dp) }
                state.error != null && state.context == null ->
                    ErrorStatus(state.error, onRetry)
                else -> {
                    val listState = rememberLazyListState()
                    LaunchedEffect(state.messages.size) {
                        if (state.messages.isNotEmpty()) {
                            listState.animateScrollToItem(state.messages.lastIndex)
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Spacer(Modifier.size(4.dp)) }
                        if (state.messages.isEmpty()) {
                            item {
                                VybEmptyState(
                                    icon = Icons.Default.Groups,
                                    title = "No messages yet",
                                    body = "Start the first community message.",
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        } else {
                            items(state.messages, key = CommunityMessageItem::id) {
                                CommunityMessageBubble(it)
                            }
                        }
                        item { Spacer(Modifier.size(4.dp)) }
                    }
                    state.error?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.draft,
                            onValueChange = onDraftChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Message your community") },
                            maxLines = 4,
                            shape = RoundedCornerShape(22.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = onSend,
                            enabled = state.context != null &&
                                state.draft.isNotBlank() &&
                                !state.isSending
                        ) {
                            if (state.isSending) {
                                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityMessageBubble(message: CommunityMessageItem) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.isMine) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                VybPanelLifted
            },
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(horizontal = 13.dp, vertical = 9.dp)) {
                if (!message.isMine) {
                    Text(
                        message.authorName,
                        color = VybIndigo,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(message.body, color = VybText)
                Text(
                    buildString {
                        append(message.timestamp)
                        if (message.reactionCount > 0) append(" · ${message.reactionCount} reactions")
                        if (message.replyCount > 0) append(" · ${message.replyCount} replies")
                    },
                    modifier = Modifier.align(Alignment.End).padding(top = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = VybMuted
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationContent(
    state: ConversationUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDraftChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleViewOnce: () -> Unit,
    onSendMedia: (Uri, String, String, Int?, Int?, Int?) -> Unit,
    onSendMediaBatch: (List<ChatMediaDraft>) -> Unit,
    onOpenMedia: (ChatMessageItem) -> Unit,
    onCloseViewOnce: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordingSeconds by rememberSaveable { mutableStateOf(0) }
    var recordingStartedAt by remember { mutableStateOf(0L) }
    var isRecording by rememberSaveable { mutableStateOf(false) }
    var pendingMedia by remember { mutableStateOf<List<ChatMediaDraft>>(emptyList()) }
    var editingMedia by remember { mutableStateOf<ChatMediaDraft?>(null) }
    var cameraCaptureFile by remember { mutableStateOf<File?>(null) }

    fun beginVoiceRecording() {
        val file = File(context.cacheDir, "voice-note-${System.currentTimeMillis()}.m4a")
        val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        runCatching {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(96_000)
            recorder.setAudioSamplingRate(44_100)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
        }.onSuccess {
            mediaRecorder = recorder
            recordingFile = file
            recordingSeconds = 0
            recordingStartedAt = android.os.SystemClock.elapsedRealtime()
            isRecording = true
        }.onFailure {
            recorder.release()
            file.delete()
        }
    }

    fun finishVoiceRecording(send: Boolean) {
        val completedFile = recordingFile
        val durationMs = (android.os.SystemClock.elapsedRealtime() - recordingStartedAt)
            .coerceAtLeast(recordingSeconds * 1_000L)
            .toInt()
        runCatching { mediaRecorder?.stop() }
        mediaRecorder?.release()
        mediaRecorder = null
        recordingFile = null
        recordingStartedAt = 0L
        isRecording = false
        recordingSeconds = 0
        if (send && completedFile?.isFile == true && completedFile.length() > 0 && durationMs >= 350) {
            onSendMedia(Uri.fromFile(completedFile), completedFile.name, "audio/mp4", null, null, durationMs)
        } else {
            completedFile?.delete()
        }
    }

    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val available = (8 - pendingMedia.size).coerceAtLeast(0)
        val additions = uris.take(available).map { uri ->
            val metadata = readPickedMediaMetadata(context, uri)
            ChatMediaDraft(uri, metadata.fileName, metadata.mimeType, metadata.width, metadata.height, metadata.durationMs)
        }
        pendingMedia = (pendingMedia + additions).take(8)
    }
    val cameraCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val file = cameraCaptureFile
        if (captured && file?.isFile == true && file.length() > 0) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val metadata = readPickedMediaMetadata(context, uri)
            val draft = ChatMediaDraft(uri, metadata.fileName, "image/jpeg", metadata.width, metadata.height, null)
            pendingMedia = (pendingMedia + draft).take(8)
            editingMedia = draft
        } else {
            file?.delete()
        }
        cameraCaptureFile = null
    }
    fun launchCamera() {
        val directory = File(context.cacheDir, "chat-camera").apply { mkdirs() }
        val file = File(directory, "chat-${System.currentTimeMillis()}.jpg")
        cameraCaptureFile = file
        cameraCapture.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }
    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1_000)
            recordingSeconds += 1
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { mediaRecorder?.stop() }
            mediaRecorder?.release()
            if (isRecording) recordingFile?.delete()
        }
    }
    VybResponsiveFrame(modifier.fillMaxSize(), maxContentWidth = 900.dp) {
    Column(Modifier.fillMaxSize().imePadding()) {
        Surface(
            color = VybPanelLifted.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, VybBorder)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                SocialAvatar(
                    avatarUrl = state.peerAvatarUrl,
                    displayName = state.peerName.ifBlank { "Conversation" },
                    size = 42.dp
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        state.peerName.ifBlank { "Conversation" },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                    Row(
                        modifier = Modifier.padding(start = 10.dp, top = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = VybTeal
                        )
                        Text(
                            when {
                                state.isPeerTyping -> " Typing..."
                                state.isOnline -> " Secure · Online"
                                else -> " Secure · ${state.peerHandle}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isOnline || state.isPeerTyping) VybTeal else VybMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box {
                    IconButton(onClick = { settingsOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Conversation options")
                    }
                }
            }
        }
        when {
            state.isLoading -> CenterStatus { VybLoadingMark(width = 96.dp) }
            state.error != null && state.messages.isEmpty() -> ErrorStatus(state.error, onRetry)
            else -> {
                val listState = rememberLazyListState()
                LaunchedEffect(state.messages.size, state.isPeerTyping) {
                    val targetIndex = state.messages.size + if (state.isPeerTyping) 1 else 0
                    if (targetIndex > 0) listState.animateScrollToItem(targetIndex)
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    item {
                        Surface(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            color = VybTeal.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, VybTeal.copy(alpha = 0.28f)),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, null, Modifier.size(13.dp), tint = VybTeal)
                                Text(
                                    " End-to-end encrypted chat",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VybMuted
                                )
                            }
                        }
                    }
                    items(state.messages, key = ChatMessageItem::id) { message ->
                        MessageBubble(message, onOpenMedia)
                    }
                    if (state.isPeerTyping) {
                        item(key = "peer-typing") { PeerTypingBubble() }
                    }
                    item { Spacer(Modifier.size(4.dp)) }
                }
                state.error?.let {
                    Text(
                        it,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    color = VybPanelLifted.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, VybBorder),
                    shape = RoundedCornerShape(28.dp)
                ) {
                Column(Modifier.fillMaxWidth()) {
                if (pendingMedia.isNotEmpty()) {
                    ChatMediaPreviewStrip(
                        media = pendingMedia,
                        onEdit = { editingMedia = it },
                        onRemove = { removed ->
                            pendingMedia = pendingMedia.filterNot { it.uri == removed.uri }
                            if (pendingMedia.isEmpty() && state.viewOnceEnabled) onToggleViewOnce()
                        }
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isRecording) {
                    IconButton(
                        onClick = { mediaPicker.launch(arrayOf("image/*", "video/*", "audio/*")) },
                        enabled = !state.mediaInFlight && !isRecording
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add media")
                    }
                    IconButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED
                            ) launchCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
                        },
                        enabled = !state.mediaInFlight && !isRecording && pendingMedia.size < 8
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Open camera")
                    }
                    if (pendingMedia.any { !it.mimeType.startsWith("audio/") }) {
                        IconButton(
                            onClick = onToggleViewOnce,
                            enabled = !isRecording
                        ) {
                            Icon(
                                Icons.Default.LooksOne,
                                contentDescription = if (state.viewOnceEnabled) "Disable view once" else "Enable view once",
                                tint = if (state.viewOnceEnabled) VybTeal else VybMuted
                            )
                        }
                    }
                    }
                    if (isRecording) {
                        IconButton(onClick = { finishVoiceRecording(false) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Discard voice note", tint = MaterialTheme.colorScheme.error)
                        }
                        Text(
                            "%d:%02d".format(recordingSeconds / 60, recordingSeconds % 60),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Slide left to cancel",
                            modifier = Modifier.weight(1f),
                            color = VybMuted,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else {
                    OutlinedTextField(
                        value = state.draft,
                        onValueChange = onDraftChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    }
                    Spacer(Modifier.width(6.dp))
                    if (state.draft.isBlank() && pendingMedia.isEmpty() && !isRecording && !state.mediaInFlight) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(VybTeal.copy(alpha = 0.12f))
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                                            PackageManager.PERMISSION_GRANTED
                                        ) {
                                            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                                            while (currentEvent.changes.any { it.pressed }) awaitPointerEvent()
                                            return@awaitEachGesture
                                        }
                                        beginVoiceRecording()
                                        val startX = down.position.x
                                        var cancelled = false
                                        do {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull()
                                            if (change != null && change.position.x - startX < -96.dp.toPx()) cancelled = true
                                        } while (event.changes.any { it.pressed })
                                        finishVoiceRecording(!cancelled)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Hold to record voice note", tint = VybTeal)
                        }
                    } else {
                    IconButton(
                        onClick = {
                            val selected = pendingMedia
                            if (selected.isNotEmpty()) {
                                onSendMediaBatch(selected)
                                pendingMedia = emptyList()
                            } else if (isRecording) {
                                finishVoiceRecording(true)
                            } else {
                                onSend()
                            }
                        },
                        enabled = !state.isSending && !state.mediaInFlight
                    ) {
                        if (state.isSending || state.mediaInFlight) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else if (isRecording) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "%d:%02d".format(recordingSeconds / 60, recordingSeconds % 60),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Icon(Icons.Default.Stop, contentDescription = "Send voice note")
                            }
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                    }
                }
                }
                }
            }
        }
    }
    }
    if (settingsOpen) {
        ChatSettingsSheet(
            durationKey = state.durationKey,
            onDurationChange = onDurationChange,
            onRefresh = onRetry,
            onDismiss = { settingsOpen = false }
        )
    }
    editingMedia?.let { selected ->
        StoryBuilderScreen(
            media = SelectedMedia(
                uri = selected.uri,
                fileName = selected.fileName,
                mimeType = selected.mimeType,
                sizeBytes = runCatching {
                    context.contentResolver.openAssetFileDescriptor(selected.uri, "r")?.use { it.length }
                }.getOrNull()?.coerceAtLeast(0L) ?: 0L,
                mediaType = if (selected.mimeType.startsWith("video/")) "video" else "image"
            ),
            intent = MediaPublishIntent.Post,
            onApply = { edited ->
                val metadata = readPickedMediaMetadata(context, edited.uri)
                val updated = ChatMediaDraft(
                    edited.uri,
                    edited.fileName,
                    edited.mimeType,
                    metadata.width,
                    metadata.height,
                    metadata.durationMs
                )
                pendingMedia = pendingMedia.map { if (it.uri == selected.uri) updated else it }
                editingMedia = null
            },
            onDismiss = { editingMedia = null }
        )
    }
    val activeMedia = state.activeMediaMessageId?.let { id ->
        state.messages.firstOrNull { it.id == id }
    }
    if (activeMedia?.localMediaUri != null) {
        val lightboxMedia = if (activeMedia.attachment?.viewOnce == true) {
            listOf(activeMedia)
        } else {
            state.messages.filter {
                it.attachment != null && !it.attachment.viewOnce && it.localMediaUri != null && !it.mediaConsumed
            }
        }
        ChatMediaLightbox(
            messages = lightboxMedia,
            initialMessageId = activeMedia.id,
            viewOnce = activeMedia.attachment?.viewOnce == true,
            onClose = onCloseViewOnce
        )
    }
}

@Composable
private fun ChatMediaLightbox(
    messages: List<ChatMessageItem>,
    initialMessageId: String,
    viewOnce: Boolean,
    onClose: () -> Unit
) {
    if (messages.isEmpty()) return
    val initialPage = messages.indexOfFirst { it.id == initialMessageId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { messages.size }
    Dialog(
        onDismissRequest = { if (!viewOnce) onClose() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !viewOnce,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Box(Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { messages[it].id }
                ) { page ->
                    val message = messages[page]
                    val uri = message.localMediaUri
                    if (uri != null && message.attachment?.mimeType?.startsWith("image/") == true) {
                        ZoomableChatImage(uri = uri, contentDescription = "Chat image")
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            ChatMediaContent(message = message, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 18.dp)
                        .background(Color.Black.copy(alpha = 0.56f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = if (viewOnce) "Close and consume view once media" else "Close media",
                        tint = Color.White
                    )
                }
                if (viewOnce) {
                    Text(
                        "View once · closing removes access",
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 30.dp)
                            .background(Color.Black.copy(alpha = 0.52f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableChatImage(uri: String, contentDescription: String) {
    var scale by remember(uri) { mutableStateOf(1f) }
    var offset by remember(uri) { mutableStateOf(Offset.Zero) }
    AsyncImage(
        model = uri,
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .pointerInput(uri) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        if (event.changes.size > 1 || scale > 1f) {
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale <= 1.01f) Offset.Zero else offset + pan
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(uri) {
                detectTapGestures(onDoubleTap = {
                    scale = if (scale > 1.01f) 1f else 2f
                    offset = Offset.Zero
                })
            },
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatSettingsSheet(
    durationKey: String,
    onDurationChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = remember {
        listOf(
            "instant" to "Instant",
            "1h" to "1h",
            "24h" to "24h",
            "7d" to "7d",
            "30d" to "30d",
            "90d" to "90d"
        )
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VybPanelLifted,
        contentColor = VybText,
        dragHandle = {
            Box(
                Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape).background(VybMuted.copy(alpha = 0.45f))
            )
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Chat settings",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close chat settings")
                }
            }
            Text(
                "Auto-Destruct Time",
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = VybMuted
            )
            options.chunked(3).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowOptions.forEach { (value, label) ->
                        val selected = durationKey == value
                        Surface(
                            modifier = Modifier.weight(1f).clickable {
                                onDurationChange(value)
                            },
                            color = if (selected) VybIndigo else VybPanelLifted,
                            border = BorderStroke(
                                1.dp,
                                if (selected) VybPurple.copy(alpha = 0.65f) else VybBorder
                            ),
                            shape = RoundedCornerShape(9.dp)
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = if (selected) Color.White else VybMuted,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
                    onRefresh()
                    onDismiss()
                },
                color = VybTeal.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, VybTeal.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Refresh conversation",
                    modifier = Modifier.padding(13.dp),
                    color = VybTeal,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessageItem, onOpenMedia: (ChatMessageItem) -> Unit) {
    if (message.body.startsWith("Suspected screenshot:")) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Surface(
                color = VybPanelLifted.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, VybBorder),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Image, null, Modifier.size(14.dp), tint = VybMuted)
                    Text(
                        " ${message.body.removePrefix("Suspected screenshot: ")}",
                        color = VybMuted,
                        fontSize = 10.sp
                    )
                    Text("  ${message.timestamp}", color = VybMuted.copy(alpha = 0.72f), fontSize = 8.sp)
                }
            }
        }
        return
    }
    var receiptExpanded by rememberSaveable(message.id) { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val bubbleMaxWidth = maxWidth * if (message.attachment != null) 0.91f else 0.84f
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        val shape = if (message.isMine) {
            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp)
        } else {
            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 20.dp)
        }
        val brush = if (message.isMine) {
            Brush.linearGradient(listOf(VybIndigo, VybPurple))
        } else {
            Brush.linearGradient(listOf(VybTeal.copy(alpha = 0.78f), VybTeal.copy(alpha = 0.50f)))
        }
        val isGeneratedAttachmentLabel = message.attachment != null && message.body.trim().lowercase() in setOf(
            "voice note", "photo", "video", "shared image", "shared video", "shared media"
        )
        val receiptLabel = when (message.deliveryState) {
            ChatDeliveryState.Pending -> "Sending"
            ChatDeliveryState.Sent -> "Sent"
            ChatDeliveryState.Delivered -> "Delivered"
            ChatDeliveryState.Read -> "Read"
            ChatDeliveryState.Failed -> "Failed"
        }
        val receiptColor = when (message.deliveryState) {
            ChatDeliveryState.Pending, ChatDeliveryState.Sent -> Color.White.copy(alpha = 0.52f)
            ChatDeliveryState.Delivered -> Color(0xFFFFD166)
            ChatDeliveryState.Read -> Color(0xFF6EF3B2)
            ChatDeliveryState.Failed -> MaterialTheme.colorScheme.error
        }
        Box(
            Modifier.widthIn(
                min = 76.dp,
                max = bubbleMaxWidth
            )
                .clip(shape)
                .background(brush)
                .border(0.75.dp, Color.White.copy(alpha = 0.14f), shape)
        ) {
            Column(if (message.attachment != null) Modifier.fillMaxWidth() else Modifier) {
                if (message.attachment != null) {
                    when {
                        message.mediaConsumed -> Surface(
                            modifier = Modifier.fillMaxWidth().padding(2.dp),
                            color = Color.Black.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                "View-once media opened",
                                modifier = Modifier.padding(18.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 12.sp
                            )
                        }
                        message.attachment.viewOnce && message.localMediaUri == null -> Surface(
                            modifier = Modifier.fillMaxWidth().padding(2.dp).clickable { onOpenMedia(message) },
                            color = Color.Black.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (message.mediaLoading) {
                                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Icon(Icons.Default.LooksOne, null, tint = Color.White)
                                    Text(" View once", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        else -> ChatMediaContent(
                            message = message,
                            modifier = Modifier.fillMaxWidth().padding(2.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenMedia(message) }
                        )
                    }
                }
                if (!isGeneratedAttachmentLabel) {
                    Text(
                        message.body,
                        modifier = Modifier.padding(
                            start = 13.dp,
                            end = 13.dp,
                            top = if (message.attachment != null) 6.dp else 8.dp,
                            bottom = 24.dp
                        ),
                        color = if (message.isReadable) Color.White else Color.White.copy(alpha = 0.72f)
                    )
                }
            }
            run {
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 6.dp),
                    color = Color.Black.copy(alpha = 0.24f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        buildAnnotatedString {
                            append(message.timestamp)
                            if (message.isMine) {
                                append(" ")
                                pushStyle(SpanStyle(color = receiptColor, fontWeight = FontWeight.Bold))
                                append(if (receiptExpanded) receiptLabel else "●")
                                pop()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            .clickable(enabled = message.isMine) { receiptExpanded = !receiptExpanded },
                        color = Color.White.copy(alpha = 0.84f),
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun PeerTypingBubble() {
    val transition = rememberInfiniteTransition(label = "peer typing")
    val phases = listOf(0, 120, 240).mapIndexed { index, delayMs ->
        transition.animateFloat(
            initialValue = 0.34f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 420, delayMillis = delayMs),
                repeatMode = RepeatMode.Reverse
            ),
            label = "typing dot $index"
        )
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Row(
            modifier = Modifier
                .animateContentSize()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 20.dp))
                .background(Brush.linearGradient(listOf(VybTeal.copy(alpha = 0.78f), VybTeal.copy(alpha = 0.50f))))
                .padding(horizontal = 15.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            phases.forEach { phase ->
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = phase.value))
                )
            }
        }
    }
}

@Composable
private fun ChatMediaContent(message: ChatMessageItem, modifier: Modifier = Modifier) {
    val attachment = message.attachment ?: return
    val uri = message.localMediaUri
    when {
        message.mediaLoading || uri == null -> Box(
            modifier.aspectRatio(4f / 3f).background(Color.Black.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
        }
        attachment.mimeType.startsWith("image/") -> AsyncImage(
            model = uri,
            contentDescription = "Shared image",
            modifier = modifier.heightIn(min = 120.dp, max = 360.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        attachment.mimeType.startsWith("video/") -> AndroidView(
            modifier = modifier.aspectRatio(
                if ((attachment.width ?: 0) > 0 && (attachment.height ?: 0) > 0) {
                    attachment.width!!.toFloat() / attachment.height!!.toFloat()
                } else 16f / 9f
            ),
            factory = { viewContext ->
                VideoView(viewContext).apply {
                    setVideoURI(Uri.parse(uri))
                    setMediaController(android.widget.MediaController(viewContext).also { it.setAnchorView(this) })
                    setOnPreparedListener { player -> player.isLooping = false }
                }
            },
            update = { view -> if (!view.isPlaying) view.setVideoURI(Uri.parse(uri)) }
        )
        else -> VoiceNotePlayer(uri = uri, durationMs = attachment.durationMs, modifier = modifier.padding(bottom = 12.dp))
    }
}

@Composable
private fun VoiceNotePlayer(uri: String, durationMs: Int?, modifier: Modifier = Modifier) {
    var playing by remember(uri) { mutableStateOf(false) }
    var prepared by remember(uri) { mutableStateOf(false) }
    var preparing by remember(uri) { mutableStateOf(true) }
    var pendingPlay by remember(uri) { mutableStateOf(false) }
    var playbackFailed by remember(uri) { mutableStateOf(false) }
    var positionMs by remember(uri) { mutableStateOf(0) }
    var resolvedDurationMs by remember(uri) { mutableStateOf(durationMs?.coerceAtLeast(1) ?: 1) }
    var playbackSpeed by remember(uri) { mutableStateOf(1f) }
    val context = LocalContext.current
    val player = remember(uri) { MediaPlayer() }
    DisposableEffect(player, uri) {
        runCatching {
            player.setDataSource(context, Uri.parse(uri))
            player.setOnPreparedListener { preparedPlayer ->
                prepared = true
                preparing = false
                playbackFailed = false
                resolvedDurationMs = preparedPlayer.duration.coerceAtLeast(durationMs ?: 1)
                if (pendingPlay) {
                    pendingPlay = false
                    preparedPlayer.start()
                    playing = true
                }
            }
            player.setOnCompletionListener {
                playing = false
                pendingPlay = false
                positionMs = 0
                runCatching { player.seekTo(0) }
            }
            player.setOnErrorListener { _, _, _ ->
                prepared = false
                preparing = false
                pendingPlay = false
                playing = false
                playbackFailed = true
                true
            }
            player.prepareAsync()
        }.onFailure {
            preparing = false
            playbackFailed = true
        }
        onDispose { player.release() }
    }
    LaunchedEffect(player, playing) {
        while (playing) {
            positionMs = runCatching { player.currentPosition }.getOrDefault(positionMs)
            delay(120)
        }
    }
    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            enabled = !playbackFailed,
            onClick = {
                when {
                    playing -> {
                        player.pause()
                        playing = false
                    }
                    prepared -> {
                        runCatching { player.playbackParams = player.playbackParams.setSpeed(playbackSpeed) }
                        player.start()
                        playing = true
                    }
                    else -> pendingPlay = true
                }
            }
        ) {
            when {
                preparing || pendingPlay -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                else -> Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pause voice note" else "Play voice note"
                )
            }
        }
        Surface(
            modifier = Modifier.padding(end = 5.dp).clickable {
                playbackSpeed = when (playbackSpeed) {
                    1f -> 1.5f
                    1.5f -> 2f
                    else -> 1f
                }
                if (prepared) runCatching { player.playbackParams = player.playbackParams.setSpeed(playbackSpeed) }
            },
            color = Color.Black.copy(alpha = 0.14f),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                if (playbackSpeed == 1f) "1×" else "${playbackSpeed}×",
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(Modifier.weight(1f)) {
            val progress = positionMs.coerceIn(0, resolvedDurationMs).toFloat() / resolvedDurationMs.coerceAtLeast(1)
            val seekModifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .pointerInput(prepared, resolvedDurationMs) {
                    detectTapGestures { offset ->
                        if (prepared) {
                            positionMs = ((offset.x / size.width) * resolvedDurationMs).toInt().coerceIn(0, resolvedDurationMs)
                            player.seekTo(positionMs)
                        }
                    }
                }
                .pointerInput(prepared, resolvedDurationMs) {
                    detectHorizontalDragGestures { change, _ ->
                        if (prepared) {
                            positionMs = ((change.position.x / size.width) * resolvedDurationMs).toInt().coerceIn(0, resolvedDurationMs)
                            player.seekTo(positionMs)
                            change.consume()
                        }
                    }
                }
            Canvas(seekModifier) {
                val bars = 38
                val gap = size.width / bars
                repeat(bars) { index ->
                    val normalizedHeight = 0.28f + (((index * 37) % 19) / 27f)
                    val barHeight = size.height * normalizedHeight.coerceAtMost(0.92f)
                    val x = gap * index + gap / 2
                    drawLine(
                        color = if (x <= size.width * progress) Color.White else Color.White.copy(alpha = 0.30f),
                        start = Offset(x, (size.height - barHeight) / 2),
                        end = Offset(x, (size.height + barHeight) / 2),
                        strokeWidth = (gap * 0.42f).coerceAtLeast(2f),
                        cap = StrokeCap.Round
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatAudioDuration(positionMs), color = Color.White.copy(alpha = 0.70f), fontSize = 9.sp)
                Text(
                    if (playbackFailed) "Unavailable" else formatAudioDuration(resolvedDurationMs),
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun ChatMediaPreviewStrip(
    media: List<ChatMediaDraft>,
    onEdit: (ChatMediaDraft) -> Unit,
    onRemove: (ChatMediaDraft) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            "${media.size}/8 selected · tap the pencil to edit",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            color = VybMuted,
            fontSize = 10.sp
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            itemsIndexed(media, key = { _, item -> item.uri.toString() }) { index, item ->
                Box(
                    Modifier.size(width = 82.dp, height = 94.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.22f))
                        .border(1.dp, VybBorder, RoundedCornerShape(14.dp))
                ) {
                    if (item.mimeType.startsWith("image/")) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = "Selected media ${index + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            if (item.mimeType.startsWith("video/")) Icons.Default.PlayArrow else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp).align(Alignment.Center),
                            tint = Color.White
                        )
                    }
                    if (item.mimeType.startsWith("image/")) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopStart).padding(3.dp).size(30.dp).clickable { onEdit(item) },
                            color = Color.Black.copy(alpha = 0.68f),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit selected media ${index + 1}", Modifier.padding(6.dp), tint = Color.White)
                        }
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(30.dp).clickable { onRemove(item) },
                        color = Color.Black.copy(alpha = 0.68f),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove selected media ${index + 1}", Modifier.padding(6.dp), tint = Color.White)
                    }
                    Text(
                        "${index + 1}",
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatAudioDuration(durationMs: Int): String =
    "%d:%02d".format(durationMs / 60_000, (durationMs / 1_000) % 60)

private data class PickedMediaMetadata(
    val fileName: String,
    val mimeType: String,
    val width: Int?,
    val height: Int?,
    val durationMs: Int?
)

private fun readPickedMediaMetadata(context: Context, uri: Uri): PickedMediaMetadata {
    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
    var fileName = "chat-media-${System.currentTimeMillis()}"
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) fileName = cursor.getString(0) ?: fileName
    }
    var width: Int? = null
    var height: Int? = null
    var durationMs: Int? = null
    if (mimeType.startsWith("image/")) {
        runCatching {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            width = options.outWidth.takeIf { it > 0 }
            height = options.outHeight.takeIf { it > 0 }
        }
    } else if (mimeType.startsWith("video/") || mimeType.startsWith("audio/")) {
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
            }
        }
    }
    return PickedMediaMetadata(fileName, mimeType, width, height, durationMs)
}

@Composable
private fun CenterStatus(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun ErrorStatus(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = 28.dp),
            color = MaterialTheme.colorScheme.error
        )
        IconButton(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = "Retry")
        }
    }
}
