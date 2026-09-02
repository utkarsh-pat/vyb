package social.vyb.app.features.search

import org.junit.Assert.assertEquals
import org.junit.Test

class FollowStatsMergeTest {
    @Test
    fun `follow response preserves the independently loaded post count`() {
        val profileStats = ProfileStats(posts = 8, followers = 2, following = 3)
        val followStats = ProfileStats(followers = 4, following = 5)

        assertEquals(
            ProfileStats(posts = 8, followers = 4, following = 5),
            profileStats.withFollowCounts(followStats)
        )
    }
}
