/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.vivi.R
import com.music.vivi.constants.CrossfadeCurve
import java.util.Locale

/**
 * Live preview of a [CrossfadeCurve]: plots the outgoing (fading-out) and incoming
 * (fading-in) volume ramps across the configured crossfade duration, so the person
 * can see the shape of the curve before committing to it — similar to Morphe's
 * crossfade curve preview.
 */
@Composable
fun CrossfadeCurvePreview(
    curve: CrossfadeCurve,
    durationSeconds: Float,
    modifier: Modifier = Modifier,
) {
    val outgoingColor = MaterialTheme.colorScheme.tertiary
    val incomingColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor, fontSize = 9.sp)
    val legendStyle = MaterialTheme.typography.labelSmall

    val outgoingLabel = stringResource(R.string.crossfade_curve_preview_outgoing)
    val incomingLabel = stringResource(R.string.crossfade_curve_preview_incoming)
    val curveName = when (curve) {
        CrossfadeCurve.EQUAL_POWER -> stringResource(R.string.crossfade_curve_entry_equal_power)
        CrossfadeCurve.EASE_OUT_QUAD -> stringResource(R.string.crossfade_curve_entry_ease_out_quad)
        CrossfadeCurve.EASE_OUT_CUBIC -> stringResource(R.string.crossfade_curve_entry_ease_out_cubic)
        CrossfadeCurve.SMOOTHSTEP -> stringResource(R.string.crossfade_curve_entry_smoothstep)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendDot(outgoingColor)
                Spacer(Modifier.width(4.dp))
                Text(text = outgoingLabel, style = legendStyle, color = labelColor)
                Spacer(Modifier.width(12.dp))
                LegendDot(incomingColor)
                Spacer(Modifier.width(4.dp))
                Text(text = incomingLabel, style = legendStyle, color = labelColor)
            }
            Text(
                text = curveName,
                style = legendStyle,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            val padL = 28.dp.toPx()
            val padR = 4.dp.toPx()
            val padT = 6.dp.toPx()
            val padB = 16.dp.toPx()
            val gW = size.width - padL - padR
            val gH = size.height - padT - padB

            // Horizontal grid lines + volume-percentage labels.
            for (i in 0..4) {
                val y = padT + gH * i / 4f
                drawLine(
                    color = gridColor,
                    start = Offset(padL, y),
                    end = Offset(size.width - padR, y),
                    strokeWidth = 1f,
                )
                val pct = 100 - i * 25
                drawText(
                    textMeasurer = textMeasurer,
                    text = "$pct%",
                    style = labelStyle,
                    topLeft = Offset(0f, (y - 6.dp.toPx()).coerceAtLeast(0f)),
                )
            }

            // Time labels along the x-axis, spread across the configured duration.
            for (i in 0..4) {
                val x = padL + gW * i / 4f
                val seconds = durationSeconds * i / 4f
                val label = when {
                    seconds <= 0f -> "0s"
                    seconds % 1f == 0f -> "${seconds.toInt()}s"
                    else -> String.format(Locale.US, "%.1fs", seconds)
                }
                val measured = textMeasurer.measure(label, labelStyle)
                val textX = (x - measured.size.width / 2f)
                    .coerceIn(0f, size.width - measured.size.width)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(textX, size.height - padB + 2.dp.toPx()),
                )
            }

            // Plot both curves across the fade duration.
            val outPath = Path()
            val inPath = Path()
            val steps = 60
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val x = padL + t * gW
                val outY = padT + (1f - curve.fadeOut(t)) * gH
                val inY = padT + (1f - curve.fadeIn(t)) * gH
                if (i == 0) {
                    outPath.moveTo(x, outY)
                    inPath.moveTo(x, inY)
                } else {
                    outPath.lineTo(x, outY)
                    inPath.lineTo(x, inY)
                }
            }

            drawPath(
                path = outPath,
                color = outgoingColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            )
            drawPath(
                path = inPath,
                color = incomingColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color),
    )
}
