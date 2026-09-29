package com.tomakethecut.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomakethecut.core.domain.usecase.GetPlayerCutOddsUseCase
import com.tomakethecut.core.domain.usecase.GetPlayerStatsUseCase
import com.tomakethecut.core.domain.usecase.PlayerCutOddsDetail
import com.tomakethecut.core.model.PlayerSeasonStats
import com.tomakethecut.core.model.Sportsbook
import com.tomakethecut.core.ui.state.LoadState
import com.tomakethecut.core.ui.state.loadWithRetry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlayerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPlayerCutOdds: GetPlayerCutOddsUseCase,
    getPlayerStats: GetPlayerStatsUseCase,
) : ViewModel() {

    /*
     * Navigation stores the type-safe route's properties in SavedStateHandle under their
     * property names. Reading the keys directly (instead of `savedStateHandle.toRoute()`)
     * keeps this ViewModel testable with a plain map — `toRoute()` needs Android's Bundle
     * and therefore Robolectric.
     */
    private val tournamentId: String = checkNotNull(savedStateHandle[ARG_TOURNAMENT_ID]) { "Missing $ARG_TOURNAMENT_ID" }
    private val playerId: String = checkNotNull(savedStateHandle[ARG_PLAYER_ID]) { "Missing $ARG_PLAYER_ID" }

    private val oddsRetries = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val statsRetries = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val selectedSnapshot = MutableStateFlow<Int?>(null)
    private val selectedSeason = MutableStateFlow<Int?>(null)
    private val hiddenBooks = MutableStateFlow<Set<Sportsbook>>(emptySet())

    private val odds = loadWithRetry(oddsRetries) { getPlayerCutOdds(tournamentId, playerId) }
    private val stats = loadWithRetry(statsRetries) { getPlayerStats(playerId) }

    val uiState: StateFlow<PlayerDetailUiState> =
        combine(odds, stats, selectedSnapshot, selectedSeason, hiddenBooks) { odds, stats, snapshot, season, hidden ->
            PlayerDetailUiState(
                odds = odds,
                stats = stats,
                selectedSnapshotIndex = resolveSnapshot(odds, snapshot),
                selectedStatsSeason = resolveSeason(stats, season),
                hiddenBooks = hidden,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerDetailUiState())

    fun onSnapshotSelected(index: Int) {
        selectedSnapshot.value = index
    }

    fun onStatsSeasonSelected(season: Int) {
        selectedSeason.value = season
    }

    fun onToggleBook(book: Sportsbook) {
        hiddenBooks.update { if (book in it) it - book else it + book }
    }

    fun onRetryOdds() {
        oddsRetries.tryEmit(Unit)
    }

    fun onRetryStats() {
        statsRetries.tryEmit(Unit)
    }

    internal companion object {
        // Must match the property names of PlayerDetailRoute.
        const val ARG_TOURNAMENT_ID = "tournamentId"
        const val ARG_PLAYER_ID = "playerId"

        /** Default to the latest snapshot that actually has a price — "where things stand now". */
        fun resolveSnapshot(odds: LoadState<PlayerCutOddsDetail>, requested: Int?): Int? {
            val timeline = (odds as? LoadState.Success)?.data?.entry?.timeline ?: return null
            val count = timeline.snapshots.size
            if (count == 0) return null
            if (requested != null && requested in 0 until count) return requested
            return timeline.consensus.indexOfLast { it != null }.takeIf { it >= 0 } ?: (count - 1)
        }

        fun resolveSeason(stats: LoadState<List<PlayerSeasonStats>>, requested: Int?): Int? {
            val seasons = (stats as? LoadState.Success)?.data?.map { it.season } ?: return null
            return requested?.takeIf { it in seasons } ?: seasons.firstOrNull()
        }
    }
}
