package com.lifetrack.expense.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun observeExpenses(): Flow<List<Expense>>

    @Query("SELECT COUNT(*) FROM expenses")
    fun observeExpenseCount(): Flow<Int>

    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    fun observeExpensesBetween(from: Instant, to: Instant): Flow<List<Expense>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE timestamp BETWEEN :from AND :to")
    fun observeTotalBetween(from: Instant, to: Instant): Flow<Double>

    @Query("SELECT category, COUNT(*) AS count FROM expenses GROUP BY category ORDER BY count DESC")
    fun observeCategoryUsage(): Flow<List<CategoryUsage>>

    /**
     * Bulk rename. Categories have no table of their own — a category exists because
     * rows use it (see MEMORY.md) — so renaming one means updating those rows.
     */
    @Query("UPDATE expenses SET category = :to WHERE category = :from")
    suspend fun renameCategory(from: String, to: String)

    @Insert
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)
}
