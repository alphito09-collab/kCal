package com.alphito.kcal.core.model

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Eaten (logged) calories are kept apart from planned calories. */
class DayPlanTest {
    private val date = LocalDate.of(2026, 11, 25)

    private fun item(id: Long, type: MealType, kcal: Int, servings: Double = 1.0) =
        PlannedItem(id, date, type, 0, Food("f$id", "F$id", kcal, FoodRole.Main, persistentSetOf(type), persistentSetOf()), servings)

    private fun plan(checked: Set<MealType> = emptySet(), lunchServings: Double = 1.0) = DayPlan(
        date, targetKcal = 1_710,
        slots = listOf(
            MealType.Breakfast to item(1, MealType.Breakfast, 450),
            MealType.Lunch to item(2, MealType.Lunch, 600, lunchServings),
            MealType.Snack to item(3, MealType.Snack, 200),
            MealType.Dinner to item(4, MealType.Dinner, 546),
        ).map { (type, it) -> MealSlot(date, type, persistentListOf(it), type in checked) }.toImmutableList(),
    )

    @Test fun `nothing logged means zero eaten even though the plan is full`() {
        val day = plan()
        assertEquals(1_796, day.plannedKcal)
        assertEquals(0, day.eatenKcal)
    }

    @Test fun `logging and removing a meal changes eaten only`() {
        assertEquals(450, plan(checked = setOf(MealType.Breakfast)).eatenKcal)
        assertEquals(1_050, plan(checked = setOf(MealType.Breakfast, MealType.Lunch)).eatenKcal)
        assertEquals(450, plan(checked = setOf(MealType.Breakfast)).eatenKcal) // lunch unticked again
        assertEquals(1_796, plan(checked = setOf(MealType.Breakfast)).plannedKcal)
    }

    @Test fun `a changed quantity of a logged meal changes eaten`() {
        assertEquals(900, plan(checked = setOf(MealType.Lunch), lunchServings = 1.5).eatenKcal)
        assertEquals(0, plan(lunchServings = 1.5).eatenKcal) // not logged: still nothing eaten
    }
}
