package com.tomakethecut.core.data.mapper

import com.tomakethecut.core.data.network.dto.LineDto
import com.tomakethecut.core.data.network.dto.MakeCutMarketDto
import com.tomakethecut.core.data.network.dto.PlayerMarketDto
import com.tomakethecut.core.data.network.dto.SnapshotDto
import com.tomakethecut.core.data.network.dto.TournamentDto
import com.tomakethecut.core.model.AmericanOdds
import com.tomakethecut.core.model.BookPrice
import com.tomakethecut.core.model.CutResult
import com.tomakethecut.core.model.EventType
import com.tomakethecut.core.model.MakeCutMarket
import com.tomakethecut.core.model.OddsSnapshot
import com.tomakethecut.core.model.Player
import com.tomakethecut.core.model.PlayerCutOdds
import com.tomakethecut.core.model.Sportsbook
import com.tomakethecut.core.model.Tour
import com.tomakethecut.core.model.Tournament
import com.tomakethecut.core.model.TournamentStatus
import java.time.Instant
import java.time.LocalDate

/*
 * Mapping rule of thumb: be strict about what the app can't work without (ids, dates)
 * and lenient about everything else. One malformed price should cost us one data point,
 * not the whole leaderboard.
 */

fun TournamentDto.toModel() = Tournament(
    id = id,
    name = name,
    course = course,
    location = location,
    season = season,
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    status = enumOrNull<TournamentStatus>(status) ?: TournamentStatus.UPCOMING,
    eventType = enumOrNull<EventType>(eventType) ?: EventType.FULL_FIELD,
    cutRule = cutRule,
)

fun MakeCutMarketDto.toModel(): MakeCutMarket {
    val snapshotIds = snapshots.map { it.id }.toSet()
    return MakeCutMarket(
        tournamentId = tournamentId,
        snapshots = snapshots.map(SnapshotDto::toModel).sortedBy { it.capturedAt },
        players = players.map { it.toModel(snapshotIds) },
    )
}

fun SnapshotDto.toModel() = OddsSnapshot(
    id = id,
    label = label,
    round = round,
    holesCompleted = holesCompleted,
    capturedAt = Instant.parse(capturedAt),
)

fun PlayerMarketDto.toModel(validSnapshotIds: Set<String>) = PlayerCutOdds(
    player = Player(id = playerId, name = playerName, country = country, tour = tour.toTour()),
    prices = lines.filter { it.snapshotId in validSnapshotIds }.mapNotNull(LineDto::toModelOrNull),
    result = result?.let { enumOrNull<CutResult>(it) },
)

/** Drops lines from books we don't support yet, or with impossible prices. */
fun LineDto.toModelOrNull(): BookPrice? {
    val book = Sportsbook.fromApiKey(sportsbook) ?: return null
    val yes = yesPrice.toAmericanOddsOrNull() ?: return null
    return BookPrice(snapshotId = snapshotId, sportsbook = book, yes = yes, no = noPrice?.toAmericanOddsOrNull())
}

private fun Int.toAmericanOddsOrNull(): AmericanOdds? = if (this <= -100 || this >= 100) AmericanOdds(this) else null

internal fun String.toTour(): Tour = enumOrNull<Tour>(this) ?: Tour.PGA_TOUR

internal inline fun <reified E : Enum<E>> enumOrNull(value: String): E? =
    enumValues<E>().firstOrNull { it.name.equals(value, ignoreCase = true) }
