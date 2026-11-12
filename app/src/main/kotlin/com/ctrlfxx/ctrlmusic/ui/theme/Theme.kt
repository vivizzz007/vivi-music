/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.ctrlfxx.ctrlmusic.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
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
import androidx.core.graphics.ColorUtils
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

import androidx.compose.runtime.getValue
import com.ctrlfxx.ctrlmusic.constants.SelectedFontKey
import com.ctrlfxx.ctrlmusic.constants.CustomFontPathKey
import com.ctrlfxx.ctrlmusic.constants.AppFont
import com.ctrlfxx.ctrlmusic.utils.rememberPreference
import androidx.compose.ui.text.font.FontFamily
import android.graphics.Typeface as AndroidTypeface
import androidx.compose.ui.text.font.Typeface as ComposeTypeface

// Ctrl Music colors. The logo keeps its rose gold; the app's default accent is blue.
val CtrlAccent = Color(0xFFB5928D)
val CtrlBlue = Color(0xFF0A84FF)
val CtrlBackground = Color(0xFF1F1F1F)
val DefaultThemeColor = CtrlBlue

@Composable
fun CtrlMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    tintSurfaces: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val selectedFontValue by rememberPreference(SelectedFontKey, AppFont.SF_PRO.value)
    val customFontPath by rememberPreference(CustomFontPathKey, "")

    val brandFont = remember(selectedFontValue, customFontPath) {
        when (AppFont.fromValue(selectedFontValue)) {
            AppFont.SYSTEM -> FontFamily.Default
            AppFont.SF_PRO -> SfProDisplayFontFamily
            AppFont.OUTFIT -> OutfitFontFamily
            AppFont.PLUS_JAKARTA_SANS -> PlusJakartaSansFontFamily
            AppFont.CUSTOM -> {
                try {
                    if (customFontPath.isNotEmpty() && java.io.File(customFontPath).exists()) {
                        val typeface = AndroidTypeface.createFromFile(customFontPath)
                        FontFamily(ComposeTypeface(typeface))
                    } else {
                        FontFamily.Default
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    FontFamily.Default
                }
            }
        }
    }

        val typography = remember(brandFont) {
        getTypography(brandFont = brandFont, plainFont = brandFont)
    }


    // Ctrl Music always uses its own palette instead of wallpaper colors.
    val useSystemDynamicColor = false

    // Select the appropriate color scheme generation method
    val baseColorScheme = if (useSystemDynamicColor) {
        // Use standard Material 3 dynamic color functions for system wallpaper colors
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Use materialKolor only when a specific seed color is provided
        rememberDynamicColorScheme(
            seedColor = themeColor, // themeColor is guaranteed non-default here
            isDark = true,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = if (themeColor.toArgb() == 0xFF000000.toInt()) PaletteStyle.Monochrome else PaletteStyle.TonalSpot
        )
    }

    // Apply pureBlack modification if needed, similar to original logic
    // Ctrl Music is dark-only for now; a light theme is planned for a later version.
    val colorScheme = remember(baseColorScheme, pureBlack, themeColor, tintSurfaces) {
        val ctrl = baseColorScheme.ctrlDark(themeColor, tintSurfaces)
        if (pureBlack) ctrl.pureBlack(true) else ctrl
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = typography, // Use the dynamically configured typography
        motionScheme = MotionScheme.expressive(),
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
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

/** Keeps an accent readable on charcoal: never too dark, too pale or too washed out. */
fun normalizeAccent(color: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    val isNeutral = hsl[1] < 0.12f
    if (!isNeutral) hsl[1] = hsl[1].coerceIn(0.45f, 0.95f)
    hsl[2] = hsl[2].coerceIn(0.55f, 0.72f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Color.over(base: Color, amount: Float): Color =
    Color(ColorUtils.blendARGB(base.toArgb(), toArgb(), amount))

/**
 * Ctrl Music's only look: Apple Music-style charcoal surfaces with one accent color.
 * With [tintSurfaces] (dynamic color from the song cover), the surfaces pick up a hint of the accent.
 */
fun ColorScheme.ctrlDark(accent: Color, tintSurfaces: Boolean = false): ColorScheme {
    val a = normalizeAccent(accent)
    val t = if (tintSurfaces) 1f else 0f
    val bg = a.over(CtrlBackground, 0.07f * t)
    return copy(
        primary = a,
        onPrimary = if (a.luminance() > 0.45f) Color(0xFF111111) else Color.White,
        primaryContainer = a.over(CtrlBackground, 0.32f),
        onPrimaryContainer = Color.White.over(a, 0.75f),
        secondary = Color.White.over(a, 0.45f),
        onSecondary = Color(0xFF111111),
        secondaryContainer = a.over(Color(0xFF2C2C2E), 0.22f),
        onSecondaryContainer = Color.White.over(a, 0.80f),
        tertiary = a,
        onTertiary = if (a.luminance() > 0.45f) Color(0xFF111111) else Color.White,
        tertiaryContainer = a.over(CtrlBackground, 0.28f),
        onTertiaryContainer = Color.White.over(a, 0.80f),
        background = bg,
        onBackground = Color(0xFFF5F5F7),
        surface = bg,
        onSurface = Color(0xFFF5F5F7),
        surfaceVariant = a.over(Color(0xFF2C2C2E), 0.08f * t),
        onSurfaceVariant = Color(0xFFA1A1A6),
        surfaceDim = a.over(Color(0xFF1A1A1A), 0.06f * t),
        surfaceBright = a.over(Color(0xFF3A3A3C), 0.10f * t),
        surfaceContainerLowest = a.over(Color(0xFF181818), 0.06f * t),
        surfaceContainerLow = a.over(Color(0xFF232323), 0.08f * t),
        surfaceContainer = a.over(Color(0xFF282828), 0.10f * t),
        surfaceContainerHigh = a.over(Color(0xFF2C2C2E), 0.11f * t),
        surfaceContainerHighest = a.over(Color(0xFF363638), 0.12f * t),
        surfaceTint = a,
        outline = Color(0xFF5A5A5E),
        outlineVariant = Color(0xFF3A3A3C),
        inverseSurface = Color(0xFFF5F5F7),
        inverseOnSurface = CtrlBackground,
        inversePrimary = a.over(Color.Black, 0.6f),
    )
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
