package com.lifetrack.goal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.goal.data.Goal
import com.lifetrack.goal.data.GoalProgress
import com.lifetrack.goal.data.GoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One goal as the list renders it. */
data class GoalItem(
    val goal: Goal,
    val fraction: Float,
    val daysRemaining: Long?,
    val isComplete: Boolean,
    val isOverdue: Boolean,
)

data class GoalUiState(
    val isLoading: Boolean = true,
    val active: List<GoalItem> = emptyList(),
    val completed: List<GoalItem> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && active.isEmpty() && completed.isEmpty()
}

class GoalViewModel(private val repository: GoalRepository) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<GoalUiState> = combine(
        repository.observeGoals(),
        today,
    ) { goals, today ->
        val items = GoalProgress.byUrgency(goals).map { it.toItem(today) }
        GoalUiState(
            isLoading = false,
            active = items.filterNot { it.isComplete },
            // Finished goals move to their own section rather than vanishing —
            // seeing what you finished is the point of tracking goals.
            completed = items.filter { it.isComplete },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalUiState(),
    )

    fun refreshDate() {
        val now = LocalDate.now()
        if (now != today.value) today.value = now
    }

    fun add(name: String, targetValue: Double, unit: String, deadline: LocalDate?) {
        viewModelScope.launch { repository.add(name, targetValue, unit, deadline) }
    }

    fun setProgress(goal: Goal, value: Double) {
        viewModelScope.launch { repository.setProgress(goal, value) }
    }

    fun increment(goal: Goal, by: Double) {
        viewModelScope.launch { repository.increment(goal, by) }
    }

    fun delete(goal: Goal) {
        viewModelScope.launch { repository.delete(goal) }
    }
}

fun Goal.toItem(today: LocalDate): GoalItem = GoalItem(
    goal = this,
    fraction = GoalProgress.fraction(this),
    daysRemaining = GoalProgress.daysRemaining(this, today),
    isComplete = GoalProgress.isComplete(this),
    isOverdue = GoalProgress.isOverdue(this, today),
)
