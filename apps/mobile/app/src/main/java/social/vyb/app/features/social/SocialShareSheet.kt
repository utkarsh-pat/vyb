package social.vyb.app.features.social

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import social.vyb.app.features.messages.ChatInboxItem
import social.vyb.app.features.messages.ChatRepository
import social.vyb.app.ui.VybPanelLifted
import social.vyb.app.ui.VybText
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybIndigo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialShareSheet(
    postId: String,
    postKind: String,
    postPlacement: String,
    postTitle: String,
    postBody: String,
    postMediaUrl: String? = null,
    authorDisplayName: String,
    authorUsername: String,
    authorIsAnonymous: Boolean,
    onDismiss: () -> Unit,
    onAddToStory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val chatRepository = remember(context.applicationContext) { ChatRepository(context.applicationContext) }
    var shareTargets by remember(postId) { mutableStateOf<List<ChatInboxItem>>(emptyList()) }
    var targetsLoading by remember(postId) { mutableStateOf(true) }
    var targetError by remember(postId) { mutableStateOf<String?>(null) }
    var busyConversationId by remember(postId) { mutableStateOf<String?>(null) }
    var searchQuery by remember(postId) { mutableStateOf("") }

    LaunchedEffect(postId) {
        targetsLoading = true
        targetError = null
        runCatching { chatRepository.loadInbox() }
            .onSuccess { shareTargets = it }
            .onFailure { targetError = it.message ?: "Could not load recent chats." }
        targetsLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Share",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = VybText,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ShareActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy Link",
                    onClick = {
                        val url = getPostShareUrl(postId, postKind, postPlacement)
                        clipboardManager.setText(AnnotatedString(url))
                        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )

                ShareActionButton(
                    icon = Icons.Default.Add, // Placeholder for Add to Story icon
                    label = "Add to Story",
                    onClick = {
                        onAddToStory()
                        onDismiss()
                    }
                )

                ShareActionButton(
                    icon = Icons.Default.Share, // Placeholder for WhatsApp icon
                    label = "WhatsApp",
                    onClick = {
                        val url = getPostShareUrl(postId, postKind, postPlacement)
                        val text = getPostShareText(postTitle, postBody, authorDisplayName, authorUsername, authorIsAnonymous, url)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            setPackage("com.whatsapp")
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    }
                )

                ShareActionButton(
                    icon = Icons.Default.MoreHoriz,
                    label = "More",
                    onClick = {
                        val url = getPostShareUrl(postId, postKind, postPlacement)
                        val text = getPostShareText(postTitle, postBody, authorDisplayName, authorUsername, authorIsAnonymous, url)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TITLE, postTitle.takeIf { it.isNotBlank() } ?: "Vyb")
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share via"))
                        onDismiss()
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text("OR SEND TO A FRIEND", modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelSmall, color = VybMuted)
                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                placeholder = { Text("Search recent chats...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            when {
                targetsLoading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                targetError != null -> Text(targetError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                else -> {
                    val filteredTargets = shareTargets.filter {
                        it.peerName.contains(searchQuery, ignoreCase = true) ||
                            it.peerHandle.contains(searchQuery, ignoreCase = true)
                    }
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        items(filteredTargets.size, key = { filteredTargets[it].id }) { index ->
                            val user = filteredTargets[index]
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SocialAvatar(avatarUrl = user.avatarUrl, displayName = user.peerName, size = 48.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(user.peerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(user.peerHandle, color = VybMuted, style = MaterialTheme.typography.bodySmall)
                                }
                                Button(
                                    enabled = busyConversationId == null,
                                    onClick = {
                                        busyConversationId = user.id
                                        scope.launch {
                                            runCatching {
                                                chatRepository.sendVibeCard(
                                                    conversationId = user.id,
                                                    postId = postId,
                                                    title = postTitle,
                                                    body = postBody,
                                                    mediaUrl = postMediaUrl,
                                                    authorUsername = if (authorIsAnonymous) "anonymous" else authorUsername,
                                                    authorDisplayName = if (authorIsAnonymous) "Anonymous Vyber" else authorDisplayName
                                                )
                                            }.onSuccess {
                                                Toast.makeText(context, "Sent to ${user.peerName}", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            }.onFailure {
                                                targetError = it.message ?: "Could not send this post."
                                            }
                                            busyConversationId = null
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VybIndigo)
                                ) {
                                    if (busyConversationId == user.id) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text("SEND")
                                    }
                                }
                            }
                        }
                        if (filteredTargets.isEmpty()) {
                            item {
                                Text(
                                    "No matching secure chats yet.",
                                    color = VybMuted,
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(VybPanelLifted, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = VybText,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = VybMuted
        )
    }
}

private fun getPostShareUrl(postId: String, postKind: String, postPlacement: String): String {
    val path = if (postKind == "video" || postPlacement == "vibe") "/vibes" else "/home"
    return "https://vybnet.app$path?post=${Uri.encode(postId)}"
}

private fun getPostShareText(
    title: String,
    body: String,
    authorDisplayName: String,
    authorUsername: String,
    authorIsAnonymous: Boolean,
    url: String
): String {
    val authorLabel = if (authorIsAnonymous) "Anonymous Vyber" else (authorDisplayName.takeIf { it.isNotBlank() } ?: authorUsername)
    val textTitle = title.trim().takeIf { it.isNotBlank() } ?: body.trim().takeIf { it.isNotBlank() } ?: "Vyb by $authorLabel"
    return "$textTitle\n$url"
}
