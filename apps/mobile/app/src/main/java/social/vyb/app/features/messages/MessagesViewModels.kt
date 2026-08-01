package social.vyb.app.features.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import social.vyb.app.features.realtime.ChatRealtimeEvent

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
    val isOnline: Boolean = false,
    val isRealtimeConnected: Boolean = false,
    val isPeerTyping: Boolean = false,
    val messages: List<ChatMessageItem> = emptyList(),
    val draft: String = "",
    val error: String? = null
)

class ConversationViewModel(
    private val conversationId: String,
    private val repository: ChatRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ConversationUiState())
    val state: StateFlow<ConversationUiState> = _state.asStateFlow()
    private var viewerUserId: String? = null
    private var typingStopJob: Job? = null
    private var peerTypingExpiryJob: Job? = null

    init {
        refresh()
        observeRealtime()
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = if (silent) it.isLoading else true, error = null) }
            runCatching { repository.loadConversation(conversationId) }
                .onSuccess { result ->
                    viewerUserId = result.viewerUserId
                    _state.update {
                        it.copy(
                            isLoading = false,
                            peerName = result.peerName,
                            peerHandle = result.peerHandle,
                            isOnline = result.isOnline,
                            messages = result.messages
                        )
                    }
                    repository.acknowledgeDelivered(
                        conversationId,
                        result.messages.filterNot(ChatMessageItem::isMine).map(ChatMessageItem::id)
                    )
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error.message ?: "Conversation could not load.")
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

    fun send() {
        val text = state.value.draft.trim()
        if (text.isEmpty() || state.value.isSending) return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null) }
            runCatching { repository.sendText(conversationId, text) }
                .onSuccess { message ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            draft = "",
                            messages = it.messages + message
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isSending = false, error = error.message ?: "Message could not be sent.")
                    }
                }
        }
    }

    private fun observeRealtime() {
        viewModelScope.launch {
            repository.realtimeEvents(conversationId).collect { event ->
                when (event) {
                    ChatRealtimeEvent.Connecting ->
                        _state.update { it.copy(isRealtimeConnected = false) }
                    ChatRealtimeEvent.Connected ->
                        _state.update { it.copy(isRealtimeConnected = true) }
                    is ChatRealtimeEvent.Disconnected ->
                        _state.update {
                            it.copy(isRealtimeConnected = false, isPeerTyping = false)
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
                    is ChatRealtimeEvent.MessageChanged,
                    is ChatRealtimeEvent.ReadChanged,
                    ChatRealtimeEvent.SyncRequired -> refresh(silent = true)
                }
            }
        }
    }

    override fun onCleared() {
        typingStopJob?.cancel()
        peerTypingExpiryJob?.cancel()
        repository.sendTyping(conversationId, false)
        super.onCleared()
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
