package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.AudioNormalizationKey
import com.music.vivi.constants.AudioOffload
import com.music.vivi.constants.AudioQuality
import com.music.vivi.constants.AudioQualityKey
import com.music.vivi.constants.AutoDownloadOnLikeKey
import com.music.vivi.constants.AutoLoadMoreKey
import com.music.vivi.constants.AutoSkipNextOnErrorKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.DisableLoadMoreWhenRepeatAllKey
import com.music.vivi.constants.HistoryDuration
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.PauseOnHeadphonesDisconnectKey
import com.music.vivi.constants.PauseOnZeroVolumeKey
import com.music.vivi.constants.PersistentQueueKey
import com.music.vivi.constants.SeekExtraSeconds
import com.music.vivi.constants.SettingsShapeColorTertiaryKey
import com.music.vivi.constants.SimilarContent
import com.music.vivi.constants.SkipSilenceKey
import com.music.vivi.constants.SmartShuffleKey
import com.music.vivi.constants.SmartSuggestionsKey
import com.music.vivi.constants.StopMusicOnTaskClearKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.Material3ExpressiveSettingsGroup
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference

/**
 * Screen for configuring audio and player-related settings.
 * Includes audio quality, normalization, skip silence, simple shuffle, and other playback options.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: (() -> Unit)? = null,
) {
    val (settingsShapeTertiary, _) = rememberPreference(SettingsShapeColorTertiaryKey, false)
    val (darkMode, _) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkMode, isSystemInDarkTheme) {
        if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
    }

    val (iconBgColor, iconStyleColor) = if (settingsShapeTertiary) {
        if (useDarkTheme) {
            Pair(
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.onTertiary
            )
        } else {
            Pair(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    } else {
        Pair(
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.primary
        )
    }

    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(
        PersistentQueueKey,
        defaultValue = true
    )
    val (skipSilence, onSkipSilenceChange) = rememberPreference(
        SkipSilenceKey,
        defaultValue = false
    )
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(
        AudioNormalizationKey,
        defaultValue = true
    )

    val (audioOffload, onAudioOffloadChange) = rememberPreference(
        key = AudioOffload,
        defaultValue = false
    )

    val (seekExtraSeconds, onSeekExtraSeconds) = rememberPreference(
        SeekExtraSeconds,
        defaultValue = false
    )

    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(
        AutoLoadMoreKey,
        defaultValue = true
    )

    val (disableLoadMoreWhenRepeatAll, onDisableLoadMoreWhenRepeatAllChange) = rememberPreference(
        DisableLoadMoreWhenRepeatAllKey,
        defaultValue = false
    )
    val (autoDownloadOnLike, onAutoDownloadOnLikeChange) = rememberPreference(
        AutoDownloadOnLikeKey,
        defaultValue = false
    )
    val (similarContentEnabled, similarContentEnabledChange) = rememberPreference(
        key = SimilarContent,
        defaultValue = true
    )
    val (pauseOnZeroVolume, onPauseOnZeroVolumeChange) = rememberPreference(
        PauseOnZeroVolumeKey,
        defaultValue = false
    )
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(
        AutoSkipNextOnErrorKey,
        defaultValue = false
    )
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(
        StopMusicOnTaskClearKey,
        defaultValue = false
    )
    val (historyDuration, onHistoryDurationChange) = rememberPreference(
        HistoryDuration,
        defaultValue = 30f
    )
    val (smartShuffle, onSmartShuffleChange) = rememberPreference(
        SmartShuffleKey,
        defaultValue = false
    )
    val (smartSuggestions, onSmartSuggestionsChange) = rememberPreference(
        SmartSuggestionsKey,
        defaultValue = false
    )
    val (smartPause, onSmartPauseChange) = rememberPreference(
        PauseOnHeadphonesDisconnectKey,
        defaultValue = false
    )

    val scrollState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(
                            onClick = { onBack?.invoke() ?: navController.navigateUp() },
                            onLongClick = navController::backToMain
                        ) {
                            Icon(
                                painterResource(R.drawable.arrow_back),
                                contentDescription = null
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 32.dp
                )
            ) {
                item {
                    Spacer(modifier = Modifier.height(25.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.player_and_audio),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.configure_player_audio),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                // Player Section
                item {
                    Text(
                        text = stringResource(R.string.player).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = listOf(
                            {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    ModernInfoItem(
                                        icon = {
                                            Icon(
                                                painterResource(R.drawable.graphic_eq),
                                                null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        title = stringResource(R.string.audio_quality),
                                        subtitle = stringResource(R.string.select_playback_audio_quality),
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor
                                    )

                                    val options =
                                        listOf(
                                            AudioQuality.LOW,
                                            AudioQuality.HIGH,
                                            AudioQuality.VERY_HIGH,
                                            AudioQuality.AUTO
                                        )
                                    val labels = listOf(
                                        stringResource(R.string.audio_quality_low),
                                        stringResource(R.string.audio_quality_high),
                                        stringResource(R.string.audio_quality_very_high),
                                        stringResource(R.string.audio_quality_auto)
                                    )

                                    val context = LocalContext.current
                                    val (innerTubeCookie, _) = rememberPreference(InnerTubeCookieKey, "")
                                    val isLoggedIn = remember(innerTubeCookie) {
                                        "SAPISID" in com.music.innertube.utils.parseCookieString(innerTubeCookie)
                                    }

                                    FlowRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 64.dp, bottom = 12.dp, end = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(
                                            ButtonGroupDefaults.ConnectedSpaceBetween
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        options.forEachIndexed { index, value ->
                                            ToggleButton(
                                                checked = audioQuality == value,
                                                onCheckedChange = {
                                                    if (value == AudioQuality.VERY_HIGH) {
                                                        if (!isLoggedIn) {
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                R.string.login_required_premium,
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                            onAudioQualityChange(value) // Still allow selection, but warn
                                                        } else {
                                                            onAudioQualityChange(value)
                                                        }
                                                    } else {
                                                        onAudioQualityChange(value)
                                                    }
                                                },
                                                colors = ToggleButtonDefaults.toggleButtonColors(
                                                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                                                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                shapes = when (index) {
                                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                                },
                                                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton }
                                            ) {
                                                Text(labels[index], style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            },
                            {
                                // History Duration with permanent slider
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    ModernInfoItem(
                                        icon = {
                                            Icon(
                                                painterResource(R.drawable.history),
                                                null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        title = stringResource(R.string.history_duration),
                                        subtitle = "${historyDuration.toInt()} seconds",
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor
                                    )

                                    Slider(
                                        value = historyDuration,
                                        onValueChange = onHistoryDurationChange,
                                        valueRange = 0f..60f,
                                        steps = 11,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                            .padding(bottom = 12.dp)
                                    )
                                }
                            },
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.fast_forward),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.skip_silence),
                                            subtitle = stringResource(R.string.remove_silent_parts),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = skipSilence,
                                        onCheckedChange = onSkipSilenceChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            },
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.volume_up),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.audio_normalization),
                                            subtitle = stringResource(R.string.normalize_audio_levels),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = audioNormalization,
                                        onCheckedChange = onAudioNormalizationChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            },
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.graphic_eq),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.audio_offload),
                                            subtitle = stringResource(R.string.audio_offload_description),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = audioOffload,
                                        onCheckedChange = onAudioOffloadChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            },
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.arrow_forward),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.seek_seconds_addup),
                                            subtitle = stringResource(R.string.seek_seconds_addup_description),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = seekExtraSeconds,
                                        onCheckedChange = onSeekExtraSeconds,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            },
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.volume_off),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.pause_on_zero_volume),
                                            subtitle = stringResource(R.string.pause_on_zero_volume_description),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = pauseOnZeroVolume,
                                        onCheckedChange = onPauseOnZeroVolumeChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            },
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.shuffle),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.smart_shuffle),
                                            subtitle = stringResource(R.string.smart_shuffle_desc),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = smartShuffle,
                                        onCheckedChange = onSmartShuffleChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            },
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.auto_playlist),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.smart_suggestions),
                                            subtitle = stringResource(R.string.smart_suggestions_desc),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = smartSuggestions,
                                        onCheckedChange = onSmartSuggestionsChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            },
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.headphones),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.smart_pause),
                                            subtitle = stringResource(R.string.smart_pause_desc),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = smartPause,
                                        onCheckedChange = onSmartPauseChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            }
                        )
                    )
                }

                // Queue Section
                item {
                    Text(
                        text = stringResource(R.string.queue).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = listOf(
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.queue_music),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.persistent_queue),
                                            subtitle = stringResource(R.string.persistent_queue_desc),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = persistentQueue,
                                        onCheckedChange = onPersistentQueueChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            },
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.playlist_add),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.auto_load_more),
                                            subtitle = stringResource(R.string.auto_load_more_desc),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = autoLoadMore,
                                        onCheckedChange = onAutoLoadMoreChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            },
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.repeat),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.disable_load_more_when_repeat_all),
                                            subtitle = stringResource(R.string.disable_load_more_when_repeat_all_desc),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = disableLoadMoreWhenRepeatAll,
                                        onCheckedChange = onDisableLoadMoreWhenRepeatAllChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            },
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.download),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.auto_download_on_like),
                                            subtitle = stringResource(R.string.auto_download_on_like_desc),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = autoDownloadOnLike,
                                        onCheckedChange = onAutoDownloadOnLikeChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            },
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.similar),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.enable_similar_content),
                                            subtitle = stringResource(R.string.similar_content_desc),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = similarContentEnabled,
                                        onCheckedChange = similarContentEnabledChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            },
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.skip_next),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.auto_skip_next_on_error),
                                            subtitle = stringResource(R.string.auto_skip_next_on_error_desc),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = autoSkipNextOnError,
                                        onCheckedChange = onAutoSkipNextOnErrorChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            }
                        )
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.misc).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = listOf(
                            {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.clear_all),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.stop_music_on_task_clear),
                                            subtitle = stringResource(R.string.stop_playback_when_closed),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = stopMusicOnTaskClear,
                                        onCheckedChange = onStopMusicOnTaskClearChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            }
                        )
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
