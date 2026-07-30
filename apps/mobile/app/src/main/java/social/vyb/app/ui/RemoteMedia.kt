package social.vyb.app.ui

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

private const val WEB_ORIGIN = "https://www.vybnet.app"

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
    autoPlay: Boolean = false
) {
    val resolvedUrl = remember(url) { resolveRemoteMediaUrl(url) }
    var prepared by remember(resolvedUrl) { mutableStateOf(false) }
    var failed by remember(resolvedUrl) { mutableStateOf(false) }
    Box(modifier.background(Color(0xFF182130)), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                VideoView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setMediaController(MediaController(context).also { it.setAnchorView(this) })
                    this.contentDescription = contentDescription
                }
            },
            update = { video ->
                video.setOnPreparedListener { player ->
                    prepared = true
                    failed = false
                    player.isLooping = true
                    player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                    if (autoPlay) video.start()
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
                if (autoPlay && prepared && !video.isPlaying) video.start()
            },
            onRelease = VideoView::stopPlayback
        )
        when {
            failed -> {
                LaunchedEffect(resolvedUrl) {
                    Log.e("VybRemoteMedia", "Video failed: ${safeMediaLogValue(resolvedUrl)}")
                }
                Text("Media unavailable", color = VybMuted)
            }
            !prepared -> CircularProgressIndicator(color = VybIndigo)
        }
    }
}

private fun safeMediaLogValue(value: String): String =
    runCatching {
        val uri = URI(value)
        "${uri.scheme}://${uri.host}${uri.path}"
    }.getOrDefault("<invalid-media-url>")
