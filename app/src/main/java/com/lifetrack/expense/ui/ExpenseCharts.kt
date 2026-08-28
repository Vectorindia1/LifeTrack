package com.lifetrack.expense.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.lifetrack.core.ui.Money
import com.lifetrack.expense.viewmodel.CategoryTotal
import com.lifetrack.expense.viewmodel.SpendPoint
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent

/** Spend per category — PRD 7.4's bar chart. */
@Composable
fun CategoryBarChart(
    totals: List<CategoryTotal>,
    modifier: Modifier = Modifier,
) {
    if (totals.isEmpty()) return
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(totals) {
        modelProducer.runTransaction {
            columnModel { series(totals.map { it.total }) }
        }
    }

    val labels = totals.map { it.category }
    val barColor = MaterialTheme.colorScheme.primary

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(
                        fill = Fill(barColor),
                        thickness = 16.dp,
                        shape = RectangleShape,
                    ),
                ),
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    Money.formatCompact(value)
                },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    // Category names are long; the first few characters are enough.
                    labels.getOrElse(value.toInt()) { "" }.take(4)
                },
            ),
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
    )
}

/** Spend over time — PRD 7.4's line chart. */
@Composable
fun SpendLineChart(
    points: List<SpendPoint>,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(points) {
        modelProducer.runTransaction {
            lineModel { series(points.map { it.total }) }
        }
    }

    val labels = points.map { it.label }
    val lineColor = MaterialTheme.colorScheme.tertiary

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(Fill(lineColor)),
                    ),
                ),
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    Money.formatCompact(value)
                },
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
            .height(180.dp),
    )
}
