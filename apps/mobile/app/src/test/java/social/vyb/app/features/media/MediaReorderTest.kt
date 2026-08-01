package social.vyb.app.features.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MediaReorderTest {
    @Test
    fun movesMediaForwardWithoutDroppingItems() {
        assertEquals(listOf("b", "c", "a", "d"), reorderMediaItems(listOf("a", "b", "c", "d"), 0, 2))
    }

    @Test
    fun movesMediaBackwardWithoutDroppingItems() {
        assertEquals(listOf("a", "d", "b", "c"), reorderMediaItems(listOf("a", "b", "c", "d"), 3, 1))
    }

    @Test
    fun invalidMoveKeepsOriginalInstance() {
        val items = listOf("a", "b")
        assertSame(items, reorderMediaItems(items, -1, 1))
        assertSame(items, reorderMediaItems(items, 0, 4))
        assertSame(items, reorderMediaItems(items, 1, 1))
    }
}
