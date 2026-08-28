package com.lifetrack.calorie.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface CalorieDao {

    @Query("SELECT * FROM calorie_logs WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    fun observeLogsBetween(from: Instant, to: Instant): Flow<List<CalorieLog>>

    @Query("SELECT COALESCE(SUM(calories), 0) FROM calorie_logs WHERE timestamp BETWEEN :from AND :to")
    fun observeTotalBetween(from: Instant, to: Instant): Flow<Int>

    @Insert
    suspend fun insert(log: CalorieLog): Long

    @Delete
    suspend fun delete(log: CalorieLog)

    @Query("SELECT * FROM calorie_goal WHERE id = 1")
    fun observeGoal(): Flow<CalorieGoal?>

    @Upsert
    suspend fun upsertGoal(goal: CalorieGoal)
}
