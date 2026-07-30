package social.vyb.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class VybLayoutInfo(
    val compactWidth: Boolean,
    val compactHeight: Boolean,
    val wide: Boolean,
    val horizontalPadding: Dp,
    val sectionSpacing: Dp
)

internal fun resolveVybLayout(maxWidth: Dp, maxHeight: Dp): VybLayoutInfo =
    VybLayoutInfo(
        compactWidth = maxWidth < 360.dp,
        compactHeight = maxHeight < 700.dp,
        wide = maxWidth >= 600.dp,
        horizontalPadding = when {
            maxWidth < 360.dp -> 12.dp
            maxWidth >= 600.dp -> 28.dp
            else -> 16.dp
        },
        sectionSpacing = if (maxHeight < 700.dp) 10.dp else 16.dp
    )

@Composable
fun VybResponsiveFrame(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = 720.dp,
    content: @Composable (VybLayoutInfo) -> Unit
) {
    BoxWithConstraints(modifier) {
        val info = resolveVybLayout(maxWidth, maxHeight)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = maxContentWidth)
            ) {
                content(info)
            }
        }
    }
}

@Composable
fun VybEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp),
        color = VybPanel.copy(alpha = .82f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, VybBorder)
    ) {
        Column(
            Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                color = VybTeal.copy(alpha = .12f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = VybTeal)
                }
            }
            Text(
                title,
                Modifier.padding(top = 16.dp),
                color = VybText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                body,
                Modifier.padding(top = 6.dp),
                color = VybMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
