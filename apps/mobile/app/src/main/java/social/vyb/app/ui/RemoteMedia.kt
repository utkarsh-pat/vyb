package social.vyb.app.ui

import android.media.MediaPlayer
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun VybRemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier.background(Color(0xFF182130)),
        contentScale = contentScale,
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VybIndigo)
            }
        },
        error = {
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
    var prepared by remember(url) { mutableStateOf(false) }
    var failed by remember(url) { mutableStateOf(false) }
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
                if (video.tag != url) {
                    prepared = false
                    failed = false
                    video.tag = url
                    video.setVideoPath(url)
                }
                if (autoPlay && prepared && !video.isPlaying) video.start()
            },
            onRelease = VideoView::stopPlayback
        )
        when {
            failed -> Text("Media unavailable", color = VybMuted)
            !prepared -> CircularProgressIndicator(color = VybIndigo)
        }
    }
}
