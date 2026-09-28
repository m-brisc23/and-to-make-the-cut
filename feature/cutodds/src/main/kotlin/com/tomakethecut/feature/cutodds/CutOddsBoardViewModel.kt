package com.tomakethecut.feature.cutodds

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomakethecut.core.domain.usecase.BoardSort
import com.tomakethecut.core.domain.usecase.CutOddsBoard
import com.tomakethecut.core.domain.usecase.GetCutOddsBoardUseCase
import com.tomakethecut.core.domain.usecase.GetTournamentsUseCase
import com.tomakethecut.core.domain.usecase.featured
import com.tomakethecut.core.domain.usecase.filterAndSort
import com.tomakethecut.core.model.Seasons
import com.tomakethecut.core.model.Tournament
import com.tomakethecut.core.ui.state.ErrorKind
import com.tomakethecut.core.ui.state.LoadState
import com.tomakethecut.core.ui.state.loadWithRetry
import com.tomakethecut.core.ui.state.toLoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Unidirectional data flow: user intents come in through plain functions, state goes out
 * through a single [uiState] StateFlow. The ViewModel owns *no* mutable UI fields — the
 * state is derived reactively from inputs, so it can't drift out of sync.
 *
 * User selections live in [SavedStateHandle] so they survive process death, not just
 * rotation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CutOddsBoardViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getTournaments: GetTournamentsUseCase,
    private val getCutOddsBoard: GetCutOddsBoardUseCase,
) : ViewModel() {

    private val selectedSeason = savedStateHandle.getStateFlow(KEY_SEASON, Seasons.current)
    private val selectedTournamentId = savedStateHandle.getStateFlow<String?>(KEY_TOURNAMENT, null)
    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val sort = savedStateHandle.getStateFlow(KEY_SORT, BoardSort.MOST_LIKELY)

    private val isRefreshing = MutableStateFlow(false)
    private val transientError = MutableStateFlow<ErrorKind?>(null)
    private val tournamentRetries = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val boardRefreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val tournaments: StateFlow<LoadState<List<Tournament>>> = selectedSeason
        .flatMapLatest { season -> loadWithRetry(tournamentRetries) { getTournaments(season) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LoadState.Loading)

    private val selectedTournament: Flow<Tournament?> =
        combine(tournaments, selectedTournamentId) { state, id ->
            val list = (state as? LoadState.Success)?.data ?: return@combine null
            list.firstOrNull { it.id == id } ?: list.featured()
        }.distinctUntilChanged()

    /** Pairs the board with the tournament it belongs to, so they can never be mismatched mid-switch. */
    private val board: Flow<Pair<Tournament?, LoadState<CutOddsBoard>>> =
        selectedTournament.flatMapLatest { tournament ->
            if (tournament == null) {
                flowOf<Pair<Tournament?, LoadState<CutOddsBoard>>>(null to LoadState.Loading)
            } else {
                boardFor(tournament.id).map { tournament to it }
            }
        }

    private val controls = combine(query, sort, isRefreshing, transientError, ::Controls)

    val uiState: StateFlow<CutOddsBoardUiState> =
        combine(selectedSeason, tournaments, board, controls) { season, tournamentsState, (tournament, boardState), c ->
            CutOddsBoardUiState(
                selectedSeason = season,
                tournaments = tournamentsState,
                selectedTournamentId = tournament?.id,
                board = when (boardState) {
                    is LoadState.Success -> LoadState.Success(
                        BoardData(
                            tournament = requireNotNull(tournament),
                            snapshots = boardState.data.snapshots,
                            entries = boardState.data.entries.filterAndSort(c.query, c.sort),
                            totalPlayers = boardState.data.entries.size,
                        ),
                    )
                    is LoadState.Error -> boardState
                    LoadState.Loading -> LoadState.Loading
                },
                query = c.query,
                sort = c.sort,
                isRefreshing = c.isRefreshing,
                transientError = c.transientError,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CutOddsBoardUiState())

    fun onSeasonSelected(season: Int) {
        if (season == selectedSeason.value) return
        savedStateHandle[KEY_SEASON] = season
        savedStateHandle[KEY_TOURNAMENT] = null // fall back to that season's featured event
    }

    fun onTournamentSelected(tournamentId: String) {
        savedStateHandle[KEY_TOURNAMENT] = tournamentId
    }

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    fun onSortChange(value: BoardSort) {
        savedStateHandle[KEY_SORT] = value
    }

    fun onRefresh() {
        boardRefreshes.tryEmit(Unit)
    }

    fun onRetry() {
        if (tournaments.value is LoadState.Error) tournamentRetries.tryEmit(Unit) else boardRefreshes.tryEmit(Unit)
    }

    fun onTransientErrorShown() {
        transientError.value = null
    }

    private fun boardFor(tournamentId: String): Flow<LoadState<CutOddsBoard>> = flow {
        emit(LoadState.Loading)
        var current = getCutOddsBoard(tournamentId).toLoadState()
        emit(current)
        boardRefreshes.collect {
            if (current !is LoadState.Success) emit(LoadState.Loading)
            isRefreshing.value = true
            val next = try {
                getCutOddsBoard(tournamentId, forceRefresh = true).toLoadState()
            } finally {
                isRefreshing.value = false
            }
            if (next is LoadState.Error && current is LoadState.Success) {
                // Keep the last good odds on screen; tell the user the refresh failed.
                transientError.value = next.kind
            } else {
                current = next
                emit(next)
            }
        }
    }

    private data class Controls(
        val query: String,
        val sort: BoardSort,
        val isRefreshing: Boolean,
        val transientError: ErrorKind?,
    )

    internal companion object {
        const val KEY_SEASON = "season"
        const val KEY_TOURNAMENT = "tournamentId"
        const val KEY_QUERY = "query"
        const val KEY_SORT = "sort"

        /** Keep upstream flows alive across a configuration change, but not in the background. */
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
