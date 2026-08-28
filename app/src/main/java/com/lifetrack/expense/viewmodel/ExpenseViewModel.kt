package com.lifetrack.expense.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.expense.data.Expense
import com.lifetrack.expense.data.ExpenseCategories
import com.lifetrack.expense.data.ExpenseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Which totals window the screen is showing. PRD 7.4: daily / weekly / monthly. */
enum class SpendWindow { DAY, WEEK, MONTH }

data class CategoryTotal(val category: String, val total: Double)

data class SpendPoint(val label: String, val total: Double)

data class ExpenseUiState(
    val isLoading: Boolean = true,
    val window: SpendWindow = SpendWindow.WEEK,
    val windowTotal: Double = 0.0,
    val todayTotal: Double = 0.0,
    val recent: List<Expense> = emptyList(),
    val byCategory: List<CategoryTotal> = emptyList(),
    val overTime: List<SpendPoint> = emptyList(),
    val knownCategories: List<String> = ExpenseCategories.PRESETS,
) {
    val isEmpty: Boolean get() = !isLoading && recent.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())
    private val window = MutableStateFlow(SpendWindow.WEEK)

    val uiState: StateFlow<ExpenseUiState> = combine(
        repository.observeExpenses(),
        window,
        today,
    ) { expenses, window, today ->
        val zone = ZoneId.systemDefault()
        val range = window.rangeEnding(today)

        val inWindow = expenses.filter { it.localDate(zone) in range }

        ExpenseUiState(
            isLoading = false,
            window = window,
            windowTotal = inWindow.sumOf { it.amount },
            todayTotal = expenses.filter { it.localDate(zone) == today }.sumOf { it.amount },
            recent = expenses.take(RECENT_LIMIT),
            byCategory = inWindow
                .groupBy { it.category }
                .map { (category, rows) -> CategoryTotal(category, rows.sumOf { it.amount }) }
                .sortedByDescending { it.total },
            overTime = spendOverTime(expenses, window, today, zone),
            knownCategories = ExpenseCategories.allKnown(expenses.map { it.category }),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpenseUiState(),
    )

    /**
     * A point per bucket across the window: days for DAY/WEEK, days for MONTH too —
     * PRD 7.4 asks for "spend over time", and a daily line is the honest resolution.
     */
    private fun spendOverTime(
        expenses: List<Expense>,
        window: SpendWindow,
        today: LocalDate,
        zone: ZoneId,
    ): List<SpendPoint> {
        val range = window.rangeEnding(today)
        val byDate = expenses.groupBy { it.localDate(zone) }
        val points = mutableListOf<SpendPoint>()
        var cursor = range.start
        while (!cursor.isAfter(range.endInclusive)) {
            points += SpendPoint(
                label = cursor.dayOfMonth.toString(),
                total = byDate[cursor].orEmpty().sumOf { it.amount },
            )
            cursor = cursor.plusDays(1)
        }
        return points
    }

    fun setWindow(value: SpendWindow) {
        window.value = value
    }

    fun refreshDate() {
        val now = LocalDate.now()
        if (now != today.value) today.value = now
    }

    fun add(amount: Double, category: String, note: String?) {
        viewModelScope.launch { repository.add(amount, category, note) }
    }

    fun delete(expense: Expense) {
        viewModelScope.launch { repository.delete(expense) }
    }

    private companion object {
        const val RECENT_LIMIT = 25
    }
}

private fun Expense.localDate(zone: ZoneId): LocalDate =
    timestamp.atZone(zone).toLocalDate()

/** Inclusive local-date range ending today. */
fun SpendWindow.rangeEnding(today: LocalDate): ClosedRange<LocalDate> = when (this) {
    SpendWindow.DAY -> today..today
    SpendWindow.WEEK -> today.minusDays(6)..today
    SpendWindow.MONTH -> YearMonth.from(today).atDay(1)..today
}
