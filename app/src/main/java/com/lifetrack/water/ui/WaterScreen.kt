package com.lifetrack.water.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifetrack.R
import com.lifetrack.core.ui.AppViewModelProvider
import com.lifetrack.core.ui.ProgressRing
import com.lifetrack.core.ui.chart.ChartPoint
import com.lifetrack.core.ui.chart.SimpleBarChart
import com.lifetrack.water.data.WaterLog
import com.lifetrack.water.viewmodel.WaterUiState
import com.lifetrack.water.viewmodel.WaterViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WaterScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: WaterViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustom by remember { mutableStateOf(false) }
    var showTarget by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }

    WaterContent(
        uiState = uiState,
        onAdd = viewModel::add,
        onUndo = viewModel::undoLast,
        onCustom = { showCustom = true },
        onEditTarget = { showTarget = true },
        onDelete = viewModel::delete,
        contentPadding = contentPadding,
        modifier = modifier,
    )

    if (showCustom) {
        CustomWaterDialog(
            onDismiss = { showCustom = false },
            onConfirm = { ml ->
                viewModel.add(ml)
                showCustom = false
            },
        )
    }
    if (showTarget) {
        WaterTargetDialog(
            current = uiState.targetMl,
            onDismiss = { showTarget = false },
            onConfirm = { ml ->
                viewModel.setTargetMl(ml)
                showTarget = false
            },
        )
    }
}

@Composable
private fun WaterContent(
    uiState: WaterUiState,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    onCustom: () -> Unit,
    onEditTarget: () -> Unit,
    onDelete: (WaterLog) -> Unit,
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
            RingCard(
                uiState = uiState,
                onAdd = onAdd,
                onUndo = onUndo,
                onCustom = onCustom,
                onEditTarget = onEditTarget,
            )
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.water_week_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    SimpleBarChart(
                        points = uiState.week.map {
                            ChartPoint(it.date.dayOfMonth.toString(), it.ml.toDouble())
                        },
                        color = MaterialTheme.colorScheme.tertiary,
                        yFormatter = { "${(it / 1000).toInt()}L" },
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.water_today_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (uiState.todaysLogs.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.water_empty_today),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(uiState.todaysLogs, key = { it.id }) { log ->
                WaterRow(log = log, onDelete = { onDelete(log) })
            }
        }
    }
}

@Composable
private fun RingCard(
    uiState: WaterUiState,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    onCustom: () -> Unit,
    onEditTarget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ringDesc = stringResource(
        R.string.water_ring_desc,
        uiState.drunkToday,
        uiState.targetMl,
    )

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProgressRing(
                progress = uiState.progress,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.semantics { contentDescription = ringDesc },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(
                            R.string.water_progress,
                            uiState.drunkToday,
                            uiState.targetMl,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (uiState.isGoalMet) {
                            stringResource(R.string.water_goal_met)
                        } else {
                            stringResource(R.string.water_remaining, uiState.remainingMl)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            WaterQuickAddRow(onAdd = onAdd, onCustom = onCustom)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onUndo,
                    enabled = uiState.todaysLogs.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.water_undo))
                }
                TextButton(onClick = onEditTarget) {
                    Text(stringResource(R.string.water_target_edit))
                }
            }
        }
    }
}

@Composable
private fun WaterRow(
    log: WaterLog,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val time = remember(log.timestamp) {
        log.timestamp.atZone(ZoneId.systemDefault()).toLocalTime().format(formatter)
    }

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
                text = stringResource(R.string.water_unit, log.mlAmount),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(R.string.water_delete),
                )
            }
        }
    }
}
