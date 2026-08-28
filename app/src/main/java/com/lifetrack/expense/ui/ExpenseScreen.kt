package com.lifetrack.expense.ui

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifetrack.R
import com.lifetrack.core.ui.AppViewModelProvider
import com.lifetrack.core.ui.Money
import com.lifetrack.expense.data.Expense
import com.lifetrack.expense.viewmodel.ExpenseUiState
import com.lifetrack.expense.viewmodel.ExpenseViewModel
import com.lifetrack.expense.viewmodel.SpendWindow
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ExpenseScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }

    ExpenseContent(
        uiState = uiState,
        onWindow = viewModel::setWindow,
        onDelete = viewModel::delete,
        onAddClick = { showAddSheet = true },
        contentPadding = contentPadding,
        modifier = modifier,
    )

    if (showAddSheet) {
        AddExpenseSheet(
            knownCategories = uiState.knownCategories,
            onDismiss = { showAddSheet = false },
            onSave = { amount, category, note ->
                viewModel.add(amount, category, note)
                showAddSheet = false
            },
        )
    }
}

@Composable
private fun ExpenseContent(
    uiState: ExpenseUiState,
    onWindow: (SpendWindow) -> Unit,
    onDelete: (Expense) -> Unit,
    onAddClick: () -> Unit,
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
            item { TotalCard(uiState = uiState, onWindow = onWindow) }

            if (uiState.isEmpty) {
                item { EmptyExpenses() }
            } else {
                item {
                    ChartCard(title = stringResource(R.string.expense_by_category)) {
                        if (uiState.byCategory.isEmpty()) {
                            NoData()
                        } else {
                            CategoryBarChart(totals = uiState.byCategory)
                        }
                    }
                }
                item {
                    ChartCard(title = stringResource(R.string.expense_over_time)) {
                        if (uiState.overTime.size < 2) {
                            NoData()
                        } else {
                            SpendLineChart(points = uiState.overTime)
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.expense_recent),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(uiState.recent, key = { it.id }) { expense ->
                    ExpenseRow(expense = expense, onDelete = { onDelete(expense) })
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
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.expense_add))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TotalCard(
    uiState: ExpenseUiState,
    onWindow: (SpendWindow) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SpendWindow.entries.forEachIndexed { index, window ->
                    SegmentedButton(
                        selected = uiState.window == window,
                        onClick = { onWindow(window) },
                        shape = SegmentedButtonDefaults.itemShape(index, SpendWindow.entries.size),
                    ) {
                        Text(stringResource(window.labelRes()))
                    }
                }
            }
            Text(
                text = stringResource(R.string.expense_total_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = Money.format(uiState.windowTotal),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun NoData(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.expense_no_data),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM") }
    val date = remember(expense.timestamp) {
        expense.timestamp.atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(text = expense.category, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = expense.note?.let { "$date · $it" } ?: date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = Money.format(expense.amount),
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(R.string.expense_delete),
                )
            }
        }
    }
}

@Composable
private fun EmptyExpenses(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.expense_empty_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.expense_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun SpendWindow.labelRes(): Int = when (this) {
    SpendWindow.DAY -> R.string.expense_window_day
    SpendWindow.WEEK -> R.string.expense_window_week
    SpendWindow.MONTH -> R.string.expense_window_month
}
