package com.lifetrack.water.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.water.data.WaterGoal
import com.lifetrack.water.data.WaterLog
import com.lifetrack.water.data.WaterRepository
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

data class DayWater(val date: LocalDate, val ml: Int)

data class WaterUiState(
    val isLoading: Boolean = true,
    val targetMl: Int = WaterGoal.DEFAULT_DAILY_TARGET_ML,
    val drunkToday: Int = 0,
    val todaysLogs: List<WaterLog> = emptyList(),
    val week: List<DayWater> = emptyList(),
) {
    val progress: Float
        get() = if (targetMl <= 0) 0f else (drunkToday.toFloat() / targetMl).coerceIn(0f, 1f)
    val remainingMl: Int get() = (targetMl - drunkToday).coerceAtLeast(0)
    val isGoalMet: Boolean get() = drunkToday >= targetMl
}

@OptIn(ExperimentalCoroutinesApi::class)
class WaterViewModel(private val repository: WaterRepository) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<WaterUiState> = combine(
        today.flatMapLatest { repository.observeLogsBetween(it.minusDays(6), it) },
        repository.observeGoal(),
        today,
    ) { logs, goal, today ->
        val zone = ZoneId.systemDefault()
        val byDate = logs.groupBy { it.timestamp.atZone(zone).toLocalDate() }

        WaterUiState(
            isLoading = false,
            targetMl = goal?.dailyTargetMl ?: WaterGoal.DEFAULT_DAILY_TARGET_ML,
            drunkToday = byDate[today].orEmpty().sumOf { it.mlAmount },
            todaysLogs = byDate[today].orEmpty().sortedByDescending { it.timestamp },
            week = (6 downTo 0).map { back ->
                val date = today.minusDays(back.toLong())
                DayWater(date, byDate[date].orEmpty().sumOf { it.mlAmount })
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WaterUiState(),
    )

    fun refreshDate() {
        val now = LocalDate.now()
        if (now != today.value) today.value = now
    }

    fun add(mlAmount: Int) {
        viewModelScope.launch { repository.add(mlAmount) }
    }

    /** Undo a mis-tap by removing today's most recent drink. */
    fun undoLast() {
        val last = uiState.value.todaysLogs.firstOrNull() ?: return
        viewModelScope.launch { repository.delete(last) }
    }

    fun delete(log: WaterLog) {
        viewModelScope.launch { repository.delete(log) }
    }

    fun setTargetMl(target: Int) {
        viewModelScope.launch { repository.setDailyTargetMl(target) }
    }

    companion object {
        /** PRD 7.6's quick-add amounts. */
        const val QUICK_SMALL_ML = 250
        const val QUICK_LARGE_ML = 500
    }
}
