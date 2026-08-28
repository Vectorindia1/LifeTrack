package com.lifetrack.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.calorie.data.CalorieGoal
import com.lifetrack.calorie.data.CalorieLog
import com.lifetrack.calorie.data.CalorieRepository
import com.lifetrack.expense.data.Expense
import com.lifetrack.expense.data.ExpenseRepository
import com.lifetrack.habit.data.HabitLog
import com.lifetrack.habit.data.HabitRepository
import com.lifetrack.habit.data.HabitSchedule
import com.lifetrack.habit.viewmodel.HabitItem
import com.lifetrack.water.data.WaterGoal
import com.lifetrack.water.data.WaterLog
import com.lifetrack.water.data.WaterRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
    val spentToday: Double = 0.0,
    val caloriesEaten: Int = 0,
    val calorieTarget: Int = CalorieGoal.DEFAULT_DAILY_TARGET,
    val waterDrunkMl: Int = 0,
    val waterTargetMl: Int = WaterGoal.DEFAULT_DAILY_TARGET_ML,
) {
    val doneCount: Int get() = habitsDueToday.count { it.isDoneToday }
    val dueCount: Int get() = habitsDueToday.size
    val allDone: Boolean get() = dueCount > 0 && doneCount == dueCount

    val calorieProgress: Float
        get() = if (calorieTarget <= 0) 0f else (caloriesEaten.toFloat() / calorieTarget).coerceIn(0f, 1f)
    val isOverCalories: Boolean get() = caloriesEaten > calorieTarget

    val waterProgress: Float
        get() = if (waterTargetMl <= 0) 0f else (waterDrunkMl.toFloat() / waterTargetMl).coerceIn(0f, 1f)
    val isWaterGoalMet: Boolean get() = waterDrunkMl >= waterTargetMl
}

/**
 * Everything the dashboard needs for one particular day, gathered in one place.
 *
 * This exists because `combine`'s typed overloads stop at five flows and the
 * dashboard now aggregates more than that. Grouping the per-day sources here keeps
 * the top-level combine to two arguments and leaves room for goals and diary in
 * milestones 7 and 8.
 */
private data class DayData(
    val completions: List<HabitLog> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val food: List<CalorieLog> = emptyList(),
    val water: List<WaterLog> = emptyList(),
    val calorieGoal: CalorieGoal? = null,
    val waterGoal: WaterGoal? = null,
)

/**
 * Dashboard v2 — habits, calories, spend and water. Goals and diary arrive with
 * milestones 7 and 8.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val habitRepository: HabitRepository,
    private val expenseRepository: ExpenseRepository,
    private val calorieRepository: CalorieRepository,
    private val waterRepository: WaterRepository,
) : ViewModel() {

    /**
     * Held as state rather than captured once, so a process left open across
     * midnight shows the new day when the screen resumes. See [refreshDate].
     */
    private val today = MutableStateFlow(LocalDate.now())

    private val dayData: Flow<Pair<LocalDate, DayData>> = today.flatMapLatest { date ->
        combine(
            habitRepository.observeRecentCompletions(date),
            expenseRepository.observeBetween(date, date),
            calorieRepository.observeLogsBetween(date, date),
            waterRepository.observeLogsBetween(date, date),
            combine(
                calorieRepository.observeGoal(),
                waterRepository.observeGoal(),
            ) { calorieGoal, waterGoal -> calorieGoal to waterGoal },
        ) { completions, expenses, food, water, (calorieGoal, waterGoal) ->
            date to DayData(completions, expenses, food, water, calorieGoal, waterGoal)
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        habitRepository.observeHabits(),
        dayData,
    ) { habits, (date, data) ->
        val completedByHabit = data.completions
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
            spentToday = data.expenses.sumOf { it.amount },
            caloriesEaten = data.food.sumOf { it.calories },
            calorieTarget = data.calorieGoal?.dailyTarget ?: CalorieGoal.DEFAULT_DAILY_TARGET,
            waterDrunkMl = data.water.sumOf { it.mlAmount },
            waterTargetMl = data.waterGoal?.dailyTargetMl ?: WaterGoal.DEFAULT_DAILY_TARGET_ML,
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
            habitRepository.setCompleted(item.habit.id, today.value, !item.isDoneToday)
        }
    }

    /** Quick-add water without leaving the dashboard — PRD 7.1's +250/+500 buttons. */
    fun addWater(mlAmount: Int) {
        viewModelScope.launch { waterRepository.add(mlAmount) }
    }
}
