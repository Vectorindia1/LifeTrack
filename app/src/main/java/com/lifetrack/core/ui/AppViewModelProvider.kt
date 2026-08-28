package com.lifetrack.core.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lifetrack.LifeTrackApplication
import com.lifetrack.dashboard.viewmodel.DashboardViewModel
import com.lifetrack.habit.viewmodel.HabitViewModel

/** ViewModel wiring for the manual-DI setup. One entry per ViewModel. */
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer { DashboardViewModel(lifeTrackApplication().container.habitRepository) }
        initializer { HabitViewModel(lifeTrackApplication().container.habitRepository) }
    }
}

private fun CreationExtras.lifeTrackApplication(): LifeTrackApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LifeTrackApplication
