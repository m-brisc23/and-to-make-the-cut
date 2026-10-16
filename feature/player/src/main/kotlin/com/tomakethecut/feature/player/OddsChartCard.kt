package com.tomakethecut.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomakethecut.core.domain.usecase.PlayerCutOddsDetail
import com.tomakethecut.core.model.OddsTimeline
import com.tomakethecut.core.model.Sportsbook
import com.tomakethecut.core.ui.chart.ChartSeries
import com.tomakethecut.core.ui.chart.OddsLineChart
import com.tomakethecut.core.ui.theme.ChartPalette
import com.tomakethecut.core.ui.theme.ToMakeTheCutChartTheme

private const val CONSENSUS_KEY = "consensus"

@Composable
internal fun OddsChartCard(
    detail: PlayerCutOddsDetail,
    selectedIndex: Int?,
    hiddenBooks: Set<Sportsbook>,
    onSnapshotSelected: (Int) -> Unit,
    onToggleBook: (Sportsbook) -> Unit,
) {
    val palette = ToMakeTheCutChartTheme.palette
    val timeline = detail.entry.timeline
    val consensusLabel = stringResource(R.string.player_consensus)
    // Mapping domain -> chart series is cheap but allocates; remember it per input.
    val series = remember(timeline, hiddenBooks, palette, consensusLabel) {
        timeline.toChartSeries(hiddenBooks, palette, consensusLabel)
    }
    val snapshots = timeline.snapshots
    val description = stringResource(
        R.string.player_chart_description,
        detail.entry.player.name,
        snapshots.size,
        snapshots.firstOrNull()?.label.orEmpty(),
        snapshots.lastOrNull()?.label.orEmpty(),
    )

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.player_chart_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.player_chart_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            selectedIndex?.let { index ->
                Text(
                    text = "${snapshots[index].label} · $consensusLabel ${timeline.consensus[index]?.formatPercent() ?: "—"}",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            OddsLineChart(
                series = series,
                xLabels = snapshots.map { it.label },
                selectedIndex = selectedIndex,
                onSelectedIndexChange = onSnapshotSelected,
                contentDescription = description,
            )
            Legend(timeline.byBook.keys, hiddenBooks, palette, consensusLabel, onToggleBook)
            selectedIndex?.let { PriceTable(timeline, it, palette, consensusLabel) }
        }
    }
}

internal fun OddsTimeline.toChartSeries(
    hiddenBooks: Set<Sportsbook>,
    palette: ChartPalette,
    consensusLabel: String,
): List<ChartSeries> = buildList {
    byBook.forEach { (book, points) ->
        if (book !in hiddenBooks) {
            add(ChartSeries(book.apiKey, book.displayName, palette.colorFor(book), points.map { it?.probability?.value?.toFloat() }))
        }
    }
    add(ChartSeries(CONSENSUS_KEY, consensusLabel, palette.consensus, consensus.map { it?.value?.toFloat() }, emphasized = true))
}

@Composable
private fun Swatch(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).background(color, CircleShape))
}

/** Legend doubles as the series toggle. Consensus is always on — it's the headline number. */
@Composable
private fun Legend(
    books: Set<Sportsbook>,
    hidden: Set<Sportsbook>,
    palette: ChartPalette,
    consensusLabel: String,
    onToggle: (Sportsbook) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.width(16.dp).height(3.dp).background(palette.consensus))
            Text(consensusLabel, style = MaterialTheme.typography.labelLarge)
        }
        books.forEach { book ->
            FilterChip(
                selected = book !in hidden,
                onClick = { onToggle(book) },
                label = { Text(book.displayName) },
                leadingIcon = { Swatch(palette.colorFor(book)) },
            )
        }
    }
}

/**
 * The same numbers as the chart, as a table: exact values for people who want them,
 * and an accessible alternative for anyone who can't distinguish the line colours.
 */
@Composable
private fun PriceTable(timeline: OddsTimeline, index: Int, palette: ChartPalette, consensusLabel: String) {
    val none = stringResource(R.string.player_table_none)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider()
        TableRow(
            swatch = null,
            cells = listOf(
                stringResource(R.string.player_table_book),
                stringResource(R.string.player_table_yes),
                stringResource(R.string.player_table_no),
                stringResource(R.string.player_table_fair),
            ),
            header = true,
        )
        timeline.byBook.forEach { (book, points) ->
            val point = points.getOrNull(index)
            TableRow(
                swatch = palette.colorFor(book),
                cells = if (point == null) {
                    listOf(book.displayName, stringResource(R.string.player_not_offered), none, none)
                } else {
                    listOf(book.displayName, point.yes.toString(), point.no?.toString() ?: none, point.probability.formatPercent())
                },
            )
        }
        TableRow(
            swatch = palette.consensus,
            cells = listOf(consensusLabel, none, none, timeline.consensus.getOrNull(index)?.formatPercent() ?: none),
            header = true,
        )
    }
}

@Composable
private fun TableRow(swatch: Color?, cells: List<String>, header: Boolean = false) {
    val style = if (header) {
        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    } else {
        MaterialTheme.typography.bodyMedium
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1.4f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (swatch != null) Swatch(swatch)
            Text(cells[0], style = style, maxLines = 1)
        }
        cells.drop(1).forEach { cell ->
            Text(cell, style = style, textAlign = TextAlign.End, modifier = Modifier.weight(1f), maxLines = 1)
        }
    }
}
