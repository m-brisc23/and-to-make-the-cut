package com.tomakethecut.feature.cutodds

import androidx.compose.runtime.Immutable
import com.tomakethecut.core.domain.usecase.CutOddsBoardEntry
import com.tomakethecut.core.model.CutResult
import com.tomakethecut.core.model.Tour

/**
 * Everything one list row needs, precomputed off the main thread.
 *
 * Two reasons this exists instead of passing [CutOddsBoardEntry] straight to Compose:
 *  1. **No work during composition.** The row used to map the consensus timeline to floats
 *     and format the percentage on every recomposition, on the main thread. Now that happens
 *     once, on `Dispatchers.Default`, when the state is built.
 *  2. **Skippable rows.** Classes from our pure-Kotlin modules aren't compiled by the Compose
 *     compiler, so it treats them as unstable. `@Immutable` tells it this row's equality is
 *     meaningful, so unchanged rows skip recomposition when you type in search or refresh.
 */
@Immutable
data class BoardRow(
    val playerId: String,
    val name: String,
    val country: String,
    val isKornFerry: Boolean,
    val result: CutResult?,
    /** e.g. "83%"; null when no book is pricing the player. */
    val percentText: String?,
    val changePoints: Double?,
    val bestPriceBook: String?,
    val bestPriceOdds: String?,
    /** Consensus probability per snapshot, 0..1, ready for the Sparkline. */
    val sparkline: List<Float?>,
)

internal fun CutOddsBoardEntry.toRow() = BoardRow(
    playerId = player.id,
    name = player.name,
    country = player.country,
    isKornFerry = player.tour == Tour.KORN_FERRY_TOUR,
    result = result,
    percentText = currentProbability?.formatPercent(),
    changePoints = changeSinceOpen,
    bestPriceBook = bestYesPrice?.sportsbook?.displayName,
    bestPriceOdds = bestYesPrice?.odds?.toString(),
    sparkline = timeline.consensus.map { it?.value?.toFloat() },
)
