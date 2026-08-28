package com.lifetrack.backup.data

import com.lifetrack.calorie.data.CalorieGoal
import com.lifetrack.calorie.data.CalorieLog
import com.lifetrack.core.data.AppPreferences
import com.lifetrack.core.data.ThemeMode
import com.lifetrack.diary.data.DiaryEntry
import com.lifetrack.diary.data.Mood
import com.lifetrack.expense.data.Expense
import com.lifetrack.goal.data.Goal
import com.lifetrack.habit.data.FrequencyType
import com.lifetrack.habit.data.Habit
import com.lifetrack.habit.data.HabitLog
import com.lifetrack.notification.data.FeatureType
import com.lifetrack.notification.data.NotificationSettings
import com.lifetrack.period.data.PeriodLog
import com.lifetrack.water.data.WaterGoal
import com.lifetrack.water.data.WaterLog
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Hand-written JSON mapping for every entity, one function pair per table.
 *
 * No serialization library — the entities are small and few, and explicit code here
 * matches how the rest of this app already handles (de)serialization by hand
 * (`core/data/Converters.kt`), rather than pulling in reflection-based magic for a
 * feature that runs once in a while and must never silently corrupt a user's data.
 *
 * Dates/times are ISO strings (`LocalDate.toString()`/`LocalTime.toString()`, both
 * already ISO-8601 by default) and instants are epoch millis — the same
 * representations Room itself stores via `Converters.kt`, so there is exactly one
 * date/time convention in this codebase, not two. JSON keys are `snake_case` to read
 * naturally if a user opens the export file themselves; Kotlin fields stay camelCase.
 */
object BackupCodec {

    fun habitToJson(h: Habit) = JSONObject().apply {
        put("id", h.id)
        put("name", h.name)
        put("frequency_type", h.frequencyType.name)
        put("days_of_week_mask", h.daysOfWeekMask)
        put("times_per_week", h.timesPerWeek?.let { it as Any } ?: JSONObject.NULL)
        put("streak_count", h.streakCount)
        put("created_at", h.createdAt.toEpochMilli())
    }

    fun habitFromJson(o: JSONObject) = Habit(
        id = o.getLong("id"),
        name = o.getString("name"),
        frequencyType = FrequencyType.valueOf(o.getString("frequency_type")),
        daysOfWeekMask = o.getInt("days_of_week_mask"),
        timesPerWeek = if (o.isNull("times_per_week")) null else o.getInt("times_per_week"),
        streakCount = o.getInt("streak_count"),
        createdAt = Instant.ofEpochMilli(o.getLong("created_at")),
    )

    fun habitLogToJson(h: HabitLog) = JSONObject().apply {
        put("id", h.id)
        put("habit_id", h.habitId)
        put("date", h.date.toString())
        put("completed", h.completed)
    }

    fun habitLogFromJson(o: JSONObject) = HabitLog(
        id = o.getLong("id"),
        habitId = o.getLong("habit_id"),
        date = LocalDate.parse(o.getString("date")),
        completed = o.getBoolean("completed"),
    )

    fun goalToJson(g: Goal) = JSONObject().apply {
        put("id", g.id)
        put("name", g.name)
        put("target_value", g.targetValue)
        put("current_value", g.currentValue)
        put("unit", g.unit)
        put("deadline", g.deadline?.toString() ?: JSONObject.NULL)
        put("created_at", g.createdAt.toEpochMilli())
    }

    fun goalFromJson(o: JSONObject) = Goal(
        id = o.getLong("id"),
        name = o.getString("name"),
        targetValue = o.getDouble("target_value"),
        currentValue = o.getDouble("current_value"),
        unit = o.getString("unit"),
        deadline = if (o.isNull("deadline")) null else LocalDate.parse(o.getString("deadline")),
        createdAt = Instant.ofEpochMilli(o.getLong("created_at")),
    )

    fun expenseToJson(e: Expense) = JSONObject().apply {
        put("id", e.id)
        put("category", e.category)
        put("amount", e.amount)
        put("note", e.note ?: JSONObject.NULL)
        put("timestamp", e.timestamp.toEpochMilli())
    }

    fun expenseFromJson(o: JSONObject) = Expense(
        id = o.getLong("id"),
        category = o.getString("category"),
        amount = o.getDouble("amount"),
        note = if (o.isNull("note")) null else o.getString("note"),
        timestamp = Instant.ofEpochMilli(o.getLong("timestamp")),
    )

    fun calorieLogToJson(c: CalorieLog) = JSONObject().apply {
        put("id", c.id)
        put("food_name", c.foodName)
        put("calories", c.calories)
        put("timestamp", c.timestamp.toEpochMilli())
    }

    fun calorieLogFromJson(o: JSONObject) = CalorieLog(
        id = o.getLong("id"),
        foodName = o.getString("food_name"),
        calories = o.getInt("calories"),
        timestamp = Instant.ofEpochMilli(o.getLong("timestamp")),
    )

    fun calorieGoalToJson(c: CalorieGoal) = JSONObject().apply {
        put("id", c.id)
        put("daily_target", c.dailyTarget)
    }

    fun calorieGoalFromJson(o: JSONObject) = CalorieGoal(
        id = o.getLong("id"),
        dailyTarget = o.getInt("daily_target"),
    )

    fun waterLogToJson(w: WaterLog) = JSONObject().apply {
        put("id", w.id)
        put("ml_amount", w.mlAmount)
        put("timestamp", w.timestamp.toEpochMilli())
    }

    fun waterLogFromJson(o: JSONObject) = WaterLog(
        id = o.getLong("id"),
        mlAmount = o.getInt("ml_amount"),
        timestamp = Instant.ofEpochMilli(o.getLong("timestamp")),
    )

    fun waterGoalToJson(w: WaterGoal) = JSONObject().apply {
        put("id", w.id)
        put("daily_target_ml", w.dailyTargetMl)
    }

    fun waterGoalFromJson(o: JSONObject) = WaterGoal(
        id = o.getLong("id"),
        dailyTargetMl = o.getInt("daily_target_ml"),
    )

    fun diaryEntryToJson(d: DiaryEntry) = JSONObject().apply {
        put("id", d.id)
        put("date", d.date.toString())
        put("text", d.text)
        put("mood", d.mood?.name ?: JSONObject.NULL)
    }

    fun diaryEntryFromJson(o: JSONObject) = DiaryEntry(
        id = o.getLong("id"),
        date = LocalDate.parse(o.getString("date")),
        text = o.getString("text"),
        mood = if (o.isNull("mood")) null else Mood.valueOf(o.getString("mood")),
    )

    fun notificationSettingsToJson(n: NotificationSettings) = JSONObject().apply {
        put("id", n.id)
        put("feature_type", n.featureType.name)
        put("enabled", n.enabled)
        put("reminder_time", n.reminderTime.toString())
    }

    fun notificationSettingsFromJson(o: JSONObject) = NotificationSettings(
        id = o.getLong("id"),
        featureType = FeatureType.valueOf(o.getString("feature_type")),
        enabled = o.getBoolean("enabled"),
        reminderTime = LocalTime.parse(o.getString("reminder_time")),
    )

    fun appPreferencesToJson(a: AppPreferences) = JSONObject().apply {
        put("id", a.id)
        put("theme_mode", a.themeMode.name)
        put("water_increment_small_ml", a.waterIncrementSmallMl)
        put("water_increment_large_ml", a.waterIncrementLargeMl)
        put("display_name", a.displayName ?: JSONObject.NULL)
        put("currency_locale_tag", a.currencyLocaleTag ?: JSONObject.NULL)
        put("water_reminder_enabled", a.waterReminderEnabled)
        put("water_reminder_interval_minutes", a.waterReminderIntervalMinutes)
    }

    fun appPreferencesFromJson(o: JSONObject) = AppPreferences(
        id = o.getLong("id"),
        themeMode = ThemeMode.valueOf(o.getString("theme_mode")),
        waterIncrementSmallMl = o.getInt("water_increment_small_ml"),
        waterIncrementLargeMl = o.getInt("water_increment_large_ml"),
        displayName = if (o.isNull("display_name")) null else o.getString("display_name"),
        currencyLocaleTag = if (o.isNull("currency_locale_tag")) null else o.getString("currency_locale_tag"),
        waterReminderEnabled = o.getBoolean("water_reminder_enabled"),
        waterReminderIntervalMinutes = o.getInt("water_reminder_interval_minutes"),
    )

    fun periodLogToJson(p: PeriodLog) = JSONObject().apply {
        put("id", p.id)
        put("start_date", p.startDate.toString())
        put("end_date", p.endDate?.toString() ?: JSONObject.NULL)
    }

    fun periodLogFromJson(o: JSONObject) = PeriodLog(
        id = o.getLong("id"),
        startDate = LocalDate.parse(o.getString("start_date")),
        endDate = if (o.isNull("end_date")) null else LocalDate.parse(o.getString("end_date")),
    )
}

/** [JSONArray] built from a list via a per-item mapper — the pattern every table below uses. */
fun <T> List<T>.toJsonArray(map: (T) -> JSONObject): JSONArray =
    JSONArray().apply { this@toJsonArray.forEach { put(map(it)) } }

/** The reverse of [toJsonArray]. */
fun <T> JSONArray.toList(map: (JSONObject) -> T): List<T> =
    (0 until length()).map { map(getJSONObject(it)) }
