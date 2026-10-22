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
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.launch

// Shared by the player and mini player: motion is one feel for the whole app, while look stays per surface.
internal data class LiveMeshMotion(
    val beatSpeedBoost: Float,
    val pulseRise: Float,
    val pulseFall: Float,
    val tempoRise: Float,
    val tempoFall: Float,
    val artworkDimStrength: Float,
) {
    companion object {
        const val BEAT_SPEED_BOOST_MIN = 0f
        const val BEAT_SPEED_BOOST_MAX = 3f
        const val RATE_MIN = 0.5f
        const val PULSE_RISE_MAX = 80f
        const val PULSE_FALL_MAX = 20f
        const val TEMPO_RATE_MAX = 10f
        const val DIM_STRENGTH_MIN = 0f
        const val DIM_STRENGTH_MAX = 1f

        val Default = LiveMeshMotion(
            beatSpeedBoost = BEAT_SPEED_BOOST,
            pulseRise = PULSE_RISE_PER_SECOND,
            pulseFall = PULSE_FALL_PER_SECOND,
            tempoRise = TEMPO_RISE_PER_SECOND,
            tempoFall = TEMPO_FALL_PER_SECOND,
            artworkDimStrength = ARTWORK_DIM_STRENGTH,
        )
    }
}

internal fun LiveMeshMotion.clamped(): LiveMeshMotion = LiveMeshMotion(
    beatSpeedBoost = beatSpeedBoost.coerceIn(LiveMeshMotion.BEAT_SPEED_BOOST_MIN, LiveMeshMotion.BEAT_SPEED_BOOST_MAX),
    pulseRise = pulseRise.coerceIn(LiveMeshMotion.RATE_MIN, LiveMeshMotion.PULSE_RISE_MAX),
    pulseFall = pulseFall.coerceIn(LiveMeshMotion.RATE_MIN, LiveMeshMotion.PULSE_FALL_MAX),
    tempoRise = tempoRise.coerceIn(LiveMeshMotion.RATE_MIN, LiveMeshMotion.TEMPO_RATE_MAX),
    tempoFall = tempoFall.coerceIn(LiveMeshMotion.RATE_MIN, LiveMeshMotion.TEMPO_RATE_MAX),
    artworkDimStrength = artworkDimStrength.coerceIn(LiveMeshMotion.DIM_STRENGTH_MIN, LiveMeshMotion.DIM_STRENGTH_MAX),
)

internal object LiveMeshMotionKeys {
    val beatSpeedBoost: Preferences.Key<Float> = floatPreferencesKey("liveMeshMotionBeatSpeedBoost")
    val pulseRise: Preferences.Key<Float> = floatPreferencesKey("liveMeshMotionPulseRise")
    val pulseFall: Preferences.Key<Float> = floatPreferencesKey("liveMeshMotionPulseFall")
    val tempoRise: Preferences.Key<Float> = floatPreferencesKey("liveMeshMotionTempoRise")
    val tempoFall: Preferences.Key<Float> = floatPreferencesKey("liveMeshMotionTempoFall")
    val artworkDimStrength: Preferences.Key<Float> = floatPreferencesKey("liveMeshMotionArtworkDimStrength")
}

internal fun Preferences.toLiveMeshMotion(defaults: LiveMeshMotion = LiveMeshMotion.Default): LiveMeshMotion = LiveMeshMotion(
    beatSpeedBoost = this[LiveMeshMotionKeys.beatSpeedBoost] ?: defaults.beatSpeedBoost,
    pulseRise = this[LiveMeshMotionKeys.pulseRise] ?: defaults.pulseRise,
    pulseFall = this[LiveMeshMotionKeys.pulseFall] ?: defaults.pulseFall,
    tempoRise = this[LiveMeshMotionKeys.tempoRise] ?: defaults.tempoRise,
    tempoFall = this[LiveMeshMotionKeys.tempoFall] ?: defaults.tempoFall,
    artworkDimStrength = this[LiveMeshMotionKeys.artworkDimStrength] ?: defaults.artworkDimStrength,
).clamped()

internal class LiveMeshMotionStore(
    val motion: LiveMeshMotion,
    val save: (LiveMeshMotion) -> Unit,
)

@Composable
internal fun rememberLiveMeshMotion(): LiveMeshMotionStore {
    val defaults = LiveMeshMotion.Default
    val beatSpeedBoost by rememberPreference(LiveMeshMotionKeys.beatSpeedBoost, defaultValue = defaults.beatSpeedBoost)
    val pulseRise by rememberPreference(LiveMeshMotionKeys.pulseRise, defaultValue = defaults.pulseRise)
    val pulseFall by rememberPreference(LiveMeshMotionKeys.pulseFall, defaultValue = defaults.pulseFall)
    val tempoRise by rememberPreference(LiveMeshMotionKeys.tempoRise, defaultValue = defaults.tempoRise)
    val tempoFall by rememberPreference(LiveMeshMotionKeys.tempoFall, defaultValue = defaults.tempoFall)
    val artworkDimStrength by rememberPreference(
        LiveMeshMotionKeys.artworkDimStrength,
        defaultValue = defaults.artworkDimStrength,
    )

    val motion = LiveMeshMotion(
        beatSpeedBoost = beatSpeedBoost,
        pulseRise = pulseRise,
        pulseFall = pulseFall,
        tempoRise = tempoRise,
        tempoFall = tempoFall,
        artworkDimStrength = artworkDimStrength,
    ).clamped()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val save: (LiveMeshMotion) -> Unit = { value ->
        val clampedValue = value.clamped()
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[LiveMeshMotionKeys.beatSpeedBoost] = clampedValue.beatSpeedBoost
                prefs[LiveMeshMotionKeys.pulseRise] = clampedValue.pulseRise
                prefs[LiveMeshMotionKeys.pulseFall] = clampedValue.pulseFall
                prefs[LiveMeshMotionKeys.tempoRise] = clampedValue.tempoRise
                prefs[LiveMeshMotionKeys.tempoFall] = clampedValue.tempoFall
                prefs[LiveMeshMotionKeys.artworkDimStrength] = clampedValue.artworkDimStrength
            }
        }
    }
    return LiveMeshMotionStore(motion, save)
}
