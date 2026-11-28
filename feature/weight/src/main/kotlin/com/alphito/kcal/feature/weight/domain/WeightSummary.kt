package com.alphito.kcal.feature.weight.domain

import com.alphito.kcal.core.model.WeightEntry

/** [start] is the first weigh-in (logged at onboarding), the baseline for goal progress. */
data class WeightSummary(val current: Double?, val previous: Double?, val recent: List<WeightEntry>, val start: Double? = null) {
    val change: Double? get() = if (current != null && previous != null) current - previous else null
}

/** Latest two weigh-ins drive the progress card; the last four populate the history list. */
object SummarizeWeights {
    operator fun invoke(entries: List<WeightEntry>): WeightSummary {
        val sorted = entries.sortedBy { it.date }
        return WeightSummary(
            current = sorted.lastOrNull()?.weightKg,
            previous = sorted.getOrNull(sorted.lastIndex - 1)?.weightKg,
            recent = sorted.takeLast(4),
            start = sorted.firstOrNull()?.weightKg,
        )
    }
}
