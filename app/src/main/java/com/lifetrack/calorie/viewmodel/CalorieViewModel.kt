package com.lifetrack.calorie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.calorie.data.CalorieGoal
import com.lifetrack.calorie.data.CalorieLog
import com.lifetrack.calorie.data.CalorieRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Chart span, per PRD 7.5's "past 7/30 days". */
enum class CalorieSpan(val days: Int) { WEEK(7), MONTH(30) }

data class DayCalories(val date: LocalDate, val total: Int)

data class CalorieUiState(
    val isLoading: Boolean = true,
    val target: Int = CalorieGoal.DEFAULT_DAILY_TARGET,
    val eatenToday: Int = 0,
    val todaysLogs: List<CalorieLog> = emptyList(),
    val span: CalorieSpan = CalorieSpan.WEEK,
    val history: List<DayCalories> = emptyList(),
) {
    /** Clamped for the progress bar; [isOverTarget] carries the overshoot instead. */
    val progress: Float get() = if (target <= 0) 0f else (eatenToday.toFloat() / target).coerceIn(0f, 1f)
    val remaining: Int get() = target - eatenToday
    val isOverTarget: Boolean get() = eatenToday > target
}

@OptIn(ExperimentalCoroutinesApi::class)
class CalorieViewModel(private val repository: CalorieRepository) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())
    private val span = MutableStateFlow(CalorieSpan.WEEK)

    val uiState: StateFlow<CalorieUiState> = combine(
        combine(today, span) { today, span -> today to span }
            .flatMapLatest { (today, span) ->
                repository.observeLogsBetween(today.minusDays(span.days - 1L), today)
            },
        repository.observeGoal(),
        today,
        span,
    ) { logs, goal, today, span ->
        val zone = ZoneId.systemDefault()
        val byDate = logs.groupBy { it.timestamp.atZone(zone).toLocalDate() }

        CalorieUiState(
            isLoading = false,
            target = goal?.dailyTarget ?: CalorieGoal.DEFAULT_DAILY_TARGET,
            eatenToday = byDate[today].orEmpty().sumOf { it.calories },
            todaysLogs = byDate[today].orEmpty().sortedByDescending { it.timestamp },
            span = span,
            history = (span.days - 1 downTo 0).map { back ->
                val date = today.minusDays(back.toLong())
                DayCalories(date, byDate[date].orEmpty().sumOf { it.calories })
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalorieUiState(),
    )

    fun setSpan(value: CalorieSpan) {
        span.value = value
    }

    fun refreshDate() {
        val now = LocalDate.now()
        if (now != today.value) today.value = now
    }

    fun add(foodName: String, calories: Int) {
        viewModelScope.launch { repository.add(foodName, calories) }
    }

    fun delete(log: CalorieLog) {
        viewModelScope.launch { repository.delete(log) }
    }

    fun setTarget(target: Int) {
        viewModelScope.launch { repository.setDailyTarget(target) }
    }
}
