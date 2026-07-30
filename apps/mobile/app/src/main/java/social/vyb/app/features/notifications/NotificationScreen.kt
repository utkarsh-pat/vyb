package social.vyb.app.features.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybEmptyState
import social.vyb.app.ui.VybIndigo
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanel
import social.vyb.app.ui.VybResponsiveFrame
import social.vyb.app.ui.VybText

@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    onNavigateHref: (String) -> Unit,
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val state by notificationViewModel.state.collectAsStateWithLifecycle()
    VybResponsiveFrame(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = VybText)
                }
                Column {
                    Text("Notifications", color = VybText, fontWeight = FontWeight.Black)
                    Text("${state.unreadCount} unread", color = VybMuted)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = notificationViewModel::refresh, enabled = !state.loading) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = VybText)
                }
                OutlinedButton(
                    onClick = notificationViewModel::markAllRead,
                    enabled = state.unreadCount > 0 && !state.mutating
                ) {
                    Icon(Icons.Default.DoneAll, null, Modifier.size(18.dp))
                    Text("Read all", modifier = Modifier.padding(start = 5.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                listOf("all" to "All", "unread" to "Unread", "read" to "Read").forEach { (key, label) ->
                    Surface(
                        onClick = { notificationViewModel.setFilter(key) },
                        modifier = Modifier.weight(1f).padding(horizontal = 3.dp),
                        color = if (state.filter == key) VybIndigo else VybPanel,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (state.filter == key) VybIndigo else VybBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(label, color = VybText, fontWeight = if (state.filter == key) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VybIndigo)
                }
                state.error != null -> VybEmptyState(
                    icon = Icons.Default.NotificationsNone,
                    title = "Notifications unavailable",
                    body = state.error.orEmpty(),
                    actionLabel = "Try again",
                    onAction = notificationViewModel::refresh,
                    modifier = Modifier.padding(20.dp)
                )
                state.items.isEmpty() -> VybEmptyState(
                    icon = Icons.Default.NotificationsNone,
                    title = "You’re all caught up",
                    body = "Campus updates, messages, events and marketplace alerts will appear here.",
                    modifier = Modifier.padding(20.dp)
                )
                else -> LazyColumn {
                    state.notice?.let { notice ->
                        item {
                            Text(
                                notice,
                                color = VybText,
                                modifier = Modifier.fillMaxWidth().background(VybIndigo.copy(alpha = .16f))
                                    .padding(12.dp)
                            )
                        }
                    }
                    items(state.items, key = NotificationItem::id) { item ->
                        NotificationRow(
                            item = item,
                            enabled = !state.mutating,
                            onClick = { notificationViewModel.open(item, onNavigateHref) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItem, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)
            .background(if (item.state.readAt == null) VybIndigo.copy(alpha = .12f) else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier.size(38.dp).background(VybIndigo.copy(alpha = .2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.NotificationsNone, null, tint = VybText, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(item.copy.title, color = VybText, fontWeight = FontWeight.Bold)
            Text(
                item.copy.body,
                color = VybMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${item.category.replaceFirstChar(Char::uppercase)} · ${item.createdAt.take(10)}",
                color = VybMuted,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
        if (item.state.readAt == null) {
            Box(Modifier.padding(top = 5.dp).size(8.dp).background(VybIndigo, CircleShape))
        }
    }
    HorizontalDivider(color = VybBorder)
}
