package com.alphito.kcal.core.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.model.MealType
import com.alphito.kcal.core.model.ReminderType
import java.time.LocalTime

/** Posts one reminder (if still enabled) and schedules the next one. */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
    private val preferences: PreferencesRepository,
    private val notifications: KcalNotifications,
    private val scheduler: ReminderScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val type = inputData.getString(KEY_TYPE)?.let { n -> ReminderType.entries.firstOrNull { it.name == n } } ?: return Result.success()
        if (type !in preferences.snapshot().reminders) return Result.success()
        val ctx = applicationContext
        val (title, body) = when (type) {
            ReminderType.Water -> ctx.getString(R.string.notif_water_title) to ctx.getString(R.string.notif_water_body)
            ReminderType.Meals -> ctx.getString(
                when (ReminderSlots.mealAt(LocalTime.now())) {
                    MealType.Breakfast -> R.string.notif_breakfast_title
                    MealType.Lunch -> R.string.notif_lunch_title
                    MealType.Snack -> R.string.notif_snack_title
                    MealType.Dinner -> R.string.notif_dinner_title
                },
            ) to ctx.getString(R.string.notif_meal_body)
            ReminderType.WeighIn -> ctx.getString(R.string.notif_weigh_in_title) to ctx.getString(R.string.notif_weigh_in_body)
        }
        notifications.post(NOTIFICATION_BASE + type.ordinal, KcalNotifications.CHANNEL_REMINDERS, title, body)
        scheduler.schedule(type)
        return Result.success()
    }

    companion object {
        const val KEY_TYPE = "type"
        private const val NOTIFICATION_BASE = 7_100
    }
}
