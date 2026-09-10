/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * True-black AMOLED color scheme for Wear OS.
 * Optimized for Samsung Galaxy Watch 7 AMOLED panel battery efficiency.
 */
private val ViviWearColorScheme = ColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3700B3),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFF70F5E8),
    tertiary = Color(0xFFCF6679),
    onTertiary = Color.Black,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainer = Color(0xFF1C1B1F),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerLow = Color(0xFF121212),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    background = Color.Black,
    onBackground = Color.White,
    error = Color(0xFFCF6679),
    onError = Color.Black,
)

@Composable
fun ViviWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ViviWearColorScheme,
        content = content,
    )
}
