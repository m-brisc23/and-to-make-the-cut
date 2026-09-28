package com.tomakethecut.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AmericanOddsTest {

    @Test
    fun `favourite converts to implied probability`() {
        assertEquals(0.8, AmericanOdds(-400).impliedProbability, 1e-9)
    }

    @Test
    fun `underdog converts to implied probability`() {
        assertEquals(0.25, AmericanOdds(300).impliedProbability, 1e-9)
    }

    @Test
    fun `even money is fifty percent both ways`() {
        assertEquals(0.5, AmericanOdds(100).impliedProbability, 1e-9)
        assertEquals(0.5, AmericanOdds(-100).impliedProbability, 1e-9)
    }

    @Test
    fun `profit per 100 ranks prices from the bettor's perspective`() {
        assertEquals(25.0, AmericanOdds(-400).profitPer100, 1e-9)
        assertEquals(300.0, AmericanOdds(300).profitPer100, 1e-9)
    }

    @Test
    fun `positive prices print with a plus sign`() {
        assertEquals("+320", AmericanOdds(320).toString())
        assertEquals("-450", AmericanOdds(-450).toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `prices between minus 100 and plus 100 are rejected`() {
        AmericanOdds(50)
    }
}
