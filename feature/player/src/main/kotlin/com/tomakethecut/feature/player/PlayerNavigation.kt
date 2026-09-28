package com.tomakethecut.feature.player

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * Only ids travel through navigation — never whole objects. The destination loads what
 * it needs from the (cached) repository, so it works identically from a deep link,
 * after process death, or from the board.
 */
@Serializable
data class PlayerDetailRoute(val tournamentId: String, val playerId: String)

fun NavController.navigateToPlayerDetail(
    tournamentId: String,
    playerId: String,
    builder: NavOptionsBuilder.() -> Unit = {},
) = navigate(PlayerDetailRoute(tournamentId, playerId), builder)

fun NavGraphBuilder.playerDetailScreen(onBack: () -> Unit) {
    composable<PlayerDetailRoute> {
        PlayerDetailScreen(onBack = onBack)
    }
}
