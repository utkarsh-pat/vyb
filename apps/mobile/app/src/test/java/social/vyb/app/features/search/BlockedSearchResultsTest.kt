package social.vyb.app.features.search

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockedSearchResultsTest {
    @Test
    fun `blocking removes every searchable surface owned by that account`() {
        val blocked = "blocked-user"
        val state = SearchUiState(
            suggestions = listOf(person(blocked), person("visible")),
            results = listOf(person(blocked), person("visible")),
            posts = listOf(content("post-blocked", blocked), content("post-visible", "visible")),
            vibes = listOf(content("vibe-blocked", blocked), content("vibe-visible", "visible")),
            marketplace = listOf(market("market-blocked", blocked), market("market-visible", "visible"))
        )

        val filtered = state.withoutBlockedUser("BLOCKED-USER")

        assertEquals(listOf("visible"), filtered.suggestions.map { it.username })
        assertEquals(listOf("visible"), filtered.results.map { it.username })
        assertEquals(listOf("post-visible"), filtered.posts.map { it.id })
        assertEquals(listOf("vibe-visible"), filtered.vibes.map { it.id })
        assertEquals(listOf("market-visible"), filtered.marketplace.map { it.id })
    }

    private fun person(username: String) = CampusPerson(
        userId = username,
        username = username,
        displayName = username
    )

    private fun content(id: String, username: String) = SearchContentItem(
        id = id,
        title = id,
        body = "",
        authorName = username,
        authorUsername = username
    )

    private fun market(id: String, username: String) = MarketSearchItem(
        id = id,
        kind = MarketSearchKind.Listing,
        title = id,
        description = "",
        category = "",
        priceLabel = "",
        location = "",
        ownerName = username,
        ownerUsername = username
    )
}
