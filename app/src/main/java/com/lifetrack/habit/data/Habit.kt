package com.lifetrack.habit.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * How often a habit is expected to be done.
 *
 * [CUSTOM_DAYS] is storable from day one on purpose — see MEMORY.md (2026-08-28).
 * The v1 UI may only offer DAILY and WEEKLY; the column exists so that adding
 * custom schedules later does not require a Room migration.
 */
enum class FrequencyType {
    DAILY,
    WEEKLY,
    CUSTOM_DAYS,
}

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    /** Bitmask, bit 0 = Monday … bit 6 = Sunday. Only meaningful for [FrequencyType.CUSTOM_DAYS]. */
    val daysOfWeekMask: Int = ALL_DAYS_MASK,
    /** Target completions per week. Only meaningful for [FrequencyType.WEEKLY]. */
    val timesPerWeek: Int? = null,
    val streakCount: Int = 0,
    val createdAt: Instant = Instant.now(),
) {
    companion object {
        const val ALL_DAYS_MASK = 0b111_1111
    }
}
