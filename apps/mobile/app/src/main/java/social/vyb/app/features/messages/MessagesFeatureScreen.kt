package social.vyb.app.features.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybIndigo
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanelLifted
import social.vyb.app.ui.VybText
import social.vyb.app.ui.VybEmptyState
import social.vyb.app.ui.VybLoadingMark
import social.vyb.app.ui.VybResponsiveFrame

@Composable
fun MessagesFeatureScreen(
    modifier: Modifier = Modifier,
    communitySlug: String? = null,
    onOpenCommunity: ((String) -> Unit)? = null,
    onCloseCommunity: (() -> Unit)? = null
) {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { ChatRepository(context) }
    var selectedConversationId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCommunitySlug by rememberSaveable { mutableStateOf<String?>(null) }
    var communityMode by rememberSaveable { mutableStateOf(false) }

    val activeCommunitySlug = communitySlug ?: selectedCommunitySlug
    if (activeCommunitySlug != null) {
        val communityViewModel: CommunityConversationViewModel = viewModel(
            key = "community-$activeCommunitySlug",
            factory = MessagesViewModelFactory(
                repository = repository,
                communitySlug = activeCommunitySlug
            )
        )
        val state by communityViewModel.state.collectAsStateWithLifecycle()
        CommunityConversationContent(
            state = state,
            onBack = {
                if (communitySlug != null) onCloseCommunity?.invoke()
                else selectedCommunitySlug = null
            },
            onRetry = communityViewModel::refresh,
            onDraftChange = communityViewModel::updateDraft,
            onSend = communityViewModel::send,
            modifier = modifier
        )
        return
    }

    val conversationId = selectedConversationId
    if (conversationId == null) {
        val inboxViewModel: MessagesInboxViewModel = viewModel(
            factory = MessagesViewModelFactory(repository)
        )
        val state by inboxViewModel.state.collectAsStateWithLifecycle()
        InboxContent(
            state = state,
            onQueryChange = inboxViewModel::updateQuery,
            onRetry = inboxViewModel::refresh,
            onOpen = { selectedConversationId = it },
            communityMode = communityMode,
            onCommunityModeChange = { communityMode = it },
            onOpenCommunity = { slug ->
                if (onOpenCommunity != null) onOpenCommunity(slug)
                else selectedCommunitySlug = slug
            },
            modifier = modifier
        )
    } else {
        val conversationViewModel: ConversationViewModel = viewModel(
            key = "conversation-$conversationId",
            factory = MessagesViewModelFactory(repository, conversationId)
        )
        val state by conversationViewModel.state.collectAsStateWithLifecycle()
        ConversationContent(
            state = state,
            onBack = { selectedConversationId = null },
            onRetry = conversationViewModel::refresh,
            onDraftChange = conversationViewModel::updateDraft,
            onSend = conversationViewModel::send,
            modifier = modifier
        )
    }
}

@Composable
private fun InboxContent(
    state: InboxUiState,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onOpen: (String) -> Unit,
    communityMode: Boolean,
    onCommunityModeChange: (Boolean) -> Unit,
    onOpenCommunity: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    VybResponsiveFrame(modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        Text(
            text = if (communityMode) "Communities" else "Messages",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = VybText
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = {
                Text(if (communityMode) "Search your communities" else "Search conversations")
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VybPanelLifted,
                unfocusedContainerColor = VybPanelLifted,
                focusedBorderColor = VybIndigo,
                unfocusedBorderColor = VybBorder,
                focusedTextColor = VybText,
                unfocusedTextColor = VybText,
                focusedPlaceholderColor = VybMuted,
                unfocusedPlaceholderColor = VybMuted
            )
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(false to "Chats", true to "Community").forEach { (isCommunity, label) ->
                Surface(
                    onClick = { onCommunityModeChange(isCommunity) },
                    modifier = Modifier.weight(1f),
                    color = if (communityMode == isCommunity) VybIndigo else VybPanelLifted,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(
                        Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = VybText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        val visibleError = if (communityMode) state.communityError else state.error
        val visibleEmpty = if (communityMode) {
            state.filteredCommunities.isEmpty()
        } else {
            state.filteredItems.isEmpty()
        }
        when {
            state.isLoading -> CenterStatus { VybLoadingMark(width = 96.dp) }
            visibleError != null -> ErrorStatus(visibleError, onRetry)
            visibleEmpty -> Box(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                VybEmptyState(
                    icon = if (communityMode) Icons.Default.Groups else Icons.Default.ChatBubbleOutline,
                    title = if (state.query.isBlank()) {
                        if (communityMode) "No communities yet" else "No conversations yet"
                    } else {
                        "No matches found"
                    },
                    body = if (state.query.isBlank()) {
                        if (communityMode) {
                            "Your verified campus circles will appear here."
                        } else {
                            "Your campus conversations will stay organized here."
                        }
                    } else {
                        "Try another name or clear the search."
                    }
                )
            }
            communityMode -> LazyColumn(Modifier.padding(top = 8.dp)) {
                items(state.filteredCommunities, key = CommunityInboxItem::id) { item ->
                    CommunityInboxRow(item = item, onClick = { onOpenCommunity(item.slug) })
                }
            }
            else -> LazyColumn(Modifier.padding(top = 8.dp)) {
                items(state.filteredItems, key = ChatInboxItem::id) { item ->
                    InboxRow(item = item, onClick = { onOpen(item.id) })
                }
            }
        }
    }
    }
}

@Composable
private fun CommunityInboxRow(item: CommunityInboxItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape)
                .background(if (item.isOfficial) VybIndigo else VybPanelLifted),
            contentAlignment = Alignment.Center
        ) {
            Text(item.name.take(1).uppercase(), color = VybText, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.padding(start = 13.dp).weight(1f)) {
            Text(item.name, color = VybText, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                "${item.type.replaceFirstChar(Char::uppercase)} · ${item.memberCount} members",
                color = VybMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            item.membershipRole ?: "member",
            color = VybMuted,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun InboxRow(item: ChatInboxItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                Modifier.size(52.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(item.peerName.take(1).uppercase(), fontWeight = FontWeight.Bold)
            }
            if (item.isOnline) {
                Box(
                    Modifier.align(Alignment.BottomEnd).size(13.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Column(Modifier.padding(start = 13.dp).weight(1f)) {
            Text(item.peerName, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                item.preview,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(item.timestamp, style = MaterialTheme.typography.labelMedium)
            if (item.unreadCount > 0) {
                Badge(Modifier.padding(top = 5.dp)) { Text(item.unreadCount.toString()) }
            }
        }
    }
}

@Composable
private fun CommunityConversationContent(
    state: CommunityConversationUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    VybResponsiveFrame(modifier.fillMaxSize(), maxContentWidth = 900.dp) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Surface(tonalElevation = 2.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.context?.name ?: "Community",
                            fontWeight = FontWeight.Bold,
                            color = VybText
                        )
                        Text(
                            state.context?.let {
                                "${it.memberCount} members · ${it.type.replaceFirstChar(Char::uppercase)}"
                            } ?: "Campus conversation",
                            style = MaterialTheme.typography.bodySmall,
                            color = VybMuted
                        )
                    }
                    IconButton(onClick = onRetry, enabled = !state.isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
            when {
                state.isLoading -> CenterStatus { VybLoadingMark(width = 96.dp) }
                state.error != null && state.context == null ->
                    ErrorStatus(state.error, onRetry)
                else -> {
                    val listState = rememberLazyListState()
                    LaunchedEffect(state.messages.size) {
                        if (state.messages.isNotEmpty()) {
                            listState.animateScrollToItem(state.messages.lastIndex)
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Spacer(Modifier.size(4.dp)) }
                        if (state.messages.isEmpty()) {
                            item {
                                VybEmptyState(
                                    icon = Icons.Default.Groups,
                                    title = "No messages yet",
                                    body = "Start the first community message.",
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        } else {
                            items(state.messages, key = CommunityMessageItem::id) {
                                CommunityMessageBubble(it)
                            }
                        }
                        item { Spacer(Modifier.size(4.dp)) }
                    }
                    state.error?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.draft,
                            onValueChange = onDraftChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Message your community") },
                            maxLines = 4,
                            shape = RoundedCornerShape(22.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = onSend,
                            enabled = state.context != null &&
                                state.draft.isNotBlank() &&
                                !state.isSending
                        ) {
                            if (state.isSending) {
                                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityMessageBubble(message: CommunityMessageItem) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.isMine) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                VybPanelLifted
            },
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(horizontal = 13.dp, vertical = 9.dp)) {
                if (!message.isMine) {
                    Text(
                        message.authorName,
                        color = VybIndigo,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(message.body, color = VybText)
                Text(
                    buildString {
                        append(message.timestamp)
                        if (message.reactionCount > 0) append(" · ${message.reactionCount} reactions")
                        if (message.replyCount > 0) append(" · ${message.replyCount} replies")
                    },
                    modifier = Modifier.align(Alignment.End).padding(top = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = VybMuted
                )
            }
        }
    }
}

@Composable
private fun ConversationContent(
    state: ConversationUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    VybResponsiveFrame(modifier.fillMaxSize(), maxContentWidth = 900.dp) {
    Column(Modifier.fillMaxSize().imePadding()) {
        Surface(tonalElevation = 2.dp) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        state.peerName.ifBlank { "Conversation" },
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            state.isPeerTyping -> "Typing..."
                            state.isOnline -> "Online"
                            else -> state.peerHandle
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.isOnline) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRetry, enabled = !state.isLoading) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }
        when {
            state.isLoading -> CenterStatus { VybLoadingMark(width = 96.dp) }
            state.error != null && state.messages.isEmpty() -> ErrorStatus(state.error, onRetry)
            else -> {
                val listState = rememberLazyListState()
                LaunchedEffect(state.messages.size) {
                    if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(Modifier.size(4.dp)) }
                    items(state.messages, key = ChatMessageItem::id) { message ->
                        MessageBubble(message)
                    }
                    item { Spacer(Modifier.size(4.dp)) }
                }
                state.error?.let {
                    Text(
                        it,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.draft,
                        onValueChange = onDraftChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message") },
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = onSend,
                        enabled = state.draft.isNotBlank() && !state.isSending
                    ) {
                        if (state.isSending) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun MessageBubble(message: ChatMessageItem) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.isMine) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(horizontal = 13.dp, vertical = 9.dp)) {
                Text(
                    message.body,
                    color = if (message.isReadable) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    message.timestamp,
                    modifier = Modifier.align(Alignment.End).padding(top = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CenterStatus(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun ErrorStatus(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = 28.dp),
            color = MaterialTheme.colorScheme.error
        )
        IconButton(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = "Retry")
        }
    }
}
