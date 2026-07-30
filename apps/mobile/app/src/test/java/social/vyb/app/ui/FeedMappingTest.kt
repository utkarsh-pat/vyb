package social.vyb.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import social.vyb.app.data.RemoteAuthor
import social.vyb.app.data.RemoteMediaAsset
import social.vyb.app.data.RemotePost

class FeedMappingTest {
    @Test
    fun feedMappingPreservesMediaAndViewerState() {
        val mapped = toFeedPost(
            RemotePost(
                id = "post-1",
                title = "Campus fest",
                body = "Tonight at eight",
                kind = "image",
                media = listOf(
                    RemoteMediaAsset("https://cdn.vybnet.app/fest.webp", "image", "image/webp")
                ),
                reactions = 12,
                comments = 3,
                savedCount = 4,
                isSaved = true,
                viewerReactionType = "like",
                createdAt = "2026-07-29T10:00:00.000Z",
                author = RemoteAuthor(
                    userId = "user-1",
                    username = "host",
                    displayName = "Campus Host",
                    avatarUrl = "https://cdn.vybnet.app/avatar.webp"
                )
            )
        )

        assertEquals(1, mapped.media.size)
        assertEquals("image/webp", mapped.media.single().mimeType)
        assertEquals("https://cdn.vybnet.app/avatar.webp", mapped.avatarUrl)
        assertTrue(mapped.isSaved)
        assertEquals("like", mapped.viewerReactionType)
        assertEquals(4, mapped.savedCount)
    }

    @Test
    fun anonymousFeedNeverLeaksAuthorIdentity() {
        val mapped = toFeedPost(
            RemotePost(
                id = "post-2",
                body = "Anonymous campus thought",
                isAnonymous = true,
                createdAt = "2026-07-29T10:00:00.000Z",
                author = RemoteAuthor(
                    username = "real.person",
                    displayName = "Real Person",
                    avatarUrl = "https://cdn.vybnet.app/private.webp"
                )
            )
        )

        assertEquals("Anonymous", mapped.author)
        assertEquals("@anonymous", mapped.handle)
        assertTrue(mapped.isAnonymous)
    }
}
