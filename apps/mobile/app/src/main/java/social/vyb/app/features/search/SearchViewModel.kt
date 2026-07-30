package social.vyb.app.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val suggestions: List<CampusPerson> = emptyList(),
    val results: List<CampusPerson> = emptyList(),
    val posts: List<SearchContentItem> = emptyList(),
    val vibes: List<SearchContentItem> = emptyList(),
    val marketplace: List<MarketSearchItem> = emptyList(),
    val selectedCategory: SearchCategory = SearchCategory.People,
    val categoryErrors: Map<SearchCategory, String> = emptyMap(),
    val selectedProfile: PublicProfileResponse? = null,
    val loading: Boolean = true,
    val profileLoading: Boolean = false,
    val mutatingUsers: Set<String> = emptySet(),
    val error: String? = null
) {
    val visiblePeople: List<CampusPerson>
        get() = if (query.isBlank()) suggestions else results

    fun resultCount(category: SearchCategory): Int = when (category) {
        SearchCategory.People -> visiblePeople.size
        SearchCategory.Posts -> posts.size
        SearchCategory.Vibes -> vibes.size
        SearchCategory.Marketplace -> marketplace.size
    }
}

class SearchViewModel(
    private val repository: SearchRepository = SearchRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    init {
        loadSuggestions()
    }

    fun updateQuery(query: String) {
        val normalized = query.take(80)
        _state.update { it.copy(query = normalized, error = null) }
        searchJob?.cancel()
        if (normalized.isBlank()) {
            _state.update {
                it.copy(
                    results = emptyList(),
                    posts = emptyList(),
                    vibes = emptyList(),
                    marketplace = emptyList(),
                    selectedCategory = SearchCategory.People,
                    categoryErrors = emptyMap(),
                    loading = false
                )
            }
            if (_state.value.suggestions.isEmpty()) loadSuggestions()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _state.update { it.copy(loading = true) }
            runCatching { repository.search(normalized) }
                .onSuccess { result ->
                    if (_state.value.query == normalized) {
                        _state.update {
                            it.copy(
                                results = result.people,
                                posts = result.posts,
                                vibes = result.vibes,
                                marketplace = result.marketplace,
                                categoryErrors = result.categoryErrors,
                                loading = false,
                                error = null
                            )
                        }
                    }
                }
                .onFailure { fail(it, "Search is unavailable right now.") }
        }
    }

    fun retry() {
        if (_state.value.query.isBlank()) loadSuggestions()
        else {
            repository.invalidateDiscovery()
            updateQuery(_state.value.query)
        }
    }

    fun selectCategory(category: SearchCategory) {
        _state.update { it.copy(selectedCategory = category, error = null) }
    }

    fun openProfile(username: String) {
        viewModelScope.launch {
            _state.update { it.copy(profileLoading = true, error = null) }
            runCatching { repository.profile(username) }
                .onSuccess { profile ->
                    _state.update { it.copy(selectedProfile = profile, profileLoading = false) }
                }
                .onFailure { fail(it, "Profile could not be loaded.") }
        }
    }

    fun closeProfile() {
        _state.update { it.copy(selectedProfile = null, error = null) }
    }

    fun toggleFollow(person: CampusPerson) {
        if (person.username in _state.value.mutatingUsers) return
        val newFollowing = !person.isFollowing
        _state.update {
            it.copy(mutatingUsers = it.mutatingUsers + person.username, error = null)
        }
        viewModelScope.launch {
            runCatching { repository.setFollowing(person.username, newFollowing) }
                .onSuccess { response -> applyFollowResponse(response) }
                .onFailure { fail(it, "Follow status could not be updated.", person.username) }
        }
    }

    private fun loadSuggestions() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { repository.suggested() }
                .onSuccess { people ->
                    _state.update { it.copy(suggestions = people, loading = false) }
                }
                .onFailure { fail(it, "Suggestions could not be loaded.") }
        }
    }

    private fun applyFollowResponse(response: FollowResponse) {
        fun update(person: CampusPerson): CampusPerson =
            if (person.username == response.username) {
                person.copy(isFollowing = response.isFollowing, stats = response.stats)
            } else {
                person
            }

        _state.update { current ->
            val selected = current.selectedProfile
            current.copy(
                suggestions = current.suggestions.map(::update),
                results = current.results.map(::update),
                selectedProfile = if (selected?.profile?.username == response.username) {
                    selected.copy(
                        profile = update(selected.profile),
                        isFollowing = response.isFollowing,
                        stats = response.stats
                    )
                } else {
                    selected
                },
                mutatingUsers = current.mutatingUsers - response.username
            )
        }
    }

    private fun fail(error: Throwable, fallback: String, username: String? = null) {
        _state.update {
            it.copy(
                loading = false,
                profileLoading = false,
                mutatingUsers = username?.let(it.mutatingUsers::minus) ?: it.mutatingUsers,
                error = error.message?.takeIf(String::isNotBlank) ?: fallback
            )
        }
    }
}
