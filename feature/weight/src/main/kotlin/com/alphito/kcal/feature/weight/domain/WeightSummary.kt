package com.alphito.kcal.feature.weight.domain

import com.alphito.kcal.core.model.WeightEntry

data class WeightSummary(val current: Double?, val previous: Double?, val recent: List<WeightEntry>) {
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
        )
    }
}
