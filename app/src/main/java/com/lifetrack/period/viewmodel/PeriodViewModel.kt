package com.lifetrack.period.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.period.data.CycleStats
import com.lifetrack.period.data.PeriodLog
import com.lifetrack.period.data.PeriodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PeriodUiState(
    val isLoading: Boolean = true,
    val logs: List<PeriodLog> = emptyList(),
    val currentCycleDay: Int? = null,
    val averageCycleLength: Double? = null,
) {
    val isEmpty: Boolean get() = !isLoading && logs.isEmpty()
}

class PeriodViewModel(private val repository: PeriodRepository) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<PeriodUiState> = combine(
        repository.observeAll(),
        today,
    ) { logs, today ->
        PeriodUiState(
            isLoading = false,
            logs = logs,
            currentCycleDay = CycleStats.currentCycleDay(logs, today),
            averageCycleLength = CycleStats.averageCycleLength(logs),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PeriodUiState(),
    )

    fun refreshDate() {
        val now = LocalDate.now()
        if (now != today.value) today.value = now
    }

    /** One tap to log a period starting today — the common case. */
    fun logToday() {
        viewModelScope.launch { repository.logStart(today.value) }
    }

    fun logStart(date: LocalDate) {
        viewModelScope.launch { repository.logStart(date) }
    }

    fun setEndDate(log: PeriodLog, endDate: LocalDate?) {
        viewModelScope.launch { repository.setEndDate(log, endDate) }
    }

    fun delete(log: PeriodLog) {
        viewModelScope.launch { repository.delete(log) }
    }
}
