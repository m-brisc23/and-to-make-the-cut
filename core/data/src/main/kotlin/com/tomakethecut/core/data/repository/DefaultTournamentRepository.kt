package com.tomakethecut.core.data.repository

import com.tomakethecut.core.data.mapper.toModel
import com.tomakethecut.core.data.network.OddsApi
import com.tomakethecut.core.domain.DataException
import com.tomakethecut.core.domain.di.DefaultDispatcher
import com.tomakethecut.core.domain.di.IoDispatcher
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
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : TournamentRepository {

    private val cache = MemoryCache<Int, List<Tournament>>()

    override suspend fun getTournaments(season: Int, forceRefresh: Boolean): List<Tournament> =
        cache.getOrLoad(season, forceRefresh) {
            // Network on IO (this is also where OkHttp/Retrofit lazily initialise on first use),
            // mapping on Default: it's CPU work, and IO's large pool is for blocking calls.
            val dto = withContext(ioDispatcher) { apiCall("Tournaments for $season") { api.getTournaments(season) } }
            withContext(defaultDispatcher) { dto.tournaments.map { it.toModel() } }
        }

    override suspend fun getTournament(tournamentId: String): Tournament {
        for (season in Seasons.supported) {
            getTournaments(season).firstOrNull { it.id == tournamentId }?.let { return it }
        }
        throw DataException.NotFound("Tournament $tournamentId")
    }
}
