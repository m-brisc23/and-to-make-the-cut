@file:OptIn(ExperimentalMaterial3Api::class)

package com.tomakethecut.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomakethecut.core.domain.usecase.PlayerCutOddsDetail
import com.tomakethecut.core.model.Sportsbook
import com.tomakethecut.core.ui.component.ChangeIndicator
import com.tomakethecut.core.ui.component.CutResultBadge
import com.tomakethecut.core.ui.component.EmptyState
import com.tomakethecut.core.ui.component.ErrorState
import com.tomakethecut.core.ui.component.LoadingState
import com.tomakethecut.core.ui.component.MockDataBanner
import com.tomakethecut.core.ui.component.TourBadge
import com.tomakethecut.core.ui.state.LoadState

@Composable
internal fun PlayerDetailScreen(
    onBack: () -> Unit,
    viewModel: PlayerDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PlayerDetailContent(
        state = state,
        onBack = onBack,
        onSnapshotSelected = viewModel::onSnapshotSelected,
        onToggleBook = viewModel::onToggleBook,
        onStatsSeasonSelected = viewModel::onStatsSeasonSelected,
        onRetryOdds = viewModel::onRetryOdds,
        onRetryStats = viewModel::onRetryStats,
    )
}

@Composable
internal fun PlayerDetailContent(
    state: PlayerDetailUiState,
    onBack: () -> Unit,
    onSnapshotSelected: (Int) -> Unit,
    onToggleBook: (Sportsbook) -> Unit,
    onStatsSeasonSelected: (Int) -> Unit,
    onRetryOdds: () -> Unit,
    onRetryStats: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.playerName ?: stringResource(R.string.player_title_fallback)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.player_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "banner") { MockDataBanner(Modifier.padding(top = 8.dp)) }
            oddsSection(state, onSnapshotSelected, onToggleBook, onRetryOdds)
            statsSection(state, onStatsSeasonSelected, onRetryStats)
        }
    }
}

private fun LazyListScope.oddsSection(
    state: PlayerDetailUiState,
    onSnapshotSelected: (Int) -> Unit,
    onToggleBook: (Sportsbook) -> Unit,
    onRetry: () -> Unit,
) {
    when (val odds = state.odds) {
        LoadState.Loading -> item(key = "odds-loading") { LoadingState() }
        is LoadState.Error -> item(key = "odds-error") { ErrorState(odds.kind, onRetry) }
        is LoadState.Success -> {
            item(key = "odds-header") { OddsHeader(odds.data) }
            item(key = "odds-chart") {
                OddsChartCard(
                    detail = odds.data,
                    selectedIndex = state.selectedSnapshotIndex,
                    hiddenBooks = state.hiddenBooks,
                    onSnapshotSelected = onSnapshotSelected,
                    onToggleBook = onToggleBook,
                )
            }
        }
    }
}

private fun LazyListScope.statsSection(
    state: PlayerDetailUiState,
    onSeasonSelected: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    item(key = "stats-title") {
        Text(
            stringResource(R.string.player_stats_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    when (val stats = state.stats) {
        LoadState.Loading -> item(key = "stats-loading") { LoadingState() }
        is LoadState.Error -> item(key = "stats-error") { ErrorState(stats.kind, onRetry) }
        is LoadState.Success -> {
            if (stats.data.isEmpty()) {
                item(key = "stats-empty") { EmptyState(stringResource(R.string.player_stats_empty)) }
                return
            }
            item(key = "stats-seasons") { SeasonChips(stats.data, state.selectedStatsSeason, onSeasonSelected) }
            state.selectedSeasonStats?.let { selected ->
                item(key = "stats-grid-${selected.season}") { StatsGrid(selected) }
                item(key = "stats-sg-${selected.season}") { StrokesGainedCard(selected.strokesGained) }
                item(key = "stats-results-${selected.season}") { RecentResultsCard(selected) }
            }
        }
    }
}

@Composable
private fun OddsHeader(detail: PlayerCutOddsDetail) {
    val entry = detail.entry
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.player.name, style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.player.country, style = MaterialTheme.typography.bodyMedium)
                    TourBadge(entry.player.tour)
                }
                Text(
                    "${detail.tournament.name} · ${detail.tournament.season}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                entry.result?.let { CutResultBadge(it) }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    entry.currentProbability?.formatPercent() ?: stringResource(R.string.player_table_none),
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(stringResource(R.string.player_to_make_cut), style = MaterialTheme.typography.labelMedium)
                ChangeIndicator(entry.changeSinceOpen)
            }
        }
    }
}
