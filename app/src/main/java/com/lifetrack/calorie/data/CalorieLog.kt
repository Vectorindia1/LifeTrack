package com.lifetrack.calorie.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "calorie_logs", indices = [Index(value = ["timestamp"])])
data class CalorieLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val foodName: String,
    val calories: Int,
    val timestamp: Instant = Instant.now(),
)
