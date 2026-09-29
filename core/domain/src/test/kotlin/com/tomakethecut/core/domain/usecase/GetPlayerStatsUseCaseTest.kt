package com.tomakethecut.core.domain.usecase

import com.tomakethecut.core.domain.DataException
import com.tomakethecut.core.model.Tour
import com.tomakethecut.core.testing.FakePlayerStatsRepository
import com.tomakethecut.core.testing.TestData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class GetPlayerStatsUseCaseTest {

    private val repository = FakePlayerStatsRepository()
    private val useCase = GetPlayerStatsUseCase(repository)

    @Test
    fun `returns every available season newest first across tours`() = runTest {
        val rookie = TestData.player(id = "johnny-keefer", name = "Johnny Keefer")
        repository.stats["johnny-keefer" to 2025] = TestData.seasonStats(rookie, 2025, Tour.KORN_FERRY_TOUR)
        repository.stats["johnny-keefer" to 2026] = TestData.seasonStats(rookie, 2026, Tour.PGA_TOUR)

        val stats = useCase("johnny-keefer", seasons = listOf(2025, 2026)).getOrThrow()

        assertEquals(listOf(2026, 2025), stats.map { it.season })
        assertEquals(listOf(Tour.PGA_TOUR, Tour.KORN_FERRY_TOUR), stats.map { it.tour })
    }

    @Test
    fun `missing seasons are skipped`() = runTest {
        repository.stats["p" to 2026] = TestData.seasonStats(season = 2026)

        val stats = useCase("p", seasons = listOf(2026, 2025)).getOrThrow()

        assertEquals(listOf(2026), stats.map { it.season })
    }

    @Test
    fun `an error fails the whole request`() = runTest {
        repository.error = DataException.Network(IOException("offline"))

        assertTrue(useCase("p").isFailure)
    }
}
