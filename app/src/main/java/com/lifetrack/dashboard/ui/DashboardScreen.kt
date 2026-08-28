package com.lifetrack.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.lifetrack.core.ui.GreetingPeriod
import com.lifetrack.core.ui.IconBadge
import com.lifetrack.core.ui.Money
import com.lifetrack.core.ui.ProgressRing
import com.lifetrack.core.ui.greetingFor
import com.lifetrack.core.ui.theme.Accents
import com.lifetrack.core.ui.theme.FeatureGlyphs
import com.lifetrack.core.ui.theme.LifeTrackTheme
import com.lifetrack.core.ui.theme.goalAccent
import com.lifetrack.core.ui.theme.resolved
import com.lifetrack.dashboard.viewmodel.DashboardUiState
import com.lifetrack.dashboard.viewmodel.DashboardViewModel
import com.lifetrack.goal.viewmodel.GoalItem
import com.lifetrack.habit.viewmodel.HabitItem
import com.lifetrack.water.ui.WaterQuickAddRow
import java.time.LocalTime
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
        item { GreetingHeader(uiState) }

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

        if (uiState.topGoals.isNotEmpty()) {
            item { GoalsCard(uiState = uiState, onOpen = onOpen) }
        }

        item { DiaryCard(uiState = uiState, onOpen = onOpen) }

        item { PeriodCard(uiState = uiState, onOpen = onOpen) }
    }
}

@Composable
private fun GreetingHeader(uiState: DashboardUiState, modifier: Modifier = Modifier) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()) }
    val greetingRes = when (greetingFor(LocalTime.now().hour)) {
        GreetingPeriod.MORNING -> R.string.dashboard_greeting_morning
        GreetingPeriod.AFTERNOON -> R.string.dashboard_greeting_afternoon
        GreetingPeriod.EVENING -> R.string.dashboard_greeting_evening
        GreetingPeriod.NIGHT -> R.string.dashboard_greeting_night
    }
    val greeting = stringResource(greetingRes)
    val name = uiState.displayName

    Column(modifier = modifier) {
        Text(
            text = if (name != null) "$greeting, $name 👋" else "$greeting 👋",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "${stringResource(R.string.dashboard_subtitle)} · ${uiState.date.format(dateFormatter)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A card header: an [IconBadge], a title, and optional trailing content. */
@Composable
private fun CardHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon = icon, tint = accent)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        trailing()
    }
}

@Composable
private fun HabitsCard(
    uiState: DashboardUiState,
    onToggle: (HabitItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = Accents.Habit.resolved
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardHeader(
                icon = FeatureGlyphs.Habit.icon,
                accent = accent,
                title = stringResource(R.string.dashboard_habits_title),
            ) {
                Text(
                    text = stringResource(
                        R.string.dashboard_habits_count,
                        uiState.doneCount,
                        uiState.dueCount,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (uiState.allDone) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (uiState.habitsDueToday.isEmpty()) {
                Text(
                    text = stringResource(R.string.dashboard_nothing_due),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                uiState.habitsDueToday.forEach { item ->
                    DashboardHabitRow(item = item, accent = accent, onToggle = { onToggle(item) })
                }
                if (uiState.allDone) {
                    Text(
                        text = stringResource(R.string.dashboard_all_done),
                        style = MaterialTheme.typography.bodySmall,
                        color = accent,
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
    accent: androidx.compose.ui.graphics.Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
                color = accent,
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
    val accent = Accents.Water.resolved
    // Clickable like the other tracker cards: this is the only way to reach the full
    // Water screen from the dashboard now that the row below drops its "Custom"
    // button (see WaterQuickAddRow — three elements were starving each other of
    // width here, and PRD 7.1 only asks for the two quick-add buttons anyway).
    ElevatedCard(
        onClick = { onOpen(Destination.Water) },
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProgressRing(
                progress = uiState.waterProgress,
                size = 76.dp,
                thickness = 9.dp,
                color = accent,
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
                    color = if (uiState.isWaterGoalMet) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WaterQuickAddRow(
                    smallMl = uiState.waterIncrementSmallMl,
                    largeMl = uiState.waterIncrementLargeMl,
                    onAdd = onAddWater,
                    onCustom = { onOpen(Destination.Water) },
                    showCustom = false,
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
    val accent = Accents.Calorie.resolved
    ElevatedCard(
        onClick = { onOpen(Destination.Calories) },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CardHeader(
                icon = FeatureGlyphs.Calorie.icon,
                accent = accent,
                title = stringResource(R.string.dashboard_calories_title),
            ) {
                Text(
                    text = stringResource(
                        R.string.calorie_eaten_of_target,
                        uiState.caloriesEaten,
                        uiState.calorieTarget,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.isOverCalories) MaterialTheme.colorScheme.error else accent,
                )
            }
            LinearProgressIndicator(
                progress = { uiState.calorieProgress },
                modifier = Modifier.fillMaxWidth(),
                color = if (uiState.isOverCalories) MaterialTheme.colorScheme.error else accent,
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
    val accent = Accents.Expense.resolved
    ElevatedCard(
        onClick = { onOpen(Destination.Expenses) },
        modifier = modifier.fillMaxWidth(),
    ) {
        CardHeader(
            icon = FeatureGlyphs.Expense.icon,
            accent = accent,
            title = stringResource(R.string.dashboard_spent_title),
            modifier = Modifier.padding(16.dp),
        ) {
            if (uiState.spentToday > 0.0) {
                Text(text = Money.format(uiState.spentToday), style = MaterialTheme.typography.titleMedium)
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

/** PRD 7.1's diary streak indicator and "write today's entry" prompt. */
@Composable
private fun DiaryCard(
    uiState: DashboardUiState,
    onOpen: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = Accents.Diary.resolved
    ElevatedCard(
        onClick = { onOpen(Destination.Diary) },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CardHeader(
                icon = FeatureGlyphs.Diary.icon,
                accent = accent,
                title = stringResource(R.string.dashboard_diary_title),
            ) {
                if (uiState.diaryStreak > 0) {
                    Text(
                        text = "🔥 ${uiState.diaryStreak}",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                    )
                }
            }
            Text(
                text = if (uiState.diaryWrittenToday) {
                    stringResource(R.string.dashboard_diary_done)
                } else {
                    stringResource(R.string.dashboard_diary_prompt)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.diaryWrittenToday) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 52.dp),
            )
        }
    }
}

/**
 * Period tracker card — not in the original PRD, added by user request (session 12).
 * Wording is neutral about whose cycle is being tracked, matching the feature's intent.
 */
@Composable
private fun PeriodCard(
    uiState: DashboardUiState,
    onOpen: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = Accents.Period.resolved
    ElevatedCard(
        onClick = { onOpen(Destination.Period) },
        modifier = modifier.fillMaxWidth(),
    ) {
        CardHeader(
            icon = FeatureGlyphs.Period.icon,
            accent = accent,
            title = stringResource(R.string.dashboard_period_title),
            modifier = Modifier.padding(16.dp),
        ) {
            val day = uiState.periodCurrentCycleDay
            Text(
                text = when {
                    day != null -> stringResource(R.string.period_current_day, day)
                    uiState.hasPeriodLogs -> ""
                    else -> stringResource(R.string.dashboard_period_prompt)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** PRD 7.1's "active goals with progress bars (top 2-3, see all for the rest)". */
@Composable
private fun GoalsCard(
    uiState: DashboardUiState,
    onOpen: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardHeader(
                icon = Icons.Filled.Flag,
                accent = Accents.Goal.resolved,
                title = stringResource(R.string.dashboard_goals_title),
            ) {
                if (uiState.hasMoreGoals) {
                    TextButton(onClick = { onOpen(Destination.Goals) }) {
                        Text(stringResource(R.string.dashboard_goals_see_all))
                    }
                }
            }
            uiState.topGoals.forEach { item -> DashboardGoalRow(item) }
        }
    }
}

@Composable
private fun DashboardGoalRow(item: GoalItem, modifier: Modifier = Modifier) {
    val accent = goalAccent(item.goal.id).resolved
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.goal.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(item.fraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = if (item.isOverdue) MaterialTheme.colorScheme.error else accent,
            )
        }
        LinearProgressIndicator(
            progress = { item.fraction },
            modifier = Modifier.fillMaxWidth(),
            color = if (item.isOverdue) MaterialTheme.colorScheme.error else accent,
        )
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
