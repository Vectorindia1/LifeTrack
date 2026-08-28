package com.lifetrack.water.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row settings table; always id = [SINGLETON_ID]. */
@Entity(tableName = "water_goal")
data class WaterGoal(
    @PrimaryKey
    val id: Long = SINGLETON_ID,
    val dailyTargetMl: Int = DEFAULT_DAILY_TARGET_ML,
) {
    companion object {
        const val SINGLETON_ID = 1L
        const val DEFAULT_DAILY_TARGET_ML = 2500
    }
}
