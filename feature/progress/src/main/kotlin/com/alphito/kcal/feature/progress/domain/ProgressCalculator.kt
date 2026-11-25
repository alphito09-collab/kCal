package com.alphito.kcal.feature.progress.domain

import com.alphito.kcal.core.model.DayPlan
import com.alphito.kcal.core.model.EnergyCalculator
import com.alphito.kcal.core.model.StepsDay
import com.alphito.kcal.core.model.UserProfile
import com.alphito.kcal.core.model.WeightEntry
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class ProgressPeriod { Weekly, Monthly }

/** One week / month since the user started tracking. Only started periods have stats. */
data class PeriodStats(
    /** 0-based: Week `index + 1`, or the `index`-th month after the start month. */
    val index: Int,
    /** First and last counted day: clamped to the start date and to today. */
    val start: LocalDate,
    val end: LocalDate,
    val weightKg: Double?,
    /** null when the period has no weigh-in of its own (nothing to compare). */
    val weightChange: Double?,
    val burnedKcal: Int,
    val intakeKcal: Int,
)

/**
 * Progress is measured from the tracking start date, never before it:
 *  - Weekly: rolling 7-day weeks, Week 1 = start date … start + 6 days.
 *  - Monthly: calendar months, the first one being the start date's month.
 * The current period only counts up to today. Burned = Σ (TDEE + step calories) over counted days;
 * intake = Σ checked-off meals.
 */
object ProgressCalculator {
    /** Slots visible on the chart at once. */
    const val SLOTS = 6

    /** Index of the period containing [today] (0 = the first week / month). */
    fun currentIndex(period: ProgressPeriod, startDate: LocalDate, today: LocalDate): Int = when (period) {
        ProgressPeriod.Weekly -> ChronoUnit.DAYS.between(startDate, today) / 7
        ProgressPeriod.Monthly -> ChronoUnit.MONTHS.between(YearMonth.from(startDate), YearMonth.from(today))
    }.toInt().coerceAtLeast(0)

    /** Last slot of the default window: Week 6 until the user is past it, then the current period. */
    fun defaultWindowEnd(currentIndex: Int): Int = maxOf(SLOTS - 1, currentIndex)

    /** The six period indices visible when the window ends at [windowEnd]. */
    fun window(windowEnd: Int): IntRange = (windowEnd - SLOTS + 1).coerceAtLeast(0)..maxOf(windowEnd, SLOTS - 1)

    /** Full calendar bounds of a period (the first month starts at the start date). */
    fun bounds(period: ProgressPeriod, startDate: LocalDate, index: Int): Pair<LocalDate, LocalDate> = when (period) {
        ProgressPeriod.Weekly -> startDate.plusWeeks(index.toLong()).let { it to it.plusDays(6) }
        ProgressPeriod.Monthly -> YearMonth.from(startDate).plusMonths(index.toLong()).let {
            maxOf(it.atDay(1), startDate) to it.atEndOfMonth()
        }
    }

    /** Stats for every started period, oldest first (index 0 … current). */
    fun stats(
        period: ProgressPeriod,
        startDate: LocalDate,
        today: LocalDate,
        profile: UserProfile?,
        weights: List<WeightEntry>,
        steps: List<StepsDay>,
        days: List<DayPlan>,
    ): List<PeriodStats> {
        if (today.isBefore(startDate)) return emptyList()
        val sortedWeights = weights.sortedBy { it.date }
        val first = sortedWeights.firstOrNull()
        // Starting weight: the onboarding weigh-in (the first entry), else the profile.
        val startingWeight = first?.weightKg ?: profile?.weightKg
        val stepsByDay = steps.associate { it.date to it.steps }
        val eatenByDay = days.associate { it.date to it.eatenKcal }
        val tdee = profile?.let { EnergyCalculator.needs(it).tdee } ?: 0

        var previousWeight = startingWeight
        return (0..currentIndex(period, startDate, today)).map { index ->
            val (from, to) = bounds(period, startDate, index)
            val end = minOf(to, today)
            val inPeriod = sortedWeights.filter { !it.date.isBefore(from) && !it.date.isAfter(end) }
            // Last weigh-in of the period, else carry the latest earlier one forward.
            val weight = inPeriod.lastOrNull()?.weightKg
                ?: sortedWeights.lastOrNull { it.date.isBefore(from) }?.weightKg
                ?: startingWeight
            // The starting weigh-in itself is the baseline, not a change.
            val ownWeighIn = inPeriod.any { it !== first }
            val change = if (ownWeighIn && weight != null && previousWeight != null) weight - previousWeight!! else null
            val counted = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
            PeriodStats(
                index = index, start = from, end = end, weightKg = weight, weightChange = change,
                burnedKcal = counted.sumOf { tdee + EnergyCalculator.stepsToKcal(stepsByDay[it] ?: 0) },
                intakeKcal = counted.sumOf { eatenByDay[it] ?: 0 },
            ).also { previousWeight = weight ?: previousWeight }
        }
    }
}
