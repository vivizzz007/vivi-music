/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LiveMeshBackground
import com.music.vivi.ui.component.LiveMeshSectionHeader
import com.music.vivi.ui.component.LiveMeshSurface
import com.music.vivi.ui.component.LiveMeshValueSlider
import com.music.vivi.ui.component.rememberLiveMeshClockDriver
import com.music.vivi.ui.utils.LiveMeshConfig
import com.music.vivi.ui.utils.LiveMeshMotion
import com.music.vivi.ui.utils.LiveMeshSimulation
import com.music.vivi.ui.utils.PlayerLiveMeshKeys
import com.music.vivi.ui.utils.SIMULATED_BPM_MAX
import com.music.vivi.ui.utils.SIMULATED_BPM_MIN
import com.music.vivi.ui.utils.SIMULATED_TEMPO_MAX
import com.music.vivi.ui.utils.SIMULATED_TEMPO_MIN
import com.music.vivi.ui.utils.artworkDimFactor
import com.music.vivi.ui.utils.rememberArtworkPalette
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.beatSpeedFactor
import com.music.vivi.ui.utils.rememberLiveMeshMotion
import com.music.vivi.ui.utils.rememberLiveMeshStore
import com.music.vivi.ui.utils.simulatedFrame

private val MotionStageShape = RoundedCornerShape(28.dp)

// Every motion control lives here once, so the stage and the sliders never drift apart.
@Composable
internal fun LiveMeshMotionSections(
    motion: LiveMeshMotion,
    onChange: (LiveMeshMotion) -> Unit,
    onChangeFinished: () -> Unit,
) {
    LiveMeshSectionHeader(stringResource(R.string.live_mesh_motion_section_beat))
    LiveMeshSurface(modifier = Modifier.fillMaxWidth()) {
        LiveMeshValueSlider(
            label = stringResource(R.string.live_mesh_motion_beat_boost),
            description = stringResource(R.string.live_mesh_motion_beat_boost_desc),
            valueText = "%.2f".format(motion.beatSpeedBoost),
            value = motion.beatSpeedBoost,
            range = LiveMeshMotion.BEAT_SPEED_BOOST_MIN..LiveMeshMotion.BEAT_SPEED_BOOST_MAX,
            onValueChange = { onChange(motion.copy(beatSpeedBoost = it)) },
            onValueChangeFinished = onChangeFinished,
        )
    }

    LiveMeshSectionHeader(stringResource(R.string.live_mesh_motion_section_pulse))
    LiveMeshSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LiveMeshValueSlider(
                label = stringResource(R.string.live_mesh_motion_pulse_rise),
                description = stringResource(R.string.live_mesh_motion_pulse_rise_desc),
                valueText = "%.1f".format(motion.pulseRise),
                value = motion.pulseRise,
                range = LiveMeshMotion.RATE_MIN..LiveMeshMotion.PULSE_RISE_MAX,
                onValueChange = { onChange(motion.copy(pulseRise = it)) },
                onValueChangeFinished = onChangeFinished,
            )
            LiveMeshValueSlider(
                label = stringResource(R.string.live_mesh_motion_pulse_fall),
                description = stringResource(R.string.live_mesh_motion_pulse_fall_desc),
                valueText = "%.1f".format(motion.pulseFall),
                value = motion.pulseFall,
                range = LiveMeshMotion.RATE_MIN..LiveMeshMotion.PULSE_FALL_MAX,
                onValueChange = { onChange(motion.copy(pulseFall = it)) },
                onValueChangeFinished = onChangeFinished,
            )
        }
    }

    LiveMeshSectionHeader(stringResource(R.string.live_mesh_motion_section_tempo))
    LiveMeshSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LiveMeshValueSlider(
                label = stringResource(R.string.live_mesh_motion_tempo_rise),
                description = stringResource(R.string.live_mesh_motion_tempo_rise_desc),
                valueText = "%.2f".format(motion.tempoRise),
                value = motion.tempoRise,
                range = LiveMeshMotion.RATE_MIN..LiveMeshMotion.TEMPO_RATE_MAX,
                onValueChange = { onChange(motion.copy(tempoRise = it)) },
                onValueChangeFinished = onChangeFinished,
            )
            LiveMeshValueSlider(
                label = stringResource(R.string.live_mesh_motion_tempo_fall),
                description = stringResource(R.string.live_mesh_motion_tempo_fall_desc),
                valueText = "%.2f".format(motion.tempoFall),
                value = motion.tempoFall,
                range = LiveMeshMotion.RATE_MIN..LiveMeshMotion.TEMPO_RATE_MAX,
                onValueChange = { onChange(motion.copy(tempoFall = it)) },
                onValueChangeFinished = onChangeFinished,
            )
        }
    }

    LiveMeshSectionHeader(stringResource(R.string.live_mesh_motion_section_artwork))
    LiveMeshSurface(modifier = Modifier.fillMaxWidth()) {
        LiveMeshValueSlider(
            label = stringResource(R.string.live_mesh_motion_dim),
            description = stringResource(R.string.live_mesh_motion_dim_desc),
            valueText = "%d%%".format((motion.artworkDimStrength * 100f).toInt()),
            value = motion.artworkDimStrength,
            range = LiveMeshMotion.DIM_STRENGTH_MIN..LiveMeshMotion.DIM_STRENGTH_MAX,
            onValueChange = { onChange(motion.copy(artworkDimStrength = it)) },
            onValueChangeFinished = onChangeFinished,
        )
    }
}

@Composable
fun LiveMeshMotionScreen(navController: NavController) {
    val config = rememberLiveMeshStore(PlayerLiveMeshKeys, LiveMeshConfig.Default).config
    val store = rememberLiveMeshMotion()
    var draft by remember(store.motion) { mutableStateOf(store.motion) }
    val simulation = remember { LiveMeshSimulation() }
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState()
        ?: remember { mutableStateOf<MediaMetadata?>(null) }
    val palette = rememberArtworkPalette(mediaMetadata?.thumbnailUrl)
    val clock = rememberLiveMeshClockDriver(
        config = config,
        playing = simulation.playing,
        motion = draft,
    ) {
        simulatedFrame(
            bpm = simulation.bpm,
            tempo = simulation.tempo,
            lyricTempo = simulation.lyricTempo,
            nowMs = System.nanoTime() / 1_000_000L,
        )
    }
    val dim = artworkDimFactor(simulation.artworkHighlight, draft.artworkDimStrength)

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.live_mesh_motion_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(MotionStageShape)
        ) {
            LiveMeshBackground(
                config = config,
                artworkDim = dim,
                palette = palette,
                clock = clock,
            )
        }

        LiveMeshSurface(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.live_mesh_preview_readouts),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FilledIconButton(onClick = { simulation.playing = !simulation.playing }) {
                        Icon(
                            painter = painterResource(if (simulation.playing) R.drawable.pause else R.drawable.play),
                            contentDescription = stringResource(
                                if (simulation.playing) R.string.live_mesh_preview_pause else R.string.live_mesh_preview_play,
                            ),
                        )
                    }
                }
                ReadoutRow(
                    label = stringResource(R.string.live_mesh_preview_beat_readout),
                    progress = { clock.beatPulse },
                )
                ReadoutRow(
                    label = stringResource(R.string.live_mesh_preview_lyric_readout),
                    progress = { clock.lyricPulse },
                )
                RotationBoostReadout(
                    beatPulse = { clock.beatPulse },
                    beatReactivity = config.beatReactivity,
                    boost = draft.beatSpeedBoost,
                )
            }
        }

        LiveMeshSectionHeader(stringResource(R.string.live_mesh_preview_section_playback))
        LiveMeshSurface(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LiveMeshValueSlider(
                    label = stringResource(R.string.live_mesh_preview_bpm),
                    description = stringResource(R.string.live_mesh_preview_bpm_desc),
                    valueText = "%.0f".format(simulation.bpm),
                    value = simulation.bpm,
                    range = SIMULATED_BPM_MIN..SIMULATED_BPM_MAX,
                    onValueChange = { simulation.bpm = it },
                )
                LiveMeshValueSlider(
                    label = stringResource(R.string.live_mesh_preview_speed),
                    description = stringResource(R.string.live_mesh_preview_speed_desc),
                    valueText = "%.2fx".format(simulation.tempo),
                    value = simulation.tempo,
                    range = SIMULATED_TEMPO_MIN..SIMULATED_TEMPO_MAX,
                    onValueChange = { simulation.tempo = it },
                )
                LiveMeshValueSlider(
                    label = stringResource(R.string.live_mesh_preview_lyric_tempo),
                    description = stringResource(R.string.live_mesh_preview_lyric_tempo_desc),
                    valueText = "%d%%".format((simulation.lyricTempo * 100f).toInt()),
                    value = simulation.lyricTempo,
                    range = 0f..1f,
                    onValueChange = { simulation.lyricTempo = it },
                )
                LiveMeshValueSlider(
                    label = stringResource(R.string.live_mesh_preview_brightness),
                    description = stringResource(R.string.live_mesh_preview_brightness_desc),
                    valueText = "%d%%".format((simulation.artworkHighlight * 100f).toInt()),
                    value = simulation.artworkHighlight,
                    range = 0f..1f,
                    onValueChange = { simulation.artworkHighlight = it },
                )
            }
        }

        LiveMeshMotionSections(
            motion = draft,
            onChange = { draft = it },
            onChangeFinished = { store.save(draft) },
        )

        FilledTonalButton(
            onClick = {
                draft = LiveMeshMotion.Default
                store.save(LiveMeshMotion.Default)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(R.drawable.refresh),
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(stringResource(R.string.reset))
        }

        Spacer(Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.live_mesh_motion_title)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}

// Progress is passed as a lambda so the per-frame value is read in draw, not by recomposing the whole screen.
@Composable
private fun ReadoutRow(label: String, progress: () -> Float) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { progress().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RotationBoostReadout(beatPulse: () -> Float, beatReactivity: Float, boost: Float) {
    val factor = beatSpeedFactor(beatPulse(), beatReactivity, boost)
    Text(
        text = stringResource(R.string.live_mesh_preview_boost_readout, "%.2f".format(factor)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
