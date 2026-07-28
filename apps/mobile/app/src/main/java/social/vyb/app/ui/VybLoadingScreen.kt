package social.vyb.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val WebLoaderIndigo = Color(0xFF4245FD)
private val WebLoaderTeal = Color(0xFF01BBB9)
private const val LoaderDurationMs = 2_700

@Composable
fun VybLoadingScreen(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VybBackground, VybBackgroundDeep))),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(WebLoaderIndigo.copy(alpha = .12f), Color.Transparent),
                    center = Offset(size.width * .18f, size.height * .14f),
                    radius = size.minDimension * .72f
                ),
                radius = size.minDimension * .72f,
                center = Offset(size.width * .18f, size.height * .14f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(WebLoaderTeal.copy(alpha = .09f), Color.Transparent),
                    center = Offset(size.width * .84f, size.height * .22f),
                    radius = size.minDimension * .64f
                ),
                radius = size.minDimension * .64f,
                center = Offset(size.width * .84f, size.height * .22f)
            )
        }

        VybLoadingMark(
            modifier = Modifier.graphicsLayer {
                shadowElevation = 22.dp.toPx()
                ambientShadowColor = WebLoaderTeal.copy(alpha = .16f)
                spotShadowColor = WebLoaderTeal.copy(alpha = .16f)
            },
            width = if (maxWidth <= 520.dp) 148.dp else 168.dp
        )
    }
}

@Composable
fun VybLoadingMark(
    modifier: Modifier = Modifier,
    width: Dp = 112.dp
) {
    val transition = rememberInfiniteTransition(label = "Vyb loading loop")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(LoaderDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Vyb logo formation"
    ).value

    Canvas(
        modifier = modifier
            // The web loader lives in a square panel with overflow visible. Keep a
            // square native viewport and draw the mark inside safe bounds so its
            // glow, rounded arms, and final shadow are never clipped.
            .size(width)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .semantics { contentDescription = "Loading Vyb" }
    ) {
        drawWebLoadingMark(phase)
    }
}

private fun DrawScope.drawWebLoadingMark(phase: Float) {
    val artWidth = size.width * .82f
    val strokeScale = artWidth / 320f
    val artHeight = 260f * strokeScale
    val origin = Offset(
        x = (size.width - artWidth) / 2f,
        y = (size.height - artHeight) / 2f
    )

    fun point(x: Float, y: Float) = Offset(
        origin.x + x * strokeScale,
        origin.y + y * strokeScale
    )
    fun delayed(value: Float, delay: Float) = ((value - delay) / (1f - delay)).coerceIn(0f, 1f)

    val leftDot = dotState(phase, right = false)
    val rightPhase = delayed(phase, 80f / LoaderDurationMs)
    val rightDot = dotState(rightPhase, right = true)
    val leftStream = streamState(phase)
    val rightStream = streamState(rightPhase)
    val leftArm = armState(phase)
    val rightArm = armState(rightPhase)

    drawAnimatedLine(
        start = point(100f, 96f),
        end = point(100f, 138f),
        fraction = leftStream.fraction,
        color = WebLoaderIndigo,
        alpha = leftStream.alpha,
        width = 16f * strokeScale,
        glowWidth = 30f * strokeScale
    )
    drawAnimatedLine(
        start = point(220f, 96f),
        end = point(220f, 138f),
        fraction = rightStream.fraction,
        color = WebLoaderTeal,
        alpha = rightStream.alpha,
        width = 16f * strokeScale,
        glowWidth = 30f * strokeScale
    )

    drawAnimatedLine(
        start = point(212f, 148f),
        end = point(160f, 214f),
        fraction = rightArm.fraction,
        color = WebLoaderTeal,
        alpha = rightArm.alpha,
        width = 48f * strokeScale,
        glowWidth = 66f * strokeScale,
        shadowOffset = 12f * strokeScale
    )
    drawTealArmCutout(
        first = point(165f, 158f),
        second = point(184f, 184f),
        third = point(158f, 184f),
        alpha = tealCutoutAlpha(phase)
    )
    drawAnimatedLine(
        start = point(108f, 148f),
        end = point(160f, 214f),
        fraction = leftArm.fraction,
        color = WebLoaderIndigo,
        alpha = leftArm.alpha,
        width = 48f * strokeScale,
        glowWidth = 66f * strokeScale,
        shadowOffset = 12f * strokeScale
    )

    drawLoaderDot(point(100f, 96f), 20f * strokeScale, WebLoaderIndigo, leftDot)
    drawLoaderDot(point(220f, 96f), 20f * strokeScale, WebLoaderTeal, rightDot)
}

private fun DrawScope.drawTealArmCutout(
    first: Offset,
    second: Offset,
    third: Offset,
    alpha: Float
) {
    if (alpha <= 0f) return
    val cutout = Path().apply {
        moveTo(first.x, first.y)
        lineTo(second.x, second.y)
        lineTo(third.x, third.y)
        close()
    }
    drawPath(
        path = cutout,
        color = Color.Black.copy(alpha = alpha),
        blendMode = BlendMode.Clear
    )
}

private fun DrawScope.drawAnimatedLine(
    start: Offset,
    end: Offset,
    fraction: Float,
    color: Color,
    alpha: Float,
    width: Float,
    glowWidth: Float,
    shadowOffset: Float = 0f
) {
    if (fraction <= 0f || alpha <= 0f) return
    val animatedEnd = Offset(
        x = start.x + (end.x - start.x) * fraction,
        y = start.y + (end.y - start.y) * fraction
    )
    if (shadowOffset > 0f) {
        drawLine(
            color = Color(0xFF02081A).copy(alpha = alpha * .35f),
            start = start.copy(y = start.y + shadowOffset),
            end = animatedEnd.copy(y = animatedEnd.y + shadowOffset),
            strokeWidth = width * 1.06f,
            cap = StrokeCap.Round
        )
    }
    drawLine(
        color = color.copy(alpha = alpha * .18f),
        start = start,
        end = animatedEnd,
        strokeWidth = glowWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color.copy(alpha = alpha),
        start = start,
        end = animatedEnd,
        strokeWidth = width,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawLoaderDot(
    center: Offset,
    baseRadius: Float,
    color: Color,
    state: LoaderDotState
) {
    if (state.alpha <= 0f) return
    drawCircle(
        color = color.copy(alpha = state.alpha * .16f),
        radius = baseRadius * state.scale * 1.65f,
        center = center
    )
    drawCircle(
        color = color.copy(alpha = state.alpha),
        radius = baseRadius * state.scale,
        center = center
    )
}

private data class LoaderLineState(val fraction: Float, val alpha: Float)
private data class LoaderDotState(val scale: Float, val alpha: Float)

private fun streamState(phase: Float): LoaderLineState = when {
    phase <= .22f -> LoaderLineState(0f, 0f)
    phase <= .34f -> {
        val t = normalized(phase, .22f, .34f)
        LoaderLineState(.54f * ease(t), .88f * t)
    }
    phase <= .52f -> {
        val t = normalized(phase, .34f, .52f)
        LoaderLineState(.54f + .46f * ease(t), .88f - .24f * t)
    }
    phase <= .68f -> LoaderLineState(1f, .64f * (1f - normalized(phase, .52f, .68f)))
    else -> LoaderLineState(1f, 0f)
}

private fun armState(phase: Float): LoaderLineState = when {
    phase <= .43f -> LoaderLineState(0f, 0f)
    phase <= .60f -> {
        val t = normalized(phase, .43f, .60f)
        LoaderLineState(.72f * ease(t), t)
    }
    phase <= .72f -> {
        val t = normalized(phase, .60f, .72f)
        LoaderLineState(.72f + .28f * ease(t), 1f)
    }
    phase <= .88f -> LoaderLineState(1f, 1f)
    else -> LoaderLineState(1f, 1f - normalized(phase, .88f, 1f))
}

private fun tealCutoutAlpha(phase: Float): Float = when {
    phase <= .70f -> 0f
    phase <= .76f -> ease(normalized(phase, .70f, .76f))
    phase <= .88f -> 1f
    else -> 1f - normalized(phase, .88f, 1f)
}

private fun dotState(phase: Float, right: Boolean): LoaderDotState {
    val hiddenUntil = if (right) .10f else .08f
    val popAt = if (right) .17f else .15f
    val settleAt = if (right) .24f else .22f
    return when {
        phase <= hiddenUntil -> LoaderDotState(.2f, 0f)
        phase <= popAt -> {
            val t = normalized(phase, hiddenUntil, popAt)
            LoaderDotState(.2f + 1.04f * ease(t), t)
        }
        phase <= settleAt -> {
            val t = normalized(phase, popAt, settleAt)
            LoaderDotState(1.24f - .24f * ease(t), 1f)
        }
        phase <= .88f -> LoaderDotState(1f, 1f)
        else -> {
            val t = normalized(phase, .88f, 1f)
            LoaderDotState(1f - .12f * t, 1f - t)
        }
    }
}

private fun normalized(value: Float, start: Float, end: Float) =
    ((value - start) / (end - start)).coerceIn(0f, 1f)

private fun ease(value: Float) = FastOutSlowInEasing.transform(value.coerceIn(0f, 1f))
