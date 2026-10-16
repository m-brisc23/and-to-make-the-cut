package com.tomakethecut.core.model

import java.time.LocalDate

data class StrokesGained(
    val total: Double,
    val offTheTee: Double,
    val approach: Double,
    val aroundTheGreen: Double,
    val putting: Double,
)

sealed interface Finish {
    data class Position(val position: Int, val tied: Boolean) : Finish {
        override fun toString() = if (tied) "T$position" else "$position"
    }

    data object MissedCut : Finish {
        override fun toString() = "MC"
    }

    /** Withdrawals, DQs and anything a future provider adds that we don't model yet. */
    data class Other(val raw: String) : Finish {
        override fun toString() = raw
    }
}

data class EventResult(
    val tournamentName: String,
    val date: LocalDate,
    val finish: Finish,
    val tour: Tour,
)

data class PlayerSeasonStats(
    val player: Player,
    val season: Int,
    val tour: Tour,
    val eventsPlayed: Int,
    val cutsMade: Int,
    val wins: Int,
    val top10s: Int,
    val scoringAverage: Double,
    val strokesGained: StrokesGained,
    val drivingDistance: Double,
    val drivingAccuracyPct: Double,
    val greensInRegulationPct: Double,
    val pointsRank: Int?,
    val pointsLabel: String,
    val recentResults: List<EventResult>,
) {
    val cutsMadeRate: Probability
        get() = if (eventsPlayed == 0) Probability(0.0) else Probability.clamped(cutsMade.toDouble() / eventsPlayed)
}
