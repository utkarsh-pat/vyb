package social.vyb.app.features.social

import org.junit.Assert.assertEquals
import org.junit.Test

class PostEngagementStateTest {
    @Test
    fun idleStateAcceptsAuthoritativeRefresh() {
        val local = PostEngagementState(
            reactionCount = 2,
            viewerReactionType = "like",
            savedCount = 1,
            isSaved = true
        )
        val authoritative = PostEngagementState(
            reactionCount = 7,
            viewerReactionType = null,
            savedCount = 3,
            isSaved = false
        )

        assertEquals(authoritative, local.mergeAuthoritative(authoritative))
    }

    @Test
    fun reactionMutationKeepsLocalReactionAndRefreshesSaveState() {
        val local = PostEngagementState(
            reactionCount = 8,
            viewerReactionType = "celebrate",
            savedCount = 1,
            isSaved = true,
            reactionLoading = true
        )
        val authoritative = PostEngagementState(
            reactionCount = 7,
            viewerReactionType = null,
            savedCount = 4,
            isSaved = false
        )

        assertEquals(
            PostEngagementState(
                reactionCount = 8,
                viewerReactionType = "celebrate",
                savedCount = 4,
                isSaved = false,
                reactionLoading = true
            ),
            local.mergeAuthoritative(authoritative)
        )
    }

    @Test
    fun saveMutationKeepsLocalSaveAndRefreshesReactionState() {
        val local = PostEngagementState(
            reactionCount = 2,
            viewerReactionType = null,
            savedCount = 5,
            isSaved = true,
            saveLoading = true
        )
        val authoritative = PostEngagementState(
            reactionCount = 9,
            viewerReactionType = "like",
            savedCount = 4,
            isSaved = false
        )

        assertEquals(
            PostEngagementState(
                reactionCount = 9,
                viewerReactionType = "like",
                savedCount = 5,
                isSaved = true,
                saveLoading = true
            ),
            local.mergeAuthoritative(authoritative)
        )
    }

    @Test
    fun staleFeedSnapshotCannotUndoSuccessfulSave() {
        val backend = PostEngagementState(savedCount = 0, isSaved = false)
        val result = reconcilePendingSave(
            backend = backend,
            pending = SaveResult(postId = "post-1", savedCount = 1, isSaved = true),
        )

        assertEquals(PostEngagementState(savedCount = 1, isSaved = true), result.engagement)
        assertEquals(false, result.confirmedByBackend)
    }

    @Test
    fun refreshedFeedConfirmsAndReleasesSaveOverride() {
        val backend = PostEngagementState(savedCount = 2, isSaved = true)
        val result = reconcilePendingSave(
            backend = backend,
            pending = SaveResult(postId = "post-1", savedCount = 1, isSaved = true),
        )

        assertEquals(backend, result.engagement)
        assertEquals(true, result.confirmedByBackend)
    }
}
