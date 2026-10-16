package com.tomakethecut.core.data

import com.tomakethecut.core.data.repository.DefaultCutOddsRepository
import com.tomakethecut.core.data.repository.DefaultPlayerStatsRepository
import com.tomakethecut.core.data.repository.DefaultTournamentRepository
import com.tomakethecut.core.testing.CountingDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Main-safety contract: repositories must move network calls to the IO dispatcher and
 * mapping to the Default dispatcher themselves, so a ViewModel can call them from Main.
 */
class RepositoryDispatchersTest {

    private val network = TestNetwork()
    private val io = CountingDispatcher()
    private val default = CountingDispatcher()

    @Test
    fun `tournament repository uses io for network and default for mapping`() = runTest {
        DefaultTournamentRepository(network.oddsApi, io, default).getTournaments(2026)

        assertTrue("network call should run on IO", io.dispatches > 0)
        assertTrue("mapping should run on Default", default.dispatches > 0)
    }

    @Test
    fun `odds repository uses io for network and default for mapping`() = runTest {
        DefaultCutOddsRepository(network.oddsApi, io, default).getMakeCutMarket("us-open-2025")

        assertTrue(io.dispatches > 0)
        assertTrue(default.dispatches > 0)
    }

    @Test
    fun `stats repository uses io for network and default for mapping`() = runTest {
        DefaultPlayerStatsRepository(network.statsApi, io, default).getSeasonStats("rory-mcilroy", 2025)

        assertTrue(io.dispatches > 0)
        assertTrue(default.dispatches > 0)
    }
}
