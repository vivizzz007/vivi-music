package com.music.vivi.ui.screens.equalizer.axion

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.music.vivi.R
import com.music.vivi.ui.component.Material3SettingsGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.utils.backToMain
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AxionEqScreen(
    onBackClick: () -> Unit,
    viewModel: AxionEqViewModel = hiltViewModel()
) {
    val enabled by viewModel.enabled.collectAsState()
    val bandGains by viewModel.bandGains.collectAsState()
    val mode by viewModel.mode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vivi_equalizer)) },
                navigationIcon = {
                    com.music.vivi.ui.component.IconButton(
                        onClick = onBackClick,
                        onLongClick = {}
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Material3SettingsGroup(
                items = listOf(
                    Material3SettingsItem(
                        icon = androidx.compose.ui.res.painterResource(R.drawable.equalizer),
                        title = { Text(stringResource(R.string.eq_enable_title)) },
                        description = { Text(stringResource(R.string.eq_enable_summary)) },
                        trailingContent = {
                            Switch(
                                checked = enabled,
                                onCheckedChange = { viewModel.setEnabled(it) },
                                thumbContent = {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(
                                            id = if (enabled) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { viewModel.setEnabled(!enabled) }
                    )
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                ToggleButton(
                    checked = mode == 0,
                    onCheckedChange = { viewModel.setMode(0) },
                    modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                ) {
                    Text(stringResource(R.string.eq_simple))
                }
                ToggleButton(
                    checked = mode == 1,
                    onCheckedChange = { viewModel.setMode(1) },
                    modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                ) {
                    Text(stringResource(R.string.eq_advanced))
                }
            }

            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    fadeIn().togetherWith(fadeOut()).using(SizeTransform(clip = false))
                },
                label = "eqMode",
            ) { currentMode ->
                when (currentMode) {
                    0 -> SimpleEqMode(bandGains, enabled, viewModel)
                    else -> AdvancedEqMode(
                        bandGains = bandGains,
                        enabled = enabled,
                        onBandChange = { band, value ->
                            viewModel.setBandGain(band, value)
                        },
                        onReset = {
                            viewModel.reset()
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun SimpleEqMode(
    bandGains: FloatArray,
    enabled: Boolean,
    viewModel: AxionEqViewModel,
) {
    // Ported logic from AxionFx SimpleEqMode
    var bass by remember { mutableFloatStateOf(0f) }
    var mid by remember { mutableFloatStateOf(0f) }
    var treble by remember { mutableFloatStateOf(0f) }

    fun syncFromBands() {
        bass = (bandGains[0] + bandGains[1] + bandGains[2]) / 3f / 50f
        mid = (bandGains[3] + bandGains[4] + bandGains[5] + bandGains[6]) / 4f / 50f
        treble = (bandGains[7] + bandGains[8] + bandGains[9]) / 3f / 50f
    }

    fun applyTriangle() {
        val bv = (bass * 50f).coerceIn(-600f, 600f)
        val mv = (mid * 50f).coerceIn(-600f, 600f)
        val tv = (treble * 50f).coerceIn(-600f, 600f)
        val newGains = FloatArray(10)
        
        // Smoother, more organic transition across the spectrum
        newGains[0] = bv * 1.1f
        newGains[1] = bv * 1.0f
        newGains[2] = bv * 0.7f + mv * 0.3f
        newGains[3] = bv * 0.2f + mv * 0.8f
        newGains[4] = mv * 1.0f
        newGains[5] = mv * 1.0f
        newGains[6] = mv * 0.8f + tv * 0.2f
        newGains[7] = mv * 0.3f + tv * 0.7f
        newGains[8] = tv * 1.0f
        newGains[9] = tv * 1.15f // Extended high-end sparkle
        
        viewModel.setBandsGains(newGains)
    }

    LaunchedEffect(bandGains) {
        syncFromBands()
    }

    val viviPresets = listOf(
        R.string.eq_preset_flat to floatArrayOf(150f, 100f, 50f, 0f, -20f, 0f, 80f, 150f, 200f, 150f), // Vivi Signature (Balanced)
        R.string.eq_preset_bass_boost to floatArrayOf(500f, 400f, 250f, 100f, 0f, -50f, 0f, 100f, 200f, 300f),
        R.string.eq_preset_pure_clarity to floatArrayOf(-100f, -50f, 0f, 50f, 150f, 250f, 300f, 250f, 150f, 100f),
        R.string.eq_preset_soft_bass to floatArrayOf(200f, 180f, 140f, 80f, 30f, 20f, 60f, 90f, 110f, 130f),
        R.string.eq_preset_electronic to floatArrayOf(350f, 280f, 120f, -50f, -150f, 50f, 180f, 300f, 400f, 500f),
        R.string.eq_preset_rock to floatArrayOf(300f, 220f, 150f, 50f, -100f, 120f, 200f, 250f, 320f, 380f),
        R.string.eq_preset_pop to floatArrayOf(-150f, 0f, 100f, 180f, 250f, 220f, 150f, 80f, -50f, -120f),
        R.string.eq_preset_jazz to floatArrayOf(150f, 100f, 60f, 140f, 200f, 180f, 120f, 180f, 220f, 200f),
        R.string.eq_preset_voice to floatArrayOf(-250f, -150f, 0f, 200f, 400f, 380f, 200f, 120f, 0f, -120f),
    )

    val dolbyPresets = listOf(
        R.string.eq_preset_dolby_open to floatArrayOf(150f, 180f, 220f, 180f, 160f, 210f, 250f, 280f, 180f, 80f),
        R.string.eq_preset_dolby_rich to floatArrayOf(100f, 160f, 200f, 220f, 280f, 260f, 240f, 200f, 150f, 50f),
        R.string.eq_preset_dolby_focused to floatArrayOf(-300f, -50f, 130f, 180f, 220f, 120f, 140f, 100f, -50f, -300f),
    )

    val diracPresets = listOf(
        R.string.eq_preset_dirac_music to floatArrayOf(200f, 140f, 80f, 0f, 30f, 80f, 140f, 200f, 280f, 350f),
        R.string.eq_preset_dirac_movie to floatArrayOf(300f, 250f, 150f, 0f, 70f, 120f, 180f, 250f, 320f, 400f),
        R.string.eq_preset_dirac_game to floatArrayOf(150f, 250f, 200f, 0f, 80f, 150f, 300f, 450f, 400f, 280f),
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp), // Increased gap between curve and buttons
    ) {
        CircularEqControl(
            bass = bass, mid = mid, treble = treble,
            enabled = enabled,
            onBassChange = { bass = it; applyTriangle() },
            onMidChange = { mid = it; applyTriangle() },
            onTrebleChange = { treble = it; applyTriangle() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .aspectRatio(1f),
        )

        viviPresets.chunked(4).forEach { chunk ->
            PresetSection(
                title = if (viviPresets.first() in chunk) stringResource(R.string.eq_label_vivi) else "",
                presets = chunk,
                enabled = enabled,
                viewModel = viewModel,
                bandGains = bandGains
            )
        }
        PresetSection(stringResource(R.string.eq_label_dolby), dolbyPresets, enabled, viewModel, bandGains)
        PresetSection(stringResource(R.string.eq_label_dirac), diracPresets, enabled, viewModel, bandGains)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PresetSection(
    title: String,
    presets: List<Pair<Int, FloatArray>>,
    enabled: Boolean,
    viewModel: AxionEqViewModel,
    bandGains: FloatArray
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            presets.forEachIndexed { index, (nameRes, bands) ->
                val isSelected = remember(bandGains) {
                    bandGains.size == bands.size && 
                    abs((bandGains[0] + bandGains[1]) - (bands[0] + bands[1])) < 10f &&
                    abs((bandGains[9] + bandGains[8]) - (bands[9] + bands[8])) < 10f
                }

                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = { if (enabled) viewModel.setBandsGains(bands) },
                    enabled = enabled,
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        presets.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = stringResource(nameRes),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvancedEqMode(
    bandGains: FloatArray,
    enabled: Boolean,
    onBandChange: (Int, Float) -> Unit,
    onReset: () -> Unit,
) {
    val bandLabels = arrayOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (band in 0..9) {
                    EqBandSlider(
                        label = bandLabels[band],
                        value = bandGains[band],
                        enabled = enabled,
                        onValueChange = { onBandChange(band, it) },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            FilledTonalButton(onClick = onReset) {
                Icon(Icons.Rounded.Replay, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.eq_reset))
            }
        }
    }
}

@Composable
private fun EqBandSlider(
    label: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier.width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "%.1f".format(value / 10f),
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier.height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = -600f..600f,
                enabled = enabled,
                modifier = Modifier
                    .width(200.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            constraints.copy(
                                minWidth = constraints.minHeight,
                                maxWidth = constraints.maxHeight,
                            )
                        )
                        layout(placeable.height, placeable.width) {
                            placeable.place(
                                -placeable.width / 2 + placeable.height / 2,
                                placeable.width / 2 - placeable.height / 2
                            )
                        }
                    }
                    .graphicsLayer { rotationZ = -90f },
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
