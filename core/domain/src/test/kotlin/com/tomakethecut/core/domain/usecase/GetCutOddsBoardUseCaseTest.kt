package com.tomakethecut.core.domain.usecase

import com.tomakethecut.core.domain.DataException
import com.tomakethecut.core.model.Sportsbook
import com.tomakethecut.core.testing.FakeCutOddsRepository
import com.tomakethecut.core.testing.TestData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetCutOddsBoardUseCaseTest {

    private val repository = FakeCutOddsRepository()
    private val useCase = GetCutOddsBoardUseCase(repository)

    @Test
    fun `best yes price is the highest paying book at the latest snapshot`() = runTest {
        repository.markets["t"] = TestData.market(
            tournamentId = "t",
            players = listOf(
                TestData.playerOdds(
                    prices = listOf(
                        TestData.price("s0", Sportsbook.FANDUEL, yes = -100, no = -120), // old snapshot: ignored
                        TestData.price("s2", Sportsbook.DRAFTKINGS, yes = -450),
                        TestData.price("s2", Sportsbook.BETMGM, yes = -400),
                        TestData.price("s2", Sportsbook.CAESARS, yes = -500),
                    ),
                ),
            ),
        )

        val best = useCase("t").getOrThrow().entries.single().bestYesPrice!!

        assertEquals(Sportsbook.BETMGM, best.sportsbook)
        assertEquals(-400, best.odds.value)
    }

    @Test
    fun `repository errors are returned as a failure not thrown`() = runTest {
        repository.error = DataException.Server(503)

        val result = useCase("t")

        assertTrue(result.exceptionOrNull() is DataException.Server)
    }

    @Test
    fun `force refresh is passed through`() = runTest {
        repository.markets["t"] = TestData.market(tournamentId = "t")

        useCase("t", forceRefresh = true)

        assertEquals(listOf("t"), repository.forceRefreshRequests)
    }
}
