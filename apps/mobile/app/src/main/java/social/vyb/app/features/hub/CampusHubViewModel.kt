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

    fun openEventById(eventId: String, onResolved: () -> Unit = {}) {
        val normalizedId = eventId.trim()
        if (normalizedId.isEmpty()) return
        _state.value.events.firstOrNull { it.id == normalizedId }?.let {
            openEvent(it)
            onResolved()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.events.isEmpty(), error = null) }
            runCatching { repository.loadEvent(normalizedId) }
                .onSuccess { loaded ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            selectedEvent = loaded,
                            events = (it.events.filterNot { item -> item.id == loaded.id } + loaded)
                        )
                    }
                    onResolved()
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.displayMessage()) }
                }
        }
    }

    fun closeEvent() {
        _state.update { it.copy(selectedEvent = null, busyId = null, error = null) }
    }

    fun openHostEditor(event: HubEvent? = null) {
        if (event != null && !event.isHostedByViewer) return
        _state.update {
            it.copy(
                hostEditorOpen = true,
                hostEditorEvent = event,
                error = null,
            )
        }
    }

    fun closeHostEditor() {
        if (_state.value.busyId == "host-event") return
        _state.update { it.copy(hostEditorOpen = false, hostEditorEvent = null, error = null) }
    }

    fun saveHostedEvent(draft: HubEventHostDraft) {
        val validationError = draft.validationError()
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }
        if (_state.value.busyId != null) return
        val existing = _state.value.hostEditorEvent
        viewModelScope.launch {
            _state.update { it.copy(busyId = "host-event", error = null) }
            runCatching {
                if (existing == null) repository.createEvent(draft)
                else repository.updateEvent(existing, draft)
            }.fold(
                onSuccess = { (events, eventId) ->
                    val saved = events.firstOrNull { it.id == eventId }
                    _state.update {
                        it.copy(
                            events = events,
                            selectedEvent = saved ?: it.selectedEvent,
                            hostEditorOpen = false,
                            hostEditorEvent = null,
                            busyId = null,
                            error = null,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(busyId = null, error = error.displayMessage()) }
                },
            )
        }
    }

    fun openRegistrationAdmin(event: HubEvent) {
        if (!event.isHostedByViewer || _state.value.registrationsLoading) return
        _state.update {
            it.copy(
                registrationAdminEvent = event,
                hostRegistrations = emptyList(),
                registrationsLoading = true,
                error = null,
            )
        }
        viewModelScope.launch {
            runCatching { repository.loadRegistrations(event.id) }.fold(
                onSuccess = { (loadedEvent, registrations) ->
                    _state.update {
                        it.copy(
                            registrationAdminEvent = loadedEvent,
                            hostRegistrations = registrations,
                            registrationsLoading = false,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(registrationsLoading = false, error = error.displayMessage())
                    }
                },
            )
        }
    }

    fun closeRegistrationAdmin() {
        if (_state.value.busyId?.startsWith("registration:") == true) return
        _state.update {
            it.copy(
                registrationAdminEvent = null,
                hostRegistrations = emptyList(),
                registrationsLoading = false,
                error = null,
            )
        }
    }

    fun reviewRegistration(
        registrationId: String,
        status: String,
        reviewNote: String?,
    ) {
        val event = _state.value.registrationAdminEvent ?: return
        if (_state.value.busyId != null) return
        viewModelScope.launch {
            _state.update {
                it.copy(busyId = "registration:$registrationId", error = null)
            }
            runCatching {
                repository.manageRegistration(event.id, registrationId, status, reviewNote)
            }.fold(
                onSuccess = { (events, updatedEvent, registrations) ->
                    _state.update {
                        it.copy(
                            events = events,
                            selectedEvent = updatedEvent,
                            registrationAdminEvent = updatedEvent,
                            hostRegistrations = registrations,
                            busyId = null,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(busyId = null, error = error.displayMessage()) }
                },
            )
        }
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
