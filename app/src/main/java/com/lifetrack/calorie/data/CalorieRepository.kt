package com.lifetrack.calorie.data

import com.lifetrack.expense.data.endOfDay
import com.lifetrack.expense.data.startOfDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class CalorieRepository(private val dao: CalorieDao) {

    /** Logs across a local-date range. Same UTC-vs-local care as expenses. */
    fun observeLogsBetween(from: LocalDate, to: LocalDate): Flow<List<CalorieLog>> =
        dao.observeLogsBetween(from.startOfDay(), to.endOfDay())

    fun observeGoal(): Flow<CalorieGoal?> = dao.observeGoal()

    suspend fun add(foodName: String, calories: Int) {
        dao.insert(
            CalorieLog(
                foodName = foodName.trim().ifBlank { "Food" },
                calories = calories,
            ),
        )
    }

    suspend fun delete(log: CalorieLog) = dao.delete(log)

    suspend fun setDailyTarget(target: Int) {
        dao.upsertGoal(CalorieGoal(dailyTarget = target.coerceAtLeast(1)))
    }
}
