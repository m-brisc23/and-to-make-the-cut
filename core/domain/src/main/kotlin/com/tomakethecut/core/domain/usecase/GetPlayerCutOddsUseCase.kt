package com.tomakethecut.core.domain.usecase

import com.tomakethecut.core.domain.DataException
import com.tomakethecut.core.domain.repository.CutOddsRepository
import com.tomakethecut.core.domain.repository.TournamentRepository
import com.tomakethecut.core.domain.suspendRunCatching
import com.tomakethecut.core.model.Tournament
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

data class PlayerCutOddsDetail(
    val tournament: Tournament,
    val entry: CutOddsBoardEntry,
)

/**
 * Loads one player's make-cut history for a tournament.
 *
 * The repository caches markets, so arriving here from the board costs no extra
 * network call — the detail screen gets "instant" data while still owning its own
 * loading logic (it also works when deep-linked straight to a player).
 */
class GetPlayerCutOddsUseCase @Inject constructor(
    private val oddsRepository: CutOddsRepository,
    private val tournamentRepository: TournamentRepository,
) {
    suspend operator fun invoke(tournamentId: String, playerId: String): Result<PlayerCutOddsDetail> =
        suspendRunCatching {
            coroutineScope {
                val tournament = async { tournamentRepository.getTournament(tournamentId) }
                val market = async { oddsRepository.getMakeCutMarket(tournamentId) }
                val player = market.await().players.firstOrNull { it.player.id == playerId }
                    ?: throw DataException.NotFound("Player $playerId in $tournamentId")
                PlayerCutOddsDetail(tournament.await(), player.toEntry(market.await()))
            }
        }
}
