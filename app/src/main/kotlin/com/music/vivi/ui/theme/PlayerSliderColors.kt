/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.music.vivi.constants.PlayerBackgroundStyle

/**
 * Player slider color configuration for consistent styling across all slider types
 * 
 * This object provides standardized color schemes for Default, Squiggly, and Slim sliders
 * used in the music player interface, ensuring visual consistency and proper contrast.
 */
object PlayerSliderColors {

    // Accord overlay slider colors
    val ActiveOverlayColor = Color(0xBFFFFFFF)      // contrast_primaryOverlayColor (75% white)
    val ActiveShadeColor = Color(0x73FFFFFF)        // contrast_primaryOverlayShadeColor (45% white)
    val InactiveOverlayColor = Color(0x26FFFFFF)    // contrast_secondaryOverlayColor (15% white)
    val InactiveShadeColor = Color(0x33FFFFFF)      // contrast_secondaryOverlayShadeColor (20% white)

    /**
     * Standard slider colors for all slider types
     * 
     * @param activeColor Color for active track, ticks, and thumb
     * @param playerBackground The player background style
     * @param useDarkTheme Whether dark theme is being used
     * @return SliderColors configuration
     */
    @Composable
    fun getSliderColors(
        activeColor: Color,
        playerBackground: PlayerBackgroundStyle,
        useDarkTheme: Boolean
    ): SliderColors {
        val effectiveActiveColor = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> activeColor
            PlayerBackgroundStyle.BLUR,
            PlayerBackgroundStyle.GRADIENT,
            PlayerBackgroundStyle.GLOW_ANIMATED,
            PlayerBackgroundStyle.APPLE_MUSIC,
            PlayerBackgroundStyle.LIVE_MESH -> {
                ActiveShadeColor
            }
        }

        val inactiveTrackColor = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> {
                if (useDarkTheme) {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
            }
            PlayerBackgroundStyle.BLUR,
            PlayerBackgroundStyle.GRADIENT,
            PlayerBackgroundStyle.GLOW_ANIMATED,
            PlayerBackgroundStyle.APPLE_MUSIC,
            PlayerBackgroundStyle.LIVE_MESH -> {
                InactiveShadeColor
            }
        }
        
        return SliderDefaults.colors(
            activeTrackColor = effectiveActiveColor,
            activeTickColor = effectiveActiveColor,
            thumbColor = effectiveActiveColor,
            inactiveTrackColor = inactiveTrackColor,
            disabledActiveTrackColor = effectiveActiveColor,
            disabledInactiveTrackColor = inactiveTrackColor,
            disabledThumbColor = effectiveActiveColor
        )
    }
}
