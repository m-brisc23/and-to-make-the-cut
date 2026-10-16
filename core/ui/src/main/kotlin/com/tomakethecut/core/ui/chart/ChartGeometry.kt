package com.tomakethecut.core.ui.chart

import kotlin.math.roundToInt

/**
 * Pure maths for mapping data to pixels and touches back to data. Kept free of Compose
 * so the hit-testing — the part most likely to have off-by-one bugs — is unit tested.
 */
data class ChartGeometry(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val pointCount: Int,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun xFor(index: Int): Float =
        if (pointCount <= 1) left + width / 2 else left + width * index / (pointCount - 1)

    /** [value] is a probability in 0..1; 1.0 is drawn at the top. */
    fun yFor(value: Float): Float = bottom - height * value.coerceIn(0f, 1f)

    /** The nearest data index to a touch at [x], clamped so dragging past the edges still works. */
    fun indexAt(x: Float): Int? {
        if (pointCount == 0) return null
        if (pointCount == 1) return 0
        val step = width / (pointCount - 1)
        return ((x - left) / step).roundToInt().coerceIn(0, pointCount - 1)
    }
}
