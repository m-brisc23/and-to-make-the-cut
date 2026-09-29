package com.tomakethecut.core.data

import com.tomakethecut.core.data.repository.DefaultCutOddsRepository
import com.tomakethecut.core.data.repository.DefaultPlayerStatsRepository
import com.tomakethecut.core.data.repository.DefaultTournamentRepository
import com.tomakethecut.core.domain.DataException
import com.tomakethecut.core.model.Tour
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

/**
 * Runs the real Retrofit + OkHttp + serialization stack against the mock backend.
 * These are the tests that will catch a DTO/fixture mismatch.
 */
class RepositoryIntegrationTest {

    private val network = TestNetwork()
    private val tournaments = DefaultTournamentRepository(network.oddsApi, Dispatchers.Unconfined, Dispatchers.Unconfined)
    private val odds = DefaultCutOddsRepository(network.oddsApi, Dispatchers.Unconfined, Dispatchers.Unconfined)
    private val stats = DefaultPlayerStatsRepository(network.statsApi, Dispatchers.Unconfined, Dispatchers.Unconfined)

    @Test
    fun `loads both seasons of tournaments`() = runTest {
        assertTrue(tournaments.getTournaments(2025).isNotEmpty())
        assertTrue(tournaments.getTournaments(2026).isNotEmpty())
    }

    @Test
    fun `finds a tournament by id across seasons`() = runTest {
        assertEquals("Oakmont Country Club", tournaments.getTournament("us-open-2025").course)
    }

    @Test
    fun `unknown tournament is not found`() = runTest {
        try {
            tournaments.getTournament("ryder-cup-2025")
            fail("expected NotFound")
        } catch (expected: DataException.NotFound) {
            // ok
        }
    }

    @Test
    fun `make cut market parses with prices from multiple books`() = runTest {
        val market = odds.getMakeCutMarket("the-open-championship-2025")
        assertTrue(market.snapshots.size >= 5)
        assertTrue(market.players.flatMap { it.prices }.map { it.sportsbook }.distinct().size >= 3)
        assertTrue(market.players.all { it.result != null })
    }

    @Test
    fun `korn ferry graduate has korn ferry stats in 2025 and pga tour stats in 2026`() = runTest {
        assertEquals(Tour.KORN_FERRY_TOUR, stats.getSeasonStats("johnny-keefer", 2025)!!.tour)
        assertEquals(Tour.PGA_TOUR, stats.getSeasonStats("johnny-keefer", 2026)!!.tour)
    }

    @Test
    fun `missing stats are null not an error`() = runTest {
        assertNull(stats.getSeasonStats("old-tom-morris", 2026))
    }

    @Test
    fun `io failures become network data exceptions`() = runTest {
        val offline = TestNetwork(extraInterceptor = Interceptor { throw IOException("airplane mode") })
        val repository = DefaultCutOddsRepository(offline.oddsApi, Dispatchers.Unconfined, Dispatchers.Unconfined)
        try {
            repository.getMakeCutMarket("us-open-2025")
            fail("expected Network")
        } catch (expected: DataException.Network) {
            // ok
        }
    }

    @Test
    fun `server errors become server data exceptions`() = runTest {
        val broken = TestNetwork(
            extraInterceptor = Interceptor { chain ->
                okhttp3.Response.Builder()
                    .request(chain.request())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(503)
                    .message("Unavailable")
                    .body("".toResponseBody(null))
                    .build()
            },
        )
        val repository = DefaultTournamentRepository(broken.oddsApi, Dispatchers.Unconfined, Dispatchers.Unconfined)
        try {
            repository.getTournaments(2026)
            fail("expected Server")
        } catch (expected: DataException.Server) {
            assertEquals(503, expected.code)
        }
    }
}
