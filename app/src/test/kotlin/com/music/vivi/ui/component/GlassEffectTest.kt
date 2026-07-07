package com.music.vivi.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.graphics.Color

class GlassEffectConfigTest {

    @Test
    fun `default config has global enabled false`() {
        val config = GlassEffectConfig()
        assertFalse(config.globalEnabled)
    }

    @Test
    fun `default config has white text color`() {
        val config = GlassEffectConfig()
        assertEquals(Color.White, config.textColor)
    }

    @Test
    fun `default config enables all components`() {
        val config = GlassEffectConfig()
        assertTrue(config.playerEnabled)
        assertTrue(config.miniPlayerEnabled)
        assertTrue(config.navBarEnabled)
    }

    @Test
    fun `default config has chromatic aberration enabled`() {
        val config = GlassEffectConfig()
        assertTrue(config.chromaticAberration)
    }

    @Test
    fun `default config has depth effect enabled`() {
        val config = GlassEffectConfig()
        assertTrue(config.depthEffect)
    }

    @Test
    fun `default config has valid blur radius`() {
        val config = GlassEffectConfig()
        assertTrue(config.blurRadius > 0f)
    }

    @Test
    fun `custom config values are stored correctly`() {
        val config = GlassEffectConfig(
            globalEnabled = true,
            vibrancy = 1.5f,
            blurRadius = 64f,
            chromaticAberration = false,
            depthEffect = false,
            textColor = Color.Black,
        )
        assertTrue(config.globalEnabled)
        assertEquals(1.5f, config.vibrancy, 0.001f)
        assertEquals(64f, config.blurRadius, 0.001f)
        assertFalse(config.chromaticAberration)
        assertFalse(config.depthEffect)
        assertEquals(Color.Black, config.textColor)
    }

    @Test
    fun `surface tint defaults to unspecified meaning theme-adaptive`() {
        val config = GlassEffectConfig()
        assertEquals(Color.Unspecified, config.surfaceTintColor)
    }

    @Test
    fun `defaults follow the Apple liquid glass recipe`() {
        val config = GlassEffectConfig()
        // 8dp blur + 24dp/24dp lens (0.5 * LENS_MAX_DP) + 40% tint, per the
        // library author's Apple-matched LiquidBottomTabs parameters.
        assertEquals(8f, config.blurRadius, 0.001f)
        assertEquals(24f, config.lensHeight * LENS_MAX_DP, 0.001f)
        assertEquals(24f, config.lensAmount * LENS_MAX_DP, 0.001f)
        assertEquals(0.4f, config.surfaceOpacity, 0.001f)
    }

    @Test
    fun `lens config defaults are reasonable`() {
        val config = GlassEffectConfig()
        assertTrue(config.lensHeight in 0f..1f)
        assertTrue(config.lensAmount in 0f..1f)
    }

    @Test
    fun `isEnabledFor returns false for every component when global switch is off`() {
        val config = GlassEffectConfig(
            globalEnabled = false,
            playerEnabled = true,
            miniPlayerEnabled = true,
            navBarEnabled = true,
        )
        GlassComponent.entries.forEach { component ->
            assertFalse(config.isEnabledFor(component))
        }
    }

    @Test
    fun `isEnabledFor respects per-component switches when global switch is on`() {
        val config = GlassEffectConfig(
            globalEnabled = true,
            playerEnabled = true,
            miniPlayerEnabled = false,
            navBarEnabled = true,
        )
        assertTrue(config.isEnabledFor(GlassComponent.PLAYER))
        assertFalse(config.isEnabledFor(GlassComponent.MINI_PLAYER))
        assertTrue(config.isEnabledFor(GlassComponent.NAV_BAR))
    }

    @Test
    fun `glassSaturation maps default vibrancy to the library default`() {
        // vibrancy 1 must match the library's vibrancy() effect (saturation x1.5)
        assertEquals(1.5f, glassSaturation(1f), 0.001f)
    }

    @Test
    fun `glassSaturation maps zero vibrancy to unchanged saturation`() {
        assertEquals(1f, glassSaturation(0f), 0.001f)
    }

    @Test
    fun `glassSaturation clamps out of range vibrancy`() {
        assertEquals(1f, glassSaturation(-5f), 0.001f)
        assertEquals(2f, glassSaturation(99f), 0.001f)
    }

    @Test
    fun `glassResolutionScale keeps clear glass at full resolution`() {
        // No blur means upscaling artifacts would be visible, so no downscaling.
        assertEquals(1f, glassResolutionScale(0f), 0.001f)
    }

    @Test
    fun `glassResolutionScale bottoms out at the minimum for blurred glass`() {
        assertEquals(MIN_GLASS_RESOLUTION_SCALE, glassResolutionScale(FULL_QUALITY_BLUR_DP), 0.001f)
        assertEquals(MIN_GLASS_RESOLUTION_SCALE, glassResolutionScale(100f), 0.001f)
    }

    @Test
    fun `glassResolutionScale interpolates for light blur`() {
        val mid = glassResolutionScale(FULL_QUALITY_BLUR_DP / 2f)
        assertTrue(mid > MIN_GLASS_RESOLUTION_SCALE)
        assertTrue(mid < 1f)
    }

    @Test
    fun `glass is supported from Android 12`() {
        assertFalse(isGlassSupported(sdkInt = 30))
        assertTrue(isGlassSupported(sdkInt = 31))
        assertTrue(isGlassSupported(sdkInt = 34))
    }
}