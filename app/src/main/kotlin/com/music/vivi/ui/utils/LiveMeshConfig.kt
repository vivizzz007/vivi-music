/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.music.vivi.constants.LiveMeshSeparateMiniKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.roundToInt

internal data class LiveMeshConfig(
    val speed: Float,
    val darkTint: Float,
    val renderScale: Float,
    val bottomShade: Float,
    val beatReactivity: Float,
    val lyricReactivity: Float,
) {
    companion object {
        const val SPEED_MIN = 0.25f
        const val SPEED_MAX = 3f
        const val DARK_TINT_MIN = 0f
        const val DARK_TINT_MAX = 0.6f
        const val RENDER_SCALE_MIN = 1f
        const val RENDER_SCALE_MAX = 8f
        const val BOTTOM_SHADE_MIN = 0f
        const val BOTTOM_SHADE_MAX = 1f
        const val REACTIVITY_MIN = 0f
        const val REACTIVITY_MAX = 1f

        val Default = LiveMeshConfig(
            speed = 2f,
            darkTint = 0.15f,
            renderScale = 4f,
            bottomShade = 0.25f,
            beatReactivity = 0.6f,
            lyricReactivity = 0.8f,
        )

        val MiniDefault = Default.copy(
            darkTint = 0.3f,
        )
    }
}

internal fun LiveMeshConfig.clamped(): LiveMeshConfig = LiveMeshConfig(
    speed = speed.coerceIn(LiveMeshConfig.SPEED_MIN, LiveMeshConfig.SPEED_MAX),
    darkTint = darkTint.coerceIn(LiveMeshConfig.DARK_TINT_MIN, LiveMeshConfig.DARK_TINT_MAX),
    renderScale = renderScale.coerceIn(LiveMeshConfig.RENDER_SCALE_MIN, LiveMeshConfig.RENDER_SCALE_MAX),
    bottomShade = bottomShade.coerceIn(LiveMeshConfig.BOTTOM_SHADE_MIN, LiveMeshConfig.BOTTOM_SHADE_MAX),
    beatReactivity = beatReactivity.coerceIn(LiveMeshConfig.REACTIVITY_MIN, LiveMeshConfig.REACTIVITY_MAX),
    lyricReactivity = lyricReactivity.coerceIn(LiveMeshConfig.REACTIVITY_MIN, LiveMeshConfig.REACTIVITY_MAX),
)

// Layers turn faster on fast songs; 120 BPM is neutral, and the factor is limited so it never feels frantic.
internal fun autoTempoFactor(bpm: Float?): Float =
    if (bpm == null) 1f else (bpm / 120f).coerceIn(0.5f, 2f)

// Higher speed shortens the rotation period so the mesh turns faster without changing its shape.
internal fun liveMeshPeriodMillis(baseMillis: Int, speed: Float): Int =
    (baseMillis / speed.coerceAtLeast(LiveMeshConfig.SPEED_MIN)).roundToInt()

internal fun advanceAngle(current: Float, direction: Float, tempo: Float, dtSeconds: Float, periodMs: Int): Float =
    (current + direction * 360f * tempo * dtSeconds * 1000f / periodMs) % 360f

// Per-frame factors 0.5 / 0.12 and speed 0.05 / 0.03 from the Better Lyrics Lucid preset, converted to rates at 60 fps.
internal const val BEAT_SPEED_BOOST = 1.4f
internal const val PULSE_RISE_PER_SECOND = 41.6f
internal const val PULSE_FALL_PER_SECOND = 7.6f
internal const val TEMPO_RISE_PER_SECOND = 3.08f
internal const val TEMPO_FALL_PER_SECOND = 1.82f

// Rising and falling use separate rates, so a value snaps toward a new target and then settles back slowly.
// The exponential form gives the same response at any frame rate.
internal fun followTowards(
    current: Float,
    target: Float,
    risePerSecond: Float,
    fallPerSecond: Float,
    dtSeconds: Float,
): Float {
    val rate = if (target > current) risePerSecond else fallPerSecond
    return current + (target - current) * (1f - exp(-rate * dtSeconds))
}

// Beats push the rotation forward for a moment; reactivity scales how much, and zero keeps the speed unchanged.
internal fun beatSpeedFactor(beat: Float, beatReactivity: Float, boost: Float = BEAT_SPEED_BOOST): Float =
    1f + beat.coerceIn(0f, 1f) * boost * beatReactivity

// Player keys keep the original camelCase names; other scopes prepend their prefix to the "LiveMesh" part.
internal class LiveMeshKeys(private val prefix: String) {
    private fun name(suffix: String): String =
        if (prefix.isEmpty()) "liveMesh$suffix" else "${prefix}LiveMesh$suffix"

    val speed: Preferences.Key<Float> = floatPreferencesKey(name("Speed"))
    val darkTint: Preferences.Key<Float> = floatPreferencesKey(name("DarkTint"))
    val renderScale: Preferences.Key<Float> = floatPreferencesKey(name("RenderScale"))
    val bottomShade: Preferences.Key<Float> = floatPreferencesKey(name("BottomShade"))
    val beatReactivity: Preferences.Key<Float> = floatPreferencesKey(name("BeatReactivity"))
    val lyricReactivity: Preferences.Key<Float> = floatPreferencesKey(name("LyricReactivity"))
}

internal val PlayerLiveMeshKeys = LiveMeshKeys(prefix = "")
internal val MiniLiveMeshKeys = LiveMeshKeys(prefix = "mini")

internal class LiveMeshStore(
    val config: LiveMeshConfig,
    val save: (LiveMeshConfig) -> Unit,
)

internal fun Preferences.toLiveMeshConfig(keys: LiveMeshKeys, defaults: LiveMeshConfig): LiveMeshConfig = LiveMeshConfig(
    speed = this[keys.speed] ?: defaults.speed,
    darkTint = this[keys.darkTint] ?: defaults.darkTint,
    renderScale = this[keys.renderScale] ?: defaults.renderScale,
    bottomShade = this[keys.bottomShade] ?: defaults.bottomShade,
    beatReactivity = this[keys.beatReactivity] ?: defaults.beatReactivity,
    lyricReactivity = this[keys.lyricReactivity] ?: defaults.lyricReactivity,
).clamped()

@Composable
internal fun rememberMiniLiveMeshStore(): LiveMeshStore {
    val separate by rememberPreference(LiveMeshSeparateMiniKey, defaultValue = false)
    val playerStore = rememberLiveMeshStore(PlayerLiveMeshKeys, LiveMeshConfig.Default)
    val miniStore = rememberLiveMeshStore(MiniLiveMeshKeys, LiveMeshConfig.MiniDefault)
    return selectLiveMeshStore(separate, playerStore, miniStore)
}

internal fun <T> selectLiveMeshStore(separate: Boolean, playerStore: T, miniStore: T): T =
    if (separate) miniStore else playerStore

internal data class LiveMeshRowVisibility(
    val presets: Boolean,
    val playerSettings: Boolean,
    val separateMini: Boolean,
    val miniSettings: Boolean,
    val autoTempo: Boolean,
    val motion: Boolean,
)

// Each row follows the surface it configures: player-only rows hide while the player uses another style, and vice versa.
// Shared rows (auto tempo, separate switch) stay while either surface uses Live Mesh, since both read them.
internal fun liveMeshRowVisibility(
    playerStyle: PlayerBackgroundStyle,
    miniStyle: PlayerBackgroundStyle,
    separateMini: Boolean,
): LiveMeshRowVisibility {
    val playerLive = playerStyle == PlayerBackgroundStyle.LIVE_MESH
    val miniLive = miniStyle == PlayerBackgroundStyle.LIVE_MESH
    val anyLive = playerLive || miniLive
    return LiveMeshRowVisibility(
        presets = playerLive,
        playerSettings = playerLive,
        separateMini = anyLive,
        miniSettings = miniLive && separateMini,
        autoTempo = anyLive,
        motion = anyLive,
    )
}

@Composable
internal fun rememberLiveMeshStore(keys: LiveMeshKeys, defaults: LiveMeshConfig): LiveMeshStore {
    val speed by rememberPreference(keys.speed, defaultValue = defaults.speed)
    val darkTint by rememberPreference(keys.darkTint, defaultValue = defaults.darkTint)
    val renderScale by rememberPreference(keys.renderScale, defaultValue = defaults.renderScale)
    val bottomShade by rememberPreference(keys.bottomShade, defaultValue = defaults.bottomShade)
    val beatReactivity by rememberPreference(keys.beatReactivity, defaultValue = defaults.beatReactivity)
    val lyricReactivity by rememberPreference(keys.lyricReactivity, defaultValue = defaults.lyricReactivity)

    val config = LiveMeshConfig(
        speed = speed,
        darkTint = darkTint,
        renderScale = renderScale,
        bottomShade = bottomShade,
        beatReactivity = beatReactivity,
        lyricReactivity = lyricReactivity,
    ).clamped()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val save: (LiveMeshConfig) -> Unit = { value ->
        val clampedValue = value.clamped()
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[keys.speed] = clampedValue.speed
                prefs[keys.darkTint] = clampedValue.darkTint
                prefs[keys.renderScale] = clampedValue.renderScale
                prefs[keys.bottomShade] = clampedValue.bottomShade
                prefs[keys.beatReactivity] = clampedValue.beatReactivity
                prefs[keys.lyricReactivity] = clampedValue.lyricReactivity
            }
        }
    }
    return LiveMeshStore(config, save)
}
