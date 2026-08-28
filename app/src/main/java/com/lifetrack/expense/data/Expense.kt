package com.lifetrack.expense.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * [category] is a plain String, exactly as PRD section 6 specifies. That keeps
 * preset-only and user-defined categories both open without a migration —
 * see MEMORY.md (2026-08-28).
 */
@Entity(
    tableName = "expenses",
    indices = [Index(value = ["timestamp"]), Index(value = ["category"])],
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val category: String,
    val amount: Double,
    val note: String? = null,
    val timestamp: Instant = Instant.now(),
)

/** How many expenses use a given category. Drives the settings screen's category list. */
data class CategoryUsage(
    val category: String,
    val count: Int,
)
