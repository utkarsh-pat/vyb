package social.vyb.app.features.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import social.vyb.app.data.RemoteAuthor
import social.vyb.app.data.RemoteMediaAsset
import social.vyb.app.data.RemotePost

class UniversalSearchMatchingTest {
    @Test
    fun searchRequiresEveryTokenAcrossAllIndexedFields() {
        val values = listOf(
            "Scientific calculator",
            "Barely used for first-year maths",
            "Hostel Block C"
        )

        assertTrue(matchesSearchQuery("calculator hostel", values))
        assertTrue(matchesSearchQuery("  FIRST-year   MATHS ", values))
        assertFalse(matchesSearchQuery("calculator laptop", values))
    }

    @Test
    fun postSearchIncludesAuthorAndLocation() {
        val post = remotePost()

        assertTrue(post.matchesSearchQuery("utkarsh library"))
        assertTrue(post.matchesSearchQuery("compose workshop"))
        assertFalse(post.matchesSearchQuery("market bicycle"))
    }

    @Test
    fun contentMappingUsesPrimaryMediaAndProtectsAnonymousIdentity() {
        val item = remotePost(anonymous = true).toSearchContent()

        assertEquals("Anonymous", item.authorName)
        assertEquals("anonymous", item.authorUsername)
        assertEquals("https://cdn.example/post.mp4", item.mediaUrl)
        assertEquals("video", item.mediaKind)
    }

    private fun remotePost(anonymous: Boolean = false) = RemotePost(
        id = "post-1",
        title = "Compose workshop",
        body = "Meet near the library",
        kind = "video",
        media = listOf(
            RemoteMediaAsset(
                url = "https://cdn.example/post.mp4",
                kind = "video"
            )
        ),
        location = "Central Library",
        isAnonymous = anonymous,
        createdAt = "2026-07-30T10:00:00Z",
        author = RemoteAuthor(
            userId = "user-1",
            username = "utkarsh",
            displayName = "Utkarsh Patel",
            isAnonymous = anonymous
        )
    )
}
