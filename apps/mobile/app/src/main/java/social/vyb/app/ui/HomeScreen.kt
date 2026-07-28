package social.vyb.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import social.vyb.app.data.FeedPost
import social.vyb.app.data.VybRepository
import social.vyb.app.data.VybUiState
import social.vyb.app.features.social.CommentsBottomSheet
import social.vyb.app.features.social.CreatePostComposer
import social.vyb.app.features.social.PostActionsBar
import social.vyb.app.features.social.PostEngagementState
import social.vyb.app.features.social.SocialActionsViewModel
import social.vyb.app.features.social.PostCommunityOption
import social.vyb.app.features.hub.CampusHubRepository
import social.vyb.app.features.stories.StoriesLane
import social.vyb.app.ui.theme.Lime

@Composable
fun HomeScreen(
    state: VybUiState,
    repository: VybRepository,
    onRefresh: () -> Unit,
    onOpenMessages: () -> Unit
) {
    val socialViewModel: SocialActionsViewModel = viewModel()
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
                HeaderIcon(onClick = onOpenMessages) {
                    Icon(Icons.Default.ChatBubbleOutline, "Chats", tint = VybText)
                }
                Spacer(Modifier.size(6.dp))
                HeaderIcon(onClick = {}) {
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
        if (false) item {
            Row(
                Modifier.fillMaxWidth().padding(20.dp, 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Good evening, ${state.displayName.split(" ").first()} 👋",
                        style = MaterialTheme.typography.titleLarge)
                    Text(state.college, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.NotificationsNone, "Notifications")
                }
                IconButton(onClick = { composerOpen = !composerOpen }) {
                    Icon(Icons.Default.Add, "Create post")
                }
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
            LaunchedEffect(post.id, post.likes) {
                socialViewModel.seedPost(post.id, post.likes)
            }
            PostCard(
                post = post,
                engagement = socialState.engagements[post.id] ?: PostEngagementState(),
                onLike = { socialViewModel.toggleReaction(post.id) },
                onComments = { commentsPostId = post.id },
                onSave = { socialViewModel.toggleSave(post.id) }
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
            onAddComment = { text, done ->
                socialViewModel.addComment(postId, text, onAdded = done)
            },
            onDismiss = { commentsPostId = null }
        )
    }
    }
    }
}

@Composable
private fun HeaderIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
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
    onLike: () -> Unit,
    onComments: () -> Unit,
    onSave: () -> Unit
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
                ) { Text(post.author.take(1), fontWeight = FontWeight.Bold) }
                Column(Modifier.padding(start = 11.dp).weight(1f)) {
                    Text(post.author, fontWeight = FontWeight.Bold)
                    Text("${post.handle} · ${post.time}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.MoreHoriz, "More")
            }
            AssistChip(onClick = {}, label = { Text(post.category) })
            Text(post.body, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            PostActionsBar(
                postId = post.id,
                engagement = engagement,
                commentCount = post.comments,
                onToggleReaction = onLike,
                onOpenComments = onComments,
                onToggleSave = onSave
            )
        }
    }
}

@Composable
private fun Action(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, label, Modifier.size(20.dp))
        Text(label, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodyMedium)
    }
}
