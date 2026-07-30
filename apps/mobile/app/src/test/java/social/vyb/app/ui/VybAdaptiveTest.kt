package social.vyb.app.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VybAdaptiveTest {
    @Test
    fun compactPhoneUsesTightSafeSpacing() {
        val layout = resolveVybLayout(maxWidth = 359.dp, maxHeight = 699.dp)

        assertTrue(layout.compactWidth)
        assertTrue(layout.compactHeight)
        assertFalse(layout.wide)
        assertEquals(12.dp, layout.horizontalPadding)
        assertEquals(10.dp, layout.sectionSpacing)
    }

    @Test
    fun tabletUsesWideSpacing() {
        val layout = resolveVybLayout(maxWidth = 600.dp, maxHeight = 900.dp)

        assertFalse(layout.compactWidth)
        assertFalse(layout.compactHeight)
        assertTrue(layout.wide)
        assertEquals(28.dp, layout.horizontalPadding)
        assertEquals(16.dp, layout.sectionSpacing)
    }
}
