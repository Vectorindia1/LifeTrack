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

    private suspend fun update(transform: (AppPreferences) -> AppPreferences) {
        dao.upsert(transform(preferences.first()))
    }
}
