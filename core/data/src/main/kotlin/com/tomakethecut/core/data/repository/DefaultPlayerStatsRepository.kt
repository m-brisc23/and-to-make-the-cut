package com.tomakethecut.core.data.repository

import com.tomakethecut.core.data.mapper.toModel
import com.tomakethecut.core.data.network.StatsApi
import com.tomakethecut.core.domain.DataException
import com.tomakethecut.core.domain.repository.PlayerStatsRepository
import com.tomakethecut.core.model.PlayerSeasonStats
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPlayerStatsRepository @Inject constructor(
    private val api: StatsApi,
) : PlayerStatsRepository {

    private val cache = MemoryCache<Pair<String, Int>, Optional>()

    override suspend fun getSeasonStats(playerId: String, season: Int): PlayerSeasonStats? =
        cache.getOrLoad(playerId to season) {
            try {
                Optional(apiCall("Stats for $playerId in $season") { api.getSeasonStats(playerId, season).toModel() })
            } catch (e: DataException.NotFound) {
                // "No stats this season" is a normal answer, not an error — and worth caching.
                Optional(null)
            }
        }.value

    /** MemoryCache can't hold nulls, so wrap them. */
    private data class Optional(val value: PlayerSeasonStats?)
}
