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
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.music.vivi.constants.LiveMeshSeparateMiniKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal data class LiveMeshConfig(
    val blurStrength: Float,
    val saturation: Float,
    val speed: Float,
    val darkTint: Float,
    val renderScale: Float,
    val layer2Alpha: Float,
    val layer3Alpha: Float,
    val zoom: Float,
    val reverseDirection: Boolean,
    val bottomShade: Float,
    val textureSize: Int,
    val beatReactivity: Float,
    val lyricReactivity: Float,
) {
    companion object {
        const val BLUR_STRENGTH_MIN = 0.5f
        const val BLUR_STRENGTH_MAX = 2f
        const val SATURATION_MIN = 0.5f
        const val SATURATION_MAX = 2.5f
        const val SPEED_MIN = 0.25f
        const val SPEED_MAX = 3f
        const val DARK_TINT_MIN = 0f
        const val DARK_TINT_MAX = 0.6f
        const val RENDER_SCALE_MIN = 1f
        const val RENDER_SCALE_MAX = 8f
        const val LAYER_ALPHA_MIN = 0f
        const val LAYER_ALPHA_MAX = 1f
        const val ZOOM_MIN = 1f
        const val ZOOM_MAX = 2f
        const val BOTTOM_SHADE_MIN = 0f
        const val BOTTOM_SHADE_MAX = 1f
        const val TEXTURE_SIZE_MIN = 64
        const val TEXTURE_SIZE_MAX = 256
        const val REACTIVITY_MIN = 0f
        const val REACTIVITY_MAX = 1f

        val Default = LiveMeshConfig(
            blurStrength = 1f,
            saturation = 1.8f,
            speed = 1f,
            darkTint = 0.2f,
            renderScale = 4f,
            layer2Alpha = 0.6f,
            layer3Alpha = 0.5f,
            zoom = 1.7f,
            reverseDirection = false,
            bottomShade = 0.25f,
            textureSize = 128,
            beatReactivity = 0.5f,
            lyricReactivity = 0.5f,
        )

        val MiniDefault = Default.copy(
            saturation = 1.6f,
            darkTint = 0.3f,
            zoom = 1.5f,
        )
    }
}

internal fun LiveMeshConfig.clamped(): LiveMeshConfig = LiveMeshConfig(
    blurStrength = blurStrength.coerceIn(LiveMeshConfig.BLUR_STRENGTH_MIN, LiveMeshConfig.BLUR_STRENGTH_MAX),
    saturation = saturation.coerceIn(LiveMeshConfig.SATURATION_MIN, LiveMeshConfig.SATURATION_MAX),
    speed = speed.coerceIn(LiveMeshConfig.SPEED_MIN, LiveMeshConfig.SPEED_MAX),
    darkTint = darkTint.coerceIn(LiveMeshConfig.DARK_TINT_MIN, LiveMeshConfig.DARK_TINT_MAX),
    renderScale = renderScale.coerceIn(LiveMeshConfig.RENDER_SCALE_MIN, LiveMeshConfig.RENDER_SCALE_MAX),
    layer2Alpha = layer2Alpha.coerceIn(LiveMeshConfig.LAYER_ALPHA_MIN, LiveMeshConfig.LAYER_ALPHA_MAX),
    layer3Alpha = layer3Alpha.coerceIn(LiveMeshConfig.LAYER_ALPHA_MIN, LiveMeshConfig.LAYER_ALPHA_MAX),
    zoom = zoom.coerceIn(LiveMeshConfig.ZOOM_MIN, LiveMeshConfig.ZOOM_MAX),
    reverseDirection = reverseDirection,
    bottomShade = bottomShade.coerceIn(LiveMeshConfig.BOTTOM_SHADE_MIN, LiveMeshConfig.BOTTOM_SHADE_MAX),
    textureSize = textureSize.coerceIn(LiveMeshConfig.TEXTURE_SIZE_MIN, LiveMeshConfig.TEXTURE_SIZE_MAX),
    beatReactivity = beatReactivity.coerceIn(LiveMeshConfig.REACTIVITY_MIN, LiveMeshConfig.REACTIVITY_MAX),
    lyricReactivity = lyricReactivity.coerceIn(LiveMeshConfig.REACTIVITY_MIN, LiveMeshConfig.REACTIVITY_MAX),
)

internal const val BEAT_ZOOM_BOOST = 0.06f
internal const val LYRIC_ZOOM_BOOST = 0.04f

// Beats and lyric lines briefly push the layers outward; the reactivity sliders decide how far they move.
internal fun reactiveZoom(
    baseZoom: Float,
    beat: Float,
    lyric: Float,
    beatReactivity: Float,
    lyricReactivity: Float,
): Float = baseZoom +
    beat.coerceIn(0f, 1f) * BEAT_ZOOM_BOOST * beatReactivity +
    lyric.coerceIn(0f, 1f) * LYRIC_ZOOM_BOOST * lyricReactivity

// Layers turn faster on fast songs; 120 BPM is neutral, and the factor is limited so it never feels frantic.
internal fun autoTempoFactor(bpm: Float?): Float =
    if (bpm == null) 1f else (bpm / 120f).coerceIn(0.5f, 2f)

private const val ADAPTIVE_SLOW_FRAME_MS = 24f
private const val ADAPTIVE_FAST_FRAME_MS = 17f
private const val ADAPTIVE_STEP = 1.5f

// Raises the render-down factor (cheaper frames) when frames run slow and eases back when they recover.
internal fun nextAdaptiveScale(current: Float, avgFrameMs: Float, base: Float): Float = when {
    avgFrameMs > ADAPTIVE_SLOW_FRAME_MS -> (current * ADAPTIVE_STEP).coerceIn(base, LiveMeshConfig.RENDER_SCALE_MAX)
    avgFrameMs < ADAPTIVE_FAST_FRAME_MS && current > base -> (current / ADAPTIVE_STEP).coerceAtLeast(base)
    else -> current.coerceIn(base, LiveMeshConfig.RENDER_SCALE_MAX)
}

// Higher speed shortens the rotation period so the mesh turns faster without changing its shape.
internal fun liveMeshPeriodMillis(baseMillis: Int, speed: Float): Int =
    (baseMillis / speed.coerceAtLeast(LiveMeshConfig.SPEED_MIN)).roundToInt()

internal fun advanceAngle(current: Float, direction: Float, tempo: Float, dtSeconds: Float, periodMs: Int): Float =
    (current + direction * 360f * tempo * dtSeconds * 1000f / periodMs) % 360f

// Player keys keep the original camelCase names; other scopes prepend their prefix to the "LiveMesh" part.
internal class LiveMeshKeys(private val prefix: String) {
    private fun name(suffix: String): String =
        if (prefix.isEmpty()) "liveMesh$suffix" else "${prefix}LiveMesh$suffix"

    val blurStrength: Preferences.Key<Float> = floatPreferencesKey(name("BlurStrength"))
    val saturation: Preferences.Key<Float> = floatPreferencesKey(name("Saturation"))
    val speed: Preferences.Key<Float> = floatPreferencesKey(name("Speed"))
    val darkTint: Preferences.Key<Float> = floatPreferencesKey(name("DarkTint"))
    val renderScale: Preferences.Key<Float> = floatPreferencesKey(name("RenderScale"))
    val layer2Alpha: Preferences.Key<Float> = floatPreferencesKey(name("Layer2Alpha"))
    val layer3Alpha: Preferences.Key<Float> = floatPreferencesKey(name("Layer3Alpha"))
    val zoom: Preferences.Key<Float> = floatPreferencesKey(name("Zoom"))
    val reverseDirection: Preferences.Key<Boolean> = booleanPreferencesKey(name("ReverseDirection"))
    val bottomShade: Preferences.Key<Float> = floatPreferencesKey(name("BottomShade"))
    val textureSize: Preferences.Key<Int> = intPreferencesKey(name("TextureSize"))
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
    blurStrength = this[keys.blurStrength] ?: defaults.blurStrength,
    saturation = this[keys.saturation] ?: defaults.saturation,
    speed = this[keys.speed] ?: defaults.speed,
    darkTint = this[keys.darkTint] ?: defaults.darkTint,
    renderScale = this[keys.renderScale] ?: defaults.renderScale,
    layer2Alpha = this[keys.layer2Alpha] ?: defaults.layer2Alpha,
    layer3Alpha = this[keys.layer3Alpha] ?: defaults.layer3Alpha,
    zoom = this[keys.zoom] ?: defaults.zoom,
    reverseDirection = this[keys.reverseDirection] ?: defaults.reverseDirection,
    bottomShade = this[keys.bottomShade] ?: defaults.bottomShade,
    textureSize = this[keys.textureSize] ?: defaults.textureSize,
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
    val adaptiveQuality: Boolean,
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
        adaptiveQuality = playerLive,
    )
}

@Composable
internal fun rememberLiveMeshStore(keys: LiveMeshKeys, defaults: LiveMeshConfig): LiveMeshStore {
    val blurStrength by rememberPreference(keys.blurStrength, defaultValue = defaults.blurStrength)
    val saturation by rememberPreference(keys.saturation, defaultValue = defaults.saturation)
    val speed by rememberPreference(keys.speed, defaultValue = defaults.speed)
    val darkTint by rememberPreference(keys.darkTint, defaultValue = defaults.darkTint)
    val renderScale by rememberPreference(keys.renderScale, defaultValue = defaults.renderScale)
    val layer2Alpha by rememberPreference(keys.layer2Alpha, defaultValue = defaults.layer2Alpha)
    val layer3Alpha by rememberPreference(keys.layer3Alpha, defaultValue = defaults.layer3Alpha)
    val zoom by rememberPreference(keys.zoom, defaultValue = defaults.zoom)
    val reverseDirection by rememberPreference(keys.reverseDirection, defaultValue = defaults.reverseDirection)
    val bottomShade by rememberPreference(keys.bottomShade, defaultValue = defaults.bottomShade)
    val textureSize by rememberPreference(keys.textureSize, defaultValue = defaults.textureSize)
    val beatReactivity by rememberPreference(keys.beatReactivity, defaultValue = defaults.beatReactivity)
    val lyricReactivity by rememberPreference(keys.lyricReactivity, defaultValue = defaults.lyricReactivity)

    val config = LiveMeshConfig(
        blurStrength = blurStrength,
        saturation = saturation,
        speed = speed,
        darkTint = darkTint,
        renderScale = renderScale,
        layer2Alpha = layer2Alpha,
        layer3Alpha = layer3Alpha,
        zoom = zoom,
        reverseDirection = reverseDirection,
        bottomShade = bottomShade,
        textureSize = textureSize,
        beatReactivity = beatReactivity,
        lyricReactivity = lyricReactivity,
    ).clamped()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val save: (LiveMeshConfig) -> Unit = { value ->
        val clampedValue = value.clamped()
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[keys.blurStrength] = clampedValue.blurStrength
                prefs[keys.saturation] = clampedValue.saturation
                prefs[keys.speed] = clampedValue.speed
                prefs[keys.darkTint] = clampedValue.darkTint
                prefs[keys.renderScale] = clampedValue.renderScale
                prefs[keys.layer2Alpha] = clampedValue.layer2Alpha
                prefs[keys.layer3Alpha] = clampedValue.layer3Alpha
                prefs[keys.zoom] = clampedValue.zoom
                prefs[keys.reverseDirection] = clampedValue.reverseDirection
                prefs[keys.bottomShade] = clampedValue.bottomShade
                prefs[keys.textureSize] = clampedValue.textureSize
                prefs[keys.beatReactivity] = clampedValue.beatReactivity
                prefs[keys.lyricReactivity] = clampedValue.lyricReactivity
            }
        }
    }
    return LiveMeshStore(config, save)
}
