package com.tomakethecut.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.tomakethecut.feature.cutodds.CutOddsBoardRoute
import com.tomakethecut.feature.cutodds.cutOddsBoardScreen
import com.tomakethecut.feature.player.navigateToPlayerDetail
import com.tomakethecut.feature.player.playerDetailScreen

/**
 * The app module is the only place that knows about *both* features — it's the
 * composition root for navigation just as the Hilt component is for objects.
 */
@Composable
fun ToMakeTheCutNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = CutOddsBoardRoute, modifier = modifier) {
        cutOddsBoardScreen(
            onPlayerClick = { tournamentId, playerId ->
                navController.navigateToPlayerDetail(tournamentId, playerId) { launchSingleTop = true }
            },
        )
        playerDetailScreen(onBack = navController::popBackStack)
    }
}
