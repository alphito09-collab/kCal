package com.alphito.kcal.feature.water.data

import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import com.alphito.kcal.core.data.repository.PreferencesRepository
import com.alphito.kcal.core.database.dao.WaterDao
import com.alphito.kcal.core.database.entity.WaterLogEntity
import com.alphito.kcal.core.model.WaterDay
import com.alphito.kcal.feature.water.domain.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

internal class WaterRepositoryImpl(
    private val dao: WaterDao,
    private val preferences: PreferencesRepository,
    private val dispatchers: DispatcherProvider,
) : WaterRepository {
    override fun observeDay(date: LocalDate): Flow<WaterDay> = combine(
        dao.observeTotal(date.toEpochDay()),
        preferences.preferences.map { it.waterGoalMl }.distinctUntilChanged(),
    ) { total, goal -> WaterDay(date, total, goal) }.flowOn(dispatchers.io)

    override suspend fun add(date: LocalDate, amountMl: Int) = withContext(dispatchers.io) {
        dao.insert(WaterLogEntity(epochDay = date.toEpochDay(), amountMl = amountMl, loggedAt = System.currentTimeMillis()))
    }

    override suspend fun setGoal(goalMl: Int) = preferences.setWaterGoal(goalMl)
}
