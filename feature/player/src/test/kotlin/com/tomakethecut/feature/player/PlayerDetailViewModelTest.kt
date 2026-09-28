package com.tomakethecut.feature.player

import androidx.lifecycle.SavedStateHandle
import com.tomakethecut.core.domain.DataException
import com.tomakethecut.core.domain.usecase.GetPlayerCutOddsUseCase
import com.tomakethecut.core.domain.usecase.GetPlayerStatsUseCase
import com.tomakethecut.core.model.Sportsbook
import com.tomakethecut.core.model.Tour
import com.tomakethecut.core.testing.FakeCutOddsRepository
import com.tomakethecut.core.testing.FakePlayerStatsRepository
import com.tomakethecut.core.testing.FakeTournamentRepository
import com.tomakethecut.core.testing.MainDispatcherRule
import com.tomakethecut.core.testing.TestData
import com.tomakethecut.core.ui.state.ErrorKind
import com.tomakethecut.core.ui.state.LoadState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class PlayerDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tournaments = FakeTournamentRepository()
    private val odds = FakeCutOddsRepository()
    private val stats = FakePlayerStatsRepository()

    private val tournament = TestData.tournament(id = "masters-2026")
    private val keefer = TestData.player("keefer", "Johnny Keefer")

    @Before
    fun setUp() {
        tournaments.tournamentsBySeason[2026] = listOf(tournament)
        odds.markets[tournament.id] = TestData.market(
            tournamentId = tournament.id,
            snapshots = TestData.snapshots(4),
            players = listOf(
                TestData.playerOdds(
                    keefer,
                    listOf(
                        TestData.price("s0", Sportsbook.DRAFTKINGS, yes = -110, no = -110),
                        TestData.price("s1", Sportsbook.DRAFTKINGS, yes = -150, no = 130),
                        TestData.price("s1", Sportsbook.FANDUEL, yes = -140, no = 120),
                        TestData.price("s2", Sportsbook.FANDUEL, yes = -200, no = 170),
                        // s3: markets pulled — no prices
                    ),
                ),
            ),
        )
        stats.stats["keefer" to 2025] = TestData.seasonStats(keefer, 2025, Tour.KORN_FERRY_TOUR)
        stats.stats["keefer" to 2026] = TestData.seasonStats(keefer, 2026, Tour.PGA_TOUR)
    }

    private fun createViewModel() = PlayerDetailViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                PlayerDetailViewModel.ARG_TOURNAMENT_ID to tournament.id,
                PlayerDetailViewModel.ARG_PLAYER_ID to "keefer",
            ),
        ),
        getPlayerCutOdds = GetPlayerCutOddsUseCase(odds, tournaments),
        getPlayerStats = GetPlayerStatsUseCase(stats),
    )

    private fun TestScope.collect(viewModel: PlayerDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    @Test
    fun `loads odds and stats for the player`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)

        val state = viewModel.uiState.value
        assertEquals("Johnny Keefer", state.playerName)
        assertTrue(state.odds is LoadState.Success)
        assertEquals(listOf(2026, 2025), (state.stats as LoadState.Success).data.map { it.season })
    }

    @Test
    fun `selected snapshot defaults to the latest priced one, not the last`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)

        assertEquals(2, viewModel.uiState.value.selectedSnapshotIndex)
    }

    @Test
    fun `scrubbing the chart updates the selection`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)

        viewModel.onSnapshotSelected(0)

        assertEquals(0, viewModel.uiState.value.selectedSnapshotIndex)
    }

    @Test
    fun `stats default to the newest season and can switch to korn ferry`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)
        assertEquals(Tour.PGA_TOUR, viewModel.uiState.value.selectedSeasonStats?.tour)

        viewModel.onStatsSeasonSelected(2025)

        assertEquals(Tour.KORN_FERRY_TOUR, viewModel.uiState.value.selectedSeasonStats?.tour)
    }

    @Test
    fun `books can be toggled off and on`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)

        viewModel.onToggleBook(Sportsbook.FANDUEL)
        assertEquals(setOf(Sportsbook.FANDUEL), viewModel.uiState.value.hiddenBooks)

        viewModel.onToggleBook(Sportsbook.FANDUEL)
        assertEquals(emptySet<Sportsbook>(), viewModel.uiState.value.hiddenBooks)
    }

    @Test
    fun `a stats failure does not hide the odds`() = runTest {
        stats.error = DataException.Network(IOException())
        val viewModel = createViewModel()
        collect(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.odds is LoadState.Success)
        assertEquals(LoadState.Error(ErrorKind.NETWORK), state.stats)
    }

    @Test
    fun `stats retry recovers`() = runTest {
        stats.error = DataException.Server(500)
        val viewModel = createViewModel()
        collect(viewModel)

        stats.error = null
        viewModel.onRetryStats()

        assertTrue(viewModel.uiState.value.stats is LoadState.Success)
    }

    @Test
    fun `unknown player shows not found for odds`() = runTest {
        odds.markets[tournament.id] = TestData.market(tournamentId = tournament.id, players = emptyList())
        val viewModel = createViewModel()
        collect(viewModel)

        assertEquals(LoadState.Error(ErrorKind.NOT_FOUND), viewModel.uiState.value.odds)
    }
}
