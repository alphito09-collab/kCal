package com.alphito.kcal.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {
    private val today = 20_000L
    private val onboarded = today - 30

    @Test fun `new user has no streak`() {
        assertEquals(0, Streak.count(emptyList(), today, startDay = today))
    }

    @Test fun `one fully checked day is a streak of one`() {
        assertEquals(1, Streak.count(listOf(today), today, onboarded))
        assertEquals(1, Streak.count(listOf(today - 1), today, onboarded))
    }

    private val meals = listOf("Breakfast", "Lunch", "Snack", "Dinner")
    private fun planned(vararg days: Long) = days.flatMap { d -> meals.map { d to it } }

    @Test fun `a day is complete only when every planned meal is checked`() {
        val planned = planned(today - 1, today)
        val checked = meals.map { (today - 1) to it } + listOf(today to "Breakfast", today to "Lunch")
        assertEquals(setOf(today - 1), Streak.completedDays(planned, checked))
    }

    @Test fun `a partially checked day doesn't count`() {
        val checked = listOf(today to "Breakfast", today to "Lunch", today to "Snack")
        val completed = Streak.completedDays(planned(today), checked)
        assertEquals(0, Streak.count(completed, today, onboarded))
    }

    @Test fun `checks without a planned meal don't complete a day`() {
        // Seeded checks on days with no plan must not create a streak.
        assertEquals(emptySet<Long>(), Streak.completedDays(emptyList(), meals.map { (today - 1) to it }))
    }

    @Test fun `a day with fewer planned meals is complete once those are checked`() {
        val planned = listOf(today to "Lunch", today to "Dinner")
        assertEquals(setOf(today), Streak.completedDays(planned, planned))
    }

    @Test fun `consecutive completed days add up`() {
        assertEquals(3, Streak.count(listOf(today, today - 1, today - 2), today, onboarded))
    }

    @Test fun `a gap resets the streak`() {
        assertEquals(0, Streak.count(listOf(today - 2, today - 3), today, onboarded))
        assertEquals(1, Streak.count(listOf(today, today - 2, today - 3), today, onboarded))
    }

    @Test fun `unfinished today keeps yesterday's streak`() {
        assertEquals(2, Streak.count(listOf(today - 1, today - 2), today, onboarded))
    }

    @Test fun `days before onboarding never count`() {
        // e.g. meals checked by debug sample data before the user finished onboarding
        assertEquals(0, Streak.count(listOf(today - 1, today - 2), today, startDay = today))
        assertEquals(1, Streak.count(listOf(today, today - 1, today - 2), today, startDay = today))
    }

    @Test fun `no onboarding day means no streak`() {
        assertEquals(0, Streak.count(listOf(today, today - 1), today, startDay = null))
    }

    @Test fun `future days are ignored`() {
        assertEquals(1, Streak.count(listOf(today + 1, today), today, onboarded))
    }
}
