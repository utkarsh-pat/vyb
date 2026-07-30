package social.vyb.app.features.social

import org.junit.Assert.assertEquals
import org.junit.Test

class CommentThreadMutationTest {
    @Test
    fun `replace comment preserves thread order`() {
        val original = listOf(comment("one"), comment("two"))
        val updated = comment("one", body = "edited")

        val result = original.replaceComment(updated)

        assertEquals(listOf("one", "two"), result.map(SocialComment::id))
        assertEquals("edited", result.first().body)
    }

    @Test
    fun `delete removes the complete nested reply branch`() {
        val comments = listOf(
            comment("root"),
            comment("reply", parent = "root"),
            comment("nested", parent = "reply"),
            comment("other")
        )

        val result = comments.removeCommentBranch("root")

        assertEquals(listOf("other"), result.map(SocialComment::id))
    }

    @Test
    fun `comment reaction updates only the matching row and trusts server count`() {
        val comments = listOf(comment("first"), comment("second"))

        val result = comments.applyCommentReaction(
            CommentReactionResult(
                commentId = "second",
                aggregateCount = 4,
                active = true
            )
        )

        assertEquals(0, result.first().reactions)
        assertEquals(false, result.first().viewerHasLiked)
        assertEquals(4, result[1].reactions)
        assertEquals(true, result[1].viewerHasLiked)
    }

    private fun comment(
        id: String,
        parent: String? = null,
        body: String = id
    ) = SocialComment(
        id = id,
        postId = "post",
        parentCommentId = parent,
        body = body
    )
}
