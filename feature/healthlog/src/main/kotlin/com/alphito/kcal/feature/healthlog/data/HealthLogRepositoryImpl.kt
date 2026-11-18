package com.alphito.kcal.feature.healthlog.data

import com.alphito.kcal.core.common.dispatcher.DispatcherProvider
import com.alphito.kcal.core.database.dao.HealthLogDao
import com.alphito.kcal.core.database.entity.DairyLogEntity
import com.alphito.kcal.core.database.entity.IbsLogEntity
import com.alphito.kcal.core.database.entity.SugarEntity
import com.alphito.kcal.core.model.DairyLog
import com.alphito.kcal.core.model.DairyReaction
import com.alphito.kcal.core.model.IbsLog
import com.alphito.kcal.core.model.IbsSymptom
import com.alphito.kcal.core.model.Severity
import com.alphito.kcal.core.model.SugarReading
import com.alphito.kcal.feature.healthlog.domain.HealthLogRepository
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

internal class HealthLogRepositoryImpl(
    private val dao: HealthLogDao,
    private val dispatchers: DispatcherProvider,
) : HealthLogRepository {

    override fun sugar(from: LocalDate, to: LocalDate): Flow<List<SugarReading>> =
        dao.observeSugar(from.toEpochDay(), to.toEpochDay())
            .map { rows -> rows.map { SugarReading(LocalDate.ofEpochDay(it.epochDay), it.mgDl) } }
            .distinctUntilChanged().flowOn(dispatchers.io)

    override suspend fun saveSugar(reading: SugarReading) = withContext(dispatchers.io) {
        dao.upsertSugar(SugarEntity(reading.date.toEpochDay(), reading.mgDl))
    }

    override fun dairy(limit: Int): Flow<List<DairyLog>> = dao.observeDairy(limit).map { rows ->
        rows.map {
            DairyLog(
                date = LocalDate.ofEpochDay(it.epochDay), hadDairy = it.hadDairy,
                reactions = it.reactions.split(',').mapNotNull { n -> DairyReaction.entries.firstOrNull { r -> r.name == n } }.toImmutableSet(),
                note = it.note,
            )
        }
    }.distinctUntilChanged().flowOn(dispatchers.io)

    override suspend fun saveDairy(log: DairyLog) = withContext(dispatchers.io) {
        dao.upsertDairy(DairyLogEntity(log.date.toEpochDay(), log.hadDairy, log.reactions.joinToString(",") { it.name }, log.note))
    }

    override fun ibs(from: LocalDate, to: LocalDate): Flow<List<IbsLog>> =
        dao.observeIbs(from.toEpochDay(), to.toEpochDay()).map { rows ->
            rows.map {
                IbsLog(
                    date = LocalDate.ofEpochDay(it.epochDay),
                    symptoms = it.symptoms.split(',').mapNotNull { n -> IbsSymptom.entries.firstOrNull { s -> s.name == n } }.toImmutableSet(),
                    severity = Severity.entries.firstOrNull { s -> s.name == it.severity } ?: Severity.Mild,
                )
            }
        }.distinctUntilChanged().flowOn(dispatchers.io)

    override suspend fun saveIbs(log: IbsLog) = withContext(dispatchers.io) {
        dao.upsertIbs(IbsLogEntity(log.date.toEpochDay(), log.symptoms.joinToString(",") { it.name }, log.severity.name))
    }
}
