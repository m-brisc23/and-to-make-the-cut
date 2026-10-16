package com.tomakethecut.feature.cutodds

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Type-safe route (Navigation 2.8+): no string templates, no manual argument parsing. */
@Serializable
data object CutOddsBoardRoute

/**
 * Each feature exposes a NavGraphBuilder extension instead of its screens. The app
 * module wires features together without knowing their internals, and features never
 * depend on each other — navigation to the player screen is just a callback here.
 */
fun NavGraphBuilder.cutOddsBoardScreen(onPlayerClick: (tournamentId: String, playerId: String) -> Unit) {
    composable<CutOddsBoardRoute> {
        CutOddsBoardScreen(onPlayerClick = onPlayerClick)
    }
}
