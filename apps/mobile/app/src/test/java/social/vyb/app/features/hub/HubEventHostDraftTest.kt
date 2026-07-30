package social.vyb.app.features.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HubEventHostDraftTest {
    private val validDraft = HubEventHostDraft(
        title = "Campus design meetup",
        club = "Design Club",
        category = "Workshop",
        description = "A practical campus design systems workshop.",
        location = "Innovation Lab",
        startsAt = "2026-08-15T10:00:00Z",
        endsAt = "2026-08-15T12:00:00Z",
        passKind = "free",
        passLabel = "Free entry",
        capacity = "80",
        responseMode = "apply",
        registrationClosesAt = "2026-08-14T10:00:00Z",
    )

    @Test
    fun validCoreHostDraftBuildsBackendRequest() {
        assertNull(validDraft.validationError(nowEpochMillis = 0L))

        val request = validDraft.toRequest()

        assertEquals("Campus design meetup", request.title)
        assertEquals(80, request.capacity)
        assertEquals("apply", request.responseMode)
        assertEquals("individual", request.entryMode)
        assertTrue(request.media.isEmpty())
        assertTrue(request.formFields.isEmpty())
    }

    @Test
    fun editRequestPreservesExistingMediaAndAdvancedRegistrationConfig() {
        val event = HubEvent(
            id = "event-1",
            title = "Original",
            media = listOf(
                HubEventMediaAsset(id = "media-1", url = "https://cdn.vybnet.app/poster.webp")
            ),
            registrationConfig = HubEventRegistrationConfig(
                entryMode = "team",
                teamSizeMin = 2,
                teamSizeMax = 4,
                allowAttachments = true,
                attachmentLabel = "Project proof",
                formFields = listOf(
                    HubEventFormField(
                        id = "portfolio",
                        label = "Portfolio",
                        required = true,
                    )
                ),
            ),
        )

        val request = validDraft.toRequest(event)

        assertEquals(listOf("media-1"), request.keepMediaIds)
        assertEquals("team", request.entryMode)
        assertEquals(2, request.teamSizeMin)
        assertEquals(4, request.teamSizeMax)
        assertTrue(request.allowAttachments)
        assertEquals("portfolio", request.formFields.single().id)
        assertTrue(request.media.isEmpty())
    }

    @Test
    fun invalidTimingAndCapacityAreRejectedBeforeNetworkMutation() {
        val endBeforeStart = validDraft.copy(endsAt = "2026-08-15T09:00:00Z")
        val invalidCapacity = validDraft.copy(capacity = "0")

        assertTrue(endBeforeStart.validationError(nowEpochMillis = 0L)!!.contains("after"))
        assertTrue(invalidCapacity.validationError(nowEpochMillis = 0L)!!.contains("positive"))
    }

    @Test
    fun registrationMustCloseBeforeStart() {
        val closesAfterStart = validDraft.copy(
            registrationClosesAt = "2026-08-15T11:00:00Z"
        )

        assertTrue(closesAfterStart.validationError(nowEpochMillis = 0L)!!.contains("before"))
    }
}
