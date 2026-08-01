package social.vyb.app.features.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MarketValidationTest {
    @Test
    fun `sale requires a positive amount`() {
        assertThrows(IllegalArgumentException::class.java) {
            draft(tab = "sale", amount = null).validated()
        }
        assertThrows(IllegalArgumentException::class.java) {
            draft(tab = "sale", amount = 0).validated()
        }
    }

    @Test
    fun `request allows an omitted budget and trims fields`() {
        val result = draft(
            tab = "buying",
            amount = null,
            title = "  Calculator  ",
            category = "  Books  "
        ).validated()

        assertEquals("Calculator", result.title)
        assertEquals("Books", result.category)
        assertEquals(null, result.amount)
    }

    @Test
    fun `oversized fields fail before a network request`() {
        assertThrows(IllegalArgumentException::class.java) {
            draft(title = "x".repeat(121)).validated()
        }
        assertThrows(IllegalArgumentException::class.java) {
            draft(description = "x".repeat(2_001)).validated()
        }
    }

    private fun draft(
        tab: String = "sale",
        amount: Long? = 500,
        title: String = "Desk lamp",
        category: String = "Hostel",
        description: String = "Working condition"
    ) = MarketPostDraft(
        tab = tab,
        title = title,
        category = category,
        description = description,
        amount = amount,
        campusSpot = "Library gate",
        condition = "Good"
    )
}
