package com.tomakethecut.core.domain.usecase

import com.tomakethecut.core.domain.repository.TournamentRepository
import com.tomakethecut.core.domain.suspendRunCatching
import com.tomakethecut.core.model.Tournament
import com.tomakethecut.core.model.TournamentStatus
import javax.inject.Inject

/**
 * Use cases are single-purpose classes with one public `operator fun invoke`.
 *
 * Trade-off: for a one-line pass-through a use case can feel like ceremony (Google's
 * guidance calls the domain layer *optional*). We keep them because each one here
 * carries real logic (ordering, selection, combining sources) that would otherwise
 * leak into ViewModels and be duplicated across screens.
 */
class GetTournamentsUseCase @Inject constructor(
    private val repository: TournamentRepository,
) {
    suspend operator fun invoke(season: Int, forceRefresh: Boolean = false): Result<List<Tournament>> =
        suspendRunCatching {
            repository.getTournaments(season, forceRefresh).sortedByDescending { it.startDate }
        }
}

/**
 * Picks the tournament to show by default: the one being played now, else this
 * week's upcoming event, else the most recently completed one.
 */
fun List<Tournament>.featured(): Tournament? =
    firstOrNull { it.status == TournamentStatus.IN_PROGRESS }
        ?: filter { it.status == TournamentStatus.UPCOMING }.minByOrNull { it.startDate }
        ?: filter { it.status == TournamentStatus.COMPLETED }.maxByOrNull { it.endDate }
