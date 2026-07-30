package social.vyb.app.features.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SocialLinkSafetyTest {
    @Test
    fun normalizesBareAllowedHostToHttps() {
        assertEquals(
            "https://github.com/vyb",
            safeSocialUrl("github", "github.com/vyb")
        )
    }

    @Test
    fun acceptsOfficialSubdomain() {
        assertEquals(
            "https://in.linkedin.com/in/vyb",
            safeSocialUrl("linkedin", "https://in.linkedin.com/in/vyb")
        )
    }

    @Test
    fun normalizesSupportedHandlesAndEmail() {
        assertEquals(
            "https://x.com/vyb",
            safeSocialUrl("twitter", "@vyb")
        )
        assertEquals(
            "https://codeforces.com/profile/tourist",
            safeSocialUrl("codeforces", "tourist")
        )
        assertEquals(
            "mailto:member@college.edu",
            safeSocialUrl("email", "member@college.edu")
        )
    }

    @Test
    fun rejectsSpoofedAndExecutableLinks() {
        assertNull(safeSocialUrl("github", "https://github.com.evil.example/vyb"))
        assertNull(safeSocialUrl("instagram", "javascript:alert(1)"))
        assertNull(safeSocialUrl("linkedin", "https://user@linkedin.com/in/vyb"))
        assertNull(safeSocialUrl("email", "hello@example.com\r\nBcc:attacker@example.com"))
    }
}
