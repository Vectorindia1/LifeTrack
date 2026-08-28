package com.lifetrack.period.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class PeriodRepository(private val dao: PeriodDao) {

    fun observeAll(): Flow<List<PeriodLog>> = dao.observeAll()

    /** Logs a new period starting on [date]. The unique index on `startDate` rejects a duplicate. */
    suspend fun logStart(date: LocalDate) = dao.upsert(PeriodLog(startDate = date))

    suspend fun setEndDate(log: PeriodLog, endDate: LocalDate?) =
        dao.upsert(log.copy(endDate = endDate))

    suspend fun delete(log: PeriodLog) = dao.delete(log)
}
