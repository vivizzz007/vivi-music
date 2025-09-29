package com.music.vivi.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.ChipSortTypeKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.DefaultOpenTabKey
import com.music.vivi.constants.DynamicThemeKey
import com.music.vivi.constants.GridItemSize
import com.music.vivi.constants.GridItemsSizeKey
import com.music.vivi.constants.LibraryFilter
import com.music.vivi.constants.LyricsClickKey
import com.music.vivi.constants.LyricsScrollKey
import com.music.vivi.constants.LyricsTextPositionKey
import com.music.vivi.constants.UseNewPlayerDesignKey
import com.music.vivi.constants.UseNewMiniPlayerDesignKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.constants.PureBlackKey
import com.music.vivi.constants.PlayerButtonsStyle
import com.music.vivi.constants.PlayerButtonsStyleKey
import com.music.vivi.constants.SliderStyle
import com.music.vivi.constants.SliderStyleKey
import com.music.vivi.constants.SlimNavBarKey
import com.music.vivi.constants.ShowLikedPlaylistKey
import com.music.vivi.constants.ShowDownloadedPlaylistKey
import com.music.vivi.constants.ShowTopPlaylistKey
import com.music.vivi.constants.ShowCachedPlaylistKey
import com.music.vivi.constants.ShowUploadedPlaylistKey
import com.music.vivi.constants.SwipeThumbnailKey
import com.music.vivi.constants.SwipeSensitivityKey
import com.music.vivi.constants.SwipeToSongKey
import com.music.vivi.constants.HidePlayerThumbnailKey
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.EnumListPreference
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.ListPreference
import com.music.vivi.ui.component.PlayerSliderTrack
import com.music.vivi.ui.component.PreferenceEntry
import com.music.vivi.ui.component.PreferenceGroupTitle
import com.music.vivi.ui.component.SwitchPreference
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import me.saket.squiggles.SquigglySlider
import kotlin.math.roundToInt




import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.music.vivi.update.experiment.SettingsListItem
import com.music.vivi.update.experiment.SheetDragHandle
import com.music.vivi.update.mordernswitch.ModernSwitch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(
        DynamicThemeKey,
        defaultValue = true
    )
    val (darkMode, onDarkModeChange) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO
    )
    val (useNewPlayerDesign, onUseNewPlayerDesignChange) = rememberPreference(
        UseNewPlayerDesignKey,
        defaultValue = true
    )
    val (useNewMiniPlayerDesign, onUseNewMiniPlayerDesignChange) = rememberPreference(
        UseNewMiniPlayerDesignKey,
        defaultValue = true
    )
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(
        HidePlayerThumbnailKey,
        defaultValue = false
    )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.GRADIENT,
        )
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)
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

    val availableBackgroundStyles = PlayerBackgroundStyle.entries.filter {
        it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme =
        remember(darkMode, isSystemInDarkTheme) {
            if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
        }

    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(
        key = ChipSortTypeKey,
        defaultValue = LibraryFilter.LIBRARY
    )

    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }
    var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }

    // Bottom sheet states
    val darkModeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDarkModeSheet by remember { mutableStateOf(false) }

    val playerBackgroundSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPlayerBackgroundSheet by remember { mutableStateOf(false) }

    val playerButtonsStyleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPlayerButtonsStyleSheet by remember { mutableStateOf(false) }

    val lyricsPositionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showLyricsPositionSheet by remember { mutableStateOf(false) }

    val defaultOpenTabSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDefaultOpenTabSheet by remember { mutableStateOf(false) }

    val gridItemSizeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showGridItemSizeSheet by remember { mutableStateOf(false) }

    val defaultChipSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDefaultChipSheet by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Calculate scroll-based animations
    val titleAlpha by remember {
        derivedStateOf {
            1f - (scrollState.value / 200f).coerceIn(0f, 1f)
        }
    }

    val titleScale by remember {
        derivedStateOf {
            1f - (scrollState.value / 400f).coerceIn(0f, 0.3f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                // Header Section with scroll animations
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .graphicsLayer {
                            alpha = titleAlpha
                            scaleX = titleScale
                            scaleY = titleScale
                        }
                ) {
                    Text(
                        text = stringResource(R.string.appearance),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            item {
                // Lottie Animation Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.appearencegaming))
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                SettingsSection(stringResource(R.string.theme)) {
                    SettingsListItem(
                        title = stringResource(R.string.enable_dynamic_theme),
                        subtitle = if (dynamicTheme) "Enabled" else "Disabled",
                        onClick = { onDynamicThemeChange(!dynamicTheme) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.palette),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = dynamicTheme,
                                onCheckedChange = onDynamicThemeChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.dark_theme),
                        subtitle = when (darkMode) {
                            DarkMode.ON -> stringResource(R.string.dark_theme_on)
                            DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                            DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                        },
                        onClick = { showDarkModeSheet = true },
                        isLast = !useDarkTheme,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.dark_mode),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    if (useDarkTheme) {
                        SettingsListItem(
                            title = stringResource(R.string.pure_black),
                            subtitle = if (pureBlack) "Enabled" else "Disabled",
                            onClick = { onPureBlackChange(!pureBlack) },
                            isLast = true,
                            leadingContent = {
                                Icon(
                                    painterResource(R.drawable.contrast),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            },
                            trailingContent = {
                                ModernSwitch(
                                    checked = pureBlack,
                                    onCheckedChange = onPureBlackChange
                                )
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.player)) {
                    SettingsListItem(
                        title = stringResource(R.string.player_background_style),
                        subtitle = when (playerBackground) {
                            PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                            PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                            PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                        },
                        onClick = { showPlayerBackgroundSheet = true },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.gradient),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.hide_player_thumbnail),
                        subtitle = stringResource(R.string.hide_player_thumbnail_desc),
                        onClick = { onHidePlayerThumbnailChange(!hidePlayerThumbnail) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.history),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = hidePlayerThumbnail,
                                onCheckedChange = onHidePlayerThumbnailChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.player_buttons_style),
                        subtitle = when (playerButtonsStyle) {
                            PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                            PlayerButtonsStyle.SECONDARY -> stringResource(R.string.secondary_color_style)
                        },
                        onClick = { showPlayerButtonsStyleSheet = true },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.palette),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.player_slider_style),
                        subtitle = when (sliderStyle) {
                            SliderStyle.DEFAULT -> stringResource(R.string.default_)
                            SliderStyle.SQUIGGLY -> stringResource(R.string.squiggly)
                            SliderStyle.SLIM -> stringResource(R.string.slim)
                        },
                        onClick = { showSliderOptionDialog = true },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.sliders),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.enable_swipe_thumbnail),
                        subtitle = if (swipeThumbnail) "Enabled" else "Disabled",
                        onClick = { onSwipeThumbnailChange(!swipeThumbnail) },
                        isLast = !swipeThumbnail,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.swipe),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = swipeThumbnail,
                                onCheckedChange = onSwipeThumbnailChange
                            )
                        }
                    )

                    // Use AnimatedVisibility for conditional composable content
                    AnimatedVisibility(visible = swipeThumbnail) {
                        Column {
                            SettingsListItem(
                                title = stringResource(R.string.swipe_sensitivity),
                                subtitle = "${(swipeSensitivity * 100).roundToInt()}%",
                                onClick = {
                                    tempSensitivity = swipeSensitivity // Reset to current value when opening
                                    showSensitivityDialog = true
                                },
                                isLast = true,
                                leadingContent = {
                                    Icon(
                                        painterResource(R.drawable.tune),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.lyrics)) {
                    SettingsListItem(
                        title = stringResource(R.string.lyrics_text_position),
                        subtitle = when (lyricsPosition) {
                            LyricsPosition.LEFT -> stringResource(R.string.left)
                            LyricsPosition.CENTER -> stringResource(R.string.center)
                            LyricsPosition.RIGHT -> stringResource(R.string.right)
                        },
                        onClick = { showLyricsPositionSheet = true },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.lyrics),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.lyrics_click_change),
                        subtitle = if (lyricsClick) "Enabled" else "Disabled",
                        onClick = { onLyricsClickChange(!lyricsClick) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.lyrics),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = lyricsClick,
                                onCheckedChange = onLyricsClickChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.lyrics_auto_scroll),
                        subtitle = if (lyricsScroll) "Enabled" else "Disabled",
                        onClick = { onLyricsScrollChange(!lyricsScroll) },
                        isLast = true,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.lyrics),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = lyricsScroll,
                                onCheckedChange = onLyricsScrollChange
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.misc)) {
                    SettingsListItem(
                        title = stringResource(R.string.default_open_tab),
                        subtitle = when (defaultOpenTab) {
                            NavigationTab.HOME -> stringResource(R.string.home)
                            NavigationTab.SEARCH -> stringResource(R.string.search)
                            NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                        },
                        onClick = { showDefaultOpenTabSheet = true },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.nav_bar),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.default_lib_chips),
                        subtitle = when (defaultChip) {
                            LibraryFilter.SONGS -> stringResource(R.string.songs)
                            LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                            LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                            LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                            LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                        },
                        onClick = { showDefaultChipSheet = true },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.tab),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.swipe_song_to_add),
                        subtitle = if (swipeToSong) "Enabled" else "Disabled",
                        onClick = { onSwipeToSongChange(!swipeToSong) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.swipe),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = swipeToSong,
                                onCheckedChange = onSwipeToSongChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.slim_navbar),
                        subtitle = if (slimNav) "Enabled" else "Disabled",
                        onClick = { onSlimNavChange(!slimNav) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.nav_bar),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = slimNav,
                                onCheckedChange = onSlimNavChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.grid_cell_size),
                        subtitle = when (gridItemSize) {
                            GridItemSize.BIG -> stringResource(R.string.big)
                            GridItemSize.SMALL -> stringResource(R.string.small)
                        },
                        onClick = { showGridItemSizeSheet = true },
                        isLast = true,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.grid_view),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.auto_playlists)) {
                    SettingsListItem(
                        title = stringResource(R.string.show_liked_playlist),
                        subtitle = if (showLikedPlaylist) "Shown" else "Hidden",
                        onClick = { onShowLikedPlaylistChange(!showLikedPlaylist) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.favorite),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = showLikedPlaylist,
                                onCheckedChange = onShowLikedPlaylistChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.show_downloaded_playlist),
                        subtitle = if (showDownloadedPlaylist) "Shown" else "Hidden",
                        onClick = { onShowDownloadedPlaylistChange(!showDownloadedPlaylist) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.offline),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = showDownloadedPlaylist,
                                onCheckedChange = onShowDownloadedPlaylistChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.show_top_playlist),
                        subtitle = if (showTopPlaylist) "Shown" else "Hidden",
                        onClick = { onShowTopPlaylistChange(!showTopPlaylist) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.trending_up),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = showTopPlaylist,
                                onCheckedChange = onShowTopPlaylistChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.show_cached_playlist),
                        subtitle = if (showCachedPlaylist) "Shown" else "Hidden",
                        onClick = { onShowCachedPlaylistChange(!showCachedPlaylist) },
                        isLast = true,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.cached),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = showCachedPlaylist,
                                onCheckedChange = onShowCachedPlaylistChange
                            )
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }

    if (showSensitivityDialog) {
        DefaultDialog(
            onDismiss = {
                tempSensitivity = swipeSensitivity
                showSensitivityDialog = false
            },
            buttons = {
                TextButton(
                    onClick = {
                        tempSensitivity = 0.73f
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        tempSensitivity = swipeSensitivity
                        showSensitivityDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onSwipeSensitivityChange(tempSensitivity)
                        showSensitivityDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        ) {
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
                    text = stringResource(R.string.sensitivity_percentage, (tempSensitivity * 100).roundToInt()),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Slider(
                    value = tempSensitivity,
                    onValueChange = { tempSensitivity = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Dark Mode Bottom Sheet
    if (showDarkModeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDarkModeSheet = false },
            sheetState = darkModeSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.dark_theme),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose your dark theme preference",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                DarkMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDarkModeChange(mode)
                                showDarkModeSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = darkMode == mode,
                            onClick = {
                                onDarkModeChange(mode)
                                showDarkModeSheet = false
                            }
                        )
                        Text(
                            text = when (mode) {
                                DarkMode.ON -> stringResource(R.string.dark_theme_on)
                                DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                                DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Player Background Style Bottom Sheet
    if (showPlayerBackgroundSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPlayerBackgroundSheet = false },
            sheetState = playerBackgroundSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.player_background_style),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose the background style for the player",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                availableBackgroundStyles.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPlayerBackgroundChange(style)
                                showPlayerBackgroundSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = playerBackground == style,
                            onClick = {
                                onPlayerBackgroundChange(style)
                                showPlayerBackgroundSheet = false
                            }
                        )
                        Text(
                            text = when (style) {
                                PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.default_)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Player Buttons Style Bottom Sheet
    if (showPlayerButtonsStyleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPlayerButtonsStyleSheet = false },
            sheetState = playerButtonsStyleSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.player_buttons_style),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose the style for player buttons",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                PlayerButtonsStyle.values().forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPlayerButtonsStyleChange(style)
                                showPlayerButtonsStyleSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = playerButtonsStyle == style,
                            onClick = {
                                onPlayerButtonsStyleChange(style)
                                showPlayerButtonsStyleSheet = false
                            }
                        )
                        Text(
                            text = when (style) {
                                PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                                PlayerButtonsStyle.SECONDARY -> stringResource(R.string.secondary_color_style)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Lyrics Position Bottom Sheet
    if (showLyricsPositionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLyricsPositionSheet = false },
            sheetState = lyricsPositionSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.lyrics_text_position),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose the position for lyrics text",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                LyricsPosition.values().forEach { position ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLyricsPositionChange(position)
                                showLyricsPositionSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = lyricsPosition == position,
                            onClick = {
                                onLyricsPositionChange(position)
                                showLyricsPositionSheet = false
                            }
                        )
                        Text(
                            text = when (position) {
                                LyricsPosition.LEFT -> stringResource(R.string.left)
                                LyricsPosition.CENTER -> stringResource(R.string.center)
                                LyricsPosition.RIGHT -> stringResource(R.string.right)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Default Open Tab Bottom Sheet
    if (showDefaultOpenTabSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDefaultOpenTabSheet = false },
            sheetState = defaultOpenTabSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.default_open_tab),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose the default tab to open when launching the app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                NavigationTab.values().forEach { tab ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDefaultOpenTabChange(tab)
                                showDefaultOpenTabSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = defaultOpenTab == tab,
                            onClick = {
                                onDefaultOpenTabChange(tab)
                                showDefaultOpenTabSheet = false
                            }
                        )
                        Text(
                            text = when (tab) {
                                NavigationTab.HOME -> stringResource(R.string.home)
                                NavigationTab.SEARCH -> stringResource(R.string.search)
                                NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Grid Item Size Bottom Sheet
    if (showGridItemSizeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGridItemSizeSheet = false },
            sheetState = gridItemSizeSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.grid_cell_size),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose the size for grid items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                GridItemSize.values().forEach { size ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onGridItemSizeChange(size)
                                showGridItemSizeSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = gridItemSize == size,
                            onClick = {
                                onGridItemSizeChange(size)
                                showGridItemSizeSheet = false
                            }
                        )
                        Text(
                            text = when (size) {
                                GridItemSize.BIG -> stringResource(R.string.big)
                                GridItemSize.SMALL -> stringResource(R.string.small)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Default Chip Bottom Sheet
    if (showDefaultChipSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDefaultChipSheet = false },
            sheetState = defaultChipSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.default_lib_chips),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose the default library filter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                listOf(
                    LibraryFilter.LIBRARY,
                    LibraryFilter.PLAYLISTS,
                    LibraryFilter.SONGS,
                    LibraryFilter.ALBUMS,
                    LibraryFilter.ARTISTS
                ).forEach { filter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDefaultChipChange(filter)
                                showDefaultChipSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = defaultChip == filter,
                            onClick = {
                                onDefaultChipChange(filter)
                                showDefaultChipSheet = false
                            }
                        )
                        Text(
                            text = when (filter) {
                                LibraryFilter.SONGS -> stringResource(R.string.songs)
                                LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Slider Style Dialog
    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            }
        ) {
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
                            if (sliderStyle == SliderStyle.DEFAULT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.DEFAULT)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier.weight(1f)
                    )
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
                            if (sliderStyle == SliderStyle.SQUIGGLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.SQUIGGLY)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    SquigglySlider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.squiggly),
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
                            if (sliderStyle == SliderStyle.SLIM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.SLIM)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = SliderDefaults.colors()
                            );
                        },
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {}
                                )
                            }
                    )

                    Text(
                        text = stringResource(R.string.slim),
                        style = MaterialTheme.typography.labelLarge
                    )
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



