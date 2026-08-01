package social.vyb.app.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VybNetworkTest {
    @Test
    fun `normalizes absolute API origin and preserves path`() {
        assertEquals(
            "https://api.vybnet.app/v1/",
            VybNetwork.normalizeBaseUrl("  https://api.vybnet.app/v1  ")
        )
        assertEquals(
            "http://10.0.2.2:4000/",
            VybNetwork.normalizeBaseUrl("http://10.0.2.2:4000/")
        )
    }

    @Test
    fun `rejects relative credentialed or query-bearing base URLs`() {
        listOf(
            "",
            "/api",
            "https://user:pass@api.vybnet.app",
            "https://api.vybnet.app?token=secret",
            "ftp://api.vybnet.app"
        ).forEach { value ->
            assertThrows(IllegalArgumentException::class.java) {
                VybNetwork.normalizeBaseUrl(value)
            }
        }
    }
}
