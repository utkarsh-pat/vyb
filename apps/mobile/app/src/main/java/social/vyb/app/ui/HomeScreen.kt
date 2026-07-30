package social.vyb.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import social.vyb.app.data.FeedPost
import social.vyb.app.data.FeedMedia
import social.vyb.app.data.VybUiState
import social.vyb.app.features.social.CommentsBottomSheet
import social.vyb.app.features.social.CreatePostComposer
import social.vyb.app.features.social.PostActionsBar
import social.vyb.app.features.social.PostOverflowActions
import social.vyb.app.features.social.PostEngagementState
import social.vyb.app.features.social.SocialActionsViewModel
import social.vyb.app.features.social.SocialOperationFeedback
import social.vyb.app.features.social.PostCommunityOption
import social.vyb.app.features.hub.CampusHubRepository
import social.vyb.app.features.stories.StoriesLane

@Composable
fun HomeScreen(
    state: VybUiState,
    onRefresh: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenNotifications: () -> Unit,
    socialViewModel: SocialActionsViewModel
) {
    val socialState = socialViewModel.state
    var composerOpen by remember { mutableStateOf(false) }
    var commentsPostId by remember { mutableStateOf<String?>(null) }
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
                VybBrandLockup(
                    Modifier.weight(1f),
                    compact = true,
                    showSubtitle = !layout.compactWidth
                )
                HeaderIcon(onClick = onOpenSearch) {
                    Icon(Icons.Default.Search, "Search", tint = VybText)
                }
                Spacer(Modifier.size(6.dp))
                HeaderIcon(onClick = onOpenMessages) {
                    Icon(Icons.Default.ChatBubbleOutline, "Chats", tint = VybText)
                }
                Spacer(Modifier.size(6.dp))
                HeaderIcon(onClick = onOpenNotifications) {
                    Icon(Icons.Default.NotificationsNone, "Notifications", tint = VybText)
                }
                Spacer(Modifier.size(6.dp))
                HeaderIcon(onClick = { composerOpen = !composerOpen }) {
                    Icon(Icons.Default.Add, "Create post", tint = VybText)
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(
                        horizontal = layout.horizontalPadding,
                        vertical = if (layout.compactHeight) 2.dp else 6.dp
                    )
            ) {
                Text(
                    "Good evening, ${state.displayName.substringBefore(" ")}",
                    color = VybText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(state.college, color = VybMuted)
            }
        }
        item {
            Column(Modifier.fillMaxWidth().widthIn(max = 720.dp)) {
                StoriesLane(showLoadingIndicator = !state.feedLoading)
                Spacer(Modifier.height(18.dp))
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
                onLike = { socialViewModel.toggleReaction(post.id) },
                onComments = { commentsPostId = post.id },
                onSave = { socialViewModel.toggleSave(post.id) },
                onLoadReactionMembers = { socialViewModel.loadReactionMembers(post.id) },
                onRepost = { quote ->
                    socialViewModel.repost(post.id, quote, onCreated = { onRefresh() })
                },
                onUpdate = { title, body ->
                    socialViewModel.updatePost(post.id, title, body) { onRefresh() }
                },
                onDelete = { socialViewModel.deletePost(post.id, onRefresh) },
                onReport = { reason -> socialViewModel.report("post", post.id, reason) }
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
            .clip(CircleShape)
            .background(VybPanelLifted)
    ) {
        IconButton(onClick = onClick, content = content)
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
    onLike: () -> Unit,
    onComments: () -> Unit,
    onSave: () -> Unit,
    onLoadReactionMembers: () -> Unit,
    onRepost: (String) -> Unit,
    onUpdate: (String, String) -> Unit,
    onDelete: () -> Unit,
    onReport: (String) -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VybPanel),
        border = androidx.compose.foundation.BorderStroke(1.dp, VybBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
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
                Column(Modifier.padding(start = 11.dp).weight(1f)) {
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
                    onLoadReactionMembers = onLoadReactionMembers,
                    onRepost = onRepost,
                    onUpdate = onUpdate,
                    onDelete = onDelete,
                    onReport = onReport
                )
            }
            Surface(
                modifier = Modifier.padding(vertical = 8.dp),
                shape = RoundedCornerShape(50),
                color = VybIndigo.copy(alpha = .16f)
            ) {
                Text(
                    post.category,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = VybText
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
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            PostActionsBar(
                postId = post.id,
                engagement = engagement,
                commentCount = commentCount,
                onToggleReaction = onLike,
                onOpenComments = onComments,
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
