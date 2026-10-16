package com.tomakethecut.core.data

import com.tomakethecut.core.data.repository.DefaultCutOddsRepository
import com.tomakethecut.core.data.repository.DefaultPlayerStatsRepository
import com.tomakethecut.core.data.repository.DefaultTournamentRepository
import com.tomakethecut.core.model.Seasons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the mock data itself: every tournament listed has a market, and every player
 * priced in a market has stats for that season. If someone edits the generator and
 * breaks referential integrity, this fails long before a user taps a dead link.
 */
class FixtureContractTest {

    private val network = TestNetwork()
    private val tournaments = DefaultTournamentRepository(network.oddsApi, Dispatchers.Unconfined, Dispatchers.Unconfined)
    private val odds = DefaultCutOddsRepository(network.oddsApi, Dispatchers.Unconfined, Dispatchers.Unconfined)
    private val stats = DefaultPlayerStatsRepository(network.statsApi, Dispatchers.Unconfined, Dispatchers.Unconfined)

    @Test
    fun `every tournament has a market and every priced player has stats`() = runTest {
        for (season in Seasons.supported) {
            for (tournament in tournaments.getTournaments(season)) {
                val market = odds.getMakeCutMarket(tournament.id)
                assertTrue("${tournament.id} has no snapshots", market.snapshots.isNotEmpty())
                for (player in market.players) {
                    assertNotNull(
                        "${player.player.id} has no $season stats",
                        stats.getSeasonStats(player.player.id, season),
                    )
                }
            }
        }
    }
}
