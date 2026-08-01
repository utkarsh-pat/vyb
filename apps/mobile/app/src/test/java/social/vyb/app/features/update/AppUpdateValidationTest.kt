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

    @Test
    fun `minimum supported version makes update mandatory without force flag`() {
        val manifest = updateManifest(
            minimumSupportedVersionCode = 8,
            forceUpdate = false
        )

        assertEquals(true, manifest.isMandatoryFor(currentVersionCode = 7))
        assertEquals(false, manifest.isMandatoryFor(currentVersionCode = 8))
    }

    @Test
    fun `rejects stale or inconsistent available update metadata`() {
        assertThrows(IllegalArgumentException::class.java) {
            validateUpdateManifest(
                updateManifest(latestVersionCode = 7),
                currentVersionCode = 7
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateUpdateManifest(
                updateManifest(
                    latestVersionCode = 9,
                    minimumSupportedVersionCode = 10
                ),
                currentVersionCode = 7
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateUpdateManifest(
                updateManifest(platform = "ios"),
                currentVersionCode = 7
            )
        }
    }

    private fun updateManifest(
        platform: String = "android",
        latestVersionCode: Int = 8,
        minimumSupportedVersionCode: Int = 1,
        forceUpdate: Boolean = false
    ) = AndroidUpdateManifest(
        platform = platform,
        latestVersionCode = latestVersionCode,
        latestVersionName = "0.1.8",
        minimumSupportedVersionCode = minimumSupportedVersionCode,
        forceUpdate = forceUpdate,
        apkUrl = "https://downloads.vybnet.app/Vyb.apk",
        apkSha256 = checksum,
        updateAvailable = true
    )
}
