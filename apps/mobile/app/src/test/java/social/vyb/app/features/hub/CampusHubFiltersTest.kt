package social.vyb.app.features.hub

import org.junit.Assert.assertEquals
import org.junit.Test

class CampusHubFiltersTest {
    private val events = listOf(
        HubEvent(
            id = "saved-tech",
            title = "Android Build Night",
            club = "GDSC",
            category = "Tech",
            location = "Lab 4",
            status = "published",
            isSaved = true
        ),
        HubEvent(
            id = "hosted-culture",
            title = "Music Auditions",
            club = "Cultural Club",
            category = "Culture",
            status = "draft",
            isHostedByViewer = true
        ),
        HubEvent(
            id = "ended-sports",
            title = "Campus football final",
            category = "Sports",
            status = "ended"
        )
    )

    @Test
    fun savedScopeAndSearchComposeWithoutLeakingOtherEvents() {
        assertEquals(
            listOf("saved-tech"),
            filterHubEvents(events, "android", "Saved", "Tech").map(HubEvent::id)
        )
    }

    @Test
    fun upcomingExcludesDraftsWhileHostingIncludesOwnDraft() {
        assertEquals(
            listOf("saved-tech"),
            filterHubEvents(events, "", "Upcoming", "All").map(HubEvent::id)
        )
        assertEquals(
            listOf("hosted-culture"),
            filterHubEvents(events, "", "Hosting", "All").map(HubEvent::id)
        )
    }

    @Test
    fun endedScopeOnlyShowsCompletedEvents() {
        assertEquals(
            listOf("ended-sports"),
            filterHubEvents(events, "", "Ended", "All").map(HubEvent::id)
        )
    }
}
