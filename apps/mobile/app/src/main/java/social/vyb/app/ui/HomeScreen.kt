package social.vyb.app.ui

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import social.vyb.app.data.FeedPost
import social.vyb.app.data.FeedMedia
import social.vyb.app.data.VybUiState
import social.vyb.app.features.social.CreatePostComposer
import social.vyb.app.features.social.PostActionsBar
import social.vyb.app.features.social.PostOverflowActions
import social.vyb.app.features.social.PostReactionMembersDialog
import social.vyb.app.features.social.PostRepostDialog
import social.vyb.app.features.social.PostEngagementState
import social.vyb.app.features.social.SocialActionsViewModel
import social.vyb.app.features.social.SocialActionsRepository
import social.vyb.app.features.social.SocialPost
import social.vyb.app.features.social.SocialThreadSheet
import social.vyb.app.features.social.SocialOperationFeedback
import social.vyb.app.features.social.SocialAvatar
import social.vyb.app.features.social.SocialPostLightbox
import social.vyb.app.features.social.SocialShareSheet
import social.vyb.app.features.social.PostCommunityOption
import social.vyb.app.features.social.ContentMeasurementRepository
import social.vyb.app.features.hub.CampusHubRepository
import social.vyb.app.features.media.MediaComposerViewModel
import social.vyb.app.features.stories.StoriesLane
import social.vyb.app.R

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: VybUiState,
    initialPostId: String? = null,
    onInitialPostConsumed: () -> Unit = {},
    onRefresh: () -> Unit,
    onReconcile: () -> Unit,
    onApplyPendingFeedChanges: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenNotifications: () -> Unit,
    onCreateStory: () -> Unit,
    onOpenProfile: (String) -> Unit,
    socialViewModel: SocialActionsViewModel
) {
    val socialState = socialViewModel.state
    val mediaComposerViewModel: MediaComposerViewModel = viewModel(key = "home_media_composer")
    val mediaComposerState by mediaComposerViewModel.uiState.collectAsStateWithLifecycle()
    var composerOpen by remember { mutableStateOf(false) }
    var composerNotice by remember { mutableStateOf<String?>(null) }
    var commentsPostId by remember { mutableStateOf<String?>(null) }
    var reactionsPostId by remember { mutableStateOf<String?>(null) }
    var repostPostId by remember { mutableStateOf<String?>(null) }
    var sharePostId by remember { mutableStateOf<String?>(null) }
    var fullPostId by remember { mutableStateOf<String?>(null) }
    val feedListState = rememberLazyListState()
    var showPostFab by remember { mutableStateOf(true) }
    val feedLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(feedLifecycleOwner, onReconcile) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onReconcile()
        }
        feedLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { feedLifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(onReconcile) {
        while (true) {
            kotlinx.coroutines.delay(45_000)
            onReconcile()
        }
    }
    LaunchedEffect(state.hasPendingFeedChanges) {
        if (
            state.hasPendingFeedChanges &&
            feedListState.firstVisibleItemIndex == 0 &&
            feedListState.firstVisibleItemScrollOffset <= 56
        ) {
            onApplyPendingFeedChanges()
        }
    }
    LaunchedEffect(composerNotice) {
        if (composerNotice != null) {
            kotlinx.coroutines.delay(2_200)
            composerNotice = null
        }
    }
    LaunchedEffect(mediaComposerState.publishedItem?.id) {
        if (mediaComposerState.publishedItem != null) {
            onRefresh()
            mediaComposerViewModel.clearMessage()
        }
    }
    LaunchedEffect(feedListState) {
        var previousIndex = feedListState.firstVisibleItemIndex
        var previousOffset = feedListState.firstVisibleItemScrollOffset
        snapshotFlow {
            feedListState.firstVisibleItemIndex to feedListState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            val awayFromTop = index > 0 || offset > 56
            val movingDownFeed = index > previousIndex ||
                (index == previousIndex && offset > previousOffset + 2)
            val movingTowardTop = index < previousIndex ||
                (index == previousIndex && offset < previousOffset - 2)
            showPostFab = when {
                !awayFromTop -> true
                movingDownFeed -> false
                movingTowardTop -> true
                else -> showPostFab
            }
            previousIndex = index
            previousOffset = offset
        }
    }
    LaunchedEffect(feedListState, state.feed.size, state.feedNextCursor) {
        snapshotFlow { feedListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { lastVisibleIndex ->
                if (
                    state.feedNextCursor != null &&
                    !state.feedLoadingMore &&
                    lastVisibleIndex >= state.feed.lastIndex - 3
                ) {
                    onLoadMore()
                }
            }
    }
    var resolvedNotificationPost by remember { mutableStateOf<FeedPost?>(null) }
    BackHandler(
        enabled = fullPostId != null || commentsPostId != null ||
            reactionsPostId != null || repostPostId != null || sharePostId != null || composerOpen
    ) {
        when {
            sharePostId != null -> sharePostId = null
            fullPostId != null -> fullPostId = null
            commentsPostId != null -> commentsPostId = null
            reactionsPostId != null -> reactionsPostId = null
            repostPostId != null -> repostPostId = null
            composerOpen -> composerOpen = false
        }
    }
    val socialRepository = remember { SocialActionsRepository() }
    val measurementRepository = remember { ContentMeasurementRepository() }
    val measurementLifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(initialPostId, state.feed) {
        val requestedId = initialPostId ?: return@LaunchedEffect
        resolvedNotificationPost = state.feed.firstOrNull { it.id == requestedId }
            ?: runCatching { socialRepository.loadPost(requestedId).toFeedPost() }.getOrNull()
        if (resolvedNotificationPost != null) fullPostId = requestedId
        onInitialPostConsumed()
    }
    val campusHubRepository = remember { CampusHubRepository() }
    var postCommunities by remember { mutableStateOf<List<PostCommunityOption>>(emptyList()) }
    LaunchedEffect(composerOpen) {
        if (composerOpen) {
            postCommunities = runCatching { campusHubRepository.loadCommunities() }
                .getOrDefault(emptyList())
                .filter { it.isMember && it.membershipStatus != "left" }
                .map { PostCommunityOption(id = it.id, name = it.name) }
        }
    }

    SharedTransitionLayout {
        val sharedTransitionScope = this@SharedTransitionLayout
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = true) {
                val feedAnimatedVisibilityScope = this@AnimatedVisibility
                VybResponsiveFrame(Modifier.fillMaxSize()) { layout ->
    Box {
    PullToRefreshBox(
        isRefreshing = state.feedLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            if (state.feedLoading) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp),
                    color = VybPanel.copy(alpha = .96f),
                    shape = CircleShape
                ) {
                    VybLoadingMark(Modifier.padding(2.dp), width = 42.dp)
                }
            }
        }
    ) {
    LazyColumn(
        state = feedListState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(
                        start = layout.horizontalPadding,
                        end = layout.horizontalPadding,
                        top = 4.dp,
                        bottom = if (layout.compactHeight) 6.dp else 10.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    Image(
                        painter = painterResource(R.drawable.vyb_logo),
                        contentDescription = "Vyb",
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                HeaderIcon(onClick = onOpenSearch) {
                    Icon(Icons.Default.Search, "Search", tint = VybText, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.size(6.dp))
                HeaderIcon(onClick = onOpenNotifications) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        "Notifications",
                        tint = VybText,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Spacer(Modifier.size(6.dp))
                if (layout.wide) Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(VybIndigo, Color(0xFF08C7D4))
                            )
                        )
                        .clickable { composerOpen = !composerOpen }
                ) {
                    Row(
                        Modifier.height(38.dp).padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text(
                            "Post",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 5.dp)
                        )
                    }
                }
                if (layout.wide) Spacer(Modifier.size(6.dp))
                HeaderIcon(onClick = onOpenMessages) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        "Chats",
                        tint = VybText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().widthIn(max = 720.dp)) {
                StoriesLane(
                    showLoadingIndicator = !state.feedLoading,
                    onCreateStory = onCreateStory
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        if (state.feedLoading) {
            item {
                FeedSkeleton(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp)
                )
            }
        }
        state.feedError?.let { error ->
            item {
                VybEmptyState(
                    icon = Icons.Default.AutoAwesome,
                    title = "Feed unavailable",
                    body = error,
                    actionLabel = "Try again",
                    onAction = onRefresh,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                )
            }
        }
        if (!state.feedLoading && state.feedError == null && state.feed.isEmpty()) {
            item {
                VybEmptyState(
                    icon = Icons.Default.AutoAwesome,
                    title = "Your campus feed is quiet",
                    body = "New posts from your verified campus will appear here.",
                    actionLabel = "Refresh",
                    onAction = onRefresh,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                )
            }
        }
        items(state.feed, key = { it.id }) { post ->
            val measurementView = LocalView.current
            var measurementVisible by remember(post.id) { mutableStateOf(false) }
            LaunchedEffect(post.id, measurementVisible) {
                if (!measurementVisible) return@LaunchedEffect
                kotlinx.coroutines.delay(500)
                if (!measurementLifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@LaunchedEffect
                measurementRepository.record(post.id, "impression", visibleMs = 500)
                kotlinx.coroutines.delay(500)
                if (!measurementLifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@LaunchedEffect
                measurementRepository.record(post.id, "qualified_view", visibleMs = 1000, flush = true)
            }
            LaunchedEffect(
                post.id,
                post.likes,
                post.viewerReactionType,
                post.savedCount,
                post.isSaved
            ) {
                socialViewModel.seedPost(
                    postId = post.id,
                    reactionCount = post.likes,
                    viewerReactionType = post.viewerReactionType,
                    savedCount = post.savedCount,
                    isSaved = post.isSaved
                )
            }
            with(sharedTransitionScope) {
                Box(
                    Modifier.onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInWindow()
                        val viewportBottom = measurementView.rootView.height.toFloat()
                        val visibleHeight = (minOf(bounds.bottom, viewportBottom) - maxOf(bounds.top, 0f)).coerceAtLeast(0f)
                        measurementVisible = bounds.height > 0f && visibleHeight / bounds.height >= .5f
                    }
                ) {
                PostCard(
                    animatedVisibilityScope = feedAnimatedVisibilityScope,
                    post = post,
                    engagement = socialState.engagements[post.id] ?: PostEngagementState(),
                    commentCount = socialState.commentThreads[post.id]
                        ?.takeIf { it.loaded }
                        ?.items
                        ?.size
                        ?: post.comments,
                    isOwner = post.viewerCanManage,
                    busy = post.id in socialState.busyPostIds,
                    reactionMembers = socialState.reactionMembers[post.id]
                        ?: social.vyb.app.features.social.ReactionMembersState(),
                    onReaction = { type -> socialViewModel.toggleReaction(post.id, type) },
                    onComments = { commentsPostId = post.id },
                    onSave = { socialViewModel.toggleSave(post.id) },
                    onShare = { sharePostId = post.id },
                    onOpenReactions = {
                        reactionsPostId = post.id
                        socialViewModel.loadReactionMembers(post.id)
                    },
                    onRepost = { quote, placement ->
                        socialViewModel.repost(
                            post.id,
                            quote,
                            placement,
                            onCreated = { onRefresh() }
                        )
                    },
                    onOpenRepost = { repostPostId = post.id },
                    onViewPost = { fullPostId = post.id },
                    onUpdate = { title, body ->
                        socialViewModel.updatePost(post.id, title, body) { onRefresh() }
                    },
                    onDelete = { socialViewModel.deletePost(post.id, onRefresh) },
                    onReport = { reason -> socialViewModel.report("post", post.id, reason) },
                    onViewInsights = {
                        socialViewModel.loadContentInsights(post.id)
                    },
                    onNotInterested = {
                        socialViewModel.hideRecommendation(post.id, onRefresh)
                    },
                    compact = !layout.wide
                )
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
    AnimatedVisibility(
        visible = state.hasPendingFeedChanges,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 76.dp)
    ) {
        Surface(
            modifier = Modifier.clickable(onClick = onApplyPendingFeedChanges),
            color = VybIndigo.copy(alpha = .94f),
            shape = RoundedCornerShape(999.dp),
            shadowElevation = 8.dp
        ) {
            Text(
                "New posts",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
            )
        }
    }
    AnimatedVisibility(
        visible = (showPostFab || mediaComposerState.isPublishing || socialState.creatingPost) &&
            !composerOpen && fullPostId == null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 18.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            FloatingActionButton(
                onClick = { if (!mediaComposerState.isPublishing && !socialState.creatingPost) composerOpen = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                VybIndigo.copy(alpha = .72f),
                                Color(0xFF08C7D4).copy(alpha = .58f)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = .22f), CircleShape),
                containerColor = Color.Transparent,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create post")
            }
            if (mediaComposerState.isPublishing || socialState.creatingPost) {
                CircularProgressIndicator(
                    progress = {
                        if (mediaComposerState.isPublishing) mediaComposerState.progress.coerceIn(0f, 1f)
                        else .18f
                    },
                    modifier = Modifier.size(62.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = .18f),
                    strokeWidth = 3.dp
                )
            }
        }
    }
    composerNotice?.let { notice ->
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp),
            color = Color.Black.copy(alpha = .78f),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(
                notice,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
    if (composerOpen) {
        CreatePostComposer(
            state = socialState,
            displayName = state.displayName,
            username = state.email.substringBefore("@").ifBlank { "vybmember" },
            communities = postCommunities,
            mediaViewModel = mediaComposerViewModel,
            onDraftSaved = { composerNotice = "saved as draft" },
            onPublishingStarted = { composerOpen = false },
            onDismiss = { if (!socialState.creatingPost) composerOpen = false },
            onPublish = {
                    text,
                    anonymous,
                    allowAnonymousComments,
                    visibility,
                    communityId,
                    onPublished ->
                socialViewModel.createPost(
                    text = text,
                    isAnonymous = anonymous,
                    allowAnonymousComments = allowAnonymousComments,
                    visibility = visibility,
                    communityId = communityId
                ) {
                    onPublished()
                    onRefresh()
                }
            },
            onSchedulePost = {
                    publishAtMillis,
                    text,
                    anonymous,
                    allowAnonymousComments,
                    visibility,
                    communityId,
                    onPublished ->
                socialViewModel.schedulePost(
                    publishAtMillis = publishAtMillis,
                    text = text,
                    isAnonymous = anonymous,
                    allowAnonymousComments = allowAnonymousComments,
                    visibility = visibility,
                    communityId = communityId
                ) {
                    onPublished()
                    onRefresh()
                }
                composerNotice = "post scheduled"
            }
        )
    }
    commentsPostId?.let { postId ->
        SocialThreadSheet(
            postId = postId,
            thread = socialState.commentThreads[postId]
                ?: social.vyb.app.features.social.CommentThreadState(),
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
    reactionsPostId?.let { postId ->
        PostReactionMembersDialog(
            state = socialState.reactionMembers[postId]
                ?: social.vyb.app.features.social.ReactionMembersState(),
            onDismiss = { reactionsPostId = null }
        )
    }
    repostPostId?.let { postId ->
        PostRepostDialog(
            onDismiss = { repostPostId = null },
            onRepost = { quote, placement ->
                socialViewModel.repost(postId, quote, placement) {
                    repostPostId = null
                    onRefresh()
                }
            }
        )
    }
    fullPostId?.let { postId ->
        (state.feed.firstOrNull { it.id == postId }
            ?: resolvedNotificationPost?.takeIf { it.id == postId })?.let { post ->
            AnimatedVisibility(
                visible = fullPostId != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                val lightboxScope: AnimatedVisibilityScope = this
                with(sharedTransitionScope) {
                    SocialPostLightbox(
                        animatedVisibilityScope = lightboxScope,
                        post = post,
                        engagement = socialState.engagements[post.id] ?: PostEngagementState(),
                        commentCount = socialState.commentThreads[post.id]?.items?.size ?: post.comments,
                        isOwner = post.viewerCanManage,
                        busy = post.id in socialState.busyPostIds,
                        reactionMembers = socialState.reactionMembers[post.id]
                            ?: social.vyb.app.features.social.ReactionMembersState(),
                        onReaction = { socialViewModel.toggleReaction(post.id, it) },
                        onComments = { fullPostId = null; commentsPostId = post.id },
                        onSave = { socialViewModel.toggleSave(post.id) },
                        onShare = { sharePostId = post.id },
                        onOpenReactions = {
                            fullPostId = null
                            reactionsPostId = post.id
                            socialViewModel.loadReactionMembers(post.id)
                        },
                        onRepost = { quote, placement ->
                            socialViewModel.repost(post.id, quote, placement) { onRefresh() }
                        },
                        onOpenRepost = { fullPostId = null; repostPostId = post.id },
                        onOpenProfile = onOpenProfile,
                        onUpdate = { title, body -> socialViewModel.updatePost(post.id, title, body) { onRefresh() } },
                        onDelete = {
                            socialViewModel.deletePost(post.id) { fullPostId = null; onRefresh() }
                        },
                        onReport = { reason -> socialViewModel.report("post", post.id, reason) },
                        onViewInsights = { socialViewModel.loadContentInsights(post.id) },
                        onNotInterested = {
                            socialViewModel.hideRecommendation(post.id) {
                                fullPostId = null
                                onRefresh()
                            }
                        },
                        onDismiss = { fullPostId = null }
                    )
                }
            }
        }
    }
    SocialOperationFeedback(
        state = socialState,
        onDismissError = socialViewModel::clearOperationError,
        onDismissNotice = socialViewModel::clearOperationNotice
    )
    }
    }
            }
        }
    }
    sharePostId?.let { postId ->
        (state.feed.firstOrNull { it.id == postId }
            ?: resolvedNotificationPost?.takeIf { it.id == postId })?.let { post ->
            SocialShareSheet(
                postId = post.id,
                postKind = post.kind,
                postPlacement = "feed",
                postTitle = post.title,
                postBody = post.body,
                postMediaUrl = post.media.firstOrNull()?.url,
                authorDisplayName = post.author,
                authorUsername = post.handle.removePrefix("@"),
                authorIsAnonymous = post.isAnonymous,
                onDismiss = { sharePostId = null },
                onAddToStory = {
                    sharePostId = null
                    onCreateStory()
                },
            )
        }
    }
}
}

@Composable
private fun HeaderIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            color = VybPanelLifted,
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.PostCard(
    animatedVisibilityScope: AnimatedVisibilityScope,
    post: FeedPost,
    engagement: PostEngagementState,
    commentCount: Int,
    isOwner: Boolean,
    busy: Boolean,
    reactionMembers: social.vyb.app.features.social.ReactionMembersState,
    onReaction: (String) -> Unit,
    onComments: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onOpenReactions: () -> Unit,
    onRepost: (String, String) -> Unit,
    onOpenRepost: () -> Unit,
    onViewPost: () -> Unit,
    onUpdate: (String, String) -> Unit,
    onDelete: () -> Unit,
    onReport: (String) -> Unit,
    onViewInsights: () -> Unit,
    onNotInterested: () -> Unit,
    expanded: Boolean = false,
    compact: Boolean = true
) {
    Card(
        Modifier
            .fillMaxWidth()
            .widthIn(max = if (expanded) 900.dp else 720.dp)
            .padding(horizontal = if (compact) 0.dp else 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(if (compact) 0.dp else 20.dp),
        colors = CardDefaults.cardColors(containerColor = VybPanel),
        border = androidx.compose.foundation.BorderStroke(1.dp, VybBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        val contentPadding = if (compact) 14.dp else 16.dp
        Column {
            Column(Modifier.padding(start = contentPadding, top = contentPadding, end = contentPadding)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                SocialAvatar(
                    avatarUrl = post.avatarUrl.takeUnless { post.isAnonymous },
                    displayName = if (post.isAnonymous) "Anonymous Vyber" else post.author,
                    size = if (compact) 36.dp else 44.dp
                )
                Column(Modifier.padding(start = if (compact) 9.dp else 11.dp).weight(1f)) {
                    Text(post.author, fontWeight = FontWeight.Bold)
                    Text("${post.handle} · ${post.time}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PostOverflowActions(
                    postId = post.id,
                    title = post.title,
                    body = post.body,
                    isOwner = isOwner,
                    busy = busy,
                    reactionMembers = reactionMembers,
                    onLoadReactionMembers = onOpenReactions,
                    onViewPost = onViewPost,
                    onViewInsights = onViewInsights.takeIf { isOwner },
                    onNotInterested = onNotInterested.takeUnless { isOwner },
                    onRepost = onRepost,
                    onUpdate = onUpdate,
                    onDelete = onDelete,
                    onReport = onReport
                )
                }
                val displayTitle = post.title.takeUnless {
                    it.isBlank() || it == post.body || it.equals("Campus update", ignoreCase = true) ||
                        it.startsWith("Repost ", ignoreCase = true) ||
                        it.startsWith("Quote repost ", ignoreCase = true)
                }
                displayTitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (post.body.isNotBlank()) {
                    Text(post.body, style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (post.media.isNotEmpty()) {
                FeedMediaCarousel(
                    animatedVisibilityScope = animatedVisibilityScope,
                    postId = post.id,
                    media = post.media,
                    author = post.author,
                    onOpen = onViewPost,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            Column(Modifier.padding(horizontal = contentPadding)) {
                Spacer(Modifier.height(if (post.media.isEmpty()) 12.dp else 2.dp))
                PostActionsBar(
                    postId = post.id,
                    engagement = engagement,
                    commentCount = commentCount,
                    title = post.title,
                    body = post.body,
                    onToggleReaction = onReaction,
                    onOpenReactions = onOpenReactions,
                    onOpenComments = onComments,
                    onRepost = onOpenRepost,
                    onToggleSave = onSave,
                    onShare = onShare,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.FeedMediaCarousel(
    animatedVisibilityScope: AnimatedVisibilityScope,
    postId: String,
    media: List<FeedMedia>,
    author: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { media.size })
    val rootView = LocalView.current
    var isMostlyVisible by remember(postId) { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                val viewportBottom = rootView.rootView.height.toFloat()
                val visibleHeight = (
                    minOf(bounds.bottom, viewportBottom) - maxOf(bounds.top, 0f)
                ).coerceAtLeast(0f)
                val next = bounds.height > 0f && visibleHeight / bounds.height >= .58f
                if (next != isMostlyVisible) isMostlyVisible = next
            }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .background(VybPanelLifted)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { page -> "${media[page].url}-$page" }
            ) { page ->
                val item = media[page]
                Box(Modifier.fillMaxSize().clickable(onClick = onOpen)) {
                    if (item.kind == "video") {
                        VybRemoteVideo(
                            url = item.url,
                            contentDescription = "$author video ${page + 1} of ${media.size}",
                            modifier = Modifier.fillMaxSize().sharedElement(sharedContentState = rememberSharedContentState(key = "video-${postId}-${page}"), animatedVisibilityScope = animatedVisibilityScope),
                            autoPlay = isMostlyVisible && pagerState.currentPage == page,
                            crop = true,
                            muted = true
                        )
                    } else {
                        VybRemoteImage(
                            url = item.url,
                            contentDescription = "$author post image ${page + 1} of ${media.size}",
                            modifier = Modifier.fillMaxSize().sharedElement(sharedContentState = rememberSharedContentState(key = "image-${postId}-${page}"), animatedVisibilityScope = animatedVisibilityScope),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            if (media.size > 1) {
                Icon(
                    imageVector = Icons.Outlined.FilterNone,
                    contentDescription = "${media.size} media items",
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(18.dp),
                    tint = Color.White
                )
            }
        }
        if (media.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(media.size) { index ->
                    Box(
                        Modifier
                            .padding(horizontal = 2.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .34f)
                                }
                            )
                    )
                }
            }
        }
}
}

private fun SocialPost.toFeedPost(): FeedPost {
    val anonymous = author.isAnonymous
    val resolvedTitle = normalizeFeedPostTitle(title)
    val resolvedMedia = media.ifEmpty {
        mediaUrl?.takeIf(String::isNotBlank)?.let { url ->
            listOf(
                social.vyb.app.features.social.SocialMediaAsset(
                    url = url,
                    kind = if (kind == "video") "video" else "image"
                )
            )
        }.orEmpty()
    }
    return FeedPost(
        id = id,
        authorUserId = author.userId,
        author = if (anonymous) "Anonymous" else author.displayName,
        handle = if (anonymous) "@anonymous" else "@${author.username}",
        avatarUrl = author.avatarUrl,
        time = formatSocialAge(createdAt),
        title = resolvedTitle,
        body = body.ifBlank { resolvedTitle },
        kind = kind,
        media = resolvedMedia.map { FeedMedia(it.url, it.kind, it.mimeType) },
        location = location,
        visibility = "public",
        isAnonymous = anonymous,
        likes = reactions,
        comments = comments,
        savedCount = savedCount,
        isSaved = isSaved,
        viewerReactionType = viewerReactionType,
        viewerCanManage = viewerCanManage,
        category = location?.takeIf(String::isNotBlank)
            ?: kind.replaceFirstChar { it.uppercase() }
    )
}
