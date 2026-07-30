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
    fun rejectsSpoofedAndExecutableLinks() {
        assertNull(safeSocialUrl("github", "https://github.com.evil.example/vyb"))
        assertNull(safeSocialUrl("instagram", "javascript:alert(1)"))
        assertNull(safeSocialUrl("linkedin", "https://user@linkedin.com/in/vyb"))
    }
}
