package com.lifetrack.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.core.data.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Milestone-1 dashboard state. This exists mainly to prove the Room stack works
 * end-to-end at runtime — the real dashboard (PRD 7.1) lands in milestone 3 and
 * will replace these counts with today's actual habit/calorie/water/spend data.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val habitCount: Int = 0,
    val goalCount: Int = 0,
    val expenseCount: Int = 0,
    val diaryCount: Int = 0,
    val calorieTarget: Int = 0,
    val waterTargetMl: Int = 0,
    val reminderCount: Int = 0,
)

class DashboardViewModel(container: AppContainer) : ViewModel() {

    private val counts = combine(
        container.habitDao.observeHabitCount(),
        container.goalDao.observeGoalCount(),
        container.expenseDao.observeExpenseCount(),
        container.diaryDao.observeEntryCount(),
        container.notificationSettingsDao.observeCount(),
    ) { habits, goals, expenses, diary, reminders ->
        intArrayOf(habits, goals, expenses, diary, reminders)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        counts,
        container.calorieDao.observeGoal(),
        container.waterDao.observeGoal(),
    ) { c, calorieGoal, waterGoal ->
        DashboardUiState(
            isLoading = false,
            habitCount = c[0],
            goalCount = c[1],
            expenseCount = c[2],
            diaryCount = c[3],
            reminderCount = c[4],
            calorieTarget = calorieGoal?.dailyTarget ?: 0,
            waterTargetMl = waterGoal?.dailyTargetMl ?: 0,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )
}
