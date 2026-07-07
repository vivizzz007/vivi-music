/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.music.vivi.R

/**
 * Pure HSV/hex color conversions used by [ColorPickerDialog].
 * Kept free of android.* dependencies so they are unit-testable on the JVM.
 */
object ColorPickerConversions {

    /** Converts HSV components (hue 0..360, saturation/value 0..1) to an opaque [Color]. */
    fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
        val h = ((hue % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)

        val c = v * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = v - c

        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(r + m, g + m, b + m)
    }

    /** Extracts HSV components (hue 0..360, saturation/value 0..1) from [color]. */
    fun colorToHsv(color: Color): Triple<Float, Float, Float> {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        val hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * ((b - r) / delta + 2f)
            else -> 60f * ((r - g) / delta + 4f)
        }.let { ((it % 360f) + 360f) % 360f }
        val saturation = if (max == 0f) 0f else delta / max
        return Triple(hue, saturation, max)
    }

    /** Formats [color] as an uppercase RRGGBB hex string without the leading '#'. */
    fun colorToHex(color: Color): String {
        val argb = color.toArgb()
        return "%06X".format(argb and 0xFFFFFF)
    }

    /**
     * Parses a RRGGBB hex string (optionally prefixed with '#') to an opaque [Color],
     * or null if the input is not a valid 6-digit hex color.
     */
    fun parseHexColor(hex: String): Color? {
        val cleaned = hex.trim().removePrefix("#")
        if (cleaned.length != 6) return null
        val rgb = cleaned.toIntOrNull(16) ?: return null
        return Color(0xFF000000.toInt() or rgb)
    }
}

private val presetColors = listOf(
    Color.White,
    Color(0xFFE0E0E0),
    Color(0xFF9E9E9E),
    Color.Black,
    Color(0xFFEF5350),
    Color(0xFFFFB74D),
    Color(0xFFFFF176),
    Color(0xFF81C784),
    Color(0xFF4FC3F7),
    Color(0xFF7986CB),
    Color(0xFFBA68C8),
    Color(0xFFF06292),
)

/**
 * A full HSV color picker dialog with a saturation/value area, hue slider, hex input
 * and preset swatches. Calls [onConfirm] with the chosen color.
 *
 * The reset button selects [defaultColor] in the picker; when [onReset] is provided
 * it is invoked instead (for defaults that aren't a fixed color, e.g. theme-adaptive)
 * and the dialog is expected to be dismissed by the callback.
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
    defaultColor: Color? = null,
    onReset: (() -> Unit)? = null,
) {
    val initialHsv = remember(initialColor) { ColorPickerConversions.colorToHsv(initialColor) }
    var hue by rememberSaveable(initialColor) { mutableStateOf(initialHsv.first) }
    var saturation by rememberSaveable(initialColor) { mutableStateOf(initialHsv.second) }
    var value by rememberSaveable(initialColor) { mutableStateOf(initialHsv.third) }

    val selectedColor = ColorPickerConversions.hsvToColor(hue, saturation, value)
    var hexInput by rememberSaveable(initialColor) {
        mutableStateOf(ColorPickerConversions.colorToHex(initialColor))
    }
    var hexError by rememberSaveable { mutableStateOf(false) }

    fun selectColor(color: Color) {
        val (h, s, v) = ColorPickerConversions.colorToHsv(color)
        hue = h
        saturation = s
        value = v
        hexInput = ColorPickerConversions.colorToHex(color)
        hexError = false
    }

    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(title) },
        buttons = {
            if (defaultColor != null || onReset != null) {
                TextButton(
                    onClick = {
                        if (onReset != null) {
                            onReset()
                        } else {
                            defaultColor?.let(::selectColor)
                        }
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }
                Spacer(Modifier.weight(1f))
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
            TextButton(onClick = { onConfirm(selectedColor) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SaturationValuePanel(
                hue = hue,
                saturation = saturation,
                value = value,
                onChange = { s, v ->
                    saturation = s
                    value = v
                    hexInput = ColorPickerConversions.colorToHex(
                        ColorPickerConversions.hsvToColor(hue, s, v)
                    )
                    hexError = false
                },
            )

            Spacer(Modifier.height(16.dp))

            HueSlider(
                hue = hue,
                onChange = { h ->
                    hue = h
                    hexInput = ColorPickerConversions.colorToHex(
                        ColorPickerConversions.hsvToColor(h, saturation, value)
                    )
                    hexError = false
                },
            )

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )

                Spacer(Modifier.width(12.dp))

                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input.take(7)
                        val parsed = ColorPickerConversions.parseHexColor(input)
                        hexError = parsed == null
                        if (parsed != null) {
                            val (h, s, v) = ColorPickerConversions.colorToHsv(parsed)
                            hue = h
                            saturation = s
                            value = v
                        }
                    },
                    prefix = { Text("#") },
                    isError = hexError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    label = { Text(stringResource(R.string.color_picker_hex)) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                presetColors.take(6).forEach { preset ->
                    PresetSwatch(preset, selectedColor) { selectColor(preset) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                presetColors.drop(6).forEach { preset ->
                    PresetSwatch(preset, selectedColor) { selectColor(preset) }
                }
            }
        }
    }
}

@Composable
private fun PresetSwatch(
    color: Color,
    selectedColor: Color,
    onClick: () -> Unit,
) {
    val isSelected = color.toArgb() == selectedColor.toArgb()
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                },
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun SaturationValuePanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (saturation: Float, value: Float) -> Unit,
) {
    val hueColor = ColorPickerConversions.hsvToColor(hue, 1f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(hue) {
                detectTapGestures { offset ->
                    onChange(
                        (offset.x / size.width).coerceIn(0f, 1f),
                        1f - (offset.y / size.height).coerceIn(0f, 1f),
                    )
                }
            }
            .pointerInput(hue) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onChange(
                        (change.position.x / size.width).coerceIn(0f, 1f),
                        1f - (change.position.y / size.height).coerceIn(0f, 1f),
                    )
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

            val thumbCenter = Offset(saturation * size.width, (1f - value) * size.height)
            drawCircle(Color.Black.copy(alpha = 0.6f), radius = 10.dp.toPx(), center = thumbCenter)
            drawCircle(Color.White, radius = 8.dp.toPx(), center = thumbCenter)
            drawCircle(
                ColorPickerConversions.hsvToColor(hue, saturation, value),
                radius = 6.dp.toPx(),
                center = thumbCenter,
            )
        }
    }
}

@Composable
private fun HueSlider(
    hue: Float,
    onChange: (hue: Float) -> Unit,
) {
    val hueColors = remember {
        List(7) { i -> ColorPickerConversions.hsvToColor(i * 60f, 1f, 1f) }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onChange((offset.x / size.width).coerceIn(0f, 1f) * 360f)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onChange((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Brush.horizontalGradient(hueColors))

            val thumbCenter = Offset((hue / 360f) * size.width, size.height / 2f)
            drawCircle(Color.Black.copy(alpha = 0.6f), radius = 10.dp.toPx(), center = thumbCenter)
            drawCircle(Color.White, radius = 8.dp.toPx(), center = thumbCenter)
            drawCircle(
                ColorPickerConversions.hsvToColor(hue, 1f, 1f),
                radius = 6.dp.toPx(),
                center = thumbCenter,
            )
        }
    }
}
