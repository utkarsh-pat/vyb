package social.vyb.app.features.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProfileEditDraftTest {
    @Test
    fun validDraftMapsToCanonicalRequest() {
        val request = validDraft().copy(
            section = "a",
            bio = "  Building Vyb  ",
            github = " github.com/utkarsh ",
            instagram = ""
        ).toRequest()

        assertEquals("A", request.section)
        assertEquals("Building Vyb", request.bio)
        assertEquals(mapOf("github" to "github.com/utkarsh"), request.socialLinks)
    }

    @Test
    fun invalidUsernameIsRejectedBeforeNetworkCall() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            validDraft().copy(username = "Bad User").toRequest()
        }
        assertEquals(
            "User ID must be 3–24 characters using lowercase letters, numbers, dots, or underscores.",
            error.message
        )
    }

    @Test
    fun hostellerMustProvideHostelName() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            validDraft().copy(isHosteller = true, hostelName = "").toRequest()
        }
        assertEquals("Hostel name is required for hostellers.", error.message)
    }

    private fun validDraft() = ProfileEditDraft(
        username = "utkarsh.patel",
        firstName = "Utkarsh",
        lastName = "Patel",
        course = "B.Tech",
        stream = "Computer Science and Engineering",
        year = "4",
        section = "A",
        isHosteller = false
    )
}
