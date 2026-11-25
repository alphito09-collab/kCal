package com.alphito.kcal.core.data.repository

import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import com.alphito.kcal.core.common.time.AppClock
import com.alphito.kcal.core.database.dao.MealPlanDao
import com.alphito.kcal.core.database.dao.StepsDao
import com.alphito.kcal.core.database.dao.WeightDao
import com.alphito.kcal.core.datastore.PreferencesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * The day the user started tracking: Progress "Week 1" and the first month begin here, and nothing
 * before it is counted. Set once when onboarding completes and never moved afterwards (profile
 * edits and re-planning leave it alone). Deleting all data clears it with the other preferences.
 */
interface TrackingStartRepository {
    val startDate: Flow<LocalDate>
    /** Records [date] unless a start date already exists. */
    suspend fun init(date: LocalDate)
}

internal class TrackingStartRepositoryImpl(
    private val preferences: PreferencesDataSource,
    private val weightDao: WeightDao,
    private val stepsDao: StepsDao,
    private val mealPlanDao: MealPlanDao,
    private val clock: AppClock,
    private val dispatchers: DispatcherProvider,
) : TrackingStartRepository {

    override val startDate: Flow<LocalDate> = preferences.trackingStartDay
        .map { day -> LocalDate.ofEpochDay(day ?: fallback()) }
        .distinctUntilChanged()
        .flowOn(dispatchers.io)

    override suspend fun init(date: LocalDate) {
        preferences.initTrackingStartDay(date.toEpochDay())
    }

    /**
     * Installs from before the start date was recorded: the earliest real activity (first weigh-in,
     * first checked meal, first steps), else today. Saved once, so it stays put from then on.
     */
    private suspend fun fallback(): Long {
        val today = clock.today().toEpochDay()
        val first = listOfNotNull(weightDao.firstDay(), mealPlanDao.firstCheckedDay(), stepsDao.firstDay()).minOrNull()
        val day = first?.coerceAtMost(today) ?: today
        preferences.initTrackingStartDay(day)
        return day
    }
}
