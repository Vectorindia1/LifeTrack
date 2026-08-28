package com.lifetrack.notification.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationSettingsDao {

    @Query("SELECT * FROM notification_settings ORDER BY reminderTime ASC")
    fun observeAll(): Flow<List<NotificationSettings>>

    @Query("SELECT COUNT(*) FROM notification_settings")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM notification_settings WHERE enabled = 1 ORDER BY reminderTime ASC")
    suspend fun getEnabled(): List<NotificationSettings>

    /** A feature may own more than one reminder time — water does. */
    @Query("SELECT * FROM notification_settings WHERE featureType = :featureType ORDER BY reminderTime ASC")
    suspend fun getFor(featureType: FeatureType): List<NotificationSettings>

    @Query("UPDATE notification_settings SET enabled = :enabled WHERE featureType = :featureType")
    suspend fun setEnabled(featureType: FeatureType, enabled: Boolean)

    @Upsert
    suspend fun upsert(settings: NotificationSettings)
}
