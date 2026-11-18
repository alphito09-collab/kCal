package com.alphito.kcal.feature.progress

import com.alphito.kcal.core.model.Gender
import com.alphito.kcal.core.model.StepsDay
import com.alphito.kcal.core.model.UserProfile
import com.alphito.kcal.core.model.WeightEntry
import com.alphito.kcal.feature.progress.domain.ProgressCalculator
import com.alphito.kcal.feature.progress.domain.ProgressPeriod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ProgressCalculatorTest {
    private val today = LocalDate.of(2026, 8, 31)
    private val profile = UserProfile("A", 24, Gender.Female, 165, 68.0)

    @Test fun `six weekly buckets ending today`() {
        val ranges = ProgressCalculator.ranges(ProgressPeriod.Weekly, today)
        assertEquals(6, ranges.size)
        assertEquals(today, ranges.last().second)
        assertEquals(today.minusDays(6), ranges.last().first)
    }

    @Test fun `weight change compares with previous bucket`() {
        val weights = listOf(WeightEntry(today.minusWeeks(1), 69.0), WeightEntry(today, 68.4))
        val stats = ProgressCalculator.stats(ProgressPeriod.Weekly, today, profile, weights, emptyList(), emptyList())
        assertEquals(-0.6, stats.last().weightChange!!, 0.001)
    }

    @Test fun `burned adds tdee and steps`() {
        val stats = ProgressCalculator.stats(
            ProgressPeriod.Weekly, today, profile, emptyList(), listOf(StepsDay(today, 2_000)), emptyList(),
        )
        assertEquals(2060 * 7 + 100, stats.last().burnedKcal)
    }
}
