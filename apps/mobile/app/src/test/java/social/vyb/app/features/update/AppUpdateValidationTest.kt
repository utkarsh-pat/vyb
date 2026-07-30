package social.vyb.app.features.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppUpdateValidationTest {
    private val checksum = "a".repeat(64)

    @Test
    fun `accepts only verified vyb https download hosts`() {
        val result = validateUpdateDownload(
            "https://downloads.vybnet.app/releases/Vyb.apk?token=signed",
            checksum.uppercase()
        )

        assertEquals("downloads.vybnet.app", result.uri.host)
        assertEquals(checksum, result.sha256)
    }

    @Test
    fun `rejects untrusted host insecure scheme and invalid checksum`() {
        assertThrows(IllegalArgumentException::class.java) {
            validateUpdateDownload("https://evil.example/Vyb.apk", checksum)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateUpdateDownload("http://vybnet.app/Vyb.apk", checksum)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateUpdateDownload("https://vybnet.app/Vyb.apk", "not-a-checksum")
        }
    }

    @Test
    fun `sanitizes server version before using it as a file name`() {
        assertEquals("evil-name", safeVersionLabel("../../evil name"))
        assertEquals("update", safeVersionLabel("..."))
    }
}
