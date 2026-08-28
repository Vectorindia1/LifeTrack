package com.lifetrack.habit.ui

import com.lifetrack.core.ui.theme.Accents
import com.lifetrack.core.ui.theme.resolved
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifetrack.core.ui.chart.ChartPoint
import com.lifetrack.core.ui.chart.SimpleBarChart
import com.lifetrack.habit.viewmodel.RateBar

/**
 * Completion rate as a bar chart — PRD's "simple graphs only" rule, so no markers,
 * no zoom, no second axis.
 */
@Composable
fun HabitCompletionChart(
    bars: List<RateBar>,
    modifier: Modifier = Modifier,
) {
    SimpleBarChart(
        // Percentages read better on an axis than 0..1 fractions.
        points = bars.map { ChartPoint(it.label, (it.rate * 100).toDouble()) },
        color = Accents.Habit.resolved,
        height = 160.dp,
        yFormatter = { "${it.toInt()}%" },
        modifier = modifier,
    )
}
