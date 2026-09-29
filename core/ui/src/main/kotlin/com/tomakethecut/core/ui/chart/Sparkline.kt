package com.tomakethecut.core.ui.chart

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A word-sized trend line for list rows. Fixed 0..1 y-range on purpose: auto-scaling
 * would make a 2-point wobble look as dramatic as a 60-point collapse.
 *
 * The path is built in `drawWithCache`, so scrolling a list of sparklines re-uses each row's
 * path instead of rebuilding it on every frame.
 */
@Composable
fun Sparkline(
    values: List<Float?>,
    color: Color,
    referenceColor: Color,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier = modifier
            .size(width = 72.dp, height = 28.dp)
            .drawWithCache {
                val g = ChartGeometry(0f, 2.dp.toPx(), size.width, size.height - 2.dp.toPx(), values.size)
                val path = linePath(g, values)
                val lastIndex = values.indexOfLast { it != null }
                val last = if (lastIndex >= 0) Offset(g.xFor(lastIndex), g.yFor(values[lastIndex]!!)) else null
                val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind {
                    drawLine(referenceColor, Offset(0f, g.yFor(0.5f)), Offset(size.width, g.yFor(0.5f)), strokeWidth = 1.dp.toPx())
                    drawPath(path, color, style = stroke)
                    last?.let { drawCircle(color, radius = 2.5.dp.toPx(), center = it) }
                }
            },
    )
}
