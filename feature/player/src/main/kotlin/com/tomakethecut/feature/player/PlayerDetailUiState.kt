package com.tomakethecut.feature.player

import com.tomakethecut.core.domain.usecase.PlayerCutOddsDetail
import com.tomakethecut.core.model.PlayerSeasonStats
import com.tomakethecut.core.model.Sportsbook
import com.tomakethecut.core.ui.state.LoadState

/**
 * Odds and stats are *independent* sections with their own load state: if the stats
 * service is down, the user still sees the odds chart (and vice versa). One global
 * Loading/Error for the whole screen would throw away perfectly good data.
 */
data class PlayerDetailUiState(
    val odds: LoadState<PlayerCutOddsDetail> = LoadState.Loading,
    val stats: LoadState<List<PlayerSeasonStats>> = LoadState.Loading,
    /** Resolved selection — always a valid index once odds have loaded. */
    val selectedSnapshotIndex: Int? = null,
    /** Resolved selection — always one of the loaded seasons once stats have loaded. */
    val selectedStatsSeason: Int? = null,
    val hiddenBooks: Set<Sportsbook> = emptySet(),
) {
    val playerName: String?
        get() = (odds as? LoadState.Success)?.data?.entry?.player?.name
            ?: (stats as? LoadState.Success)?.data?.firstOrNull()?.player?.name

    val selectedSeasonStats: PlayerSeasonStats?
        get() = (stats as? LoadState.Success)?.data?.firstOrNull { it.season == selectedStatsSeason }
}
