package com.lifetrack.habit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifetrack.R
import com.lifetrack.core.ui.AppViewModelProvider
import com.lifetrack.habit.data.FrequencyType
import com.lifetrack.habit.data.Habit
import com.lifetrack.habit.viewmodel.ChartWindow
import com.lifetrack.habit.viewmodel.HabitItem
import com.lifetrack.habit.viewmodel.HabitUiState
import com.lifetrack.habit.viewmodel.HabitViewModel

@Composable
fun HabitScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }

    HabitContent(
        uiState = uiState,
        onToggle = viewModel::toggle,
        onDelete = viewModel::delete,
        onChartWindow = viewModel::setChartWindow,
        onAddClick = { showAddSheet = true },
        contentPadding = contentPadding,
        modifier = modifier,
    )

    if (showAddSheet) {
        AddHabitSheet(
            onDismiss = { showAddSheet = false },
            onSave = { name, frequency, mask, timesPerWeek ->
                viewModel.addHabit(name, frequency, mask, timesPerWeek)
                showAddSheet = false
            },
        )
    }
}

@Composable
private fun HabitContent(
    uiState: HabitUiState,
    onToggle: (HabitItem) -> Unit,
    onDelete: (Habit) -> Unit,
    onChartWindow: (ChartWindow) -> Unit,
    onAddClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isEmpty) {
            EmptyHabits(contentPadding)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + 16.dp,
                    // Leave room for the FAB so the last row is never trapped under it.
                    bottom = contentPadding.calculateBottomPadding() + 88.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { TodayHeader(uiState) }

                items(uiState.items, key = { it.habit.id }) { item ->
                    HabitRow(
                        item = item,
                        onToggle = { onToggle(item) },
                        onDelete = { onDelete(item.habit) },
                    )
                }

                item {
                    CompletionCard(
                        uiState = uiState,
                        onChartWindow = onChartWindow,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = contentPadding.calculateBottomPadding()),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.habit_add))
        }
    }
}

@Composable
private fun TodayHeader(uiState: HabitUiState, modifier: Modifier = Modifier) {
    val dueToday = uiState.items.count { it.isScheduledToday }
    val doneToday = uiState.items.count { it.isScheduledToday && it.isDoneToday }
    Text(
        text = stringResource(R.string.habit_today_progress, doneToday, dueToday),
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun HabitRow(
    item: HabitItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDoneToday) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // One tap to log — PRD's ≤2-tap rule.
            val toggleLabel = stringResource(R.string.habit_toggle_desc, item.habit.name)
            Checkbox(
                checked = item.isDoneToday,
                onCheckedChange = { onToggle() },
                modifier = Modifier.semantics { contentDescription = toggleLabel },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.habit.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = item.subtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(R.string.habit_delete),
                )
            }
        }
    }
}

@Composable
private fun HabitItem.subtitle(): String = when {
    !isScheduledToday -> stringResource(R.string.habit_not_today)
    streak <= 0 -> stringResource(R.string.habit_streak_none)
    habit.frequencyType == FrequencyType.WEEKLY -> stringResource(R.string.habit_streak_weeks, streak)
    else -> stringResource(R.string.habit_streak, streak)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompletionCard(
    uiState: HabitUiState,
    onChartWindow: (ChartWindow) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.habit_chart_title),
                style = MaterialTheme.typography.titleMedium,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ChartWindow.entries.forEachIndexed { index, window ->
                    SegmentedButton(
                        selected = uiState.chartWindow == window,
                        onClick = { onChartWindow(window) },
                        shape = SegmentedButtonDefaults.itemShape(index, ChartWindow.entries.size),
                    ) {
                        Text(
                            stringResource(
                                when (window) {
                                    ChartWindow.WEEKS -> R.string.habit_chart_weeks
                                    ChartWindow.MONTHS -> R.string.habit_chart_months
                                },
                            ),
                        )
                    }
                }
            }

            if (uiState.bars.all { it.rate == 0f }) {
                Text(
                    text = stringResource(R.string.habit_chart_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                HabitCompletionChart(bars = uiState.bars)
            }
        }
    }
}

@Composable
private fun EmptyHabits(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.habit_empty_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.habit_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
