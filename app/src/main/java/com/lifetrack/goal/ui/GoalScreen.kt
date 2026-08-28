package com.lifetrack.goal.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifetrack.R
import com.lifetrack.core.ui.AppViewModelProvider
import com.lifetrack.goal.data.Goal
import com.lifetrack.goal.viewmodel.GoalItem
import com.lifetrack.goal.viewmodel.GoalUiState
import com.lifetrack.goal.viewmodel.GoalViewModel

@Composable
fun GoalScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: GoalViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var updating by remember { mutableStateOf<Goal?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }

    GoalContent(
        uiState = uiState,
        onIncrement = { viewModel.increment(it, 1.0) },
        onUpdateClick = { updating = it },
        onDelete = viewModel::delete,
        onAddClick = { showAddSheet = true },
        contentPadding = contentPadding,
        modifier = modifier,
    )

    if (showAddSheet) {
        AddGoalSheet(
            onDismiss = { showAddSheet = false },
            onSave = { name, target, unit, deadline ->
                viewModel.add(name, target, unit, deadline)
                showAddSheet = false
            },
        )
    }

    updating?.let { goal ->
        UpdateProgressDialog(
            goal = goal,
            onDismiss = { updating = null },
            onConfirm = { value ->
                viewModel.setProgress(goal, value)
                updating = null
            },
        )
    }
}

@Composable
private fun GoalContent(
    uiState: GoalUiState,
    onIncrement: (Goal) -> Unit,
    onUpdateClick: (Goal) -> Unit,
    onDelete: (Goal) -> Unit,
    onAddClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isEmpty) {
            EmptyGoals(contentPadding)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 88.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (uiState.active.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.goal_active_title)) }
                    items(uiState.active, key = { it.goal.id }) { item ->
                        GoalCard(
                            item = item,
                            onIncrement = { onIncrement(item.goal) },
                            onUpdateClick = { onUpdateClick(item.goal) },
                            onDelete = { onDelete(item.goal) },
                        )
                    }
                }
                if (uiState.completed.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.goal_completed_title)) }
                    items(uiState.completed, key = { it.goal.id }) { item ->
                        GoalCard(
                            item = item,
                            onIncrement = { onIncrement(item.goal) },
                            onUpdateClick = { onUpdateClick(item.goal) },
                            onDelete = { onDelete(item.goal) },
                        )
                    }
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
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.goal_add))
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun GoalCard(
    item: GoalItem,
    onIncrement: () -> Unit,
    onUpdateClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isComplete) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.goal_delete),
                    )
                }
            }

            LinearProgressIndicator(
                progress = { item.fraction },
                modifier = Modifier.fillMaxWidth(),
                color = if (item.isOverdue) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        R.string.goal_progress,
                        item.goal.currentValue.trimZeros(),
                        item.goal.targetValue.trimZeros(),
                        item.goal.unit,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                DeadlineLabel(item)
            }

            if (!item.isComplete) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onIncrement) {
                        Text(stringResource(R.string.goal_increment))
                    }
                    OutlinedButton(onClick = onUpdateClick) {
                        Text(stringResource(R.string.goal_set_value))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeadlineLabel(item: GoalItem, modifier: Modifier = Modifier) {
    val days = item.daysRemaining
    when {
        item.isComplete -> Text(
            text = stringResource(R.string.goal_complete),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier,
        )
        days == null -> Unit
        days < 0L -> Text(
            text = stringResource(R.string.goal_overdue, -days),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
        days == 0L -> Text(
            text = stringResource(R.string.goal_due_today),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
        days == 1L -> Text(
            text = stringResource(R.string.goal_due_tomorrow),
            style = MaterialTheme.typography.labelMedium,
            modifier = modifier,
        )
        else -> Text(
            text = stringResource(R.string.goal_days_left, days),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
}

@Composable
private fun UpdateProgressDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var text by remember { mutableStateOf(goal.currentValue.trimZeros()) }
    val parsed = text.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goal_update_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    if (input.all { it.isDigit() || it == '.' || it == ',' }) text = input
                },
                label = { Text(stringResource(R.string.goal_update_value)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { onConfirm(parsed!!) }) {
                Text(stringResource(R.string.goal_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.goal_cancel)) }
        },
    )
}

@Composable
private fun EmptyGoals(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.goal_empty_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.goal_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** "5" rather than "5.0", but "5.5" stays "5.5". Goal values are usually whole. */
internal fun Double.trimZeros(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()
