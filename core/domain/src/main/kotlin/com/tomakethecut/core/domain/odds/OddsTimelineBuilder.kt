package com.tomakethecut.core.domain.odds

import com.tomakethecut.core.model.OddsSnapshot
import com.tomakethecut.core.model.OddsTimeline
import com.tomakethecut.core.model.PlayerCutOdds
import com.tomakethecut.core.model.Sportsbook
import com.tomakethecut.core.model.TimelinePoint

/**
 * Pivots a flat list of prices (the shape odds APIs return) into per-book series that
 * are index-aligned with the tournament's snapshots (the shape a chart wants).
 */
fun PlayerCutOdds.toTimeline(snapshots: List<OddsSnapshot>): OddsTimeline {
    val pricesBySnapshot = prices.groupBy { it.snapshotId }

    val byBook: Map<Sportsbook, List<TimelinePoint?>> = prices
        .map { it.sportsbook }
        .distinct()
        .sortedBy { it.ordinal } // stable order => stable colours in the chart
        .associateWith { book ->
            snapshots.map { snapshot ->
                pricesBySnapshot[snapshot.id]
                    ?.firstOrNull { it.sportsbook == book }
                    ?.let { TimelinePoint(OddsMath.noVigProbability(it.yes, it.no), it.yes, it.no) }
            }
        }

    val consensus = snapshots.indices.map { index ->
        OddsMath.consensus(byBook.values.mapNotNull { series -> series[index]?.probability })
    }

    return OddsTimeline(snapshots = snapshots, byBook = byBook, consensus = consensus)
}
