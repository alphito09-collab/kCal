package com.alphito.kcal.feature.dietplan.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Monday → Sunday of the week containing [date]. */
fun weekOf(date: LocalDate): List<LocalDate> {
    val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0..6).map { monday.plusDays(it.toLong()) }
}
