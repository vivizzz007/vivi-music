package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Light / dark / follow-system theme mode, persisted in [DesktopSettings]. */
enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun from(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

data class AccentColor(val name: String, val color: Color)

/**
 * Accent color palette mirroring the Android app's theme colors. The first
 * entry is the "dynamic/system" sentinel (transparent) which selects the
 * default accent color.
 */
object AccentPalette {
    val default: Color = Color(0xFFED5564)

    /** Resolve a sentinel (dynamic) swatch to the default accent color. */
    fun effective(color: Color): Color = if (color == Color.Transparent) default else color

    val colors: List<AccentColor> = listOf(
        AccentColor("Dynamic", Color.Transparent),
        AccentColor("Crimson", Color(0xFFEC5464)),
        AccentColor("Rose", Color(0xFFD81B60)),
        AccentColor("Purple", Color(0xFF8E24AA)),
        AccentColor("Monochrome", Color(0xFF000000)),
        AccentColor("Deep Purple", Color(0xFF5E35B1)),
        AccentColor("Indigo", Color(0xFF3949AB)),
        AccentColor("Blue", Color(0xFF1E88E5)),
        AccentColor("Sky Blue", Color(0xFF039BE5)),
        AccentColor("Cyan", Color(0xFF00ACC1)),
        AccentColor("Teal", Color(0xFF00897B)),
        AccentColor("Green", Color(0xFF43A047)),
        AccentColor("Light Green", Color(0xFF7CB342)),
        AccentColor("Lime", Color(0xFFC0CA33)),
        AccentColor("Yellow", Color(0xFFFDD835)),
        AccentColor("Amber", Color(0xFFFFB300)),
        AccentColor("Orange", Color(0xFFFB8C00)),
        AccentColor("Deep Orange", Color(0xFFF4511E)),
        AccentColor("Brown", Color(0xFF6D4C41)),
        AccentColor("Grey", Color(0xFF757575)),
        AccentColor("Blue Grey", Color(0xFF546E7A)),
    )
}

/** Packs a [Color] into an opaque ARGB [Int] (cross-platform, no Android API). */
fun colorToArgbInt(color: Color): Int {
    val a = (color.alpha * 255 + 0.5f).toInt()
    val r = (color.red * 255 + 0.5f).toInt()
    val g = (color.green * 255 + 0.5f).toInt()
    val b = (color.blue * 255 + 0.5f).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

/** Reconstructs a [Color] from an ARGB [Int] (cross-platform, no Android API). */
fun argbIntToColor(argb: Int): Color = Color(
    red = ((argb shr 16) and 0xFF) / 255f,
    green = ((argb shr 8) and 0xFF) / 255f,
    blue = (argb and 0xFF) / 255f,
    alpha = ((argb ushr 24) and 0xFF) / 255f,
)

/**
 * Applies the selected light/dark mode (resolving "system" against the OS)
 * and accent color to the whole app via [MaterialTheme].
 */
@Composable
fun AppTheme(
    mode: ThemeMode,
    accent: Color,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val useDark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // Same seed-based tonal palette as the Android app (TonalSpot + SPEC_2025),
    // so the desktop colors match the mobile app pixel-perfectly.
    val colorScheme = rememberDynamicColorScheme(
        seedColor = AccentPalette.effective(accent),
        isDark = useDark,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = PaletteStyle.TonalSpot,
    )
    // "Pure black" replaces the tonal dark surfaces with a true black background.
    val effective = if (pureBlack && useDark) {
        colorScheme.copy(background = Color.Black, surface = Color.Black)
    } else {
        colorScheme
    }
    MaterialTheme(colorScheme = effective) {
        // Material3's MaterialTheme does NOT set LocalContentColor, so any Text
        // without an explicit color would fall back to the default (black) and
        // never adapt to the theme. Provide it explicitly so text follows the
        // onBackground color, then paint the whole window with the theme
        // background (otherwise the native light window shows through in dark
        // mode).
        CompositionLocalProvider(LocalContentColor provides effective.onBackground) {
            Box(Modifier.fillMaxSize().background(effective.background)) {
                content()
            }
        }
    }
}
