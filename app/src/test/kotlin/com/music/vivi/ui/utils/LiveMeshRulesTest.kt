package com.music.vivi.ui.utils

import androidx.datastore.preferences.core.mutablePreferencesOf
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
        assertFalse(rows.miniSettings)
    }

    @Test
    fun miniOnlyLiveMeshHidesPlayerRowsAndShowsMiniRows() {
        val rows = liveMeshRowVisibility(other, live, separateMini = true)

        assertFalse(rows.presets)
        assertFalse(rows.playerSettings)
        assertTrue(rows.miniSettings)
    }

    @Test
    fun sharedRowsStayWhileEitherSurfaceUsesLiveMesh() {
        listOf(liveMeshRowVisibility(live, other, false), liveMeshRowVisibility(other, live, false)).forEach { rows ->
            assertTrue(rows.separateMini)
            assertTrue(rows.autoTempo)
            assertTrue(rows.motion)
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
        assertTrue(rows.autoTempo && rows.motion)
    }

    @Test
    fun noSurfaceOnLiveMeshHidesEveryRowEvenWithSeparateSettingsOn() {
        val others = PlayerBackgroundStyle.values().filter { it != live }
        others.forEach { player ->
            others.forEach { mini ->
                val rows = liveMeshRowVisibility(player, mini, separateMini = true)
                assertFalse("$player / $mini", rows.presets || rows.playerSettings)
                assertFalse("$player / $mini", rows.miniSettings)
                assertFalse("$player / $mini", rows.separateMini)
                assertFalse("$player / $mini", rows.autoTempo)
                assertFalse("$player / $mini", rows.motion)
            }
        }
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
        val once = LiveMeshConfig.Default.copy(speed = 9f, renderScale = 99f).clamped()

        assertEquals(once, once.clamped())
    }

    @Test
    fun clampingKeepsShadesWithinRange() {
        val clamped = LiveMeshConfig.Default.copy(bottomShade = 7f).clamped()

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
    fun crispPresetRendersAtFullResolution() {
        val crisp = LiveMeshPresets.single { it.config.renderScale == 1f }.config

        assertEquals(1f, crisp.renderScale, 0f)
    }

    @Test
    fun lowPowerPresetTurnsOffBeatAndLyricReactions() {
        val lowPower = LiveMeshPresets.single { it.config.renderScale == 8f }.config

        assertEquals(0f, lowPower.beatReactivity, 0f)
        assertEquals(0f, lowPower.lyricReactivity, 0f)
        assertEquals(LiveMeshConfig.RENDER_SCALE_MAX, lowPower.renderScale, 0f)
    }

    @Test
    fun originalPresetIsTheVividPartyLookAndIsFirst() {
        val original = LiveMeshPresets.first()

        assertEquals(LiveMeshConfig.Default, original.config)
        assertEquals(2f, original.config.speed, 0f)
    }

    @Test
    fun classicPresetKeepsThePreviousDefaultLook() {
        val classic = LiveMeshPresets.last().config

        assertEquals(1f, classic.speed, 0f)
        assertEquals(0.5f, classic.beatReactivity, 0f)
    }

    @Test
    fun playerAndMiniKeysUseDistinctPreferenceNames() {
        assertNotEquals(PlayerLiveMeshKeys.speed.name, MiniLiveMeshKeys.speed.name)
        assertEquals("miniLiveMeshSpeed", MiniLiveMeshKeys.speed.name)
    }

    @Test
    fun customPrefixBuildsItsOwnNamesForEveryField() {
        val keys = LiveMeshKeys(prefix = "x")

        assertEquals("xLiveMeshRenderScale", keys.renderScale.name)
        assertEquals("xLiveMeshLyricReactivity", keys.lyricReactivity.name)
    }

    @Test
    fun storedValuesForEveryFieldAreReadBack() {
        val keys = PlayerLiveMeshKeys
        val prefs = mutablePreferencesOf(
            keys.speed to 2f,
            keys.darkTint to 0.1f,
            keys.renderScale to 2f,
            keys.bottomShade to 0.5f,
            keys.beatReactivity to 0.9f,
            keys.lyricReactivity to 0.1f,
        )

        val config = prefs.toLiveMeshConfig(keys, LiveMeshConfig.Default)

        assertEquals(
            LiveMeshConfig(
                speed = 2f,
                darkTint = 0.1f,
                renderScale = 2f,
                bottomShade = 0.5f,
                beatReactivity = 0.9f,
                lyricReactivity = 0.1f,
            ),
            config,
        )
    }

    @Test
    fun miniPreferencesDoNotLeakIntoPlayerSettings() {
        val prefs = mutablePreferencesOf(MiniLiveMeshKeys.speed to 1.2f)

        val player = prefs.toLiveMeshConfig(PlayerLiveMeshKeys, LiveMeshConfig.Default)

        assertEquals(LiveMeshConfig.Default.speed, player.speed, 0f)
    }
}
