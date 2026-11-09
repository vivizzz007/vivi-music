package com.music.vivi.ui.utils

import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveMeshConfigTest {
    private fun config(
        speed: Float = 1f,
        darkTint: Float = 0.2f,
        renderScale: Float = 4f,
        bottomShade: Float = 0.25f,
        beat: Float = 0.5f,
        lyric: Float = 0.5f,
    ) = LiveMeshConfig(
        speed = speed,
        darkTint = darkTint,
        renderScale = renderScale,
        bottomShade = bottomShade,
        beatReactivity = beat,
        lyricReactivity = lyric,
    )

    @Test
    fun defaultConfigIsTheVividPartyLook() {
        assertEquals(
            LiveMeshConfig(speed = 2f, darkTint = 0.15f, renderScale = 4f, bottomShade = 0.25f, beatReactivity = 0.6f, lyricReactivity = 0.8f),
            LiveMeshConfig.Default,
        )
    }

    @Test
    fun miniDefaultKeepsOriginalMiniAppearance() {
        assertEquals(0.3f, LiveMeshConfig.MiniDefault.darkTint, 0f)
        assertEquals(LiveMeshConfig.Default.speed, LiveMeshConfig.MiniDefault.speed, 0f)
    }

    @Test
    fun defaultConfigIsAlreadyWithinBounds() {
        assertEquals(LiveMeshConfig.Default, LiveMeshConfig.Default.clamped())
    }

    @Test
    fun valuesBelowMinimumAreRaisedToMinimum() {
        val clamped = config(speed = 0f, darkTint = -1f, renderScale = 0f, bottomShade = -1f).clamped()

        assertEquals(LiveMeshConfig.SPEED_MIN, clamped.speed, 0f)
        assertEquals(LiveMeshConfig.DARK_TINT_MIN, clamped.darkTint, 0f)
        assertEquals(LiveMeshConfig.RENDER_SCALE_MIN, clamped.renderScale, 0f)
        assertEquals(LiveMeshConfig.BOTTOM_SHADE_MIN, clamped.bottomShade, 0f)
    }

    @Test
    fun valuesAboveMaximumAreLoweredToMaximum() {
        val clamped = config(speed = 9f, darkTint = 5f, renderScale = 99f, bottomShade = 5f).clamped()

        assertEquals(LiveMeshConfig.SPEED_MAX, clamped.speed, 0f)
        assertEquals(LiveMeshConfig.DARK_TINT_MAX, clamped.darkTint, 0f)
        assertEquals(LiveMeshConfig.RENDER_SCALE_MAX, clamped.renderScale, 0f)
        assertEquals(LiveMeshConfig.BOTTOM_SHADE_MAX, clamped.bottomShade, 0f)
    }

    @Test
    fun valuesInsideBoundsAreKept() {
        val config = config(speed = 0.5f, darkTint = 0.4f, renderScale = 2f, bottomShade = 0.5f)

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
    fun reactivityValuesAreClampedToUnitRange() {
        val clamped = config(beat = -1f, lyric = 3f).clamped()

        assertEquals(LiveMeshConfig.REACTIVITY_MIN, clamped.beatReactivity, 0f)
        assertEquals(LiveMeshConfig.REACTIVITY_MAX, clamped.lyricReactivity, 0f)
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
    fun playerKeysKeepOriginalPreferenceNames() {
        assertEquals("liveMeshSpeed", PlayerLiveMeshKeys.speed.name)
        assertEquals("liveMeshDarkTint", PlayerLiveMeshKeys.darkTint.name)
    }

    @Test
    fun miniKeysAreSeparateFromPlayerKeys() {
        assertEquals("miniLiveMeshSpeed", MiniLiveMeshKeys.speed.name)
        assertEquals("miniLiveMeshDarkTint", MiniLiveMeshKeys.darkTint.name)
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
        val custom = LiveMeshConfig.Default.copy(speed = 2.9f)

        assertEquals(-1, activePresetIndex(custom))
    }

    @Test
    fun emptyPreferencesFallBackToDefaults() {
        val config = mutablePreferencesOf().toLiveMeshConfig(PlayerLiveMeshKeys, LiveMeshConfig.Default)

        assertEquals(LiveMeshConfig.Default, config)
    }

    @Test
    fun storedPreferenceOverridesDefaultAndOthersStayAtDefault() {
        val prefs = mutablePreferencesOf(PlayerLiveMeshKeys.speed to 0.5f)

        val config = prefs.toLiveMeshConfig(PlayerLiveMeshKeys, LiveMeshConfig.Default)

        assertEquals(LiveMeshConfig.Default.copy(speed = 0.5f), config)
    }

    @Test
    fun storedOutOfRangeValuesAreClampedOnRead() {
        val prefs = mutablePreferencesOf(
            PlayerLiveMeshKeys.speed to 9f,
            PlayerLiveMeshKeys.renderScale to 99f,
        )

        val config = prefs.toLiveMeshConfig(PlayerLiveMeshKeys, LiveMeshConfig.Default)

        assertEquals(LiveMeshConfig.SPEED_MAX, config.speed, 0f)
        assertEquals(LiveMeshConfig.RENDER_SCALE_MAX, config.renderScale, 0f)
    }

    @Test
    fun miniPreferencesAreReadWithMiniDefaults() {
        val config = mutablePreferencesOf().toLiveMeshConfig(MiniLiveMeshKeys, LiveMeshConfig.MiniDefault)

        assertEquals(LiveMeshConfig.MiniDefault, config)
    }
}
