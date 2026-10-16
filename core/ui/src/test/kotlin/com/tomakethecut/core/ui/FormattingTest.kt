package com.tomakethecut.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FormattingTest {

    @Test
    fun `change points are signed and rounded`() {
        assertEquals("+12 pts", Formatting.changePoints(11.6))
        assertEquals("−3 pts", Formatting.changePoints(-3.2))
        assertEquals("±0 pts", Formatting.changePoints(0.4))
    }

    @Test
    fun `signed decimals use a real minus sign`() {
        assertEquals("+0.42", Formatting.signed(0.42))
        assertEquals("−1.10", Formatting.signed(-1.1))
    }

    @Test
    fun `date range collapses the month when it does not change`() {
        assertEquals("Apr 9–12, 2026", Formatting.dateRange(LocalDate.of(2026, 4, 9), LocalDate.of(2026, 4, 12)))
        assertEquals("May 29–Jun 1, 2025", Formatting.dateRange(LocalDate.of(2025, 5, 29), LocalDate.of(2025, 6, 1)))
    }
}
