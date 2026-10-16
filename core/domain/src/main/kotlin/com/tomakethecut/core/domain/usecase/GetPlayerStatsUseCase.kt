package com.tomakethecut.core.domain.usecase

import com.tomakethecut.core.domain.repository.PlayerStatsRepository
import com.tomakethecut.core.domain.suspendRunCatching
import com.tomakethecut.core.model.PlayerSeasonStats
import com.tomakethecut.core.model.Seasons
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Fetches a player's stats for every supported season in parallel, newest first.
 *
 * A season with no record (e.g. a 2026 rookie in 2025 on the PGA TOUR — who'll have
 * Korn Ferry stats instead) is simply omitted. If *any* request fails with an error
 * the whole call fails: showing half a profile silently would be misleading.
 */
class GetPlayerStatsUseCase @Inject constructor(
    private val repository: PlayerStatsRepository,
) {
    suspend operator fun invoke(
        playerId: String,
        seasons: List<Int> = Seasons.supported,
    ): Result<List<PlayerSeasonStats>> = suspendRunCatching {
        coroutineScope {
            seasons
                .map { season -> async { repository.getSeasonStats(playerId, season) } }
                .awaitAll()
                .filterNotNull()
                .sortedByDescending { it.season }
        }
    }
}
