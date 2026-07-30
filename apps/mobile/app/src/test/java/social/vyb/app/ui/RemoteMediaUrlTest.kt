package social.vyb.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteMediaUrlTest {
    @Test
    fun `legacy firebase migration key resolves to canonical R2 proxy key`() {
        assertEquals(
            "https://www.vybnet.app/api/media/social/tenant/posts/feed/user/photo.jpg",
            resolveRemoteMediaUrl(
                "firebase-migration/social/tenant/posts/feed/user/photo.jpg"
            )
        )
    }

    @Test
    fun `same origin legacy proxy URL is canonicalized`() {
        assertEquals(
            "https://www.vybnet.app/api/media/social/tenant/vibes/video.mp4",
            resolveRemoteMediaUrl(
                "https://www.vybnet.app/api/media/firebase-migration/social/tenant/vibes/video.mp4"
            )
        )
    }

    @Test
    fun `valid third party absolute URL remains unchanged`() {
        val url = "https://images.example.test/photo.jpg?width=640"
        assertEquals(url, resolveRemoteMediaUrl(url))
    }
}
