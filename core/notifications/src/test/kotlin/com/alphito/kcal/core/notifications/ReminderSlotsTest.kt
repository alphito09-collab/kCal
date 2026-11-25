package com.alphito.kcal.core.notifications

import com.alphito.kcal.core.model.MealType
import com.alphito.kcal.core.model.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderSlotsTest {
    private val wednesday10 = LocalDateTime.of(2026, 11, 18, 10, 0)

    @Test fun `water fires at the next odd hour`() =
        assertEquals(LocalDateTime.of(2026, 11, 18, 11, 0), ReminderSlots.next(ReminderType.Water, wednesday10))

    @Test fun `water rolls to tomorrow morning after the last slot`() =
        assertEquals(LocalDateTime.of(2026, 11, 19, 9, 0), ReminderSlots.next(ReminderType.Water, wednesday10.withHour(22)))

    @Test fun `meals pick the next meal time`() =
        assertEquals(LocalDateTime.of(2026, 11, 18, 13, 0), ReminderSlots.next(ReminderType.Meals, wednesday10))

    @Test fun `weigh in is next monday 9am`() =
        assertEquals(LocalDateTime.of(2026, 11, 23, 9, 0), ReminderSlots.next(ReminderType.WeighIn, wednesday10))

    @Test fun `weigh in on monday after 9 moves a week`() =
        assertEquals(LocalDateTime.of(2026, 11, 30, 9, 0), ReminderSlots.next(ReminderType.WeighIn, LocalDateTime.of(2026, 11, 23, 9, 0)))

    @Test fun `meal type resolves from time`() = assertEquals(MealType.Lunch, ReminderSlots.mealAt(LocalTime.of(13, 0)))
}
