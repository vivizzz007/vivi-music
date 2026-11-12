/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.theme

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
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

import androidx.compose.runtime.getValue
import com.music.vivi.constants.SelectedFontKey
import com.music.vivi.constants.CustomFontPathKey
import com.music.vivi.constants.AppFont
import com.music.vivi.utils.rememberPreference
import androidx.compose.ui.text.font.FontFamily
import android.graphics.Typeface as AndroidTypeface
import androidx.compose.ui.text.font.Typeface as ComposeTypeface

// Ctrl Music brand colors
val CtrlAccent = Color(0xFFB5928D)
val CtrlBackground = Color(0xFF1F1F1F)
val DefaultThemeColor = CtrlAccent

@Composable
fun vivimusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
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
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = if (themeColor.toArgb() == 0xFF000000.toInt()) PaletteStyle.Monochrome else PaletteStyle.TonalSpot
        )
    }

    // Apply pureBlack modification if needed, similar to original logic
    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else if (darkTheme && themeColor == DefaultThemeColor) {
            baseColorScheme.ctrlDark()
        } else {
            baseColorScheme
        }
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

// Apple Music-style dark look in Ctrl Music colors: charcoal surfaces, rose-gold accent.
fun ColorScheme.ctrlDark() = copy(
    primary = CtrlAccent,
    onPrimary = CtrlBackground,
    primaryContainer = Color(0xFF4A3A37),
    onPrimaryContainer = Color(0xFFF3DDD8),
    secondary = Color(0xFFD9BDB7),
    onSecondary = CtrlBackground,
    secondaryContainer = Color(0xFF3A3130),
    onSecondaryContainer = Color(0xFFF3DDD8),
    tertiary = CtrlAccent,
    background = CtrlBackground,
    onBackground = Color(0xFFF5F5F5),
    surface = CtrlBackground,
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFA1A1A6),
    surfaceDim = Color(0xFF1A1A1A),
    surfaceBright = Color(0xFF3A3A3C),
    surfaceContainerLowest = Color(0xFF181818),
    surfaceContainerLow = Color(0xFF232323),
    surfaceContainer = Color(0xFF282828),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF363638),
    outline = Color(0xFF5A5A5E),
    outlineVariant = Color(0xFF3A3A3C),
    inverseSurface = Color(0xFFF5F5F5),
    inverseOnSurface = CtrlBackground,
    inversePrimary = Color(0xFF7A5E59),
    surfaceTint = CtrlAccent,
)

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
