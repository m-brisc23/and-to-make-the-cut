package com.tomakethecut.core.data.network

import com.tomakethecut.core.data.network.dto.MakeCutMarketDto
import com.tomakethecut.core.data.network.dto.TournamentsResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * An odds *aggregator* contract (the shape services like The Odds API or OddsJam expose),
 * rather than DraftKings' private endpoints. Aggregating across books behind one API is
 * what you'd do in production: sportsbooks don't offer public APIs, their internal
 * endpoints change without notice, and scraping them usually violates their terms.
 *
 * To go live: point `ApiConfig.oddsBaseUrl` at the real provider and, if its JSON differs,
 * add DTOs + a mapper for it. Nothing above the data layer changes.
 */
interface OddsApi {

    @GET("v1/golf/tournaments")
    suspend fun getTournaments(@Query("season") season: Int): TournamentsResponseDto

    @GET("v1/golf/tournaments/{tournamentId}/markets/make-cut")
    suspend fun getMakeCutMarket(@Path("tournamentId") tournamentId: String): MakeCutMarketDto
}
