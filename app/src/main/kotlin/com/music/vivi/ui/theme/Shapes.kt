package com.music.vivi.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Expressive shapes for the application, featuring more rounded and distinct corners.
 * used when the user selects the "Expressive" theme option.
 */
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp), // Tags, small chips
    small = RoundedCornerShape(16.dp), // Cards, dialogs
    medium = RoundedCornerShape(24.dp), // Bottom sheets, large cards
    large = RoundedCornerShape(32.dp), // FABs, Large containers
    extraLarge = RoundedCornerShape(48.dp) // Full screen sheets/panels
)

/**
 * Default shapes for the application, following standard Material 3 guidelines.
 */
val DefaultShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
