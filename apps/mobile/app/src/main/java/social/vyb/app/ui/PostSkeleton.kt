package social.vyb.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.04f),
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.04f),
        ),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 600f, 300f)
    )
}

@Composable
fun PostCardSkeleton(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    val base = Color(0xFF0A0A0A)
    Column(
        modifier
            .fillMaxWidth()
            .background(base)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(brush))
            Spacer(Modifier.width(10.dp))
            Column {
                Box(Modifier.height(12.dp).width(120.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                Spacer(Modifier.height(5.dp))
                Box(Modifier.height(10.dp).width(80.dp).clip(RoundedCornerShape(5.dp)).background(brush))
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(brush))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(0.75f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(brush))
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(14.dp)).background(brush))
        Spacer(Modifier.height(14.dp))
        Row {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(brush))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(brush))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(brush))
        }
    }
}

@Composable
fun FeedSkeleton(modifier: Modifier = Modifier) {
    Column(modifier) {
        repeat(3) {
            PostCardSkeleton()
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
        }
    }
}
