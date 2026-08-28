package com.lifetrack.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifetrack.R
import com.lifetrack.core.data.ThemeMode
import com.lifetrack.core.ui.AppViewModelProvider
import com.lifetrack.expense.data.CategoryUsage
import com.lifetrack.habit.data.Habit
import com.lifetrack.notification.data.FeatureType
import com.lifetrack.notification.data.NotificationSettings
import com.lifetrack.settings.viewmodel.SettingsUiState
import com.lifetrack.settings.viewmodel.SettingsViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var editingNumber by remember { mutableStateOf<NumberEdit?>(null) }
    var editingTime by remember { mutableStateOf<NotificationSettings?>(null) }
    var renaming by remember { mutableStateOf<CategoryUsage?>(null) }

    SettingsContent(
        uiState = uiState,
        onEditNumber = { editingNumber = it },
        onToggleReminder = viewModel::setReminderEnabled,
        onEditTime = { editingTime = it },
        onDeleteHabit = viewModel::deleteHabit,
        onRenameCategory = { renaming = it },
        onTheme = viewModel::setTheme,
        contentPadding = contentPadding,
        modifier = modifier,
    )

    editingNumber?.let { edit ->
        NumberDialog(
            edit = edit,
            onDismiss = { editingNumber = null },
            onConfirm = { value ->
                when (edit.kind) {
                    NumberKind.CALORIE_TARGET -> viewModel.setCalorieTarget(value)
                    NumberKind.WATER_TARGET -> viewModel.setWaterTarget(value)
                    NumberKind.INCREMENT_SMALL -> viewModel.setWaterIncrements(
                        value,
                        uiState.preferences.waterIncrementLargeMl,
                    )
                    NumberKind.INCREMENT_LARGE -> viewModel.setWaterIncrements(
                        uiState.preferences.waterIncrementSmallMl,
                        value,
                    )
                }
                editingNumber = null
            },
        )
    }

    editingTime?.let { settings ->
        ReminderTimeDialog(
            settings = settings,
            onDismiss = { editingTime = null },
            onConfirm = { time ->
                viewModel.setReminderTime(settings, time)
                editingTime = null
            },
        )
    }

    renaming?.let { usage ->
        RenameCategoryDialog(
            usage = usage,
            onDismiss = { renaming = null },
            onConfirm = { newName ->
                viewModel.renameCategory(usage.category, newName)
                renaming = null
            },
        )
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onEditNumber: (NumberEdit) -> Unit,
    onToggleReminder: (FeatureType, Boolean) -> Unit,
    onEditTime: (NotificationSettings) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onRenameCategory: (CategoryUsage) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
            start = 16.dp,
            end = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        item {
            SettingsCard(stringResource(R.string.settings_targets)) {
                ValueRow(
                    label = stringResource(R.string.settings_calorie_target),
                    value = stringResource(R.string.settings_kcal_value, uiState.calorieTarget),
                    onClick = {
                        onEditNumber(NumberEdit(NumberKind.CALORIE_TARGET, uiState.calorieTarget))
                    },
                )
                ValueRow(
                    label = stringResource(R.string.settings_water_target),
                    value = stringResource(R.string.settings_ml_value, uiState.waterTargetMl),
                    onClick = {
                        onEditNumber(NumberEdit(NumberKind.WATER_TARGET, uiState.waterTargetMl))
                    },
                )
            }
        }

        item {
            SettingsCard(stringResource(R.string.settings_water_increments)) {
                ValueRow(
                    label = stringResource(R.string.settings_increment_small),
                    value = stringResource(
                        R.string.settings_ml_value,
                        uiState.preferences.waterIncrementSmallMl,
                    ),
                    onClick = {
                        onEditNumber(
                            NumberEdit(
                                NumberKind.INCREMENT_SMALL,
                                uiState.preferences.waterIncrementSmallMl,
                            ),
                        )
                    },
                )
                ValueRow(
                    label = stringResource(R.string.settings_increment_large),
                    value = stringResource(
                        R.string.settings_ml_value,
                        uiState.preferences.waterIncrementLargeMl,
                    ),
                    onClick = {
                        onEditNumber(
                            NumberEdit(
                                NumberKind.INCREMENT_LARGE,
                                uiState.preferences.waterIncrementLargeMl,
                            ),
                        )
                    },
                )
            }
        }

        item {
            SettingsCard(stringResource(R.string.settings_notifications)) {
                Text(
                    text = stringResource(R.string.settings_notifications_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                FeatureType.entries.forEach { feature ->
                    val rows = uiState.remindersByFeature[feature].orEmpty()
                    ReminderRow(
                        feature = feature,
                        rows = rows,
                        onToggle = { onToggleReminder(feature, it) },
                        onEditTime = onEditTime,
                    )
                }
            }
        }

        item {
            SettingsCard(stringResource(R.string.settings_habits)) {
                if (uiState.habits.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_no_habits),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    uiState.habits.forEach { habit ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { onDeleteHabit(habit) }) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = stringResource(R.string.settings_delete),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(stringResource(R.string.settings_categories)) {
                if (uiState.categories.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_no_categories),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    uiState.categories.forEach { usage ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = usage.category,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.settings_category_count,
                                        usage.count,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { onRenameCategory(usage) }) {
                                Text(stringResource(R.string.settings_rename))
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(stringResource(R.string.settings_theme)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.preferences.themeMode == mode,
                            onClick = { onTheme(mode) },
                            label = { Text(stringResource(mode.labelRes())) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onClick) { Text(stringResource(R.string.settings_edit)) }
    }
}

@Composable
private fun ReminderRow(
    feature: FeatureType,
    rows: List<NotificationSettings>,
    onToggle: (Boolean) -> Unit,
    onEditTime: (NotificationSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val enabled = rows.any { it.enabled }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(feature.labelRes()),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Water legitimately has two reminder times; each is edited separately.
            rows.forEach { settings ->
                TextButton(onClick = { onEditTime(settings) }) {
                    Text(settings.reminderTime.format(formatter))
                }
            }
        }
    }
}

private enum class NumberKind { CALORIE_TARGET, WATER_TARGET, INCREMENT_SMALL, INCREMENT_LARGE }

private data class NumberEdit(val kind: NumberKind, val current: Int)

@Composable
private fun NumberDialog(
    edit: NumberEdit,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(edit.current.toString()) }
    val parsed = text.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(edit.kind.labelRes())) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input -> if (input.all { it.isDigit() }) text = input },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(enabled = parsed != null && parsed > 0, onClick = { onConfirm(parsed!!) }) {
                Text(stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    settings: NotificationSettings,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = settings.reminderTime.hour,
        initialMinute = settings.reminderTime.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(settings.featureType.labelRes())) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) }
        },
    )
}

@Composable
private fun RenameCategoryDialog(
    usage: CategoryUsage,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(usage.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_rename_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.settings_rename_label)) },
                    singleLine = true,
                )
                Text(
                    text = stringResource(R.string.settings_rename_note, usage.count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank() && text != usage.category,
                onClick = { onConfirm(text) },
            ) {
                Text(stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) }
        },
    )
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

private fun FeatureType.labelRes(): Int = when (this) {
    FeatureType.HABIT -> R.string.feature_habit
    FeatureType.GOAL -> R.string.feature_goal
    FeatureType.CALORIE -> R.string.feature_calorie
    FeatureType.WATER -> R.string.feature_water
    FeatureType.DIARY -> R.string.feature_diary
}

private fun NumberKind.labelRes(): Int = when (this) {
    NumberKind.CALORIE_TARGET -> R.string.settings_calorie_target
    NumberKind.WATER_TARGET -> R.string.settings_water_target
    NumberKind.INCREMENT_SMALL -> R.string.settings_increment_small
    NumberKind.INCREMENT_LARGE -> R.string.settings_increment_large
}
