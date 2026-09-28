package com.tomakethecut.core.data.mapper

import com.tomakethecut.core.data.network.dto.EventResultDto
import com.tomakethecut.core.data.network.dto.PlayerSeasonStatsDto
import com.tomakethecut.core.data.network.dto.StrokesGainedDto
import com.tomakethecut.core.model.EventResult
import com.tomakethecut.core.model.Finish
import com.tomakethecut.core.model.Player
import com.tomakethecut.core.model.PlayerSeasonStats
import com.tomakethecut.core.model.StrokesGained
import java.time.LocalDate

fun PlayerSeasonStatsDto.toModel(): PlayerSeasonStats {
    val tour = tour.toTour()
    return PlayerSeasonStats(
        player = Player(id = playerId, name = playerName, country = country, tour = tour),
        season = season,
        tour = tour,
        eventsPlayed = eventsPlayed,
        cutsMade = cutsMade,
        wins = wins,
        top10s = top10s,
        scoringAverage = scoringAverage,
        strokesGained = strokesGained.toModel(),
        drivingDistance = drivingDistance,
        drivingAccuracyPct = drivingAccuracyPct,
        greensInRegulationPct = greensInRegulationPct,
        pointsRank = pointsRank,
        pointsLabel = pointsLabel,
        recentResults = recentResults.map(EventResultDto::toModel).sortedByDescending { it.date },
    )
}

fun StrokesGainedDto.toModel() = StrokesGained(total, offTheTee, approach, aroundTheGreen, putting)

fun EventResultDto.toModel() = EventResult(
    tournamentName = tournamentName,
    date = LocalDate.parse(date),
    finish = parseFinish(finish),
    tour = tour.toTour(),
)

private val POSITION = Regex("(T?)(\\d{1,3})")

/** "1" -> 1st, "T12" -> tied 12th, "MC"/"CUT" -> missed cut, anything else kept verbatim. */
fun parseFinish(raw: String): Finish {
    val value = raw.trim().uppercase()
    if (value == "MC" || value == "CUT") return Finish.MissedCut
    val match = POSITION.matchEntire(value) ?: return Finish.Other(raw)
    return Finish.Position(position = match.groupValues[2].toInt(), tied = match.groupValues[1].isNotEmpty())
}
