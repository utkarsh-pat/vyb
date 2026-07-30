package social.vyb.app.features.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import social.vyb.app.data.RemoteAuthor
import social.vyb.app.data.RemotePost

class CommunityMessageMappingTest {
    @Test
    fun ownPostMapsToReadableCommunityMessage() {
        val message = remotePost(
            id = "post-1",
            body = "Hello community",
            authorUserId = "viewer-1"
        ).toCommunityMessage(viewerUserId = "viewer-1")

        assertEquals("Hello community", message.body)
        assertEquals("Utkarsh", message.authorName)
        assertEquals("@utkarsh", message.authorHandle)
        assertTrue(message.isMine)
        assertFalse(message.isAnonymous)
    }

    @Test
    fun anonymousPostNeverLeaksAuthorOrOwnState() {
        val message = remotePost(
            id = "post-2",
            body = "",
            authorUserId = "viewer-1",
            anonymous = true
        ).toCommunityMessage(viewerUserId = "viewer-1")

        assertEquals("Fallback title", message.body)
        assertEquals("Anonymous", message.authorName)
        assertEquals("@anonymous", message.authorHandle)
        assertFalse(message.isMine)
        assertTrue(message.isAnonymous)
    }

    @Test
    fun mergingMessagesDeduplicatesAndOrdersChronologically() {
        val newer = remotePost(
            id = "newer",
            body = "Second",
            authorUserId = "user-2",
            createdAt = "2026-07-30T10:01:00Z"
        ).toCommunityMessage("viewer-1")
        val older = remotePost(
            id = "older",
            body = "First",
            authorUserId = "user-1",
            createdAt = "2026-07-30T10:00:00Z"
        ).toCommunityMessage("viewer-1")

        assertEquals(
            listOf("older", "newer"),
            mergeCommunityMessages(
                current = listOf(newer),
                incoming = listOf(older, newer)
            ).map(CommunityMessageItem::id)
        )
    }

    private fun remotePost(
        id: String,
        body: String,
        authorUserId: String,
        anonymous: Boolean = false,
        createdAt: String = "2026-07-30T10:00:00Z"
    ) = RemotePost(
        id = id,
        title = "Fallback title",
        body = body,
        createdAt = createdAt,
        isAnonymous = anonymous,
        reactions = 4,
        comments = 2,
        author = RemoteAuthor(
            userId = authorUserId,
            username = "utkarsh",
            displayName = "Utkarsh",
            isAnonymous = anonymous
        )
    )
}
