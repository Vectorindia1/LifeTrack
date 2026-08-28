package com.lifetrack.habit.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.lifetrack.habit.viewmodel.RateBar

/**
 * Completion rate as a simple bar chart — PRD's "simple graphs only" rule, so no
 * markers, no zoom, no second axis.
 *
 * Vico 3.x API. Examples for Vico 1.x/2.x will not compile against this; see MEMORY.md.
 */
@Composable
fun HabitCompletionChart(
    bars: List<RateBar>,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(bars) {
        if (bars.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            columnModel {
                // Percentages read better on an axis than 0..1 fractions.
                series(bars.map { (it.rate * 100).toInt() })
            }
        }
    }

    if (bars.isEmpty()) return

    val labels = bars.map { it.label }
    val columnColor = MaterialTheme.colorScheme.primary

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(
                        fill = Fill(columnColor),
                        thickness = 16.dp,
                        shape = RectangleShape,
                    ),
                ),
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = { _, value, _ -> "${value.toInt()}%" },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    labels.getOrElse(value.toInt()) { "" }
                },
            ),
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
    )
}
