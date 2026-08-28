package com.lifetrack.habit.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT COUNT(*) FROM habits")
    fun observeHabitCount(): Flow<Int>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabit(id: Long): Habit?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit)

    @Delete
    suspend fun delete(habit: Habit)

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun observeLogsForDate(date: LocalDate): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeLogsInRange(habitId: Long, from: LocalDate, to: LocalDate): Flow<List<HabitLog>>

    /** Completed logs for every habit in one query — streaks and charts are computed from this. */
    @Query("SELECT * FROM habit_logs WHERE completed = 1 AND date BETWEEN :from AND :to")
    fun observeCompletedLogsBetween(from: LocalDate, to: LocalDate): Flow<List<HabitLog>>

    /** Relies on the unique (habitId, date) index so a dashboard tap is one write. */
    @Upsert
    suspend fun upsertLog(log: HabitLog)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteLog(habitId: Long, date: LocalDate)
}
