package com.tomakethecut.core.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A word-sized trend line for list rows. Fixed 0..1 y-range on purpose: auto-scaling
 * would make a 2-point wobble look as dramatic as a 60-point collapse.
 */
@Composable
fun Sparkline(
    values: List<Float?>,
    color: Color,
    referenceColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = 72.dp, height = 28.dp)) {
        val g = ChartGeometry(0f, 2.dp.toPx(), size.width, size.height - 2.dp.toPx(), values.size)
        drawLine(referenceColor, Offset(0f, g.yFor(0.5f)), Offset(size.width, g.yFor(0.5f)), strokeWidth = 1.dp.toPx())
        val path = Path()
        var penDown = false
        var last: Offset? = null
        values.forEachIndexed { i, v ->
            if (v == null) {
                penDown = false
            } else {
                val p = Offset(g.xFor(i), g.yFor(v))
                if (penDown) path.lineTo(p.x, p.y) else path.moveTo(p.x, p.y)
                penDown = true
                last = p
            }
        }
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        last?.let { drawCircle(color, radius = 2.5.dp.toPx(), center = it) }
    }
}
