package com.lifetrack.goal.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY deadline IS NULL, deadline ASC, createdAt ASC")
    fun observeGoals(): Flow<List<Goal>>

    @Query("SELECT COUNT(*) FROM goals")
    fun observeGoalCount(): Flow<Int>

    @Query("SELECT * FROM goals WHERE deadline IS NOT NULL AND deadline <= :through ORDER BY deadline ASC")
    suspend fun getGoalsDueThrough(through: LocalDate): List<Goal>

    @Insert
    suspend fun insert(goal: Goal): Long

    @Update
    suspend fun update(goal: Goal)

    @Delete
    suspend fun delete(goal: Goal)
}
