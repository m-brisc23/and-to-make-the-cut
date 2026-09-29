package com.tomakethecut.feature.cutodds

import androidx.lifecycle.SavedStateHandle
import com.tomakethecut.core.domain.DataException
import com.tomakethecut.core.domain.usecase.BoardSort
import com.tomakethecut.core.domain.usecase.GetCutOddsBoardUseCase
import com.tomakethecut.core.domain.usecase.GetTournamentsUseCase
import com.tomakethecut.core.model.TournamentStatus
import com.tomakethecut.core.testing.FakeCutOddsRepository
import com.tomakethecut.core.testing.FakeTournamentRepository
import com.tomakethecut.core.testing.MainDispatcherRule
import com.tomakethecut.core.testing.TestData
import com.tomakethecut.core.ui.state.ErrorKind
import com.tomakethecut.core.ui.state.LoadState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class CutOddsBoardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tournamentRepository = FakeTournamentRepository()
    private val oddsRepository = FakeCutOddsRepository()

    private val masters = TestData.tournament(id = "masters-2026", start = LocalDate.of(2026, 4, 9))
    private val procore = TestData.tournament(id = "procore-2026", name = "Procore Championship", start = LocalDate.of(2026, 9, 24))
    private val sanderson = TestData.tournament(
        id = "sanderson-2026",
        name = "Sanderson Farms Championship",
        status = TournamentStatus.UPCOMING,
        start = LocalDate.of(2026, 10, 1),
    )
    private val open2025 = TestData.tournament(id = "open-2025", season = 2025, start = LocalDate.of(2025, 7, 17))

    private val scheffler = TestData.player("scheffler", "Scottie Scheffler")
    private val keefer = TestData.player("keefer", "Johnny Keefer")

    @Before
    fun setUp() {
        tournamentRepository.tournamentsBySeason[2026] = listOf(masters, procore, sanderson)
        tournamentRepository.tournamentsBySeason[2025] = listOf(open2025)
        listOf(masters, procore, sanderson, open2025).forEach { t ->
            oddsRepository.markets[t.id] = TestData.market(
                tournamentId = t.id,
                players = listOf(
                    // Keefer 50%, Scheffler 80%
                    TestData.playerOdds(keefer, listOf(TestData.price("s2", yes = -110, no = -110))),
                    TestData.playerOdds(scheffler, listOf(TestData.price("s2", yes = -400, no = 400))),
                ),
            )
        }
    }

    private fun createViewModel(savedState: Map<String, Any?> = emptyMap()) = CutOddsBoardViewModel(
        savedStateHandle = SavedStateHandle(savedState),
        getTournaments = GetTournamentsUseCase(tournamentRepository),
        getCutOddsBoard = GetCutOddsBoardUseCase(oddsRepository, mainDispatcherRule.testDispatcher),
        defaultDispatcher = mainDispatcherRule.testDispatcher,
    )

    /** `stateIn(WhileSubscribed)` only runs while collected, exactly like on screen. */
    private fun TestScope.collect(viewModel: CutOddsBoardViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    private val CutOddsBoardUiState.boardData get() = (board as LoadState.Success).data

    @Test
    fun `initial state is loading`() = runTest {
        assertEquals(LoadState.Loading, createViewModel().uiState.value.board)
    }

    @Test
    fun `defaults to the featured tournament with players most likely first`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)

        val state = viewModel.uiState.value
        assertEquals("sanderson-2026", state.selectedTournamentId)
        assertEquals(listOf("scheffler", "keefer"), state.boardData.rows.map { it.playerId })
    }

    @Test
    fun `tournaments are listed newest first`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)

        val tournaments = (viewModel.uiState.value.tournaments as LoadState.Success).data
        assertEquals(listOf("sanderson-2026", "procore-2026", "masters-2026"), tournaments.map { it.id })
    }

    @Test
    fun `selecting a tournament loads its board`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)

        viewModel.onTournamentSelected("masters-2026")

        assertEquals("masters-2026", viewModel.uiState.value.boardData.tournament.id)
    }

    @Test
    fun `board shows loading while the market request is in flight`() = runTest {
        val gate = CompletableDeferred<Unit>()
        oddsRepository.gate = gate
        val viewModel = createViewModel()
        collect(viewModel)

        assertEquals(LoadState.Loading, viewModel.uiState.value.board)

        gate.complete(Unit)
        assertTrue(viewModel.uiState.value.board is LoadState.Success)
    }

    @Test
    fun `switching season resets to that season's featured tournament`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)
        viewModel.onTournamentSelected("masters-2026")

        viewModel.onSeasonSelected(2025)

        val state = viewModel.uiState.value
        assertEquals(2025, state.selectedSeason)
        assertEquals("open-2025", state.selectedTournamentId)
    }

    @Test
    fun `search filters players without changing the total`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)

        viewModel.onQueryChange("keef")

        val data = viewModel.uiState.value.boardData
        assertEquals(listOf("keefer"), data.rows.map { it.playerId })
        assertEquals(2, data.totalPlayers)
    }

    @Test
    fun `sort can be changed`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)

        viewModel.onSortChange(BoardSort.LEAST_LIKELY)

        assertEquals(listOf("keefer", "scheffler"), viewModel.uiState.value.boardData.rows.map { it.playerId })
    }

    @Test
    fun `selections are restored from saved state after process death`() = runTest {
        val viewModel = createViewModel(
            mapOf(
                CutOddsBoardViewModel.KEY_TOURNAMENT to "procore-2026",
                CutOddsBoardViewModel.KEY_QUERY to "scottie",
            ),
        )
        collect(viewModel)

        val state = viewModel.uiState.value
        assertEquals("procore-2026", state.selectedTournamentId)
        assertEquals(listOf("scheffler"), state.boardData.rows.map { it.playerId })
    }

    @Test
    fun `tournament load failure shows an error and retry recovers`() = runTest {
        tournamentRepository.error = DataException.Network(IOException("offline"))
        val viewModel = createViewModel()
        collect(viewModel)
        assertEquals(LoadState.Error(ErrorKind.NETWORK), viewModel.uiState.value.tournaments)

        tournamentRepository.error = null
        viewModel.onRetry()

        assertTrue(viewModel.uiState.value.board is LoadState.Success)
    }

    @Test
    fun `refresh forces a network reload`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)

        viewModel.onRefresh()

        assertEquals(listOf("sanderson-2026"), oddsRepository.forceRefreshRequests)
        assertEquals(false, viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `failed refresh keeps the old odds and reports a transient error`() = runTest {
        val viewModel = createViewModel()
        collect(viewModel)

        oddsRepository.error = DataException.Server(503)
        viewModel.onRefresh()

        val state = viewModel.uiState.value
        assertTrue(state.board is LoadState.Success)
        assertEquals(ErrorKind.SERVER, state.transientError)

        viewModel.onTransientErrorShown()
        assertNull(viewModel.uiState.value.transientError)
    }

    @Test
    fun `failed initial board load can be retried`() = runTest {
        oddsRepository.error = DataException.Server(500)
        val viewModel = createViewModel()
        collect(viewModel)
        assertEquals(LoadState.Error(ErrorKind.SERVER), viewModel.uiState.value.board)

        oddsRepository.error = null
        viewModel.onRetry()

        assertTrue(viewModel.uiState.value.board is LoadState.Success)
    }
}
