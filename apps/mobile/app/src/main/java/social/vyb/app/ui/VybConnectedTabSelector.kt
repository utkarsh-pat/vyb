package social.vyb.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class VybConnectedTab(
    val label: String,
    val badgeCount: Int = 0
)

/**
 * Two connected top tabs matching the PWA selector silhouette.
 *
 * The selected surface has a convex top shoulder followed by an inverse lower
 * shoulder into the adjacent tab. That continuous S seam is intentional: using
 * two rounded pills here produces the circular cut-out that the web UI avoids.
 */
@Composable
fun VybConnectedTabSelector(
    tabs: List<VybConnectedTab>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    require(tabs.size == 2) { "The connected selector supports exactly two tabs." }
    require(selectedIndex in tabs.indices) { "selectedIndex must address a tab." }
    val inactiveSurface = VybBackgroundDeep

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(Color.Transparent)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawConnectedTabSurface(
                selectedIndex = selectedIndex,
                color = inactiveSurface
            )
        }

        Row(
            Modifier
                .fillMaxSize()
                .selectableGroup()
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val contentColor = animateColorAsState(
                    targetValue = if (selected) VybText else VybMuted,
                    animationSpec = tween(180),
                    label = "connected tab content"
                ).value

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .selectable(
                            selected = selected,
                            onClick = { onSelected(index) },
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.wrapContentWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tab.label.uppercase(),
                            color = contentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            maxLines = 1
                        )
                        if (tab.badgeCount > 0) {
                            Badge(
                                modifier = Modifier.padding(start = 7.dp),
                                containerColor = VybIndigo,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = tab.badgeCount.coerceAtMost(99).toString(),
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawConnectedTabSurface(
    selectedIndex: Int,
    color: Color
) {
    val seam = size.width / 2f
    val shoulder = (18.dp.toPx()).coerceAtMost(size.height * 0.42f)
    val outerRadius = 16.dp.toPx()
    val selectedOnLeft = selectedIndex == 0
    val path = Path()

    if (selectedOnLeft) {
        // Draw RIGHT box (Inactive) with Boundary L
        path.moveTo(size.width, size.height)
        path.lineTo(size.width, outerRadius)
        path.quadraticBezierTo(size.width, 0f, size.width - outerRadius, 0f)
        path.lineTo(seam - shoulder, 0f)
        path.cubicTo(
            seam - shoulder * 0.448f, 0f,
            seam, shoulder * 0.448f,
            seam, shoulder
        )
        path.lineTo(seam, size.height - shoulder)
        path.cubicTo(
            seam, size.height - shoulder * 0.448f,
            seam + shoulder * 0.448f, size.height,
            seam + shoulder, size.height
        )
        path.lineTo(size.width, size.height)
    } else {
        // Draw LEFT box (Inactive) with Boundary R
        path.moveTo(0f, size.height)
        path.lineTo(0f, outerRadius)
        path.quadraticBezierTo(0f, 0f, outerRadius, 0f)
        path.lineTo(seam + shoulder, 0f)
        path.cubicTo(
            seam + shoulder * 0.448f, 0f,
            seam, shoulder * 0.448f,
            seam, shoulder
        )
        path.lineTo(seam, size.height - shoulder)
        path.cubicTo(
            seam, size.height - shoulder * 0.448f,
            seam - shoulder * 0.448f, size.height,
            seam - shoulder, size.height
        )
        path.lineTo(0f, size.height)
    }
    path.close()
    drawPath(path, color)
}
