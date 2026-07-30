package social.vyb.app.features.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDiscoveryTest {
    private val dashboard = MarketDashboard(
        listings = listOf(
            listing(
                id = "new-book",
                title = "Database systems book",
                category = "Books",
                price = 700,
                createdAt = "2026-07-30T10:00:00Z",
                saved = true,
            ),
            listing(
                id = "old-cycle",
                title = "Campus cycle",
                category = "Transport",
                price = 2_500,
                createdAt = "2026-07-28T10:00:00Z",
            ),
            listing(
                id = "cheap-book",
                title = "Calculus handbook",
                category = "Books",
                price = 250,
                createdAt = "2026-07-29T10:00:00Z",
            ),
        ),
        requests = listOf(
            request("high", "Need a scientific calculator", 900, "2026-07-29T10:00:00Z"),
            request("open", "Looking for lab coat", null, "2026-07-30T10:00:00Z"),
            request("low", "Need engineering drawing tools", 300, "2026-07-28T10:00:00Z"),
        ),
    )

    @Test
    fun queryCategoryAndSavedFiltersCompose() {
        val visible = dashboard.visibleContent(
            MarketUiState(
                tab = "sale",
                query = "database",
                category = "books",
                showSavedOnly = true,
            )
        )

        assertEquals(listOf("new-book"), visible.listings.map { it.id })
        assertTrue(visible.requests.isEmpty())
    }

    @Test
    fun listingPriceSortsAreDeterministic() {
        val lowToHigh = dashboard.visibleContent(
            MarketUiState(tab = "sale", sort = MarketSort.PriceLowToHigh)
        )
        val highToLow = dashboard.visibleContent(
            MarketUiState(tab = "sale", sort = MarketSort.PriceHighToLow)
        )

        assertEquals(listOf("cheap-book", "new-book", "old-cycle"), lowToHigh.listings.map { it.id })
        assertEquals(listOf("old-cycle", "new-book", "cheap-book"), highToLow.listings.map { it.id })
    }

    @Test
    fun requestPriceSortKeepsOpenBudgetLast() {
        val visible = dashboard.visibleContent(
            MarketUiState(tab = "buying", sort = MarketSort.PriceLowToHigh)
        )

        assertEquals(listOf("low", "high", "open"), visible.requests.map { it.id })
    }

    @Test
    fun categoriesAreScopedToTheActiveTabAndDeduplicated() {
        val categories = dashboard.copy(
            listings = dashboard.listings + listing(
                id = "another-book",
                title = "Another book",
                category = "books",
                price = 100,
                createdAt = "2026-07-27T10:00:00Z",
            )
        ).categoriesFor("sale")

        assertEquals(listOf("Books", "Transport"), categories)
    }

    private fun listing(
        id: String,
        title: String,
        category: String,
        price: Long,
        createdAt: String,
        saved: Boolean = false,
    ) = MarketListing(
        id = id,
        title = title,
        description = "Available on campus",
        category = category,
        priceAmount = price,
        createdAt = createdAt,
        isSaved = saved,
        seller = MarketActor(username = "seller", displayName = "Campus Seller"),
    )

    private fun request(
        id: String,
        title: String,
        budget: Long?,
        createdAt: String,
    ) = MarketRequest(
        id = id,
        title = title,
        detail = "Needed this week",
        category = "Campus essentials",
        budgetAmount = budget,
        createdAt = createdAt,
        requester = MarketActor(username = "buyer", displayName = "Campus Buyer"),
    )
}
