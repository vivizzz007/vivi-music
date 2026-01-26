// package com.music.vivi.update.shapes
//
// import androidx.graphics.shapes.RoundedPolygon
// import androidx.graphics.shapes.CornerRounding
// import androidx.graphics.shapes.star
// import androidx.compose.ui.graphics.Shape
// import androidx.graphics.shapes.toPath
// import androidx.compose.ui.geometry.Size
// import androidx.compose.ui.graphics.Outline
// import androidx.compose.ui.graphics.asAndroidPath
// import androidx.compose.ui.graphics.asComposePath
// import androidx.compose.ui.unit.Density
// import androidx.compose.ui.unit.LayoutDirection
//
//
// // Create a custom shape class for the star
// class MorphPolygonShape(private val polygon: RoundedPolygon) : Shape {
//    override fun createOutline(
//        size: Size,
//        layoutDirection: LayoutDirection,
//        density: Density
//    ): Outline {
//        val path = polygon.toPath().asComposePath()
//        val matrix = android.graphics.Matrix()
//        // Scale to fill the size - increased multiplier for bigger shape
//        val scale = minOf(size.width, size.height) / 1.8f  // Changed from 2f to 1.8f for bigger size
//        matrix.setScale(scale, scale)
//        // Translate to center (RoundedPolygon is centered at 0,0)
//        matrix.postTranslate(size.width / 2f, size.height / 2f)
//        path.asAndroidPath().transform(matrix)
//        return Outline.Generic(path)
//    }
// }
//
// // Create the star shape - bigger and more prominent
// val starShape = RoundedPolygon.star(
//    numVerticesPerRadius = 4,
//    innerRadius = 0.45f,  // Increased from 0.352f for less pronounced points
//    rounding = CornerRounding(0.25f),  // Reduced from 0.32f for sharper edges
//    innerRounding = CornerRounding(0.25f)  // Reduced from 0.32f
// )
//
//
//
//
