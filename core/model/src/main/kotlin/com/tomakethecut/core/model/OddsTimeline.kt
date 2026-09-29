package com.tomakethecut.core.model

/** A single book's price at one snapshot, with the no-vig probability derived from it. */
data class TimelinePoint(
    val probability: Probability,
    val yes: AmericanOdds,
    val no: AmericanOdds?,
)

/**
 * Chart-ready view of one player's make-cut odds across a tournament.
 *
 * Every list is index-aligned with [snapshots]; a `null` entry means that book had
 * no market at that moment (not yet posted, or pulled because the outcome was
 * all-but-certain). Keeping gaps explicit lets the chart draw a broken line instead
 * of inventing data.
 */
data class OddsTimeline(
    val snapshots: List<OddsSnapshot>,
    val byBook: Map<Sportsbook, List<TimelinePoint?>>,
    val consensus: List<Probability?>,
) {
    val latestConsensus: Probability? get() = consensus.lastOrNull { it != null }
    val openingConsensus: Probability? get() = consensus.firstOrNull { it != null }

    /** Change in percentage points from the first to the latest priced snapshot. */
    val changeSinceOpen: Double?
        get() {
            val open = openingConsensus ?: return null
            val latest = latestConsensus ?: return null
            return (latest.value - open.value) * 100
        }
}
