package com.alphito.kcal.core.data.repository

import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import com.alphito.kcal.core.database.dao.HealthLogDao
import com.alphito.kcal.core.database.dao.StepsDao
import com.alphito.kcal.core.database.dao.WeightDao
import com.alphito.kcal.core.database.entity.DairyLogEntity
import com.alphito.kcal.core.database.entity.IbsLogEntity
import com.alphito.kcal.core.database.entity.StepsEntity
import com.alphito.kcal.core.database.entity.SugarEntity
import com.alphito.kcal.core.database.entity.WeightEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.sin

/**
 * DEBUG BUILDS ONLY, opt-in – fills chart history (weights, steps, health logs) for development.
 * Never writes meal checks: those drive the streak and calorie history, which must start empty.
 * Enabled with `-Pkcal.sampleData=true` on a debug build (see app/build.gradle.kts).
 */
class SampleDataSeeder internal constructor(
    private val weightDao: WeightDao,
    private val stepsDao: StepsDao,
    private val healthLogDao: HealthLogDao,
    private val dispatchers: DispatcherProvider,
) {
    /**
     * Seeds [HISTORY_DAYS] days ending yesterday and returns the first seeded day, which becomes the
     * tracking start date (so Progress never shows fake data from before it). null if nothing was seeded.
     */
    suspend fun seed(today: LocalDate, currentWeightKg: Double): LocalDate? = withContext(dispatchers.io) {
        if (weightDao.observeAll().first().isNotEmpty()) return@withContext null
        val firstDay = today.minusDays(HISTORY_DAYS.toLong())
        // Weekly weigh-ins from the first day, trending down to the current weight (logged today).
        listOf(1.8, 1.4, 1.0, 0.6, 0.3, 0.0).forEachIndexed { i, delta ->
            weightDao.upsert(WeightEntity(firstDay.plusWeeks(i.toLong()).toEpochDay(), currentWeightKg + delta))
        }
        // Six weeks of daily steps with a gentle weekly rhythm.
        for (d in 1..HISTORY_DAYS) {
            val day = today.minusDays(d.toLong())
            val steps = (6_800 + 1_900 * sin(d / 3.0) + (d % 7) * 160).toInt().coerceAtLeast(1_500)
            stepsDao.upsert(StepsEntity(day.toEpochDay(), steps))
        }
        val sugar = listOf(128, 112, 120, 96, 108, 92, 110)
        val severity = listOf("Mild", "Moderate", "Mild", "Mild", "Severe", "Moderate", "Moderate")
        for (i in 0 until 7) {
            val day = today.minusDays((6 - i).toLong()).toEpochDay()
            healthLogDao.upsertSugar(SugarEntity(day, sugar[i]))
            healthLogDao.upsertIbs(IbsLogEntity(day, if (i % 2 == 0) "Bloating" else "", severity[i]))
        }
        listOf(
            DairyLogEntity(today.minusDays(4).toEpochDay(), true, "Bloating", "after milk tea"),
            DairyLogEntity(today.minusDays(7).toEpochDay(), true, "", "cheese"),
            DairyLogEntity(today.minusDays(10).toEpochDay(), true, "Gas,Bloating", "after ice cream"),
            DairyLogEntity(today.minusDays(14).toEpochDay(), false, "", ""),
        ).forEach { healthLogDao.upsertDairy(it) }
        firstDay
    }

    private companion object { const val HISTORY_DAYS = 41 }
}
