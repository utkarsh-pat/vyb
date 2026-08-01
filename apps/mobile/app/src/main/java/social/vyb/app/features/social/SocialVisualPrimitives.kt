package social.vyb.app.features.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import social.vyb.app.ui.resolveRemoteMediaUrl
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer

enum class SocialAvatarPresentation {
    RemoteImage,
    BlankAsset
}

internal fun socialAvatarPresentation(avatarUrl: String?): SocialAvatarPresentation =
    if (avatarUrl.isNullOrBlank()) {
        SocialAvatarPresentation.BlankAsset
    } else {
        SocialAvatarPresentation.RemoteImage
    }


private var circularCommentIcon: ImageVector? = null
val CircularCommentIcon: ImageVector
    get() = circularCommentIcon ?: ImageVector.Builder(
        name = "VybCircularComment",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M12 20.25c4.97 0 9-3.694 9-8.25s-4.03-8.25-9-8.25S3 7.444 3 12c0 2.104.859 4.023 2.273 5.48.432.447.74 1.04.586 1.641-.183.711-.532 1.488-1.025 2.115a.498.498 0 00.41.791c1.512-.132 2.871-.78 3.84-1.647A8.905 8.905 0 0012 20.25z"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.6f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build().also { circularCommentIcon = it }

object SocialActionIcons {
    val Comment: ImageVector = CircularCommentIcon
}

@Composable
fun SocialCommentAction(
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = "Open comments"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) .75f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "commentBouncy"
    )
    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            SocialActionIcons.Comment,
            contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SocialAvatar(
    avatarUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    contentDescription: String? = "$displayName profile avatar"
) {
    Surface(
        modifier = modifier.size(size).clip(CircleShape),
        color = Color(0xFFD1D5DB),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .14f)),
        shape = CircleShape
    ) {
        if (socialAvatarPresentation(avatarUrl) == SocialAvatarPresentation.RemoteImage) {
            SubcomposeAsyncImage(
                model = resolveRemoteMediaUrl(requireNotNull(avatarUrl)),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { BlankAvatarAsset() },
                error = { BlankAvatarAsset() },
                success = { SubcomposeAsyncImageContent() }
            )
        } else {
            BlankAvatarAsset()
        }
    }
}

@Composable
private fun BlankAvatarAsset() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFA3A3A3)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = Color(0xFFF4F4F5),
            modifier = Modifier.fillMaxSize(.76f)
        )
    }
}

@Composable
fun BouncyIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.75f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bouncy"
    )

    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        content()
    }
}
