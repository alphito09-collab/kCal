package com.alphito.kcal.feature.water.domain

import com.alphito.kcal.core.model.WaterDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface WaterRepository {
    fun observeDay(date: LocalDate): Flow<WaterDay>
    suspend fun add(date: LocalDate, amountMl: Int)
    suspend fun setGoal(goalMl: Int)
}
