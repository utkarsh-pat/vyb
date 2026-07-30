package social.vyb.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileDraftValidatorTest {
    @Test
    fun validCampusProfilePasses() {
        assertNull(validateProfileDraft(validDraft()))
    }

    @Test
    fun invalidUsernameIsRejected() {
        val error = validateProfileDraft(validDraft().copy(username = "_bad user"))
        assertEquals(
            "User ID must be 3–24 characters using letters, numbers, dots, or underscores.",
            error
        )
    }

    @Test
    fun hostellerRequiresHostelName() {
        val error = validateProfileDraft(
            validDraft().copy(isHosteller = true, hostelName = "")
        )
        assertEquals("Hostel name is required for hostellers.", error)
    }

    @Test
    fun yearMustStayInsideBackendContract() {
        val error = validateProfileDraft(validDraft().copy(year = 7))
        assertEquals("Year must be between 1 and 6.", error)
    }

    private fun validDraft() = ProfileDraft(
        username = "utkarsh.patel",
        firstName = "Utkarsh",
        course = "B.Tech",
        stream = "Computer Science and Engineering",
        year = 4,
        section = "A",
        isHosteller = false,
        hostelName = ""
    )
}
