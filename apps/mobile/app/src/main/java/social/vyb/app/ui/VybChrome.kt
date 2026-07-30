package social.vyb.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import social.vyb.app.R

data class VybPalette(
    val background: Color,
    val backgroundDeep: Color,
    val panel: Color,
    val panelLifted: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    val indigo: Color,
    val purple: Color,
    val teal: Color,
    val pink: Color
)

val VybDarkPalette = VybPalette(
    background = Color(0xFF0F172A),
    backgroundDeep = Color(0xFF020617),
    panel = Color(0xFF0F1424),
    panelLifted = Color(0xFF272E40),
    border = Color(0x1FFFFFFF),
    text = Color(0xFFE6EEFC),
    muted = Color(0xFF94A3B8),
    indigo = Color(0xFF6366F1),
    purple = Color(0xFF7C3AED),
    teal = Color(0xFF14B8A6),
    pink = Color(0xFFEC4899)
)

val VybLightPalette = VybPalette(
    background = Color(0xFFF4F7FC),
    backgroundDeep = Color(0xFFE9EEF7),
    panel = Color.White,
    panelLifted = Color(0xFFF0F3FA),
    border = Color(0x1F15213A),
    text = Color(0xFF111827),
    muted = Color(0xFF64748B),
    indigo = Color(0xFF4F46E5),
    purple = Color(0xFF7C3AED),
    teal = Color(0xFF0F9488),
    pink = Color(0xFFDB2777)
)

val LocalVybPalette = staticCompositionLocalOf { VybDarkPalette }

val VybBackground: Color @Composable get() = LocalVybPalette.current.background
val VybBackgroundDeep: Color @Composable get() = LocalVybPalette.current.backgroundDeep
val VybPanel: Color @Composable get() = LocalVybPalette.current.panel
val VybPanelLifted: Color @Composable get() = LocalVybPalette.current.panelLifted
val VybBorder: Color @Composable get() = LocalVybPalette.current.border
val VybText: Color @Composable get() = LocalVybPalette.current.text
val VybMuted: Color @Composable get() = LocalVybPalette.current.muted
val VybIndigo: Color @Composable get() = LocalVybPalette.current.indigo
val VybPurple: Color @Composable get() = LocalVybPalette.current.purple
val VybTeal: Color @Composable get() = LocalVybPalette.current.teal
val VybPink: Color @Composable get() = LocalVybPalette.current.pink

val VybAccentBrush: Brush
    @Composable get() = Brush.linearGradient(colors = listOf(VybIndigo, VybPurple))

@Composable
fun VybPageBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(VybBackground, VybBackgroundDeep)
                )
            )
    ) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-110).dp, y = (-80).dp)
                .size(300.dp)
                .blur(92.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(VybIndigo.copy(alpha = .14f), RoundedCornerShape(150.dp))
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = 50.dp)
                .size(220.dp)
                .blur(84.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(VybTeal.copy(alpha = .10f), RoundedCornerShape(110.dp))
        )
        content()
    }
}

@Composable
fun VybBrandLockup(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showSubtitle: Boolean = true
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.vyb_logo),
            contentDescription = "Vyb",
            modifier = Modifier
                .size(if (compact) 34.dp else 38.dp)
                .clip(RoundedCornerShape(if (compact) 11.dp else 13.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "vyb",
                color = VybText,
                fontSize = if (compact) 18.sp else 20.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.7).sp
            )
            if (showSubtitle) {
                Text(
                    text = "VERIFIED COLLEGE ACCESS",
                    color = VybMuted,
                    fontSize = 8.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.15.sp
                )
            }
        }
    }
}

@Composable
fun VybGlassPanel(
    modifier: Modifier = Modifier,
    radius: Int = 16,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = VybPanel.copy(alpha = .88f),
        shape = RoundedCornerShape(radius.dp),
        border = BorderStroke(1.dp, VybBorder),
        shadowElevation = 6.dp,
        content = content
    )
}
