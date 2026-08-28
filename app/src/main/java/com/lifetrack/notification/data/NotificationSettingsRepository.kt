package com.lifetrack.notification.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

class NotificationSettingsRepository(private val dao: NotificationSettingsDao) {

    fun observeAll(): Flow<List<NotificationSettings>> = dao.observeAll()

    /**
     * Enabled reminder times grouped by feature — the shape [com.lifetrack.notification.domain.DailyDigest]
     * expects. A disabled feature is absent from the map rather than present-and-empty,
     * so "disabled" and "no times configured" behave identically.
     */
    suspend fun enabledReminders(): Map<FeatureType, List<LocalTime>> =
        dao.getEnabled()
            .groupBy { it.featureType }
            .mapValues { (_, rows) -> rows.map { it.reminderTime }.sorted() }

    suspend fun setEnabled(feature: FeatureType, enabled: Boolean) =
        dao.setEnabled(feature, enabled)

    suspend fun upsert(settings: NotificationSettings) = dao.upsert(settings)
}
