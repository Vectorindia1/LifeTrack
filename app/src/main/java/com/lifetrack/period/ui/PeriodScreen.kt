package com.lifetrack.period.ui

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifetrack.R
import com.lifetrack.core.ui.AppViewModelProvider
import com.lifetrack.core.ui.IconBadge
import com.lifetrack.core.ui.theme.Accents
import com.lifetrack.core.ui.theme.FeatureGlyphs
import com.lifetrack.core.ui.theme.resolved
import com.lifetrack.period.data.CycleStats
import com.lifetrack.period.data.PeriodLog
import com.lifetrack.period.viewmodel.PeriodUiState
import com.lifetrack.period.viewmodel.PeriodViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun PeriodScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: PeriodViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var editingEndDate by remember { mutableStateOf<PeriodLog?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }

    PeriodContent(
        uiState = uiState,
        onLogToday = viewModel::logToday,
        onLogOtherDate = { showDatePicker = true },
        onSetEndDate = { editingEndDate = it },
        onClearEndDate = { viewModel.setEndDate(it, null) },
        onDelete = viewModel::delete,
        contentPadding = contentPadding,
        modifier = modifier,
    )

    if (showDatePicker) {
        DatePickerSheet(
            initialDate = LocalDate.now(),
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                viewModel.logStart(date)
                showDatePicker = false
            },
        )
    }

    editingEndDate?.let { log ->
        DatePickerSheet(
            initialDate = log.endDate ?: log.startDate,
            onDismiss = { editingEndDate = null },
            onConfirm = { date ->
                viewModel.setEndDate(log, date)
                editingEndDate = null
            },
        )
    }
}

@Composable
private fun PeriodContent(
    uiState: PeriodUiState,
    onLogToday: () -> Unit,
    onLogOtherDate: () -> Unit,
    onSetEndDate: (PeriodLog) -> Unit,
    onClearEndDate: (PeriodLog) -> Unit,
    onDelete: (PeriodLog) -> Unit,
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
            StatsCard(uiState = uiState, onLogToday = onLogToday, onLogOtherDate = onLogOtherDate)
        }

        if (uiState.isEmpty) {
            item { EmptyState() }
        } else {
            item {
                Text(
                    text = stringResource(R.string.period_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(uiState.logs, key = { it.id }) { log ->
                PeriodRow(
                    log = log,
                    onEditEndDate = { onSetEndDate(log) },
                    onClearEndDate = { onClearEndDate(log) },
                    onDelete = { onDelete(log) },
                )
            }
        }
    }
}

@Composable
private fun StatsCard(
    uiState: PeriodUiState,
    onLogToday: () -> Unit,
    onLogOtherDate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = Accents.Period.resolved
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = FeatureGlyphs.Period.icon, tint = accent)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = uiState.currentCycleDay?.let {
                            stringResource(R.string.period_current_day, it)
                        } ?: stringResource(R.string.period_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = uiState.averageCycleLength?.let {
                            stringResource(R.string.period_average_length, it.toInt())
                        } ?: stringResource(R.string.period_average_unknown),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // One tap for the common case — logging the day it actually starts.
                Button(onClick = onLogToday) {
                    Text(stringResource(R.string.period_log_today))
                }
                OutlinedButton(onClick = onLogOtherDate) {
                    Text(stringResource(R.string.period_log_other_date))
                }
            }
        }
    }
}

@Composable
private fun PeriodRow(
    log: PeriodLog,
    onEditEndDate: () -> Unit,
    onClearEndDate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }
    val duration = CycleStats.duration(log)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = log.startDate.format(formatter), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = duration?.let { stringResource(R.string.period_duration_days, it) }
                            ?: stringResource(R.string.period_ongoing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // "Set" opens a picker; "Clear" acts immediately — the two are
                // different actions, not the same button toggling its label.
                TextButton(onClick = if (log.endDate == null) onEditEndDate else onClearEndDate) {
                    Text(
                        stringResource(
                            if (log.endDate == null) R.string.period_set_end else R.string.period_clear_end,
                        ),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.period_delete),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = stringResource(R.string.period_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.period_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    // The picker reports UTC millis; convert on that same basis so the
                    // date tapped is the date stored, regardless of local time zone.
                    state.selectedDateMillis?.let {
                        onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            ) {
                Text(stringResource(R.string.period_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.period_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}
