package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Appearance hub: three rows mirroring the Android app's Appearance sub-menu
 * (Theme, App font, Canvas), each opening a dedicated sub-screen.
 */
@Composable
fun AppearanceSection(
    language: String,
    selectedFont: AppFont,
    densityScale: Float,
    screenTransition: String,
    onOpenTheme: () -> Unit,
    onOpenFont: () -> Unit,
    onOpenCanvas: () -> Unit,
    onOpenDensity: () -> Unit,
    onOpenTransitions: () -> Unit,
    onOpenPlayerDesign: () -> Unit = {},
) {
    Text(Localization.get(language, "appearance"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

    AppearanceEntryRow(
        language = language,
        icon = { Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = Localization.get(language, "theme_colors"),
        onClick = onOpenTheme,
    )
    AppearanceEntryRow(
        language = language,
        icon = { Icon(Icons.Filled.FontDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = Localization.get(language, "app_font"),
        subtitle = Localization.get(language, when (selectedFont) {
            AppFont.SYSTEM -> "font_system"
            AppFont.GOOGLE_SANS -> "font_google_sans"
            AppFont.SANS_FLEX -> "font_sans_flex"
            AppFont.OUTFIT -> "font_outfit"
            AppFont.PLUS_JAKARTA_SANS -> "font_plus_jakarta_sans"
        }),
        onClick = onOpenFont,
    )
    AppearanceEntryRow(
        language = language,
        icon = { Icon(Icons.Filled.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = Localization.get(language, "vivimusic_canvas"),
        onClick = onOpenCanvas,
    )
    AppearanceEntryRow(
        language = language,
        icon = { Icon(Icons.Filled.SettingsBrightness, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = Localization.get(language, "density_and_grid"),
        subtitle = densityLabel(densityScale),
        onClick = onOpenDensity,
    )
    AppearanceEntryRow(
        language = language,
        icon = { Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = Localization.get(language, "player_design"),
        onClick = onOpenPlayerDesign,
    )
    AppearanceEntryRow(
        language = language,
        icon = { Icon(Icons.Filled.MotionPhotosOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = Localization.get(language, "screen_transitions"),
        subtitle = Localization.get(language, when (screenTransition) {
            "slide" -> "transition_slide"
            "off" -> "transition_off"
            else -> "transition_fade"
        }),
        onClick = onOpenTransitions,
    )
}

/**
 * Screen transitions sub-screen: Off / Fade / Slide (matches the Android
 * navigation transition options).
 */
@Composable
fun TransitionsScreen(
    language: String,
    screenTransition: String,
    onScreenTransitionChange: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            Localization.get(language, "screen_transitions"),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        listOf("off" to "transition_off", "fade" to "transition_fade", "slide" to "transition_slide").forEach { (value, key) ->
            RadioRow(
                title = Localization.get(language, key),
                desc = "",
                selected = screenTransition == value,
                onClick = { onScreenTransitionChange(value) },
            )
        }
    }
}

/** Human-readable density label (e.g. "110%") for a scale value. */
private fun densityLabel(scale: Float): String = "${(scale * 100).roundToInt()}%"

@Composable
private fun AppearanceEntryRow(
    language: String,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Theme sub-screen: 4 mode circles (System / Light / Dark / Pure black) +
 * the full 21-color accent palette + a live preview card. Mirrors the Android
 * `ThemeScreen` (pixel-perfect mode selection).
 */
@Composable
fun ThemeSection(
    language: String,
    mode: ThemeMode,
    accent: Color,
    onModeChange: (ThemeMode) -> Unit,
    onAccentChange: (Color) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(Localization.get(language, "theme_colors"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

        // Live preview card (uses the currently applied theme).
        ThemePreviewCard(Modifier.fillMaxWidth().padding(top = 16.dp).height(140.dp))

        Spacer(Modifier.height(24.dp))

        Text(Localization.get(language, "theme_mode"), style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemeModeCircle(
                language = language,
                label = Localization.get(language, "theme_system"),
                selected = mode == ThemeMode.SYSTEM && !pureBlack,
                previewDark = false,
                pureBlack = false,
                isAuto = true,
                onClick = { onModeChange(ThemeMode.SYSTEM); onPureBlackChange(false) },
            )
            ThemeModeCircle(
                language = language,
                label = Localization.get(language, "theme_light"),
                selected = mode == ThemeMode.LIGHT && !pureBlack,
                previewDark = false,
                pureBlack = false,
                isAuto = false,
                onClick = { onModeChange(ThemeMode.LIGHT); onPureBlackChange(false) },
            )
            ThemeModeCircle(
                language = language,
                label = Localization.get(language, "theme_dark"),
                selected = mode == ThemeMode.DARK && !pureBlack,
                previewDark = true,
                pureBlack = false,
                isAuto = false,
                onClick = { onModeChange(ThemeMode.DARK); onPureBlackChange(false) },
            )
            ThemeModeCircle(
                language = language,
                label = Localization.get(language, "pure_black"),
                selected = mode == ThemeMode.DARK && pureBlack,
                previewDark = true,
                pureBlack = true,
                isAuto = false,
                onClick = { onModeChange(ThemeMode.DARK); onPureBlackChange(true) },
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(Localization.get(language, "color_palette"), style = MaterialTheme.typography.titleMedium)
        // Palette swatches (wrap via FlowRow-like manual chunking: show in rows of 7).
        AccentPalette.colors.chunked(7).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { entry ->
                    // Re-detect the OS accent when the "Dynamic" (system)
                    // swatch is picked, so Material You picks up changes.
                    val resolved = if (entry.color == Color.Transparent) {
                        AccentPalette.refreshSystemAccent()
                        AccentPalette.systemAccent() ?: AccentPalette.default
                    } else {
                        entry.color
                    }
                    AccentSwatch(
                        color = entry.color,
                        selected = resolved == AccentPalette.effective(accent),
                        onClick = { onAccentChange(resolved) },
                    )
                }
            }
        }

        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun ThemeModeCircle(
    language: String,
    label: String,
    selected: Boolean,
    previewDark: Boolean,
    pureBlack: Boolean,
    isAuto: Boolean,
    onClick: () -> Unit,
) {
    val innerColor = when {
        pureBlack -> Color.Black
        previewDark -> MaterialTheme.colorScheme.surface
        else -> Color.White
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                )
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(18.dp),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(innerColor)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isAuto) {
                    Icon(
                        Icons.Filled.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                } else if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (pureBlack) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun AccentSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                if (color == Color.Transparent) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                } else {
                    Modifier.background(color)
                },
            )
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (color == Color.Transparent) {
            Icon(
                Icons.Filled.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        } else if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ThemePreviewCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .weight(2f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Column(
                    Modifier.weight(1f).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondary),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.tertiary),
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                )
            }
        }
    }
}

/**
 * Font sub-screen: the 5 fonts from the Android app with a live typography
 * preview and radio selection. Bundled `.ttf` fonts are loaded from resources.
 */
@Composable
fun FontSection(
    language: String,
    selectedFont: AppFont,
    onFontChange: (AppFont) -> Unit,
) {
    val activeFamily = AppFonts.familyFor(selectedFont)

    Column(Modifier.fillMaxWidth()) {
        Text(Localization.get(language, "app_font"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    Localization.get(language, "typography_preview").uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    Localization.get(language, "preview_text_quote"),
                    fontFamily = activeFamily,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Text(
                    Localization.get(language, "font_selection"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(Localization.get(language, "font_selection"), style = MaterialTheme.typography.titleMedium)

        FontOption(
            language = language,
            title = Localization.get(language, "font_system"),
            desc = Localization.get(language, "font_system_desc"),
            family = FontFamily.Default,
            selected = selectedFont == AppFont.SYSTEM,
            onClick = { onFontChange(AppFont.SYSTEM) },
        )
        FontOption(
            language = language,
            title = Localization.get(language, "font_google_sans"),
            desc = Localization.get(language, "font_google_sans_desc"),
            family = AppFonts.googleSans,
            selected = selectedFont == AppFont.GOOGLE_SANS,
            onClick = { onFontChange(AppFont.GOOGLE_SANS) },
        )
        FontOption(
            language = language,
            title = Localization.get(language, "font_sans_flex"),
            desc = Localization.get(language, "font_sans_flex_desc"),
            family = AppFonts.sansFlex,
            selected = selectedFont == AppFont.SANS_FLEX,
            onClick = { onFontChange(AppFont.SANS_FLEX) },
        )
        FontOption(
            language = language,
            title = Localization.get(language, "font_outfit"),
            desc = Localization.get(language, "font_outfit_desc"),
            family = AppFonts.outfit,
            selected = selectedFont == AppFont.OUTFIT,
            onClick = { onFontChange(AppFont.OUTFIT) },
        )
        FontOption(
            language = language,
            title = Localization.get(language, "font_plus_jakarta_sans"),
            desc = Localization.get(language, "font_plus_jakarta_sans_desc"),
            family = AppFonts.plusJakartaSans,
            selected = selectedFont == AppFont.PLUS_JAKARTA_SANS,
            onClick = { onFontChange(AppFont.PLUS_JAKARTA_SANS) },
        )

        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun FontOption(
    language: String,
    title: String,
    desc: String,
    family: FontFamily,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, fontFamily = family, style = MaterialTheme.typography.bodyLarge)
            Text(
                desc,
                fontFamily = family,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Canvas sub-screen: enable/disable the animated canvas and choose its source
 * (Auto / Apple Music / ViViMusic / Tidal), matching the Android canvas screen.
 */
@Composable
fun CanvasSection(
    language: String,
    canvasEnabled: Boolean,
    onCanvasEnabledChange: (Boolean) -> Unit,
    canvasSource: CanvasSource,
    onCanvasSourceChange: (CanvasSource) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(Localization.get(language, "vivimusic_canvas"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

        Text(
            Localization.get(language, "vivimusic_canvas_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )

        // Main toggle as a capsule banner (like the Android app).
        Surface(
            onClick = { onCanvasEnabledChange(!canvasEnabled) },
            shape = RoundedCornerShape(50),
            color = if (canvasEnabled) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    Localization.get(language, "use_canvas"),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (canvasEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(checked = canvasEnabled, onCheckedChange = onCanvasEnabledChange)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(Localization.get(language, "canvas_source"), style = MaterialTheme.typography.titleMedium)

        CanvasSourceOption(
            language = language,
            title = Localization.get(language, "canvas_source_auto"),
            desc = Localization.get(language, "canvas_source_auto_desc"),
            selected = canvasSource == CanvasSource.AUTO,
            enabled = canvasEnabled,
            onClick = { onCanvasSourceChange(CanvasSource.AUTO) },
        )
        CanvasSourceOption(
            language = language,
            title = Localization.get(language, "canvas_source_apple_music"),
            desc = Localization.get(language, "canvas_source_apple_music_desc"),
            selected = canvasSource == CanvasSource.APPLE_MUSIC,
            enabled = canvasEnabled,
            onClick = { onCanvasSourceChange(CanvasSource.APPLE_MUSIC) },
        )
        CanvasSourceOption(
            language = language,
            title = Localization.get(language, "canvas_source_vivimusic"),
            desc = Localization.get(language, "canvas_source_vivimusic_desc"),
            selected = canvasSource == CanvasSource.VIVIMUSIC,
            enabled = canvasEnabled,
            onClick = { onCanvasSourceChange(CanvasSource.VIVIMUSIC) },
        )
        CanvasSourceOption(
            language = language,
            title = Localization.get(language, "canvas_source_tidal"),
            desc = Localization.get(language, "canvas_source_tidal_desc"),
            selected = canvasSource == CanvasSource.TIDAL,
            enabled = canvasEnabled,
            onClick = { onCanvasSourceChange(CanvasSource.TIDAL) },
        )

        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun CanvasSourceOption(
    language: String,
    title: String,
    desc: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Density scale presets (fractional; 1f = 100%). */
private val DENSITY_PRESETS = listOf(
    2.0f, 1.8f, 1.5f, 1.4f, 1.3f, 1.25f, 1.2f, 1.1f,
    1f, 0.85f, 0.75f, 0.65f, 0.55f,
)

/** Grid cell width presets in dp (small / medium / large). */
private val GRID_PRESETS = listOf(140 to "grid_small", 160 to "grid_medium", 200 to "grid_large", 240 to "grid_xlarge")

/**
 * Density & grid sub-screen: UI density scale (200% down to 55%) and the
 * adaptive grid cell size used by album/artist/playlist grids.
 */
@Composable
fun DensityScreen(
    language: String,
    densityScale: Float,
    onDensityScaleChange: (Float) -> Unit,
    gridItemSize: Int,
    onGridItemSizeChange: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            Localization.get(language, "density_and_grid"),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        Text(
            Localization.get(language, "density_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        DENSITY_PRESETS.forEach { scale ->
            RadioRow(
                title = densityLabel(scale),
                desc = "",
                selected = kotlin.math.abs(scale - densityScale) < 0.001f,
                onClick = { onDensityScaleChange(scale) },
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            Localization.get(language, "grid_item_size"),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            Localization.get(language, "grid_item_size_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        GRID_PRESETS.forEach { (size, key) ->
            RadioRow(
                title = Localization.get(language, key),
                desc = "",
                selected = gridItemSize == size,
                onClick = { onGridItemSizeChange(size) },
            )
        }
    }
}

@Composable
private fun RadioRow(
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (desc.isNotEmpty()) {
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Player design sub-screen: full-player layout variant (classic / new / v2 /
 * expressive), background style (canvas / gradient / blur / glow / apple
 * music / live mesh), rotating artwork and the Apple mini-player variant.
 */
@Composable
fun PlayerDesignScreen(
    language: String,
    design: PlayerDesign,
    onDesignChange: (PlayerDesign) -> Unit,
    background: PlayerBackgroundStyle,
    onBackgroundChange: (PlayerBackgroundStyle) -> Unit,
    rotatingThumbnail: Boolean,
    onRotatingThumbnailChange: (Boolean) -> Unit,
    miniPlayerStyle: String,
    onMiniPlayerStyleChange: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            Localization.get(language, "player_design"),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        Text(
            Localization.get(language, "player_design_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        listOf(
            PlayerDesign.CLASSIC to "player_design_classic",
            PlayerDesign.NEW to "player_design_new",
            PlayerDesign.V2 to "player_design_v2",
            PlayerDesign.EXPRESSIVE to "player_design_expressive",
        ).forEach { (value, key) ->
            RadioRow(
                title = Localization.get(language, key),
                desc = "",
                selected = design == value,
                onClick = { onDesignChange(value) },
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            Localization.get(language, "player_background"),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            Localization.get(language, "player_background_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        listOf(
            PlayerBackgroundStyle.CANVAS to "canvas",
            PlayerBackgroundStyle.GRADIENT to "player_background_gradient",
            PlayerBackgroundStyle.BLUR to "player_background_blur",
            PlayerBackgroundStyle.GLOW to "player_background_glow",
            PlayerBackgroundStyle.APPLE_MUSIC to "player_background_apple",
            PlayerBackgroundStyle.LIVE_MESH to "player_background_mesh",
        ).forEach { (value, key) ->
            RadioRow(
                title = Localization.get(language, key),
                desc = "",
                selected = background == value,
                onClick = { onBackgroundChange(value) },
            )
        }
        Spacer(Modifier.height(16.dp))
        SwitchRow(
            title = Localization.get(language, "rotating_thumbnail"),
            desc = Localization.get(language, "rotating_thumbnail_desc"),
            checked = rotatingThumbnail,
            onCheckedChange = onRotatingThumbnailChange,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            Localization.get(language, "mini_player"),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            Localization.get(language, "mini_player_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        listOf(
            MiniPlayerStyle.STANDARD to "mini_player_standard",
            MiniPlayerStyle.APPLE to "mini_player_apple",
            MiniPlayerStyle.OUTLINE to "mini_player_outline",
            MiniPlayerStyle.PURE_BLACK to "mini_player_pure_black",
        ).forEach { (value, key) ->
            RadioRow(
                title = Localization.get(language, key),
                desc = "",
                selected = MiniPlayerStyle.from(miniPlayerStyle) == value,
                onClick = { onMiniPlayerStyleChange(value.key) },
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
