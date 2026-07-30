package social.vyb.app.features.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundNotificationContentTest {
    @Test
    fun `uses notification copy with data deep link`() {
        val content = buildForegroundNotificationContent(
            data = mapOf("href" to "/messages/abc"),
            notificationTitle = "New message",
            notificationBody = "Hello from campus"
        )

        assertEquals("New message", content?.title)
        assertEquals("Hello from campus", content?.body)
        assertEquals("/messages/abc", content?.href)
    }

    @Test
    fun `supports data-only FCM and defaults to notifications destination`() {
        val content = buildForegroundNotificationContent(
            data = mapOf("title" to "Campus update", "body" to "Event starts soon"),
            notificationTitle = null,
            notificationBody = null
        )

        assertEquals("Campus update", content?.title)
        assertEquals("/notifications", content?.href)
    }

    @Test
    fun `drops empty payload instead of displaying a blank notification`() {
        assertNull(
            buildForegroundNotificationContent(
                data = emptyMap(),
                notificationTitle = null,
                notificationBody = null
            )
        )
    }
}
