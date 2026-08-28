package com.lifetrack.goal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val targetValue: Double,
    val currentValue: Double = 0.0,
    /** Free text, e.g. "km", "books", "kg". */
    val unit: String,
    val deadline: LocalDate? = null,
    val createdAt: Instant = Instant.now(),
)
