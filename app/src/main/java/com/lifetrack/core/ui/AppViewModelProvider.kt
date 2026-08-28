package com.lifetrack.core.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lifetrack.LifeTrackApplication
import com.lifetrack.dashboard.viewmodel.DashboardViewModel
import com.lifetrack.calorie.viewmodel.CalorieViewModel
import com.lifetrack.diary.viewmodel.DiaryViewModel
import com.lifetrack.settings.viewmodel.SettingsViewModel
import com.lifetrack.expense.viewmodel.ExpenseViewModel
import com.lifetrack.goal.viewmodel.GoalViewModel
import com.lifetrack.period.viewmodel.PeriodViewModel
import com.lifetrack.habit.viewmodel.HabitViewModel
import com.lifetrack.water.viewmodel.WaterViewModel

/** ViewModel wiring for the manual-DI setup. One entry per ViewModel. */
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer {
            DashboardViewModel(
                habitRepository = lifeTrackApplication().container.habitRepository,
                expenseRepository = lifeTrackApplication().container.expenseRepository,
                calorieRepository = lifeTrackApplication().container.calorieRepository,
                waterRepository = lifeTrackApplication().container.waterRepository,
                goalRepository = lifeTrackApplication().container.goalRepository,
                diaryRepository = lifeTrackApplication().container.diaryRepository,
                periodRepository = lifeTrackApplication().container.periodRepository,
                preferencesRepository = lifeTrackApplication().container.preferencesRepository,
            )
        }
        initializer { HabitViewModel(lifeTrackApplication().container.habitRepository) }
        initializer { ExpenseViewModel(lifeTrackApplication().container.expenseRepository) }
        initializer { CalorieViewModel(lifeTrackApplication().container.calorieRepository) }
        initializer {
            val app = lifeTrackApplication()
            WaterViewModel(app.container.waterRepository, app.container.preferencesRepository)
        }
        initializer {
            val app = lifeTrackApplication()
            SettingsViewModel(
                context = app,
                preferencesRepository = app.container.preferencesRepository,
                calorieRepository = app.container.calorieRepository,
                waterRepository = app.container.waterRepository,
                notificationRepository = app.container.notificationSettingsRepository,
                habitRepository = app.container.habitRepository,
                expenseRepository = app.container.expenseRepository,
            )
        }
        initializer { GoalViewModel(lifeTrackApplication().container.goalRepository) }
        initializer { PeriodViewModel(lifeTrackApplication().container.periodRepository) }
        initializer {
            val container = lifeTrackApplication().container
            DiaryViewModel(
                diaryRepository = container.diaryRepository,
                habitRepository = container.habitRepository,
                expenseRepository = container.expenseRepository,
                waterRepository = container.waterRepository,
                calorieRepository = container.calorieRepository,
            )
        }
    }
}

private fun CreationExtras.lifeTrackApplication(): LifeTrackApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LifeTrackApplication
