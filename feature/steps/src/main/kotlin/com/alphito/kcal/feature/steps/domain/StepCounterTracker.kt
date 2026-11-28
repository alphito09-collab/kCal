package com.alphito.kcal.feature.steps.domain

import kotlin.math.roundToLong

/**
 * Turns raw step-sensor events into steps to save. Pure (no Android) so the rules are unit-tested.
 *
 * Both native sensors run during a tracking session and the session total is the **larger** of the two:
 * - `TYPE_STEP_DETECTOR` fires per step with low latency, so every step shows up immediately.
 * - `TYPE_STEP_COUNTER` (cumulative since boot) can be seconds late and, on many chips, ignores
 *   short bursts – but it also counts warm-up steps the detector may miss.
 *
 * Taking the max (never the sum) means a step is never counted twice, the total never goes down
 * when the counter disagrees, and it catches up when the counter is ahead. Every increase is
 * returned for saving immediately.
 *
 * @param restored the last counter reading of an earlier session on this boot and day, plus how far
 *   the detector was ahead of the counter then. Steps walked while the service wasn't running are
 *   recovered from it without counting that lead twice.
 */
class StepCounterTracker(restored: Restored? = null) {

    data class Restored(val counterValue: Float, val detectorLead: Int)

    private var restored = restored
    private var sessionBase: Float? = null
    private var counterSteps = 0
    private var detectorSteps = 0
    private var credited = 0

    /** Last counter reading; everything up to it is saved. */
    var lastCounterValue: Float? = null
        private set

    /** Steps saved beyond what the counter has seen (detector ahead). Kept with the baseline. */
    val detectorLead: Int get() = (credited - counterSteps).coerceAtLeast(0)

    /** A `TYPE_STEP_COUNTER` reading. Returns the steps to save now. */
    fun onCounter(value: Float): Int {
        var gap = 0
        val base = sessionBase
        if (base == null || value < base) {
            // First reading of the session: it's the baseline. If an earlier session on this boot
            // left off at a lower value, the difference was walked while nothing was listening.
            restored?.let { r ->
                if (base == null && value >= r.counterValue) {
                    gap = ((value - r.counterValue).roundToLong().toInt() - r.detectorLead).coerceAtLeast(0)
                }
            }
            restored = null
            // Steps before a late first reading are still covered by the detector (max below).
            // On a counter reset, re-base so the counter steps so far carry on instead of going negative.
            sessionBase = value - counterSteps
        } else {
            counterSteps = (value - base).roundToLong().toInt()
        }
        lastCounterValue = value
        return gap + advance()
    }

    /** `TYPE_STEP_DETECTOR` events ([count] steps). Returns the steps to save now. */
    fun onDetector(count: Int): Int {
        if (count <= 0) return 0
        detectorSteps += count
        return advance()
    }

    private fun advance(): Int {
        val total = maxOf(counterSteps, detectorSteps)
        val delta = total - credited
        credited = total
        return delta.coerceAtLeast(0)
    }
}
