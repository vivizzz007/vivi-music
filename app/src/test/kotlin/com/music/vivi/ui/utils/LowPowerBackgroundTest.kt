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
        assertTrue(shouldReduceBackgroundEffects(BackgroundEffectsMode.AUTO, isLowRamDevice = true, animatorDurationScale = 1f))
    }

    @Test
    fun autoModeReducesEffectsWhenSystemAnimationsAreDisabled() {
        assertTrue(shouldReduceBackgroundEffects(BackgroundEffectsMode.AUTO, isLowRamDevice = false, animatorDurationScale = 0f))
    }

    @Test
    fun autoModeReducesEffectsWhenLowRamAndAnimationsAreDisabled() {
        assertTrue(shouldReduceBackgroundEffects(BackgroundEffectsMode.AUTO, isLowRamDevice = true, animatorDurationScale = 0f))
    }

    @Test
    fun autoModeKeepsFullEffectsWhenAnimationScaleIsReducedButNotZero() {
        assertFalse(shouldReduceBackgroundEffects(BackgroundEffectsMode.AUTO, isLowRamDevice = false, animatorDurationScale = 0.5f))
    }

    @Test
    fun autoModeKeepsFullEffectsOnCapableDevice() {
        assertFalse(shouldReduceBackgroundEffects(BackgroundEffectsMode.AUTO, isLowRamDevice = false, animatorDurationScale = 1f))
    }

    @Test
    fun reducedModeForcesReducedEffectsOnCapableDevice() {
        assertTrue(shouldReduceBackgroundEffects(BackgroundEffectsMode.REDUCED, isLowRamDevice = false, animatorDurationScale = 1f))
    }

    @Test
    fun reducedModeForcesReducedEffectsWhenAllSignalsAreAbsent() {
        assertTrue(shouldReduceBackgroundEffects(BackgroundEffectsMode.REDUCED, isLowRamDevice = false, animatorDurationScale = 2f))
    }

    @Test
    fun fullModeDisablesReductionOnLowRamDevice() {
        assertFalse(shouldReduceBackgroundEffects(BackgroundEffectsMode.FULL, isLowRamDevice = true, animatorDurationScale = 1f))
    }

    @Test
    fun fullModeDisablesReductionWhenSystemAnimationsAreDisabled() {
        assertFalse(shouldReduceBackgroundEffects(BackgroundEffectsMode.FULL, isLowRamDevice = false, animatorDurationScale = 0f))
    }

    @Test
    fun fullModeDisablesReductionOnLowRamDeviceWithDisabledAnimations() {
        assertFalse(shouldReduceBackgroundEffects(BackgroundEffectsMode.FULL, isLowRamDevice = true, animatorDurationScale = 0f))
    }

    @Test
    fun reductionMatrixMatchesModeAndDeviceSignals() {
        val scales = listOf(0f, 0.5f, 1f)
        val expected = mapOf(
            BackgroundEffectsMode.AUTO to listOf(true, true, true, true, false, false),
            BackgroundEffectsMode.FULL to listOf(false, false, false, false, false, false),
            BackgroundEffectsMode.REDUCED to listOf(true, true, true, true, true, true),
        )
        BackgroundEffectsMode.values().forEach { mode ->
            val actual = listOf(true, false).flatMap { lowRam ->
                scales.map { scale -> shouldReduceBackgroundEffects(mode, lowRam, scale) }
            }
            assertEquals("mode=$mode", expected.getValue(mode), actual)
        }
    }

    @Test
    fun backgroundEffectsModesKeepSettingsDialogOrder() {
        assertEquals(
            listOf(BackgroundEffectsMode.AUTO, BackgroundEffectsMode.FULL, BackgroundEffectsMode.REDUCED),
            BackgroundEffectsMode.values().toList(),
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
    fun defaultStyleIsUnchangedWhenReduced() {
        assertEquals(PlayerBackgroundStyle.DEFAULT, PlayerBackgroundStyle.DEFAULT.withReducedEffects())
    }

    @Test
    fun blurStyleIsUnchangedWhenReduced() {
        assertEquals(PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.BLUR.withReducedEffects())
    }

    @Test
    fun gradientStyleIsUnchangedWhenReduced() {
        assertEquals(PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.GRADIENT.withReducedEffects())
    }

    @Test
    fun appleMusicStyleIsUnchangedWhenReduced() {
        assertEquals(PlayerBackgroundStyle.APPLE_MUSIC, PlayerBackgroundStyle.APPLE_MUSIC.withReducedEffects())
    }

    @Test
    fun everyAnimatedStyleHasAStaticFallbackThatIsNotAnimated() {
        val animated = setOf(PlayerBackgroundStyle.GLOW_ANIMATED, PlayerBackgroundStyle.LIVE_MESH)
        val staticStyles = setOf(
            PlayerBackgroundStyle.DEFAULT,
            PlayerBackgroundStyle.BLUR,
            PlayerBackgroundStyle.GRADIENT,
            PlayerBackgroundStyle.APPLE_MUSIC,
        )
        animated.forEach { style ->
            assertTrue("$style should fall back to a static style", style.withReducedEffects() in staticStyles)
        }
    }

    @Test
    fun lowPowerImageSizeIsNinetySixPixels() {
        assertEquals(96, LOW_POWER_IMAGE_SIZE)
    }

    @Test
    fun backgroundImageIsDownsampledWhenReduced() {
        assertEquals(LOW_POWER_IMAGE_SIZE, backgroundImageSize(reduced = true, fullSize = 128))
    }

    @Test
    fun backgroundImageKeepsFullSizeWhenNotReduced() {
        assertEquals(128, backgroundImageSize(reduced = false, fullSize = 128))
    }

    @Test
    fun backgroundImageAtExactlyTheLowPowerSizeIsUnchangedWhenReduced() {
        assertEquals(LOW_POWER_IMAGE_SIZE, backgroundImageSize(reduced = true, fullSize = LOW_POWER_IMAGE_SIZE))
    }

    @Test
    fun backgroundImageSmallerThanLowPowerSizeIsNotGrownWhenReduced() {
        assertEquals(80, backgroundImageSize(reduced = true, fullSize = 80))
    }

    @Test
    fun backgroundImageOfZeroSizeStaysZeroWhenReduced() {
        assertEquals(0, backgroundImageSize(reduced = true, fullSize = 0))
    }

    @Test
    fun backgroundImageNeverExceedsLowPowerSizeWhenReducedForLargeInput() {
        assertEquals(LOW_POWER_IMAGE_SIZE, backgroundImageSize(reduced = true, fullSize = 1000))
    }
}
