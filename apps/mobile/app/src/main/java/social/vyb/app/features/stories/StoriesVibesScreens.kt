package social.vyb.app.features.stories

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.lifecycle.viewmodel.compose.viewModel
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import social.vyb.app.ui.VybEmptyState
import social.vyb.app.ui.VybLoadingMark
import social.vyb.app.features.media.StoryCompositionCodec
import social.vyb.app.features.media.StoryCompositionJson

/**
 * Drop-in home-feed story lane. It owns no navigation and opens the full-screen
 * story viewer internally.
 */
@Composable
fun StoriesLane(
    modifier: Modifier = Modifier,
    showLoadingIndicator: Boolean = true,
    viewModel: StoriesVibesViewModel = viewModel()
) {
    val state by viewModel.stories.collectAsState()
    LaunchedEffect(viewModel) { viewModel.initialize() }

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Stories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                enabled = !state.isRefreshing,
                onClick = viewModel::refreshStories
            ) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, "Refresh stories")
                }
            }
        }

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
                InlineMessage(
                    message = state.error ?: "No active stories yet."
                )
            }
            else -> {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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

/**
 * Drop-in replacement for the placeholder Vibes route.
 */
@Composable
fun NativeVibesScreen(
    modifier: Modifier = Modifier,
    viewModel: StoriesVibesViewModel = viewModel()
) {
    val state by viewModel.vibes.collectAsState()
    LaunchedEffect(viewModel) { viewModel.initialize() }

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
            LaunchedEffect(pagerState.currentPage, state.items.size, state.nextCursor) {
                if (pagerState.currentPage >= state.items.lastIndex - 2) {
                    viewModel.loadMoreVibes()
                }
            }

            Box(modifier.fillMaxSize().background(Color.Black)) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { index -> state.items[index].id }
                ) { page ->
                    VibePage(
                        vibe = state.items[page],
                        isActive = pagerState.currentPage == page,
                        isReactionBusy = state.items[page].id in state.busyVibeIds,
                        onToggleLike = { viewModel.toggleVibeLike(state.items[page].id) }
                    )
                }

                Row(
                    Modifier.align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = .75f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "VIBES",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        enabled = !state.isRefreshing,
                        onClick = viewModel::refreshVibes
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, "Refresh vibes", tint = Color.White)
                        }
                    }
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
            if (story.avatarUrl != null) {
                RemoteImage(story.avatarUrl, Modifier.fillMaxSize(), ContentScale.Crop)
            } else {
                Text(
                    story.displayName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold
                )
            }
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
    isReactionBusy: Boolean,
    onToggleLike: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
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
                NativeVideo(mediaUrl, isActive, Modifier.fillMaxSize())
            }
            else -> RemoteImage(mediaUrl, Modifier.fillMaxSize(), ContentScale.Fit)
        }

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = .86f)
                    )
                )
            )
        )

        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                enabled = !isReactionBusy,
                onClick = onToggleLike,
                modifier = Modifier.background(Color.Black.copy(alpha = .38f), CircleShape)
            ) {
                if (isReactionBusy) {
                    CircularProgressIndicator(
                        Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        if (vibe.viewerReactionType != null) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                        "Like vibe",
                        tint = if (vibe.viewerReactionType != null) Color(0xFFFF4D67) else Color.White
                    )
                }
            }
            Text(vibe.reactions.toCompactMetric(), color = Color.White)
            Spacer(Modifier.height(14.dp))
            Text("${vibe.comments} comments", color = Color.White)
        }

        Column(
            Modifier.align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 18.dp, end = 82.dp, bottom = 28.dp)
        ) {
            Text(
                "@${vibe.author.username}",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            val description = vibe.body.ifBlank { vibe.title }
            if (description.isNotBlank()) {
                Text(
                    description,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
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
    runCatching { android.graphics.Color.parseColor(this ?: "#FFFFFF") }
        .getOrDefault(android.graphics.Color.WHITE)

@Composable
private fun NativeVideo(
    url: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    crop: Boolean = false
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setOnPreparedListener { player ->
                    player.isLooping = true
                    player.setVideoScalingMode(
                        if (crop) MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                        else MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    )
                    if (isActive) start()
                }
            }
        },
        update = { video ->
            if (video.tag != url) {
                video.tag = url
                video.setVideoPath(url)
            }
            if (isActive && !video.isPlaying) video.start()
            if (!isActive && video.isPlaying) video.pause()
        },
        onRelease = { video ->
            video.stopPlayback()
        }
    )
}

@Composable
private fun RemoteImage(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bitmap by remoteBitmap(url)
    Box(modifier.background(Color(0xFF182130)), contentAlignment = Alignment.Center) {
        if (bitmap == null) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            androidx.compose.foundation.Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun remoteBitmap(url: String): State<Bitmap?> {
    val bitmapState = remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) {
        val loadedBitmap = withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.instanceFollowRedirects = true
                connection.inputStream.use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
        bitmapState.value = loadedBitmap
    }
    return bitmapState
}

@Composable
private fun Avatar(url: String?, displayName: String) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF27364D)),
        contentAlignment = Alignment.Center
    ) {
        if (url == null) {
            Text(displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        } else {
            RemoteImage(url, Modifier.fillMaxSize(), ContentScale.Crop)
        }
    }
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

private fun Int.toCompactMetric(): String = when {
    this >= 1_000_000 -> "${this / 1_000_000}.${(this % 1_000_000) / 100_000}M"
    this >= 1_000 -> "${this / 1_000}.${(this % 1_000) / 100}K"
    else -> toString()
}
