package com.lifetrack.backup.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifetrack.calorie.data.CalorieGoal
import com.lifetrack.calorie.data.CalorieLog
import com.lifetrack.core.data.AppPreferences
import com.lifetrack.diary.data.DiaryEntry
import com.lifetrack.expense.data.Expense
import com.lifetrack.goal.data.Goal
import com.lifetrack.habit.data.Habit
import com.lifetrack.habit.data.HabitLog
import com.lifetrack.notification.data.NotificationSettings
import com.lifetrack.period.data.PeriodLog
import com.lifetrack.water.data.WaterGoal
import com.lifetrack.water.data.WaterLog

/**
 * One DAO spanning every table, used only for full-database export/import.
 *
 * Deliberately separate from the 8 per-feature DAOs rather than adding "get all
 * rows" methods to each of them — this keeps the backup concern in one file instead
 * of scattering it across every feature package, and Room does not require a DAO to
 * be scoped to a single entity.
 *
 * Every insert uses [OnConflictStrategy.REPLACE] and explicit primary keys from the
 * export — import always clears each table first (see [BackupRepository]), so a
 * REPLACE here can only ever mean "restore the row that used to be there."
 */
@Dao
interface BackupDao {

    @Query("SELECT * FROM habits") suspend fun getAllHabits(): List<Habit>
    @Query("SELECT * FROM habit_logs") suspend fun getAllHabitLogs(): List<HabitLog>
    @Query("SELECT * FROM goals") suspend fun getAllGoals(): List<Goal>
    @Query("SELECT * FROM expenses") suspend fun getAllExpenses(): List<Expense>
    @Query("SELECT * FROM calorie_logs") suspend fun getAllCalorieLogs(): List<CalorieLog>
    @Query("SELECT * FROM calorie_goal") suspend fun getAllCalorieGoals(): List<CalorieGoal>
    @Query("SELECT * FROM water_logs") suspend fun getAllWaterLogs(): List<WaterLog>
    @Query("SELECT * FROM water_goal") suspend fun getAllWaterGoals(): List<WaterGoal>
    @Query("SELECT * FROM diary_entries") suspend fun getAllDiaryEntries(): List<DiaryEntry>
    @Query("SELECT * FROM notification_settings") suspend fun getAllNotificationSettings(): List<NotificationSettings>
    @Query("SELECT * FROM app_preferences") suspend fun getAllAppPreferences(): List<AppPreferences>
    @Query("SELECT * FROM period_logs") suspend fun getAllPeriodLogs(): List<PeriodLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHabits(rows: List<Habit>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHabitLogs(rows: List<HabitLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGoals(rows: List<Goal>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExpenses(rows: List<Expense>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCalorieLogs(rows: List<CalorieLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCalorieGoals(rows: List<CalorieGoal>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWaterLogs(rows: List<WaterLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWaterGoals(rows: List<WaterGoal>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDiaryEntries(rows: List<DiaryEntry>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertNotificationSettings(rows: List<NotificationSettings>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAppPreferences(rows: List<AppPreferences>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPeriodLogs(rows: List<PeriodLog>)

    @Query("DELETE FROM habits") suspend fun clearHabits()
    @Query("DELETE FROM habit_logs") suspend fun clearHabitLogs()
    @Query("DELETE FROM goals") suspend fun clearGoals()
    @Query("DELETE FROM expenses") suspend fun clearExpenses()
    @Query("DELETE FROM calorie_logs") suspend fun clearCalorieLogs()
    @Query("DELETE FROM calorie_goal") suspend fun clearCalorieGoals()
    @Query("DELETE FROM water_logs") suspend fun clearWaterLogs()
    @Query("DELETE FROM water_goal") suspend fun clearWaterGoals()
    @Query("DELETE FROM diary_entries") suspend fun clearDiaryEntries()
    @Query("DELETE FROM notification_settings") suspend fun clearNotificationSettings()
    @Query("DELETE FROM app_preferences") suspend fun clearAppPreferences()
    @Query("DELETE FROM period_logs") suspend fun clearPeriodLogs()
}
