package com.tomakethecut.core.ui.chart

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tomakethecut.core.ui.theme.ToMakeTheCutChartTheme

@Immutable
data class ChartSeries(
    val key: String,
    val label: String,
    val color: Color,
    /** Probabilities in 0..1, index-aligned with the x-axis. `null` draws a gap. */
    val values: List<Float?>,
    val emphasized: Boolean = false,
)

private val PadLeft = 40.dp
private val PadRight = 12.dp
private val PadTop = 8.dp
private val PadBottom = 24.dp
private val GridTicks = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

/**
 * An interactive multi-series probability chart drawn directly on a Canvas.
 *
 * Why hand-rolled rather than a charting library? Our needs are narrow (one y-scale,
 * 0–100%, gaps, a scrubbable crosshair) and a Canvas implementation is ~200 lines we
 * fully control, with no dependency whose Compose-version compatibility we'd have to
 * track. The trade-off flips the moment we need zoom, many chart types or animation
 * polish — then a library like Vico earns its keep.
 *
 * The chart is *stateless*: the selected index is hoisted to the caller, so the
 * readout panel, the price table and the chart all stay in sync from one source of truth.
 *
 * Performance: drawing happens on the main thread, and scrubbing can redraw at 60–120 fps.
 * So the chart is split into two layers:
 *  - a **base layer** (grid, labels, series paths) built with `drawWithCache` — paths and
 *    text layouts are computed once per size/data change, never while you drag;
 *  - a thin **overlay** that only draws the crosshair and selection markers.
 * Axis labels are measured once with `remember`, not on every draw.
 */
@Composable
fun OddsLineChart(
    series: List<ChartSeries>,
    xLabels: List<String>,
    selectedIndex: Int?,
    onSelectedIndexChange: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 240.dp,
) {
    val palette = ToMakeTheCutChartTheme.palette
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = palette.axisText)
    val onSelect by rememberUpdatedState(onSelectedIndexChange)
    val pointCount = xLabels.size

    val gridLabels = remember(textMeasurer, axisStyle) {
        GridTicks.map { tick -> textMeasurer.measure("${(tick * 100).toInt()}%", axisStyle) }
    }
    val firstLabel = remember(textMeasurer, axisStyle, xLabels) {
        xLabels.firstOrNull()?.let { textMeasurer.measure(it, axisStyle) }
    }
    val lastLabel = remember(textMeasurer, axisStyle, xLabels) {
        xLabels.takeIf { it.size > 1 }?.last()?.let { textMeasurer.measure(it, axisStyle) }
    }
    // Emphasised (consensus) series last so it sits on top.
    val drawOrder = remember(series) { series.sortedBy { it.emphasized } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(pointCount) {
                detectTapGestures { offset ->
                    geometry(size.width.toFloat(), size.height.toFloat(), pointCount, this).indexAt(offset.x)?.let(onSelect)
                }
            }
            .pointerInput(pointCount) {
                val g = { geometry(size.width.toFloat(), size.height.toFloat(), pointCount, this) }
                detectHorizontalDragGestures(
                    onDragStart = { offset -> g().indexAt(offset.x)?.let(onSelect) },
                ) { change, _ ->
                    change.consume()
                    g().indexAt(change.position.x)?.let(onSelect)
                }
            },
    ) {
        // Base layer: does not read selectedIndex, so its cache survives scrubbing.
        Spacer(
            Modifier
                .matchParentSize()
                .drawWithCache {
                    val g = geometry(size.width, size.height, pointCount, this)
                    val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                    val paths = drawOrder.map { s -> s to linePath(g, s.values) }
                    val thin = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val thick = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    onDrawBehind {
                        drawGrid(g, palette.grid, gridLabels, dash)
                        drawXLabels(g, firstLabel, lastLabel)
                        paths.forEach { (s, path) ->
                            drawPath(path, s.color, style = if (s.emphasized) thick else thin)
                            drawIsolatedPoints(g, s)
                        }
                    }
                },
        )
        // Overlay: the only thing redrawn while dragging.
        Spacer(
            Modifier
                .matchParentSize()
                .drawBehind {
                    val index = selectedIndex?.takeIf { it in 0 until pointCount } ?: return@drawBehind
                    val g = geometry(size.width, size.height, pointCount, this)
                    val x = g.xFor(index)
                    drawLine(palette.axisText.copy(alpha = 0.6f), Offset(x, g.top), Offset(x, g.bottom), strokeWidth = 1.dp.toPx())
                    drawOrder.forEach { s ->
                        s.values.getOrNull(index)?.let { v ->
                            val center = Offset(x, g.yFor(v))
                            drawCircle(palette.surface, radius = 6.dp.toPx(), center = center) // 2dp surface ring
                            drawCircle(s.color, radius = 4.dp.toPx(), center = center)
                        }
                    }
                },
        )
    }
}

private fun geometry(width: Float, height: Float, pointCount: Int, density: Density) = with(density) {
    ChartGeometry(
        left = PadLeft.toPx(),
        top = PadTop.toPx(),
        right = width - PadRight.toPx(),
        bottom = height - PadBottom.toPx(),
        pointCount = pointCount,
    )
}

/** A path with gaps: `null` lifts the pen so missing markets aren't bridged with invented data. */
internal fun linePath(g: ChartGeometry, values: List<Float?>): Path {
    val path = Path()
    var penDown = false
    values.forEachIndexed { i, v ->
        if (v == null) {
            penDown = false
        } else {
            val x = g.xFor(i)
            val y = g.yFor(v)
            if (penDown) path.lineTo(x, y) else path.moveTo(x, y)
            penDown = true
        }
    }
    return path
}

private fun DrawScope.drawGrid(g: ChartGeometry, color: Color, labels: List<TextLayoutResult>, dash: PathEffect) {
    GridTicks.forEachIndexed { i, tick ->
        val y = g.yFor(tick)
        drawLine(
            color = color,
            start = Offset(g.left, y),
            end = Offset(g.right, y),
            strokeWidth = 1.dp.toPx(),
            // 50% is the "coin flip" line — the one reference that means something here.
            pathEffect = if (tick == 0.5f) dash else null,
        )
        val label = labels[i]
        drawText(label, topLeft = Offset(g.left - label.size.width - 6.dp.toPx(), y - label.size.height / 2f))
    }
}

private fun DrawScope.drawXLabels(g: ChartGeometry, first: TextLayoutResult?, last: TextLayoutResult?) {
    // Only the ends: every label would collide on a phone. The readout names the selected point.
    first ?: return
    val y = g.bottom + 6.dp.toPx()
    if (last == null) {
        drawText(first, topLeft = Offset(g.xFor(0) - first.size.width / 2f, y))
        return
    }
    drawText(first, topLeft = Offset(g.left, y))
    if (first.size.width + last.size.width < g.width) {
        drawText(last, topLeft = Offset(g.right - last.size.width, y))
    }
}

/** A point with no neighbours would be invisible as a line — draw it as a dot. */
private fun DrawScope.drawIsolatedPoints(g: ChartGeometry, s: ChartSeries) {
    s.values.forEachIndexed { i, v ->
        if (v != null && s.values.getOrNull(i - 1) == null && s.values.getOrNull(i + 1) == null) {
            drawCircle(s.color, radius = 3.dp.toPx(), center = Offset(g.xFor(i), g.yFor(v)))
        }
    }
}
