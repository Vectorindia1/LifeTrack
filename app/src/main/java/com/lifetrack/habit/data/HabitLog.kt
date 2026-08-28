package com.lifetrack.habit.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * One row per habit per day. The unique index on (habitId, date) is what lets a
 * dashboard tap be a single upsert rather than a read-then-write.
 */
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["habitId", "date"], unique = true),
        Index(value = ["date"]),
    ],
)
data class HabitLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val habitId: Long,
    val date: LocalDate,
    val completed: Boolean = true,
)
