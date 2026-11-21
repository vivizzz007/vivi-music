/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LiveMeshBackground
import com.music.vivi.ui.component.LiveMeshSectionHeader
import com.music.vivi.ui.component.LiveMeshSurface
import com.music.vivi.ui.utils.LiveMeshConfig
import com.music.vivi.ui.utils.LiveMeshPresets
import com.music.vivi.ui.utils.activePresetIndex
import com.music.vivi.ui.utils.shouldPopOnActivation
import com.music.vivi.ui.utils.PlayerLiveMeshKeys
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.rememberLiveMeshStore
import com.music.vivi.ui.utils.toLiveMeshConfig
import com.music.vivi.utils.dataStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val PresetPreviewShape = RoundedCornerShape(24.dp)

@Composable
fun LiveMeshPresetsScreen(navController: NavController) {
    val context = LocalContext.current
    // Read straight from DataStore so the active preset is known before the first frame; the default would flash "Original".
    val savedConfig by produceState<LiveMeshConfig?>(initialValue = null, context) {
        context.dataStore.data
            .map { it.toLiveMeshConfig(PlayerLiveMeshKeys, LiveMeshConfig.Default) }
            .distinctUntilChanged()
            .collect { value = it }
    }
    val isLoaded = savedConfig != null
    val activeIndex = activePresetIndex(savedConfig)
    val playerLiveMesh = rememberLiveMeshStore(PlayerLiveMeshKeys, LiveMeshConfig.Default)
    var pendingIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val pendingPreset = pendingIndex?.let { LiveMeshPresets.getOrNull(it) }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.live_mesh_presets_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LiveMeshSectionHeader(stringResource(R.string.live_mesh_presets))

        LiveMeshPresets.forEachIndexed { index, preset ->
            LivePresetCard(
                name = stringResource(preset.nameRes),
                description = stringResource(preset.descriptionRes),
                config = preset.config,
                isActive = index == activeIndex,
                isLoaded = isLoaded,
                onApply = { pendingIndex = index },
            )
        }

        Spacer(Modifier.height(16.dp))
    }

    pendingPreset?.let { preset ->
        DefaultDialog(
            onDismiss = { pendingIndex = null },
            icon = { Icon(painterResource(R.drawable.palette), contentDescription = null) },
            title = { Text(stringResource(R.string.live_mesh_apply_title)) },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    TextButton(onClick = { pendingIndex = null }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Button(onClick = {
                        playerLiveMesh.save(preset.config)
                        pendingIndex = null
                    }) {
                        Text(stringResource(R.string.live_mesh_apply))
                    }
                }
            },
        ) {
            Text(
                text = stringResource(R.string.live_mesh_apply_message, stringResource(preset.nameRes)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.live_mesh_presets)) },
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

@Composable
private fun LivePresetCard(
    name: String,
    description: String,
    config: LiveMeshConfig,
    isActive: Boolean,
    isLoaded: Boolean,
    onApply: () -> Unit,
) {
    // Small spring "pop" when a preset becomes active, so applying one feels acknowledged.
    val pop = remember { Animatable(1f) }
    var wasActive by remember { mutableStateOf(isActive) }
    var wasLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(isActive, isLoaded) {
        if (shouldPopOnActivation(wasActive, isActive, wasLoaded)) {
            pop.snapTo(0.96f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        wasActive = isActive
        wasLoaded = isLoaded
    }

    LiveMeshSurface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
        highlighted = isActive,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(PresetPreviewShape)
            ) {
                LiveMeshBackground(config = config)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isLoaded) AnimatedContent(
                    targetState = isActive,
                    transitionSpec = {
                        (fadeIn(spring()) + scaleIn(initialScale = 0.9f)) togetherWith
                            (fadeOut(spring()) + scaleOut(targetScale = 0.9f))
                    },
                    label = "livePresetButton",
                ) { active ->
                    if (active) {
                        FilledTonalButton(onClick = {}, enabled = false) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(stringResource(R.string.live_mesh_preset_active))
                        }
                    } else {
                        FilledTonalButton(onClick = onApply) {
                            Text(stringResource(R.string.live_mesh_apply))
                        }
                    }
                }
            }
        }
    }
}
