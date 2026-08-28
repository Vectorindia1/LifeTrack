package com.lifetrack.habit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.habit.data.FrequencyType
import com.lifetrack.habit.data.Habit
import com.lifetrack.habit.data.HabitRepository
import com.lifetrack.habit.data.HabitSchedule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One habit as the checklist renders it. */
data class HabitItem(
    val habit: Habit,
    val isDoneToday: Boolean,
    val isScheduledToday: Boolean,
    val streak: Int,
    /** This calendar week's completed dates, for the day-dot row. */
    val completedThisWeek: Set<LocalDate> = emptySet(),
)

/** One bar in the completion-rate chart. */
data class RateBar(
    val label: String,
    val rate: Float,
)

enum class ChartWindow { WEEKS, MONTHS }

data class HabitUiState(
    val isLoading: Boolean = true,
    val items: List<HabitItem> = emptyList(),
    val chartWindow: ChartWindow = ChartWindow.WEEKS,
    val bars: List<RateBar> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModel(private val repository: HabitRepository) : ViewModel() {

    private val chartWindow = MutableStateFlow(ChartWindow.WEEKS)

    /** State, not a captured constant, so crossing midnight is handled. See [refreshDate]. */
    private val todayFlow = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<HabitUiState> = combine(
        repository.observeHabits(),
        todayFlow.flatMapLatest { repository.observeRecentCompletions(it) },
        chartWindow,
        todayFlow,
    ) { habits, logs, window, today ->
        val completedByHabit: Map<Long, Set<LocalDate>> = logs
            .groupBy { it.habitId }
            .mapValues { (_, entries) -> entries.mapTo(mutableSetOf()) { it.date } }

        val weekStart = HabitSchedule.weekStart(today)
        val items = habits.map { habit ->
            val completed = completedByHabit[habit.id].orEmpty()
            HabitItem(
                habit = habit,
                isDoneToday = today in completed,
                isScheduledToday = HabitSchedule.isScheduledOn(habit, today),
                streak = HabitSchedule.currentStreak(habit, completed, today),
                completedThisWeek = completed.filterTo(mutableSetOf()) {
                    !it.isBefore(weekStart) && it.isBefore(weekStart.plusDays(7))
                },
            )
        }

        HabitUiState(
            isLoading = false,
            items = items,
            chartWindow = window,
            bars = buildBars(habits, completedByHabit, window, today),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitUiState(),
    )

    private fun buildBars(
        habits: List<Habit>,
        completedByHabit: Map<Long, Set<LocalDate>>,
        window: ChartWindow,
        today: LocalDate,
    ): List<RateBar> {
        if (habits.isEmpty()) return emptyList()
        val ranges = when (window) {
            ChartWindow.WEEKS -> HabitSchedule.recentWeeks(today, WEEK_BARS)
            ChartWindow.MONTHS -> HabitSchedule.recentMonths(today, MONTH_BARS)
        }
        return ranges.map { range ->
            val rate = HabitSchedule.aggregateRate(
                habits = habits,
                completedByHabit = completedByHabit,
                from = range.start,
                to = range.endInclusive,
                today = today,
            )
            RateBar(
                label = when (window) {
                    ChartWindow.WEEKS -> range.start.dayOfMonth.toString()
                    ChartWindow.MONTHS -> range.start.month.value.toString()
                },
                rate = rate ?: 0f,
            )
        }
    }

    /** Call when the screen resumes, so the date is right after midnight. */
    fun refreshDate() {
        val now = LocalDate.now()
        if (now != todayFlow.value) todayFlow.value = now
    }

    fun setChartWindow(window: ChartWindow) {
        chartWindow.value = window
    }

    /** One tap from the checklist. Writes the log, then refreshes the cached streak. */
    fun toggle(item: HabitItem) {
        viewModelScope.launch {
            repository.setCompleted(item.habit.id, todayFlow.value, !item.isDoneToday)
        }
    }

    fun addHabit(
        name: String,
        frequencyType: FrequencyType,
        daysOfWeekMask: Int,
        timesPerWeek: Int?,
    ) {
        viewModelScope.launch {
            repository.addHabit(name, frequencyType, daysOfWeekMask, timesPerWeek)
        }
    }

    fun delete(habit: Habit) {
        viewModelScope.launch { repository.delete(habit) }
    }

    private companion object {
        const val WEEK_BARS = 8
        const val MONTH_BARS = 6
    }
}
