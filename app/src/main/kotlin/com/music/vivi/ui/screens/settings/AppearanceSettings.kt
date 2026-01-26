package com.music.vivi.ui.screens.settings

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
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
import com.music.vivi.constants.AccentColorKey
import com.music.vivi.constants.AppleMusicLyricsBlurKey
import com.music.vivi.constants.ChipSortTypeKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.DefaultOpenTabKey
import com.music.vivi.constants.DynamicThemeKey
import com.music.vivi.constants.GridItemSize
import com.music.vivi.constants.GridItemsSizeKey
import com.music.vivi.constants.HidePlayerThumbnailKey
import com.music.vivi.constants.HighRefreshRateKey
import com.music.vivi.constants.LibraryFilter
import com.music.vivi.constants.LyricsClickKey
import com.music.vivi.constants.LyricsLetterByLetterAnimationKey
import com.music.vivi.constants.LyricsLineSpacingKey
import com.music.vivi.constants.LyricsScrollKey
import com.music.vivi.constants.LyricsTextPositionKey
import com.music.vivi.constants.LyricsTextSizeKey
import com.music.vivi.constants.LyricsVerticalPositionKey
import com.music.vivi.constants.LyricsWordForWordKey
import com.music.vivi.constants.Material3ExpressiveKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.constants.PlayerButtonsStyle
import com.music.vivi.constants.PlayerButtonsStyleKey
import com.music.vivi.constants.PureBlackKey
import com.music.vivi.constants.RotatingThumbnailKey
import com.music.vivi.constants.SettingsShapeColorTertiaryKey
import com.music.vivi.constants.ShowCachedPlaylistKey
import com.music.vivi.constants.ShowDownloadedPlaylistKey
import com.music.vivi.constants.ShowLikedPlaylistKey
import com.music.vivi.constants.ShowNowPlayingAppleMusicKey
import com.music.vivi.constants.ShowTopPlaylistKey
import com.music.vivi.constants.ShowUploadedPlaylistKey
import com.music.vivi.constants.SliderStyle
import com.music.vivi.constants.SliderStyleKey
import com.music.vivi.constants.SlimNavBarKey
import com.music.vivi.constants.SwipeSensitivityKey
import com.music.vivi.constants.SwipeThumbnailKey
import com.music.vivi.constants.SwipeToRemoveSongKey
import com.music.vivi.constants.SwipeToSongKey
import com.music.vivi.constants.UseNewMiniPlayerDesignKey
import com.music.vivi.constants.UseNewPlayerDesignKey
import com.music.vivi.playback.MusicService
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.PlayerSliderTrack
import com.music.vivi.ui.component.RoundedCheckbox
import com.music.vivi.ui.component.WavySlider
import com.music.vivi.ui.theme.DefaultThemeColor
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.Material3ExpressiveSettingsGroup
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.update.widget.MusicPlayerWidgetReceiver
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import kotlin.math.roundToInt

/**
 * Screen for customizing the app's visual appearance.
 * Includes settings for themes, player design, lyrics style, and more.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(
        DynamicThemeKey,
        defaultValue = true
    )
    val (settingsShapeTertiary, onSettingsShapeTertiaryChange) = rememberPreference(
        key = SettingsShapeColorTertiaryKey,
        defaultValue = false
    )
    val (darkMode, onDarkModeChange) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO
    )
    val (highRefreshRate, onHighRefreshRateChange) = rememberPreference(
        HighRefreshRateKey,
        defaultValue = false
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
    LaunchedEffect(darkMode) {
        val intent = android.content.Intent(context, MusicService::class.java).apply {
            action = MusicPlayerWidgetReceiver.ACTION_UPDATE_WIDGET
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            // Service might be restricted in background
        }
    }
    val (useNewPlayerDesign, onUseNewPlayerDesignChange) = rememberPreference(
        UseNewPlayerDesignKey,
        defaultValue = true
    )

// switch for thumbnail
    val (rotatingThumbnail, onRotatingThumbnailChange) = rememberPreference(
        RotatingThumbnailKey,
        defaultValue = false
    )

    val (swipeToRemoveSong, onSwipeToRemoveSongChange) = rememberPreference(
        SwipeToRemoveSongKey,
        defaultValue = false

    )
    val (useNewMiniPlayerDesign, onUseNewMiniPlayerDesignChange) = rememberPreference(
        UseNewMiniPlayerDesignKey,
        defaultValue = true
    )
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(
        HidePlayerThumbnailKey,
        defaultValue = false
    )
    val (playerBackground, onPlayerBackgroundChange) = rememberEnumPreference(
        PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.GRADIENT
    )
    val (showNowPlayingAppleMusic, onShowNowPlayingAppleMusicChange) = rememberPreference(
        ShowNowPlayingAppleMusicKey,
        defaultValue = false
    )
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)
    val (material3Expressive, onMaterial3ExpressiveChange) = rememberPreference(
        Material3ExpressiveKey,
        defaultValue = false
    )
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(
        DefaultOpenTabKey,
        defaultValue = NavigationTab.HOME
    )
    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(
        PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(
        LyricsTextPositionKey,
        defaultValue = LyricsPosition.LEFT
    )
    val (lyricsVerticalPosition, onLyricsVerticalPositionChange) = rememberEnumPreference(
        LyricsVerticalPositionKey,
        defaultValue = LyricsVerticalPosition.TOP
    )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) = rememberPreference(LyricsScrollKey, defaultValue = true)
    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(
        SliderStyleKey,
        defaultValue = SliderStyle.DEFAULT
    )
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(
        SwipeThumbnailKey,
        defaultValue = true
    )
    val (swipeSensitivity, onSwipeSensitivityChange) = rememberPreference(
        SwipeSensitivityKey,
        defaultValue = 0.73f
    )
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(
        GridItemsSizeKey,
        defaultValue = GridItemSize.SMALL
    )
    val (slimNav, onSlimNavChange) = rememberPreference(
        SlimNavBarKey,
        defaultValue = false
    )
    val (swipeToSong, onSwipeToSongChange) = rememberPreference(
        SwipeToSongKey,
        defaultValue = false
    )
    val (showLikedPlaylist, onShowLikedPlaylistChange) = rememberPreference(
        ShowLikedPlaylistKey,
        defaultValue = true
    )
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) = rememberPreference(
        ShowDownloadedPlaylistKey,
        defaultValue = true
    )
    val (showTopPlaylist, onShowTopPlaylistChange) = rememberPreference(
        ShowTopPlaylistKey,
        defaultValue = true
    )
    val (showCachedPlaylist, onShowCachedPlaylistChange) = rememberPreference(
        ShowCachedPlaylistKey,
        defaultValue = true
    )
    val (showUploadedPlaylist, onShowUploadedPlaylistChange) = rememberPreference(
        ShowUploadedPlaylistKey,
        defaultValue = true
    )
    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(
        key = ChipSortTypeKey,
        defaultValue = LibraryFilter.LIBRARY
    )

    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(
        LyricsTextSizeKey,
        defaultValue = 28f
    )
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(
        LyricsLineSpacingKey,
        defaultValue = 6f
    )
    val (lyricsWordForWord, onLyricsWordForWordChange) = rememberPreference(
        LyricsWordForWordKey,
        defaultValue = true
    )
    val (letterByLetterAnimation, onLetterByLetterAnimationChange) = rememberPreference(
        LyricsLetterByLetterAnimationKey,
        defaultValue = false
    )
    val (appleMusicLyricsBlur, onAppleMusicLyricsBlurChange) = rememberPreference(
        AppleMusicLyricsBlurKey,
        defaultValue = true
    )

    val availableBackgroundStyles = remember(Build.VERSION.SDK_INT) {
        PlayerBackgroundStyle.entries.filter {
            it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }
    }

    var showSliderOptionDialog by rememberSaveable { mutableStateOf(false) }
    var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }
    var showPlayerBackgroundDialog by rememberSaveable { mutableStateOf(false) }
    var showPlayerButtonsStyleDialog by rememberSaveable { mutableStateOf(false) }
    var showDefaultChipDialog by rememberSaveable { mutableStateOf(false) }
    var showLyricsTextSizeDialog by rememberSaveable { mutableStateOf(false) }
    var showLyricsLineSpacingDialog by rememberSaveable { mutableStateOf(false) }
    val (accentColorInt, onAccentColorChange) = rememberPreference(
        AccentColorKey,
        defaultValue = DefaultThemeColor.toArgb()
    )

    // Lyrics Text Size Dialog
    if (showLyricsTextSizeDialog) {
        var tempTextSize by remember { mutableFloatStateOf(lyricsTextSize) }
        val sliderState = rememberSliderState(
            value = tempTextSize,
            valueRange = 16f..48f,
            steps = 31 // Snap to integer values (48 - 16 - 1 = 31 gaps)
        )

        DefaultDialog(
            onDismiss = { showLyricsTextSizeDialog = false },
            content = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.lyrics_text_size),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.size),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${sliderState.value.roundToInt()} sp",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(state = sliderState)
                    }
                }
            },
            buttons = {
                TextButton(
                    onClick = { sliderState.value = 28f }
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = { showLyricsTextSizeDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onLyricsTextSizeChange(sliderState.value)
                        showLyricsTextSizeDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    // Lyrics Line Spacing Dialog
    if (showLyricsLineSpacingDialog) {
        var tempLineSpacing by remember { mutableFloatStateOf(lyricsLineSpacing) }
        val sliderState = rememberSliderState(
            value = tempLineSpacing,
            valueRange = 0f..32f,
            steps = 31 // Snap to integer values (32 - 0 - 1 = 31 gaps)
        )

        DefaultDialog(
            onDismiss = { showLyricsLineSpacingDialog = false },
            content = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.lyrics_line_spacing),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.spacing),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${sliderState.value.roundToInt()} dp",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(state = sliderState)
                    }
                }
            },
            buttons = {
                TextButton(
                    onClick = { sliderState.value = 6f }
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = { showLyricsLineSpacingDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onLyricsLineSpacingChange(sliderState.value)
                        showLyricsLineSpacingDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    // Slider Style Dialog
    if (showSliderOptionDialog) {
        DefaultDialog(
            onDismiss = { showSliderOptionDialog = false },
            content = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle ==
                                    SliderStyle.DEFAULT
                                ) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSliderStyleChange(SliderStyle.DEFAULT)
                                showSliderOptionDialog = false
                            }
                            .padding(16.dp)
                    ) {
                        val state = rememberSliderState(value = 0.5f)
                        Slider(state = state, modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.default_),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle ==
                                    SliderStyle.WAVY
                                ) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSliderStyleChange(SliderStyle.WAVY)
                                showSliderOptionDialog = false
                            }
                            .padding(16.dp)
                    ) {
                        var sliderValue by remember { mutableFloatStateOf(0.5f) }
                        WavySlider(
                            value = sliderValue,
                            valueRange = 0f..1f,
                            onValueChange = { sliderValue = it },
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.wavy),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle ==
                                    SliderStyle.SLIM
                                ) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSliderStyleChange(SliderStyle.SLIM)
                                showSliderOptionDialog = false
                            }
                            .padding(16.dp)
                    ) {
                        val state = rememberSliderState(value = 0.5f)
                        Slider(
                            state = state,
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { sliderState ->
                                PlayerSliderTrack(
                                    sliderState = sliderState,
                                    colors = SliderDefaults.colors()
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .pointerInput(Unit) {
                                    detectTapGestures(onPress = {})
                                }
                        )
                        Text(
                            text = stringResource(R.string.slim),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showSliderOptionDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Swipe Sensitivity Dialog
    if (showSensitivityDialog) {
        var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }
        val sliderState = rememberSliderState(
            value = tempSensitivity,
            valueRange = 0f..1f
        )

        DefaultDialog(
            onDismiss = {
                showSensitivityDialog = false
            },
            content = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.swipe_sensitivity),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = stringResource(R.string.sensitivity_percentage, (sliderState.value * 100).roundToInt()),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Slider(state = sliderState)
                }
            },
            buttons = {
                TextButton(
                    onClick = {
                        sliderState.value = 0.73f
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        showSensitivityDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onSwipeSensitivityChange(sliderState.value)
                        showSensitivityDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    // Player Background Dialog
    if (showPlayerBackgroundDialog) {
        DefaultDialog(
            onDismiss = { showPlayerBackgroundDialog = false },
            content = {
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    availableBackgroundStyles.forEach { value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPlayerBackgroundChange(value)
                                    showPlayerBackgroundDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoundedCheckbox(
                                checked = value == playerBackground,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                when (value) {
                                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                    PlayerBackgroundStyle.APPLE_MUSIC -> stringResource(R.string.apple_music)
                                }
                            )
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showPlayerBackgroundDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Player Buttons Style Dialog
    if (showPlayerButtonsStyleDialog) {
        DefaultDialog(
            onDismiss = { showPlayerButtonsStyleDialog = false },
            content = {
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    PlayerButtonsStyle.entries.forEach { value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPlayerButtonsStyleChange(value)
                                    showPlayerButtonsStyleDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoundedCheckbox(
                                checked = value == playerButtonsStyle,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                when (value) {
                                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                                    PlayerButtonsStyle.SECONDARY -> stringResource(R.string.secondary_color_style)
                                    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                                }
                            )
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showPlayerButtonsStyleDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Default Chip Dialog
    if (showDefaultChipDialog) {
        DefaultDialog(
            onDismiss = { showDefaultChipDialog = false },
            content = {
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    listOf(
                        LibraryFilter.LIBRARY,
                        LibraryFilter.PLAYLISTS,
                        LibraryFilter.SONGS,
                        LibraryFilter.ALBUMS,
                        LibraryFilter.ARTISTS
                    ).forEach { value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDefaultChipChange(value)
                                    showDefaultChipDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoundedCheckbox(
                                checked = value == defaultChip,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                when (value) {
                                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                                }
                            )
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showDefaultChipDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
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
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.appearance),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.customize_look_feel),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Theme Section
                item {
                    Text(
                        text = stringResource(R.string.theme).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    val themeItems =
                        remember(useDarkTheme, dynamicTheme, darkMode, pureBlack, material3Expressive, settingsShapeTertiary, iconBgColor, iconStyleColor, highRefreshRate) {
                            buildList<@Composable () -> Unit> {
                                add {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = {
                                                    Icon(
                                                        painterResource(R.drawable.palette),
                                                        null,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                },
                                                title = stringResource(R.string.enable_dynamic_theme),
                                                subtitle = stringResource(R.string.dynamic_color_theming_subtitle),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(
                                            checked = dynamicTheme,
                                            onCheckedChange = { checked ->
                                                onDynamicThemeChange(checked)
                                                if (!checked) {
                                                    onAccentColorChange(Color(0xFF4285F4).toArgb())
                                                }
                                            },
                                            modifier = Modifier.padding(end = 20.dp)
                                        )
                                    }
                                }
                                add {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.palette),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.accent_color),
                                            subtitle = stringResource(R.string.select_static_theme_color),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )

                                        val presetColors = remember {
                                            listOf(
                                                Color(0xFF4285F4), // Blue
                                                Color(0xFFDB4437), // Red
                                                Color(0xFFF4B400), // Yellow
                                                Color(0xFF0F9D58), // Green
                                                Color(0xFF673AB7), // Purple
                                                Color(0xFFE91E63), // Pink
                                                Color(0xFFFF9800), // Orange
                                                Color(0xFF00BCD4), // Cyan
                                                Color(0xFF009688) // Teal
                                            )
                                        }

                                        LazyRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 64.dp, bottom = 12.dp, end = 20.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            items(presetColors) { color ->
                                                val isSelected = accentColorInt == color.toArgb() && !dynamicTheme
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .background(color, CircleShape)
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            onAccentColorChange(color.toArgb())
                                                            onDynamicThemeChange(false)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val scale by animateFloatAsState(
                                                        targetValue = if (isSelected) 1f else 0f
                                                    )
                                                    if (scale > 0f) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(24.dp).scale(scale)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                add {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(iconBgColor, RoundedCornerShape(12.dp))
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CompositionLocalProvider(LocalContentColor provides iconStyleColor) {
                                                Icon(
                                                    painterResource(R.drawable.dark_mode),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.dark_theme),
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.select_theme_preference),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            val options = listOf(DarkMode.AUTO, DarkMode.ON, DarkMode.OFF)
                                            val labels =
                                                listOf(
                                                    stringResource(R.string.dark_theme_follow_system),
                                                    stringResource(R.string.dark_theme_on),
                                                    stringResource(R.string.dark_theme_off)
                                                )

                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(
                                                    ButtonGroupDefaults.ConnectedSpaceBetween
                                                ),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                options.forEachIndexed { index, value ->
                                                    ToggleButton(
                                                        checked = darkMode == value,
                                                        onCheckedChange = { onDarkModeChange(value) },
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
                                                        modifier = Modifier.weight(1f).semantics {
                                                            role = Role.RadioButton
                                                        }
                                                    ) {
                                                        Text(labels[index], style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (useDarkTheme) {
                                    add {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                ModernInfoItem(
                                                    icon = {
                                                        Icon(
                                                            painterResource(R.drawable.contrast),
                                                            null,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    },
                                                    title = stringResource(R.string.pure_black),
                                                    subtitle = stringResource(R.string.use_pure_black_dark_theme),
                                                    iconBackgroundColor = iconBgColor,
                                                    iconContentColor = iconStyleColor
                                                )
                                            }
                                            ModernSwitch(
                                                checked = pureBlack,
                                                onCheckedChange = onPureBlackChange,
                                                modifier = Modifier.padding(end = 20.dp)
                                            )
                                        }
                                    }
                                }
                                add {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = {
                                                    Icon(
                                                        painterResource(R.drawable.palette),
                                                        null,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                },
                                                title = "Material 3 Expressive",
                                                subtitle = "Use expressive shapes and layout",
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(
                                            checked = material3Expressive,
                                            onCheckedChange = onMaterial3ExpressiveChange,
                                            modifier = Modifier.padding(end = 20.dp)
                                        )
                                    }
                                }
                                add {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = {
                                                    Icon(
                                                        painterResource(R.drawable.palette),
                                                        null,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                },
                                                title = stringResource(R.string.settings_shape_tertiary_color),
                                                subtitle = stringResource(R.string.use_tertiary_color_icon_backgrounds),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(
                                            checked = settingsShapeTertiary,
                                            onCheckedChange = onSettingsShapeTertiaryChange,
                                            modifier = Modifier.padding(end = 20.dp)
                                        )
                                    }
                                }
                                add {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = {
                                                    Icon(
                                                        painterResource(R.drawable.palette),
                                                        null,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                },
                                                title = stringResource(R.string.high_refresh_rate),
                                                subtitle = stringResource(R.string.high_refresh_rate_desc),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(
                                            checked = highRefreshRate,
                                            onCheckedChange = onHighRefreshRateChange,
                                            modifier = Modifier.padding(end = 20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = themeItems
                    )
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
                    val playerItems =
                        remember(useNewPlayerDesign, useNewMiniPlayerDesign, rotatingThumbnail, playerBackground, showNowPlayingAppleMusic, hidePlayerThumbnail, playerButtonsStyle, sliderStyle, swipeThumbnail, swipeSensitivity, iconBgColor, iconStyleColor) {
                            buildList<@Composable () -> Unit> {
                                add {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = {
                                                    Icon(
                                                        painterResource(R.drawable.palette),
                                                        null,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                },
                                                title = stringResource(R.string.new_player_design),
                                                subtitle = stringResource(R.string.modern_player_interface),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(
                                            checked = useNewPlayerDesign,
                                            onCheckedChange = onUseNewPlayerDesignChange,
                                            modifier = Modifier.padding(end = 20.dp)
                                        )
                                    }
                                }
                                add {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = {
                                                    Icon(
                                                        painterResource(R.drawable.nav_bar),
                                                        null,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                },
                                                title = stringResource(R.string.new_mini_player_design),
                                                subtitle = stringResource(R.string.modern_mini_player),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(
                                            checked = useNewMiniPlayerDesign,
                                            onCheckedChange = onUseNewMiniPlayerDesignChange,
                                            modifier = Modifier.padding(end = 20.dp)
                                        )
                                    }
                                }
                                add {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = {
                                                    Icon(
                                                        painterResource(R.drawable.cycle_rotation),
                                                        null,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                },
                                                title = stringResource(R.string.rotating_thumbnail),
                                                subtitle = stringResource(R.string.enable_rotating_clover_thumbnail),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(
                                            checked = rotatingThumbnail,
                                            onCheckedChange = onRotatingThumbnailChange,
                                            modifier = Modifier.padding(end = 20.dp)
                                        )
                                    }
                                }
                                add {
                                    ModernInfoItem(
                                        icon = {
                                            Icon(
                                                painterResource(R.drawable.gradient),
                                                null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        title = stringResource(R.string.player_background_style),
                                        subtitle = when (playerBackground) {
                                            PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                            PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                            PlayerBackgroundStyle.BLUR -> stringResource(
                                                R.string.player_background_blur
                                            )
                                            PlayerBackgroundStyle.APPLE_MUSIC -> "Apple Music"
                                        },
                                        onClick = { showPlayerBackgroundDialog = true },
                                        showArrow = true,
                                        showSettingsIcon = true,
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor
                                    )
                                }
                                if (playerBackground == PlayerBackgroundStyle.APPLE_MUSIC) {
                                    add {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                ModernInfoItem(
                                                    icon = {
                                                        Icon(
                                                            painterResource(R.drawable.play_arrow),
                                                            null,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    },
                                                    title = stringResource(R.string.show_now_playing),
                                                    subtitle = stringResource(
                                                        R.string.enable_now_playing_text_on_player_background
                                                    ),
                                                    iconBackgroundColor = iconBgColor,
                                                    iconContentColor = iconStyleColor
                                                )
                                            }
                                            ModernSwitch(
                                                checked = showNowPlayingAppleMusic,
                                                onCheckedChange = onShowNowPlayingAppleMusicChange,
                                                modifier = Modifier.padding(end = 20.dp)
                                            )
                                        }
                                    }
                                }
                                add {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = {
                                                    Icon(
                                                        painterResource(R.drawable.hide_image),
                                                        null,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                },
                                                title = stringResource(R.string.hide_player_thumbnail),
                                                subtitle = stringResource(R.string.hide_player_thumbnail_desc),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(
                                            checked = hidePlayerThumbnail,
                                            onCheckedChange = onHidePlayerThumbnailChange,
                                            modifier = Modifier.padding(end = 20.dp)
                                        )
                                    }
                                }
                                add {
                                    ModernInfoItem(
                                        icon = {
                                            Icon(
                                                painterResource(R.drawable.palette),
                                                null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        title = stringResource(R.string.player_buttons_style),
                                        subtitle = when (playerButtonsStyle) {
                                            PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                                            PlayerButtonsStyle.SECONDARY -> stringResource(
                                                R.string.secondary_color_style
                                            )
                                            PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                                        },
                                        onClick = { showPlayerButtonsStyleDialog = true },
                                        showArrow = true,
                                        showSettingsIcon = true,
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor
                                    )
                                }
                                add {
                                    ModernInfoItem(
                                        icon = {
                                            Icon(
                                                painterResource(R.drawable.sliders),
                                                null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        title = stringResource(R.string.player_slider_style),
                                        subtitle = when (sliderStyle) {
                                            SliderStyle.DEFAULT -> stringResource(R.string.default_)
                                            SliderStyle.WAVY -> stringResource(R.string.wavy)
                                            SliderStyle.SLIM -> stringResource(R.string.slim)
                                        },
                                        onClick = { showSliderOptionDialog = true },
                                        showArrow = true,
                                        showSettingsIcon = true,
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor
                                    )
                                }
                                add {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = {
                                                    Icon(
                                                        painterResource(R.drawable.swipe),
                                                        null,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                },
                                                title = stringResource(R.string.enable_swipe_thumbnail),
                                                subtitle = stringResource(R.string.swipe_on_player_thumbnail),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(
                                            checked = swipeThumbnail,
                                            onCheckedChange = onSwipeThumbnailChange,
                                            modifier = Modifier.padding(end = 20.dp)
                                        )
                                    }
                                }
                                if (swipeThumbnail) {
                                    add {
                                        ModernInfoItem(
                                            icon = {
                                                Icon(
                                                    painterResource(R.drawable.tune),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.swipe_sensitivity),
                                            subtitle = stringResource(
                                                R.string.sensitivity_percentage,
                                                (
                                                    swipeSensitivity *
                                                        100
                                                    ).roundToInt()
                                            ),
                                            onClick = { showSensitivityDialog = true },
                                            showArrow = true,
                                            showSettingsIcon = true,
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                }
                            }
                        }
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = playerItems
                    )
                }

                // Lyrics Section
                item {
                    Text(
                        text = stringResource(R.string.lyrics).uppercase(),
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(iconBgColor, RoundedCornerShape(12.dp))
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CompositionLocalProvider(LocalContentColor provides iconStyleColor) {
                                            Icon(
                                                painterResource(R.drawable.lyrics),
                                                null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.lyrics_text_position),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.select_text_alignment),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        val options = LyricsPosition.entries
                                        val labels =
                                            listOf(
                                                stringResource(R.string.left),
                                                stringResource(R.string.center),
                                                stringResource(R.string.right)
                                            )

                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(
                                                ButtonGroupDefaults.ConnectedSpaceBetween
                                            ),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            options.forEachIndexed { index, value ->
                                                ToggleButton(
                                                    checked = lyricsPosition == value,
                                                    onCheckedChange = { onLyricsPositionChange(value) },
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
                                                    modifier = Modifier.weight(1f).semantics {
                                                        role = Role.RadioButton
                                                    }
                                                ) {
                                                    Text(labels[index], style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(iconBgColor, RoundedCornerShape(12.dp))
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CompositionLocalProvider(LocalContentColor provides iconStyleColor) {
                                            Icon(
                                                painterResource(R.drawable.lyrics),
                                                null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.lyrics_position),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.select_active_line_position),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        val options = LyricsVerticalPosition.entries
                                        val labels =
                                            listOf(stringResource(R.string.top), stringResource(R.string.center_option))

                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(
                                                ButtonGroupDefaults.ConnectedSpaceBetween
                                            ),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            options.forEachIndexed { index, value ->
                                                ToggleButton(
                                                    checked = lyricsVerticalPosition == value,
                                                    onCheckedChange = { onLyricsVerticalPositionChange(value) },
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
                                                    modifier = Modifier.weight(1f).semantics {
                                                        role = Role.RadioButton
                                                    }
                                                ) {
                                                    Text(labels[index], style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
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
                                                    painterResource(R.drawable.lyrics),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.lyrics_click_change),
                                            subtitle = stringResource(R.string.click_change_lyrics_position),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = lyricsClick,
                                        onCheckedChange = onLyricsClickChange,
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
                                                    painterResource(R.drawable.lyrics),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.lyrics_auto_scroll),
                                            subtitle = stringResource(R.string.auto_scroll_lyrics_subtitle),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = lyricsScroll,
                                        onCheckedChange = onLyricsScrollChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            },
                            {
                                ModernInfoItem(
                                    icon = {
                                        Icon(painterResource(R.drawable.tune), null, modifier = Modifier.size(22.dp))
                                    },
                                    title = stringResource(R.string.lyrics_text_size),
                                    subtitle = "${lyricsTextSize.roundToInt()} sp",
                                    onClick = { showLyricsTextSizeDialog = true },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            },
                            {
                                ModernInfoItem(
                                    icon = {
                                        Icon(painterResource(R.drawable.tune), null, modifier = Modifier.size(22.dp))
                                    },
                                    title = stringResource(R.string.lyrics_line_spacing),
                                    subtitle = "${lyricsLineSpacing.roundToInt()} dp",
                                    onClick = { showLyricsLineSpacingDialog = true },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
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
                                                    painterResource(id = R.drawable.lyrics),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.apple_lyrics),
                                            subtitle = stringResource(R.string.highlight_words_discretely),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = lyricsWordForWord,
                                        onCheckedChange = {
                                            onLyricsWordForWordChange(it)
                                            if (it) {
                                                onLetterByLetterAnimationChange(false)
                                            }
                                        },
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
                                                    painterResource(id = R.drawable.blur_on),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.apple_music_lyrics_blur),
                                            subtitle = stringResource(R.string.apple_music_lyrics_blur_desc),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = appleMusicLyricsBlur,
                                        onCheckedChange = onAppleMusicLyricsBlurChange,
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
                                                    painterResource(id = R.drawable.lyrics),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.letter_by_letter_animation),
                                            subtitle = stringResource(R.string.animate_lyrics_letter_by_letter),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = letterByLetterAnimation,
                                        onCheckedChange = {
                                            onLetterByLetterAnimationChange(it)
                                            if (it) {
                                                onLyricsWordForWordChange(false)
                                            }
                                        },
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            }
                        )
                    )
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
                                                painterResource(R.drawable.nav_bar),
                                                null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        title = stringResource(R.string.default_open_tab),
                                        subtitle = stringResource(R.string.select_default_tab),
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor
                                    )

                                    val options = NavigationTab.entries
                                    val labels = listOf(
                                        stringResource(R.string.home),
                                        stringResource(R.string.search),
                                        stringResource(R.string.filter_library)
                                    )

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
                                                checked = defaultOpenTab == value,
                                                onCheckedChange = { onDefaultOpenTabChange(value) },
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
                                ModernInfoItem(
                                    icon = {
                                        Icon(painterResource(R.drawable.tab), null, modifier = Modifier.size(22.dp))
                                    },
                                    title = stringResource(R.string.default_lib_chips),
                                    subtitle = when (defaultChip) {
                                        LibraryFilter.SONGS -> stringResource(R.string.songs)
                                        LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                        LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                        LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                        LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                                    },
                                    onClick = { showDefaultChipDialog = true },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
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
                                                    painterResource(R.drawable.swipe),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.swipe_song_to_add),
                                            subtitle = stringResource(R.string.swipe_add_songs_queue),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = swipeToSong,
                                        onCheckedChange = onSwipeToSongChange,
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
                                                    painterResource(R.drawable.nav_bar),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.slim_navbar),
                                            subtitle = stringResource(R.string.compact_navigation_bar),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = slimNav,
                                        onCheckedChange = onSlimNavChange,
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
                                                    painterResource(R.drawable.swipe),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.swipe_song_to_remove),
                                            subtitle = stringResource(R.string.swipe_remove_songs),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = swipeToRemoveSong,
                                        onCheckedChange = onSwipeToRemoveSongChange,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            },
                            {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    ModernInfoItem(
                                        icon = {
                                            Icon(
                                                painterResource(R.drawable.grid_view),
                                                null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        title = stringResource(R.string.grid_cell_size),
                                        subtitle = stringResource(R.string.change_size_items_library),
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor
                                    )

                                    val options = listOf(GridItemSize.SMALL, GridItemSize.BIG)
                                    val labels = listOf(stringResource(R.string.small), stringResource(R.string.big))

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
                                                checked = gridItemSize == value,
                                                onCheckedChange = { onGridItemSizeChange(value) },
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
                            }
                        )
                    )
                }

                // Auto Playlists Section
                item {
                    Text(
                        text = stringResource(R.string.auto_playlists).uppercase(),
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
                                                    painterResource(R.drawable.favorite),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.show_liked_playlist),
                                            subtitle = stringResource(R.string.display_liked_songs_playlist),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = showLikedPlaylist,
                                        onCheckedChange = onShowLikedPlaylistChange,
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
                                                    painterResource(R.drawable.offline),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.show_downloaded_playlist),
                                            subtitle = stringResource(R.string.display_downloaded_playlist),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = showDownloadedPlaylist,
                                        onCheckedChange = onShowDownloadedPlaylistChange,
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
                                                    painterResource(R.drawable.trending_up),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.show_top_playlist),
                                            subtitle = stringResource(R.string.display_top_songs_playlist),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = showTopPlaylist,
                                        onCheckedChange = onShowTopPlaylistChange,
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
                                                    painterResource(R.drawable.cached),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.show_cached_playlist),
                                            subtitle = stringResource(R.string.display_cached_playlist),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = showCachedPlaylist,
                                        onCheckedChange = onShowCachedPlaylistChange,
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
                                                    painterResource(R.drawable.backup),
                                                    null,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(R.string.show_uploaded_playlist),
                                            subtitle = stringResource(R.string.display_uploaded_playlist),
                                            iconBackgroundColor = iconBgColor,
                                            iconContentColor = iconStyleColor
                                        )
                                    }
                                    ModernSwitch(
                                        checked = showUploadedPlaylist,
                                        onCheckedChange = onShowUploadedPlaylistChange,
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
enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}

enum class LyricsVerticalPosition {
    TOP,
    CENTER,
}
