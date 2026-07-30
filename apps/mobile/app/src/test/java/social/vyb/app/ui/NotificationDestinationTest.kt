package social.vyb.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationDestinationTest {
    @Test
    fun campusLoadErrorsDoNotExposeInternalNetworkDetails() {
        assertEquals(
            "We couldn't reach Vyb. Check your connection and try again.",
            userFacingCampusLoadError(
                "failed to connect to /10.0.2.2 (port 4000) from /100.69.156.20"
            )
        )
        assertEquals(
            "Your campus access could not be verified. Sign in again and retry.",
            userFacingCampusLoadError("PERMISSION_DENIED")
        )
    }

    @Test
    fun relativeProfileLinkTargetsNativeProfileRoute() {
        assertEquals("search-profile/utkarsh.patel", notificationDestination("/u/utkarsh.patel"))
    }

    @Test
    fun supportedWebLinksMapToNativeDestinations() {
        assertEquals("market", notificationDestination("https://vybnet.app/market?listing=123"))
        assertEquals("messages", notificationDestination("/messages/chat-123"))
        assertEquals(
            "messages/community/design-club",
            notificationDestination("/messages/community/design-club")
        )
        assertEquals("hub", notificationDestination("/events/host"))
    }

    @Test
    fun externalHostsCannotControlNativeNavigation() {
        assertEquals("home", notificationDestination("https://example.com/messages/private"))
    }

    @Test
    fun malformedOrUnknownTargetsFallBackHome() {
        assertEquals("home", notificationDestination("not a valid uri"))
        assertEquals("home", notificationDestination("/unexpected"))
    }
}
