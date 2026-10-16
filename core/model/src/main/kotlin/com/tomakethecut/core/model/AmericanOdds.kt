package com.tomakethecut.core.model

import kotlin.math.abs

/**
 * A price in American ("moneyline") format, e.g. `-450` or `+320`.
 *
 * Modelled as a value class rather than a raw [Int] so the compiler stops us from
 * mixing up "a price" and "a percentage" — and so the invariant (no prices between
 * -100 and +100 exclusive) is enforced in exactly one place.
 */
@JvmInline
value class AmericanOdds(val value: Int) {
    init {
        require(value <= -100 || value >= 100) { "American odds must be <= -100 or >= +100, was $value" }
    }

    /** Break-even probability including the bookmaker's margin ("vig"). */
    val impliedProbability: Double
        get() = if (value < 0) {
            abs(value).toDouble() / (abs(value) + 100)
        } else {
            100.0 / (value + 100)
        }

    /** Profit on a 100-unit stake. Larger is always better for the bettor. */
    val profitPer100: Double
        get() = if (value < 0) 100.0 * 100 / abs(value) else value.toDouble()

    override fun toString(): String = if (value > 0) "+$value" else value.toString()
}
