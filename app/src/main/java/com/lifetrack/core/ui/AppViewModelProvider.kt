package com.lifetrack.core.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lifetrack.LifeTrackApplication
import com.lifetrack.dashboard.viewmodel.DashboardViewModel
import com.lifetrack.calorie.viewmodel.CalorieViewModel
import com.lifetrack.expense.viewmodel.ExpenseViewModel
import com.lifetrack.goal.viewmodel.GoalViewModel
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
            )
        }
        initializer { HabitViewModel(lifeTrackApplication().container.habitRepository) }
        initializer { ExpenseViewModel(lifeTrackApplication().container.expenseRepository) }
        initializer { CalorieViewModel(lifeTrackApplication().container.calorieRepository) }
        initializer { WaterViewModel(lifeTrackApplication().container.waterRepository) }
        initializer { GoalViewModel(lifeTrackApplication().container.goalRepository) }
    }
}

private fun CreationExtras.lifeTrackApplication(): LifeTrackApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LifeTrackApplication
