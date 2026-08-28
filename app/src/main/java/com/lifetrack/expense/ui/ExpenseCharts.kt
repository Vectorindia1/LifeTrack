package com.lifetrack.expense.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lifetrack.core.ui.Money
import com.lifetrack.core.ui.chart.ChartPoint
import com.lifetrack.core.ui.chart.SimpleBarChart
import com.lifetrack.core.ui.chart.SimpleLineChart
import com.lifetrack.expense.viewmodel.CategoryTotal
import com.lifetrack.expense.viewmodel.SpendPoint

/** Spend per category — PRD 7.4's bar chart. */
@Composable
fun CategoryBarChart(
    totals: List<CategoryTotal>,
    modifier: Modifier = Modifier,
) {
    SimpleBarChart(
        points = totals.map { ChartPoint(it.category, it.total) },
        color = MaterialTheme.colorScheme.primary,
        yFormatter = Money::formatCompact,
        modifier = modifier,
    )
}

/** Spend over time — PRD 7.4's line chart. */
@Composable
fun SpendLineChart(
    points: List<SpendPoint>,
    modifier: Modifier = Modifier,
) {
    SimpleLineChart(
        points = points.map { ChartPoint(it.label, it.total) },
        color = MaterialTheme.colorScheme.tertiary,
        yFormatter = Money::formatCompact,
        modifier = modifier,
    )
}
