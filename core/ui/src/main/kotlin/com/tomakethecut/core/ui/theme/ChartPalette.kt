package com.tomakethecut.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.tomakethecut.core.model.Sportsbook

/**
 * Colours for data visualisation, provided alongside (not inside) the Material scheme.
 *
 * Rule: colour follows the *entity*. DraftKings is always slot 1, whether or not the
 * other books are toggled off — so hiding a line never repaints the survivors.
 */
@Immutable
data class ChartPalette(
    val series: List<Color>,
    val consensus: Color,
    val grid: Color,
    val axisText: Color,
    val surface: Color,
) {
    fun colorFor(book: Sportsbook): Color = series[book.ordinal % series.size]
}

val LocalChartPalette = staticCompositionLocalOf<ChartPalette> { error("No ChartPalette provided") }

object ToMakeTheCutChartTheme {
    val palette: ChartPalette
        @Composable @ReadOnlyComposable
        get() = LocalChartPalette.current
}
