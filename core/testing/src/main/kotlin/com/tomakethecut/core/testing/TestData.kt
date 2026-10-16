package com.tomakethecut.core.testing

import com.tomakethecut.core.model.AmericanOdds
import com.tomakethecut.core.model.BookPrice
import com.tomakethecut.core.model.CutResult
import com.tomakethecut.core.model.EventResult
import com.tomakethecut.core.model.EventType
import com.tomakethecut.core.model.Finish
import com.tomakethecut.core.model.MakeCutMarket
import com.tomakethecut.core.model.OddsSnapshot
import com.tomakethecut.core.model.Player
import com.tomakethecut.core.model.PlayerCutOdds
import com.tomakethecut.core.model.PlayerSeasonStats
import com.tomakethecut.core.model.Sportsbook
import com.tomakethecut.core.model.StrokesGained
import com.tomakethecut.core.model.Tour
import com.tomakethecut.core.model.Tournament
import com.tomakethecut.core.model.TournamentStatus
import java.time.Instant
import java.time.LocalDate

/**
 * Test-data builders with sensible defaults: each test overrides only the fields it
 * cares about, which keeps the *intent* of the test visible.
 */
object TestData {

    fun tournament(
        id: String = "masters-tournament-2026",
        season: Int = 2026,
        status: TournamentStatus = TournamentStatus.COMPLETED,
        start: LocalDate = LocalDate.of(2026, 4, 9),
        name: String = "Masters Tournament",
    ) = Tournament(
        id = id,
        name = name,
        course = "Augusta National Golf Club",
        location = "Augusta, GA",
        season = season,
        startDate = start,
        endDate = start.plusDays(3),
        status = status,
        eventType = EventType.MAJOR,
        cutRule = "Top 50 & ties",
    )

    fun player(
        id: String = "scottie-scheffler",
        name: String = "Scottie Scheffler",
        country: String = "USA",
        tour: Tour = Tour.PGA_TOUR,
    ) = Player(id, name, country, tour)

    fun snapshots(count: Int = 3): List<OddsSnapshot> = (0 until count).map { i ->
        OddsSnapshot(
            id = "s$i",
            label = "Snapshot $i",
            round = if (i == 0) 0 else 1,
            holesCompleted = i * 6,
            capturedAt = Instant.parse("2026-04-08T12:00:00Z").plusSeconds(3_600L * i),
        )
    }

    fun price(
        snapshotId: String,
        book: Sportsbook = Sportsbook.DRAFTKINGS,
        yes: Int = -200,
        no: Int? = 170,
    ) = BookPrice(snapshotId, book, AmericanOdds(yes), no?.let(::AmericanOdds))

    fun playerOdds(
        player: Player = player(),
        prices: List<BookPrice> = listOf(price("s0"), price("s1"), price("s2")),
        result: CutResult? = null,
    ) = PlayerCutOdds(player, prices, result)

    fun market(
        tournamentId: String = "masters-tournament-2026",
        snapshots: List<OddsSnapshot> = snapshots(),
        players: List<PlayerCutOdds> = listOf(playerOdds()),
    ) = MakeCutMarket(tournamentId, snapshots, players)

    fun seasonStats(
        player: Player = player(),
        season: Int = 2026,
        tour: Tour = Tour.PGA_TOUR,
    ) = PlayerSeasonStats(
        player = player,
        season = season,
        tour = tour,
        eventsPlayed = 20,
        cutsMade = 19,
        wins = 4,
        top10s = 14,
        scoringAverage = 68.4,
        strokesGained = StrokesGained(2.8, 0.7, 1.3, 0.3, 0.5),
        drivingDistance = 306.2,
        drivingAccuracyPct = 63.1,
        greensInRegulationPct = 71.5,
        pointsRank = 1,
        pointsLabel = "FedExCup",
        recentResults = listOf(
            EventResult("The Open Championship", LocalDate.of(season, 7, 19), Finish.Position(1, tied = false), tour),
            EventResult("U.S. Open", LocalDate.of(season, 6, 21), Finish.MissedCut, tour),
        ),
    )
}
