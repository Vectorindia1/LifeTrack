package com.lifetrack.goal.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class GoalRepository(private val dao: GoalDao) {

    fun observeGoals(): Flow<List<Goal>> = dao.observeGoals()

    suspend fun add(name: String, targetValue: Double, unit: String, deadline: LocalDate?): Long =
        dao.insert(
            Goal(
                name = name.trim(),
                targetValue = targetValue,
                unit = unit.trim(),
                deadline = deadline,
            ),
        )

    /** Absolute value entry. Clamped at zero — negative progress is never meaningful. */
    suspend fun setProgress(goal: Goal, value: Double) {
        dao.update(goal.copy(currentValue = value.coerceAtLeast(0.0)))
    }

    /** The increment button from PRD 7.3. */
    suspend fun increment(goal: Goal, by: Double) {
        setProgress(goal, goal.currentValue + by)
    }

    suspend fun delete(goal: Goal) = dao.delete(goal)
}
