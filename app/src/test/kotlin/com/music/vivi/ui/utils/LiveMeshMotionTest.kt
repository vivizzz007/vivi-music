package com.music.vivi.ui.utils

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.music.vivi.constants.PlayerBackgroundStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveMeshMotionTest {
    @Test
    fun defaultMotionMatchesThePortedConstants() {
        val motion = LiveMeshMotion.Default

        assertEquals(BEAT_SPEED_BOOST, motion.beatSpeedBoost, 0f)
        assertEquals(PULSE_RISE_PER_SECOND, motion.pulseRise, 0f)
        assertEquals(PULSE_FALL_PER_SECOND, motion.pulseFall, 0f)
        assertEquals(TEMPO_RISE_PER_SECOND, motion.tempoRise, 0f)
        assertEquals(TEMPO_FALL_PER_SECOND, motion.tempoFall, 0f)
        assertEquals(ARTWORK_DIM_STRENGTH, motion.artworkDimStrength, 0f)
    }

    @Test
    fun clampedKeepsEveryValueInsideItsRange() {
        val clamped = LiveMeshMotion.Default.copy(
            beatSpeedBoost = 9f,
            pulseRise = 0f,
            pulseFall = 500f,
            tempoRise = -1f,
            tempoFall = 99f,
            artworkDimStrength = 2f,
        ).clamped()

        assertEquals(LiveMeshMotion.BEAT_SPEED_BOOST_MAX, clamped.beatSpeedBoost, 0f)
        assertEquals(LiveMeshMotion.RATE_MIN, clamped.pulseRise, 0f)
        assertEquals(LiveMeshMotion.PULSE_FALL_MAX, clamped.pulseFall, 0f)
        assertEquals(LiveMeshMotion.RATE_MIN, clamped.tempoRise, 0f)
        assertEquals(LiveMeshMotion.TEMPO_RATE_MAX, clamped.tempoFall, 0f)
        assertEquals(LiveMeshMotion.DIM_STRENGTH_MAX, clamped.artworkDimStrength, 0f)
    }

    @Test
    fun clampedLeavesInRangeValuesAlone() {
        assertEquals(LiveMeshMotion.Default, LiveMeshMotion.Default.clamped())
    }

    @Test
    fun savedValuesReadBackThroughPreferences() {
        val prefs = mutablePreferencesOf(
            LiveMeshMotionKeys.beatSpeedBoost to 2.5f,
            LiveMeshMotionKeys.artworkDimStrength to 0.8f,
        )

        val motion = prefs.toLiveMeshMotion()

        assertEquals(2.5f, motion.beatSpeedBoost, 0f)
        assertEquals(0.8f, motion.artworkDimStrength, 0f)
        assertEquals(LiveMeshMotion.Default.pulseRise, motion.pulseRise, 0f)
    }

    @Test
    fun missingPreferencesFallBackToDefaults() {
        assertEquals(LiveMeshMotion.Default, mutablePreferencesOf().toLiveMeshMotion())
    }

    @Test
    fun outOfRangePreferencesAreClampedWhenRead() {
        val prefs = mutablePreferencesOf(LiveMeshMotionKeys.pulseFall to 1000f)

        assertEquals(LiveMeshMotion.PULSE_FALL_MAX, prefs.toLiveMeshMotion().pulseFall, 0f)
    }

    @Test
    fun motionRowsFollowAnySurfaceThatUsesLiveMesh() {
        val live = PlayerBackgroundStyle.LIVE_MESH
        val other = PlayerBackgroundStyle.BLUR

        assertTrue(liveMeshRowVisibility(live, other, separateMini = false).motion)
        assertTrue(liveMeshRowVisibility(other, live, separateMini = true).motion)
        assertFalse(liveMeshRowVisibility(other, other, separateMini = false).motion)
    }
}
