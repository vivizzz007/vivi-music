// package com.music.vivi.update.shapes
//
// import androidx.compose.foundation.shape.CircleShape
// import androidx.compose.runtime.Composable
// import androidx.compose.ui.geometry.Size
// import androidx.compose.ui.graphics.Outline
// import androidx.compose.ui.graphics.Path
// import androidx.compose.ui.graphics.Shape
// import androidx.compose.ui.unit.Density
// import androidx.compose.ui.unit.Dp
// import androidx.compose.ui.unit.LayoutDirection
// import androidx.compose.ui.unit.dp
// import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
// import kotlin.math.PI
// import kotlin.math.cos
// import kotlin.math.min
// import kotlin.math.sin
//
// class RoundedStarShape(
//    private val sides: Int,
//    private val curve: Double = 0.09,
//    private val rotation: Float = 0f,
//    iterations: Int = 360,
//
//    ) : Shape {
//
//    private companion object {
//        const val TWO_PI = 2 * PI
//    }
//
//    private val steps = (TWO_PI) / min(iterations, 360)
//    private val rotationDegree = (PI / 180) * rotation
//
//    override fun createOutline(
//        size: Size,
//        layoutDirection: LayoutDirection,
//        density: Density
//    ): Outline = Outline.Generic(Path().apply {
//
//
//        val r = min(size.height, size.width) * 0.4 * mapRange(1.0, 0.0, 0.5, 1.0, curve)
//
//        val xCenter = size.width * .5f
//        val yCenter = size.height * .5f
//
//        moveTo(xCenter, yCenter)
//
//        var t = 0.0
//
//        while (t <= TWO_PI) {
//            val x = r * (cos(t - rotationDegree) * (1 + curve * cos(sides * t)))
//            val y = r * (sin(t - rotationDegree) * (1 + curve * cos(sides * t)))
//            lineTo((x + xCenter).toFloat(), (y + yCenter).toFloat())
//
//            t += steps
//        }
//
//        val x = r * (cos(t - rotationDegree) * (1 + curve * cos(sides * t)))
//        val y = r * (sin(t - rotationDegree) * (1 + curve * cos(sides * t)))
//        lineTo((x + xCenter).toFloat(), (y + yCenter).toFloat())
//
//    })
//
//
//    private fun mapRange(a: Double, b: Double, c: Double, d: Double, x: Double): Double {
//        return (x - a) / (b - a) * (d - c) + c
//    }
// }
//
// @Composable
// fun threeShapeSwitch(index: Int, thirdShapeCornerRadius: Dp = 16.dp): Shape { // Ensure the function returns a Shape
//    return when (index) { // Return the result of the when expression
//        0 -> RoundedStarShape(
//            sides = 6,
//            rotation = 10f
//        )
//        1 -> CircleShape
//        2 -> AbsoluteSmoothCornerShape(
//            cornerRadiusBL = thirdShapeCornerRadius,
//            cornerRadiusTR = thirdShapeCornerRadius,
//            smoothnessAsPercentBL = 60,
//            smoothnessAsPercentTR = 60,
//            cornerRadiusTL = thirdShapeCornerRadius,
//            cornerRadiusBR = thirdShapeCornerRadius,
//            smoothnessAsPercentTL = 60,
//            smoothnessAsPercentBR = 60
//        )
//        else -> CircleShape // It's good practice to have a default case
//    }
// }
