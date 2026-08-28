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
    /**
     * BCP-47 language tag (e.g. "en-IN", "en-US") whose *country* determines the
     * currency symbol and grouping used for money everywhere in the app. Null means
     * "follow the device locale" — the original behaviour, still the default. See
     * [com.lifetrack.core.ui.CurrencyOption] for the curated picker list and
     * [effectiveCurrencyLocale] for how this resolves to an actual [java.util.Locale].
     */
    val currencyLocaleTag: String? = null,
    /**
     * A recurring "drink water" nudge every [waterReminderIntervalMinutes], during
     * waking hours only. **Off by default and separate from [FeatureType.WATER]'s
     * digest reminder** — a per-hour alert is a deliberate departure from this app's
     * "one consolidated notification" rule (PRD 7.8), made at explicit user request.
     * See MEMORY.md before changing the default or folding this into the digest.
     */
    val waterReminderEnabled: Boolean = false,
    val waterReminderIntervalMinutes: Int = DEFAULT_WATER_REMINDER_MINUTES,
) {
    companion object {
        const val SINGLETON_ID = 1L
        const val DEFAULT_SMALL_ML = 250
        const val DEFAULT_LARGE_ML = 500
        const val DEFAULT_WATER_REMINDER_MINUTES = 60
    }
}

/** Pure resolution from a stored preference to an actual [java.util.Locale] — see [AppPreferences.currencyLocaleTag]. */
fun AppPreferences.effectiveCurrencyLocale(): java.util.Locale =
    currencyLocaleTag?.let(java.util.Locale::forLanguageTag) ?: java.util.Locale.getDefault()

@Dao
interface AppPreferencesDao {

    @Query("SELECT * FROM app_preferences WHERE id = 1")
    fun observe(): Flow<AppPreferences?>

    @Upsert
    suspend fun upsert(preferences: AppPreferences)
}
