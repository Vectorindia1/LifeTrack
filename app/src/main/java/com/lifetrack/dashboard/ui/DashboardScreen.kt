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
import com.lifetrack.core.ui.theme.LifeTrackTheme
import com.lifetrack.dashboard.viewmodel.DashboardUiState
import com.lifetrack.dashboard.viewmodel.DashboardViewModel
import com.lifetrack.habit.viewmodel.HabitItem
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
        onOpen = onOpen,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onToggle: (HabitItem) -> Unit,
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
            onOpen = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
