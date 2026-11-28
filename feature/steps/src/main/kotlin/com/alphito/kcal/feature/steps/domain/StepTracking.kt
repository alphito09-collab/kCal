package com.alphito.kcal.feature.steps.domain

import com.alphito.kcal.core.model.StepsDay
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class TrackingStatus { Idle, Tracking, NoSensor }

/**
 * Starts/stops the step foreground service and exposes whether it is running. Also owns the
 * "tracking on" preference, so every way of starting or stopping (screen, notification, boot) agrees.
 */
interface StepTrackingController {
    val status: StateFlow<TrackingStatus>
    val hasStepSensor: Boolean
    fun start()
    fun stop()
    /** Restarts tracking the user left on (app launch, after a reboot) if it's still permitted. */
    suspend fun resumeIfEnabled()
}

enum class HistoryRange { Months, Weeks, Days }

data class HistoryBucket(val label: String, val steps: Int, val start: LocalDate)

/** Groups daily rows into the three history views shown on the Steps screen. */
object StepsHistory {
    fun buckets(range: HistoryRange, days: List<StepsDay>, today: LocalDate, locale: Locale, weekLabel: (Int) -> String): List<HistoryBucket> {
        val byDay = days.associate { it.date to it.steps }
        return when (range) {
            HistoryRange.Days -> (6 downTo 0).map { back ->
                val d = today.minusDays(back.toLong())
                HistoryBucket(d.dayOfWeek.getDisplayName(TextStyle.SHORT, locale), byDay[d] ?: 0, d)
            }
            HistoryRange.Weeks -> (3 downTo 0).mapIndexed { index, back ->
                val end = today.minusWeeks(back.toLong())
                val start = end.minusDays(6)
                HistoryBucket(weekLabel(index + 1), sum(byDay, start, end), start)
            }
            HistoryRange.Months -> (5 downTo 0).map { back ->
                val month = today.minusMonths(back.toLong()).withDayOfMonth(1)
                val end = if (back == 0) today else month.plusMonths(1).minusDays(1)
                HistoryBucket(month.month.getDisplayName(TextStyle.SHORT, locale), sum(byDay, month, end), month)
            }
        }
    }

    private fun sum(byDay: Map<LocalDate, Int>, start: LocalDate, end: LocalDate): Int =
        (0..ChronoUnit.DAYS.between(start, end)).sumOf { byDay[start.plusDays(it)] ?: 0 }
}
