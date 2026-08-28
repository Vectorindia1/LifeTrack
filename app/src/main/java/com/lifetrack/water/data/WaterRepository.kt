package com.lifetrack.water.data

import com.lifetrack.expense.data.endOfDay
import com.lifetrack.expense.data.startOfDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class WaterRepository(private val dao: WaterDao) {

    fun observeLogsBetween(from: LocalDate, to: LocalDate): Flow<List<WaterLog>> =
        dao.observeLogsBetween(from.startOfDay(), to.endOfDay())

    fun observeGoal(): Flow<WaterGoal?> = dao.observeGoal()

    suspend fun add(mlAmount: Int) {
        if (mlAmount <= 0) return
        dao.insert(WaterLog(mlAmount = mlAmount))
    }

    suspend fun delete(log: WaterLog) = dao.delete(log)

    suspend fun setDailyTargetMl(target: Int) {
        dao.upsertGoal(WaterGoal(dailyTargetMl = target.coerceAtLeast(1)))
    }
}
