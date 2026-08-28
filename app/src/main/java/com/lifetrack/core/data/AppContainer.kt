package com.lifetrack.core.data

import android.content.Context
import com.lifetrack.calorie.data.CalorieRepository
import com.lifetrack.diary.data.DiaryRepository
import com.lifetrack.expense.data.ExpenseRepository
import com.lifetrack.goal.data.GoalRepository
import com.lifetrack.habit.data.HabitRepository
import com.lifetrack.notification.data.NotificationSettingsRepository
import com.lifetrack.water.data.WaterRepository

/**
 * Hand-rolled service locator. PRD section 5 lists Hilt as optional — the app is
 * not complex enough to warrant it yet. See MEMORY.md (2026-08-28) before adding a
 * DI framework.
 */
class AppContainer(context: Context) {

    val database: LifeTrackDatabase = LifeTrackDatabase.getInstance(context)

    val habitDao get() = database.habitDao()
    val goalDao get() = database.goalDao()
    val expenseDao get() = database.expenseDao()
    val calorieDao get() = database.calorieDao()
    val waterDao get() = database.waterDao()
    val diaryDao get() = database.diaryDao()
    val notificationSettingsDao get() = database.notificationSettingsDao()

    val habitRepository: HabitRepository by lazy { HabitRepository(habitDao) }
    val expenseRepository: ExpenseRepository by lazy { ExpenseRepository(expenseDao) }
    val calorieRepository: CalorieRepository by lazy { CalorieRepository(calorieDao) }
    val waterRepository: WaterRepository by lazy { WaterRepository(waterDao) }
    val goalRepository: GoalRepository by lazy { GoalRepository(goalDao) }
    val diaryRepository: DiaryRepository by lazy { DiaryRepository(diaryDao) }
    val notificationSettingsRepository: NotificationSettingsRepository by lazy {
        NotificationSettingsRepository(notificationSettingsDao)
    }
}
