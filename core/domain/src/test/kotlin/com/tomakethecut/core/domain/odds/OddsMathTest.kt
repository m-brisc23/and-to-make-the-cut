package com.tomakethecut.core.domain.odds

import com.tomakethecut.core.model.AmericanOdds
import com.tomakethecut.core.model.Probability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OddsMathTest {

    @Test
    fun `removing the vig normalises both sides to 100 percent`() {
        // -120 => 54.55%, +100 => 50.00%, total 104.55%
        val fair = OddsMath.noVigProbability(AmericanOdds(-120), AmericanOdds(100))
        assertEquals(0.5217, fair.value, 1e-4)
    }

    @Test
    fun `symmetric market is a coin flip`() {
        val fair = OddsMath.noVigProbability(AmericanOdds(-110), AmericanOdds(-110))
        assertEquals(0.5, fair.value, 1e-9)
    }

    @Test
    fun `yes-only market falls back to implied probability`() {
        val fair = OddsMath.noVigProbability(AmericanOdds(-400), no = null)
        assertEquals(0.8, fair.value, 1e-9)
    }

    @Test
    fun `overround of a standard -110 market is about 4 and a half percent`() {
        assertEquals(4.76, OddsMath.overroundPercent(AmericanOdds(-110), AmericanOdds(-110)), 0.01)
    }

    @Test
    fun `consensus averages available books`() {
        val result = OddsMath.consensus(listOf(Probability(0.6), Probability(0.8)))
        assertEquals(0.7, result!!.value, 1e-9)
    }

    @Test
    fun `consensus of nothing is null not zero`() {
        assertNull(OddsMath.consensus(emptyList()))
    }
}
