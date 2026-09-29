package com.tomakethecut.core.data.network.dto

import kotlinx.serialization.Serializable

/*
 * DTOs mirror the wire format exactly — strings stay strings, enums stay strings.
 * Converting to typed domain models happens in the mappers, where an unexpected value
 * can be handled gracefully instead of crashing deserialization for the whole payload.
 */

@Serializable
data class TournamentsResponseDto(
    val season: Int,
    val tournaments: List<TournamentDto>,
)

@Serializable
data class TournamentDto(
    val id: String,
    val name: String,
    val course: String,
    val location: String,
    val season: Int,
    val startDate: String,
    val endDate: String,
    val status: String,
    val eventType: String,
    val cutRule: String = "",
)

@Serializable
data class MakeCutMarketDto(
    val tournamentId: String,
    val market: String,
    val snapshots: List<SnapshotDto>,
    val players: List<PlayerMarketDto>,
)

@Serializable
data class SnapshotDto(
    val id: String,
    val label: String,
    val round: Int,
    val holesCompleted: Int,
    val capturedAt: String,
)

@Serializable
data class PlayerMarketDto(
    val playerId: String,
    val playerName: String,
    val country: String,
    val tour: String,
    val lines: List<LineDto>,
    val result: String? = null,
)

@Serializable
data class LineDto(
    val sportsbook: String,
    val snapshotId: String,
    val yesPrice: Int,
    val noPrice: Int? = null,
)
