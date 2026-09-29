package com.tomakethecut.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ProbabilityTest {

    @Test
    fun `formats as whole percent`() {
        assertEquals("83%", Probability(0.8261).formatPercent())
    }

    @Test
    fun `clamped keeps values in range`() {
        assertEquals(1.0, Probability.clamped(1.2).value, 0.0)
        assertEquals(0.0, Probability.clamped(-0.1).value, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects out of range values`() {
        Probability(1.01)
    }
}
