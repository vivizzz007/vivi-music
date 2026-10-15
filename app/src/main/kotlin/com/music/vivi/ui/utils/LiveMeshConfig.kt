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

internal const val BEAT_ZOOM_BOOST = 0.08f
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

// Higher speed shortens the rotation period so the mesh turns faster without changing its shape.
internal fun liveMeshPeriodMillis(baseMillis: Int, speed: Float): Int =
    (baseMillis / speed.coerceAtLeast(LiveMeshConfig.SPEED_MIN)).roundToInt()

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

@Composable
internal fun rememberMiniLiveMeshStore(): LiveMeshStore {
    val separate by rememberPreference(LiveMeshSeparateMiniKey, defaultValue = false)
    val playerStore = rememberLiveMeshStore(PlayerLiveMeshKeys, LiveMeshConfig.Default)
    val miniStore = rememberLiveMeshStore(MiniLiveMeshKeys, LiveMeshConfig.MiniDefault)
    return if (separate) miniStore else playerStore
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
