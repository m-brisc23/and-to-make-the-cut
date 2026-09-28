package com.tomakethecut.core.data.repository

import com.tomakethecut.core.data.di.IoDispatcher
import com.tomakethecut.core.data.mapper.toModel
import com.tomakethecut.core.data.network.OddsApi
import com.tomakethecut.core.domain.DataException
import com.tomakethecut.core.domain.repository.TournamentRepository
import com.tomakethecut.core.model.Seasons
import com.tomakethecut.core.model.Tournament
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTournamentRepository @Inject constructor(
    private val api: OddsApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TournamentRepository {

    private val cache = MemoryCache<Int, List<Tournament>>()

    override suspend fun getTournaments(season: Int, forceRefresh: Boolean): List<Tournament> =
        cache.getOrLoad(season, forceRefresh) {
            val dto = apiCall("Tournaments for $season") { api.getTournaments(season) }
            // Retrofit is already main-safe; the dispatcher is for parsing dates/mapping.
            withContext(ioDispatcher) { dto.tournaments.map { it.toModel() } }
        }

    override suspend fun getTournament(tournamentId: String): Tournament {
        for (season in Seasons.supported) {
            getTournaments(season).firstOrNull { it.id == tournamentId }?.let { return it }
        }
        throw DataException.NotFound("Tournament $tournamentId")
    }
}
