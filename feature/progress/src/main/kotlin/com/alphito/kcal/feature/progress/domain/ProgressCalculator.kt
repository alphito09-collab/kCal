package com.alphito.kcal.feature.progress.domain

import com.alphito.kcal.core.model.DayPlan
import com.alphito.kcal.core.model.EnergyCalculator
import com.alphito.kcal.core.model.StepsDay
import com.alphito.kcal.core.model.UserProfile
import com.alphito.kcal.core.model.WeightEntry
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class ProgressPeriod { Weekly, Monthly }

data class PeriodStats(
    val start: LocalDate,
    val end: LocalDate,
    val weightKg: Double?,
    val weightChange: Double?,
    val burnedKcal: Int,
    val intakeKcal: Int,
)

/**
 * Buckets history into the last six weeks or months.
 * Burned = Σ (TDEE + step calories) for each elapsed day; intake = Σ checked-off meals.
 */
object ProgressCalculator {
    const val BUCKETS = 6

    fun ranges(period: ProgressPeriod, today: LocalDate): List<Pair<LocalDate, LocalDate>> = when (period) {
        ProgressPeriod.Weekly -> (BUCKETS - 1 downTo 0).map { back ->
            val end = today.minusWeeks(back.toLong())
            end.minusDays(6) to end
        }
        ProgressPeriod.Monthly -> (BUCKETS - 1 downTo 0).map { back ->
            val start = today.minusMonths(back.toLong()).withDayOfMonth(1)
            start to if (back == 0) today else start.plusMonths(1).minusDays(1)
        }
    }

    fun stats(
        period: ProgressPeriod,
        today: LocalDate,
        profile: UserProfile?,
        weights: List<WeightEntry>,
        steps: List<StepsDay>,
        days: List<DayPlan>,
    ): List<PeriodStats> {
        val sortedWeights = weights.sortedBy { it.date }
        val stepsByDay = steps.associate { it.date to it.steps }
        val eatenByDay = days.associate { it.date to it.eatenKcal }
        val tdee = profile?.let { EnergyCalculator.needs(it).tdee } ?: 0
        var previousWeight: Double? = null
        return ranges(period, today).map { (start, end) ->
            val weight = sortedWeights.lastOrNull { !it.date.isAfter(end) }?.weightKg
            val dayCount = ChronoUnit.DAYS.between(start, end).toInt() + 1
            val dates = (0 until dayCount).map { start.plusDays(it.toLong()) }
            val burned = dates.sumOf { tdee + EnergyCalculator.stepsToKcal(stepsByDay[it] ?: 0) }
            val intake = dates.sumOf { eatenByDay[it] ?: 0 }
            PeriodStats(
                start = start, end = end, weightKg = weight,
                weightChange = if (weight != null && previousWeight != null) weight - previousWeight!! else null,
                burnedKcal = burned, intakeKcal = intake,
            ).also { previousWeight = weight ?: previousWeight }
        }
    }
}
