package com.music.vivi.ui.utils

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.music.vivi.constants.LiveMeshShakeKey
import com.music.vivi.constants.PlayerBackgroundStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveMeshRulesTest {
    @Test
    fun popPlaysOnlyWhenAPresetBecomesActiveAfterTheFirstLoad() {
        assertTrue(shouldPopOnActivation(wasActive = false, isActive = true, wasLoaded = true))
    }

    @Test
    fun popIsSkippedOnTheFirstLoadThatActivatesThePreset() {
        assertEquals(false, shouldPopOnActivation(wasActive = false, isActive = true, wasLoaded = false))
    }

    @Test
    fun popIsSkippedWhenThePresetWasAlreadyActive() {
        assertEquals(false, shouldPopOnActivation(wasActive = true, isActive = true, wasLoaded = true))
    }

    @Test
    fun popIsSkippedWhenThePresetStopsBeingActive() {
        assertEquals(false, shouldPopOnActivation(wasActive = true, isActive = false, wasLoaded = true))
    }

    @Test
    fun popIsSkippedWhileInactive() {
        assertEquals(false, shouldPopOnActivation(wasActive = false, isActive = false, wasLoaded = true))
        assertEquals(false, shouldPopOnActivation(wasActive = false, isActive = false, wasLoaded = false))
    }

    @Test
    fun playerStoreIsUsedUnlessMiniSettingsAreSeparate() {
        assertEquals("player", selectLiveMeshStore(separate = false, playerStore = "player", miniStore = "mini"))
    }

    @Test
    fun miniStoreIsUsedWhenSettingsAreSeparate() {
        assertEquals("mini", selectLiveMeshStore(separate = true, playerStore = "player", miniStore = "mini"))
    }

    private val live = PlayerBackgroundStyle.LIVE_MESH
    private val other = PlayerBackgroundStyle.GRADIENT

    @Test
    fun playerOnlyLiveMeshShowsPlayerRowsAndHidesMiniRows() {
        val rows = liveMeshRowVisibility(live, other, separateMini = true)

        assertTrue(rows.presets)
        assertTrue(rows.playerSettings)
        assertTrue(rows.adaptiveQuality)
        assertTrue(rows.shake)
        assertFalse(rows.miniSettings)
    }

    @Test
    fun miniOnlyLiveMeshHidesPlayerRowsAndShowsMiniRows() {
        val rows = liveMeshRowVisibility(other, live, separateMini = true)

        assertFalse(rows.presets)
        assertFalse(rows.playerSettings)
        assertFalse(rows.adaptiveQuality)
        assertFalse(rows.shake)
        assertTrue(rows.miniSettings)
    }

    @Test
    fun sharedRowsStayWhileEitherSurfaceUsesLiveMesh() {
        listOf(liveMeshRowVisibility(live, other, false), liveMeshRowVisibility(other, live, false)).forEach { rows ->
            assertTrue(rows.separateMini)
            assertTrue(rows.autoTempo)
        }
    }

    @Test
    fun miniSettingsNeedSeparateSettingsOn() {
        assertFalse(liveMeshRowVisibility(live, live, separateMini = false).miniSettings)
        assertTrue(liveMeshRowVisibility(live, live, separateMini = true).miniSettings)
    }

    @Test
    fun bothSurfacesOnLiveMeshShowEveryRow() {
        val rows = liveMeshRowVisibility(live, live, separateMini = true)

        assertTrue(rows.presets && rows.playerSettings && rows.separateMini && rows.miniSettings)
        assertTrue(rows.autoTempo && rows.adaptiveQuality && rows.shake)
    }

    @Test
    fun noSurfaceOnLiveMeshHidesEveryRowEvenWithSeparateSettingsOn() {
        val others = PlayerBackgroundStyle.values().filter { it != live }
        others.forEach { player ->
            others.forEach { mini ->
                val rows = liveMeshRowVisibility(player, mini, separateMini = true)
                assertFalse("$player / $mini", rows.presets || rows.playerSettings || rows.adaptiveQuality || rows.shake)
                assertFalse("$player / $mini", rows.miniSettings)
                assertFalse("$player / $mini", rows.separateMini)
                assertFalse("$player / $mini", rows.autoTempo)
            }
        }
    }

    @Test
    fun zeroReactivityIgnoresBeatAndLyricPulses() {
        assertEquals(1.7f, reactiveZoom(1.7f, beat = 1f, lyric = 1f, beatReactivity = 0f, lyricReactivity = 0f), 1e-6f)
    }

    @Test
    fun fullPulsesAddBothBoostsAtFullReactivity() {
        val expected = 1.7f + BEAT_ZOOM_BOOST + LYRIC_ZOOM_BOOST
        assertEquals(expected, reactiveZoom(1.7f, beat = 1f, lyric = 1f, beatReactivity = 1f, lyricReactivity = 1f), 1e-6f)
    }

    @Test
    fun halfPulseGivesHalfBoost() {
        assertEquals(1.7f + BEAT_ZOOM_BOOST / 2f, reactiveZoom(1.7f, beat = 0.5f, lyric = 0f, beatReactivity = 1f, lyricReactivity = 0f), 1e-6f)
    }

    @Test
    fun lyricBoostUsesItsOwnReactivity() {
        assertEquals(1.7f + LYRIC_ZOOM_BOOST * 0.5f, reactiveZoom(1.7f, beat = 0f, lyric = 1f, beatReactivity = 1f, lyricReactivity = 0.5f), 1e-6f)
    }

    @Test
    fun zoomBaseIsKeptWhenNoPulseIsPresent() {
        assertEquals(2f, reactiveZoom(2f, beat = 0f, lyric = 0f, beatReactivity = 1f, lyricReactivity = 1f), 0f)
    }

    @Test
    fun autoTempoIsNeutralOnlyNearOneTwentyBpm() {
        assertEquals(1f, autoTempoFactor(120f), 1e-6f)
        assertNotEquals(1f, autoTempoFactor(100f), 1e-6f)
    }

    @Test
    fun autoTempoScalesLinearlyWithBpm() {
        assertEquals(0.75f, autoTempoFactor(90f), 1e-6f)
        assertEquals(1.5f, autoTempoFactor(180f), 1e-6f)
    }

    @Test
    fun autoTempoClampsSlowEstimatesToHalfSpeed() {
        assertEquals(0.5f, autoTempoFactor(60f), 1e-6f)
        assertEquals(0.5f, autoTempoFactor(0f), 1e-6f)
        assertEquals(0.5f, autoTempoFactor(-40f), 1e-6f)
    }

    @Test
    fun autoTempoClampsFastEstimatesToDoubleSpeed() {
        assertEquals(2f, autoTempoFactor(300f), 1e-6f)
        assertEquals(2f, autoTempoFactor(10_000f), 1e-6f)
    }

    @Test
    fun adaptiveScaleHoldsExactlyAtTheSlowThreshold() {
        assertEquals(3f, nextAdaptiveScale(current = 3f, avgFrameMs = 24f, base = 2f), 0f)
    }

    @Test
    fun adaptiveScaleHoldsExactlyAtTheFastThreshold() {
        assertEquals(3f, nextAdaptiveScale(current = 3f, avgFrameMs = 17f, base = 2f), 0f)
    }

    @Test
    fun adaptiveScaleDoesNotGrowBeyondMaximumWhenAlreadyThere() {
        assertEquals(LiveMeshConfig.RENDER_SCALE_MAX, nextAdaptiveScale(current = LiveMeshConfig.RENDER_SCALE_MAX, avgFrameMs = 60f, base = 1f), 0f)
    }

    @Test
    fun adaptiveScaleEasesBackOnlyDownToBase() {
        assertEquals(2f, nextAdaptiveScale(current = 2.5f, avgFrameMs = 10f, base = 2f), 1e-6f)
    }

    @Test
    fun adaptiveScaleSnapsUpToBaseWhenBelowIt() {
        assertEquals(2f, nextAdaptiveScale(current = 1f, avgFrameMs = 20f, base = 2f), 0f)
    }

    @Test
    fun adaptiveScaleStaysAtBaseWhenFramesAreFastAndNothingWasRaised() {
        assertEquals(2f, nextAdaptiveScale(current = 2f, avgFrameMs = 5f, base = 2f), 0f)
    }

    @Test
    fun adaptiveScaleRepeatedlyRaisesUnderSustainedLoad() {
        var scale = 2f
        repeat(10) { scale = nextAdaptiveScale(scale, avgFrameMs = 40f, base = 2f) }

        assertEquals(LiveMeshConfig.RENDER_SCALE_MAX, scale, 0f)
    }

    @Test
    fun unitSpeedKeepsEveryPeriod() {
        assertEquals(40000, liveMeshPeriodMillis(40000, speed = 1f))
    }

    @Test
    fun halfSpeedDoublesThePeriod() {
        assertEquals(80000, liveMeshPeriodMillis(40000, speed = 0.5f))
    }

    @Test
    fun negativeSpeedFallsBackToSlowestPeriod() {
        assertEquals(160000, liveMeshPeriodMillis(40000, speed = -2f))
    }

    @Test
    fun triplingSpeedRoundsThePeriodToWholeMilliseconds() {
        assertEquals(26667, liveMeshPeriodMillis(80000, speed = 3f))
    }

    @Test
    fun clampingIsIdempotent() {
        val once = LiveMeshConfig.Default.copy(zoom = 9f, textureSize = 9999).clamped()

        assertEquals(once, once.clamped())
    }

    @Test
    fun clampingKeepsReverseFlagAndShadesWithinRange() {
        val clamped = LiveMeshConfig.Default.copy(reverseDirection = true, bottomShade = 7f).clamped()

        assertTrue(clamped.reverseDirection)
        assertEquals(LiveMeshConfig.BOTTOM_SHADE_MAX, clamped.bottomShade, 0f)
    }

    @Test
    fun everyPresetHasAUniqueDescription() {
        assertEquals(LiveMeshPresets.size, LiveMeshPresets.map { it.descriptionRes }.distinct().size)
    }

    @Test
    fun everyPresetExceptOriginalChangesTheLook() {
        LiveMeshPresets.drop(1).forEach { preset ->
            assertNotEquals(LiveMeshConfig.Default, preset.config)
        }
    }

    @Test
    fun crispPresetRendersAtFullResolutionWithSharperTexture() {
        val crisp = LiveMeshPresets.single { it.config.renderScale == 1f }.config

        assertEquals(256, crisp.textureSize)
    }

    @Test
    fun lowPowerPresetTurnsOffBeatAndLyricReactions() {
        val lowPower = LiveMeshPresets.last().config

        assertEquals(0f, lowPower.beatReactivity, 0f)
        assertEquals(0f, lowPower.lyricReactivity, 0f)
        assertEquals(LiveMeshConfig.RENDER_SCALE_MAX, lowPower.renderScale, 0f)
    }

    @Test
    fun partyPresetIsTheFastestAndMostReactive() {
        val party = LiveMeshPresets.single { it.config.speed == 2f }.config

        assertTrue(party.beatReactivity > LiveMeshConfig.Default.beatReactivity)
        assertTrue(party.lyricReactivity > LiveMeshConfig.Default.lyricReactivity)
    }

    @Test
    fun shakeSwitchUsesItsStoredPreferenceName() {
        assertEquals("liveMeshShake", LiveMeshShakeKey.name)
    }

    @Test
    fun playerAndMiniKeysUseDistinctPreferenceNames() {
        assertNotEquals(PlayerLiveMeshKeys.textureSize.name, MiniLiveMeshKeys.textureSize.name)
        assertEquals("miniLiveMeshTextureSize", MiniLiveMeshKeys.textureSize.name)
    }

    @Test
    fun customPrefixBuildsItsOwnNamesForEveryField() {
        val keys = LiveMeshKeys(prefix = "x")

        assertEquals("xLiveMeshZoom", keys.zoom.name)
        assertEquals("xLiveMeshLyricReactivity", keys.lyricReactivity.name)
    }

    @Test
    fun storedValuesForEveryFieldAreReadBack() {
        val keys = PlayerLiveMeshKeys
        val prefs = mutablePreferencesOf(
            keys.blurStrength to 1.5f,
            keys.saturation to 2.2f,
            keys.speed to 2f,
            keys.darkTint to 0.1f,
            keys.renderScale to 2f,
            keys.layer2Alpha to 0.7f,
            keys.layer3Alpha to 0.4f,
            keys.zoom to 1.9f,
            keys.reverseDirection to true,
            keys.bottomShade to 0.5f,
            keys.textureSize to 192,
            keys.beatReactivity to 0.9f,
            keys.lyricReactivity to 0.1f,
        )

        val config = prefs.toLiveMeshConfig(keys, LiveMeshConfig.Default)

        assertEquals(
            LiveMeshConfig(
                blurStrength = 1.5f,
                saturation = 2.2f,
                speed = 2f,
                darkTint = 0.1f,
                renderScale = 2f,
                layer2Alpha = 0.7f,
                layer3Alpha = 0.4f,
                zoom = 1.9f,
                reverseDirection = true,
                bottomShade = 0.5f,
                textureSize = 192,
                beatReactivity = 0.9f,
                lyricReactivity = 0.1f,
            ),
            config,
        )
    }

    @Test
    fun miniPreferencesDoNotLeakIntoPlayerSettings() {
        val prefs = mutablePreferencesOf(MiniLiveMeshKeys.zoom to 1.2f)

        val player = prefs.toLiveMeshConfig(PlayerLiveMeshKeys, LiveMeshConfig.Default)

        assertEquals(LiveMeshConfig.Default.zoom, player.zoom, 0f)
    }
}
