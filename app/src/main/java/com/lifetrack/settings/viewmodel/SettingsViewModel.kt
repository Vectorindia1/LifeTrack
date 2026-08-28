package com.lifetrack.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.calorie.data.CalorieGoal
import com.lifetrack.calorie.data.CalorieRepository
import com.lifetrack.core.data.AppPreferences
import com.lifetrack.core.data.PreferencesRepository
import com.lifetrack.core.data.ThemeMode
import com.lifetrack.expense.data.CategoryUsage
import com.lifetrack.expense.data.ExpenseRepository
import com.lifetrack.habit.data.Habit
import com.lifetrack.habit.data.HabitRepository
import com.lifetrack.notification.data.FeatureType
import com.lifetrack.notification.data.NotificationSettings
import com.lifetrack.notification.data.NotificationSettingsRepository
import com.lifetrack.notification.work.DigestRunner
import com.lifetrack.notification.work.DigestScheduler
import com.lifetrack.water.data.WaterGoal
import com.lifetrack.water.data.WaterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

data class SettingsUiState(
    val isLoading: Boolean = true,
    val preferences: AppPreferences = AppPreferences(),
    val calorieTarget: Int = CalorieGoal.DEFAULT_DAILY_TARGET,
    val waterTargetMl: Int = WaterGoal.DEFAULT_DAILY_TARGET_ML,
    val reminders: List<NotificationSettings> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val categories: List<CategoryUsage> = emptyList(),
) {
    /** Reminders grouped for display; water legitimately has two rows. */
    val remindersByFeature: Map<FeatureType, List<NotificationSettings>>
        get() = reminders.groupBy { it.featureType }
}

class SettingsViewModel(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val calorieRepository: CalorieRepository,
    private val waterRepository: WaterRepository,
    private val notificationRepository: NotificationSettingsRepository,
    private val habitRepository: HabitRepository,
    private val expenseRepository: ExpenseRepository,
    private val backupRepository: com.lifetrack.backup.data.BackupRepository,
) : ViewModel() {

    private val targets = combine(
        calorieRepository.observeGoal(),
        waterRepository.observeGoal(),
    ) { calorie, water -> calorie to water }

    private val management = combine(
        habitRepository.observeHabits(),
        expenseRepository.observeCategoryUsage(),
    ) { habits, categories -> habits to categories }

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        targets,
        notificationRepository.observeAll(),
        management,
    ) { preferences, (calorieGoal, waterGoal), reminders, (habits, categories) ->
        SettingsUiState(
            isLoading = false,
            preferences = preferences,
            calorieTarget = calorieGoal?.dailyTarget ?: CalorieGoal.DEFAULT_DAILY_TARGET,
            waterTargetMl = waterGoal?.dailyTargetMl ?: WaterGoal.DEFAULT_DAILY_TARGET_ML,
            reminders = reminders,
            habits = habits,
            categories = categories,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setDisplayName(name: String?) {
        viewModelScope.launch { preferencesRepository.setDisplayName(name) }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setCurrency(option: com.lifetrack.core.ui.CurrencyOption) {
        viewModelScope.launch { preferencesRepository.setCurrencyLocaleTag(option.tag) }
    }

    /** Builds the full-database backup JSON and hands it to the caller to write to disk. */
    fun exportBackup(onExported: (String) -> Unit) {
        viewModelScope.launch { onExported(backupRepository.export()) }
    }

    /** @param onResult a human-readable error, or null on success — see [BackupRepository.import]. */
    fun importBackup(json: String, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(backupRepository.import(json)) }
    }

    /** Re-applies scheduling immediately, same as a reminder-time edit does for the digest. */
    fun setWaterReminder(enabled: Boolean, intervalMinutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setWaterReminder(enabled, intervalMinutes)
            com.lifetrack.notification.work.WaterReminderScheduler.apply(context)
        }
    }

    fun setWaterIncrements(smallMl: Int, largeMl: Int) {
        viewModelScope.launch { preferencesRepository.setWaterIncrements(smallMl, largeMl) }
    }

    fun setCalorieTarget(target: Int) {
        viewModelScope.launch { calorieRepository.setDailyTarget(target) }
    }

    fun setWaterTarget(targetMl: Int) {
        viewModelScope.launch { waterRepository.setDailyTargetMl(targetMl) }
    }

    /**
     * Every reminder change re-arms the scheduler, so a new time takes effect from the
     * next check rather than the next app launch.
     */
    fun setReminderEnabled(feature: FeatureType, enabled: Boolean) {
        viewModelScope.launch {
            notificationRepository.setEnabled(feature, enabled)
            DigestScheduler.scheduleNext(context)
        }
    }

    fun setReminderTime(settings: NotificationSettings, time: LocalTime) {
        viewModelScope.launch {
            notificationRepository.upsert(settings.copy(reminderTime = time))
            DigestScheduler.scheduleNext(context)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch { habitRepository.delete(habit) }
    }

    fun renameCategory(from: String, to: String) {
        viewModelScope.launch { expenseRepository.renameCategory(from, to) }
    }

    /**
     * Runs the exact same digest logic the scheduled worker uses, but ignoring the
     * "has this feature's reminder time passed yet" gate — so a user can see the
     * notification pipeline actually works right now, instead of waiting for the
     * next scheduled check (which, on a fresh install late in the day, can be
     * tomorrow morning). See MEMORY.md for why this exists.
     */
    fun sendTestNotification() {
        viewModelScope.launch { DigestRunner.run(context, ignoreTiming = true) }
    }
}
