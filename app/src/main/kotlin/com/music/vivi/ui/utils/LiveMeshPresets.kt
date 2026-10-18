/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import androidx.annotation.StringRes
import com.music.vivi.R

internal data class LiveMeshPreset(
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val config: LiveMeshConfig,
)

// The look the other presets were tuned against; kept separate so they do not change with the default.
private val ClassicLook = LiveMeshConfig(
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

internal val LiveMeshPresets: List<LiveMeshPreset> = listOf(
    LiveMeshPreset(
        nameRes = R.string.live_mesh_preset_original,
        descriptionRes = R.string.live_mesh_preset_original_desc,
        config = LiveMeshConfig.Default,
    ),
    LiveMeshPreset(
        nameRes = R.string.live_mesh_preset_calm,
        descriptionRes = R.string.live_mesh_preset_calm_desc,
        config = ClassicLook.copy(
            blurStrength = 1.4f,
            saturation = 1.2f,
            speed = 0.5f,
            darkTint = 0.3f,
            layer2Alpha = 0.4f,
            layer3Alpha = 0.3f,
            zoom = 1.6f,
            beatReactivity = 0.2f,
            lyricReactivity = 0.2f,
        ),
    ),
    LiveMeshPreset(
        nameRes = R.string.live_mesh_preset_dreamy,
        descriptionRes = R.string.live_mesh_preset_dreamy_desc,
        config = ClassicLook.copy(
            blurStrength = 1.8f,
            saturation = 1.5f,
            speed = 0.7f,
            darkTint = 0.25f,
            layer2Alpha = 0.5f,
            layer3Alpha = 0.4f,
            zoom = 1.8f,
            reverseDirection = true,
            beatReactivity = 0.3f,
            lyricReactivity = 0.5f,
        ),
    ),
    LiveMeshPreset(
        nameRes = R.string.live_mesh_preset_crisp,
        descriptionRes = R.string.live_mesh_preset_crisp_desc,
        config = ClassicLook.copy(
            blurStrength = 1.2f,
            saturation = 1.6f,
            renderScale = 1f,
            textureSize = 256,
            beatReactivity = 0.3f,
            lyricReactivity = 0.4f,
        ),
    ),
    LiveMeshPreset(
        nameRes = R.string.live_mesh_preset_low_power,
        descriptionRes = R.string.live_mesh_preset_low_power_desc,
        config = ClassicLook.copy(
            speed = 0.5f,
            renderScale = 8f,
            textureSize = 64,
            beatReactivity = 0f,
            lyricReactivity = 0f,
        ),
    ),
    LiveMeshPreset(
        nameRes = R.string.live_mesh_preset_classic,
        descriptionRes = R.string.live_mesh_preset_classic_desc,
        config = ClassicLook,
    ),
)

// The first load also flips isActive; skipping it keeps the card from popping every time the screen opens.
internal fun shouldPopOnActivation(wasActive: Boolean, isActive: Boolean, wasLoaded: Boolean): Boolean =
    isActive && !wasActive && wasLoaded

// -1 when nothing is loaded yet or the saved settings match no preset (custom).
internal fun activePresetIndex(config: LiveMeshConfig?): Int =
    if (config == null) -1 else LiveMeshPresets.indexOfFirst { it.config == config }
