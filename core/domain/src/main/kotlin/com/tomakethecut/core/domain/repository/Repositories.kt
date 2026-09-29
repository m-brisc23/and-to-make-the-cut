package com.tomakethecut.core.domain.repository

import com.tomakethecut.core.model.MakeCutMarket
import com.tomakethecut.core.model.PlayerSeasonStats
import com.tomakethecut.core.model.Tournament

/*
 * Repository contracts. Small, role-specific interfaces (Interface Segregation):
 * the player screen never sees tournament-listing methods it doesn't use, and each
 * interface can be faked in a handful of lines in tests.
 *
 * All functions are main-safe suspend functions and throw DataException on failure.
 */

interface TournamentRepository {
    suspend fun getTournaments(season: Int, forceRefresh: Boolean = false): List<Tournament>

    /** @throws com.tomakethecut.core.domain.DataException.NotFound when the id is unknown. */
    suspend fun getTournament(tournamentId: String): Tournament
}

interface CutOddsRepository {
    suspend fun getMakeCutMarket(tournamentId: String, forceRefresh: Boolean = false): MakeCutMarket
}

interface PlayerStatsRepository {
    /** Returns `null` when the player has no record for that season (e.g. a rookie). */
    suspend fun getSeasonStats(playerId: String, season: Int): PlayerSeasonStats?
}
