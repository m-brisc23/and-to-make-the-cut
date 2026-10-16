package com.tomakethecut.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.tomakethecut.core.model.CutResult
import com.tomakethecut.core.model.Tour
import com.tomakethecut.core.ui.Formatting
import com.tomakethecut.core.ui.R
import kotlin.math.roundToInt

@Composable
private fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier, icon: (@Composable () -> Unit)? = null) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            icon?.invoke()
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun TourBadge(tour: Tour, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    when (tour) {
        Tour.PGA_TOUR -> Pill(tour.displayName, colors.primaryContainer, colors.onPrimaryContainer, modifier)
        Tour.KORN_FERRY_TOUR -> Pill(tour.displayName, colors.secondaryContainer, colors.onSecondaryContainer, modifier)
    }
}

/** Status is never colour-alone: every result carries an icon and a word. */
@Composable
fun CutResultBadge(result: CutResult, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val made = result == CutResult.MADE_CUT
    Pill(
        text = stringResource(if (made) R.string.core_ui_made_cut else R.string.core_ui_missed_cut),
        container = if (made) colors.primaryContainer else colors.errorContainer,
        content = if (made) colors.onPrimaryContainer else colors.onErrorContainer,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = if (made) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
            )
        },
    )
}

/** "▲ +12 pts" style movement indicator. Flat moves (< 1 pt) render neutral. */
@Composable
fun ChangeIndicator(points: Double?, modifier: Modifier = Modifier) {
    if (points == null) return
    val colors = MaterialTheme.colorScheme
    val rounded = points.roundToInt()
    val text = Formatting.changePoints(points)
    val description = when {
        rounded > 0 -> stringResource(R.string.core_ui_change_up, text)
        rounded < 0 -> stringResource(R.string.core_ui_change_down, text)
        else -> text
    }
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = when {
            rounded > 0 -> colors.primary
            rounded < 0 -> colors.error
            else -> colors.onSurfaceVariant
        }
        when {
            rounded > 0 -> Icon(Icons.Default.KeyboardArrowUp, null, tint = tint, modifier = Modifier.size(16.dp))
            rounded < 0 -> Icon(Icons.Default.KeyboardArrowDown, null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}
