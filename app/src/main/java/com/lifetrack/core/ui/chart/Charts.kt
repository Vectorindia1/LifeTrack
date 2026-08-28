package com.lifetrack.core.ui.chart

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent

/**
 * The two chart shapes this app is allowed to draw.
 *
 * CLAUDE.md restricts graphs to bar, line or progress ring, so wrapping Vico once
 * here keeps every chart consistent and keeps Vico's API surface — which changes
 * sharply between major versions — confined to a single file. Progress rings are
 * plain Compose and deliberately not here.
 *
 * Vico 3.x API; see MEMORY.md before changing any of this.
 */

/** One data point: a bar/point value and the label under it. */
data class ChartPoint(val label: String, val value: Double)

/** Bars get slightly rounded tops rather than hard rectangles — a small but visible polish touch. */
private val BarShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)

@Composable
fun SimpleBarChart(
    points: List<ChartPoint>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    maxLabelChars: Int = 4,
    yFormatter: (Double) -> String = { it.toInt().toString() },
) {
    if (points.isEmpty()) return
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(points) {
        modelProducer.runTransaction {
            columnModel { series(points.map { it.value }) }
        }
    }

    val labels = points.map { it.label }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(
                        fill = Fill(color),
                        thickness = 16.dp,
                        shape = BarShape,
                    ),
                ),
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = CartesianValueFormatter { _, value, _ -> yFormatter(value) },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    labels.getOrElse(value.toInt()) { "" }.take(maxLabelChars)
                },
            ),
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    )
}

@Composable
fun SimpleLineChart(
    points: List<ChartPoint>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    yFormatter: (Double) -> String = { it.toInt().toString() },
) {
    // A single point is not a line; drawing one is misleading.
    if (points.size < 2) return
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(points) {
        modelProducer.runTransaction {
            lineModel { series(points.map { it.value }) }
        }
    }

    val labels = points.map { it.label }
    // A soft gradient fill under the line, fading to nothing — the "area chart" look
    // from the design reference, without turning this into a second chart type.
    val areaFill = remember(color) {
        Fill(
            Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.32f), color.copy(alpha = 0f)),
            ),
        )
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(Fill(color)),
                        areaFill = LineCartesianLayer.AreaFill.single(areaFill),
                    ),
                ),
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = CartesianValueFormatter { _, value, _ -> yFormatter(value) },
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
            .height(height),
    )
}
