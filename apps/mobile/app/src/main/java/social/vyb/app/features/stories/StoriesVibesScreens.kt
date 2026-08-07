package social.vyb.app.features.stories

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import social.vyb.app.ui.VybEmptyState
import social.vyb.app.ui.VybLoadingMark
import social.vyb.app.ui.VybRemoteImage
import social.vyb.app.ui.VybFillVideoView
import social.vyb.app.ui.VybRemoteVideo
import social.vyb.app.ui.resolveRemoteMediaUrl
import social.vyb.app.features.media.StoryCompositionCodec
import social.vyb.app.features.media.StoryCompositionJson
import social.vyb.app.features.social.CommentThreadState
import social.vyb.app.features.social.ContentMeasurementRepository
import social.vyb.app.features.social.SocialThreadSheet
import social.vyb.app.features.social.PostEngagementState
import social.vyb.app.features.social.PostOverflowActions
import social.vyb.app.features.social.PostRepostDialog
import social.vyb.app.features.social.PostReactionMembersDialog
import social.vyb.app.features.social.ReactionMembersState
import social.vyb.app.features.social.SocialActionsViewModel
import social.vyb.app.features.social.SocialOperationFeedback
import social.vyb.app.features.social.SocialAvatar
import social.vyb.app.features.social.SocialCommentAction
import social.vyb.app.features.social.SocialReactionsSheet
import social.vyb.app.features.social.BouncyIconButton

/**
 * Drop-in home-feed story lane. It owns no navigation and opens the full-screen
 * story viewer internally.
 */
@Composable
fun StoriesLane(
    modifier: Modifier = Modifier,
    showLoadingIndicator: Boolean = true,
    onCreateStory: () -> Unit = {},
    viewModel: StoriesVibesViewModel = viewModel()
) {
    val state by viewModel.stories.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.loadStories() }

    Column(modifier) {
        when {
            state.isLoading -> {
                if (showLoadingIndicator) {
                    Box(Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
                        VybLoadingMark(width = 72.dp)
                    }
                } else {
                    Spacer(Modifier.height(12.dp))
                }
            }
            state.items.isEmpty() -> {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    )
                ) {
                    item { AddStoryBubble(onCreateStory) }
                }
                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            else -> {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { AddStoryBubble(onCreateStory) }
                    itemsIndexed(state.items, key = { _, story -> story.id }) { index, story ->
                        StoryBubble(story = story, onClick = { viewModel.openStory(index) })
                    }
                }
                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    if (state.selectedStory != null) {
        StoryViewerDialog(state = state, viewModel = viewModel)
    }
}

@Composable
private fun AddStoryBubble(onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(62.dp),
            color = social.vyb.app.ui.VybPanelLifted,
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add your story",
                    tint = social.vyb.app.ui.VybText,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Text(
            "Your story",
            color = social.vyb.app.ui.VybText,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 7.dp)
        )
    }
}

/**
 * Drop-in replacement for the placeholder Vibes route.
 */
@Composable
fun NativeVibesScreen(
    modifier: Modifier = Modifier,
    viewModel: StoriesVibesViewModel = viewModel(),
    initialVibeId: String? = null,
    onInitialVibeConsumed: () -> Unit = {},
    viewerUserId: String? = null,
    socialViewModel: SocialActionsViewModel,
    onCreateVibe: () -> Unit = {},
    onCreateStory: () -> Unit = {},
    onSearch: () -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    refreshSignal: Int = 0,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val measurementRepository = remember { ContentMeasurementRepository() }
    val state by viewModel.vibes.collectAsStateWithLifecycle()
    val socialState = socialViewModel.state
    var commentsPostId by remember { mutableStateOf<String?>(null) }
    var shareVibeId by remember { mutableStateOf<String?>(null) }
    var reportVibeId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(viewModel) { viewModel.loadVibes() }
    LaunchedEffect(refreshSignal) {
        if (refreshSignal > 0) viewModel.refreshVibes()
    }

    shareVibeId?.let { id ->
        val vibe = state.items.find { it.id == id }
        vibe?.let { v ->
            social.vyb.app.features.social.SocialShareSheet(
                postId = v.id,
                postKind = v.kind,
                postPlacement = v.placement,
                postTitle = v.title,
                postBody = v.body,
                postMediaUrl = v.mediaUrl,
                authorDisplayName = v.author.displayName,
                authorUsername = v.author.username,
                authorIsAnonymous = v.author.isAnonymous,
                onDismiss = { shareVibeId = null },
                onAddToStory = onCreateStory
            )
        }
    }

    when {
        state.isLoading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                VybLoadingMark(width = 104.dp)
            }
        }
        state.items.isEmpty() -> {
            Box(
                modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                VybEmptyState(
                    icon = Icons.Default.AutoAwesome,
                    title = if (state.error == null) "No campus vibes yet" else "Vibes unavailable",
                    body = state.error ?: "Short videos from your campus will appear here.",
                    actionLabel = "Refresh",
                    onAction = viewModel::refreshVibes
                )
            }
        }
        else -> {
            val pagerState = rememberPagerState(pageCount = { state.items.size })
            LaunchedEffect(initialVibeId, state.items) {
                val targetIndex = state.items.indexOfFirst { it.id == initialVibeId }
                if (targetIndex >= 0) {
                    pagerState.scrollToPage(targetIndex)
                    onInitialVibeConsumed()
                }
            }
            LaunchedEffect(pagerState.currentPage, state.items.size, state.nextCursor) {
                if (pagerState.currentPage >= state.items.lastIndex - 2) {
                    viewModel.loadMoreVibes()
                }
            }

            BoxWithConstraints(modifier.fillMaxSize().background(Color.Black)) {
                val tablet = maxWidth >= 700.dp
                val stageModifier = if (tablet) {
                    Modifier.fillMaxHeight().widthIn(max = 560.dp).align(Alignment.Center)
                } else {
                    Modifier.fillMaxSize()
                }
                VerticalPager(
                    state = pagerState,
                    modifier = stageModifier,
                    key = { index -> state.items[index].id }
                ) { page ->
                    val vibe = state.items[page]
                    LaunchedEffect(
                        vibe.id,
                        vibe.reactions,
                        vibe.viewerReactionType,
                        vibe.savedCount,
                        vibe.isSaved
                    ) {
                        socialViewModel.seedPost(
                            postId = vibe.id,
                            reactionCount = vibe.reactions,
                            viewerReactionType = vibe.viewerReactionType,
                            savedCount = vibe.savedCount,
                            isSaved = vibe.isSaved
                        )
                    }
                    VibePage(
                        vibe = vibe,
                        isActive = pagerState.currentPage == page,
                        measurementRepository = measurementRepository,
                        engagement = socialState.engagements[vibe.id] ?: PostEngagementState(),
                        commentCount = socialState.commentThreads[vibe.id]
                            ?.takeIf { it.loaded }
                            ?.items
                            ?.size
                            ?: vibe.comments,
                        isOwner = vibe.viewerCanManage ||
                            (
                                viewerUserId != null &&
                                    viewerUserId == (vibe.author.userId ?: vibe.userId)
                            ),
                        busy = vibe.id in socialState.busyPostIds,
                        reactionMembers = socialState.reactionMembers[vibe.id]
                            ?: ReactionMembersState(),
                        onToggleLike = { socialViewModel.toggleReaction(vibe.id) },
                        onOpenComments = { commentsPostId = vibe.id },
                        onToggleSave = { socialViewModel.toggleSave(vibe.id) },
                        onLoadReactionMembers = {
                            socialViewModel.loadReactionMembers(vibe.id)
                        },
                        onRepost = { quote, placement ->
                            socialViewModel.repost(vibe.id, quote, placement = placement) {
                                viewModel.refreshVibes()
                            }
                        },
                        onUpdate = { title, body ->
                            socialViewModel.updatePost(vibe.id, title, body) {
                                viewModel.refreshVibes()
                            }
                        },
                        onDelete = {
                            socialViewModel.deletePost(vibe.id, viewModel::refreshVibes)
                        },
                        onReport = { reason ->
                            socialViewModel.report("post", vibe.id, reason)
                        },
                        onViewInsights = { socialViewModel.loadContentInsights(vibe.id) },
                        onNotInterested = {
                            socialViewModel.hideRecommendation(vibe.id, viewModel::refreshVibes)
                        },
                        onSearch = onSearch,
                        onCreateVibe = onCreateVibe,
                        onRefresh = viewModel::refreshVibes,
                        onShare = { shareVibeId = vibe.id },
                        onOpenProfile = onOpenProfile
                    )
                }

                state.error?.let {
                    Text(
                        it,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .background(Color(0xCCB3261E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                if (state.isLoadingMore) {
                    CircularProgressIndicator(
                        Modifier.align(Alignment.BottomEnd).padding(18.dp).size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }

    commentsPostId?.let { postId ->
        SocialThreadSheet(
            postId = postId,
            thread = socialState.commentThreads[postId] ?: CommentThreadState(),
            onLoad = { socialViewModel.loadComments(postId) },
            onRetry = { socialViewModel.loadComments(postId, force = true) },
            onAddComment = { text, parentCommentId, isAnonymous, mediaUrl, mediaType, done ->
                socialViewModel.addComment(
                    postId,
                    text,
                    parentCommentId = parentCommentId,
                    isAnonymous = isAnonymous,
                    mediaUrl = mediaUrl,
                    mediaType = mediaType,
                    onAdded = done
                )
            },
            onToggleCommentReaction = { commentId ->
                socialViewModel.toggleCommentReaction(postId, commentId)
            },
            onUpdateComment = { commentId, body ->
                socialViewModel.updateComment(postId, commentId, body)
            },
            onDeleteComment = { commentId ->
                socialViewModel.deleteComment(postId, commentId)
            },
            onReportComment = { commentId, reason ->
                socialViewModel.report("comment", commentId, reason)
            },
            busyCommentIds = socialState.busyCommentIds,
            onDismiss = { commentsPostId = null }
        )
    }
    SocialOperationFeedback(
        state = socialState,
        onDismissError = socialViewModel::clearOperationError,
        onDismissNotice = socialViewModel::clearOperationNotice
    )
}

@Composable
private fun StoryBubble(story: StoryItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(72.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(66.dp)
                .clip(CircleShape)
                .border(
                    3.dp,
                    if (story.viewerHasSeen) Color.Gray else MaterialTheme.colorScheme.primary,
                    CircleShape
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            SocialAvatar(
                avatarUrl = story.avatarUrl,
                displayName = story.displayName,
                size = 52.dp,
                contentDescription = "${story.displayName} story avatar"
            )
            if (story.mediaType == "video") {
                Icon(
                    Icons.Default.PlayArrow,
                    null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .size(19.dp)
                        .background(Color.Black.copy(alpha = .6f), CircleShape)
                )
            }
        }
        Text(
            if (story.isOwn) "Your story" else story.username,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

@Composable
private fun StoryViewerDialog(
    state: StoriesUiState,
    viewModel: StoriesVibesViewModel
) {
    val story = state.selectedStory ?: return
    Dialog(
        onDismissRequest = viewModel::closeStory,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            StoryMedia(story, Modifier.fillMaxSize())

            Row(
                Modifier.align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = .85f), Color.Transparent))
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(story.avatarUrl, story.displayName)
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(story.displayName, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("@${story.username}", color = Color.White.copy(alpha = .75f))
                }
                IconButton(onClick = viewModel::closeStory) {
                    Icon(Icons.Default.Close, "Close story", tint = Color.White)
                }
            }

            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier.weight(.34f).fillMaxHeight()
                        .clickable(onClick = viewModel::previousStory)
                )
                Spacer(Modifier.weight(.32f).fillMaxHeight())
                Box(
                    Modifier.weight(.34f).fillMaxHeight()
                        .clickable(onClick = viewModel::nextStory)
                )
            }

            Column(
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .9f)))
                    )
                    .padding(18.dp)
            ) {
                if (story.caption.isNotBlank()) {
                    Text(story.caption, color = Color.White, modifier = Modifier.padding(bottom = 10.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        enabled = story.id !in state.busyStoryIds,
                        onClick = viewModel::toggleSelectedStoryLike
                    ) {
                        if (story.id in state.busyStoryIds) {
                            CircularProgressIndicator(
                                Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                if (story.viewerHasLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                "Like story",
                                tint = if (story.viewerHasLiked) Color(0xFFFF4D67) else Color.White
                            )
                        }
                    }
                    Text(story.reactions.toCompactMetric(), color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun VibePage(
    vibe: VibeItem,
    isActive: Boolean,
    measurementRepository: ContentMeasurementRepository,
    engagement: PostEngagementState,
    commentCount: Int,
    isOwner: Boolean,
    busy: Boolean,
    reactionMembers: ReactionMembersState,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onToggleSave: () -> Unit,
    onLoadReactionMembers: () -> Unit,
    onRepost: (String, String) -> Unit,
    onUpdate: (String, String) -> Unit,
    onDelete: () -> Unit,
    onReport: (String) -> Unit,
    onViewInsights: () -> Unit,
    onNotInterested: () -> Unit,
    onSearch: () -> Unit,
    onCreateVibe: () -> Unit,
    onRefresh: () -> Unit,
    onShare: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val playbackPreferences = remember {
        context.getSharedPreferences("vibe_playback", Context.MODE_PRIVATE)
    }
    var repostOpen by remember(vibe.id) { mutableStateOf(false) }
    var muted by remember { mutableStateOf(playbackPreferences.getBoolean("muted", false)) }
    var hasAudio by remember(vibe.id) { mutableStateOf<Boolean?>(null) }
    var paused by remember(vibe.id) { mutableStateOf(false) }
    var playbackRate by remember(vibe.id) { mutableFloatStateOf(1f) }
    var holdActivated by remember(vibe.id) { mutableStateOf(false) }
    var showPlaybackFeedback by remember(vibe.id) { mutableStateOf(false) }
    var heartBurst by remember(vibe.id) { mutableStateOf(false) }
    var likesOpen by remember(vibe.id) { mutableStateOf(false) }
    var descriptionExpanded by remember(vibe.id) { mutableStateOf(false) }
    var videoPlaySent by remember(vibe.id) { mutableStateOf(false) }
    var videoViewSent by remember(vibe.id) { mutableStateOf(false) }
    var videoCompleteSent by remember(vibe.id) { mutableStateOf(false) }
    var watchedMs by remember(vibe.id) { mutableIntStateOf(0) }
    var previousPositionMs by remember(vibe.id) { mutableIntStateOf(0) }
    val gestureScope = rememberCoroutineScope()
    // Swipe-to-profile state
    var swipeDeltaX by remember(vibe.id) { mutableFloatStateOf(0f) }
    var swipeConsumed by remember(vibe.id) { mutableStateOf(false) }
    val profileHintAlpha by animateFloatAsState(
        targetValue = (swipeDeltaX / 160f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "profileHint"
    )
    LaunchedEffect(heartBurst) {
        if (heartBurst) {
            delay(520)
            heartBurst = false
        }
    }
    // Reset swipe state when vibe changes
    LaunchedEffect(vibe.id) {
        swipeDeltaX = 0f
        swipeConsumed = false
    }
    LaunchedEffect(vibe.id, isActive) {
        if (!isActive) {
            measurementRepository.flush()
            return@LaunchedEffect
        }
        videoPlaySent = false
        videoViewSent = false
        videoCompleteSent = false
        watchedMs = 0
        previousPositionMs = 0
        delay(500)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            measurementRepository.record(vibe.id, "impression", visibleMs = 500)
        }
        delay(500)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            measurementRepository.record(vibe.id, "qualified_view", visibleMs = 1000, flush = true)
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(vibe.id) {
                var profileSwipeEligible = false
                detectHorizontalDragGestures(
                    onDragStart = { start ->
                        profileSwipeEligible = start.y < size.height - 72.dp.toPx()
                        swipeDeltaX = 0f
                    },
                    onDragEnd = {
                        if (
                            swipeDeltaX > 140f && !swipeConsumed &&
                            !vibe.author.isAnonymous && vibe.author.username.isNotBlank()
                        ) {
                            swipeConsumed = true
                            onOpenProfile(vibe.author.username)
                        }
                        swipeDeltaX = 0f
                    },
                    onDragCancel = { swipeDeltaX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        if (profileSwipeEligible && !swipeConsumed) {
                            // A right-to-left drag has a negative delta. Convert it to a
                            // positive reveal distance while cancelling any reverse motion.
                            swipeDeltaX = (swipeDeltaX - dragAmount).coerceAtLeast(0f)
                            if (swipeDeltaX > 10f) change.consume()
                        }
                    }
                )
            }
    ) {
        val mediaUrl = vibe.playableMediaUrl
        when {
            mediaUrl == null -> {
                Text(
                    "Media unavailable",
                    color = Color.White.copy(alpha = .65f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            vibe.kind == "video" || vibe.media.any { it.kind == "video" } -> {
                NativeVideo(
                    url = mediaUrl,
                    isActive = isActive && !paused,
                    modifier = Modifier.fillMaxSize(),
                    crop = true,
                    muted = muted,
                    playbackRate = playbackRate,
                    showScrubber = true,
                    onAudioAvailabilityChanged = { hasAudio = it },
                    onPlaybackProgress = { positionMs, durationMs ->
                        if (!isActive || durationMs <= 0 ||
                            !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                        ) return@NativeVideo
                        if (!videoPlaySent) {
                            videoPlaySent = true
                            gestureScope.launch { measurementRepository.record(vibe.id, "video_play") }
                        }
                        val delta = positionMs - previousPositionMs
                        if (delta in 1..1_500) watchedMs += delta
                        if (previousPositionMs > durationMs * 8 / 10 && positionMs < durationMs / 5) {
                            gestureScope.launch { measurementRepository.record(vibe.id, "video_replay") }
                        }
                        previousPositionMs = positionMs
                        val progress = ((positionMs.toLong() * 10_000L) / durationMs).toInt().coerceIn(0, 10_000)
                        if (!videoViewSent && (watchedMs >= 3_000 || progress >= 3_000)) {
                            videoViewSent = true
                            gestureScope.launch {
                                measurementRepository.record(
                                    vibe.id,
                                    "video_view",
                                    watchMs = maxOf(3_000, watchedMs),
                                    progressBasisPoints = progress
                                )
                            }
                        }
                        if (!videoCompleteSent && progress >= 9_500) {
                            videoCompleteSent = true
                            gestureScope.launch {
                                measurementRepository.record(
                                    vibe.id,
                                    "video_complete",
                                    watchMs = watchedMs,
                                    progressBasisPoints = progress,
                                    flush = true
                                )
                            }
                        }
                    }
                )
            }
            else -> RemoteImage(mediaUrl, Modifier.fillMaxSize(), ContentScale.Fit)
        }

        Box(
            Modifier
                .fillMaxSize()
                // Keep the gesture surface above AndroidView/VideoView so taps are
                // delivered consistently while action buttons rendered later remain clickable.
                .pointerInput(vibe.id) {
                    detectTapGestures(
                        onPress = {
                            holdActivated = false
                            val speedJob = gestureScope.launch {
                                delay(320)
                                holdActivated = true
                                playbackRate = 2f
                            }
                            tryAwaitRelease()
                            speedJob.cancel()
                            playbackRate = 1f
                        },
                        onTap = {
                            if (holdActivated) {
                                holdActivated = false
                            } else {
                                paused = !paused
                                showPlaybackFeedback = true
                                gestureScope.launch {
                                    delay(700)
                                    showPlaybackFeedback = false
                                }
                            }
                        },
                        onDoubleTap = {
                            if (engagement.viewerReactionType == null) onToggleLike()
                            heartBurst = true
                        }
                    )
                }
                .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = .86f)
                    )
                )
                )
            )

        if (vibe.kind == "video" || vibe.media.any { it.kind == "video" }) {
            IconButton(
                onClick = {
                    if (hasAudio != false) {
                        muted = !muted
                        playbackPreferences.edit().putBoolean("muted", muted).apply()
                    }
                },
                enabled = hasAudio != false,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp)
                    .size(34.dp)
                    .background(Color.Black.copy(alpha = .48f), CircleShape)
            ) {
                val audioMuted = muted || hasAudio == false
                Icon(
                    if (audioMuted) Icons.AutoMirrored.Filled.VolumeOff
                    else Icons.AutoMirrored.Filled.VolumeUp,
                    when {
                        hasAudio == false -> "This vibe has no audio"
                        muted -> "Unmute vibe"
                        else -> "Mute vibe"
                    },
                    tint = if (hasAudio == false) Color.White.copy(alpha = .72f) else Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
            if (playbackRate > 1f) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                    color = Color.Black.copy(alpha = .58f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FastForward, null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("2x", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            AnimatedVisibility(
                visible = showPlaybackFeedback,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    color = Color.Black.copy(alpha = .46f),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            if (paused) "Play vibe" else "Pause vibe",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }

        if (heartBurst) {
            Icon(
                Icons.Default.Favorite,
                null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(88.dp)
            )
        }

        // Swipe-to-profile hint arrow on right edge
        if (profileHintAlpha > 0.05f) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(48.dp)
                    .graphicsLayer { alpha = profileHintAlpha }
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "View profile",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (profileHintAlpha > 0.6f) {
                Text(
                    "View Profile",
                    color = Color.White.copy(alpha = profileHintAlpha),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 70.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VibeRailAction(
                label = engagement.reactionCount.toCompactMetric(),
                enabled = !engagement.reactionLoading,
                icon = if (engagement.viewerReactionType != null) {
                    Icons.Default.Favorite
                } else {
                    Icons.Default.FavoriteBorder
                },
                tint = if (engagement.viewerReactionType != null) Color(0xFF00D4C1) else Color.White,
                onClick = onToggleLike,
                onLabelClick = {
                    onLoadReactionMembers()
                    likesOpen = true
                }
            )
            Spacer(Modifier.height(12.dp))
            VibeCommentRailAction(
                label = commentCount.toCompactMetric(),
                onClick = onOpenComments
            )
            Spacer(Modifier.height(12.dp))
            VibeRailAction(
                label = "Repost",
                icon = Icons.Default.Sync,
                onClick = { repostOpen = true }
            )
            Spacer(Modifier.height(12.dp))
            VibeRailAction(
                label = "Share",
                icon = Icons.Default.Share,
                onClick = onShare
            )
            Spacer(Modifier.height(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PostOverflowActions(
                    postId = vibe.id,
                    title = vibe.title,
                    body = vibe.body,
                    isOwner = isOwner,
                    busy = busy,
                    reactionMembers = reactionMembers,
                    onLoadReactionMembers = onLoadReactionMembers,
                    onToggleSave = onToggleSave,
                    isSaved = engagement.isSaved,
                    onSearch = onSearch,
                    onCreateVibe = onCreateVibe,
                    onRefresh = onRefresh,
                    onViewInsights = onViewInsights.takeIf { isOwner },
                    onNotInterested = onNotInterested.takeUnless { isOwner },
                    onRepost = onRepost,
                    onUpdate = onUpdate,
                    onDelete = onDelete,
                    onReport = onReport,
                    iconTint = Color.White
                )
                Text("More", color = Color.White, fontSize = 11.sp)
            }
        }

        Column(
            Modifier.align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 18.dp, end = 82.dp, bottom = 24.dp)
        ) {
            Row(
                Modifier.clickable(
                    enabled = !vibe.author.isAnonymous,
                    onClick = { onOpenProfile(vibe.author.username) }
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(vibe.author.avatarUrl, vibe.author.displayName)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(
                        if (vibe.author.isAnonymous) "Anonymous" else "@${vibe.author.username}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (!vibe.author.isAnonymous) {
                        Text(vibe.author.displayName, color = Color.White.copy(alpha = .75f))
                    }
                }
            }
            val description = vibe.body.ifBlank { vibe.title }
            if (description.isNotBlank()) {
                Text(
                    description,
                    color = Color.White,
                    maxLines = if (descriptionExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable { descriptionExpanded = !descriptionExpanded }
                )
                if (description.length > 90) {
                    Text(
                        if (descriptionExpanded) "See less" else "See more",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { descriptionExpanded = !descriptionExpanded }
                    )
                }
            }
            if (descriptionExpanded) {
                vibe.location?.let {
                    Text(
                        it,
                        color = Color.White.copy(alpha = .72f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }
    }
    if (repostOpen) {
        PostRepostDialog(
            onDismiss = { repostOpen = false },
            onRepost = { quote, placement ->
                onRepost(quote, placement)
                repostOpen = false
            }
        )
    }
    if (likesOpen) {
        SocialReactionsSheet(
            reactionCount = engagement.reactionCount,
            members = reactionMembers.items,
            onOpenProfile = onOpenProfile,
            onDismiss = { likesOpen = false }
        )
    }
}

@Composable
private fun VibeRailAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    tint: Color = Color.White,
    onClick: () -> Unit,
    onLabelClick: (() -> Unit)? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BouncyIconButton(onClick = onClick, enabled = enabled) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(29.dp))
        }
        Text(
            label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = if (onLabelClick != null) Modifier.clickable(onClick = onLabelClick).padding(2.dp) else Modifier
        )
    }
}

@Composable
private fun VibeCommentRailAction(
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SocialCommentAction(
            onClick = onClick,
            tint = Color.White,
            contentDescription = "Open vibe comments"
        )
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StoryMedia(story: StoryItem, modifier: Modifier) {
    val composition = remember(story.compositionJson) {
        story.compositionJson?.takeIf { it.toByteArray().size <= 64 * 1024 }?.let {
            runCatching { StoryCompositionCodec.decodeFromString<StoryCompositionJson>(it) }
                .getOrNull()
                ?.takeIf { parsed -> parsed.version == 1 }
        }
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val transform = composition?.media
        val mediaModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = transform?.scale ?: 1f
                scaleY = transform?.scale ?: 1f
                rotationZ = transform?.rotationDegrees ?: 0f
                translationX = (transform?.offsetX ?: 0f) * widthPx
                translationY = (transform?.offsetY ?: 0f) * heightPx
            }
        if (story.mediaType == "video") {
            NativeVideo(
                story.mediaUrl,
                isActive = true,
                modifier = mediaModifier,
                crop = transform?.fit == "cover"
            )
        } else {
            RemoteImage(
                story.mediaUrl,
                mediaModifier,
                if (transform?.fit == "cover") ContentScale.Crop else ContentScale.Fit
            )
        }
        composition?.let {
            StoryCompositionLayers(it, widthPx, heightPx)
        }
    }
}

@Composable
private fun StoryCompositionLayers(
    composition: StoryCompositionJson,
    widthPx: Float,
    heightPx: Float
) {
    Canvas(Modifier.fillMaxSize()) {
        composition.layers.filter { it.type == "drawing" }.forEach { layer ->
            val points = layer.points.orEmpty()
            if (points.size > 1) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(points.first().x * size.width, points.first().y * size.height)
                    points.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
                }
                drawPath(
                    path,
                    color = Color(layer.color.toAndroidColor()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = (layer.width ?: .022f) * size.width
                    )
                )
            }
        }
    }
    composition.layers.filter { it.type == "text" }.forEach { layer ->
        Text(
            layer.text.orEmpty(),
            modifier = Modifier
                .offset {
                    IntOffset(
                        ((layer.x ?: .5f) * widthPx).toInt(),
                        ((layer.y ?: .5f) * heightPx).toInt()
                    )
                }
                .graphicsLayer {
                    translationX = -size.width / 2f
                    translationY = -size.height / 2f
                }
                .background(
                    if (layer.style == "highlight") Color.Black.copy(alpha = .55f)
                    else Color.Transparent,
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color(layer.color.toAndroidColor()),
            fontSize = ((layer.fontSize ?: .1f) * 360f).sp,
            fontWeight = if (layer.style == "bold" || layer.style == "highlight") {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },
            textAlign = when (layer.align) {
                "left" -> androidx.compose.ui.text.style.TextAlign.Left
                "right" -> androidx.compose.ui.text.style.TextAlign.Right
                else -> androidx.compose.ui.text.style.TextAlign.Center
            }
        )
    }
    composition.layers.filter { it.type == "sticker" }.forEach { layer ->
        Text(
            layer.value.orEmpty(),
            modifier = Modifier
                .offset {
                    IntOffset(
                        ((layer.x ?: .5f) * widthPx).toInt(),
                        ((layer.y ?: .5f) * heightPx).toInt()
                    )
                }
                .graphicsLayer {
                    translationX = -size.width / 2f
                    translationY = -size.height / 2f
                },
            fontSize = ((layer.size ?: .133f) * 360f).sp
        )
    }
}

private fun String?.toAndroidColor(): Int =
    runCatching { (this ?: "#FFFFFF").toColorInt() }
        .getOrDefault(android.graphics.Color.WHITE)

@Composable
private fun NativeVideo(
    url: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    crop: Boolean = false,
    muted: Boolean = false,
    playbackRate: Float = 1f,
    showScrubber: Boolean = true,
    onAudioAvailabilityChanged: (Boolean) -> Unit = {},
    onPlaybackProgress: (positionMs: Int, durationMs: Int) -> Unit = { _, _ -> }
) {
    val resolvedUrl = remember(url) { resolveRemoteMediaUrl(url) }
    var prepared by remember(resolvedUrl) { mutableStateOf(false) }
    var failed by remember(resolvedUrl) { mutableStateOf(false) }
    var activePlayer by remember(resolvedUrl) { mutableStateOf<MediaPlayer?>(null) }
    // Scrubber state
    var positionMs by remember(resolvedUrl) { mutableIntStateOf(0) }
    var durationMs by remember(resolvedUrl) { mutableIntStateOf(0) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    val currentPlaybackProgress by rememberUpdatedState(onPlaybackProgress)
    // Poll playback position every 300ms
    LaunchedEffect(prepared, isActive) {
        while (prepared && isActive) {
            activePlayer?.let {
                positionMs = runCatching { it.currentPosition }.getOrDefault(0)
                durationMs = runCatching { it.duration }.getOrDefault(0)
                currentPlaybackProgress(positionMs, durationMs)
            }
            delay(300)
        }
    }
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                (if (crop) VybFillVideoView(context) else VideoView(context)).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { video ->
                video.setOnPreparedListener { player ->
                    activePlayer = player
                    prepared = true
                    failed = false
                    player.isLooping = true
                    onAudioAvailabilityChanged(
                        player.trackInfo.any { it.trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO }
                    )
                    player.setVideoScalingMode(
                        if (crop) MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                        else MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    )
                    player.setVolume(if (muted) 0f else 1f, if (muted) 0f else 1f)
                    runCatching {
                        player.playbackParams = player.playbackParams.setSpeed(playbackRate)
                    }
                    if (isActive) video.start()
                }
                video.setOnErrorListener { _, _, _ ->
                    prepared = false
                    failed = true
                    true
                }
                if (video.tag != resolvedUrl) {
                    prepared = false
                    failed = false
                    video.tag = resolvedUrl
                    video.setVideoPath(resolvedUrl)
                }
                if (prepared) {
                    runCatching {
                        activePlayer?.setVolume(
                            if (muted) 0f else 1f,
                            if (muted) 0f else 1f
                        )
                        if (activePlayer != null) {
                            activePlayer?.playbackParams =
                                activePlayer!!.playbackParams.setSpeed(playbackRate)
                        }
                    }
                }
                if (isActive && prepared && !video.isPlaying) video.start()
                if (!isActive && video.isPlaying) video.pause()
            },
            onRelease = VideoView::stopPlayback
        )
        when {
            failed -> Text("Media unavailable", color = Color.White.copy(alpha = .65f))
            !prepared -> CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
        }
        // Scrubber bar at the very bottom
        if (showScrubber && prepared && durationMs > 0) {
            val displayFraction = if (isScrubbing) scrubFraction
                else positionMs.toFloat() / durationMs.toFloat()
            BoxWithConstraints(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(32.dp)
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(displayFraction, 0f..1f)
                        setProgress { requested ->
                            val next = requested.coerceIn(0f, 1f)
                            scrubFraction = next
                            activePlayer?.seekTo((next * durationMs).roundToInt())
                            true
                        }
                    }
                    .pointerInput(resolvedUrl, durationMs) {
                        detectTapGestures { offset ->
                            val next = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            scrubFraction = next
                            activePlayer?.seekTo((next * durationMs).roundToInt())
                        }
                    }
                    .pointerInput(resolvedUrl) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isScrubbing = true
                                scrubFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            },
                            onDragEnd = {
                                activePlayer?.seekTo((scrubFraction * durationMs).roundToInt())
                                isScrubbing = false
                            },
                            onDragCancel = { isScrubbing = false },
                            onDrag = { change, _ ->
                                change.consume()
                                scrubFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            }
                        )
                    }
            ) {
                val thumbX = maxWidth * displayFraction
                Canvas(Modifier.fillMaxSize()) {
                    // Track
                    drawLine(
                        color = android.graphics.Color.WHITE.let { Color(it).copy(alpha = 0.3f) },
                        start = Offset(0f, size.height - 4.dp.toPx()),
                        end = Offset(size.width, size.height - 4.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Progress
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, size.height - 4.dp.toPx()),
                        end = Offset(size.width * displayFraction, size.height - 4.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Thumb
                    val thumbRadius = if (isScrubbing) 7.dp.toPx() else 4.dp.toPx()
                    drawCircle(
                        color = Color.White,
                        radius = thumbRadius,
                        center = Offset(size.width * displayFraction, size.height - 4.dp.toPx())
                    )
                }
                // Time tooltip while scrubbing
                if (isScrubbing) {
                    val seekMs = (scrubFraction * durationMs).roundToInt()
                    val totalMs = durationMs
                    fun msToMmSs(ms: Int): String {
                        val s = ms / 1000
                        return "%d:%02d".format(s / 60, s % 60)
                    }
                    Box(
                        Modifier
                            .offset(x = (thumbX - 28.dp).coerceIn(0.dp, maxWidth - 56.dp))
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            "${msToMmSs(seekMs)} / ${msToMmSs(totalMs)}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteImage(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    VybRemoteImage(
        url = url,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale
    )
}

@Composable
private fun Avatar(url: String?, displayName: String) {
    SocialAvatar(
        avatarUrl = url,
        displayName = displayName,
        size = 42.dp
    )
}

@Composable
private fun InlineMessage(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color(0xFF111A2E),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(message, fontWeight = FontWeight.SemiBold)
            Text(
                "Fresh campus moments will appear in this lane.",
                color = Color(0xFF9CA9B9),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}



fun Int.toCompactMetric(): String = when {
    this >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", this / 1_000_000.0)
    this >= 10_000 -> String.format(java.util.Locale.US, "%.1fk", this / 1000.0)
    this >= 1_000 -> String.format(java.util.Locale.US, "%.1fk", this / 1000.0)
    else -> this.toString()
}
