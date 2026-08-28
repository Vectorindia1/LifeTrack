package com.lifetrack.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PreferencesRepository(private val dao: AppPreferencesDao) {

    /** Never null downstream: a missing row means defaults, not an error. */
    val preferences: Flow<AppPreferences> = dao.observe().map { it ?: AppPreferences() }

    suspend fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    suspend fun setWaterIncrements(smallMl: Int, largeMl: Int) = update {
        it.copy(
            waterIncrementSmallMl = smallMl.coerceAtLeast(1),
            waterIncrementLargeMl = largeMl.coerceAtLeast(1),
        )
    }

    suspend fun setDisplayName(name: String?) = update {
        it.copy(displayName = name?.trim()?.ifBlank { null })
    }

    /** Null resets to "follow the device locale" — see [AppPreferences.currencyLocaleTag]. */
    suspend fun setCurrencyLocaleTag(tag: String?) = update { it.copy(currencyLocaleTag = tag) }

    /**
     * WorkManager's own floor for a `PeriodicWorkRequest` is 15 minutes; anything
     * below that would silently be clamped up by the OS anyway, so clamp here where
     * it's visible instead.
     */
    suspend fun setWaterReminder(enabled: Boolean, intervalMinutes: Int) = update {
        it.copy(waterReminderEnabled = enabled, waterReminderIntervalMinutes = intervalMinutes.coerceAtLeast(15))
    }

    private suspend fun update(transform: (AppPreferences) -> AppPreferences) {
        dao.upsert(transform(preferences.first()))
    }
}
