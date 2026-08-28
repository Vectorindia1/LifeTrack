package com.lifetrack.habit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lifetrack.R
import com.lifetrack.habit.data.FrequencyType
import com.lifetrack.habit.data.Habit
import com.lifetrack.habit.data.HabitSchedule
import java.time.DayOfWeek

/**
 * Add-habit sheet. Offers all three frequencies from PRD 7.2 — the schema has
 * supported custom day-of-week since milestone 1, so exposing it costs one chip row.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddHabitSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, frequencyType: FrequencyType, daysOfWeekMask: Int, timesPerWeek: Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(FrequencyType.DAILY) }
    var selectedDays by remember { mutableStateOf(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)) }
    var timesPerWeek by remember { mutableIntStateOf(3) }

    val canSave = name.isNotBlank() &&
        (frequency != FrequencyType.CUSTOM_DAYS || selectedDays.isNotEmpty())

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.habit_add_title),
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.habit_name_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.habit_frequency_label),
                style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FrequencyType.entries.forEach { type ->
                    FilterChip(
                        selected = frequency == type,
                        onClick = { frequency = type },
                        label = { Text(stringResource(type.labelRes())) },
                    )
                }
            }

            when (frequency) {
                FrequencyType.CUSTOM_DAYS -> DayPicker(
                    selected = selectedDays,
                    onToggle = { day ->
                        selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                    },
                )

                FrequencyType.WEEKLY -> Column {
                    Text(
                        text = stringResource(R.string.habit_times_per_week, timesPerWeek),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = timesPerWeek.toFloat(),
                        onValueChange = { timesPerWeek = it.toInt() },
                        valueRange = 1f..7f,
                        steps = 5,
                    )
                }

                FrequencyType.DAILY -> Unit
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.habit_cancel)) }
                Button(
                    enabled = canSave,
                    onClick = {
                        onSave(
                            name,
                            frequency,
                            when (frequency) {
                                FrequencyType.CUSTOM_DAYS -> HabitSchedule.maskOf(selectedDays)
                                else -> Habit.ALL_DAYS_MASK
                            },
                            if (frequency == FrequencyType.WEEKLY) timesPerWeek else null,
                        )
                    },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(stringResource(R.string.habit_save))
                }
            }
        }
    }
}

@Composable
private fun DayPicker(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DayOfWeek.entries.forEach { day ->
            FilterChip(
                selected = day in selected,
                onClick = { onToggle(day) },
                label = { Text(stringResource(day.initialRes())) },
            )
        }
    }
}

private fun FrequencyType.labelRes(): Int = when (this) {
    FrequencyType.DAILY -> R.string.habit_freq_daily
    FrequencyType.WEEKLY -> R.string.habit_freq_weekly
    FrequencyType.CUSTOM_DAYS -> R.string.habit_freq_custom
}

private fun DayOfWeek.initialRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.day_mon
    DayOfWeek.TUESDAY -> R.string.day_tue
    DayOfWeek.WEDNESDAY -> R.string.day_wed
    DayOfWeek.THURSDAY -> R.string.day_thu
    DayOfWeek.FRIDAY -> R.string.day_fri
    DayOfWeek.SATURDAY -> R.string.day_sat
    DayOfWeek.SUNDAY -> R.string.day_sun
}
