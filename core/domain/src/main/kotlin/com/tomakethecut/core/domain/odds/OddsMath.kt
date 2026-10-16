package com.tomakethecut.core.domain.odds

import com.tomakethecut.core.model.AmericanOdds
import com.tomakethecut.core.model.Probability

/**
 * Pure functions for turning prices into probabilities.
 *
 * Kept as top-level functions in an `object` rather than a class with an interface:
 * this is deterministic math with a single correct implementation, so there's
 * nothing to substitute in tests and DI would be ceremony.
 */
object OddsMath {

    /**
     * Removes the bookmaker margin from a two-way market using the multiplicative
     * ("proportional") method: p_yes / (p_yes + p_no).
     *
     * If only the Yes side is offered we fall back to its implied probability, which
     * slightly *overstates* the chance because the vig is still baked in. That's a
     * deliberate, documented approximation rather than dropping the data point.
     */
    fun noVigProbability(yes: AmericanOdds, no: AmericanOdds?): Probability {
        val pYes = yes.impliedProbability
        if (no == null) return Probability.clamped(pYes)
        val pNo = no.impliedProbability
        return Probability.clamped(pYes / (pYes + pNo))
    }

    /** The bookmaker's margin in percent, e.g. 4.5 for a -120/+100 style market. */
    fun overroundPercent(yes: AmericanOdds, no: AmericanOdds): Double =
        (yes.impliedProbability + no.impliedProbability - 1.0) * 100

    /** Simple average of the books' fair probabilities; null when nobody is pricing it. */
    fun consensus(probabilities: List<Probability>): Probability? =
        if (probabilities.isEmpty()) null else Probability.clamped(probabilities.map { it.value }.average())
}
