package com.tomakethecut.core.domain.usecase

import com.tomakethecut.core.model.TournamentStatus
import com.tomakethecut.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FeaturedTournamentTest {

    private val completedOld = TestData.tournament(id = "old", start = LocalDate.of(2026, 3, 1))
    private val completedRecent = TestData.tournament(id = "recent", start = LocalDate.of(2026, 9, 24))
    private val upcomingSoon = TestData.tournament(id = "soon", status = TournamentStatus.UPCOMING, start = LocalDate.of(2026, 10, 1))
    private val upcomingLater = TestData.tournament(id = "later", status = TournamentStatus.UPCOMING, start = LocalDate.of(2026, 10, 8))
    private val live = TestData.tournament(id = "live", status = TournamentStatus.IN_PROGRESS)

    @Test
    fun `live tournament wins`() {
        assertEquals("live", listOf(completedRecent, upcomingSoon, live).featured()?.id)
    }

    @Test
    fun `otherwise the soonest upcoming event`() {
        assertEquals("soon", listOf(upcomingLater, completedRecent, upcomingSoon).featured()?.id)
    }

    @Test
    fun `otherwise the most recently completed event`() {
        assertEquals("recent", listOf(completedOld, completedRecent).featured()?.id)
    }

    @Test
    fun `empty list has nothing to feature`() {
        assertNull(emptyList<com.tomakethecut.core.model.Tournament>().featured())
    }
}
