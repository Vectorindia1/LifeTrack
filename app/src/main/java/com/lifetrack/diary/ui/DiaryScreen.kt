package com.lifetrack.diary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifetrack.R
import com.lifetrack.core.ui.AppViewModelProvider
import com.lifetrack.core.ui.Money
import com.lifetrack.core.ui.StatTile
import com.lifetrack.core.ui.theme.Accents
import com.lifetrack.core.ui.theme.FeatureGlyphs
import com.lifetrack.core.ui.theme.resolved
import com.lifetrack.diary.data.DaySummary
import com.lifetrack.diary.data.Mood
import com.lifetrack.diary.viewmodel.DiaryUiState
import com.lifetrack.diary.viewmodel.DiaryViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun DiaryScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }

    DiaryContent(
        uiState = uiState,
        onSelectDate = viewModel::selectDate,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
private fun DiaryContent(
    uiState: DiaryUiState,
    onSelectDate: (LocalDate) -> Unit,
    onSave: (String, Mood?) -> Unit,
    onDelete: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var month by remember(uiState.selectedDate) { mutableStateOf(YearMonth.from(uiState.selectedDate)) }

    // Draft state is keyed to the selected date so switching days loads that day's
    // entry rather than carrying the previous day's text across.
    var text by remember(uiState.selectedDate, uiState.entry?.id) {
        mutableStateOf(uiState.entry?.text.orEmpty())
    }
    var mood by remember(uiState.selectedDate, uiState.entry?.id) {
        mutableStateOf(uiState.entry?.mood)
    }

    val summaryLine = uiState.summary.line(uiState.currencyLocale)

    // PRD 7.7: prefill a blank entry with the day's summary to kill the blank page.
    // Only when there is no saved entry and nothing has been typed, and nothing is
    // persisted until Save is pressed, so this cannot create junk entries.
    LaunchedEffect(uiState.selectedDate, uiState.entry, summaryLine) {
        if (uiState.entry == null && text.isEmpty() && summaryLine != null) {
            text = "$summaryLine\n\n"
        }
    }

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
        item { StreakHeader(uiState) }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    MonthCalendar(
                        month = month,
                        selectedDate = uiState.selectedDate,
                        today = uiState.today,
                        datesWithEntries = uiState.datesWithEntries,
                        onSelectDate = onSelectDate,
                        onMonthChange = { month = it },
                    )
                }
            }
        }

        item {
            EntryEditor(
                uiState = uiState,
                text = text,
                mood = mood,
                onTextChange = { text = it },
                onMoodChange = { mood = if (mood == it) null else it },
                onSave = { onSave(text, mood) },
                onDelete = onDelete,
            )
        }
    }
}

/** The numbers behind the auto-prefilled summary line, shown as tiles rather than text. */
@Composable
private fun TodaySummaryRow(
    summary: DaySummary,
    currencyLocale: java.util.Locale,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        if (summary.habitsDue > 0) {
            StatTile(
                icon = FeatureGlyphs.Habit.icon,
                value = "${summary.habitsDone}/${summary.habitsDue}",
                label = stringResource(R.string.diary_stat_habits),
                accent = Accents.Habit.resolved,
            )
        }
        if (summary.spent > 0.0) {
            StatTile(
                icon = FeatureGlyphs.Expense.icon,
                value = Money.format(summary.spent, currencyLocale),
                label = stringResource(R.string.diary_stat_spent),
                accent = Accents.Expense.resolved,
            )
        }
        if (summary.calories > 0) {
            StatTile(
                icon = FeatureGlyphs.Calorie.icon,
                value = summary.calories.toString(),
                label = stringResource(R.string.diary_stat_calories),
                accent = Accents.Calorie.resolved,
            )
        }
        if (summary.waterMl > 0) {
            StatTile(
                icon = FeatureGlyphs.Water.icon,
                value = String.format(java.util.Locale.getDefault(), "%.1fL", summary.waterMl / 1000.0),
                label = stringResource(R.string.diary_stat_water),
                accent = Accents.Water.resolved,
            )
        }
    }
}

@Composable
private fun StreakHeader(uiState: DiaryUiState, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.diary_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (uiState.streak > 0) {
                stringResource(R.string.diary_streak, uiState.streak)
            } else {
                stringResource(R.string.diary_streak_none)
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (uiState.streak > 0) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun EntryEditor(
    uiState: DiaryUiState,
    text: String,
    mood: Mood?,
    onTextChange: (String) -> Unit,
    onMoodChange: (Mood) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM") }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (uiState.selectedDate == uiState.today) {
                        stringResource(R.string.diary_today)
                    } else {
                        uiState.selectedDate.format(dateFormatter)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (uiState.hasEntry) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = stringResource(R.string.diary_delete),
                        )
                    }
                }
            }

            if (!uiState.summary.isEmpty) {
                TodaySummaryRow(uiState.summary, uiState.currencyLocale)
            }

            Text(
                text = stringResource(R.string.diary_mood_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mood.entries.forEach { option ->
                    FilterChip(
                        selected = mood == option,
                        onClick = { onMoodChange(option) },
                        label = { Text(option.emoji) },
                    )
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text(stringResource(R.string.diary_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
            )

            Button(
                onClick = onSave,
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.diary_save))
            }
        }
    }
}

/**
 * PRD 7.7's summary line, e.g. "3/4 habits done, ₹450 spent, 1.8L water".
 *
 * Returns null when nothing was tracked, so a genuinely empty day gets a blank page
 * rather than a line of zeroes.
 */
@Composable
private fun DaySummary.line(currencyLocale: java.util.Locale): String? {
    if (isEmpty) return null
    val parts = buildList {
        if (habitsDue > 0) add(stringResource(R.string.diary_summary_habits, habitsDone, habitsDue))
        if (spent > 0.0) add(stringResource(R.string.diary_summary_spent, Money.format(spent, currencyLocale)))
        if (waterMl > 0) {
            val litres = String.format(java.util.Locale.getDefault(), "%.1fL", waterMl / 1000.0)
            add(stringResource(R.string.diary_summary_water, litres))
        }
        if (calories > 0) add(stringResource(R.string.diary_summary_calories, calories))
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
}
