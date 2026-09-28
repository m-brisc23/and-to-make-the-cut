@file:OptIn(ExperimentalMaterial3Api::class)

package com.tomakethecut.feature.cutodds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomakethecut.core.domain.usecase.BoardSort
import com.tomakethecut.core.domain.usecase.CutOddsBoardEntry
import com.tomakethecut.core.model.Tour
import com.tomakethecut.core.model.Tournament
import com.tomakethecut.core.model.TournamentStatus
import com.tomakethecut.core.ui.Formatting
import com.tomakethecut.core.ui.chart.Sparkline
import com.tomakethecut.core.ui.component.ChangeIndicator
import com.tomakethecut.core.ui.component.CutResultBadge
import com.tomakethecut.core.ui.component.EmptyState
import com.tomakethecut.core.ui.component.ErrorState
import com.tomakethecut.core.ui.component.LoadingState
import com.tomakethecut.core.ui.component.MockDataBanner
import com.tomakethecut.core.ui.component.TourBadge
import com.tomakethecut.core.ui.component.message
import com.tomakethecut.core.ui.state.LoadState
import com.tomakethecut.core.ui.theme.ToMakeTheCutChartTheme
import com.tomakethecut.core.ui.theme.ToMakeTheCutTheme

/**
 * Stateful entry point: obtains the ViewModel and adapts it to the stateless content.
 * Everything below takes plain values + lambdas, so it can be previewed and UI-tested
 * without Hilt.
 */
@Composable
internal fun CutOddsBoardScreen(
    onPlayerClick: (tournamentId: String, playerId: String) -> Unit,
    viewModel: CutOddsBoardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CutOddsBoardContent(
        state = state,
        onSeasonSelected = viewModel::onSeasonSelected,
        onTournamentSelected = viewModel::onTournamentSelected,
        onQueryChange = viewModel::onQueryChange,
        onSortChange = viewModel::onSortChange,
        onRefresh = viewModel::onRefresh,
        onRetry = viewModel::onRetry,
        onTransientErrorShown = viewModel::onTransientErrorShown,
        onPlayerClick = onPlayerClick,
    )
}

@Composable
internal fun CutOddsBoardContent(
    state: CutOddsBoardUiState,
    onSeasonSelected: (Int) -> Unit,
    onTournamentSelected: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (BoardSort) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onTransientErrorShown: () -> Unit,
    onPlayerClick: (tournamentId: String, playerId: String) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    state.transientError?.let { kind ->
        val message = kind.message()
        LaunchedEffect(kind) {
            snackbarHostState.showSnackbar(message)
            onTransientErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cutodds_title)) },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.cutodds_refresh))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
                item(key = "banner") { MockDataBanner(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                item(key = "seasons") { SeasonSelector(state.seasons, state.selectedSeason, onSeasonSelected) }
                when (val tournaments = state.tournaments) {
                    LoadState.Loading -> item(key = "t-loading") { LoadingState() }
                    is LoadState.Error -> item(key = "t-error") { ErrorState(tournaments.kind, onRetry) }
                    is LoadState.Success -> {
                        item(key = "tournaments") {
                            TournamentChips(tournaments.data, state.selectedTournamentId, onTournamentSelected)
                        }
                        boardSection(state, onQueryChange, onSortChange, onRetry, onPlayerClick)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.boardSection(
    state: CutOddsBoardUiState,
    onQueryChange: (String) -> Unit,
    onSortChange: (BoardSort) -> Unit,
    onRetry: () -> Unit,
    onPlayerClick: (String, String) -> Unit,
) {
    when (val board = state.board) {
        LoadState.Loading -> item(key = "b-loading") { LoadingState() }
        is LoadState.Error -> item(key = "b-error") { ErrorState(board.kind, onRetry) }
        is LoadState.Success -> {
            val data = board.data
            item(key = "header") { TournamentHeader(data) }
            item(key = "controls") { SearchAndSort(state.query, state.sort, onQueryChange, onSortChange) }
            if (data.entries.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        if (data.totalPlayers == 0) {
                            stringResource(R.string.cutodds_no_players)
                        } else {
                            stringResource(R.string.cutodds_no_matches, state.query)
                        },
                    )
                }
            }
            items(data.entries, key = { it.player.id }) { entry ->
                PlayerOddsRow(entry, onClick = { onPlayerClick(data.tournament.id, entry.player.id) })
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun SeasonSelector(seasons: List<Int>, selected: Int, onSelected: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        seasons.forEachIndexed { index, season ->
            SegmentedButton(
                selected = season == selected,
                onClick = { onSelected(season) },
                shape = SegmentedButtonDefaults.itemShape(index, seasons.size),
            ) { Text(stringResource(R.string.cutodds_season, season)) }
        }
    }
}

@Composable
private fun TournamentChips(tournaments: List<Tournament>, selectedId: String?, onSelected: (String) -> Unit) {
    val listState = rememberLazyListState()
    val selectedIndex = tournaments.indexOfFirst { it.id == selectedId }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex)
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tournaments, key = { it.id }) { tournament ->
            FilterChip(
                selected = tournament.id == selectedId,
                onClick = { onSelected(tournament.id) },
                label = { Text(tournament.name, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun TournamentHeader(data: BoardData) {
    val t = data.tournament
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t.name, style = MaterialTheme.typography.titleLarge)
            Text("${t.course} · ${t.location}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "${Formatting.dateRange(t.startDate, t.endDate)} · ${t.eventType.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    when (t.status) {
                        TournamentStatus.UPCOMING -> R.string.cutodds_status_upcoming
                        TournamentStatus.IN_PROGRESS -> R.string.cutodds_status_live
                        TournamentStatus.COMPLETED -> R.string.cutodds_status_completed
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            data.snapshots.lastOrNull()?.let {
                Text(stringResource(R.string.cutodds_latest_snapshot, it.label), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                stringResource(R.string.cutodds_players_priced, data.totalPlayers, t.cutRule),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.cutodds_explainer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchAndSort(
    query: String,
    sort: BoardSort,
    onQueryChange: (String) -> Unit,
    onSortChange: (BoardSort) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cutodds_clear_search))
                    }
                }
            },
            placeholder = { Text(stringResource(R.string.cutodds_search)) },
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(BoardSort.entries) { option ->
                FilterChip(
                    selected = option == sort,
                    onClick = { onSortChange(option) },
                    label = { Text(stringResource(option.labelRes())) },
                )
            }
        }
    }
}

private fun BoardSort.labelRes(): Int = when (this) {
    BoardSort.MOST_LIKELY -> R.string.cutodds_sort_most_likely
    BoardSort.LEAST_LIKELY -> R.string.cutodds_sort_least_likely
    BoardSort.BIGGEST_MOVERS -> R.string.cutodds_sort_movers
    BoardSort.NAME -> R.string.cutodds_sort_name
}

@Composable
private fun PlayerOddsRow(entry: CutOddsBoardEntry, onClick: () -> Unit) {
    val palette = ToMakeTheCutChartTheme.palette
    val percent = entry.currentProbability?.formatPercent() ?: stringResource(R.string.cutodds_no_price)
    val description = stringResource(R.string.cutodds_row_description, entry.player.name, percent)
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        headlineContent = { Text(entry.player.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.player.country, style = MaterialTheme.typography.bodySmall)
                    if (entry.player.tour == Tour.KORN_FERRY_TOUR) TourBadge(entry.player.tour)
                    entry.result?.let { CutResultBadge(it) }
                }
                entry.bestYesPrice?.let {
                    Text(
                        stringResource(R.string.cutodds_best_price, it.sportsbook.displayName, it.odds.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Sparkline(
                    values = entry.timeline.consensus.map { it?.value?.toFloat() },
                    color = palette.consensus,
                    referenceColor = palette.grid,
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(percent, style = MaterialTheme.typography.titleMedium)
                    ChangeIndicator(entry.changeSinceOpen)
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun LoadingPreview() {
    ToMakeTheCutTheme {
        CutOddsBoardContent(
            state = CutOddsBoardUiState(),
            onSeasonSelected = {},
            onTournamentSelected = {},
            onQueryChange = {},
            onSortChange = {},
            onRefresh = {},
            onRetry = {},
            onTransientErrorShown = {},
            onPlayerClick = { _, _ -> },
        )
    }
}
