package social.vyb.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import social.vyb.app.data.Chat

@Composable
fun MessagesScreen(chats: List<Chat>) {
    Box {
        LazyColumn {
            item {
                Text("Messages", Modifier.padding(20.dp, 18.dp),
                    style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text("Search conversations") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
            items(chats) { chat ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) { Text(chat.name.take(1), fontWeight = FontWeight.Bold) }
                    Column(Modifier.padding(start = 13.dp).weight(1f)) {
                        Text(chat.name, fontWeight = FontWeight.Bold)
                        Text(chat.preview, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(chat.time, style = MaterialTheme.typography.bodyMedium)
                        if (chat.unread > 0) Badge(Modifier.padding(top = 5.dp)) {
                            Text(chat.unread.toString())
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = {}, Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Default.Edit, "New message")
        }
    }
}
