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
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
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
import com.lifetrack.core.ui.IconBadge
import com.lifetrack.core.ui.theme.Accents
import com.lifetrack.core.ui.theme.FeatureGlyphs
import com.lifetrack.core.ui.theme.resolved
import com.lifetrack.expense.data.CategoryUsage
import com.lifetrack.habit.data.Habit
import com.lifetrack.notification.data.FeatureType
import com.lifetrack.notification.data.NotificationSettings
import com.lifetrack.settings.viewmodel.SettingsUiState
import com.lifetrack.settings.viewmodel.SettingsViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    var editingNumber by remember { mutableStateOf<NumberEdit?>(null) }
    var editingTime by remember { mutableStateOf<NotificationSettings?>(null) }
    var renaming by remember { mutableStateOf<CategoryUsage?>(null) }

    // Backup/restore: the export launcher gets the JSON handed to it only once the
    // user has actually picked a save location, so nothing is generated for nothing.
    // Import holds the file's content in `pendingImportJson` until the user confirms
    // the destructive replace in the dialog below — reading the file is not itself
    // destructive, so that part runs immediately.
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var backupStatus by remember { mutableStateOf<String?>(null) }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.exportBackup { json ->
            backupStatus = try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                context.getString(R.string.settings_backup_success_export)
            } catch (error: Exception) {
                context.getString(R.string.settings_backup_error_generic)
            }
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (error: Exception) {
            null
        }
        if (json != null) pendingImportJson = json else backupStatus = context.getString(R.string.settings_backup_error_generic)
    }

    // Refreshed on resume, not just once: the only way a denied permission changes is
    // the user granting it from system settings and coming back to this screen.
    var notificationsEnabled by remember { mutableStateOf(com.lifetrack.notification.Notifier.canPost(context)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        notificationsEnabled = com.lifetrack.notification.Notifier.canPost(context)
        onPauseOrDispose { }
    }

    val nextCheck = remember(uiState.reminders) {
        val enabled = uiState.reminders
            .filter { it.enabled }
            .groupBy({ it.featureType }, { it.reminderTime })
        com.lifetrack.notification.work.DigestScheduler.nextCheckTime(enabled)
    }

    SettingsContent(
        uiState = uiState,
        notificationsEnabled = notificationsEnabled,
        nextCheck = nextCheck,
        onEditNumber = { editingNumber = it },
        onToggleReminder = viewModel::setReminderEnabled,
        onEditTime = { editingTime = it },
        onDeleteHabit = viewModel::deleteHabit,
        onRenameCategory = { renaming = it },
        onTheme = viewModel::setTheme,
        onCurrency = viewModel::setCurrency,
        onWaterReminder = viewModel::setWaterReminder,
        onDisplayName = viewModel::setDisplayName,
        onOpenNotificationSettings = {
            val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            context.startActivity(intent)
        },
        onSendTestNotification = viewModel::sendTestNotification,
        backupStatus = backupStatus,
        onExport = {
            val fileName = "lifetrack-backup-${java.time.LocalDate.now()}.json"
            exportLauncher.launch(fileName)
        },
        onImport = { importLauncher.launch(arrayOf("application/json")) },
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

    pendingImportJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingImportJson = null },
            title = { Text(stringResource(R.string.settings_backup_confirm_title)) },
            text = { Text(stringResource(R.string.settings_backup_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importBackup(json) { error ->
                            backupStatus = error ?: context.getString(R.string.settings_backup_success_import)
                        }
                        pendingImportJson = null
                    },
                ) {
                    Text(stringResource(R.string.settings_backup_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportJson = null }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    notificationsEnabled: Boolean,
    nextCheck: java.time.LocalDateTime?,
    onEditNumber: (NumberEdit) -> Unit,
    onToggleReminder: (FeatureType, Boolean) -> Unit,
    onEditTime: (NotificationSettings) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onRenameCategory: (CategoryUsage) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onCurrency: (com.lifetrack.core.ui.CurrencyOption) -> Unit,
    onWaterReminder: (Boolean, Int) -> Unit,
    onDisplayName: (String?) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onSendTestNotification: () -> Unit,
    backupStatus: String?,
    onExport: () -> Unit,
    onImport: () -> Unit,
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
            SettingsCard(
                stringResource(R.string.settings_display_name),
                icon = Icons.Filled.Person,
                accent = MaterialTheme.colorScheme.primary,
            ) {
                var name by remember(uiState.preferences.displayName) {
                    mutableStateOf(uiState.preferences.displayName.orEmpty())
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.settings_display_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        enabled = name.trim() != uiState.preferences.displayName.orEmpty(),
                        onClick = { onDisplayName(name) },
                    ) {
                        Text(stringResource(R.string.settings_save))
                    }
                }
            }
        }

        item {
            SettingsCard(
                stringResource(R.string.settings_targets),
                icon = FeatureGlyphs.Calorie.icon,
                accent = Accents.Calorie.resolved,
            ) {
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
            SettingsCard(
                stringResource(R.string.settings_water_increments),
                icon = FeatureGlyphs.Water.icon,
                accent = Accents.Water.resolved,
            ) {
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
            SettingsCard(
                stringResource(R.string.settings_notifications),
                icon = Icons.Filled.Notifications,
                accent = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    text = stringResource(R.string.settings_notifications_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // The permission and next-check time are the two things that make an
                // apparently "silent" notification system either explainable or a
                // real bug. Both are invisible without this row.
                Text(
                    text = stringResource(
                        if (notificationsEnabled) {
                            R.string.settings_notif_status_granted
                        } else {
                            R.string.settings_notif_status_denied
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (notificationsEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                if (!notificationsEnabled) {
                    TextButton(onClick = onOpenNotificationSettings) {
                        Text(stringResource(R.string.settings_notif_open_settings))
                    }
                }
                Text(
                    text = nextCheck?.let {
                        stringResource(
                            R.string.settings_notif_next_check,
                            it.format(remember { java.time.format.DateTimeFormatter.ofPattern("EEE d MMM, HH:mm") }),
                        )
                    } ?: stringResource(R.string.settings_notif_next_check_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                // Doesn't wait for a scheduled time — proves the whole pipeline
                // (permission, channel, digest content) works right now.
                Button(onClick = onSendTestNotification, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_notif_test))
                }
            }
        }

        item {
            SettingsCard(
                stringResource(R.string.settings_habits),
                icon = FeatureGlyphs.Habit.icon,
                accent = Accents.Habit.resolved,
            ) {
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
            SettingsCard(
                stringResource(R.string.settings_categories),
                icon = Icons.Filled.Badge,
                accent = Accents.Expense.resolved,
            ) {
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
            SettingsCard(
                stringResource(R.string.settings_theme),
                icon = Icons.Filled.DarkMode,
                accent = MaterialTheme.colorScheme.primary,
            ) {
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

        item {
            SettingsCard(
                stringResource(R.string.settings_currency),
                icon = Icons.Filled.AttachMoney,
                accent = MaterialTheme.colorScheme.primary,
            ) {
                val selected = com.lifetrack.core.ui.CurrencyOption.fromTag(uiState.preferences.currencyLocaleTag)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    com.lifetrack.core.ui.CurrencyOption.entries.forEach { option ->
                        FilterChip(
                            selected = selected == option,
                            onClick = { onCurrency(option) },
                            label = { Text(option.displayName) },
                        )
                    }
                }
            }
        }

        item {
            val prefs = uiState.preferences
            SettingsCard(
                stringResource(R.string.settings_water_reminder),
                icon = com.lifetrack.core.ui.theme.FeatureGlyphs.Water.icon,
                accent = com.lifetrack.core.ui.theme.Accents.Water.resolved,
            ) {
                Text(
                    text = stringResource(R.string.settings_water_reminder_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.settings_water_reminder_toggle),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = prefs.waterReminderEnabled,
                        onCheckedChange = { onWaterReminder(it, prefs.waterReminderIntervalMinutes) },
                    )
                }
                if (prefs.waterReminderEnabled) {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(15, 30, 60, 90, 120).forEach { minutes ->
                            FilterChip(
                                selected = prefs.waterReminderIntervalMinutes == minutes,
                                onClick = { onWaterReminder(true, minutes) },
                                label = { Text(stringResource(R.string.settings_water_reminder_interval, minutes)) },
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(
                stringResource(R.string.settings_backup),
                icon = Icons.Filled.Backup,
                accent = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    text = stringResource(R.string.settings_backup_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onExport, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_backup_export))
                    }
                    androidx.compose.material3.OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_backup_import))
                    }
                }
                backupStatus?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Tune,
    accent: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = icon, tint = accent, size = 32.dp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
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
