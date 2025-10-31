package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.music.vivi.constants.DisableLoadMoreWhenRepeatAllKey
import com.music.vivi.constants.HistoryDuration
import com.music.vivi.constants.PersistentQueueKey
import com.music.vivi.constants.SeekExtraSeconds
import com.music.vivi.constants.SimilarContent
import com.music.vivi.constants.SkipSilenceKey
import com.music.vivi.constants.StopMusicOnTaskClearKey
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
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

    var showAudioQualityDialog by rememberSaveable { mutableStateOf(false) }
    var showHistoryDurationDialog by rememberSaveable { mutableStateOf(false) }

    // Audio Quality Dialog
    if (showAudioQualityDialog) {
        DefaultDialog(
            onDismiss = { showAudioQualityDialog = false },
            content = {
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    listOf(AudioQuality.AUTO, AudioQuality.HIGH, AudioQuality.LOW).forEach { value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAudioQualityChange(value)
                                    showAudioQualityDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = value == audioQuality,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                when (value) {
                                    AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                                    AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                                    AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                                }
                            )
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showAudioQualityDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // History Duration Dialog
    if (showHistoryDurationDialog) {
        var tempDuration by remember { mutableStateOf(historyDuration) }

        AlertDialog(
            onDismissRequest = { showHistoryDurationDialog = false },
            title = { Text(stringResource(R.string.history_duration)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "${tempDuration.toInt()} seconds",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = tempDuration,
                        onValueChange = { tempDuration = it },
                        valueRange = 0f..60f,
                        steps = 11
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onHistoryDurationChange(tempDuration)
                        showHistoryDurationDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryDurationDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

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
                            onClick = navController::navigateUp,
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
                            text = "Configure player and audio settings",
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.graphic_eq), null, modifier = Modifier.size(22.dp)) },
                                title = stringResource(R.string.audio_quality),
                                subtitle = when (audioQuality) {
                                    AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                                    AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                                    AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                                },
                                onClick = { showAudioQualityDialog = true },
                                showArrow = true,
                                showSettingsIcon = true
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.history), null, modifier = Modifier.size(22.dp)) },
                                title = stringResource(R.string.history_duration),
                                subtitle = "${historyDuration.toInt()} seconds",
                                onClick = { showHistoryDurationDialog = true },
                                showArrow = true,
                                showSettingsIcon = true
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.fast_forward), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.skip_silence),
                                        subtitle = "Remove silent parts"
                                    )
                                }
                                ModernSwitch(
                                    checked = skipSilence,
                                    onCheckedChange = onSkipSilenceChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.volume_up), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.audio_normalization),
                                        subtitle = "Normalize audio levels"
                                    )
                                }
                                ModernSwitch(
                                    checked = audioNormalization,
                                    onCheckedChange = onAudioNormalizationChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.graphic_eq), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.audio_offload),
                                        subtitle = stringResource(R.string.audio_offload_description)
                                    )
                                }
                                ModernSwitch(
                                    checked = audioOffload,
                                    onCheckedChange = onAudioOffloadChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.arrow_forward), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.seek_seconds_addup),
                                        subtitle = stringResource(R.string.seek_seconds_addup_description)
                                    )
                                }
                                ModernSwitch(
                                    checked = seekExtraSeconds,
                                    onCheckedChange = onSeekExtraSeconds,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }
                        }
                    }
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.queue_music), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.persistent_queue),
                                        subtitle = stringResource(R.string.persistent_queue_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = persistentQueue,
                                    onCheckedChange = onPersistentQueueChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.playlist_add), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.auto_load_more),
                                        subtitle = stringResource(R.string.auto_load_more_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = autoLoadMore,
                                    onCheckedChange = onAutoLoadMoreChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.repeat), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.disable_load_more_when_repeat_all),
                                        subtitle = stringResource(R.string.disable_load_more_when_repeat_all_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = disableLoadMoreWhenRepeatAll,
                                    onCheckedChange = onDisableLoadMoreWhenRepeatAllChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.download), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.auto_download_on_like),
                                        subtitle = stringResource(R.string.auto_download_on_like_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = autoDownloadOnLike,
                                    onCheckedChange = onAutoDownloadOnLikeChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.similar), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.enable_similar_content),
                                        subtitle = stringResource(R.string.similar_content_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = similarContentEnabled,
                                    onCheckedChange = similarContentEnabledChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.skip_next), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.auto_skip_next_on_error),
                                        subtitle = stringResource(R.string.auto_skip_next_on_error_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = autoSkipNextOnError,
                                    onCheckedChange = onAutoSkipNextOnErrorChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }
                        }
                    }
                }

                // Misc Section
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.clear_all), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.stop_music_on_task_clear),
                                        subtitle = "Stop playback when app is closed"
                                    )
                                }
                                ModernSwitch(
                                    checked = stopMusicOnTaskClear,
                                    onCheckedChange = onStopMusicOnTaskClearChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}