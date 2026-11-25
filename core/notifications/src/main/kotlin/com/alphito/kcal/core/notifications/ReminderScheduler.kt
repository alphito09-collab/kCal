package com.alphito.kcal.core.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.alphito.kcal.core.model.ReminderType
import java.time.Duration
import java.time.LocalDateTime

/**
 * One unique, exactly-timed WorkManager job per reminder type. Each run posts the notification
 * and enqueues the next slot – no 15-minute polling, survives reboots (WorkManager persists jobs).
 */
class ReminderScheduler(private val context: Context) {

    fun sync(enabled: Set<ReminderType>) = ReminderType.entries.forEach { type ->
        if (type in enabled) schedule(type) else cancel(type)
    }

    fun schedule(type: ReminderType, now: LocalDateTime = LocalDateTime.now()) {
        val delay = Duration.between(now, ReminderSlots.next(type, now)).coerceAtLeast(Duration.ofSeconds(1))
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay)
            .setInputData(workDataOf(ReminderWorker.KEY_TYPE to type.name))
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(name(type), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(type: ReminderType) {
        WorkManager.getInstance(context).cancelUniqueWork(name(type))
    }

    private fun name(type: ReminderType) = "reminder-${type.name.lowercase()}"

    private companion object { const val TAG = "kcal-reminder" }
}
