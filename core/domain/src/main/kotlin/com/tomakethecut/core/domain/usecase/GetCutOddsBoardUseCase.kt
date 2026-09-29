package com.tomakethecut.core.domain.usecase

import com.tomakethecut.core.domain.di.DefaultDispatcher
import com.tomakethecut.core.domain.odds.toTimeline
import com.tomakethecut.core.domain.repository.CutOddsRepository
import com.tomakethecut.core.domain.suspendRunCatching
import com.tomakethecut.core.model.MakeCutMarket
import com.tomakethecut.core.model.PlayerCutOdds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Main-safe: callers (ViewModels on `Dispatchers.Main`) can invoke this directly. Building
 * the board — pivoting every player's prices into per-book timelines, de-vigging each point,
 * averaging a consensus and finding the best price — is the heaviest CPU work in the app, so
 * this class moves it to the Default dispatcher itself rather than trusting every caller to.
 */
class GetCutOddsBoardUseCase @Inject constructor(
    private val repository: CutOddsRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(tournamentId: String, forceRefresh: Boolean = false): Result<CutOddsBoard> =
        suspendRunCatching {
            val market = repository.getMakeCutMarket(tournamentId, forceRefresh)
            withContext(defaultDispatcher) { market.toBoard() }
        }
}

internal fun MakeCutMarket.toBoard(): CutOddsBoard = CutOddsBoard(
    tournamentId = tournamentId,
    snapshots = snapshots,
    entries = players.map { it.toEntry(this) },
)

internal fun PlayerCutOdds.toEntry(market: MakeCutMarket): CutOddsBoardEntry {
    val timeline = toTimeline(market.snapshots)
    val latestPricedSnapshotId = market.snapshots
        .lastOrNull { snapshot -> prices.any { it.snapshotId == snapshot.id } }
        ?.id
    val best = prices
        .filter { it.snapshotId == latestPricedSnapshotId }
        .maxByOrNull { it.yes.profitPer100 }
        ?.let { BestPrice(it.sportsbook, it.yes) }
    return CutOddsBoardEntry(player = player, timeline = timeline, result = result, bestYesPrice = best)
}
