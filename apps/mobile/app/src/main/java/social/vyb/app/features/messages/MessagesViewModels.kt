package social.vyb.app.features.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.net.Uri
import android.os.SystemClock
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import social.vyb.app.features.realtime.ChatRealtimeEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class InboxUiState(
    val isLoading: Boolean = true,
    val items: List<ChatInboxItem> = emptyList(),
    val communities: List<CommunityInboxItem> = emptyList(),
    val query: String = "",
    val error: String? = null,
    val communityError: String? = null
) {
    val filteredItems: List<ChatInboxItem>
        get() {
            val needle = query.trim()
            return if (needle.isEmpty()) items else items.filter {
                it.peerName.contains(needle, true) || it.peerHandle.contains(needle, true)
            }
        }

    val filteredCommunities: List<CommunityInboxItem>
        get() {
            val needle = query.trim()
            return if (needle.isEmpty()) communities else communities.filter {
                it.name.contains(needle, true) ||
                    it.type.contains(needle, true) ||
                    it.membershipRole?.contains(needle, true) == true
            }
        }
}

class MessagesInboxViewModel(
    private val repository: ChatRepository
) : ViewModel() {
    private val _state = MutableStateFlow(InboxUiState())
    val state: StateFlow<InboxUiState> = _state.asStateFlow()
    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh(silent: Boolean = false) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = if (silent) it.isLoading else true,
                    error = null,
                    communityError = null
                )
            }
            val inbox = async { runCatching { repository.loadInbox() } }
            val communities = async { runCatching { repository.loadCommunityInbox() } }
            val inboxResult = inbox.await()
            val communityResult = communities.await()
            _state.update {
                it.copy(
                    isLoading = false,
                    items = inboxResult.getOrDefault(it.items),
                    communities = communityResult.getOrDefault(it.communities),
                    error = inboxResult.exceptionOrNull()?.message
                        ?: if (inboxResult.isFailure) "Messages could not load." else null,
                    communityError = communityResult.exceptionOrNull()?.message
                        ?: if (communityResult.isFailure) "Communities could not load." else null
                )
            }
        }.also { job ->
            job.invokeOnCompletion {
                if (refreshJob === job) refreshJob = null
            }
        }
    }

    fun updateQuery(value: String) {
        _state.update { it.copy(query = value.take(80)) }
    }
}

data class CommunityConversationUiState(
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val context: CommunityConversationContext? = null,
    val messages: List<CommunityMessageItem> = emptyList(),
    val draft: String = "",
    val error: String? = null
)

class CommunityConversationViewModel(
    private val slug: String,
    private val repository: ChatRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CommunityConversationUiState())
    val state: StateFlow<CommunityConversationUiState> = _state.asStateFlow()
    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh(silent: Boolean = false) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = if (silent) it.isLoading else true, error = null) }
            runCatching { repository.loadCommunityConversation(slug) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            context = result.context,
                            messages = result.messages,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Community conversation could not load."
                        )
                    }
                }
        }.also { job ->
            job.invokeOnCompletion {
                if (refreshJob === job) refreshJob = null
            }
        }
    }

    fun updateDraft(value: String) {
        _state.update { it.copy(draft = value.take(4_000), error = null) }
    }

    fun send() {
        val current = _state.value
        val context = current.context ?: return
        val text = current.draft.trim()
        if (text.isEmpty() || current.isSending) return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null) }
            runCatching { repository.sendCommunityText(context, text) }
                .onSuccess { message ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            draft = "",
                            messages = mergeCommunityMessages(it.messages, listOf(message))
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            error = error.message ?: "Community message could not be sent."
                        )
                    }
                }
        }
    }
}

data class ConversationUiState(
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val peerName: String = "",
    val peerHandle: String = "",
    val peerAvatarUrl: String? = null,
    val isOnline: Boolean = false,
    val isRealtimeConnected: Boolean = false,
    val isPeerTyping: Boolean = false,
    val messages: List<ChatMessageItem> = emptyList(),
    val draft: String = "",
    val durationKey: String = "30d",
    val viewOnceEnabled: Boolean = false,
    val mediaInFlight: Boolean = false,
    val activeMediaMessageId: String? = null,
    val identityRecoveryRequired: Boolean = false,
    val identityBackupAvailable: Boolean = false,
    val recoverySecret: String = "",
    val isRecoveringIdentity: Boolean = false,
    val error: String? = null
)

class ConversationViewModel(
    private val conversationId: String,
    private val repository: ChatRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        ConversationUiState(durationKey = repository.loadDefaultDuration(conversationId))
    )
    val state: StateFlow<ConversationUiState> = _state.asStateFlow()
    private var viewerUserId: String? = null
    private var typingStopJob: Job? = null
    private var peerTypingExpiryJob: Job? = null
    private var refreshJob: Job? = null
    private var refreshPending = false

    init {
        refresh()
        observeRealtime()
        observeScreenshots()
    }

    private fun observeScreenshots() {
        viewModelScope.launch {
            var lastAlertAt = 0L
            ChatScreenshotEvents.events.collect {
                val now = System.currentTimeMillis()
                if (now - lastAlertAt < 90_000L) return@collect
                lastAlertAt = now
                val actor = repository.currentViewerName()
                val body = "Suspected screenshot: $actor may have captured this chat."
                runCatching { repository.sendText(conversationId, body, state.value.durationKey) }
                    .onSuccess { message ->
                        _state.update { current ->
                            current.copy(messages = upsertMessage(current.messages, message))
                        }
                    }
            }
        }
    }

    fun refresh(silent: Boolean = false) {
        if (refreshJob?.isActive == true) {
            refreshPending = true
            return
        }
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = if (silent) it.isLoading else true, error = null) }
            runCatching { repository.loadConversation(conversationId) }
                .onSuccess { result ->
                    viewerUserId = result.viewerUserId
                    _state.update {
                        it.copy(
                            isLoading = false,
                            peerName = result.peerName,
                            peerHandle = result.peerHandle,
                            peerAvatarUrl = result.peerAvatarUrl,
                            isOnline = result.isOnline,
                            messages = result.messages,
                            identityRecoveryRequired = false,
                            identityBackupAvailable = false,
                            recoverySecret = "",
                            isRecoveringIdentity = false,
                            error = null
                        )
                    }
                    repository.acknowledgeDelivered(
                        conversationId,
                        result.messages.filterNot(ChatMessageItem::isMine).map(ChatMessageItem::id)
                    )
                    hydrateVisibleMedia(result.messages)
                }
                .onFailure { error ->
                    _state.update {
                        val recovery = error as? ChatRepository.IdentityRecoveryRequired
                        it.copy(
                            isLoading = false,
                            identityRecoveryRequired = recovery != null,
                            identityBackupAvailable = recovery?.backupAvailable == true,
                            error = error.message ?: "Conversation could not load."
                        )
                    }
                }
        }.also { job ->
            job.invokeOnCompletion {
                if (refreshJob === job) {
                    refreshJob = null
                    if (refreshPending) {
                        refreshPending = false
                        refresh(silent = true)
                    }
                }
            }
        }
    }

    fun updateRecoverySecret(value: String) {
        _state.update { it.copy(recoverySecret = value.take(220), error = null) }
    }

    fun restoreIdentity() {
        val secret = state.value.recoverySecret.trim()
        if (secret.isBlank() || state.value.isRecoveringIdentity) return
        viewModelScope.launch {
            _state.update { it.copy(isRecoveringIdentity = true, error = null) }
            runCatching { repository.restoreIdentity(conversationId, secret) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            isRecoveringIdentity = false,
                            identityRecoveryRequired = false,
                            recoverySecret = ""
                        )
                    }
                    refresh()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isRecoveringIdentity = false,
                            error = error.message ?: "Secure-chat identity could not be restored."
                        )
                    }
                }
        }
    }

    fun updateDraft(value: String) {
        _state.update { it.copy(draft = value.take(4_000), error = null) }
        val typing = value.isNotBlank()
        repository.sendTyping(conversationId, typing)
        typingStopJob?.cancel()
        if (typing) {
            typingStopJob = viewModelScope.launch {
                delay(2_000)
                repository.sendTyping(conversationId, false)
            }
        }
    }

    fun updateDurationKey(value: String) {
        if (value in setOf("instant", "1h", "24h", "7d", "30d", "90d")) {
            repository.saveDefaultDuration(conversationId, value)
            _state.update { it.copy(durationKey = value) }
        }
    }

    fun toggleViewOnce() {
        _state.update { it.copy(viewOnceEnabled = !it.viewOnceEnabled) }
    }

    fun sendMedia(
        source: Uri,
        fileName: String,
        mimeType: String,
        width: Int? = null,
        height: Int? = null,
        durationMs: Int? = null
    ) = sendMediaBatch(listOf(ChatMediaDraft(source, fileName, mimeType, width, height, durationMs)))

    fun sendMediaBatch(media: List<ChatMediaDraft>) {
        val selected = media.take(8)
        if (selected.isEmpty() || state.value.mediaInFlight) return
        val viewOnceEnabled = state.value.viewOnceEnabled
        val optimistic = selected.map { item ->
            val viewOnce = viewOnceEnabled && !item.mimeType.startsWith("audio/")
            val label = when {
                item.mimeType.startsWith("audio/") -> "Voice note"
                item.mimeType.startsWith("video/") -> "Video"
                else -> "Photo"
            }
            ChatMessageItem(
                id = "local-media-${UUID.randomUUID()}",
                body = label,
                timestamp = DateTimeFormatter.ofPattern("h:mm a")
                    .withZone(ZoneId.systemDefault()).format(Instant.now()),
                isMine = true,
                isReadable = true,
                deliveryState = ChatDeliveryState.Pending,
                messageKind = "image",
                attachment = ChatAttachmentDto(
                    kind = when {
                        item.mimeType.startsWith("audio/") -> "audio"
                        item.mimeType.startsWith("video/") -> "video"
                        else -> "image"
                    },
                    url = "",
                    mimeType = item.mimeType,
                    width = item.width,
                    height = item.height,
                    durationMs = item.durationMs,
                    viewOnce = viewOnce
                ),
                localMediaUri = item.uri.toString()
            )
        }
        val durationKey = state.value.durationKey
        val caption = state.value.draft
        _state.update {
            it.copy(
                draft = "",
                mediaInFlight = true,
                error = null,
                messages = it.messages + optimistic
            )
        }
        viewModelScope.launch {
            selected.forEachIndexed { index, item ->
                val optimisticId = optimistic[index].id
                val telemetryStartedAt = SystemClock.elapsedRealtime()
                runCatching {
                    repository.sendMedia(
                        conversationId = conversationId,
                        source = item.uri,
                        fileName = item.fileName,
                        mimeType = item.mimeType,
                        width = item.width,
                        height = item.height,
                        durationMs = item.durationMs,
                        viewOnce = viewOnceEnabled && !item.mimeType.startsWith("audio/"),
                        caption = if (index == 0) caption else "",
                        durationKey = durationKey
                    )
                }.onSuccess { sent ->
                    ChatTelemetry.apiAccepted(sent.id, telemetryStartedAt)
                    _state.update {
                        it.copy(messages = upsertMessage(
                            it.messages.filterNot { candidate -> candidate.id == optimisticId },
                            sent
                        ))
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            error = error.message ?: "Media could not be sent.",
                            messages = it.messages.map { candidate ->
                                if (candidate.id == optimisticId) candidate.copy(deliveryState = ChatDeliveryState.Failed)
                                else candidate
                            }
                        )
                    }
                }
            }
            _state.update { it.copy(mediaInFlight = false, viewOnceEnabled = false) }
        }
    }

    fun openMedia(message: ChatMessageItem) {
        if (message.attachment == null || message.mediaLoading || message.mediaConsumed) return
        if (message.localMediaUri != null) {
            _state.update { it.copy(activeMediaMessageId = message.id) }
            return
        }
        _state.update { current ->
            current.copy(messages = current.messages.map {
                if (it.id == message.id) it.copy(mediaLoading = true) else it
            })
        }
        viewModelScope.launch {
            runCatching {
                repository.loadAttachment(
                    conversationId,
                    message,
                    consumeViewOnce = message.attachment.viewOnce
                )
            }.onSuccess { uri ->
                _state.update { current ->
                    current.copy(
                        activeMediaMessageId = message.id,
                        messages = current.messages.map {
                            if (it.id == message.id) it.copy(localMediaUri = uri, mediaLoading = false) else it
                        }
                    )
                }
            }.onFailure { error ->
                _state.update { current ->
                    current.copy(
                        error = error.message ?: "Media could not be opened.",
                        messages = current.messages.map {
                            if (it.id == message.id) it.copy(mediaLoading = false) else it
                        }
                    )
                }
            }
        }
    }

    fun closeViewOnce() {
        val messageId = state.value.activeMediaMessageId ?: return
        val activeMessage = state.value.messages.firstOrNull { it.id == messageId }
        _state.update { current ->
            current.copy(
                activeMediaMessageId = null,
                messages = current.messages.map {
                    if (it.id == messageId && activeMessage?.attachment?.viewOnce == true) {
                        it.copy(localMediaUri = null, mediaConsumed = true)
                    } else it
                }
            )
        }
    }

    private fun hydrateVisibleMedia(messages: List<ChatMessageItem>) {
        messages.filter { it.attachment != null && !it.attachment.viewOnce && it.localMediaUri == null }
            .forEach(::openMedia)
    }

    fun send() {
        val text = state.value.draft.trim()
        if (text.isEmpty()) return
        val optimisticId = "local-${UUID.randomUUID()}"
        val optimisticMessage = ChatMessageItem(
            id = optimisticId,
            body = text,
            timestamp = DateTimeFormatter.ofPattern("h:mm a")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now()),
            isMine = true,
            isReadable = true,
            deliveryState = ChatDeliveryState.Pending
        )
        val durationKey = state.value.durationKey
        _state.update {
            it.copy(
                isSending = false,
                draft = "",
                error = null,
                messages = it.messages + optimisticMessage
            )
        }
        val telemetryStartedAt = SystemClock.elapsedRealtime()
        viewModelScope.launch {
            runCatching { repository.sendText(conversationId, text, durationKey) }
                .onSuccess { message ->
                    ChatTelemetry.apiAccepted(message.id, telemetryStartedAt)
                    _state.update {
                        it.copy(
                            isSending = false,
                            messages = upsertMessage(
                                it.messages.filterNot { candidate -> candidate.id == optimisticId },
                                message
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            error = error.message ?: "Message could not be sent.",
                            messages = it.messages.map { candidate ->
                                if (candidate.id == optimisticId) {
                                    candidate.copy(deliveryState = ChatDeliveryState.Failed)
                                } else candidate
                            }
                        )
                    }
                }
        }
    }

    private fun observeRealtime() {
        viewModelScope.launch {
            var connectingAt = 0L
            repository.realtimeEvents(conversationId).collect { event ->
                when (event) {
                    ChatRealtimeEvent.Connecting -> {
                        connectingAt = SystemClock.elapsedRealtime()
                        ChatTelemetry.transport("connecting")
                        _state.update { it.copy(isRealtimeConnected = false) }
                    }
                    ChatRealtimeEvent.Connected -> {
                        ChatTelemetry.transport(
                            "connected",
                            if (connectingAt > 0) SystemClock.elapsedRealtime() - connectingAt else null
                        )
                        _state.update { it.copy(isRealtimeConnected = true) }
                    }
                    is ChatRealtimeEvent.Disconnected -> {
                        ChatTelemetry.transport("disconnected")
                        _state.update {
                            it.copy(isRealtimeConnected = false, isPeerTyping = false)
                        }
                    }
                    is ChatRealtimeEvent.PeerTyping -> {
                        if (event.userId != viewerUserId) {
                            _state.update { it.copy(isPeerTyping = event.isTyping) }
                            peerTypingExpiryJob?.cancel()
                            if (event.isTyping) {
                                peerTypingExpiryJob = viewModelScope.launch {
                                    delay(3_500)
                                    _state.update { it.copy(isPeerTyping = false) }
                                }
                            }
                        }
                    }
                    is ChatRealtimeEvent.MessageChanged -> viewModelScope.launch {
                        val message = runCatching {
                            repository.receiveRealtimeMessage(conversationId, event.item)
                        }.getOrNull()
                        if (message == null) {
                            refresh(silent = true)
                        } else {
                            if (!message.isMine) {
                                peerTypingExpiryJob?.cancel()
                            }
                            _state.update { current ->
                                current.copy(
                                    messages = upsertMessage(current.messages, message),
                                    isPeerTyping = if (message.isMine) current.isPeerTyping else false
                                )
                            }
                            if (message.attachment != null && !message.attachment.viewOnce) openMedia(message)
                            if (!message.isMine) {
                                repository.acknowledgeDelivered(conversationId, listOf(message.id))
                                // Receipt persistence must never block the incoming bubble. The
                                // active conversation renders the decrypted envelope first, then
                                // records read state independently.
                                viewModelScope.launch {
                                    runCatching { repository.markRead(conversationId, message.id) }
                                }
                            }
                        }
                    }
                    is ChatRealtimeEvent.ReceiptChanged -> {
                        event.messageIds.forEach { ChatTelemetry.receipt(event.kind, it) }
                        val receiptState = if (event.kind == "chat.read") {
                            ChatDeliveryState.Read
                        } else {
                            ChatDeliveryState.Delivered
                        }
                        _state.update { current ->
                            val lastReceiptIndex = current.messages.indexOfLast {
                                it.id in event.messageIds
                            }
                            if (lastReceiptIndex < 0) current else current.copy(
                                messages = current.messages.mapIndexed { index, message ->
                                    if (message.isMine && index <= lastReceiptIndex &&
                                        message.deliveryState != ChatDeliveryState.Read
                                    ) message.copy(deliveryState = receiptState) else message
                                }
                            )
                        }
                    }
                    ChatRealtimeEvent.SyncRequired -> refresh(silent = true)
                }
            }
        }
    }

    override fun onCleared() {
        typingStopJob?.cancel()
        peerTypingExpiryJob?.cancel()
        refreshJob?.cancel()
        repository.sendTyping(conversationId, false)
        super.onCleared()
    }

    private fun upsertMessage(
        messages: List<ChatMessageItem>,
        incoming: ChatMessageItem
    ): List<ChatMessageItem> {
        val index = messages.indexOfFirst { it.id == incoming.id }
        if (index < 0) return messages + incoming
        return messages.toMutableList().apply { set(index, incoming) }
    }
}

class MessagesViewModelFactory(
    private val repository: ChatRepository,
    private val conversationId: String? = null,
    private val communitySlug: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(MessagesInboxViewModel::class.java) ->
            MessagesInboxViewModel(repository) as T
        modelClass.isAssignableFrom(ConversationViewModel::class.java) && conversationId != null ->
            ConversationViewModel(conversationId, repository) as T
        modelClass.isAssignableFrom(CommunityConversationViewModel::class.java) &&
            communitySlug != null ->
            CommunityConversationViewModel(communitySlug, repository) as T
        else -> error("Unknown messages ViewModel: ${modelClass.name}")
    }
}
