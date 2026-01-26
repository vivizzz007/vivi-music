package com.music.vivi.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

val DefaultThemeColor = Color(0xFFED5564)

/**
 * Main theme composable for the Music Application.
 * Configures the MaterialTheme with dynamic colors, typography, and shapes.
 *
 * @param darkTheme Whether to use the dark theme (defaults to system setting).
 * @param pureBlack Whether to use pure black for background in dark mode (OLED optimization).
 * @param themeColor The seed color for generating the color scheme.
 * @param enableDynamicTheme Whether to use Android 12+ dynamic system colors if available.
 * @param overrideColorScheme Optional specific color scheme to use, overriding generation logic.
 * @param expressive Whether to use the expressive design system (typography and shapes).
 * @param content The content to display within the theme.
 */
@Composable
fun MusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    enableDynamicTheme: Boolean = true,
    overrideColorScheme: ColorScheme? = null,
    expressive: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Determine if system dynamic colors should be used (Android S+ and default theme color)
    val useSystemDynamicColor = (
        enableDynamicTheme &&
            themeColor == DefaultThemeColor &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        )

    // Select the appropriate color scheme generation method
    val baseColorScheme = if (overrideColorScheme != null) {
        overrideColorScheme
    } else if (useSystemDynamicColor) {
        // Use standard Material 3 dynamic color functions for system wallpaper colors
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Use materialKolor only when a specific seed color is provided
        rememberDynamicColorScheme(
            seedColor = themeColor, // themeColor is guaranteed non-default here
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot // Keep existing style
        )
    }

    // Apply pureBlack modification if needed, similar to original logic
    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    // Use standard MaterialTheme instead of MaterialExpressiveTheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = if (expressive) ExpressiveTypography else AppTypography,
        shapes = if (expressive) ExpressiveShapes else DefaultShapes,
        content = content
    )
}

/**
 * Extracts a dominant theme color from the Bitmap using Palette API.
 * Uses scoring to find the most suitable color.
 */
fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

/**
 * Extracts a list of colors suitable for a gradient from the Bitmap.
 * Returns at least two colors (primary and secondary/background).
 */
fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2) {
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    } else {
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
    }
}

fun ColorScheme.pureBlack(apply: Boolean) = if (apply) {
    copy(
        surface = Color.Black,
        background = Color.Black
    )
} else {
    this
}

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
