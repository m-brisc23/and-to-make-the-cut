package com.tomakethecut.feature.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomakethecut.core.model.EventResult
import com.tomakethecut.core.model.Finish
import com.tomakethecut.core.model.PlayerSeasonStats
import com.tomakethecut.core.model.StrokesGained
import com.tomakethecut.core.ui.Formatting
import com.tomakethecut.core.ui.component.StatTile
import com.tomakethecut.core.ui.component.TourBadge
import com.tomakethecut.core.ui.theme.ToMakeTheCutChartTheme
import kotlin.math.abs
import kotlin.math.max

@Composable
internal fun SeasonChips(seasons: List<PlayerSeasonStats>, selected: Int?, onSelected: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        seasons.forEach { stats ->
            FilterChip(
                selected = stats.season == selected,
                onClick = { onSelected(stats.season) },
                // The tour is part of the label: "2025 · Korn Ferry Tour" is a different
                // competition, and its numbers aren't comparable one-to-one with PGA TOUR ones.
                label = { Text("${stats.season} · ${stats.tour.displayName}") },
            )
        }
    }
}

@Composable
internal fun StatsGrid(stats: PlayerSeasonStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TourBadge(stats.tour)
            stats.pointsRank?.let {
                Text(stringResource(R.string.player_rank, it, stats.pointsLabel), style = MaterialTheme.typography.labelLarge)
            }
        }
        TileRow(
            { StatTile(stringResource(R.string.player_events), stats.eventsPlayed.toString(), it) },
            {
                StatTile(
                    label = stringResource(R.string.player_cuts_made),
                    value = "${stats.cutsMade}/${stats.eventsPlayed}",
                    supporting = stats.cutsMadeRate.formatPercent(),
                    modifier = it,
                )
            },
        )
        TileRow(
            { StatTile(stringResource(R.string.player_wins), stats.wins.toString(), it) },
            { StatTile(stringResource(R.string.player_top10s), stats.top10s.toString(), it) },
        )
        TileRow(
            { StatTile(stringResource(R.string.player_scoring_avg), "%.2f".format(stats.scoringAverage), it) },
            {
                StatTile(
                    stringResource(R.string.player_driving_distance),
                    stringResource(R.string.player_yards, Formatting.oneDecimal(stats.drivingDistance)),
                    it,
                )
            },
        )
        TileRow(
            {
                StatTile(
                    stringResource(R.string.player_driving_accuracy),
                    stringResource(R.string.player_percent, Formatting.oneDecimal(stats.drivingAccuracyPct)),
                    it,
                )
            },
            {
                StatTile(
                    stringResource(R.string.player_gir),
                    stringResource(R.string.player_percent, Formatting.oneDecimal(stats.greensInRegulationPct)),
                    it,
                )
            },
        )
    }
}

@Composable
private fun TileRow(first: @Composable (Modifier) -> Unit, second: @Composable (Modifier) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        first(Modifier.weight(1f))
        second(Modifier.weight(1f))
    }
}

/**
 * Horizontal bars diverging from zero. One colour for every bar: direction (left/right of
 * the zero line) and the signed number carry the meaning, so we don't borrow a sportsbook
 * colour or a status colour for something that is neither.
 */
@Composable
internal fun StrokesGainedCard(sg: StrokesGained) {
    val rows = listOf(
        stringResource(R.string.player_sg_ott) to sg.offTheTee,
        stringResource(R.string.player_sg_app) to sg.approach,
        stringResource(R.string.player_sg_arg) to sg.aroundTheGreen,
        stringResource(R.string.player_sg_putt) to sg.putting,
        stringResource(R.string.player_sg_total) to sg.total,
    )
    // Shared scale across bars (at least ±1 stroke) so lengths are comparable.
    val scale = max(1.0, rows.maxOf { abs(it.second) })
    val barColor = MaterialTheme.colorScheme.primary
    val zeroColor = ToMakeTheCutChartTheme.palette.axisText

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.player_sg_title), style = MaterialTheme.typography.titleMedium)
            rows.forEachIndexed { i, (label, value) ->
                if (i == rows.lastIndex) HorizontalDivider()
                val signed = Formatting.signed(value)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = "$label $signed" },
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(104.dp))
                    Canvas(Modifier.weight(1f).height(14.dp)) {
                        val mid = size.width / 2
                        val length = (value / scale).toFloat().coerceIn(-1f, 1f) * mid
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(if (length >= 0) mid else mid + length, 0f),
                            size = Size(abs(length), size.height),
                            cornerRadius = CornerRadius(4.dp.toPx()),
                        )
                        drawLine(zeroColor, Offset(mid, 0f), Offset(mid, size.height), strokeWidth = 1.dp.toPx())
                    }
                    Text(
                        signed,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(56.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun RecentResultsCard(stats: PlayerSeasonStats) {
    if (stats.recentResults.isEmpty()) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.player_recent_results), style = MaterialTheme.typography.titleMedium)
            stats.recentResults.forEach { result -> ResultRow(result, showTour = result.tour != stats.tour) }
        }
    }
}

@Composable
private fun ResultRow(result: EventResult, showTour: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(result.tournamentName, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Formatting.shortDate(result.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (showTour) TourBadge(result.tour)
            }
        }
        val missed = result.finish == Finish.MissedCut
        Text(
            text = result.finish.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = if (missed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}
