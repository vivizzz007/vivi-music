package com.music.vivi.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
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
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score
import kotlin.math.abs

val DefaultThemeColor = Color(0xFFED5564)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun musicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Determine if system dynamic colors should be used (Android S+ and default theme color)
    val useSystemDynamicColor = (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    // Select the appropriate color scheme generation method
    val baseColorScheme = if (useSystemDynamicColor) {
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

    // Use the defined M3 Expressive Typography
    // TODO: Define M3 Expressive Shapes instance if needed
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = AppTypography, // Use the defined AppTypography
        // shapes = MaterialTheme.shapes, // Placeholder - Needs update (Shapes not used in original)
        content = content
    )
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(128) // Increased for better color variety
        .addFilter { rgb, hsl ->
            // Filter out colors that are too desaturated or too extreme in lightness
            val saturation = hsl[1]
            val lightness = hsl[2]
            saturation > 0.15f && lightness > 0.1f && lightness < 0.9f
        }
        .generate()
        .swatches

    if (extractedColors.isEmpty()) {
        return getDefaultGradient()
    }

    // Get dominant colors with good contrast and visual appeal
    val dominantColors = extractedColors
        .sortedByDescending { it.population * getColorScore(it) }
        .take(8)
        .map { it.rgb }

    return createStunningGradient(dominantColors)
}

private fun getColorScore(swatch: Palette.Swatch): Float {
    val color = Color(swatch.rgb)
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(swatch.rgb, hsl)

    val saturation = hsl[1]
    val lightness = hsl[2]

    // Prefer colors with good saturation and avoid muddy colors
    val saturationScore = when {
        saturation > 0.7f -> 1.0f
        saturation > 0.4f -> 0.8f
        saturation > 0.2f -> 0.4f
        else -> 0.1f
    }

    // Prefer colors in the sweet spot of lightness
    val lightnessScore = when {
        lightness in 0.3f..0.7f -> 1.0f
        lightness in 0.2f..0.8f -> 0.8f
        else -> 0.3f
    }

    return saturationScore * lightnessScore
}

private fun createStunningGradient(colors: List<Int>): List<Color> {
    if (colors.size < 2) return getDefaultGradient()

    // Find the most visually appealing color combinations
    val colorPairs = mutableListOf<Pair<Color, Color>>()

    for (i in colors.indices) {
        for (j in i + 1 until colors.size) {
            val color1 = Color(colors[i])
            val color2 = Color(colors[j])

            val contrast = getContrastRatio(color1, color2)
            val harmony = getColorHarmony(color1, color2)

            // Prefer pairs with good contrast but also harmony
            if (contrast > 1.5f && harmony > 0.3f) {
                colorPairs.add(color1 to color2)
            }
        }
    }

    if (colorPairs.isEmpty()) {
        return listOf(Color(colors[0]), enhanceColor(Color(colors.getOrElse(1) { colors[0] })))
    }

    // Sort by visual appeal score
    val bestPair = colorPairs
        .sortedByDescending { (c1, c2) ->
            getVisualAppealScore(c1, c2)
        }
        .first()

    return listOf(bestPair.first, bestPair.second)
}

private fun getContrastRatio(color1: Color, color2: Color): Float {
    val luminance1 = color1.luminance()
    val luminance2 = color2.luminance()
    val lighter = maxOf(luminance1, luminance2)
    val darker = minOf(luminance1, luminance2)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun getColorHarmony(color1: Color, color2: Color): Float {
    val hsl1 = FloatArray(3)
    val hsl2 = FloatArray(3)
    ColorUtils.colorToHSL(color1.toArgb(), hsl1)
    ColorUtils.colorToHSL(color2.toArgb(), hsl2)

    val hueDiff = minOf(
        abs(hsl1[0] - hsl2[0]),
        360f - abs(hsl1[0] - hsl2[0])
    )

    // Complementary (180°), triadic (120°), or analogous (30°) relationships
    return when {
        hueDiff in 170f..190f -> 1.0f // Complementary
        hueDiff in 110f..130f -> 0.8f // Triadic
        hueDiff in 20f..40f -> 0.7f   // Analogous
        hueDiff in 80f..100f -> 0.6f  // Square
        else -> 0.3f
    }
}

private fun getVisualAppealScore(color1: Color, color2: Color): Float {
    val contrast = getContrastRatio(color1, color2)
    val harmony = getColorHarmony(color1, color2)
    val vibrancy = getVibrancyScore(color1, color2)

    return (contrast * 0.3f + harmony * 0.4f + vibrancy * 0.3f)
}

private fun getVibrancyScore(color1: Color, color2: Color): Float {
    val hsl1 = FloatArray(3)
    val hsl2 = FloatArray(3)
    ColorUtils.colorToHSL(color1.toArgb(), hsl1)
    ColorUtils.colorToHSL(color2.toArgb(), hsl2)

    val avgSaturation = (hsl1[1] + hsl2[1]) / 2f
    val avgLightness = (hsl1[2] + hsl2[2]) / 2f

    // Prefer vibrant but not overwhelming colors
    val saturationScore = when {
        avgSaturation > 0.6f -> 1.0f
        avgSaturation > 0.3f -> 0.8f
        else -> 0.4f
    }

    val lightnessScore = when {
        avgLightness in 0.3f..0.7f -> 1.0f
        else -> 0.6f
    }

    return saturationScore * lightnessScore
}

private fun enhanceColor(color: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)

    // Enhance saturation and adjust lightness for better visual impact
    hsl[1] = minOf(1.0f, hsl[1] * 1.2f) // Boost saturation
    hsl[2] = when {
        hsl[2] < 0.3f -> hsl[2] * 1.3f    // Brighten dark colors
        hsl[2] > 0.7f -> hsl[2] * 0.8f    // Darken light colors
        else -> hsl[2]
    }

    return Color(ColorUtils.HSLToColor(hsl))
}

private fun getDefaultGradient(): List<Color> {
    // Dynamic defaults based on current trends - deep, rich colors
    return listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF8B5CF6)  // Purple
    )
}

// Extension function for even more stunning gradients with multiple stops
fun Bitmap.extractMultiStopGradient(stops: Int = 3): List<Color> {
    val baseColors = extractGradientColors()
    if (stops <= 2) return baseColors

    val additionalColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .sortedByDescending { it.population * getColorScore(it) }
        .take(stops + 2)
        .map { Color(it.rgb) }

    return createBalancedGradientStops(baseColors + additionalColors, stops)
}

private fun createBalancedGradientStops(colors: List<Color>, targetStops: Int): List<Color> {
    if (colors.size <= targetStops) return colors.take(targetStops)

    // Use color theory to create pleasing intermediate colors
    return colors
        .distinctBy { it.toArgb() }
        .sortedBy { color ->
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(color.toArgb(), hsl)
            hsl[0] // Sort by hue for smooth transitions
        }
        .take(targetStops)
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
