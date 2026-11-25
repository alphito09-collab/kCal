package com.alphito.kcal.core.notifications

import com.alphito.kcal.core.model.MealType
import com.alphito.kcal.core.model.ReminderType
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/** When each reminder fires. Pure functions so the schedule is unit-tested. */
object ReminderSlots {
    /** Every two hours during the day. */
    val WATER_HOURS = (9..21 step 2).map { LocalTime.of(it, 0) }
    val MEAL_TIMES = linkedMapOf(
        MealType.Breakfast to LocalTime.of(8, 0),
        MealType.Lunch to LocalTime.of(13, 0),
        MealType.Snack to LocalTime.of(17, 0),
        MealType.Dinner to LocalTime.of(20, 0),
    )
    val WEIGH_IN_DAY: DayOfWeek = DayOfWeek.MONDAY
    val WEIGH_IN_TIME: LocalTime = LocalTime.of(9, 0)

    /** The next trigger strictly after [now]. */
    fun next(type: ReminderType, now: LocalDateTime): LocalDateTime = when (type) {
        ReminderType.Water -> nextDaily(WATER_HOURS, now)
        ReminderType.Meals -> nextDaily(MEAL_TIMES.values.toList(), now)
        ReminderType.WeighIn -> {
            val candidate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(WEIGH_IN_DAY)).atTime(WEIGH_IN_TIME)
            if (candidate.isAfter(now)) candidate else candidate.plusWeeks(1)
        }
    }

    /** Which meal a reminder at [time] is for. */
    fun mealAt(time: LocalTime): MealType =
        MEAL_TIMES.entries.minBy { kotlin.math.abs(it.value.toSecondOfDay() - time.toSecondOfDay()) }.key

    private fun nextDaily(times: List<LocalTime>, now: LocalDateTime): LocalDateTime {
        val today = times.map { now.toLocalDate().atTime(it) }.firstOrNull { it.isAfter(now) }
        return today ?: now.toLocalDate().plusDays(1).atTime(times.first())
    }
}
