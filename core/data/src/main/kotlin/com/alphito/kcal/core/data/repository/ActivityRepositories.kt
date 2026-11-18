package com.alphito.kcal.core.data.repository

import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import com.alphito.kcal.core.database.dao.StepsDao
import com.alphito.kcal.core.database.dao.WeightDao
import com.alphito.kcal.core.database.entity.StepsEntity
import com.alphito.kcal.core.database.entity.WeightEntity
import com.alphito.kcal.core.model.StepsDay
import com.alphito.kcal.core.model.WeightEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

interface StepsRepository {
    fun observeDay(date: LocalDate): Flow<Int>
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<StepsDay>>
    suspend fun addSteps(date: LocalDate, delta: Int)
    suspend fun setSteps(date: LocalDate, steps: Int)
}

internal class StepsRepositoryImpl(private val dao: StepsDao, private val dispatchers: DispatcherProvider) : StepsRepository {
    override fun observeDay(date: LocalDate): Flow<Int> =
        dao.observeDay(date.toEpochDay()).map { it ?: 0 }.distinctUntilChanged().flowOn(dispatchers.io)

    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<StepsDay>> =
        dao.observeRange(from.toEpochDay(), to.toEpochDay())
            .map { rows -> rows.map { StepsDay(LocalDate.ofEpochDay(it.epochDay), it.steps) } }
            .distinctUntilChanged().flowOn(dispatchers.io)

    override suspend fun addSteps(date: LocalDate, delta: Int) = withContext(dispatchers.io) {
        if (delta > 0) dao.add(date.toEpochDay(), delta)
    }

    override suspend fun setSteps(date: LocalDate, steps: Int) = withContext(dispatchers.io) {
        dao.upsert(StepsEntity(date.toEpochDay(), steps))
    }
}

interface WeightRepository {
    val entries: Flow<List<WeightEntry>>
    suspend fun log(date: LocalDate, weightKg: Double)
}

internal class WeightRepositoryImpl(
    private val dao: WeightDao,
    private val userRepository: UserRepository,
    private val dispatchers: DispatcherProvider,
) : WeightRepository {
    override val entries: Flow<List<WeightEntry>> = dao.observeAll()
        .map { rows -> rows.map { WeightEntry(LocalDate.ofEpochDay(it.epochDay), it.weightKg) } }
        .distinctUntilChanged().flowOn(dispatchers.io)

    override suspend fun log(date: LocalDate, weightKg: Double) = withContext(dispatchers.io) {
        dao.upsert(WeightEntity(date.toEpochDay(), weightKg))
        // Keep the profile weight current so BMR/TDEE follow the user's progress.
        userRepository.update { it.copy(weightKg = weightKg) }
    }
}
