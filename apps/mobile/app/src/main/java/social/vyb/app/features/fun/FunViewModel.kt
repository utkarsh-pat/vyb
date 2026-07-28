package social.vyb.app.features.funhub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FunViewModel(private val repository: FunRepository = FunRepository()) : ViewModel() {
    private val _state = MutableStateFlow(FunUiState())
    val state: StateFlow<FunUiState> = _state.asStateFlow()
    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true
        load(false)
    }

    fun refresh() = load(true)

    private fun load(refreshing: Boolean) = viewModelScope.launch {
        _state.update { it.copy(isLoading = !refreshing, isRefreshing = refreshing, error = null) }
        runCatching { repository.loadHub() }.fold(
            onSuccess = { (inbox, connect, queens) ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        inbox = inbox,
                        connect = connect,
                        queens = queens,
                        connectPath = emptyList(),
                        queenCells = emptySet(),
                        markedCells = emptySet()
                    )
                }
            },
            onFailure = { fail(it) }
        )
    }

    fun readAllNotifications() = action {
        val result = repository.readAll()
        _state.update { current ->
            current.copy(
                inbox = current.inbox.copy(
                    unreadCount = 0,
                    items = current.inbox.items.map {
                        it.copy(state = NotificationReadState(result.readAt))
                    }
                ),
                message = "${result.updatedCount} notifications marked read."
            )
        }
    }

    fun chooseConnect(dot: ConnectDot) {
        val point = Coordinate(dot.x, dot.y)
        _state.update { current ->
            val path = current.connectPath
            current.copy(
                connectPath = when {
                    path.lastOrNull() == point -> path.dropLast(1)
                    point in path -> path.take(path.indexOf(point) + 1)
                    else -> path + point
                },
                message = null
            )
        }
    }

    fun hintConnect() = action {
        val current = _state.value
        val game = requireNotNull(current.connect)
        val hint = repository.connectHint(game.sessionId, current.connectPath)
        _state.update {
            it.copy(
                message = hint.message,
                connectPath = if (hint.nextMove != null && hint.nextMove !in it.connectPath) {
                    it.connectPath + hint.nextMove
                } else it.connectPath
            )
        }
    }

    fun submitConnect() = action {
        val current = _state.value
        val result = repository.submitConnect(requireNotNull(current.connect).sessionId, current.connectPath)
        _state.update { it.copy(message = result.message) }
    }

    fun toggleQueen(point: Coordinate) {
        _state.update { current ->
            val next = current.queenCells.toMutableSet()
            if (!next.add(point)) next.remove(point)
            current.copy(queenCells = next, markedCells = current.markedCells - point, message = null)
        }
    }

    fun hintQueens() = action {
        val current = _state.value
        val game = requireNotNull(current.queens)
        val hint = repository.queensHint(
            game.sessionId,
            current.queenCells.toList(),
            current.markedCells.toList()
        )
        _state.update {
            it.copy(
                message = hint.message,
                queenCells = hint.nextQueen?.let(it.queenCells::plus) ?: it.queenCells,
                markedCells = it.markedCells + hint.autoMarkCells
            )
        }
    }

    fun submitQueens() = action {
        val current = _state.value
        val result = repository.submitQueens(
            requireNotNull(current.queens).sessionId,
            current.queenCells.toList()
        )
        _state.update { it.copy(message = result.message) }
    }

    private fun action(block: suspend () -> Unit) = viewModelScope.launch {
        if (_state.value.isActionRunning) return@launch
        _state.update { it.copy(isActionRunning = true, error = null, message = null) }
        runCatching { block() }.onFailure { error ->
            _state.update { it.copy(error = error.message ?: "Something went wrong.") }
        }
        _state.update { it.copy(isActionRunning = false) }
    }

    private fun fail(error: Throwable) {
        _state.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                error = error.message ?: "Something went wrong."
            )
        }
    }
}
