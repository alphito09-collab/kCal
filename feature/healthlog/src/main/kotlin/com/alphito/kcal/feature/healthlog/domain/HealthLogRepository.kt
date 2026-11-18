package com.alphito.kcal.feature.healthlog.domain

import com.alphito.kcal.core.model.DairyLog
import com.alphito.kcal.core.model.IbsLog
import com.alphito.kcal.core.model.SugarReading
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HealthLogRepository {
    fun sugar(from: LocalDate, to: LocalDate): Flow<List<SugarReading>>
    suspend fun saveSugar(reading: SugarReading)
    fun dairy(limit: Int): Flow<List<DairyLog>>
    suspend fun saveDairy(log: DairyLog)
    fun ibs(from: LocalDate, to: LocalDate): Flow<List<IbsLog>>
    suspend fun saveIbs(log: IbsLog)
}
