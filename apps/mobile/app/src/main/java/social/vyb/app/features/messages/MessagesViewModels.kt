package social.vyb.app.features.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    val query: String = "",
    val error: String? = null
) {
    val filteredItems: List<ChatInboxItem>
        get() {
            val needle = query.trim()
            return if (needle.isEmpty()) items else items.filter {
                it.peerName.contains(needle, true) || it.peerHandle.contains(needle, true)
            }
        }
}

class MessagesInboxViewModel(
    private val repository: ChatRepository
) : ViewModel() {
    private val _state = MutableStateFlow(InboxUiState())
    val state: StateFlow<InboxUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.loadInbox() }
                .onSuccess { items -> _state.update { it.copy(isLoading = false, items = items) } }
                .onFailure { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error.message ?: "Messages could not load.")
                    }
                }
        }
    }

    fun updateQuery(value: String) {
        _state.update { it.copy(query = value) }
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
    private val conversationId: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(MessagesInboxViewModel::class.java) ->
            MessagesInboxViewModel(repository) as T
        modelClass.isAssignableFrom(ConversationViewModel::class.java) && conversationId != null ->
            ConversationViewModel(conversationId, repository) as T
        else -> error("Unknown messages ViewModel: ${modelClass.name}")
    }
}
