package com.tomakethecut.feature.cutodds

import com.tomakethecut.core.domain.usecase.GetCutOddsBoardUseCase
import com.tomakethecut.core.model.Sportsbook
import com.tomakethecut.core.model.Tour
import com.tomakethecut.core.testing.FakeCutOddsRepository
import com.tomakethecut.core.testing.TestData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardRowTest {

    private val repository = FakeCutOddsRepository()

    private suspend fun rowFor(market: com.tomakethecut.core.model.MakeCutMarket): BoardRow {
        repository.markets[market.tournamentId] = market
        return GetCutOddsBoardUseCase(repository, Dispatchers.Unconfined)(market.tournamentId)
            .getOrThrow().entries.single().toRow()
    }

    @Test
    fun `row carries precomputed display values`() = runTest {
        val row = rowFor(
            TestData.market(
                tournamentId = "t",
                players = listOf(
                    TestData.playerOdds(
                        player = TestData.player("keefer", "Johnny Keefer", tour = Tour.KORN_FERRY_TOUR),
                        prices = listOf(
                            TestData.price("s0", Sportsbook.DRAFTKINGS, yes = -110, no = -110), // 50%
                            TestData.price("s2", Sportsbook.DRAFTKINGS, yes = -300, no = 300), // 75%
                        ),
                    ),
                ),
            ),
        )

        assertEquals("keefer", row.playerId)
        assertTrue(row.isKornFerry)
        assertEquals("75%", row.percentText)
        assertEquals(25.0, row.changePoints!!, 1e-6)
        assertEquals("DraftKings", row.bestPriceBook)
        assertEquals("-300", row.bestPriceOdds)
        assertEquals(listOf(0.5f, null, 0.75f), row.sparkline)
    }

    @Test
    fun `unpriced player has no percent or price`() = runTest {
        val row = rowFor(TestData.market(tournamentId = "u", players = listOf(TestData.playerOdds(prices = emptyList()))))

        assertNull(row.percentText)
        assertNull(row.bestPriceOdds)
        assertEquals(listOf(null, null, null), row.sparkline)
    }
}
