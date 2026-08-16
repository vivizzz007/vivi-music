package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

/** Accent color palette mirroring the Android app's theme colors. */
object AccentPalette {
    val default: Color = Color(0xFFED5564)

    val colors: List<AccentColor> = listOf(
        AccentColor("Crimson", Color(0xFFED5564)),
        AccentColor("Rose", Color(0xFFD81B60)),
        AccentColor("Purple", Color(0xFF8E24AA)),
        AccentColor("Indigo", Color(0xFF3949AB)),
        AccentColor("Blue", Color(0xFF1E88E5)),
        AccentColor("Teal", Color(0xFF00897B)),
        AccentColor("Green", Color(0xFF43A047)),
        AccentColor("Amber", Color(0xFFFFB300)),
        AccentColor("Orange", Color(0xFFFB8C00)),
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
        seedColor = accent,
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

/** A clickable circle showing an accent color, highlighted when selected. */
@Composable
private fun AccentSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}

/**
 * Appearance section: theme mode (System / Light / Dark) and the accent color
 * palette. Rendered inside Settings.
 */
@Composable
fun AppearanceSection(
    language: String,
    mode: ThemeMode,
    accent: Color,
    onModeChange: (ThemeMode) -> Unit,
    onAccentChange: (Color) -> Unit,
    pureBlack: Boolean = false,
    onPureBlackChange: (Boolean) -> Unit = {},
) {
    Text(Localization.get(language, "appearance"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

    // Theme mode
    Text(Localization.get(language, "theme_mode"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeMode.entries.forEach { entry ->
            val selected = mode == entry
            val label = when (entry) {
                ThemeMode.SYSTEM -> Localization.get(language, "theme_system")
                ThemeMode.LIGHT -> Localization.get(language, "theme_light")
                ThemeMode.DARK -> Localization.get(language, "theme_dark")
            }
            Box(
                Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onModeChange(entry) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // Accent color palette
    Text(Localization.get(language, "accent_color"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccentPalette.colors.forEach { entry ->
            AccentSwatch(
                color = entry.color,
                selected = entry.color == accent,
                onClick = { onAccentChange(entry.color) },
            )
        }
    }

    // Pure black background in dark mode.
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.material3.Switch(checked = pureBlack, onCheckedChange = onPureBlackChange)
        Text(Localization.get(language, "pure_black"))
    }
}
