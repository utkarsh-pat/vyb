package social.vyb.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import social.vyb.app.data.FeedPost
import social.vyb.app.data.FeedMedia
import social.vyb.app.data.VybUiState
import social.vyb.app.features.social.CommentsBottomSheet
import social.vyb.app.features.social.CreatePostComposer
import social.vyb.app.features.social.PostActionsBar
import social.vyb.app.features.social.PostOverflowActions
import social.vyb.app.features.social.PostReactionMembersDialog
import social.vyb.app.features.social.PostRepostDialog
import social.vyb.app.features.social.PostEngagementState
import social.vyb.app.features.social.SocialActionsViewModel
import social.vyb.app.features.social.SocialOperationFeedback
import social.vyb.app.features.social.PostCommunityOption
import social.vyb.app.features.hub.CampusHubRepository
import social.vyb.app.features.stories.StoriesLane
import social.vyb.app.R

@Composable
fun HomeScreen(
    state: VybUiState,
    onRefresh: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenNotifications: () -> Unit,
    onCreateStory: () -> Unit,
    socialViewModel: SocialActionsViewModel
) {
    val socialState = socialViewModel.state
    var composerOpen by remember { mutableStateOf(false) }
    var commentsPostId by remember { mutableStateOf<String?>(null) }
    var reactionsPostId by remember { mutableStateOf<String?>(null) }
    var repostPostId by remember { mutableStateOf<String?>(null) }
    var fullPostId by remember { mutableStateOf<String?>(null) }
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

    VybResponsiveFrame(Modifier.fillMaxSize()) { layout ->
    Box {
    LazyColumn(
        Modifier.fillMaxSize(),
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
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                HeaderIcon(onClick = onOpenSearch) {
                    Icon(Icons.Default.Search, "Search", tint = VybText, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.size(6.dp))
                HeaderIcon(onClick = onOpenNotifications) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        "Notifications",
                        tint = VybText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.size(6.dp))
                Box(
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
                Spacer(Modifier.size(6.dp))
                HeaderIcon(onClick = onOpenMessages) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        "Chats",
                        tint = VybText,
                        modifier = Modifier.size(20.dp)
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VybLoadingMark(width = 94.dp)
                }
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
            PostCard(
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
                compact = !layout.wide
            )
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
    if (composerOpen) {
        CreatePostComposer(
            state = socialState,
            displayName = state.displayName,
            username = state.email.substringBefore("@").ifBlank { "vybmember" },
            communities = postCommunities,
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
            }
        )
    }
    commentsPostId?.let { postId ->
        CommentsBottomSheet(
            postId = postId,
            thread = socialState.commentThreads[postId]
                ?: social.vyb.app.features.social.CommentThreadState(),
            onLoad = { socialViewModel.loadComments(postId) },
            onRetry = { socialViewModel.loadComments(postId, force = true) },
            onAddComment = { text, parentCommentId, done ->
                socialViewModel.addComment(
                    postId,
                    text,
                    parentCommentId = parentCommentId,
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
        state.feed.firstOrNull { it.id == postId }?.let { post ->
            Dialog(
                onDismissRequest = { fullPostId = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(VybBackground)
                        .padding(vertical = 12.dp)
                ) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            PostCard(
                                post = post,
                                engagement = socialState.engagements[post.id]
                                    ?: PostEngagementState(),
                                commentCount = socialState.commentThreads[post.id]
                                    ?.items?.size ?: post.comments,
                                isOwner = post.viewerCanManage,
                                busy = post.id in socialState.busyPostIds,
                                reactionMembers = socialState.reactionMembers[post.id]
                                    ?: social.vyb.app.features.social.ReactionMembersState(),
                                onReaction = {
                                    socialViewModel.toggleReaction(post.id, it)
                                },
                                onComments = {
                                    fullPostId = null
                                    commentsPostId = post.id
                                },
                                onSave = { socialViewModel.toggleSave(post.id) },
                                onOpenReactions = {
                                    fullPostId = null
                                    reactionsPostId = post.id
                                    socialViewModel.loadReactionMembers(post.id)
                                },
                                onRepost = { quote, placement ->
                                    socialViewModel.repost(post.id, quote, placement) {
                                        onRefresh()
                                    }
                                },
                                onOpenRepost = {
                                    fullPostId = null
                                    repostPostId = post.id
                                },
                                onViewPost = {},
                                onUpdate = { title, body ->
                                    socialViewModel.updatePost(post.id, title, body) { onRefresh() }
                                },
                                onDelete = {
                                    socialViewModel.deletePost(post.id) {
                                        fullPostId = null
                                        onRefresh()
                                    }
                                },
                                onReport = { reason ->
                                    socialViewModel.report("post", post.id, reason)
                                },
                                expanded = true,
                                compact = false
                            )
                        }
                    }
                    IconButton(
                        onClick = { fullPostId = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(18.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = .68f))
                    ) {
                        Icon(Icons.Default.Close, "Close full post", tint = Color.White)
                    }
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

@Composable
private fun HeaderIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            color = VybPanelLifted,
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    }
}

@Composable
private fun PostCard(
    post: FeedPost,
    engagement: PostEngagementState,
    commentCount: Int,
    isOwner: Boolean,
    busy: Boolean,
    reactionMembers: social.vyb.app.features.social.ReactionMembersState,
    onReaction: (String) -> Unit,
    onComments: () -> Unit,
    onSave: () -> Unit,
    onOpenReactions: () -> Unit,
    onRepost: (String, String) -> Unit,
    onOpenRepost: () -> Unit,
    onViewPost: () -> Unit,
    onUpdate: (String, String) -> Unit,
    onDelete: () -> Unit,
    onReport: (String) -> Unit,
    expanded: Boolean = false,
    compact: Boolean = true
) {
    Card(
        Modifier
            .fillMaxWidth()
            .widthIn(max = if (expanded) 900.dp else 720.dp)
            .padding(horizontal = if (compact) 0.dp else 14.dp, vertical = 6.dp)
            .clickable(onClick = onViewPost),
        shape = RoundedCornerShape(if (compact) 0.dp else 20.dp),
        colors = CardDefaults.cardColors(containerColor = VybPanel),
        border = androidx.compose.foundation.BorderStroke(1.dp, VybBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(if (compact) 14.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(if (compact) 36.dp else 44.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = .18f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.avatarUrl.isNullOrBlank() || post.isAnonymous) {
                        Text(post.author.take(1), fontWeight = FontWeight.Bold)
                    } else {
                        VybRemoteImage(
                            url = post.avatarUrl,
                            contentDescription = "${post.author} profile photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
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
                    onRepost = onRepost,
                    onUpdate = onUpdate,
                    onDelete = onDelete,
                    onReport = onReport
                )
            }
            if (post.title.isNotBlank() && post.title != post.body) {
                Text(
                    post.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(post.body, style = MaterialTheme.typography.bodyLarge)
            if (post.media.isNotEmpty()) {
                FeedMediaCarousel(
                    media = post.media,
                    author = post.author,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable(onClick = onViewPost)
                )
            }
            Spacer(Modifier.height(if (compact) 12.dp else 16.dp))
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
                onToggleSave = onSave
            )
        }
    }
}

@Composable
private fun FeedMediaCarousel(
    media: List<FeedMedia>,
    author: String,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { media.size })
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(14.dp))
            .background(VybPanelLifted)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { page -> "${media[page].url}-$page" }
        ) { page ->
            val item = media[page]
            if (item.kind == "video") {
                VybRemoteVideo(
                    url = item.url,
                    contentDescription = "$author video ${page + 1} of ${media.size}",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                VybRemoteImage(
                    url = item.url,
                    contentDescription = "$author post image ${page + 1} of ${media.size}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        if (media.size > 1) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                color = Color.Black.copy(alpha = .68f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    "${pagerState.currentPage + 1}/${media.size}",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(media.size) { index ->
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) {
                                    Color.White
                                } else {
                                    Color.White.copy(alpha = .45f)
                                }
                            )
                    )
                }
            }
        }
    }
}
