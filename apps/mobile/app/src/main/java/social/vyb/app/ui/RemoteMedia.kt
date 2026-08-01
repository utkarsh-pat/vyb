package social.vyb.app.ui

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import java.net.URI
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
private const val WEB_ORIGIN = "https://www.vybnet.app"

/** VideoView normally measures itself to the source aspect ratio, which can reintroduce
 * letterboxing even when MediaPlayer is in crop mode. Crop surfaces must occupy their parent. */
internal class VybFillVideoView(context: Context) : VideoView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            android.view.View.MeasureSpec.getSize(widthMeasureSpec),
            android.view.View.MeasureSpec.getSize(heightMeasureSpec)
        )
    }
}

/**
 * The web client can render same-origin media paths directly, while the native client needs an
 * absolute URL. Media previously served from the retired media.vybnet.app host is also routed
 * through the production R2 proxy.
 */
internal fun resolveRemoteMediaUrl(rawUrl: String): String {
    val value = rawUrl.trim()
    if (value.isBlank()) return value
    val uri = runCatching { URI(value) }.getOrNull()
    if (uri?.isAbsolute == true) {
        if (uri.host.equals("media.vybnet.app", ignoreCase = true)) {
            val path = canonicalMediaObjectPath(uri.rawPath.orEmpty())
            return "$WEB_ORIGIN/api/media/$path" +
                (uri.rawQuery?.let { "?$it" } ?: "")
        }
        if (
            uri.host.equals("www.vybnet.app", ignoreCase = true) &&
            uri.rawPath.orEmpty().startsWith("/api/media/firebase-migration/")
        ) {
            return "$WEB_ORIGIN/api/media/${canonicalMediaObjectPath(uri.rawPath.orEmpty())}" +
                (uri.rawQuery?.let { "?$it" } ?: "")
        }
        return value
    }
    return when {
        value.startsWith("/api/media/") || value.startsWith("api/media/") ->
            "$WEB_ORIGIN/api/media/${canonicalMediaObjectPath(value)}"
        value.startsWith("/api/") -> "$WEB_ORIGIN$value"
        value.startsWith("api/") -> "$WEB_ORIGIN/$value"
        else -> "$WEB_ORIGIN/api/media/${canonicalMediaObjectPath(value)}"
    }
}

private fun canonicalMediaObjectPath(value: String): String =
    value.trimStart('/')
        .removePrefix("api/media/")
        .removePrefix("firebase-migration/")

@Composable
fun VybRemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val resolvedUrl = remember(url) { resolveRemoteMediaUrl(url) }
    SubcomposeAsyncImage(
        model = resolvedUrl,
        contentDescription = contentDescription,
        modifier = modifier.background(Color(0xFF182130)),
        contentScale = contentScale,
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VybIndigo)
            }
        },
        error = {
            LaunchedEffect(resolvedUrl) {
                Log.e("VybRemoteMedia", "Image failed: ${safeMediaLogValue(resolvedUrl)}")
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text("Media unavailable", color = VybMuted)
            }
        },
        success = { SubcomposeAsyncImageContent() }
    )
}

@Composable
fun VybRemoteVideo(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false,
    playbackSpeed: Float = 1.0f,
    isPaused: Boolean = false,
    crop: Boolean = false,
    muted: Boolean = false
) {
    val resolvedUrl = remember(url) { resolveRemoteMediaUrl(url) }
    var prepared by remember(resolvedUrl) { mutableStateOf(false) }
    var failed by remember(resolvedUrl) { mutableStateOf(false) }

    // Internal state for timeline and player
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var showPlayIcon by remember(isPaused) { mutableStateOf(isPaused) }

    // Track play icon visibility temporarily when toggled
    LaunchedEffect(isPaused) {
        showPlayIcon = true
        kotlinx.coroutines.delay(1000)
        showPlayIcon = false
    }

    // Timeline progress loop
    LaunchedEffect(mediaPlayer, isPaused) {
        val player = mediaPlayer
        if (player != null && !isPaused) {
            while (true) {
                try {
                    val currentPosition = player.currentPosition.toFloat()
                    val duration = player.duration.toFloat()
                    if (duration > 0) {
                        progress = currentPosition / duration
                    }
                } catch (e: Exception) {}
                delay(50) // 20fps update
            }
        }
    }

    var lastAppliedSpeed by remember { mutableStateOf(1f) }
    var lastAppliedVolume by remember { mutableStateOf(1f) }

    Box(modifier.background(Color(0xFF182130)), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                (if (crop) VybFillVideoView(context) else VideoView(context)).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.contentDescription = contentDescription
                }
            },
            update = { video ->
                video.setOnPreparedListener { player ->
                    mediaPlayer = player
                    prepared = true
                    failed = false
                    player.isLooping = true
                    player.setVideoScalingMode(
                        if (crop) MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                        else MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    )
                    player.setVolume(if (muted) 0f else 1f, if (muted) 0f else 1f)

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        try {
                            player.playbackParams = player.playbackParams.setSpeed(playbackSpeed)
                            lastAppliedSpeed = playbackSpeed
                        } catch (e: Exception) {}
                    }

                    if (autoPlay && !isPaused) video.start()
                }
                video.setOnErrorListener { _, _, _ ->
                    prepared = false
                    failed = true
                    true
                }
                if (video.tag != resolvedUrl || video.tag == "update_volume") {
                    prepared = false
                    failed = false
                    video.tag = resolvedUrl
                    video.setVideoPath(resolvedUrl)
                }

                if (video.tag == resolvedUrl) {
                    val targetVolume = if (muted) 0f else 1f
                    if (lastAppliedVolume != targetVolume) {
                        mediaPlayer?.setVolume(targetVolume, targetVolume)
                        lastAppliedVolume = targetVolume
                    }
                }

                // Handle dynamically passed state
                if (prepared && mediaPlayer != null) {
                    val player = mediaPlayer!!

                    // Handle Pause/Play
                    if ((isPaused || !autoPlay) && video.isPlaying) {
                        video.pause()
                    } else if (!isPaused && !video.isPlaying && autoPlay) {
                        video.start()
                    }

                    // Handle Speed dynamically using the MediaPlayer instance directly
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        if (lastAppliedSpeed != playbackSpeed) {
                            try {
                                player.playbackParams = player.playbackParams.setSpeed(playbackSpeed)
                                lastAppliedSpeed = playbackSpeed
                                // Setting speed unpauses the player on Android. Re-pause if necessary.
                                if (isPaused) {
                                    video.pause()
                                }
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }
                }
            },
            onRelease = { video ->
                mediaPlayer = null
                video.stopPlayback()
            }
        )
        when {
            failed -> {
                LaunchedEffect(resolvedUrl) {
                    Log.e("VybRemoteMedia", "Video failed: ${safeMediaLogValue(resolvedUrl)}")
                }
                Text("Media unavailable", color = VybMuted)
            }
            !prepared -> CircularProgressIndicator(color = VybIndigo)
            else -> {
                // Interactive Overlays

                // Top-Left 2x Speed Badge
                androidx.compose.animation.AnimatedVisibility(
                    visible = playbackSpeed > 1f,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("2x Speed", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Center Play/Pause Icon Animation
                androidx.compose.animation.AnimatedVisibility(
                    visible = showPlayIcon,
                    enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) + androidx.compose.animation.scaleIn(initialScale = 0.8f),
                    exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(500)) + androidx.compose.animation.scaleOut(targetScale = 1.2f),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = if (isPaused) androidx.compose.material.icons.Icons.Filled.PlayArrow else androidx.compose.material.icons.Icons.Filled.Pause,
                            contentDescription = if (isPaused) "Play" else "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Thin Progress Timeline
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
            }
        }
    }
}

private fun safeMediaLogValue(value: String): String =
    runCatching {
        val uri = URI(value)
        "${uri.scheme}://${uri.host}${uri.path}"
    }.getOrDefault("<invalid-media-url>")
