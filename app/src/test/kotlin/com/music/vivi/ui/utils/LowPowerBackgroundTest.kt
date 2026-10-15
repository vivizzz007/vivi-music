package com.music.vivi.ui.utils

import com.music.vivi.constants.BackgroundEffectsMode
import com.music.vivi.constants.PlayerBackgroundStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LowPowerBackgroundTest {
    @Test
    fun autoModeReducesEffectsOnLowRamDevice() {
        assertTrue(
            shouldReduceBackgroundEffects(BackgroundEffectsMode.AUTO, isLowRamDevice = true, animatorDurationScale = 1f)
        )
    }

    @Test
    fun autoModeReducesEffectsWhenSystemAnimationsAreDisabled() {
        assertTrue(
            shouldReduceBackgroundEffects(BackgroundEffectsMode.AUTO, isLowRamDevice = false, animatorDurationScale = 0f)
        )
    }

    @Test
    fun autoModeKeepsFullEffectsOnCapableDevice() {
        assertFalse(
            shouldReduceBackgroundEffects(BackgroundEffectsMode.AUTO, isLowRamDevice = false, animatorDurationScale = 1f)
        )
        assertFalse(
            shouldReduceBackgroundEffects(BackgroundEffectsMode.AUTO, isLowRamDevice = false, animatorDurationScale = 0.5f)
        )
    }

    @Test
    fun reducedModeForcesReducedEffectsOnCapableDevice() {
        assertTrue(
            shouldReduceBackgroundEffects(BackgroundEffectsMode.REDUCED, isLowRamDevice = false, animatorDurationScale = 1f)
        )
    }

    @Test
    fun fullModeDisablesReductionEvenOnLowRamDeviceWithDisabledAnimations() {
        assertFalse(
            shouldReduceBackgroundEffects(BackgroundEffectsMode.FULL, isLowRamDevice = true, animatorDurationScale = 0f)
        )
    }

    @Test
    fun liveMeshFallsBackToStaticBlur() {
        assertEquals(PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.LIVE_MESH.withReducedEffects())
    }

    @Test
    fun glowAnimatedFallsBackToStaticGradient() {
        assertEquals(PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.GLOW_ANIMATED.withReducedEffects())
    }

    @Test
    fun staticStylesAreUnchangedWhenReduced() {
        listOf(
            PlayerBackgroundStyle.DEFAULT,
            PlayerBackgroundStyle.BLUR,
            PlayerBackgroundStyle.GRADIENT,
            PlayerBackgroundStyle.APPLE_MUSIC,
        ).forEach { style ->
            assertEquals(style, style.withReducedEffects())
        }
    }

    @Test
    fun backgroundImageIsDownsampledOnlyWhenReduced() {
        assertEquals(LOW_POWER_IMAGE_SIZE, backgroundImageSize(reduced = true, fullSize = 128))
        assertEquals(128, backgroundImageSize(reduced = false, fullSize = 128))
    }

    @Test
    fun backgroundImageNeverGrowsWhenReduced() {
        assertEquals(80, backgroundImageSize(reduced = true, fullSize = 80))
        assertEquals(LOW_POWER_IMAGE_SIZE, backgroundImageSize(reduced = true, fullSize = 1000))
    }
}
