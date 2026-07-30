package social.vyb.app.features.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialPostParityTest {
    @Test
    fun `reaction metadata matches the six web reaction contracts`() {
        assertEquals(
            listOf("like", "fire", "support", "love", "insight", "funny"),
            ReactionChoices.map(ReactionChoice::type)
        )
        assertEquals(ReactionChoices.size, ReactionChoices.map(ReactionChoice::symbol).toSet().size)
        assertTrue(ReactionChoices.all { it.label.isNotBlank() && it.symbol.isNotBlank() })
    }

    @Test
    fun `share payload prefers body and includes canonical encoded permalink`() {
        assertEquals(
            "Campus update\nhttps://vybnet.app/post/post%20with%2Fslash",
            postShareText(
                postId = "post with/slash",
                title = "Fallback title",
                body = "Campus update"
            )
        )
    }

    @Test
    fun `share payload falls back to title when body is blank`() {
        assertEquals(
            "Fallback title\nhttps://vybnet.app/post/123",
            postShareText(postId = "123", title = "Fallback title", body = "")
        )
    }

    @Test
    fun `repost placement values match backend and web options`() {
        assertEquals(listOf("feed", "vibe"), RepostPlacements.map(RepostPlacement::value))
        assertEquals(listOf("Feed", "Vibes"), RepostPlacements.map(RepostPlacement::label))
    }
}
