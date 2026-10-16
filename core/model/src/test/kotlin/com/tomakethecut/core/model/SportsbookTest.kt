package com.tomakethecut.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SportsbookTest {

    @Test
    fun `api key lookup is case insensitive`() {
        assertEquals(Sportsbook.DRAFTKINGS, Sportsbook.fromApiKey("DraftKings"))
    }

    @Test
    fun `unknown books return null instead of crashing`() {
        assertNull(Sportsbook.fromApiKey("pointsbet"))
    }
}
