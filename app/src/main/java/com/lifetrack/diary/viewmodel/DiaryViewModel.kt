package com.lifetrack.diary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.calorie.data.CalorieRepository
import com.lifetrack.core.data.PreferencesRepository
import com.lifetrack.core.data.effectiveCurrencyLocale
import com.lifetrack.diary.data.DaySummary
import com.lifetrack.diary.data.DiaryEntry
import com.lifetrack.diary.data.DiaryRepository
import com.lifetrack.diary.data.DiaryStreak
import com.lifetrack.diary.data.Mood
import com.lifetrack.expense.data.ExpenseRepository
import com.lifetrack.habit.data.HabitRepository
import com.lifetrack.habit.data.HabitSchedule
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
import java.util.Locale

data class DiaryUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val entry: DiaryEntry? = null,
    val datesWithEntries: Set<LocalDate> = emptySet(),
    val streak: Int = 0,
    /** Stats for [selectedDate], used to prefill a blank entry. */
    val summary: DaySummary = DaySummary(),
    val currencyLocale: Locale = Locale.getDefault(),
) {
    val hasEntry: Boolean get() = entry != null
}

/**
 * The diary is the one screen that reads from every other tracker, because PRD 7.7's
 * summary line quotes them. It depends on the other repositories rather than adding
 * queries of its own.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val habitRepository: HabitRepository,
    private val expenseRepository: ExpenseRepository,
    private val waterRepository: WaterRepository,
    private val calorieRepository: CalorieRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())

    private val summaryFlow: Flow<DaySummary> = selectedDate.flatMapLatest { date ->
        combine(
            habitRepository.observeHabits(),
            habitRepository.observeRecentCompletions(date),
            expenseRepository.observeBetween(date, date),
            waterRepository.observeLogsBetween(date, date),
            calorieRepository.observeLogsBetween(date, date),
        ) { habits, completions, expenses, water, food ->
            val doneDates = completions.filter { it.date == date }.mapTo(mutableSetOf()) { it.habitId }
            val due = habits.filter { HabitSchedule.isScheduledOn(it, date) }
            DaySummary(
                habitsDone = due.count { it.id in doneDates },
                habitsDue = due.size,
                spent = expenses.sumOf { it.amount },
                waterMl = water.sumOf { it.mlAmount },
                calories = food.sumOf { it.calories },
            )
        }
    }

    // Folded together so the outer combine below stays within its 5-flow limit.
    private val summaryAndPreferences = combine(
        summaryFlow,
        preferencesRepository.preferences,
    ) { summary, preferences -> summary to preferences }

    val uiState: StateFlow<DiaryUiState> = combine(
        selectedDate.flatMapLatest { diaryRepository.observeEntryForDate(it) },
        today.flatMapLatest { diaryRepository.observeRecentEntries(it) },
        summaryAndPreferences,
        selectedDate,
        today,
    ) { entry, recent, (summary, preferences), selected, today ->
        val dates = recent.mapTo(mutableSetOf()) { it.date }
        DiaryUiState(
            isLoading = false,
            today = today,
            selectedDate = selected,
            entry = entry,
            datesWithEntries = dates,
            streak = DiaryStreak.current(dates, today),
            summary = summary,
            currencyLocale = preferences.effectiveCurrencyLocale(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiaryUiState(),
    )

    fun selectDate(date: LocalDate) {
        // Writing a diary entry for a day that hasn't happened isn't meaningful.
        if (date.isAfter(today.value)) return
        selectedDate.value = date
    }

    fun refreshDate() {
        val now = LocalDate.now()
        if (now != today.value) {
            val wasOnToday = selectedDate.value == today.value
            today.value = now
            // Only follow the clock if the user was looking at "today" already.
            if (wasOnToday) selectedDate.value = now
        }
    }

    fun save(text: String, mood: Mood?) {
        val date = selectedDate.value
        val existing = uiState.value.entry
        viewModelScope.launch {
            diaryRepository.save(existing, date, text, mood)
        }
    }

    fun delete() {
        val entry = uiState.value.entry ?: return
        viewModelScope.launch { diaryRepository.delete(entry) }
    }
}
