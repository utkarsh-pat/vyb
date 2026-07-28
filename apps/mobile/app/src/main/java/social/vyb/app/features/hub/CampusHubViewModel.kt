package social.vyb.app.features.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CampusHubViewModel(
    private val repository: CampusHubRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CampusHubUiState())
    val state: StateFlow<CampusHubUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun selectTab(tab: CampusHubTab) {
        _state.update { it.copy(selectedTab = tab, error = null) }
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            val firstLoad = _state.value.events.isEmpty() &&
                _state.value.resources.isEmpty() &&
                _state.value.communities.isEmpty()
            _state.update {
                it.copy(isLoading = firstLoad, isRefreshing = !firstLoad, error = null)
            }
            val events = async { runCatching { repository.loadEvents() } }
            val resources = async { runCatching { repository.loadResources() } }
            val communities = async { runCatching { repository.loadCommunities() } }
            val eventResult = events.await()
            val resourceResult = resources.await()
            val communityResult = communities.await()
            val failure = listOf(eventResult, resourceResult, communityResult)
                .firstNotNullOfOrNull { it.exceptionOrNull() }
            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    events = eventResult.getOrDefault(it.events),
                    resources = resourceResult.getOrDefault(it.resources),
                    communities = communityResult.getOrDefault(it.communities),
                    error = failure?.displayMessage()
                )
            }
        }
    }

    fun openEvent(event: HubEvent) {
        _state.update { it.copy(selectedEvent = event, error = null) }
        viewModelScope.launch {
            runCatching { repository.loadEvent(event.id) }
                .onSuccess { loaded ->
                    _state.update {
                        it.copy(
                            selectedEvent = loaded,
                            events = it.events.map { item -> if (item.id == loaded.id) loaded else item }
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(error = error.displayMessage()) } }
        }
    }

    fun closeEvent() {
        _state.update { it.copy(selectedEvent = null, busyId = null, error = null) }
    }

    fun toggleSave(eventId: String) = mutateEvent(eventId) {
        repository.toggleEventSave(eventId)
    }

    fun register(eventId: String) = mutateEvent(eventId) {
        repository.registerEvent(eventId)
    }

    private fun mutateEvent(eventId: String, action: suspend () -> List<HubEvent>) {
        if (_state.value.busyId != null) return
        viewModelScope.launch {
            _state.update { it.copy(busyId = eventId, error = null) }
            runCatching { action() }.fold(
                onSuccess = { events ->
                    _state.update {
                        it.copy(
                            busyId = null,
                            events = events,
                            selectedEvent = events.firstOrNull { event -> event.id == eventId }
                                ?: it.selectedEvent
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(busyId = null, error = error.displayMessage()) }
                }
            )
        }
    }

    fun openCommunity(community: HubCommunity) {
        if (community.slug.isBlank()) return
        _state.update { it.copy(busyId = community.id, error = null) }
        viewModelScope.launch {
            runCatching { repository.loadCommunity(community.slug) }.fold(
                onSuccess = { (detail, members) ->
                    _state.update {
                        it.copy(
                            busyId = null,
                            selectedCommunity = detail,
                            communityMembers = members
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(busyId = null, error = error.displayMessage()) }
                }
            )
        }
    }

    fun closeCommunity() {
        _state.update {
            it.copy(selectedCommunity = null, communityMembers = emptyList(), error = null)
        }
    }

    private fun Throwable.displayMessage() =
        message?.takeIf(String::isNotBlank) ?: "Something went wrong. Please try again."
}

class CampusHubViewModelFactory(
    private val repository: CampusHubRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CampusHubViewModel(repository) as T
}
