package com.music.vivi.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.vivi.viewmodels.DayUsageData
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun ExpressiveBarChart(
    days: List<DayUsageData>,
    goalMs: Long = 2 * 60 * 60 * 1000L,
    todayIndex: Int,
    onDayClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    chartHeight: Dp = 200.dp
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val normalColor = MaterialTheme.colorScheme.secondaryContainer
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tooltipBgColor = MaterialTheme.colorScheme.onSurface
    val tooltipTextColor = MaterialTheme.colorScheme.surface

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)
    val yLabelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

    // Today = index 0 (Mon) … 6 (Sun)
    val todayIndex = remember {
        (LocalDate.now().dayOfWeek.value - DayOfWeek.MONDAY.value).coerceIn(0, 6)
    }

    val maxValue = remember(days) {
        (days.maxOfOrNull { it.totalMs } ?: 1L).toFloat().coerceAtLeast(1f)
    }

    // Spring-animated fraction per bar
    val animatedFractions = days.mapIndexed { index, day ->
        val target = day.totalMs.toFloat() / maxValue
        val fraction by animateFloatAsState(
            targetValue = target,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "barFraction_$index"
        )
        fraction
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .pointerInput(days, todayIndex) {
                detectTapGestures(
                    onLongPress = { offset ->
                        val leftPad = 4.dp.toPx()
                        val rightPad = 28.dp.toPx()
                        val drawWidth = size.width - leftPad - rightPad
                        val barCount = days.size.coerceAtLeast(1)
                        val barSpacing = 6.dp.toPx()
                        val barWidth = ((drawWidth - barSpacing * (barCount - 1)) / barCount)

                        var hit: Int? = null
                        val clickX = offset.x
                        val slop = barSpacing / 2f

                        for (i in days.indices) {
                            val isToday = i == todayIndex
                            val effectiveWidth = if (isToday) barWidth * 1.15f else barWidth
                            val barLeft = leftPad + i * (barWidth + barSpacing) - if (isToday) (effectiveWidth - barWidth) / 2f else 0f
                            if (clickX >= barLeft - slop && clickX <= barLeft + effectiveWidth + slop) {
                                hit = i
                                break
                            }
                        }
                        
                        if (hit != null) {
                            selectedIndex = hit
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1000)
                                if (selectedIndex == hit) { // Only dismiss if user hasn't pressed another bar
                                    selectedIndex = null
                                }
                            }
                        } else {
                            selectedIndex = null
                        }
                    },
                    onTap = { offset ->
                        val leftPad = 4.dp.toPx()
                        val rightPad = 28.dp.toPx()
                        val drawWidth = size.width - leftPad - rightPad
                        val barCount = days.size.coerceAtLeast(1)
                        val barSpacing = 6.dp.toPx()
                        val barWidth = ((drawWidth - barSpacing * (barCount - 1)) / barCount)

                        var hit: Int? = null
                        val clickX = offset.x
                        val slop = barSpacing / 2f

                        for (i in days.indices) {
                            val isToday = i == todayIndex
                            val effectiveWidth = if (isToday) barWidth * 1.15f else barWidth
                            val barLeft = leftPad + i * (barWidth + barSpacing) - if (isToday) (effectiveWidth - barWidth) / 2f else 0f
                            if (clickX >= barLeft - slop && clickX <= barLeft + effectiveWidth + slop) {
                                hit = i
                                break
                            }
                        }
                        
                        if (selectedIndex != null) {
                            selectedIndex = null
                        } else if (hit != null) {
                            onDayClick(days[hit].timestamp)
                        }
                    }
                )
            }
    ) {
        val bottomPad = 28.dp.toPx()
        val topPad = 16.dp.toPx()
        val leftPad = 4.dp.toPx()
        val rightPad = 28.dp.toPx()

        val drawWidth = size.width - leftPad - rightPad
        val drawHeight = size.height - bottomPad - topPad
        val barCount = days.size.coerceAtLeast(1)
        val barSpacing = 6.dp.toPx()
        val barWidth = ((drawWidth - barSpacing * (barCount - 1)) / barCount)
            .coerceAtLeast(8.dp.toPx())

        // Y-axis labels
        drawYLabels(
            textMeasurer = textMeasurer,
            style = yLabelStyle,
            labelColor = labelColor,
            maxMs = maxValue.toLong(),
            rightEdge = size.width,
            topPad = topPad,
            drawHeight = drawHeight
        )

        days.forEachIndexed { index, dayData ->
            val fraction = animatedFractions.getOrElse(index) { 0f }
            val isToday = index == todayIndex
            val barColor = if (isToday) primaryColor else normalColor
            val effectiveWidth = if (isToday) barWidth * 1.15f else barWidth
            val barLeft = leftPad + index * (barWidth + barSpacing) -
                    if (isToday) (effectiveWidth - barWidth) / 2f else 0f
            val barHeight = (drawHeight * fraction).coerceAtLeast(4.dp.toPx())
            val barTop = topPad + drawHeight - barHeight

            // Pill bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(barLeft, barTop),
                size = Size(effectiveWidth, barHeight),
                cornerRadius = CornerRadius(effectiveWidth / 2f, effectiveWidth / 2f)
            )

            // Badge = starburst shape marks today
            val badgeRadius = 10.dp.toPx()
            
            // Calculate the TARGET height of the bar to prevent jumping mid-animation
            val targetFraction = dayData.totalMs.toFloat() / maxValue
            val targetBarHeight = (drawHeight * targetFraction).coerceAtLeast(4.dp.toPx())
            val fitsInside = targetBarHeight > (badgeRadius * 2 + 12.dp.toPx())

            if (isToday) {
                val badgeCx = barLeft + effectiveWidth / 2f

                // If target bar is tall enough, put badge inside near the top. Otherwise, put it above.
                val badgeCy = if (fitsInside) {
                    barTop + badgeRadius + 4.dp.toPx()
                } else {
                    barTop - badgeRadius - 4.dp.toPx()
                }

                // Adaptive colors:
                // Inside bar: white badge, primary tick
                // Above bar: primary badge, white tick
                val badgeBgColor = if (fitsInside) onPrimaryColor.copy(alpha = 0.9f) else primaryColor
                val badgeTickColor = if (fitsInside) primaryColor else onPrimaryColor

                // Draw starburst (8-point star / cookie / seal shape)
                val path = androidx.compose.ui.graphics.Path()
                val points = 8
                val innerRadius = badgeRadius * 0.65f
                for (i in 0 until points * 2) {
                    val angle = Math.PI / points * i - Math.PI / 2
                    val r = if (i % 2 == 0) badgeRadius else innerRadius
                    val x = badgeCx + (r * kotlin.math.cos(angle)).toFloat()
                    val y = badgeCy + (r * kotlin.math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                
                if (fitsInside) {
                    drawPath(path = path, color = onPrimaryColor.copy(alpha = 0.25f)) // subtle ring
                }
                drawPath(path = path, color = badgeBgColor)

                // Checkmark inside
                val ck = 3.dp.toPx()
                drawLine(
                    color = badgeTickColor,
                    start = Offset(badgeCx - ck * 0.6f, badgeCy),
                    end = Offset(badgeCx - ck * 0.1f, badgeCy + ck * 0.55f),
                    strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round
                )
                drawLine(
                    color = badgeTickColor,
                    start = Offset(badgeCx - ck * 0.1f, badgeCy + ck * 0.55f),
                    end = Offset(badgeCx + ck * 0.7f, badgeCy - ck * 0.5f),
                    strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round
                )
            }

            // Tooltip for selected bar
            if (selectedIndex == index) {
                val tooltipText = formatMsFull(dayData.totalMs)
                val tipStyle = TextStyle(
                    fontSize = 12.sp,
                    color = tooltipTextColor,
                    fontWeight = FontWeight.SemiBold
                )
                val tipResult = textMeasurer.measure(tooltipText, tipStyle)
                
                val tipPadX = 8.dp.toPx()
                val tipPadY = 4.dp.toPx()
                val tipWidth = tipResult.size.width + tipPadX * 2
                val tipHeight = tipResult.size.height + tipPadY * 2
                
                val badgeClearance = if (isToday && !fitsInside) badgeRadius * 2 + 12.dp.toPx() else 0f
                val tipCx = barLeft + effectiveWidth / 2f
                val tipBottom = barTop - badgeClearance - 6.dp.toPx()
                val tipTopLeft = Offset(tipCx - tipWidth / 2f, tipBottom - tipHeight)
                
                // Tooltip background
                drawRoundRect(
                    color = tooltipBgColor,
                    topLeft = tipTopLeft,
                    size = Size(tipWidth, tipHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                
                // Tooltip text
                drawText(
                    textLayoutResult = tipResult,
                    topLeft = Offset(tipTopLeft.x + tipPadX, tipTopLeft.y + tipPadY),
                    color = tooltipTextColor
                )
            }

            // X label
            val dayLabel = listOf("M", "T", "W", "T", "F", "S", "S").getOrElse(index) { "" }
            val labelResult = textMeasurer.measure(dayLabel, labelStyle)
            drawText(
                textLayoutResult = labelResult,
                topLeft = Offset(
                    barLeft + effectiveWidth / 2f - labelResult.size.width / 2f,
                    size.height - bottomPad + 6.dp.toPx()
                ),
                color = if (isToday) primaryColor else labelColor
            )
        }
    }
}

private fun DrawScope.drawYLabels(
    textMeasurer: TextMeasurer,
    style: TextStyle,
    labelColor: Color,
    maxMs: Long,
    rightEdge: Float,
    topPad: Float,
    drawHeight: Float
) {
    listOf(0L to 1f, maxMs / 2 to 0.5f, maxMs to 0f).forEach { (ms, invertedFraction) ->
        val y = topPad + drawHeight * invertedFraction
        val label = if (ms == 0L) "0" else formatMsShort(ms)
        val result = textMeasurer.measure(label, style)
        drawText(
            textLayoutResult = result,
            topLeft = Offset(rightEdge - result.size.width - 2.dp.toPx(), y - result.size.height / 2f),
            color = labelColor
        )
    }
}

private fun formatMsShort(ms: Long): String {
    val h = ms / 3600000
    val m = (ms % 3600000) / 60000
    return if (h > 0) "${h}h" else "${m}m"
}

private fun formatMsFull(ms: Long): String {
    val h = ms / 3600000
    val m = (ms % 3600000) / 60000
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}
