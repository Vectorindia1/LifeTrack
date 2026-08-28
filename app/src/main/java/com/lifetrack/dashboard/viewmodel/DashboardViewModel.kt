package com.lifetrack.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.habit.data.HabitRepository
import com.lifetrack.habit.data.HabitSchedule
import com.lifetrack.habit.viewmodel.HabitItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val isLoading: Boolean = true,
    val date: LocalDate = LocalDate.now(),
    /** Only habits actually due today — an unscheduled habit is not a chore today. */
    val habitsDueToday: List<HabitItem> = emptyList(),
    val hasAnyHabit: Boolean = false,
) {
    val doneCount: Int get() = habitsDueToday.count { it.isDoneToday }
    val dueCount: Int get() = habitsDueToday.size
    val allDone: Boolean get() = dueCount > 0 && doneCount == dueCount
}

/**
 * Dashboard v1 — habits only, per PRD milestone 3. The calorie, water, spend, goal
 * and diary sections of PRD 7.1 arrive with milestones 4–8.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val repository: HabitRepository) : ViewModel() {

    /**
     * Held as state rather than captured once, so a process left open across
     * midnight shows the new day when the screen resumes. See [refreshDate].
     */
    private val today = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeHabits(),
        today.flatMapLatest { repository.observeRecentCompletions(it) },
        today,
    ) { habits, logs, date ->
        val completedByHabit = logs
            .groupBy { it.habitId }
            .mapValues { (_, entries) -> entries.mapTo(mutableSetOf()) { it.date } }

        val due = habits
            .filter { HabitSchedule.isScheduledOn(it, date) }
            .map { habit ->
                val completed = completedByHabit[habit.id].orEmpty()
                HabitItem(
                    habit = habit,
                    isDoneToday = date in completed,
                    isScheduledToday = true,
                    streak = HabitSchedule.currentStreak(habit, completed, date),
                )
            }

        DashboardUiState(
            isLoading = false,
            date = date,
            habitsDueToday = due,
            hasAnyHabit = habits.isNotEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    /** Call when the screen resumes, so the date is right after midnight. */
    fun refreshDate() {
        val now = LocalDate.now()
        if (now != today.value) today.value = now
    }

    /** One tap, straight from the dashboard — PRD 8's ≤2-tap rule. */
    fun toggle(item: HabitItem) {
        viewModelScope.launch {
            repository.setCompleted(item.habit.id, today.value, !item.isDoneToday)
        }
    }
}
