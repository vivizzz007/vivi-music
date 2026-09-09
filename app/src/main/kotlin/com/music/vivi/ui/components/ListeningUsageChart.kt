package com.music.vivi.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.motionScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.AutoScrollCondition
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.VicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.VicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberFadingEdges
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.DashedShape
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.Position
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.common.data.ExtraStore

private data class ColumnProviderWithLimit(
    private val limit: Double,
    private val belowLimitComponent: LineComponent,
    private val aboveLimitComponent: LineComponent
) : ColumnCartesianLayer.ColumnProvider {
    override fun getColumn(
        entry: ColumnCartesianLayerModel.Entry,
        seriesIndex: Int,
        extraStore: ExtraStore,
    ): LineComponent {
        return if (entry.y < limit) belowLimitComponent
        else aboveLimitComponent
    }

    override fun getWidestSeriesColumn(
        seriesIndex: Int,
        extraStore: ExtraStore
    ): LineComponent {
        return if (belowLimitComponent.thickness > aboveLimitComponent.thickness) belowLimitComponent
        else aboveLimitComponent
    }
}

fun columnProviderWithLimit(
    limit: Number,
    belowLimitComponent: LineComponent,
    aboveLimitComponent: LineComponent
): ColumnCartesianLayer.ColumnProvider =
    ColumnProviderWithLimit(limit.toDouble(), belowLimitComponent, aboveLimitComponent)

@Composable
fun ListeningUsageChart(
    modelProducer: CartesianChartModelProducer,
    goal: Long,
    modifier: Modifier = Modifier,
    zoomEnabled: Boolean = true,
    thickness: Dp = 24.dp,
    columnCollectionSpacing: Dp = 4.dp,
    xValueFormatter: CartesianValueFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            days.getOrNull(value.toInt() % 7) ?: ""
        }
    },
    yValueFormatter: CartesianValueFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            if (value >= 60 * 60 * 1000) {
                "${(value / (60 * 60 * 1000)).toInt()} hr"
            } else {
                "${(value / (60 * 1000)).toInt()} m"
            }
        }
    },
    markerValueFormatter: DefaultCartesianMarker.ValueFormatter = remember {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val first = targets.firstOrNull()
            val value = if (first is ColumnCartesianLayerMarkerTarget) {
                first.columns.sumOf { it.entry.y.toLong() }
            } else 0L

            if (value >= 60 * 60 * 1000) {
                val hours = value / (60 * 60 * 1000)
                val minutes = (value % (60 * 60 * 1000)) / (60 * 1000)
                if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
            } else {
                "${value / (60 * 1000)}m"
            }
        }
    },
    zoomState: VicoZoomState = rememberVicoZoomState(
        zoomEnabled = zoomEnabled,
        initialZoom = Zoom.max(Zoom.Content, Zoom.fixed()),
        minZoom = Zoom.min(Zoom.Content, Zoom.fixed())
    ),
    scrollState: VicoScrollState = rememberVicoScrollState(
        initialScroll = Scroll.Absolute.End,
        autoScrollCondition = AutoScrollCondition.OnModelGrowth
    ),
    animationSpec: AnimationSpec<Float>? = null
) {
    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart =
                rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProviderWithLimit(
                            limit = goal,
                            belowLimitComponent = rememberLineComponent(
                                fill = Fill(colorScheme.secondary),
                                thickness = thickness,
                                shape = CircleShape
                            ),
                            aboveLimitComponent = rememberLineComponent(
                                fill = Fill(colorScheme.primary),
                                thickness = thickness,
                                shape = CircleShape
                            )
                        ),
                        columnCollectionSpacing = columnCollectionSpacing
                    ),
                    startAxis = VerticalAxis.rememberStart(
                        line = rememberLineComponent(Fill.Transparent, 8.dp),
                        label = rememberTextComponent(typography.bodySmall.copy(colorScheme.onSurface)),
                        tick = null,
                        guideline = null,
                        itemPlacer = VerticalAxis.ItemPlacer.count({ 4 }),
                        valueFormatter = yValueFormatter
                    ),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        line = rememberLineComponent(Fill.Transparent, 8.dp),
                        label = rememberTextComponent(typography.bodySmall.copy(colorScheme.onSurface)),
                        tick = null,
                        guideline = null,
                        valueFormatter = xValueFormatter
                    ),
                    decorations = if (goal > 0) listOf(
                        HorizontalLine(
                            y = { goal.toDouble() },
                            line = rememberLineComponent(
                                fill = Fill(colorScheme.primary),
                                thickness = 1.dp,
                                shape = DashedShape(
                                    shape = CircleShape,
                                    dashLength = 2.dp,
                                    gapLength = 2.dp
                                )
                            ),
                            horizontalLabelPosition = Position.Horizontal.Start,
                            verticalLabelPosition = Position.Vertical.Center
                        )
                    )
                    else emptyList(),
                    marker = rememberDefaultCartesianMarker(
                        rememberTextComponent(
                            style = TextStyle(
                                fontFamily = typography.bodyLarge.fontFamily,
                                color = colorScheme.inverseOnSurface,
                                fontSize = typography.bodySmall.fontSize,
                                lineHeight = typography.bodySmall.lineHeight,
                            ),
                            background = rememberShapeComponent(
                                fill = Fill(colorScheme.inverseSurface),
                                shape = shapes.small
                            ),
                            padding = Insets(vertical = 4.dp, horizontal = 8.dp),
                            margins = Insets(bottom = 2.dp)
                        ),
                        valueFormatter = markerValueFormatter,
                        guideline = rememberLineComponent(
                            fill = Fill(colorScheme.primary),
                            shape = DashedShape(
                                shape = CircleShape,
                                dashLength = 2.dp,
                                gapLength = 2.dp
                            )
                        )
                    ),
                    fadingEdges = rememberFadingEdges()
                ),
            modelProducer = modelProducer,
            zoomState = zoomState,
            scrollState = scrollState,
            animationSpec = animationSpec,
            modifier = modifier.height(226.dp),
        )
    }
}
