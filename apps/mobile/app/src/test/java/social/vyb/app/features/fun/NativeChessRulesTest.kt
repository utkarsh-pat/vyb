package social.vyb.app.features.funhub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeChessRulesTest {
    @Test
    fun openingHasTwentyLegalMoves() {
        assertEquals(20, NativeChessRulesTestApi.openingLegalMoveCount())
    }

    @Test
    fun legalOpeningSequenceProducesCompactHistory() {
        assertEquals(
            listOf("d4", "d5", "Nf3"),
            NativeChessRulesTestApi.play("d2d4", "d7d5", "g1f3"),
        )
    }

    @Test
    fun illegalMoveIsRejected() {
        assertNull(NativeChessRulesTestApi.play("d2d5"))
    }
}
