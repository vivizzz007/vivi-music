package com.music.vivi.ui.component

import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.floor

/**
 * A custom Checkbox with rounded corners.
 */
@Composable
public fun RoundedCheckbox(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val strokeWidthPx = with(LocalDensity.current) { floor(CheckboxDefaults.StrokeWidth.toPx()) }
    val checkmarkStroke = remember(strokeWidthPx) {
        Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
    }
    val outlineStroke = remember(strokeWidthPx) { Stroke(width = strokeWidthPx) }

    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        checkmarkStroke = checkmarkStroke,
        outlineStroke = outlineStroke
    )
}
