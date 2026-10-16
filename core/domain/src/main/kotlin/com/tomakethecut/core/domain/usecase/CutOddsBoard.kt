package com.tomakethecut.core.domain.usecase

import com.tomakethecut.core.model.AmericanOdds
import com.tomakethecut.core.model.CutResult
import com.tomakethecut.core.model.OddsSnapshot
import com.tomakethecut.core.model.OddsTimeline
import com.tomakethecut.core.model.Player
import com.tomakethecut.core.model.Probability
import com.tomakethecut.core.model.Sportsbook
import kotlin.math.abs

data class BestPrice(val sportsbook: Sportsbook, val odds: AmericanOdds)

data class CutOddsBoardEntry(
    val player: Player,
    val timeline: OddsTimeline,
    val result: CutResult?,
    /** Highest-paying "Yes" price across books at the latest snapshot. */
    val bestYesPrice: BestPrice?,
) {
    val currentProbability: Probability? get() = timeline.latestConsensus
    val changeSinceOpen: Double? get() = timeline.changeSinceOpen
}

data class CutOddsBoard(
    val tournamentId: String,
    val snapshots: List<OddsSnapshot>,
    val entries: List<CutOddsBoardEntry>,
)

enum class BoardSort { MOST_LIKELY, LEAST_LIKELY, BIGGEST_MOVERS, NAME }

/**
 * Pure filter + sort, kept out of the ViewModel so it is trivially unit-testable and
 * reusable (e.g. by a widget or a Wear OS tile later).
 */
fun List<CutOddsBoardEntry>.filterAndSort(query: String, sort: BoardSort): List<CutOddsBoardEntry> {
    val trimmed = query.trim()
    val filtered = if (trimmed.isEmpty()) {
        this
    } else {
        filter { it.player.name.contains(trimmed, ignoreCase = true) || it.player.country.equals(trimmed, ignoreCase = true) }
    }
    // Entries without a price always sink to the bottom, whatever the sort.
    val byName = compareBy<CutOddsBoardEntry> { it.player.name }
    return when (sort) {
        BoardSort.MOST_LIKELY -> filtered.sortedWith(
            compareBy<CutOddsBoardEntry> { it.currentProbability == null }
                .thenByDescending { it.currentProbability?.value }
                .then(byName),
        )
        BoardSort.LEAST_LIKELY -> filtered.sortedWith(
            compareBy<CutOddsBoardEntry> { it.currentProbability == null }
                .thenBy { it.currentProbability?.value }
                .then(byName),
        )
        BoardSort.BIGGEST_MOVERS -> filtered.sortedWith(
            compareBy<CutOddsBoardEntry> { it.changeSinceOpen == null }
                .thenByDescending { it.changeSinceOpen?.let(::abs) }
                .then(byName),
        )
        BoardSort.NAME -> filtered.sortedWith(byName)
    }
}
