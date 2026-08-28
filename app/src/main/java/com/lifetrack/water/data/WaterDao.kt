package com.lifetrack.water.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface WaterDao {

    @Query("SELECT * FROM water_logs WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    fun observeLogsBetween(from: Instant, to: Instant): Flow<List<WaterLog>>

    @Query("SELECT COALESCE(SUM(mlAmount), 0) FROM water_logs WHERE timestamp BETWEEN :from AND :to")
    fun observeTotalMlBetween(from: Instant, to: Instant): Flow<Int>

    @Insert
    suspend fun insert(log: WaterLog): Long

    @Delete
    suspend fun delete(log: WaterLog)

    @Query("SELECT * FROM water_goal WHERE id = 1")
    fun observeGoal(): Flow<WaterGoal?>

    @Upsert
    suspend fun upsertGoal(goal: WaterGoal)
}
