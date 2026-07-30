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
    val query: String = "",
    val category: String? = null,
    val sort: MarketSort = MarketSort.Recent,
)

enum class MarketSort(val label: String) {
    Recent("Recent"),
    PriceLowToHigh("Price: Low"),
    PriceHighToLow("Price: High"),
}

internal data class MarketVisibleContent(
    val listings: List<MarketListing> = emptyList(),
    val requests: List<MarketRequest> = emptyList(),
)

internal fun MarketDashboard.visibleContent(state: MarketUiState): MarketVisibleContent {
    val normalizedQuery = state.query.trim()
    val normalizedCategory = state.category?.trim().orEmpty()

    val listings = listings.asSequence()
        .filter { state.tab == "sale" }
        .filter { !state.showSavedOnly || it.isSaved }
        .filter { normalizedCategory.isBlank() || it.category.equals(normalizedCategory, ignoreCase = true) }
        .filter { listing ->
            normalizedQuery.isBlank() || listOf(
                listing.title,
                listing.description,
                listing.category,
                listing.condition,
                listing.location,
                listing.campusSpot,
                listing.seller.displayName,
                listing.seller.username,
            ).any { it.contains(normalizedQuery, ignoreCase = true) }
        }
        .sortedWith(
            when (state.sort) {
                MarketSort.Recent -> compareByDescending<MarketListing> { it.createdAt }
                    .thenByDescending { it.id }
                MarketSort.PriceLowToHigh -> compareBy<MarketListing> { it.priceAmount }
                    .thenByDescending { it.createdAt }
                MarketSort.PriceHighToLow -> compareByDescending<MarketListing> { it.priceAmount }
                    .thenByDescending { it.createdAt }
            }
        )
        .toList()

    val requests = requests.asSequence()
        .filter { !state.showSavedOnly && it.tab == state.tab }
        .filter { normalizedCategory.isBlank() || it.category.equals(normalizedCategory, ignoreCase = true) }
        .filter { request ->
            normalizedQuery.isBlank() || listOf(
                request.title,
                request.detail,
                request.category,
                request.tag,
                request.campusSpot,
                request.budgetLabel,
                request.requester.displayName,
                request.requester.username,
            ).any { it.contains(normalizedQuery, ignoreCase = true) }
        }
        .sortedWith(
            when (state.sort) {
                MarketSort.Recent -> compareByDescending<MarketRequest> { it.createdAt }
                    .thenByDescending { it.id }
                MarketSort.PriceLowToHigh -> compareBy<MarketRequest> {
                    it.budgetAmount ?: Long.MAX_VALUE
                }.thenByDescending { it.createdAt }
                MarketSort.PriceHighToLow -> compareByDescending<MarketRequest> {
                    it.budgetAmount ?: Long.MIN_VALUE
                }.thenByDescending { it.createdAt }
            }
        )
        .toList()

    return MarketVisibleContent(listings = listings, requests = requests)
}

internal fun MarketDashboard.categoriesFor(tab: String): List<String> =
    (if (tab == "sale") listings.map(MarketListing::category)
    else requests.filter { it.tab == tab }.map(MarketRequest::category))
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }

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
        _state.value = _state.value.copy(
            tab = tab,
            selected = null,
            category = null,
            showSavedOnly = if (tab == "sale") _state.value.showSavedOnly else false,
        )
    }

    fun toggleSavedOnly() {
        val enabled = !_state.value.showSavedOnly
        _state.value = _state.value.copy(
            showSavedOnly = enabled,
            tab = if (enabled) "sale" else _state.value.tab,
            category = if (enabled && _state.value.tab != "sale") null else _state.value.category,
            selected = null,
        )
    }

    fun setQuery(query: String) {
        _state.value = _state.value.copy(query = query.take(120))
    }

    fun selectCategory(category: String?) {
        _state.value = _state.value.copy(category = category?.trim()?.takeIf(String::isNotBlank))
    }

    fun selectSort(sort: MarketSort) {
        _state.value = _state.value.copy(sort = sort)
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
