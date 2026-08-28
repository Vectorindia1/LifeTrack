package com.lifetrack.core.data

import androidx.room.TypeConverter
import com.lifetrack.diary.data.Mood
import com.lifetrack.habit.data.FrequencyType
import com.lifetrack.notification.data.FeatureType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * All Room type converters live here rather than being scattered per feature —
 * see MEMORY.md. java.time is used directly with no desugaring, which is safe
 * only because minSdk is 26.
 */
class Converters {

    @TypeConverter
    fun instantToEpochMilli(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    /** ISO-8601 (`2026-08-28`) so date columns sort and BETWEEN-compare correctly as text. */
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    /** ISO-8601 (`20:00`), same sorting rationale as dates. */
    @TypeConverter
    fun localTimeToString(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun frequencyTypeToString(value: FrequencyType?): String? = value?.name

    @TypeConverter
    fun stringToFrequencyType(value: String?): FrequencyType? = value?.let(FrequencyType::valueOf)

    @TypeConverter
    fun moodToString(value: Mood?): String? = value?.name

    @TypeConverter
    fun stringToMood(value: String?): Mood? = value?.let(Mood::valueOf)

    @TypeConverter
    fun themeModeToString(value: ThemeMode?): String? = value?.name

    @TypeConverter
    fun stringToThemeMode(value: String?): ThemeMode? = value?.let(ThemeMode::valueOf)

    @TypeConverter
    fun featureTypeToString(value: FeatureType?): String? = value?.name

    @TypeConverter
    fun stringToFeatureType(value: String?): FeatureType? = value?.let(FeatureType::valueOf)
}
