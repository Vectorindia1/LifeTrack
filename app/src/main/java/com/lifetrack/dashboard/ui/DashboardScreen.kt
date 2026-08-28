package com.lifetrack.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifetrack.R
import com.lifetrack.core.navigation.Destination
import com.lifetrack.core.ui.AppViewModelProvider
import com.lifetrack.core.ui.Money
import com.lifetrack.core.ui.theme.LifeTrackTheme
import com.lifetrack.dashboard.viewmodel.DashboardUiState
import com.lifetrack.dashboard.viewmodel.DashboardViewModel
import com.lifetrack.core.ui.ProgressRing
import com.lifetrack.habit.viewmodel.HabitItem
import com.lifetrack.water.ui.WaterQuickAddRow
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    contentPadding: PaddingValues,
    onOpen: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Re-read the date on resume so a session left open overnight isn't stuck on yesterday.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }

    DashboardContent(
        uiState = uiState,
        onToggle = viewModel::toggle,
        onAddWater = viewModel::addWater,
        onOpen = onOpen,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onToggle: (HabitItem) -> Unit,
    onAddWater: (Int) -> Unit,
    onOpen: (Destination) -> Unit,
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
        item { DateHeader(uiState) }

        item {
            if (uiState.hasAnyHabit) {
                HabitsCard(uiState = uiState, onToggle = onToggle)
            } else if (!uiState.isLoading) {
                FirstHabitPrompt(onAddHabit = { onOpen(Destination.Habits) })
            }
        }

        item { WaterCard(uiState = uiState, onAddWater = onAddWater, onOpen = onOpen) }

        item { CaloriesCard(uiState = uiState, onOpen = onOpen) }

        item { SpentTodayCard(uiState = uiState, onOpen = onOpen) }

        // Goals, Calories and Water have no bottom-bar tab, so this row is their
        // only way in until milestones 5-7 give them real dashboard sections.
        item { MoreTrackers(onOpen = onOpen) }
    }
}

@Composable
private fun DateHeader(uiState: DashboardUiState, modifier: Modifier = Modifier) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()) }
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.dashboard_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = uiState.date.format(formatter),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HabitsCard(
    uiState: DashboardUiState,
    onToggle: (HabitItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_habits_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        R.string.dashboard_habits_count,
                        uiState.doneCount,
                        uiState.dueCount,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (uiState.allDone) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            if (uiState.habitsDueToday.isEmpty()) {
                Text(
                    text = stringResource(R.string.dashboard_nothing_due),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            } else {
                uiState.habitsDueToday.forEach { item ->
                    DashboardHabitRow(item = item, onToggle = { onToggle(item) })
                }
                if (uiState.allDone) {
                    Text(
                        text = stringResource(R.string.dashboard_all_done),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/** A single tap toggles the habit — no navigation, no dialog. */
@Composable
private fun DashboardHabitRow(
    item: HabitItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val toggleLabel = stringResource(R.string.habit_toggle_desc, item.habit.name)
        Checkbox(
            checked = item.isDoneToday,
            onCheckedChange = { onToggle() },
            modifier = Modifier.semantics { contentDescription = toggleLabel },
        )
        Text(
            text = item.habit.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (item.streak > 0) {
            Text(
                text = "🔥 ${item.streak}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
    }
}

/**
 * PRD 7.1's water progress ring with +250/+500 inline.
 *
 * The quick-add buttons live on the dashboard itself, so logging a drink is a
 * single tap from the home screen — the strictest case of PRD section 8's rule.
 */
@Composable
private fun WaterCard(
    uiState: DashboardUiState,
    onAddWater: (Int) -> Unit,
    onOpen: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProgressRing(
                progress = uiState.waterProgress,
                size = 72.dp,
                thickness = 8.dp,
                color = MaterialTheme.colorScheme.tertiary,
            ) {
                Text(
                    text = "${(uiState.waterProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.dashboard_water_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        R.string.water_progress,
                        uiState.waterDrunkMl,
                        uiState.waterTargetMl,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.isWaterGoalMet) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                WaterQuickAddRow(
                    onAdd = onAddWater,
                    onCustom = { onOpen(Destination.Water) },
                )
            }
        }
    }
}

/** PRD 7.1's calorie progress bar. */
@Composable
private fun CaloriesCard(
    uiState: DashboardUiState,
    onOpen: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = { onOpen(Destination.Calories) },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.dashboard_calories_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        R.string.calorie_eaten_of_target,
                        uiState.caloriesEaten,
                        uiState.calorieTarget,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.isOverCalories) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            LinearProgressIndicator(
                progress = { uiState.calorieProgress },
                modifier = Modifier.fillMaxWidth(),
                color = if (uiState.isOverCalories) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}

/** PRD 7.1's "today's spend total". Tapping through opens the full tracker. */
@Composable
private fun SpentTodayCard(
    uiState: DashboardUiState,
    onOpen: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = { onOpen(Destination.Expenses) },
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.dashboard_spent_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (uiState.spentToday > 0.0) {
                Text(
                    text = Money.format(uiState.spentToday),
                    style = MaterialTheme.typography.titleMedium,
                )
            } else {
                Text(
                    text = stringResource(R.string.dashboard_spent_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FirstHabitPrompt(onAddHabit: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.dashboard_no_habits_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.dashboard_no_habits_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onAddHabit) {
                Text(stringResource(R.string.dashboard_add_habit))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreTrackers(onOpen: (Destination) -> Unit, modifier: Modifier = Modifier) {
    val destinations = remember {
        listOf(Destination.Goals, Destination.Calories, Destination.Water)
    }
    Column(modifier = modifier.padding(top = 4.dp)) {
        Text(
            text = stringResource(R.string.dashboard_more),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            destinations.forEach { destination ->
                AssistChip(
                    onClick = { onOpen(destination) },
                    label = { Text(stringResource(destination.labelRes)) },
                    leadingIcon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    LifeTrackTheme {
        DashboardContent(
            uiState = DashboardUiState(isLoading = false),
            onToggle = {},
            onAddWater = {},
            onOpen = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
