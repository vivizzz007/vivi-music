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
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LiveMeshBackground
import com.music.vivi.ui.component.rememberLiveMeshClockDriver
import com.music.vivi.ui.component.LiveMeshSectionHeader
import com.music.vivi.ui.component.LiveMeshSurface
import com.music.vivi.ui.component.LiveMeshValueSlider
import com.music.vivi.ui.utils.LiveMeshConfig
import com.music.vivi.ui.utils.LiveMeshSimulation
import com.music.vivi.ui.utils.PlayerLiveMeshKeys
import com.music.vivi.ui.utils.SIMULATED_BPM_MAX
import com.music.vivi.ui.utils.SIMULATED_BPM_MIN
import com.music.vivi.ui.utils.SIMULATED_TEMPO_MAX
import com.music.vivi.ui.utils.SIMULATED_TEMPO_MIN
import com.music.vivi.ui.utils.artworkDimFactor
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.beatSpeedFactor
import com.music.vivi.ui.utils.rememberLiveMeshMotion
import com.music.vivi.ui.utils.rememberLiveMeshStore
import com.music.vivi.ui.utils.simulatedFrame

private val PreviewStageShape = RoundedCornerShape(28.dp)

@Composable
fun LiveMeshPreviewScreen(navController: NavController) {
    val config = rememberLiveMeshStore(PlayerLiveMeshKeys, LiveMeshConfig.Default).config
    val store = rememberLiveMeshMotion()
    var draft by remember(store.motion) { mutableStateOf(store.motion) }
    val motion = draft
    val simulation = remember { LiveMeshSimulation() }
    val clock = rememberLiveMeshClockDriver(
        config = config,
        playing = simulation.playing,
        motion = motion,
    ) {
        simulatedFrame(
            bpm = simulation.bpm,
            tempo = simulation.tempo,
            lyricTempo = simulation.lyricTempo,
            nowMs = System.nanoTime() / 1_000_000L,
        )
    }
    val dim = artworkDimFactor(simulation.artworkHighlight, motion.artworkDimStrength)

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.live_mesh_preview_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(PreviewStageShape)
        ) {
            LiveMeshBackground(
                config = config,
                artworkDim = dim,
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
                    boost = motion.beatSpeedBoost,
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
            }
        }

        LiveMeshSectionHeader(stringResource(R.string.live_mesh_preview_section_artwork))
        LiveMeshSurface(modifier = Modifier.fillMaxWidth()) {
            LiveMeshValueSlider(
                label = stringResource(R.string.live_mesh_preview_brightness),
                description = stringResource(R.string.live_mesh_preview_brightness_desc),
                valueText = "%d%%".format((simulation.artworkHighlight * 100f).toInt()),
                value = simulation.artworkHighlight,
                range = 0f..1f,
                onValueChange = { simulation.artworkHighlight = it },
            )
        }

        LiveMeshSectionHeader(stringResource(R.string.live_mesh_preview_section_motion))
        LiveMeshMotionSections(
            motion = draft,
            onChange = { draft = it },
            onChangeFinished = null,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { store.save(draft) },
                enabled = draft != store.motion,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.live_mesh_preview_save))
            }
            FilledTonalButton(
                onClick = { draft = store.motion },
                enabled = draft != store.motion,
            ) {
                Text(stringResource(R.string.live_mesh_preview_discard))
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.live_mesh_preview_title)) },
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
