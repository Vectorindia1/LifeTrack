package com.lifetrack.calorie.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row settings table; always id = [SINGLETON_ID]. */
@Entity(tableName = "calorie_goal")
data class CalorieGoal(
    @PrimaryKey
    val id: Long = SINGLETON_ID,
    val dailyTarget: Int = DEFAULT_DAILY_TARGET,
) {
    companion object {
        const val SINGLETON_ID = 1L
        const val DEFAULT_DAILY_TARGET = 2000
    }
}
