package com.alphito.kcal.feature.progress

import com.alphito.kcal.core.model.DayPlan
import com.alphito.kcal.core.model.Food
import com.alphito.kcal.core.model.FoodRole
import com.alphito.kcal.core.model.Gender
import com.alphito.kcal.core.model.MealSlot
import com.alphito.kcal.core.model.MealType
import com.alphito.kcal.core.model.PlannedItem
import com.alphito.kcal.core.model.StepsDay
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import com.alphito.kcal.core.model.UserProfile
import com.alphito.kcal.core.model.WeightEntry
import com.alphito.kcal.feature.progress.domain.ProgressCalculator
import com.alphito.kcal.feature.progress.domain.ProgressPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.Month

class ProgressCalculatorTest {
    private val today = LocalDate.of(2026, 11, 25)
    private val profile = UserProfile("A", 24, Gender.Female, 165, 68.0)

    private fun weekly(start: LocalDate, weights: List<WeightEntry> = emptyList(), steps: List<StepsDay> = emptyList()) =
        ProgressCalculator.stats(ProgressPeriod.Weekly, start, today, profile, weights, steps, emptyList())

    // ---------------------------------------------------------------- Weekly

    @Test fun `new user has only week 1 and it is just today`() {
        val stats = weekly(start = today, steps = listOf(StepsDay(today, 2_000), StepsDay(today.minusDays(3), 9_000)))
        assertEquals(1, stats.size)
        assertEquals(today, stats[0].start)
        assertEquals(today, stats[0].end)
        assertEquals(100, stats[0].burnedKcal) // only today's recorded steps, not the earlier row, no TDEE
        assertEquals(0..5, ProgressCalculator.window(ProgressCalculator.defaultWindowEnd(0)))
    }

    @Test fun `day 10 has a full week 1 a partial week 2 and nothing after`() {
        val start = today.minusDays(9)
        val stats = weekly(start)
        assertEquals(2, stats.size) // Wk 3–6 haven't started: no stats, no points
        assertEquals(start, stats[0].start)
        assertEquals(start.plusDays(6), stats[0].end)
        assertEquals(0, stats[0].burnedKcal) // no recorded activity → nothing burned
        assertEquals(start.plusDays(7), stats[1].start)
        assertEquals(today, stats[1].end)
        assertEquals(0, stats[1].burnedKcal)
        assertEquals(0..5, ProgressCalculator.window(ProgressCalculator.defaultWindowEnd(1)))
    }

    @Test fun `weeks are rolling 7-day blocks from the start date`() {
        val start = LocalDate.of(2026, 11, 4) // a Wednesday
        assertEquals(start to start.plusDays(6), ProgressCalculator.bounds(ProgressPeriod.Weekly, start, 0))
        assertEquals(start.plusDays(14) to start.plusDays(20), ProgressCalculator.bounds(ProgressPeriod.Weekly, start, 2))
    }

    @Test fun `window slides after week 6 so the current week is last`() {
        val start = today.minusWeeks(6) // today is the first day of week 7
        val current = ProgressCalculator.currentIndex(ProgressPeriod.Weekly, start, today)
        assertEquals(6, current)
        assertEquals(7, weekly(start).size)
        assertEquals(1..6, ProgressCalculator.window(ProgressCalculator.defaultWindowEnd(current))) // Wk 2 … Wk 7
        assertEquals(0..5, ProgressCalculator.window(5)) // paging back
    }

    @Test fun `days before the start date are never counted`() {
        val start = today.minusDays(2)
        val stats = weekly(start, steps = listOf(StepsDay(start.minusDays(1), 20_000), StepsDay(start, 1_000)))
        assertEquals(50, stats.single().burnedKcal)
    }

    // ---------------------------------------------------------------- Monthly

    @Test fun `monthly starts at the start month and counts from the start date`() {
        val start = LocalDate.of(2026, 9, 20)
        val stats = ProgressCalculator.stats(ProgressPeriod.Monthly, start, today, profile, emptyList(), emptyList(), emptyList())
        assertEquals(3, stats.size) // Sep, Oct, Nov
        assertEquals(start, stats[0].start)
        assertEquals(LocalDate.of(2026, 9, 30), stats[0].end)
        assertEquals(0, stats[0].burnedKcal)
        assertEquals(0, stats[1].burnedKcal)
        assertEquals(LocalDate.of(2026, 11, 1), stats[2].start)
        assertEquals(today, stats[2].end)
        assertEquals(0, stats[2].burnedKcal)
    }

    @Test fun `upcoming months keep labels after the start month`() {
        val start = LocalDate.of(2026, 11, 3)
        val months = (0..5).map { ProgressCalculator.bounds(ProgressPeriod.Monthly, start, it).first.month }
        assertEquals(listOf(Month.NOVEMBER, Month.DECEMBER, Month.JANUARY, Month.FEBRUARY, Month.MARCH, Month.APRIL), months)
        assertEquals(0, ProgressCalculator.currentIndex(ProgressPeriod.Monthly, start, today))
    }

    @Test fun `monthly window slides after six months`() {
        val start = LocalDate.of(2026, 4, 15)
        val current = ProgressCalculator.currentIndex(ProgressPeriod.Monthly, start, today)
        assertEquals(7, current) // Apr … Nov
        assertEquals(2..7, ProgressCalculator.window(ProgressCalculator.defaultWindowEnd(current)))
    }

    // ---------------------------------------------------------------- Weight

    @Test fun `week 1 compares against the starting weight`() {
        val start = today.minusDays(9)
        val weights = listOf(WeightEntry(start, 70.0), WeightEntry(start.plusDays(3), 69.4))
        val wk1 = weekly(start, weights)[0]
        assertEquals(69.4, wk1.weightKg!!, 1e-9)
        assertEquals(-0.6, wk1.weightChange!!, 1e-9)
    }

    @Test fun `only the starting weigh-in means no change badge`() {
        val wk1 = weekly(today, listOf(WeightEntry(today, 70.0)))[0]
        assertEquals(70.0, wk1.weightKg!!, 1e-9)
        assertNull(wk1.weightChange)
    }

    @Test fun `later weeks compare against the previous week`() {
        val start = today.minusDays(20) // weeks 1–3
        val weights = listOf(
            WeightEntry(start, 70.0),
            WeightEntry(start.plusDays(5), 69.5),
            WeightEntry(start.plusDays(16), 68.8), // week 3
        )
        val stats = weekly(start, weights)
        assertEquals(69.5, stats[1].weightKg!!, 1e-9) // week 2 carries week 1's weight forward
        assertNull(stats[1].weightChange) // no weigh-in of its own
        assertEquals(-0.7, stats[2].weightChange!!, 1e-9) // 68.8 vs week 2's 69.5
    }

    @Test fun `without any weigh-in the profile weight is used`() {
        val wk1 = weekly(today)[0]
        assertEquals(68.0, wk1.weightKg!!, 1e-9)
        assertNull(wk1.weightChange)
    }

    @Test fun `first month compares against the starting weight`() {
        val start = LocalDate.of(2026, 10, 10)
        val weights = listOf(WeightEntry(start, 72.0), WeightEntry(LocalDate.of(2026, 10, 30), 70.4), WeightEntry(LocalDate.of(2026, 11, 20), 69.9))
        val stats = ProgressCalculator.stats(ProgressPeriod.Monthly, start, today, profile, weights, emptyList(), emptyList())
        assertEquals(-1.6, stats[0].weightChange!!, 1e-9)
        assertEquals(-0.5, stats[1].weightChange!!, 1e-9)
    }

    @Test fun `a start date in the future yields nothing`() {
        assertEquals(emptyList<Any>(), weekly(today.plusDays(1)))
    }

    // ---------------------------------------------------------------- Actual burned / intake

    @Test fun `burned is only recorded step calories and each day counts once`() {
        val start = today.minusDays(2)
        val steps = listOf(StepsDay(start, 4_000), StepsDay(start.plusDays(1), 0), StepsDay(today, 6_000))
        assertEquals(200 + 0 + 300, weekly(start, steps = steps).single().burnedKcal)
    }

    private fun day(date: LocalDate, vararg meals: Pair<Int, Boolean>) = DayPlan(
        date = date, targetKcal = 1_710,
        slots = meals.mapIndexed { i, (kcal, checked) ->
            val type = MealType.entries[i]
            MealSlot(
                date, type,
                persistentListOf(PlannedItem(i.toLong(), date, type, 0, Food("f$i", "F$i", kcal, FoodRole.Main, persistentSetOf(type), persistentSetOf()))),
                checked,
            )
        }.toImmutableList(),
    )

    @Test fun `intake counts only logged meals never planned ones`() {
        val start = today.minusDays(2)
        val days = listOf(
            day(start, 450 to false, 600 to false, 200 to false, 546 to false), // planned 1,796, nothing logged
            day(start.plusDays(1), 450 to true, 600 to false, 200 to true, 546 to false),
            day(today, 450 to true, 600 to true, 200 to true, 546 to true),
        )
        val stats = ProgressCalculator.stats(ProgressPeriod.Weekly, start, today, profile, emptyList(), emptyList(), days)
        assertEquals(0 + 650 + 1_796, stats.single().intakeKcal)
    }

    @Test fun `no meals logged means zero intake for the week and the month`() {
        val start = today.minusDays(9)
        val days = (0..9).map { day(start.plusDays(it.toLong()), 500 to false, 700 to false) }
        val weeklyStats = ProgressCalculator.stats(ProgressPeriod.Weekly, start, today, profile, emptyList(), emptyList(), days)
        val monthlyStats = ProgressCalculator.stats(ProgressPeriod.Monthly, start, today, profile, emptyList(), emptyList(), days)
        assertEquals(listOf(0, 0), weeklyStats.map { it.intakeKcal })
        assertEquals(listOf(0), monthlyStats.map { it.intakeKcal })
        assertEquals(listOf(0, 0), weeklyStats.map { it.burnedKcal })
    }

    @Test fun `weekly and monthly intake are sums of the logged days in each period`() {
        val start = LocalDate.of(2026, 10, 30)
        val days = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }
            .map { day(it, 300 to true, 500 to false) }.toList() // 300 kcal logged every day
        val weeks = ProgressCalculator.stats(ProgressPeriod.Weekly, start, today, profile, emptyList(), emptyList(), days)
        assertEquals(listOf(2_100, 2_100, 2_100, 1_800), weeks.map { it.intakeKcal }) // Oct 30 … Nov 25 = 27 days
        val months = ProgressCalculator.stats(ProgressPeriod.Monthly, start, today, profile, emptyList(), emptyList(), days)
        assertEquals(listOf(600, 7_500), months.map { it.intakeKcal }) // Oct 30–31, Nov 1–25
    }
}
