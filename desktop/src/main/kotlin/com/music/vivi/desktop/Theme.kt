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

    @Volatile private var cachedSystemAccent: Color? = null

    /**
     * Best-effort "Material You" detection of the OS accent color:
     * Windows DWM accent (registry), macOS accent (defaults), GNOME accent
     * (gsettings). Returns null when the platform accent can't be read, in
     * which case the default accent is used.
     */
    fun systemAccent(): Color? {
        cachedSystemAccent?.let { return it }
        val detected = detectSystemAccent()
        cachedSystemAccent = detected
        return detected
    }

    /** Forget the cached system accent so it is re-detected on next use. */
    fun refreshSystemAccent() {
        cachedSystemAccent = null
    }

    private fun detectSystemAccent(): Color? {
        val os = System.getProperty("os.name", "").lowercase()
        return try {
            when {
                os.contains("win") -> windowsAccent()
                os.contains("mac") -> macAccent()
                os.contains("linux") -> linuxAccent()
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun runCmd(vararg cmd: String): String = try {
        ProcessBuilder(*cmd).redirectErrorStream(true).start().apply {
            waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
        }.inputStream.bufferedReader().readText()
    } catch (_: Exception) {
        ""
    }

    private fun windowsAccent(): Color? {
        val out = runCmd("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\DWM", "/v", "AccentColor")
        val m = Regex("0x([0-9A-Fa-f]{8})").find(out) ?: return null
        val abgr = m.groupValues[1].toLong(16)
        // DWM AccentColor is stored as 0xAABBGGRR.
        val r = (abgr and 0xFF).toInt()
        val g = ((abgr shr 8) and 0xFF).toInt()
        val b = ((abgr shr 16) and 0xFF).toInt()
        return Color(r / 255f, g / 255f, b / 255f)
    }

    private fun macAccent(): Color? {
        // AppleAccentColor: -1 graphite, 0 blue, 1 purple, 2 pink, 3 red,
        // 4 orange, 5 yellow, 6 green, 7 teal.
        val v = runCmd("defaults", "read", "-g", "AppleAccentColor").trim().toIntOrNull() ?: return null
        return when (v) {
            -1 -> Color(0xFF8E8E93)
            0 -> Color(0xFF0A84FF)
            1 -> Color(0xFFBF5AF2)
            2 -> Color(0xFFFF2D55)
            3 -> Color(0xFFFF453A)
            4 -> Color(0xFFFF9F0A)
            5 -> Color(0xFFFFD60A)
            6 -> Color(0xFF30D158)
            7 -> Color(0xFF64D2FF)
            else -> null
        }
    }

    private fun linuxAccent(): Color? {
        val out = runCmd("gsettings", "get", "org.gnome.desktop.interface", "accent-color")
        val rgb = Regex("rgba?\\((\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)").find(out)
        if (rgb != null) {
            val r = rgb.groupValues[1].toIntOrNull() ?: return null
            val g = rgb.groupValues[2].toIntOrNull() ?: return null
            val b = rgb.groupValues[3].toIntOrNull() ?: return null
            return Color(r / 255f, g / 255f, b / 255f)
        }
        val hex = Regex("'([0-9A-Fa-f]{8})'").find(out)
        if (hex != null) {
            val v = hex.groupValues[1].toLong(16)
            val r = ((v shr 24) and 0xFF).toInt()
            val g = ((v shr 16) and 0xFF).toInt()
            val b = ((v shr 8) and 0xFF).toInt()
            return Color(r / 255f, g / 255f, b / 255f)
        }
        return null
    }

    /** Resolve a sentinel (dynamic) swatch to the OS accent (or the default). */
    fun effective(color: Color): Color = if (color == Color.Transparent) (systemAccent() ?: default) else color

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
