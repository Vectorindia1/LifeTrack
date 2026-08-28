package com.lifetrack.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.calorie.data.CalorieGoal
import com.lifetrack.core.data.AppPreferences
import com.lifetrack.core.data.PreferencesRepository
import com.lifetrack.core.data.effectiveCurrencyLocale
import com.lifetrack.calorie.data.CalorieLog
import com.lifetrack.calorie.data.CalorieRepository
import com.lifetrack.diary.data.DiaryEntry
import com.lifetrack.diary.data.DiaryRepository
import com.lifetrack.diary.data.DiaryStreak
import com.lifetrack.expense.data.Expense
import com.lifetrack.expense.data.ExpenseRepository
import com.lifetrack.habit.data.HabitLog
import com.lifetrack.goal.data.Goal
import com.lifetrack.goal.data.GoalProgress
import com.lifetrack.goal.data.GoalRepository
import com.lifetrack.habit.data.HabitRepository
import com.lifetrack.habit.data.HabitSchedule
import com.lifetrack.goal.viewmodel.GoalItem
import com.lifetrack.goal.viewmodel.toItem
import com.lifetrack.habit.viewmodel.HabitItem
import com.lifetrack.period.data.CycleStats
import com.lifetrack.period.data.PeriodLog
import com.lifetrack.period.data.PeriodRepository
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
    /** Top few active goals, most urgent first. PRD 7.1 asks for 2-3 with a "see all". */
    val topGoals: List<GoalItem> = emptyList(),
    val totalActiveGoals: Int = 0,
    val diaryWrittenToday: Boolean = false,
    val diaryStreak: Int = 0,
    val waterIncrementSmallMl: Int = AppPreferences.DEFAULT_SMALL_ML,
    val waterIncrementLargeMl: Int = AppPreferences.DEFAULT_LARGE_ML,
    val displayName: String? = null,
    val periodCurrentCycleDay: Int? = null,
    val hasPeriodLogs: Boolean = false,
    val currencyLocale: java.util.Locale = java.util.Locale.getDefault(),
) {
    val hasMoreGoals: Boolean get() = totalActiveGoals > topGoals.size
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
 * the top-level combine small. Day-scoped sources belong here; goals do not,
 * because the goal list is the same whatever the date.
 */
private data class DayData(
    val completions: List<HabitLog> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val food: List<CalorieLog> = emptyList(),
    val water: List<WaterLog> = emptyList(),
    val calorieGoal: CalorieGoal? = null,
    val waterGoal: WaterGoal? = null,
    val diaryEntries: List<DiaryEntry> = emptyList(),
    val periodLogs: List<PeriodLog> = emptyList(),
)

/**
 * Groups the sources that aren't day-scoped queries themselves (goal, calorie, water
 * targets; the diary/period lists, which are read in full and filtered client-side)
 * so the inner `combine` inside [DashboardViewModel]'s `flatMapLatest` stays under
 * the 5-flow limit alongside it.
 */
private data class DayExtras(
    val calorieGoal: CalorieGoal?,
    val waterGoal: WaterGoal?,
    val diary: List<DiaryEntry>,
    val periods: List<PeriodLog>,
)

/**
 * The full PRD 7.1 dashboard: habits, water, calories, spend, goals and diary.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val habitRepository: HabitRepository,
    private val expenseRepository: ExpenseRepository,
    private val calorieRepository: CalorieRepository,
    private val waterRepository: WaterRepository,
    private val goalRepository: GoalRepository,
    private val diaryRepository: DiaryRepository,
    private val periodRepository: PeriodRepository,
    preferencesRepository: PreferencesRepository,
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
                diaryRepository.observeRecentEntries(date),
                periodRepository.observeAll(),
            ) { calorieGoal, waterGoal, diary, periods ->
                DayExtras(calorieGoal, waterGoal, diary, periods)
            },
        ) { completions, expenses, food, water, extras ->
            date to DayData(
                completions, expenses, food, water,
                extras.calorieGoal, extras.waterGoal, extras.diary, extras.periods,
            )
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        habitRepository.observeHabits(),
        dayData,
        goalRepository.observeGoals(),
        preferencesRepository.preferences,
    ) { habits, (date, data), goals, preferences ->
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
            topGoals = activeGoals(goals).take(DASHBOARD_GOALS).map { it.toItem(date) },
            totalActiveGoals = activeGoals(goals).size,
            diaryWrittenToday = data.diaryEntries.any { it.date == date },
            diaryStreak = DiaryStreak.current(
                data.diaryEntries.mapTo(mutableSetOf()) { it.date },
                date,
            ),
            waterIncrementSmallMl = preferences.waterIncrementSmallMl,
            waterIncrementLargeMl = preferences.waterIncrementLargeMl,
            displayName = preferences.displayName,
            periodCurrentCycleDay = CycleStats.currentCycleDay(data.periodLogs, date),
            hasPeriodLogs = data.periodLogs.isNotEmpty(),
            currencyLocale = preferences.effectiveCurrencyLocale(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    private fun activeGoals(goals: List<Goal>): List<Goal> =
        GoalProgress.byUrgency(GoalProgress.active(goals))

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

    private companion object {
        /** PRD 7.1: "top 2-3" active goals on the dashboard. */
        const val DASHBOARD_GOALS = 3
    }

    /** Quick-add water without leaving the dashboard — PRD 7.1's +250/+500 buttons. */
    fun addWater(mlAmount: Int) {
        viewModelScope.launch { waterRepository.add(mlAmount) }
    }
}
