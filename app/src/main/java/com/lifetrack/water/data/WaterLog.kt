package com.lifetrack.water.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "water_logs", indices = [Index(value = ["timestamp"])])
data class WaterLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val mlAmount: Int,
    val timestamp: Instant = Instant.now(),
)
