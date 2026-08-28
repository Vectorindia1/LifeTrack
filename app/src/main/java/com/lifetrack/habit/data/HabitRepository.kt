package com.lifetrack.habit.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Habit reads and writes. Thin on purpose — the interesting logic is pure and lives
 * in [HabitSchedule]; this just moves rows.
 */
class HabitRepository(private val dao: HabitDao) {

    fun observeHabits(): Flow<List<Habit>> = dao.observeHabits()

    /**
     * Completed logs across a bounded window. [HISTORY_DAYS] is deliberately finite:
     * streaks and the longest chart window both fit inside it, and it stops the query
     * growing without limit as the app is used for years.
     */
    fun observeRecentCompletions(today: LocalDate): Flow<List<HabitLog>> =
        dao.observeCompletedLogsBetween(today.minusDays(HISTORY_DAYS), today)

    suspend fun setCompleted(habitId: Long, date: LocalDate, completed: Boolean) {
        if (completed) {
            // The unique (habitId, date) index makes this one write, not read-then-write.
            dao.upsertLog(HabitLog(habitId = habitId, date = date, completed = true))
        } else {
            dao.deleteLog(habitId, date)
        }
    }

    suspend fun addHabit(
        name: String,
        frequencyType: FrequencyType,
        daysOfWeekMask: Int,
        timesPerWeek: Int?,
    ): Long = dao.insert(
        Habit(
            name = name.trim(),
            frequencyType = frequencyType,
            daysOfWeekMask = daysOfWeekMask,
            timesPerWeek = timesPerWeek,
        ),
    )

    suspend fun delete(habit: Habit) = dao.delete(habit)

    /**
     * Keeps [Habit.streakCount] in step with the logs.
     *
     * The logs are the single source of truth; this column is a cache so that later
     * consumers (the milestone-9 notification worker especially) can read a streak
     * without replaying history. Never compute a displayed streak from this field.
     */
    suspend fun refreshStreak(habit: Habit, completedDates: Set<LocalDate>, today: LocalDate) {
        val streak = HabitSchedule.currentStreak(habit, completedDates, today)
        if (streak != habit.streakCount) {
            dao.update(habit.copy(streakCount = streak))
        }
    }

    private companion object {
        const val HISTORY_DAYS = 400L
    }
}
