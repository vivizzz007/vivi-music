package com.music.vivi.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveMeshConfigTest {
    @Test
    fun defaultConfigMatchesOriginalPlayerAppearance() {
        val default = LiveMeshConfig.Default

        assertEquals(1f, default.blurStrength, 0f)
        assertEquals(1.8f, default.saturation, 0f)
        assertEquals(1f, default.speed, 0f)
        assertEquals(0.2f, default.darkTint, 0f)
    }

    @Test
    fun defaultConfigIsAlreadyWithinBounds() {
        assertEquals(LiveMeshConfig.Default, LiveMeshConfig.Default.clamped())
    }

    @Test
    fun valuesBelowMinimumAreRaisedToMinimum() {
        val clamped = LiveMeshConfig(blurStrength = 0f, saturation = 0f, speed = 0f, darkTint = -1f).clamped()

        assertEquals(LiveMeshConfig.BLUR_STRENGTH_MIN, clamped.blurStrength, 0f)
        assertEquals(LiveMeshConfig.SATURATION_MIN, clamped.saturation, 0f)
        assertEquals(LiveMeshConfig.SPEED_MIN, clamped.speed, 0f)
        assertEquals(LiveMeshConfig.DARK_TINT_MIN, clamped.darkTint, 0f)
    }

    @Test
    fun valuesAboveMaximumAreLoweredToMaximum() {
        val clamped = LiveMeshConfig(blurStrength = 10f, saturation = 10f, speed = 10f, darkTint = 1f).clamped()

        assertEquals(LiveMeshConfig.BLUR_STRENGTH_MAX, clamped.blurStrength, 0f)
        assertEquals(LiveMeshConfig.SATURATION_MAX, clamped.saturation, 0f)
        assertEquals(LiveMeshConfig.SPEED_MAX, clamped.speed, 0f)
        assertEquals(LiveMeshConfig.DARK_TINT_MAX, clamped.darkTint, 0f)
    }

    @Test
    fun valuesInsideBoundsAreKept() {
        val config = LiveMeshConfig(blurStrength = 1.5f, saturation = 2f, speed = 0.5f, darkTint = 0.4f)

        assertEquals(config, config.clamped())
    }

    @Test
    fun unitSpeedKeepsOriginalPeriod() {
        assertEquals(80000, liveMeshPeriodMillis(80000, speed = 1f))
    }

    @Test
    fun doublingSpeedHalvesThePeriod() {
        assertEquals(30000, liveMeshPeriodMillis(60000, speed = 2f))
    }

    @Test
    fun slowestSpeedQuadruplesThePeriod() {
        assertEquals(240000, liveMeshPeriodMillis(60000, speed = LiveMeshConfig.SPEED_MIN))
    }

    @Test
    fun zeroSpeedIsTreatedAsSlowestInsteadOfDividingByZero() {
        assertEquals(240000, liveMeshPeriodMillis(60000, speed = 0f))
    }

    @Test
    fun periodIsRoundedToWholeMilliseconds() {
        assertEquals(13333, liveMeshPeriodMillis(40000, speed = 3f))
    }
}
