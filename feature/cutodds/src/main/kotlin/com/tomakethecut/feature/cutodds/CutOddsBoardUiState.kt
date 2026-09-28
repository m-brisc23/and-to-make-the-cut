package com.tomakethecut.feature.cutodds

import com.tomakethecut.core.domain.usecase.BoardSort
import com.tomakethecut.core.domain.usecase.CutOddsBoardEntry
import com.tomakethecut.core.model.OddsSnapshot
import com.tomakethecut.core.model.Seasons
import com.tomakethecut.core.model.Tournament
import com.tomakethecut.core.ui.state.ErrorKind
import com.tomakethecut.core.ui.state.LoadState

/**
 * One immutable object describing everything the board screen shows.
 *
 * A single state object (vs. several independent StateFlows) means the UI can never
 * render an impossible combination, and a test can assert the whole screen in one line.
 */
data class CutOddsBoardUiState(
    val seasons: List<Int> = Seasons.supported,
    val selectedSeason: Int = Seasons.current,
    val tournaments: LoadState<List<Tournament>> = LoadState.Loading,
    val selectedTournamentId: String? = null,
    val board: LoadState<BoardData> = LoadState.Loading,
    val query: String = "",
    val sort: BoardSort = BoardSort.MOST_LIKELY,
    val isRefreshing: Boolean = false,
    /**
     * A failure that shouldn't replace content already on screen (e.g. a refresh failed
     * but the previous odds are still valid). Modelled as *state* that the UI clears once
     * shown — Google's guidance for one-off UI events — rather than a Channel that could
     * drop the event during a configuration change.
     */
    val transientError: ErrorKind? = null,
)

data class BoardData(
    val tournament: Tournament,
    val snapshots: List<OddsSnapshot>,
    /** Already filtered by the query and sorted. */
    val entries: List<CutOddsBoardEntry>,
    val totalPlayers: Int,
)
