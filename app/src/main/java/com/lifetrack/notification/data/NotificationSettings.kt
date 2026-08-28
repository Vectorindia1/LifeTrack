package com.lifetrack.notification.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalTime

/** The tracker a reminder belongs to. */
enum class FeatureType {
    HABIT,
    GOAL,
    CALORIE,
    WATER,
    DIARY,
}

/**
 * One row per (feature, reminder time). Deliberately **not** unique per feature:
 * PRD 7.8 gives water two reminder times (14:00 and 18:00), so a feature must be
 * able to own several rows.
 *
 * Note this drives *what goes into* the single consolidated daily digest — it is
 * not a licence to fire one notification per feature. See MEMORY.md,
 * "Notification consolidation".
 */
@Entity(
    tableName = "notification_settings",
    indices = [Index(value = ["featureType", "reminderTime"], unique = true)],
)
data class NotificationSettings(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val featureType: FeatureType,
    val enabled: Boolean = true,
    val reminderTime: LocalTime,
)
