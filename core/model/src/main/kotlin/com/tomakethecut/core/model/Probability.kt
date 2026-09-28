package com.tomakethecut.core.model

import kotlin.math.roundToInt

/** A probability in the closed range 0.0..1.0. */
@JvmInline
value class Probability(val value: Double) : Comparable<Probability> {
    init {
        require(value in 0.0..1.0) { "Probability must be within 0..1, was $value" }
    }

    val percent: Double get() = value * 100

    override fun compareTo(other: Probability): Int = value.compareTo(other.value)

    /** "83%" — whole percents are the right precision for a noisy betting market. */
    fun formatPercent(): String = "${percent.roundToInt()}%"

    companion object {
        fun clamped(value: Double) = Probability(value.coerceIn(0.0, 1.0))
    }
}
