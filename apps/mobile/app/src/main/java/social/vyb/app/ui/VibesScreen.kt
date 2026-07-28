package social.vyb.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun VibesScreen() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF20364D), Color(0xFF111827), Color.Black))
        )
    ) {
        Text(
            "VIBES",
            Modifier.align(Alignment.TopStart).padding(20.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = .35f))
            Text("Video preview", color = Color.White.copy(alpha = .6f))
        }
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VibeAction(Icons.Default.Favorite, "12.8K")
            VibeAction(Icons.Default.ChatBubble, "284")
            VibeAction(Icons.Default.Share, "Share")
        }
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Text("@riyavibes", color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                "POV: the lecture gets cancelled at 8:59 AM ✨",
                color = Color.White,
                modifier = Modifier.padding(top = 6.dp, end = 70.dp)
            )
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, null, Modifier.padding(end = 6.dp))
                Text("original sound · Riya", color = Color.White)
            }
        }
    }
}
@Composable
private fun VibeAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = {},
            modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = .35f))
        ) { Icon(icon, label, tint = Color.White) }
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}
