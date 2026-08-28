package com.lifetrack.core.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * App-level preferences that are not specific to one tracker.
 *
 * Single-row table, matching the pattern already used by `calorie_goal` and
 * `water_goal`. Kept in Room rather than adding DataStore: one storage mechanism is
 * simpler to reason about, and these values are read alongside other Room data anyway.
 */
@Entity(tableName = "app_preferences")
data class AppPreferences(
    @PrimaryKey
    val id: Long = SINGLETON_ID,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** PRD 7.9's configurable "ml increments" — the water quick-add buttons. */
    val waterIncrementSmallMl: Int = DEFAULT_SMALL_ML,
    val waterIncrementLargeMl: Int = DEFAULT_LARGE_ML,
    /** Optional, for the dashboard greeting. Never required anywhere else. */
    val displayName: String? = null,
) {
    companion object {
        const val SINGLETON_ID = 1L
        const val DEFAULT_SMALL_ML = 250
        const val DEFAULT_LARGE_ML = 500
    }
}

@Dao
interface AppPreferencesDao {

    @Query("SELECT * FROM app_preferences WHERE id = 1")
    fun observe(): Flow<AppPreferences?>

    @Upsert
    suspend fun upsert(preferences: AppPreferences)
}
