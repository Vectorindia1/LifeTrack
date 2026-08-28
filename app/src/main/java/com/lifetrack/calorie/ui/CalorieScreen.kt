package com.lifetrack.calorie.ui

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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifetrack.R
import com.lifetrack.calorie.data.CalorieLog
import com.lifetrack.calorie.viewmodel.CalorieSpan
import com.lifetrack.calorie.viewmodel.CalorieUiState
import com.lifetrack.calorie.viewmodel.CalorieViewModel
import com.lifetrack.core.ui.IconBadge
import com.lifetrack.core.ui.theme.Accents
import com.lifetrack.core.ui.theme.FeatureGlyphs
import com.lifetrack.core.ui.theme.resolved
import com.lifetrack.core.ui.AppViewModelProvider
import com.lifetrack.core.ui.chart.ChartPoint
import com.lifetrack.core.ui.chart.SimpleLineChart

@Composable
fun CalorieScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: CalorieViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }

    CalorieContent(
        uiState = uiState,
        onSpan = viewModel::setSpan,
        onDelete = viewModel::delete,
        onAddClick = { showAddSheet = true },
        onEditTarget = { showTargetDialog = true },
        contentPadding = contentPadding,
        modifier = modifier,
    )

    if (showAddSheet) {
        AddCalorieSheet(
            onDismiss = { showAddSheet = false },
            onSave = { food, calories ->
                viewModel.add(food, calories)
                showAddSheet = false
            },
        )
    }

    if (showTargetDialog) {
        TargetDialog(
            current = uiState.target,
            onDismiss = { showTargetDialog = false },
            onConfirm = { target ->
                viewModel.setTarget(target)
                showTargetDialog = false
            },
        )
    }
}

@Composable
private fun CalorieContent(
    uiState: CalorieUiState,
    onSpan: (CalorieSpan) -> Unit,
    onDelete: (CalorieLog) -> Unit,
    onAddClick: () -> Unit,
    onEditTarget: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 88.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TargetCard(uiState = uiState, onEditTarget = onEditTarget) }
            item { HistoryCard(uiState = uiState, onSpan = onSpan) }

            item {
                Text(
                    text = stringResource(R.string.calorie_today_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (uiState.todaysLogs.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.calorie_empty_today),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(uiState.todaysLogs, key = { it.id }) { log ->
                    CalorieRow(log = log, onDelete = { onDelete(log) })
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
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.calorie_add))
        }
    }
}

@Composable
private fun TargetCard(
    uiState: CalorieUiState,
    onEditTarget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = Accents.Calorie.resolved
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = FeatureGlyphs.Calorie.icon, tint = accent)
                Text(
                    text = stringResource(
                        R.string.calorie_eaten_of_target,
                        uiState.eatenToday,
                        uiState.target,
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )
                TextButton(onClick = onEditTarget) {
                    Text(stringResource(R.string.calorie_target_edit))
                }
            }

            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (uiState.isOverTarget) MaterialTheme.colorScheme.error else accent,
            )

            Text(
                text = if (uiState.isOverTarget) {
                    stringResource(R.string.calorie_over, -uiState.remaining)
                } else {
                    stringResource(R.string.calorie_remaining, uiState.remaining)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.isOverTarget) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryCard(
    uiState: CalorieUiState,
    onSpan: (CalorieSpan) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.calorie_history_title),
                style = MaterialTheme.typography.titleMedium,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                CalorieSpan.entries.forEachIndexed { index, span ->
                    SegmentedButton(
                        selected = uiState.span == span,
                        onClick = { onSpan(span) },
                        shape = SegmentedButtonDefaults.itemShape(index, CalorieSpan.entries.size),
                    ) {
                        Text(
                            stringResource(
                                when (span) {
                                    CalorieSpan.WEEK -> R.string.calorie_span_week
                                    CalorieSpan.MONTH -> R.string.calorie_span_month
                                },
                            ),
                        )
                    }
                }
            }
            SimpleLineChart(
                points = uiState.history.map {
                    ChartPoint(it.date.dayOfMonth.toString(), it.total.toDouble())
                },
                color = Accents.Calorie.resolved,
            )
        }
    }
}

@Composable
private fun CalorieRow(
    log: CalorieLog,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = log.foodName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.calorie_unit, log.calories),
                style = MaterialTheme.typography.titleSmall,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(R.string.calorie_delete),
                )
            }
        }
    }
}

/**
 * Temporary home for the daily target. PRD 7.9 puts targets in Settings, which is
 * milestone 10; until then the target would be stuck at its seeded default and the
 * feature would be untestable. Move it when Settings lands.
 */
@Composable
private fun TargetDialog(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(current.toString()) }
    val parsed = text.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calorie_target_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input -> if (input.all { it.isDigit() }) text = input },
                label = { Text(stringResource(R.string.calorie_target_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(
                enabled = parsed != null && parsed > 0,
                onClick = { onConfirm(parsed!!) },
            ) {
                Text(stringResource(R.string.calorie_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.calorie_cancel)) }
        },
    )
}
