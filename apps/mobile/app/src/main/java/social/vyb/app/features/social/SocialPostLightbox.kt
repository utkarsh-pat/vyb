@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package social.vyb.app.features.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import social.vyb.app.data.FeedMedia
import social.vyb.app.data.FeedPost
import social.vyb.app.ui.VybRemoteImage
import social.vyb.app.ui.VybRemoteVideo

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.SocialPostLightbox(
    animatedVisibilityScope: AnimatedVisibilityScope,
    post: FeedPost,
    engagement: PostEngagementState,
    commentCount: Int,
    isOwner: Boolean,
    busy: Boolean,
    reactionMembers: ReactionMembersState,
    onReaction: (String) -> Unit,
    onComments: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onOpenReactions: () -> Unit,
    onRepost: (String, String) -> Unit,
    onOpenRepost: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onUpdate: (String, String) -> Unit,
    onDelete: () -> Unit,
    onReport: (String) -> Unit,
    onViewInsights: () -> Unit,
    onNotInterested: () -> Unit,
    onDismiss: () -> Unit
) {
    val media = remember(post.id, post.media) {
        post.media
            .filter { it.url.isNotBlank() }
            .distinctBy { it.url.trim() }
    }
    val pagerState = rememberPagerState(pageCount = { media.size.coerceAtLeast(1) })
    var isPaused by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full screen media
        if (media.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { page -> "${media[page].url}-$page" }
            ) { page ->
                val item = media[page]
                if (item.kind == "video") {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .combinedClickable(
                                onClick = {
                                    isPaused = !isPaused
                                    showDetails = !showDetails
                                },
                                onDoubleClick = { onReaction("like") }
                            )
                    ) {
                        VybRemoteVideo(
                            url = item.url,
                            contentDescription = "${post.author} video ${page + 1} of ${media.size}",
                            modifier = Modifier.fillMaxSize(),
                            isPaused = isPaused,
                            autoPlay = pagerState.currentPage == page
                        )
                    }
                } else {
                    ZoomableLightboxImage(
                        url = item.url,
                        contentDescription = "${post.author} post image ${page + 1} of ${media.size}",
                        modifier = Modifier.fillMaxSize(),
                        onTap = { showDetails = !showDetails },
                        onDoubleTap = { onReaction("like") }
                    )
                }
            }
        } else {
            Text(
                "This post has no media.",
                color = Color.White.copy(alpha = .72f),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Close Button
        AnimatedVisibility(
            visible = showDetails,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(top = 40.dp, end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        // Bottom Overlay
        AnimatedVisibility(
            visible = showDetails,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                // Author row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.clickable { if (!post.isAnonymous) onOpenProfile(post.handle.removePrefix("@")) }) {
                        SocialAvatar(
                            avatarUrl = post.avatarUrl.takeUnless { post.isAnonymous },
                            displayName = if (post.isAnonymous) "Anonymous Vyber" else post.author,
                            size = 40.dp
                        )
                    }
                    Column(Modifier.padding(start = 12.dp).weight(1f).clickable { if (!post.isAnonymous) onOpenProfile(post.handle.removePrefix("@")) }) {
                        Text(post.author, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "${post.handle} · ${post.time}",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    PostOverflowActions(
                        postId = post.id,
                        title = post.title,
                        body = post.body,
                        isOwner = isOwner,
                        busy = busy,
                        reactionMembers = reactionMembers,
                        onLoadReactionMembers = onOpenReactions,
                        onViewPost = null, // We are already viewing the post
                        onViewInsights = onViewInsights.takeIf { isOwner },
                        onNotInterested = onNotInterested.takeUnless { isOwner },
                        onRepost = onRepost,
                        onUpdate = onUpdate,
                        onDelete = onDelete,
                        onReport = onReport
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Post Text
                if (post.title.isNotBlank() && post.title != post.body) {
                    Text(
                        post.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                if (post.body.isNotBlank()) {
                    Text(
                        post.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Actions
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
                    onShare = onShare
                )
            }
        }
    }
}

@Composable
private fun ZoomableLightboxImage(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit
) {
    var scale by remember(url) { mutableFloatStateOf(1f) }
    var offset by remember(url) { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .combinedClickable(onClick = onTap, onDoubleClick = onDoubleTap)
            .pointerInput(url, scale) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var event: androidx.compose.ui.input.pointer.PointerEvent
                    do {
                        event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2 || scale > 1.01f) {
                            val nextScale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                            val pan = event.calculatePan()
                            val maxX = size.width * (nextScale - 1f) / 2f
                            val maxY = size.height * (nextScale - 1f) / 2f
                            scale = nextScale
                            offset = if (nextScale <= 1.01f) Offset.Zero else Offset(
                                (offset.x + pan.x).coerceIn(-maxX, maxX),
                                (offset.y + pan.y).coerceIn(-maxY, maxY)
                            )
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        VybRemoteImage(
            url = url,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
            contentScale = ContentScale.Fit
        )
    }
}
