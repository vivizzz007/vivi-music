package com.music.vivi.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Expressive shapes for the application, featuring bold, large rounded corners
 * typical of modern "Material 3 Expressive" design.
 */
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp),
    small = RoundedCornerShape(24.dp), // Cards, Dialogs
    medium = RoundedCornerShape(32.dp), // Bottom Sheets
    large = RoundedCornerShape(48.dp), // FABs, Large Containers
    extraLarge = RoundedCornerShape(64.dp) // Full screen sheets
)

/**
 * Default shapes for the application.
 */
val DefaultShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val RoundShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val SquareShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

val CutShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.CutCornerShape(4.dp),
    small = androidx.compose.foundation.shape.CutCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.CutCornerShape(12.dp),
    large = androidx.compose.foundation.shape.CutCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.CutCornerShape(24.dp)
)

enum class ShapeStyle {
    Default,
    Round,
    Square,
    Cut,
    Expressive
}

fun makeShapes(style: ShapeStyle): Shapes {
    return when (style) {
        ShapeStyle.Default -> DefaultShapes
        ShapeStyle.Round -> RoundShapes
        ShapeStyle.Square -> SquareShapes
        ShapeStyle.Cut -> CutShapes
        ShapeStyle.Expressive -> ExpressiveShapes
    }
}
