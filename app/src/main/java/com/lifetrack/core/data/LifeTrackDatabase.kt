package com.lifetrack.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifetrack.calorie.data.CalorieDao
import com.lifetrack.calorie.data.CalorieGoal
import com.lifetrack.calorie.data.CalorieLog
import com.lifetrack.diary.data.DiaryDao
import com.lifetrack.diary.data.DiaryEntry
import com.lifetrack.expense.data.Expense
import com.lifetrack.expense.data.ExpenseDao
import com.lifetrack.goal.data.Goal
import com.lifetrack.goal.data.GoalDao
import com.lifetrack.habit.data.Habit
import com.lifetrack.habit.data.HabitDao
import com.lifetrack.habit.data.HabitLog
import com.lifetrack.notification.data.NotificationSettings
import com.lifetrack.notification.data.NotificationSettingsDao
import com.lifetrack.water.data.WaterDao
import com.lifetrack.water.data.WaterGoal
import com.lifetrack.water.data.WaterLog

/**
 * The whole local store. There is deliberately no generic `Entry` table — PRD
 * section 6's `Entry` is a concept, not a table. See MEMORY.md (2026-08-28).
 */
@Database(
    entities = [
        Habit::class,
        HabitLog::class,
        Goal::class,
        Expense::class,
        CalorieLog::class,
        CalorieGoal::class,
        WaterLog::class,
        WaterGoal::class,
        DiaryEntry::class,
        NotificationSettings::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LifeTrackDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun goalDao(): GoalDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun calorieDao(): CalorieDao
    abstract fun waterDao(): WaterDao
    abstract fun diaryDao(): DiaryDao
    abstract fun notificationSettingsDao(): NotificationSettingsDao

    companion object {
        const val DATABASE_NAME = "lifetrack.db"

        @Volatile
        private var instance: LifeTrackDatabase? = null

        fun getInstance(context: Context): LifeTrackDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): LifeTrackDatabase =
            Room.databaseBuilder(context, LifeTrackDatabase::class.java, DATABASE_NAME)
                .addCallback(SeedCallback)
                .build()

        /**
         * Seeds the rows the app assumes always exist: the two singleton target
         * rows and one reminder row per feature, at the default times in PRD 7.8.
         * Raw SQL rather than DAOs because this runs during database creation,
         * before the instance is handed out.
         */
        private object SeedCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT INTO calorie_goal (id, dailyTarget) VALUES (?, ?)",
                    arrayOf<Any>(CalorieGoal.SINGLETON_ID, CalorieGoal.DEFAULT_DAILY_TARGET),
                )
                db.execSQL(
                    "INSERT INTO water_goal (id, dailyTargetMl) VALUES (?, ?)",
                    arrayOf<Any>(WaterGoal.SINGLETON_ID, WaterGoal.DEFAULT_DAILY_TARGET_ML),
                )
                DEFAULT_REMINDERS.forEach { (feature, time) ->
                    db.execSQL(
                        "INSERT INTO notification_settings (featureType, enabled, reminderTime) VALUES (?, 1, ?)",
                        arrayOf<Any>(feature, time),
                    )
                }
            }
        }

        /** Defaults straight from PRD 7.8. All user-configurable in Settings (milestone 10). */
        private val DEFAULT_REMINDERS = listOf(
            "HABIT" to "20:00",
            "GOAL" to "09:00",
            "CALORIE" to "20:30",
            // Water is checked twice a day, per PRD 7.8 — hence two rows.
            "WATER" to "14:00",
            "WATER" to "18:00",
            "DIARY" to "21:30",
        )
    }
}
