package com.tomakethecut.core.domain.usecase

import com.tomakethecut.core.domain.odds.toTimeline
import com.tomakethecut.core.domain.repository.CutOddsRepository
import com.tomakethecut.core.domain.suspendRunCatching
import com.tomakethecut.core.model.MakeCutMarket
import com.tomakethecut.core.model.PlayerCutOdds
import javax.inject.Inject

class GetCutOddsBoardUseCase @Inject constructor(
    private val repository: CutOddsRepository,
) {
    suspend operator fun invoke(tournamentId: String, forceRefresh: Boolean = false): Result<CutOddsBoard> =
        suspendRunCatching {
            repository.getMakeCutMarket(tournamentId, forceRefresh).toBoard()
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
