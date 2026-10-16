package com.tomakethecut.core.testing

import com.tomakethecut.core.domain.DataException
import com.tomakethecut.core.domain.repository.CutOddsRepository
import com.tomakethecut.core.domain.repository.PlayerStatsRepository
import com.tomakethecut.core.domain.repository.TournamentRepository
import com.tomakethecut.core.model.MakeCutMarket
import com.tomakethecut.core.model.PlayerSeasonStats
import com.tomakethecut.core.model.Tournament
import kotlinx.coroutines.CompletableDeferred

class FakeTournamentRepository : TournamentRepository {
    val tournamentsBySeason = mutableMapOf<Int, List<Tournament>>()
    var error: Exception? = null
    var requests = 0
        private set

    override suspend fun getTournaments(season: Int, forceRefresh: Boolean): List<Tournament> {
        requests++
        error?.let { throw it }
        return tournamentsBySeason[season].orEmpty()
    }

    override suspend fun getTournament(tournamentId: String): Tournament {
        error?.let { throw it }
        return tournamentsBySeason.values.flatten().firstOrNull { it.id == tournamentId }
            ?: throw DataException.NotFound("Tournament $tournamentId")
    }
}

class FakeCutOddsRepository : CutOddsRepository {
    val markets = mutableMapOf<String, MakeCutMarket>()
    var error: Exception? = null
    val forceRefreshRequests = mutableListOf<String>()

    /** When set, calls suspend until the test completes it — handy for asserting Loading states. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun getMakeCutMarket(tournamentId: String, forceRefresh: Boolean): MakeCutMarket {
        if (forceRefresh) forceRefreshRequests += tournamentId
        gate?.await()
        error?.let { throw it }
        return markets[tournamentId] ?: throw DataException.NotFound("Market $tournamentId")
    }
}

class FakePlayerStatsRepository : PlayerStatsRepository {
    val stats = mutableMapOf<Pair<String, Int>, PlayerSeasonStats>()
    var error: Exception? = null

    override suspend fun getSeasonStats(playerId: String, season: Int): PlayerSeasonStats? {
        error?.let { throw it }
        return stats[playerId to season]
    }
}
