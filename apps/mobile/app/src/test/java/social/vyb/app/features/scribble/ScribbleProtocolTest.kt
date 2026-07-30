package social.vyb.app.features.scribble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScribbleProtocolTest {
    @Test
    fun roomCodesAreNormalizedForProtocol() {
        assertEquals("AB12CD", normalizeScribbleRoomCode(" ab-12 cd! "))
        assertEquals(12, normalizeScribbleRoomCode("abcdefghijklmnop").length)
    }

    @Test
    fun incrementalDrawingIsRoomScopedAndBounded() {
        val base = snapshot(drawing = emptyList())
        val step = ScribbleDrawStep(0f, 0f, 1f, 1f, "#111827", 5f)

        assertTrue(base.withIncomingSteps("OTHER", listOf(step)).drawing.isEmpty())
        assertEquals(1, base.withIncomingSteps("ROOM1", listOf(step)).drawing.size)
        assertEquals(5_000, base.withIncomingSteps("ROOM1", List(5_010) { step }).drawing.size)
    }

    @Test
    fun authoritativePlayingSnapshotDoesNotEraseOptimisticStrokes() {
        val step = ScribbleDrawStep(0f, 0f, 1f, 1f, "#111827", 5f)
        val current = snapshot(drawing = listOf(step))
        val incoming = snapshot(drawing = emptyList())

        assertEquals(listOf(step), mergeScribbleSnapshot(current, incoming).drawing)
        assertTrue(mergeScribbleSnapshot(current, incoming.copy(status = "ROUND_END")).drawing.isEmpty())
    }

    @Test
    fun drawerAndWordVisibilityFollowViewerSnapshot() {
        val drawer = snapshot(currentWord = "campus")
        val guesser = drawer.copy(
            viewerMembershipId = "member-2",
            currentWord = null,
            hint = "C _ _ _ _ _",
        )

        assertTrue(drawer.viewerCanDraw)
        assertFalse(guesser.viewerCanDraw)
        assertEquals("campus", drawer.visibleWord())
        assertEquals("C _ _ _ _ _", guesser.visibleWord())
    }

    private fun snapshot(
        status: String = "PLAYING",
        viewerMembershipId: String = "member-1",
        currentDrawerMembershipId: String? = "member-1",
        currentWord: String? = null,
        hint: String? = null,
        drawing: List<ScribbleDrawStep> = emptyList(),
    ) = ScribbleSnapshot(
        roomId = "ROOM1",
        status = status,
        viewerMembershipId = viewerMembershipId,
        currentDrawerMembershipId = currentDrawerMembershipId,
        currentWord = currentWord,
        hint = hint,
        drawing = drawing,
    )
}
