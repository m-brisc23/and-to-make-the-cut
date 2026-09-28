package com.tomakethecut.core.domain.usecase

import com.tomakethecut.core.domain.DataException
import com.tomakethecut.core.testing.FakeCutOddsRepository
import com.tomakethecut.core.testing.FakeTournamentRepository
import com.tomakethecut.core.testing.TestData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetPlayerCutOddsUseCaseTest {

    private val odds = FakeCutOddsRepository()
    private val tournaments = FakeTournamentRepository()
    private val useCase = GetPlayerCutOddsUseCase(odds, tournaments)

    private val tournament = TestData.tournament()

    @Test
    fun `returns the player's entry together with the tournament`() = runTest {
        tournaments.tournamentsBySeason[2026] = listOf(tournament)
        odds.markets[tournament.id] = TestData.market(tournamentId = tournament.id)

        val detail = useCase(tournament.id, "scottie-scheffler").getOrThrow()

        assertEquals("Masters Tournament", detail.tournament.name)
        assertEquals("Scottie Scheffler", detail.entry.player.name)
    }

    @Test
    fun `unknown player is a not found failure`() = runTest {
        tournaments.tournamentsBySeason[2026] = listOf(tournament)
        odds.markets[tournament.id] = TestData.market(tournamentId = tournament.id)

        val result = useCase(tournament.id, "tiger-woods")

        assertTrue(result.exceptionOrNull() is DataException.NotFound)
    }
}
