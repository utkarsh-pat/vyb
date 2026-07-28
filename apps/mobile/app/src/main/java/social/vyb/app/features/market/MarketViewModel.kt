package social.vyb.app.features.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MarketUiState(
    val dashboard: MarketDashboard? = null,
    val loading: Boolean = true,
    val mutating: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val selected: MarketDetail? = null,
    val showComposer: Boolean = false,
    val showSavedOnly: Boolean = false,
    val tab: String = "sale",
)

class MarketViewModel(
    private val repository: MarketRepository = MarketRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(MarketUiState())
    val state: StateFlow<MarketUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = launch(load = true) { repository.dashboard() }

    fun select(detail: MarketDetail?) {
        _state.value = _state.value.copy(selected = detail, error = null)
    }

    fun selectTab(tab: String) {
        _state.value = _state.value.copy(tab = tab, selected = null)
    }

    fun toggleSavedOnly() {
        _state.value = _state.value.copy(showSavedOnly = !_state.value.showSavedOnly)
    }

    fun setComposer(show: Boolean) {
        _state.value = _state.value.copy(showComposer = show, error = null)
    }

    fun create(draft: MarketPostDraft) {
        if (draft.title.isBlank() || draft.category.isBlank() || draft.description.isBlank()) {
            _state.value = _state.value.copy(error = "Title, category and description are required.")
            return
        }
        if (draft.tab == "sale" && (draft.amount ?: 0) <= 0) {
            _state.value = _state.value.copy(error = "Enter a valid listing price.")
            return
        }
        launch(notice = "Market post published.") { repository.create(draft) }
        _state.value = _state.value.copy(showComposer = false)
    }

    fun toggleSave(listingId: String) =
        launch(notice = "Saved items updated.") { repository.toggleSave(listingId) }

    fun contact(target: MarketDetail, message: String) {
        if (message.isBlank()) {
            _state.value = _state.value.copy(error = "Write a short message first.")
            return
        }
        launch(notice = "Message sent to the owner.") { repository.contact(target, message) }
        _state.value = _state.value.copy(selected = null)
    }

    fun markSold(listingId: String) =
        launch(notice = "Listing marked as sold.") { repository.markSold(listingId) }

    fun clearMessage() {
        _state.value = _state.value.copy(error = null, notice = null)
    }

    private fun launch(
        load: Boolean = false,
        notice: String? = null,
        block: suspend () -> MarketDashboard,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = load,
                mutating = !load,
                error = null,
                notice = null,
            )
            runCatching { block() }
                .onSuccess { dashboard ->
                    val selectedId = _state.value.selected?.id
                    _state.value = _state.value.copy(
                        dashboard = dashboard,
                        loading = false,
                        mutating = false,
                        notice = notice,
                        selected = selectedId?.let { id ->
                            dashboard.listings.firstOrNull { it.id == id }?.let(MarketDetail::Listing)
                                ?: dashboard.requests.firstOrNull { it.id == id }?.let(MarketDetail::Request)
                        },
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        mutating = false,
                        error = error.message ?: "Something went wrong.",
                    )
                }
        }
    }
}
