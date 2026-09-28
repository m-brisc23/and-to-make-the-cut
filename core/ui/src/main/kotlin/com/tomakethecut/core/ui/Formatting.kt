package com.tomakethecut.core.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Pure formatting helpers — no Compose, so they're covered by plain JVM tests. */
object Formatting {

    /** "+12 pts", "−3 pts", "±0 pts". Percentage *points*, not percent: 40%→50% is +10 pts. */
    fun changePoints(points: Double): String {
        val rounded = points.roundToInt()
        return when {
            rounded > 0 -> "+$rounded pts"
            rounded < 0 -> "−${abs(rounded)} pts"
            else -> "±0 pts"
        }
    }

    fun signed(value: Double, decimals: Int = 2): String {
        val text = String.format(Locale.US, "%.${decimals}f", abs(value))
        return if (value >= 0) "+$text" else "−$text"
    }

    fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

    fun dateRange(start: LocalDate, end: LocalDate): String {
        val month = DateTimeFormatter.ofPattern("MMM d", Locale.US)
        val day = DateTimeFormatter.ofPattern("d", Locale.US)
        val endText = if (start.month == end.month) end.format(day) else end.format(month)
        return "${start.format(month)}–$endText, ${end.year}"
    }

    fun shortDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
}
