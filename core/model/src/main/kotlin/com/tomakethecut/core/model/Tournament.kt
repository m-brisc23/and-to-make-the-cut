package com.tomakethecut.core.model

import java.time.LocalDate

data class Tournament(
    val id: String,
    val name: String,
    val course: String,
    val location: String,
    val season: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: TournamentStatus,
    val eventType: EventType,
    val cutRule: String,
)

enum class TournamentStatus { UPCOMING, IN_PROGRESS, COMPLETED }

enum class EventType(val displayName: String) {
    MAJOR("Major / Players"),
    SIGNATURE("Signature Event"),
    FULL_FIELD("Full-field Event"),
}
