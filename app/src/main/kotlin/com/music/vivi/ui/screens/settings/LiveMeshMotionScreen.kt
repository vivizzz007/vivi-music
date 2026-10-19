/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LiveMeshSectionHeader
import com.music.vivi.ui.component.LiveMeshSurface
import com.music.vivi.ui.component.LiveMeshValueSlider
import com.music.vivi.ui.utils.LiveMeshMotion
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.rememberLiveMeshMotion

// Shared by the settings screen and the preview, so both expose exactly the same motion controls.
@Composable
internal fun LiveMeshMotionSections(
    motion: LiveMeshMotion,
    onChange: (LiveMeshMotion) -> Unit,
    onChangeFinished: (() -> Unit)?,
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
    val store = rememberLiveMeshMotion()
    var draft by remember(store.motion) { mutableStateOf(store.motion) }

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { navController.navigate("settings/appearance/live_mesh_preview") },
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text(stringResource(R.string.live_mesh_motion_open_preview))
            }
            FilledTonalButton(
                onClick = {
                    draft = LiveMeshMotion.Default
                    store.save(LiveMeshMotion.Default)
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.refresh),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text(stringResource(R.string.reset))
            }
        }

        LiveMeshMotionSections(
            motion = draft,
            onChange = { draft = it },
            onChangeFinished = { store.save(draft) },
        )

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
