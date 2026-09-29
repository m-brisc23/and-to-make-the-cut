package com.tomakethecut.core.data.network

import com.tomakethecut.core.data.network.dto.PlayerSeasonStatsDto
import retrofit2.http.GET
import retrofit2.http.Path

/** Player statistics for the PGA TOUR and Korn Ferry Tour. Separate host from odds on purpose. */
interface StatsApi {

    @GET("v1/players/{playerId}/seasons/{season}/stats")
    suspend fun getSeasonStats(
        @Path("playerId") playerId: String,
        @Path("season") season: Int,
    ): PlayerSeasonStatsDto
}
