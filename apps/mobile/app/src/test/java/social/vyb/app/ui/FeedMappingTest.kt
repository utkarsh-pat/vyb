package social.vyb.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import social.vyb.app.data.RemoteAuthor
import social.vyb.app.data.RemoteMediaAsset
import social.vyb.app.data.RemotePost
import java.time.Instant

class FeedMappingTest {
    @Test
    fun socialAgeUsesCompactPwaStyleLabels() {
        val now = Instant.parse("2026-07-30T12:00:00Z")
        assertEquals("now", formatSocialAge("2026-07-30T11:59:40Z", now))
        assertEquals("9w", formatSocialAge("2026-05-26T12:00:00Z", now))
        assertEquals("", formatSocialAge("not-a-date", now))
    }

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
    fun feedMappingPreservesEveryCarouselItemInBackendOrder() {
        val urls = listOf("first.webp", "clip.mp4", "third.webp")
        val mapped = toFeedPost(
            RemotePost(
                id = "post-carousel",
                body = "Three campus moments",
                kind = "image",
                media = listOf(
                    RemoteMediaAsset("https://cdn.vybnet.app/${urls[0]}", "image", "image/webp"),
                    RemoteMediaAsset("https://cdn.vybnet.app/${urls[1]}", "video", "video/mp4"),
                    RemoteMediaAsset("https://cdn.vybnet.app/${urls[2]}", "image", "image/webp")
                ),
                createdAt = "2026-08-01T12:00:00.000Z",
                author = RemoteAuthor(username = "host", displayName = "Campus Host")
            )
        )

        assertEquals(urls, mapped.media.map { it.url.substringAfterLast('/') })
        assertEquals(listOf("image", "video", "image"), mapped.media.map { it.kind })
    }

    @Test
    fun feedMappingSuppressesLegacyGeneratedTitlesWithoutRemovingUserCopy() {
        val mediaOnly = toFeedPost(
            RemotePost(
                id = "legacy-media",
                title = "Campus update",
                body = "",
                kind = "image",
                mediaUrl = "https://cdn.vybnet.app/media.webp",
                createdAt = "2026-08-01T12:00:00.000Z",
                author = RemoteAuthor(username = "host", displayName = "Campus Host")
            )
        )
        val captioned = toFeedPost(
            RemotePost(
                id = "captioned-media",
                title = "Campus update",
                body = "User-written caption",
                createdAt = "2026-08-01T12:00:00.000Z",
                author = RemoteAuthor(username = "host", displayName = "Campus Host")
            )
        )

        assertEquals("", mediaOnly.title)
        assertEquals("", mediaOnly.body)
        assertEquals("", captioned.title)
        assertEquals("User-written caption", captioned.body)
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
