package com.tomakethecut.core.model

import java.time.Instant

/** A moment in time at which every book's make-cut prices were captured. */
data class OddsSnapshot(
    val id: String,
    val label: String,
    /** 0 = pre-tournament, 1 = during round one, 2 = during round two. */
    val round: Int,
    val holesCompleted: Int,
    val capturedAt: Instant,
)

/** One book's two-way make-cut market at one snapshot. */
data class BookPrice(
    val snapshotId: String,
    val sportsbook: Sportsbook,
    val yes: AmericanOdds,
    /** Some books only hang the "Yes" side; we can't remove the vig without "No". */
    val no: AmericanOdds?,
)

enum class CutResult { MADE_CUT, MISSED_CUT }

data class PlayerCutOdds(
    val player: Player,
    val prices: List<BookPrice>,
    /** Null until the cut is official. */
    val result: CutResult?,
)

/** The "Make the cut — Yes/No" market for every priced player in a tournament. */
data class MakeCutMarket(
    val tournamentId: String,
    val snapshots: List<OddsSnapshot>,
    val players: List<PlayerCutOdds>,
)
