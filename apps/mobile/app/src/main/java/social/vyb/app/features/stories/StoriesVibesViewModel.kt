package social.vyb.app.features.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StoriesVibesViewModel(
    private val repository: StoriesVibesRepository = StoriesVibesRepository()
) : ViewModel() {
    private val _stories = MutableStateFlow(StoriesUiState())
    val stories: StateFlow<StoriesUiState> = _stories.asStateFlow()

    private val _vibes = MutableStateFlow(VibesUiState())
    val vibes: StateFlow<VibesUiState> = _vibes.asStateFlow()

    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            _stories.update { it.copy(isLoading = true, error = null) }
            _vibes.update { it.copy(isLoading = true, error = null) }
            val storiesRequest = async { runCatching { repository.loadStories() } }
            val vibesRequest = async { runCatching { repository.loadVibes() } }
            storiesRequest.await().fold(
                onSuccess = { items ->
                    _stories.update { it.copy(isLoading = false, items = items) }
                },
                onFailure = { error ->
                    _stories.update { it.copy(isLoading = false, error = error.displayMessage()) }
                }
            )
            vibesRequest.await().fold(
                onSuccess = { result ->
                    _vibes.update {
                        it.copy(
                            isLoading = false,
                            items = result.items,
                            nextCursor = result.nextCursor
                        )
                    }
                },
                onFailure = { error ->
                    _vibes.update { it.copy(isLoading = false, error = error.displayMessage()) }
                }
            )
        }
    }

    fun refreshStories() {
        viewModelScope.launch {
            _stories.update { it.copy(isRefreshing = true, error = null) }
            runCatching { repository.loadStories() }.fold(
                onSuccess = { items ->
                    _stories.update {
                        it.copy(isRefreshing = false, items = items, selectedIndex = null)
                    }
                },
                onFailure = { error ->
                    _stories.update {
                        it.copy(isRefreshing = false, error = error.displayMessage())
                    }
                }
            )
        }
    }

    fun refreshVibes() {
        viewModelScope.launch {
            _vibes.update { it.copy(isRefreshing = true, error = null) }
            runCatching { repository.loadVibes() }.fold(
                onSuccess = { result ->
                    _vibes.update {
                        it.copy(
                            isRefreshing = false,
                            items = result.items,
                            nextCursor = result.nextCursor
                        )
                    }
                },
                onFailure = { error ->
                    _vibes.update {
                        it.copy(isRefreshing = false, error = error.displayMessage())
                    }
                }
            )
        }
    }

    fun loadMoreVibes() {
        val state = _vibes.value
        val cursor = state.nextCursor ?: return
        if (state.isLoadingMore) return
        viewModelScope.launch {
            _vibes.update { it.copy(isLoadingMore = true, error = null) }
            runCatching { repository.loadVibes(cursor) }.fold(
                onSuccess = { result ->
                    _vibes.update {
                        it.copy(
                            isLoadingMore = false,
                            items = (it.items + result.items).distinctBy(VibeItem::id),
                            nextCursor = result.nextCursor
                        )
                    }
                },
                onFailure = { error ->
                    _vibes.update {
                        it.copy(isLoadingMore = false, error = error.displayMessage())
                    }
                }
            )
        }
    }

    fun openStory(index: Int) {
        val story = _stories.value.items.getOrNull(index) ?: return
        _stories.update { it.copy(selectedIndex = index) }
        if (!story.viewerHasSeen) markStorySeen(story.id)
    }

    fun closeStory() {
        _stories.update { it.copy(selectedIndex = null) }
    }

    fun nextStory() {
        val state = _stories.value
        val next = (state.selectedIndex ?: return) + 1
        if (next >= state.items.size) closeStory() else openStory(next)
    }

    fun previousStory() {
        val previous = ((_stories.value.selectedIndex ?: return) - 1).coerceAtLeast(0)
        openStory(previous)
    }

    fun toggleSelectedStoryLike() {
        _stories.value.selectedStory?.let { toggleStoryLike(it.id) }
    }

    fun toggleStoryLike(storyId: String) {
        if (storyId in _stories.value.busyStoryIds) return
        viewModelScope.launch {
            _stories.update { it.copy(busyStoryIds = it.busyStoryIds + storyId, error = null) }
            runCatching { repository.toggleStoryLike(storyId) }.fold(
                onSuccess = { response ->
                    _stories.update { state ->
                        state.copy(
                            busyStoryIds = state.busyStoryIds - storyId,
                            items = state.items.map { story ->
                                if (story.id == storyId) {
                                    story.copy(
                                        reactions = response.aggregateCount,
                                        viewerHasLiked = response.active
                                    )
                                } else story
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _stories.update {
                        it.copy(
                            busyStoryIds = it.busyStoryIds - storyId,
                            error = error.displayMessage()
                        )
                    }
                }
            )
        }
    }

    fun toggleVibeLike(vibeId: String) {
        if (vibeId in _vibes.value.busyVibeIds) return
        viewModelScope.launch {
            _vibes.update { it.copy(busyVibeIds = it.busyVibeIds + vibeId, error = null) }
            runCatching { repository.toggleVibeLike(vibeId) }.fold(
                onSuccess = { response ->
                    _vibes.update { state ->
                        state.copy(
                            busyVibeIds = state.busyVibeIds - vibeId,
                            items = state.items.map { vibe ->
                                if (vibe.id == vibeId) {
                                    vibe.copy(
                                        reactions = response.aggregateCount,
                                        viewerReactionType = response.viewerReactionType
                                    )
                                } else vibe
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _vibes.update {
                        it.copy(
                            busyVibeIds = it.busyVibeIds - vibeId,
                            error = error.displayMessage()
                        )
                    }
                }
            )
        }
    }

    private fun markStorySeen(storyId: String) {
        viewModelScope.launch {
            _stories.update { state ->
                state.copy(
                    items = state.items.map { story ->
                        if (story.id == storyId) story.copy(viewerHasSeen = true) else story
                    }
                )
            }
            runCatching { repository.markStorySeen(storyId) }.onFailure { error ->
                _stories.update { state ->
                    state.copy(
                        error = error.displayMessage(),
                        items = state.items.map { story ->
                            if (story.id == storyId) story.copy(viewerHasSeen = false) else story
                        }
                    )
                }
            }
        }
    }

    private fun Throwable.displayMessage(): String =
        message?.takeIf(String::isNotBlank) ?: "Something went wrong. Please try again."
}
