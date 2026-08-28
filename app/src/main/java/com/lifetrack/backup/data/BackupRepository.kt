package com.lifetrack.backup.data

import androidx.room.withTransaction
import com.lifetrack.core.data.LifeTrackDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Full-database export/import — the safety net PRD asks for: reinstalling, updating
 * across a device, or a bad migration should never mean starting over.
 *
 * The export is a single JSON file covering every table. Import always **replaces**
 * everything currently in the database, never merges — a merge would have to resolve
 * conflicting IDs across two independently-grown histories, which is a much harder
 * problem this app has no need to solve. The whole operation runs inside one Room
 * transaction ([androidx.room.withTransaction]), so a failure partway through leaves
 * the existing data untouched rather than half-overwritten.
 */
class BackupRepository(private val db: LifeTrackDatabase) {

    private val dao get() = db.backupDao()

    suspend fun export(): String {
        val root = JSONObject()
        root.put("export_format_version", EXPORT_FORMAT_VERSION)
        root.put("exported_at", Instant.now().toEpochMilli())
        root.put("habits", dao.getAllHabits().toJsonArray(BackupCodec::habitToJson))
        root.put("habit_logs", dao.getAllHabitLogs().toJsonArray(BackupCodec::habitLogToJson))
        root.put("goals", dao.getAllGoals().toJsonArray(BackupCodec::goalToJson))
        root.put("expenses", dao.getAllExpenses().toJsonArray(BackupCodec::expenseToJson))
        root.put("calorie_logs", dao.getAllCalorieLogs().toJsonArray(BackupCodec::calorieLogToJson))
        root.put("calorie_goal", dao.getAllCalorieGoals().toJsonArray(BackupCodec::calorieGoalToJson))
        root.put("water_logs", dao.getAllWaterLogs().toJsonArray(BackupCodec::waterLogToJson))
        root.put("water_goal", dao.getAllWaterGoals().toJsonArray(BackupCodec::waterGoalToJson))
        root.put("diary_entries", dao.getAllDiaryEntries().toJsonArray(BackupCodec::diaryEntryToJson))
        root.put(
            "notification_settings",
            dao.getAllNotificationSettings().toJsonArray(BackupCodec::notificationSettingsToJson),
        )
        root.put("app_preferences", dao.getAllAppPreferences().toJsonArray(BackupCodec::appPreferencesToJson))
        root.put("period_logs", dao.getAllPeriodLogs().toJsonArray(BackupCodec::periodLogToJson))
        return root.toString(2)
    }

    /**
     * @return a human-readable reason on failure, or null on success. Nothing is
     * written to the database unless the whole file parses first — a malformed or
     * incompatible file must never leave the database half-restored.
     */
    suspend fun import(json: String): String? {
        val root = try {
            JSONObject(json)
        } catch (error: Exception) {
            return "This doesn't look like a LifeTrack backup file."
        }

        val version = root.optInt("export_format_version", -1)
        if (version != EXPORT_FORMAT_VERSION) {
            return "This backup was made by a different version of LifeTrack and can't be restored here."
        }

        val parsed = try {
            ParsedBackup(
                habits = root.array("habits").toList(BackupCodec::habitFromJson),
                habitLogs = root.array("habit_logs").toList(BackupCodec::habitLogFromJson),
                goals = root.array("goals").toList(BackupCodec::goalFromJson),
                expenses = root.array("expenses").toList(BackupCodec::expenseFromJson),
                calorieLogs = root.array("calorie_logs").toList(BackupCodec::calorieLogFromJson),
                calorieGoals = root.array("calorie_goal").toList(BackupCodec::calorieGoalFromJson),
                waterLogs = root.array("water_logs").toList(BackupCodec::waterLogFromJson),
                waterGoals = root.array("water_goal").toList(BackupCodec::waterGoalFromJson),
                diaryEntries = root.array("diary_entries").toList(BackupCodec::diaryEntryFromJson),
                notificationSettings = root.array("notification_settings")
                    .toList(BackupCodec::notificationSettingsFromJson),
                appPreferences = root.array("app_preferences").toList(BackupCodec::appPreferencesFromJson),
                periodLogs = root.array("period_logs").toList(BackupCodec::periodLogFromJson),
            )
        } catch (error: Exception) {
            return "The backup file is damaged or incomplete — nothing was changed."
        }

        db.withTransaction {
            dao.clearHabits()
            dao.clearHabitLogs()
            dao.clearGoals()
            dao.clearExpenses()
            dao.clearCalorieLogs()
            dao.clearCalorieGoals()
            dao.clearWaterLogs()
            dao.clearWaterGoals()
            dao.clearDiaryEntries()
            dao.clearNotificationSettings()
            dao.clearAppPreferences()
            dao.clearPeriodLogs()

            dao.insertHabits(parsed.habits)
            dao.insertHabitLogs(parsed.habitLogs)
            dao.insertGoals(parsed.goals)
            dao.insertExpenses(parsed.expenses)
            dao.insertCalorieLogs(parsed.calorieLogs)
            dao.insertCalorieGoals(parsed.calorieGoals)
            dao.insertWaterLogs(parsed.waterLogs)
            dao.insertWaterGoals(parsed.waterGoals)
            dao.insertDiaryEntries(parsed.diaryEntries)
            dao.insertNotificationSettings(parsed.notificationSettings)
            dao.insertAppPreferences(parsed.appPreferences)
            dao.insertPeriodLogs(parsed.periodLogs)
        }
        return null
    }

    private fun JSONObject.array(key: String): JSONArray = getJSONArray(key)

    /** Everything parsed before anything is written — see [import]. */
    private class ParsedBackup(
        val habits: List<com.lifetrack.habit.data.Habit>,
        val habitLogs: List<com.lifetrack.habit.data.HabitLog>,
        val goals: List<com.lifetrack.goal.data.Goal>,
        val expenses: List<com.lifetrack.expense.data.Expense>,
        val calorieLogs: List<com.lifetrack.calorie.data.CalorieLog>,
        val calorieGoals: List<com.lifetrack.calorie.data.CalorieGoal>,
        val waterLogs: List<com.lifetrack.water.data.WaterLog>,
        val waterGoals: List<com.lifetrack.water.data.WaterGoal>,
        val diaryEntries: List<com.lifetrack.diary.data.DiaryEntry>,
        val notificationSettings: List<com.lifetrack.notification.data.NotificationSettings>,
        val appPreferences: List<com.lifetrack.core.data.AppPreferences>,
        val periodLogs: List<com.lifetrack.period.data.PeriodLog>,
    )

    companion object {
        /**
         * Bump only when the JSON shape itself changes incompatibly (a renamed or
         * restructured field) — a new *table* can often still import fine into an
         * older reader by simply being ignored, but this project doesn't attempt
         * that leniency; a version mismatch is rejected outright rather than risking
         * a partially-understood import.
         */
        const val EXPORT_FORMAT_VERSION = 1
    }
}
