package com.tomakethecut.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColors = lightColorScheme(
    primary = Fairway40,
    onPrimary = Neutral99,
    primaryContainer = Fairway90,
    onPrimaryContainer = Fairway10,
    secondary = Sand40,
    secondaryContainer = Sand90,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    surfaceContainer = Neutral95,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    error = Red40,
)

private val DarkColors = darkColorScheme(
    primary = Fairway80,
    onPrimary = Fairway20,
    primaryContainer = Fairway30,
    onPrimaryContainer = Fairway90,
    secondary = Sand80,
    background = DarkSurface,
    onBackground = Neutral90,
    surface = DarkSurface,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceContainer = Neutral20,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant30,
    error = Red80,
)

/**
 * We deliberately skip Material You dynamic colour: the chart palette is tuned against
 * fixed surfaces, and a wallpaper-derived surface could break the contrast we validated.
 */
@Composable
fun ToMakeTheCutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val chart = ChartPalette(
        series = if (darkTheme) SeriesDark else SeriesLight,
        consensus = colors.onSurface,
        grid = colors.outlineVariant,
        axisText = colors.onSurfaceVariant,
        surface = colors.surface,
    )
    CompositionLocalProvider(LocalChartPalette provides chart) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
