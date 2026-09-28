package com.tomakethecut.core.data

import com.tomakethecut.core.data.mapper.parseFinish
import com.tomakethecut.core.data.mapper.toModel
import com.tomakethecut.core.data.mapper.toModelOrNull
import com.tomakethecut.core.data.network.dto.LineDto
import com.tomakethecut.core.data.network.dto.MakeCutMarketDto
import com.tomakethecut.core.data.network.dto.PlayerMarketDto
import com.tomakethecut.core.data.network.dto.SnapshotDto
import com.tomakethecut.core.data.network.dto.TournamentDto
import com.tomakethecut.core.model.CutResult
import com.tomakethecut.core.model.EventType
import com.tomakethecut.core.model.Finish
import com.tomakethecut.core.model.Sportsbook
import com.tomakethecut.core.model.Tour
import com.tomakethecut.core.model.TournamentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MappersTest {

    @Test
    fun `finish strings parse into typed finishes`() {
        assertEquals(Finish.Position(1, tied = false), parseFinish("1"))
        assertEquals(Finish.Position(12, tied = true), parseFinish("T12"))
        assertEquals(Finish.MissedCut, parseFinish("MC"))
        assertEquals(Finish.MissedCut, parseFinish("cut"))
        assertEquals(Finish.Other("WD"), parseFinish("WD"))
    }

    @Test
    fun `lines from unknown books are dropped`() {
        assertNull(LineDto("pointsbet", "s0", -200, 150).toModelOrNull())
    }

    @Test
    fun `lines with impossible prices are dropped`() {
        assertNull(LineDto("draftkings", "s0", 50, null).toModelOrNull())
    }

    @Test
    fun `an impossible no price keeps the yes side`() {
        val line = LineDto("fanduel", "s0", -300, 20).toModelOrNull()!!
        assertEquals(Sportsbook.FANDUEL, line.sportsbook)
        assertNull(line.no)
    }

    @Test
    fun `unknown enum values fall back instead of crashing`() {
        val t = TournamentDto("id", "n", "c", "l", 2026, "2026-10-01", "2026-10-04", "POSTPONED", "OPPOSITE_FIELD").toModel()
        assertEquals(TournamentStatus.UPCOMING, t.status)
        assertEquals(EventType.FULL_FIELD, t.eventType)
    }

    @Test
    fun `market drops lines pointing at unknown snapshots and sorts snapshots by time`() {
        val dto = MakeCutMarketDto(
            tournamentId = "t",
            market = "make_cut",
            snapshots = listOf(
                SnapshotDto("s1", "later", 1, 6, "2026-04-09T15:00:00Z"),
                SnapshotDto("s0", "earlier", 0, 0, "2026-04-08T12:00:00Z"),
            ),
            players = listOf(
                PlayerMarketDto(
                    playerId = "p", playerName = "P", country = "USA", tour = "KORN_FERRY_TOUR",
                    lines = listOf(LineDto("draftkings", "s0", -150, 120), LineDto("draftkings", "s9", -150, 120)),
                    result = "MADE_CUT",
                ),
            ),
        )

        val market = dto.toModel()

        assertEquals(listOf("s0", "s1"), market.snapshots.map { it.id })
        assertEquals(1, market.players.single().prices.size)
        assertEquals(Tour.KORN_FERRY_TOUR, market.players.single().player.tour)
        assertEquals(CutResult.MADE_CUT, market.players.single().result)
    }
}
