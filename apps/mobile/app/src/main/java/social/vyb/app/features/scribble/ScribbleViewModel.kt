package social.vyb.app.features.scribble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ScribbleViewModel(
    private val repository: ScribbleRepository = ScribbleRepository(),
) : ViewModel() {
    private val realtime = ScribbleRealtimeClient(repository::socketUrl)
    private val _state = MutableStateFlow(ScribbleUiState())
    val state: StateFlow<ScribbleUiState> = _state.asStateFlow()
    private var socketJob: Job? = null
    private var lastAction: ScribblePendingAction? = null
    private val drawBuffer = mutableListOf<ScribbleDrawStep>()
    private var drawFlushJob: Job? = null

    init {
        refreshRooms()
    }

    fun refreshRooms() {
        if (_state.value.catalogLoading) return
        viewModelScope.launch {
            _state.update { it.copy(catalogLoading = true, error = null) }
            runCatching { repository.loadPublicRooms() }.fold(
                onSuccess = { rooms ->
                    _state.update { it.copy(catalogLoading = false, rooms = rooms) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            catalogLoading = false,
                            error = error.message ?: "Could not load public Scribble rooms.",
                        )
                    }
                },
            )
        }
    }

    fun createRoom(settings: ScribbleSettings = ScribbleSettings()) {
        connect(ScribblePendingAction.Create(settings))
    }

    fun joinRoom(rawRoomId: String) {
        val roomId = normalizeScribbleRoomCode(rawRoomId)
        if (roomId.length < 4) {
            _state.update { it.copy(error = "Enter a valid Scribble room code.") }
            return
        }
        connect(ScribblePendingAction.Join(roomId))
    }

    fun retryConnection() {
        val roomId = _state.value.snapshot?.roomId
        connect(roomId?.let { ScribblePendingAction.Join(it) } ?: lastAction)
    }

    private fun connect(action: ScribblePendingAction?) {
        socketJob?.cancel()
        lastAction = action
        _state.update {
            it.copy(
                connection = "connecting",
                snapshot = null,
                error = null,
                notice = null,
            )
        }
        socketJob = viewModelScope.launch {
            realtime.observe(action)
                .catch { error ->
                    _state.update {
                        it.copy(
                            connection = "offline",
                            error = error.message ?: "Scribble connection failed.",
                        )
                    }
                }
                .collect(::reduce)
        }
    }

    private fun reduce(event: ScribbleRealtimeEvent) {
        when (event) {
            is ScribbleRealtimeEvent.Connecting -> _state.update {
                it.copy(connection = if (event.reconnecting) "reconnecting" else "connecting")
            }
            ScribbleRealtimeEvent.Connected -> _state.update {
                it.copy(connection = "live", error = null)
            }
            is ScribbleRealtimeEvent.State -> _state.update {
                it.copy(
                    snapshot = mergeScribbleSnapshot(it.snapshot, event.snapshot),
                    connection = "live",
                    error = null,
                )
            }
            is ScribbleRealtimeEvent.Catalog -> _state.update {
                it.copy(rooms = event.rooms, catalogLoading = false)
            }
            is ScribbleRealtimeEvent.Draw -> _state.update {
                it.copy(snapshot = it.snapshot?.withIncomingSteps(event.roomId, event.steps))
            }
            ScribbleRealtimeEvent.CanvasCleared -> _state.update {
                it.copy(snapshot = it.snapshot?.copy(drawing = emptyList()))
            }
            is ScribbleRealtimeEvent.Notice -> _state.update {
                it.copy(notice = event.message)
            }
            is ScribbleRealtimeEvent.Error -> _state.update {
                it.copy(error = event.message)
            }
            is ScribbleRealtimeEvent.Disconnected -> _state.update {
                it.copy(connection = if (event.retrying) "reconnecting" else "offline")
            }
        }
    }

    fun startGame() {
        if (realtime.startGame()) clearTransient()
    }

    fun chooseWord(choiceId: String) {
        if (choiceId.isNotBlank() && realtime.chooseWord(choiceId)) clearTransient()
    }

    fun submitGuess(value: String) {
        if (value.isBlank()) return
        if (realtime.guess(value)) clearTransient()
    }

    fun sendDrawStep(step: ScribbleDrawStep) {
        val snapshot = _state.value.snapshot ?: return
        if (!snapshot.viewerCanDraw) return
        _state.update { it.copy(snapshot = it.snapshot?.withIncomingSteps(snapshot.roomId, listOf(step))) }
        drawBuffer += step
        if (drawFlushJob?.isActive == true) return
        drawFlushJob = viewModelScope.launch {
            delay(20)
            val steps = drawBuffer.take(120)
            drawBuffer.subList(0, steps.size).clear()
            if (!realtime.draw(steps)) {
                _state.update { it.copy(error = "Drawing paused while Scribble reconnects.") }
            }
            drawFlushJob = null
            if (drawBuffer.isNotEmpty()) flushBufferedDrawing()
        }
    }

    fun clearCanvas() {
        val snapshot = _state.value.snapshot ?: return
        if (!snapshot.viewerCanDraw) return
        drawFlushJob?.cancel()
        drawFlushJob = null
        drawBuffer.clear()
        if (realtime.clearCanvas()) {
            _state.update { it.copy(snapshot = it.snapshot?.copy(drawing = emptyList())) }
        }
    }

    fun skipRound() {
        realtime.skipRound()
    }

    fun leaveRoom() {
        drawFlushJob?.cancel()
        drawFlushJob = null
        drawBuffer.clear()
        realtime.leave()
        socketJob?.cancel()
        socketJob = null
        lastAction = null
        _state.update {
            ScribbleUiState(
                rooms = it.rooms,
                catalogLoading = false,
                notice = "You left the Scribble room.",
            )
        }
        refreshRooms()
    }

    fun clearTransient() {
        _state.update { it.copy(error = null, notice = null) }
    }

    override fun onCleared() {
        drawFlushJob?.cancel()
        socketJob?.cancel()
        super.onCleared()
    }

    private fun flushBufferedDrawing() {
        if (drawBuffer.isEmpty() || drawFlushJob?.isActive == true) return
        drawFlushJob = viewModelScope.launch {
            val steps = drawBuffer.take(120)
            drawBuffer.subList(0, steps.size).clear()
            realtime.draw(steps)
            drawFlushJob = null
            if (drawBuffer.isNotEmpty()) flushBufferedDrawing()
        }
    }
}
