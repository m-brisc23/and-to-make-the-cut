package com.tomakethecut.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlayerSeasonStatsDto(
    val playerId: String,
    val playerName: String,
    val country: String,
    val season: Int,
    val tour: String,
    val eventsPlayed: Int,
    val cutsMade: Int,
    val wins: Int,
    val top10s: Int,
    val scoringAverage: Double,
    val strokesGained: StrokesGainedDto,
    val drivingDistance: Double,
    val drivingAccuracyPct: Double,
    val greensInRegulationPct: Double,
    val pointsRank: Int? = null,
    val pointsLabel: String = "",
    val recentResults: List<EventResultDto> = emptyList(),
)

@Serializable
data class StrokesGainedDto(
    val total: Double,
    val offTheTee: Double,
    val approach: Double,
    val aroundTheGreen: Double,
    val putting: Double,
)

@Serializable
data class EventResultDto(
    val tournamentName: String,
    val date: String,
    val finish: String,
    val tour: String,
)
