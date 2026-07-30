package social.vyb.app.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationUiState(
    val loading: Boolean = true,
    val mutating: Boolean = false,
    val filter: String = "all",
    val items: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0,
    val error: String? = null,
    val notice: String? = null
)

class NotificationViewModel(
    private val repository: NotificationRepository = NotificationRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationUiState())
    val state: StateFlow<NotificationUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun setFilter(filter: String) {
        if (filter !in setOf("all", "unread", "read") || filter == _state.value.filter) return
        _state.update { it.copy(filter = filter) }
        refresh()
    }

    fun refresh() {
        val filter = _state.value.filter
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, notice = null) }
            runCatching { repository.list(filter) }
                .onSuccess { inbox ->
                    if (_state.value.filter == filter) {
                        _state.update {
                            it.copy(
                                loading = false,
                                items = inbox.items,
                                unreadCount = inbox.unreadCount
                            )
                        }
                    }
                }
                .onFailure { fail(it, "Notifications could not be loaded.") }
        }
    }

    fun open(item: NotificationItem, navigate: (String) -> Unit) {
        if (_state.value.mutating) return
        viewModelScope.launch {
            _state.update { it.copy(mutating = true, error = null) }
            val updated = if (item.state.readAt == null) {
                runCatching { repository.markRead(item.id) }
                    .getOrElse {
                        fail(it, "Notification could not be marked as read.")
                        return@launch
                    }
            } else item
            _state.update { current ->
                current.copy(
                    mutating = false,
                    items = current.items.map { if (it.id == updated.id) updated else it },
                    unreadCount = if (item.state.readAt == null) {
                        (current.unreadCount - 1).coerceAtLeast(0)
                    } else current.unreadCount
                )
            }
            navigate(updated.copy.href)
        }
    }

    fun markAllRead() {
        if (_state.value.mutating || _state.value.unreadCount == 0) return
        viewModelScope.launch {
            _state.update { it.copy(mutating = true, error = null, notice = null) }
            runCatching { repository.markAllRead() }
                .onSuccess { result ->
                    _state.update { current ->
                        current.copy(
                            mutating = false,
                            unreadCount = 0,
                            items = if (current.filter == "unread") {
                                emptyList()
                            } else {
                                current.items.map {
                                    it.copy(state = it.state.copy(readAt = it.state.readAt ?: result.readAt))
                                }
                            },
                            notice = "${result.updatedCount} notifications marked read."
                        )
                    }
                }
                .onFailure { fail(it, "Notifications could not be updated.") }
        }
    }

    private fun fail(error: Throwable, fallback: String) {
        _state.update {
            it.copy(
                loading = false,
                mutating = false,
                error = error.message?.takeIf(String::isNotBlank) ?: fallback
            )
        }
    }
}
