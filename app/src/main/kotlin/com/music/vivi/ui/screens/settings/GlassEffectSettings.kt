package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.LiquidGlassChromaticAberrationKey
import com.music.vivi.constants.LiquidGlassDepthEffectKey
import com.music.vivi.constants.LiquidGlassBlurRadiusKey
import com.music.vivi.constants.LiquidGlassGlobalEnabledKey
import com.music.vivi.constants.LiquidGlassLensAmountKey
import com.music.vivi.constants.LiquidGlassLensHeightKey
import com.music.vivi.constants.LiquidGlassMiniPlayerEnabledKey
import com.music.vivi.constants.LiquidGlassNavBarEnabledKey
import com.music.vivi.constants.LiquidGlassPlayerEnabledKey
import com.music.vivi.constants.LiquidGlassSurfaceOpacityKey
import com.music.vivi.constants.LiquidGlassSurfaceTintColorKey
import com.music.vivi.constants.LiquidGlassTextColorKey
import com.music.vivi.constants.LiquidGlassVibrancyKey
import com.music.vivi.constants.UseFloatingNavBarKey
import com.music.vivi.ui.component.ColorPickerDialog
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.IconButton as AppIconButton
import com.music.vivi.ui.component.Material3SettingsGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassEffectSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (globalEnabled, onGlobalEnabledChange) = rememberPreference(
        LiquidGlassGlobalEnabledKey, defaultValue = false
    )
    val (vibrancy, onVibrancyChange) = rememberPreference(
        LiquidGlassVibrancyKey, defaultValue = 1f
    )
    val (blurRadius, onBlurRadiusChange) = rememberPreference(
        LiquidGlassBlurRadiusKey, defaultValue = 8f
    )
    val (lensHeight, onLensHeightChange) = rememberPreference(
        LiquidGlassLensHeightKey, defaultValue = 0.5f
    )
    val (lensAmount, onLensAmountChange) = rememberPreference(
        LiquidGlassLensAmountKey, defaultValue = 0.5f
    )
    val (chromaticAberration, onChromaticAberrationChange) = rememberPreference(
        LiquidGlassChromaticAberrationKey, defaultValue = true
    )
    val (depthEffect, onDepthEffectChange) = rememberPreference(
        LiquidGlassDepthEffectKey, defaultValue = true
    )
    // 0 marks the theme-adaptive default tint (see MainActivity); the picker then
    // shows the color the current theme resolves to.
    val (surfaceTintColorInt, onSurfaceTintColorChange) = rememberPreference(
        LiquidGlassSurfaceTintColorKey, defaultValue = 0
    )
    val adaptiveTintColor = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFAFAFA)
    } else {
        Color(0xFF121212)
    }
    val surfaceTintColor = if (surfaceTintColorInt == 0) {
        adaptiveTintColor
    } else {
        Color(surfaceTintColorInt)
    }
    val (surfaceOpacity, onSurfaceOpacityChange) = rememberPreference(
        LiquidGlassSurfaceOpacityKey, defaultValue = 0.4f
    )
    val (textColorInt, onTextColorChange) = rememberPreference(
        LiquidGlassTextColorKey, defaultValue = Color.White.toArgb()
    )
    val textColor = remember(textColorInt) { Color(textColorInt) }
    val (playerEnabled, onPlayerEnabledChange) = rememberPreference(
        LiquidGlassPlayerEnabledKey, defaultValue = true
    )
    val (miniPlayerEnabled, onMiniPlayerEnabledChange) = rememberPreference(
        LiquidGlassMiniPlayerEnabledKey, defaultValue = true
    )
    val (navBarEnabled, onNavBarEnabledChange) = rememberPreference(
        LiquidGlassNavBarEnabledKey, defaultValue = true
    )
    val (useFloatingNavBar, onUseFloatingNavBarChange) = rememberPreference(
        UseFloatingNavBarKey, defaultValue = false
    )

    var showVibrancyDialog by rememberSaveable { mutableStateOf(false) }
    var showBlurRadiusDialog by rememberSaveable { mutableStateOf(false) }
    var showLensHeightDialog by rememberSaveable { mutableStateOf(false) }
    var showLensAmountDialog by rememberSaveable { mutableStateOf(false) }
    var showSurfaceOpacityDialog by rememberSaveable { mutableStateOf(false) }
    var showSurfaceTintDialog by rememberSaveable { mutableStateOf(false) }
    var showTextColorDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Material3SettingsGroup(
            title = stringResource(R.string.nav_bar_style),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.nav_bar),
                    title = { Text(stringResource(R.string.floating_nav_bar)) },
                    description = { Text(stringResource(R.string.floating_nav_bar_desc)) },
                    trailingContent = {
                        Switch(
                            checked = useFloatingNavBar,
                            onCheckedChange = onUseFloatingNavBarChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (useFloatingNavBar) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onUseFloatingNavBarChange(!useFloatingNavBar) }
                ),
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.liquid_glass),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.check),
                    title = { Text(stringResource(R.string.liquid_glass_global_enabled)) },
                    description = {
                        Text(
                            stringResource(
                                // Glass is part of the floating nav bar experience and only
                                // takes effect while that bar is enabled.
                                if (useFloatingNavBar) {
                                    R.string.liquid_glass_performance_warning
                                } else {
                                    R.string.liquid_glass_requires_floating_nav_bar
                                }
                            )
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = globalEnabled && useFloatingNavBar,
                            onCheckedChange = onGlobalEnabledChange,
                            enabled = useFloatingNavBar,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (globalEnabled && useFloatingNavBar) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { if (useFloatingNavBar) onGlobalEnabledChange(!globalEnabled) }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.liquid_glass_effects),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.liquid_glass_vibrancy)) },
                    description = { Text(stringResource(R.string.liquid_glass_vibrancy_desc)) },
                    onClick = { showVibrancyDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.liquid_glass_blur_radius)) },
                    description = { Text(stringResource(R.string.liquid_glass_blur_radius_desc)) },
                    onClick = { showBlurRadiusDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.liquid_glass_lens_height)) },
                    onClick = { showLensHeightDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.liquid_glass_lens_amount)) },
                    onClick = { showLensAmountDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.liquid_glass_chromatic_aberration)) },
                    trailingContent = {
                        Switch(
                            checked = chromaticAberration,
                            onCheckedChange = onChromaticAberrationChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (chromaticAberration) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onChromaticAberrationChange(!chromaticAberration) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.liquid_glass_depth_effect)) },
                    trailingContent = {
                        Switch(
                            checked = depthEffect,
                            onCheckedChange = onDepthEffectChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (depthEffect) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onDepthEffectChange(!depthEffect) }
                ),
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.liquid_glass_appearance),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.palette),
                    title = { Text(stringResource(R.string.liquid_glass_surface_tint)) },
                    description = { Text(stringResource(R.string.liquid_glass_surface_tint_desc)) },
                    onClick = { showSurfaceTintDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.liquid_glass_surface_opacity)) },
                    description = { Text(stringResource(R.string.liquid_glass_surface_opacity_desc)) },
                    onClick = { showSurfaceOpacityDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.palette),
                    title = { Text(stringResource(R.string.liquid_glass_text_color)) },
                    description = { Text(stringResource(R.string.liquid_glass_text_color_desc)) },
                    onClick = { showTextColorDialog = true }
                ),
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.liquid_glass_per_component),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.music_note),
                    title = { Text(stringResource(R.string.liquid_glass_player)) },
                    trailingContent = {
                        Switch(
                            checked = playerEnabled,
                            onCheckedChange = onPlayerEnabledChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (playerEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onPlayerEnabledChange(!playerEnabled) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.music_note),
                    title = { Text(stringResource(R.string.liquid_glass_mini_player)) },
                    trailingContent = {
                        Switch(
                            checked = miniPlayerEnabled,
                            onCheckedChange = onMiniPlayerEnabledChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (miniPlayerEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onMiniPlayerEnabledChange(!miniPlayerEnabled) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.nav_bar),
                    title = { Text(stringResource(R.string.liquid_glass_nav_bar)) },
                    trailingContent = {
                        Switch(
                            checked = navBarEnabled,
                            onCheckedChange = onNavBarEnabledChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (navBarEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onNavBarEnabledChange(!navBarEnabled) }
                ),
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showVibrancyDialog) {
        var tempValue by remember { mutableFloatStateOf(vibrancy) }
        DefaultDialog(
            onDismiss = { tempValue = vibrancy; showVibrancyDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 1f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = vibrancy; showVibrancyDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onVibrancyChange(tempValue); showVibrancyDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.liquid_glass_vibrancy), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.2f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..2f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showBlurRadiusDialog) {
        var tempValue by remember { mutableFloatStateOf(blurRadius) }
        DefaultDialog(
            onDismiss = { tempValue = blurRadius; showBlurRadiusDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 8f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = blurRadius; showBlurRadiusDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onBlurRadiusChange(tempValue); showBlurRadiusDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.liquid_glass_blur_radius), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.0f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..100f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showLensHeightDialog) {
        var tempValue by remember { mutableFloatStateOf(lensHeight) }
        DefaultDialog(
            onDismiss = { tempValue = lensHeight; showLensHeightDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 0.5f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = lensHeight; showLensHeightDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onLensHeightChange(tempValue); showLensHeightDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.liquid_glass_lens_height), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.2f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showLensAmountDialog) {
        var tempValue by remember { mutableFloatStateOf(lensAmount) }
        DefaultDialog(
            onDismiss = { tempValue = lensAmount; showLensAmountDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 0.5f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = lensAmount; showLensAmountDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onLensAmountChange(tempValue); showLensAmountDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.liquid_glass_lens_amount), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.2f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showSurfaceOpacityDialog) {
        var tempValue by remember { mutableFloatStateOf(surfaceOpacity) }
        DefaultDialog(
            onDismiss = { tempValue = surfaceOpacity; showSurfaceOpacityDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 0.4f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = surfaceOpacity; showSurfaceOpacityDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onSurfaceOpacityChange(tempValue); showSurfaceOpacityDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.liquid_glass_surface_opacity), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.2f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showSurfaceTintDialog) {
        ColorPickerDialog(
            initialColor = surfaceTintColor,
            title = stringResource(R.string.liquid_glass_surface_tint),
            onDismiss = { showSurfaceTintDialog = false },
            onConfirm = { color ->
                onSurfaceTintColorChange(color.toArgb())
                showSurfaceTintDialog = false
            },
            // Reset restores the theme-adaptive default rather than a fixed color.
            onReset = {
                onSurfaceTintColorChange(0)
                showSurfaceTintDialog = false
            },
        )
    }

    if (showTextColorDialog) {
        ColorPickerDialog(
            initialColor = textColor,
            title = stringResource(R.string.liquid_glass_text_color),
            onDismiss = { showTextColorDialog = false },
            onConfirm = { color ->
                onTextColorChange(color.toArgb())
                showTextColorDialog = false
            },
            defaultColor = Color.White,
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.liquid_glass_settings)) },
        navigationIcon = {
            AppIconButton(
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


