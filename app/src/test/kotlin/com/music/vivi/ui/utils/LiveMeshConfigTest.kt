package com.music.vivi.ui.utils

import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveMeshConfigTest {
    private fun config(
        blur: Float = 1f,
        saturation: Float = 1.8f,
        speed: Float = 1f,
        darkTint: Float = 0.2f,
        renderScale: Float = 4f,
        layer2Alpha: Float = 0.6f,
        layer3Alpha: Float = 0.5f,
        zoom: Float = 1.7f,
        reverse: Boolean = false,
        bottomShade: Float = 0.25f,
        textureSize: Int = 128,
        beat: Float = 0.5f,
        lyric: Float = 0.5f,
    ) = LiveMeshConfig(
        blurStrength = blur,
        saturation = saturation,
        speed = speed,
        darkTint = darkTint,
        renderScale = renderScale,
        layer2Alpha = layer2Alpha,
        layer3Alpha = layer3Alpha,
        zoom = zoom,
        reverseDirection = reverse,
        bottomShade = bottomShade,
        textureSize = textureSize,
        beatReactivity = beat,
        lyricReactivity = lyric,
    )

    @Test
    fun defaultConfigMatchesOriginalPlayerAppearance() {
        assertEquals(config(), LiveMeshConfig.Default)
    }

    @Test
    fun miniDefaultKeepsOriginalMiniAppearance() {
        val mini = LiveMeshConfig.MiniDefault

        assertEquals(1.6f, mini.saturation, 0f)
        assertEquals(0.3f, mini.darkTint, 0f)
        assertEquals(1.5f, mini.zoom, 0f)
    }

    @Test
    fun defaultConfigIsAlreadyWithinBounds() {
        assertEquals(LiveMeshConfig.Default, LiveMeshConfig.Default.clamped())
        assertEquals(LiveMeshConfig.MiniDefault, LiveMeshConfig.MiniDefault.clamped())
    }

    @Test
    fun valuesBelowMinimumAreRaisedToMinimum() {
        val clamped = config(
            blur = 0f,
            saturation = 0f,
            speed = 0f,
            darkTint = -1f,
            renderScale = 0f,
            layer2Alpha = -1f,
            layer3Alpha = -1f,
            zoom = 0f,
            bottomShade = -1f,
            textureSize = 0,
        ).clamped()

        assertEquals(LiveMeshConfig.BLUR_STRENGTH_MIN, clamped.blurStrength, 0f)
        assertEquals(LiveMeshConfig.SATURATION_MIN, clamped.saturation, 0f)
        assertEquals(LiveMeshConfig.SPEED_MIN, clamped.speed, 0f)
        assertEquals(LiveMeshConfig.DARK_TINT_MIN, clamped.darkTint, 0f)
        assertEquals(LiveMeshConfig.RENDER_SCALE_MIN, clamped.renderScale, 0f)
        assertEquals(LiveMeshConfig.LAYER_ALPHA_MIN, clamped.layer2Alpha, 0f)
        assertEquals(LiveMeshConfig.LAYER_ALPHA_MIN, clamped.layer3Alpha, 0f)
        assertEquals(LiveMeshConfig.ZOOM_MIN, clamped.zoom, 0f)
        assertEquals(LiveMeshConfig.BOTTOM_SHADE_MIN, clamped.bottomShade, 0f)
        assertEquals(LiveMeshConfig.TEXTURE_SIZE_MIN, clamped.textureSize)
    }

    @Test
    fun valuesAboveMaximumAreLoweredToMaximum() {
        val clamped = config(
            blur = 10f,
            saturation = 10f,
            speed = 10f,
            darkTint = 1f,
            renderScale = 100f,
            layer2Alpha = 2f,
            layer3Alpha = 2f,
            zoom = 5f,
            bottomShade = 2f,
            textureSize = 4096,
        ).clamped()

        assertEquals(LiveMeshConfig.BLUR_STRENGTH_MAX, clamped.blurStrength, 0f)
        assertEquals(LiveMeshConfig.SATURATION_MAX, clamped.saturation, 0f)
        assertEquals(LiveMeshConfig.SPEED_MAX, clamped.speed, 0f)
        assertEquals(LiveMeshConfig.DARK_TINT_MAX, clamped.darkTint, 0f)
        assertEquals(LiveMeshConfig.RENDER_SCALE_MAX, clamped.renderScale, 0f)
        assertEquals(LiveMeshConfig.LAYER_ALPHA_MAX, clamped.layer2Alpha, 0f)
        assertEquals(LiveMeshConfig.LAYER_ALPHA_MAX, clamped.layer3Alpha, 0f)
        assertEquals(LiveMeshConfig.ZOOM_MAX, clamped.zoom, 0f)
        assertEquals(LiveMeshConfig.BOTTOM_SHADE_MAX, clamped.bottomShade, 0f)
        assertEquals(LiveMeshConfig.TEXTURE_SIZE_MAX, clamped.textureSize)
    }

    @Test
    fun valuesInsideBoundsAreKept() {
        val config = config(blur = 1.5f, saturation = 2f, speed = 0.5f, darkTint = 0.4f, renderScale = 2f, zoom = 1.2f)

        assertEquals(config, config.clamped())
    }

    @Test
    fun reverseFlagIsNotAffectedByClamping() {
        assertEquals(true, config(reverse = true).clamped().reverseDirection)
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
    fun advanceAngleTurnsForwardByFrameShareOfPeriod() {
        assertEquals(9f, advanceAngle(current = 0f, direction = 1f, tempo = 1f, dtSeconds = 1f, periodMs = 40000), 1e-4f)
    }

    @Test
    fun advanceAngleTurnsBackwardForNegativeDirection() {
        assertEquals(-9f, advanceAngle(current = 0f, direction = -1f, tempo = 1f, dtSeconds = 1f, periodMs = 40000), 1e-4f)
    }

    @Test
    fun advanceAngleScalesWithTempo() {
        assertEquals(18f, advanceAngle(current = 0f, direction = 1f, tempo = 2f, dtSeconds = 1f, periodMs = 40000), 1e-4f)
    }

    @Test
    fun advanceAngleWrapsAroundFullTurn() {
        assertEquals(4f, advanceAngle(current = 355f, direction = 1f, tempo = 1f, dtSeconds = 1f, periodMs = 40000), 1e-4f)
    }

    @Test
    fun advanceAngleFollowsSpeedThroughShorterPeriod() {
        val period = liveMeshPeriodMillis(40000, speed = 2f)

        assertEquals(18f, advanceAngle(current = 0f, direction = 1f, tempo = 1f, dtSeconds = 1f, periodMs = period), 1e-4f)
    }

    @Test
    fun zeroSpeedIsTreatedAsSlowestInsteadOfDividingByZero() {
        assertEquals(240000, liveMeshPeriodMillis(60000, speed = 0f))
    }

    @Test
    fun reactivityValuesAreClampedToUnitRange() {
        val clamped = config(beat = -1f, lyric = 3f).clamped()

        assertEquals(LiveMeshConfig.REACTIVITY_MIN, clamped.beatReactivity, 0f)
        assertEquals(LiveMeshConfig.REACTIVITY_MAX, clamped.lyricReactivity, 0f)
    }

    @Test
    fun reactiveZoomStaysAtBaseWhenThereIsNoPulse() {
        assertEquals(1.7f, reactiveZoom(1.7f, beat = 0f, lyric = 0f, beatReactivity = 1f, lyricReactivity = 1f), 0f)
    }

    @Test
    fun reactiveZoomGrowsWithBeatAndRespectsReactivity() {
        assertEquals(1.7f + BEAT_ZOOM_BOOST, reactiveZoom(1.7f, beat = 1f, lyric = 0f, beatReactivity = 1f, lyricReactivity = 0f), 1e-6f)
        assertEquals(1.7f, reactiveZoom(1.7f, beat = 1f, lyric = 0f, beatReactivity = 0f, lyricReactivity = 0f), 1e-6f)
    }

    @Test
    fun reactiveZoomIgnoresOutOfRangePulses() {
        assertEquals(1.7f + BEAT_ZOOM_BOOST, reactiveZoom(1.7f, beat = 5f, lyric = 0f, beatReactivity = 1f, lyricReactivity = 0f), 1e-6f)
        assertEquals(1.7f, reactiveZoom(1.7f, beat = -5f, lyric = 0f, beatReactivity = 1f, lyricReactivity = 0f), 1e-6f)
    }

    @Test
    fun presetsAreAllWithinBoundsAndUniquelyNamed() {
        LiveMeshPresets.forEach { preset ->
            assertEquals(preset.config.clamped(), preset.config)
        }
        assertEquals(LiveMeshPresets.size, LiveMeshPresets.map { it.nameRes }.distinct().size)
    }

    @Test
    fun firstPresetIsTheOriginalDefault() {
        assertEquals(LiveMeshConfig.Default, LiveMeshPresets.first().config)
    }

    @Test
    fun autoTempoIsNeutralWithoutEstimate() {
        assertEquals(1f, autoTempoFactor(null), 0f)
    }

    @Test
    fun autoTempoIsNeutralAtOneTwentyBpm() {
        assertEquals(1f, autoTempoFactor(120f), 1e-6f)
    }

    @Test
    fun autoTempoIsLimitedToHalfAndDoubleSpeed() {
        assertEquals(2f, autoTempoFactor(240f), 0f)
        assertEquals(0.5f, autoTempoFactor(30f), 0f)
    }

    @Test
    fun adaptiveScaleGrowsWhenFramesRunSlow() {
        assertEquals(3f, nextAdaptiveScale(current = 2f, avgFrameMs = 30f, base = 2f), 1e-6f)
    }

    @Test
    fun adaptiveScaleNeverExceedsMaximum() {
        assertEquals(LiveMeshConfig.RENDER_SCALE_MAX, nextAdaptiveScale(current = 7f, avgFrameMs = 40f, base = 4f), 0f)
    }

    @Test
    fun adaptiveScaleEasesBackWhenFramesRecover() {
        assertEquals(2f, nextAdaptiveScale(current = 3f, avgFrameMs = 12f, base = 2f), 1e-6f)
    }

    @Test
    fun adaptiveScaleNeverDropsBelowBase() {
        assertEquals(4f, nextAdaptiveScale(current = 4f, avgFrameMs = 5f, base = 4f), 0f)
    }

    @Test
    fun adaptiveScaleHoldsBetweenThresholds() {
        assertEquals(3f, nextAdaptiveScale(current = 3f, avgFrameMs = 20f, base = 2f), 0f)
    }

    @Test
    fun periodIsRoundedToWholeMilliseconds() {
        assertEquals(13333, liveMeshPeriodMillis(40000, speed = 3f))
    }

    @Test
    fun playerKeysKeepOriginalPreferenceNames() {
        assertEquals("liveMeshBlurStrength", PlayerLiveMeshKeys.blurStrength.name)
        assertEquals("liveMeshDarkTint", PlayerLiveMeshKeys.darkTint.name)
    }

    @Test
    fun miniKeysAreSeparateFromPlayerKeys() {
        assertEquals("miniLiveMeshBlurStrength", MiniLiveMeshKeys.blurStrength.name)
        assertEquals("miniLiveMeshReverseDirection", MiniLiveMeshKeys.reverseDirection.name)
    }

    @Test
    fun activePresetIsUnknownBeforeSettingsLoad() {
        assertEquals(-1, activePresetIndex(null))
    }

    @Test
    fun savedDefaultsMarkTheOriginalPresetActive() {
        assertEquals(0, activePresetIndex(LiveMeshConfig.Default))
    }

    @Test
    fun eachPresetConfigMapsBackToItsOwnIndex() {
        LiveMeshPresets.forEachIndexed { index, preset ->
            assertEquals(index, activePresetIndex(preset.config))
        }
    }

    @Test
    fun customSettingsMatchNoPreset() {
        val custom = LiveMeshConfig.Default.copy(zoom = 1.9f)

        assertEquals(-1, activePresetIndex(custom))
    }

    @Test
    fun emptyPreferencesFallBackToDefaults() {
        val config = mutablePreferencesOf().toLiveMeshConfig(PlayerLiveMeshKeys, LiveMeshConfig.Default)

        assertEquals(LiveMeshConfig.Default, config)
    }

    @Test
    fun storedPreferenceOverridesDefaultAndOthersStayAtDefault() {
        val prefs = mutablePreferencesOf(PlayerLiveMeshKeys.reverseDirection to true)

        val config = prefs.toLiveMeshConfig(PlayerLiveMeshKeys, LiveMeshConfig.Default)

        assertEquals(true, config.reverseDirection)
        assertEquals(LiveMeshConfig.Default.copy(reverseDirection = true), config)
    }

    @Test
    fun storedOutOfRangeValuesAreClampedOnRead() {
        val prefs = mutablePreferencesOf(
            PlayerLiveMeshKeys.zoom to 9f,
            PlayerLiveMeshKeys.textureSize to 4096,
        )

        val config = prefs.toLiveMeshConfig(PlayerLiveMeshKeys, LiveMeshConfig.Default)

        assertEquals(LiveMeshConfig.ZOOM_MAX, config.zoom, 0f)
        assertEquals(LiveMeshConfig.TEXTURE_SIZE_MAX, config.textureSize)
    }

    @Test
    fun miniPreferencesAreReadWithMiniDefaults() {
        val config = mutablePreferencesOf().toLiveMeshConfig(MiniLiveMeshKeys, LiveMeshConfig.MiniDefault)

        assertEquals(LiveMeshConfig.MiniDefault, config)
    }
}
