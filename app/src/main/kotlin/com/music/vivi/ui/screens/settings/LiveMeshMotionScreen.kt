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

        LiveMeshSectionHeader(stringResource(R.string.live_mesh_motion_section_beat))
        LiveMeshSurface(modifier = Modifier.fillMaxWidth()) {
            LiveMeshValueSlider(
                label = stringResource(R.string.live_mesh_motion_beat_boost),
                description = stringResource(R.string.live_mesh_motion_beat_boost_desc),
                valueText = "%.2f".format(draft.beatSpeedBoost),
                value = draft.beatSpeedBoost,
                range = LiveMeshMotion.BEAT_SPEED_BOOST_MIN..LiveMeshMotion.BEAT_SPEED_BOOST_MAX,
                onValueChange = { draft = draft.copy(beatSpeedBoost = it) },
                onValueChangeFinished = { store.save(draft) },
            )
        }

        LiveMeshSectionHeader(stringResource(R.string.live_mesh_motion_section_pulse))
        LiveMeshSurface(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LiveMeshValueSlider(
                    label = stringResource(R.string.live_mesh_motion_pulse_rise),
                    description = stringResource(R.string.live_mesh_motion_pulse_rise_desc),
                    valueText = "%.1f".format(draft.pulseRise),
                    value = draft.pulseRise,
                    range = LiveMeshMotion.RATE_MIN..LiveMeshMotion.PULSE_RISE_MAX,
                    onValueChange = { draft = draft.copy(pulseRise = it) },
                    onValueChangeFinished = { store.save(draft) },
                )
                LiveMeshValueSlider(
                    label = stringResource(R.string.live_mesh_motion_pulse_fall),
                    description = stringResource(R.string.live_mesh_motion_pulse_fall_desc),
                    valueText = "%.1f".format(draft.pulseFall),
                    value = draft.pulseFall,
                    range = LiveMeshMotion.RATE_MIN..LiveMeshMotion.PULSE_FALL_MAX,
                    onValueChange = { draft = draft.copy(pulseFall = it) },
                    onValueChangeFinished = { store.save(draft) },
                )
            }
        }

        LiveMeshSectionHeader(stringResource(R.string.live_mesh_motion_section_tempo))
        LiveMeshSurface(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LiveMeshValueSlider(
                    label = stringResource(R.string.live_mesh_motion_tempo_rise),
                    description = stringResource(R.string.live_mesh_motion_tempo_rise_desc),
                    valueText = "%.2f".format(draft.tempoRise),
                    value = draft.tempoRise,
                    range = LiveMeshMotion.RATE_MIN..LiveMeshMotion.TEMPO_RATE_MAX,
                    onValueChange = { draft = draft.copy(tempoRise = it) },
                    onValueChangeFinished = { store.save(draft) },
                )
                LiveMeshValueSlider(
                    label = stringResource(R.string.live_mesh_motion_tempo_fall),
                    description = stringResource(R.string.live_mesh_motion_tempo_fall_desc),
                    valueText = "%.2f".format(draft.tempoFall),
                    value = draft.tempoFall,
                    range = LiveMeshMotion.RATE_MIN..LiveMeshMotion.TEMPO_RATE_MAX,
                    onValueChange = { draft = draft.copy(tempoFall = it) },
                    onValueChangeFinished = { store.save(draft) },
                )
            }
        }

        LiveMeshSectionHeader(stringResource(R.string.live_mesh_motion_section_artwork))
        LiveMeshSurface(modifier = Modifier.fillMaxWidth()) {
            LiveMeshValueSlider(
                label = stringResource(R.string.live_mesh_motion_dim),
                description = stringResource(R.string.live_mesh_motion_dim_desc),
                valueText = "%d%%".format((draft.artworkDimStrength * 100f).toInt()),
                value = draft.artworkDimStrength,
                range = LiveMeshMotion.DIM_STRENGTH_MIN..LiveMeshMotion.DIM_STRENGTH_MAX,
                onValueChange = { draft = draft.copy(artworkDimStrength = it) },
                onValueChangeFinished = { store.save(draft) },
            )
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
