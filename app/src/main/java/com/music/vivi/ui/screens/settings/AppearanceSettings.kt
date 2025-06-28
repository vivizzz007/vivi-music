package com.music.vivi.ui.screens.settings

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.AppDesignVariantKey
import com.music.vivi.constants.AppDesignVariantType
import com.music.vivi.constants.AutoPlaylistCachedPlaylistShowKey
import com.music.vivi.constants.AutoPlaylistDownloadShowKey
import com.music.vivi.constants.AutoPlaylistLikedShowKey
import com.music.vivi.constants.AutoPlaylistLocalPlaylistShowKey
import com.music.vivi.constants.AutoPlaylistTopPlaylistShowKey
import com.music.vivi.constants.AutoPlaylistsCustomizationKey
import com.music.vivi.constants.ChipSortTypeKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.DefaultOpenTabKey
import com.music.vivi.constants.DefaultOpenTabOldKey
import com.music.vivi.constants.DynamicThemeKey
import com.music.vivi.constants.GridCellSize
import com.music.vivi.constants.GridCellSizeKey
import com.music.vivi.constants.LibraryFilter
import com.music.vivi.constants.MiniPlayerStyle
import com.music.vivi.constants.MiniPlayerStyleKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.constants.PlayerStyle
import com.music.vivi.constants.PlayerStyleKey
import com.music.vivi.constants.PureBlackKey
import com.music.vivi.constants.ShowContentFilterKey
import com.music.vivi.constants.SliderStyle
import com.music.vivi.constants.SliderStyleKey
import com.music.vivi.constants.SlimNavBarKey
import com.music.vivi.constants.SwipeSongToDismissKey
import com.music.vivi.constants.SwipeThumbnailKey
import com.music.vivi.constants.ThumbnailCornerRadiusV2Key
import com.music.vivi.ui.component.CounterDialog
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.ui.graphics.Color
import com.airbnb.lottie.compose.animateLottieCompositionAsState


@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (appDesignVariant, onAppDesignVariantChange) = rememberEnumPreference(AppDesignVariantKey, defaultValue = AppDesignVariantType.NEW)
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(DynamicThemeKey, defaultValue = false)
    val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)
    val (autoPlaylistsCustomization, onAutoPlaylistsCustomizationChange) = rememberPreference(
        AutoPlaylistsCustomizationKey, defaultValue = false)
    val (autoPlaylistLiked, onAutoPlaylistLikedChange) = rememberPreference(AutoPlaylistLikedShowKey, defaultValue = true)
    val (autoPlaylistDownload, onAutoPlaylistDownloadChange) = rememberPreference(
        AutoPlaylistDownloadShowKey, defaultValue = true)
    val (autoPlaylistTopPlaylist, onAutoPlaylistTopPlaylistChange) = rememberPreference(
        AutoPlaylistTopPlaylistShowKey, defaultValue = true)
    val (autoPlaylistCached, onAutoPlaylistCachedChange) = rememberPreference(
        AutoPlaylistCachedPlaylistShowKey, defaultValue = true)
    val (autoPlaylistLocal, onAutoPlaylistLocalChange) = rememberPreference(
        AutoPlaylistLocalPlaylistShowKey, defaultValue = true)
    val (swipeSongToDismiss, onSwipeSongToDismissChange) = rememberPreference(SwipeSongToDismissKey, defaultValue = true)
    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(SliderStyleKey, defaultValue = SliderStyle.SQUIGGLY)
    val (defaultOpenTabOld, onDefaultOpenTabOldChange) = rememberEnumPreference(DefaultOpenTabOldKey, defaultValue = NavigationTabOld.HOME)
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(DefaultOpenTabKey, defaultValue = NavigationTab.HOME)
    val (gridCellSize, onGridCellSizeChange) = rememberEnumPreference(GridCellSizeKey, defaultValue = GridCellSize.SMALL)
    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(key = ChipSortTypeKey, defaultValue = LibraryFilter.LIBRARY)
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(SwipeThumbnailKey, defaultValue = true)
    val (slimNav, onSlimNavChange) = rememberPreference(SlimNavBarKey, defaultValue = true)
    val (thumbnailCornerRadius, onThumbnailCornerRadius) = rememberPreference(ThumbnailCornerRadiusV2Key, defaultValue = 6)
    val (showContentFilter, onShowContentFilterChange) = rememberPreference(ShowContentFilterKey, defaultValue = true)
    val (playerStyle, onPlayerStyle) = rememberEnumPreference(PlayerStyleKey, defaultValue = PlayerStyle.NEW)
    val (miniPlayerStyle, onMiniPlayerStyle) = rememberEnumPreference(MiniPlayerStyleKey, defaultValue = MiniPlayerStyle.NEW)
    val (playerBackground, onPlayerBackgroundChange) = rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.BLUR)

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkMode, isSystemInDarkTheme) {
        if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
    }

    var showCornerRadiusDialog by remember { mutableStateOf(false) }
    var showSliderOptionDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(useDarkTheme) {
        if (!useDarkTheme && pureBlack) {
            onPureBlackChange(false)
        }
    }

    val settingsAnimation by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.appearence_settings))

    // Animation state
    val animationProgress by animateLottieCompositionAsState(
        composition = settingsAnimation,
        iterations = LottieConstants.IterateForever,
        speed = 1f
    )





    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = settingsAnimation,
                progress = { animationProgress },
                modifier = Modifier.size(150.dp)
            )
        }

        // Home Preferences
        PreferenceGroupTitle(title = stringResource(R.string.home1))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SwitchPreference(
                title = { Text(stringResource(R.string.show_content_filter)) },
                icon = { Icon(Icons.Rounded.FilterList, null) },
                checked = showContentFilter,
                onCheckedChange = onShowContentFilterChange
            )
        }

        // Theme Preferences
        PreferenceGroupTitle(title = stringResource(R.string.theme))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column {
                SwitchPreference(
                    title = { Text(stringResource(R.string.enable_dynamic_theme)) },
                    icon = { Icon(painterResource(R.drawable.theme_icon), null) },
                    checked = dynamicTheme,
                    onCheckedChange = onDynamicThemeChange
                )

                ListPreference(
                    title = { Text(stringResource(R.string.dark_theme)) },
                    icon = { Icon(painterResource(R.drawable.darkmode_icon), null) },
                    selectedValue = darkMode,
                    values = DarkMode.values().toList(),
                    valueText = {
                        when (it) {
                            DarkMode.ON -> stringResource(R.string.dark_theme_on)
                            DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                            DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                        }
                    },
                    onValueSelected = onDarkModeChange
                )

                AnimatedVisibility(useDarkTheme) {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.pure_black)) },
                        icon = { Icon(painterResource(R.drawable.contrast_icon), null) },
                        checked = pureBlack,
                        onCheckedChange = { checked ->
                            if (useDarkTheme) {
                                onPureBlackChange(checked)
                            }
                        }
                    )
                }
            }
        }

        // App Design Preferences
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            EnumListPreference(
                title = { Text(stringResource(R.string.app_design_variant)) },
                icon = { Icon(painterResource(R.drawable.design_icon), null) },
                selectedValue = appDesignVariant,
                onValueSelected = onAppDesignVariantChange,
                valueText = {
                    when (it) {
                        AppDesignVariantType.NEW -> stringResource(R.string.player_style_new)
                    }
                }
            )
        }

        // Player Preferences
        PreferenceGroupTitle(title = stringResource(R.string.player))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.slider_style)) },
                    description = when (sliderStyle) {
                        SliderStyle.SQUIGGLY -> stringResource(R.string.squiggly)
                        SliderStyle.COMPOSE -> stringResource(R.string.compose)
                    },
                    icon = { Icon(painterResource(R.drawable.slider_icon), null) },
                    onClick = { showSliderOptionDialog = true }
                )

                PreferenceEntry(
                    title = { Text(stringResource(R.string.thumbnail_corner_radius)) },
                    description = "$thumbnailCornerRadius%",
                    icon = { Icon(Icons.Rounded.Image, null) },
                    onClick = { showCornerRadiusDialog = true }
                )

                ListPreference(
                    title = { Text(stringResource(R.string.player_style)) },
                    icon = { Icon(painterResource(R.drawable.play_icon), null) },
                    selectedValue = playerStyle,
                    values = listOf(PlayerStyle.OLD, PlayerStyle.NEW),
                    valueText = {
                        when (it) {
                            PlayerStyle.OLD -> stringResource(R.string.player_style_old)
                            PlayerStyle.NEW -> stringResource(R.string.player_style_new)
                        }
                    },
                    onValueSelected = onPlayerStyle
                )

                ListPreference(
                    title = { Text(stringResource(R.string.mini_player_style)) },
                    icon = { Icon(painterResource(R.drawable.play), null) },
                    selectedValue = miniPlayerStyle,
                    values = listOf(MiniPlayerStyle.OLD, MiniPlayerStyle.NEW),
                    valueText = {
                        when (it) {
                            MiniPlayerStyle.OLD -> stringResource(R.string.player_style_old)
                            MiniPlayerStyle.NEW -> stringResource(R.string.player_style_new)
                        }
                    },
                    onValueSelected = onMiniPlayerStyle
                )

                EnumListPreference(
                    title = { Text(stringResource(R.string.player_background_style)) },
                    icon = { Icon(painterResource(R.drawable.music_icon), null) },
                    selectedValue = playerBackground,
                    onValueSelected = onPlayerBackgroundChange,
                    valueText = {
                        when (it) {
                            PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                            PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                            PlayerBackgroundStyle.BLURMOV -> stringResource(R.string.blurmv)
                            PlayerBackgroundStyle.BLUR -> stringResource(R.string.blur)
                            PlayerBackgroundStyle.RAINEFFECT -> stringResource(R.string.newplayer)
                        }
                    }
                )

                SwitchPreference(
                    title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
                    icon = { Icon(painterResource(R.drawable.swipe_icon), null) },
                    checked = swipeThumbnail,
                    onCheckedChange = onSwipeThumbnailChange,
                )
            }
        }

        // Misc Preferences
        PreferenceGroupTitle(title = stringResource(R.string.misc))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column {
                SwitchPreference(
                    title = { Text(stringResource(R.string.auto_playlists_customization)) },
                    icon = { Icon(painterResource(R.drawable.playlist_play), null) },
                    checked = autoPlaylistsCustomization,
                    onCheckedChange = onAutoPlaylistsCustomizationChange
                )

                AnimatedVisibility(autoPlaylistsCustomization) {
                    Column {
                        SwitchPreference(
                            title = { Text(stringResource(R.string.show_liked_auto_playlist)) },
                            icon = { Icon(Icons.Rounded.Favorite, null) },
                            checked = autoPlaylistLiked,
                            onCheckedChange = onAutoPlaylistLikedChange
                        )
                        SwitchPreference(
                            title = { Text(stringResource(R.string.show_download_auto_playlist)) },
                            icon = { Icon(Icons.Rounded.CloudDownload, null) },
                            checked = autoPlaylistDownload,
                            onCheckedChange = onAutoPlaylistDownloadChange
                        )
                        SwitchPreference(
                            title = { Text(stringResource(R.string.show_top_auto_playlist)) },
                            icon = { Icon(Icons.AutoMirrored.Rounded.TrendingUp, null) },
                            checked = autoPlaylistTopPlaylist,
                            onCheckedChange = onAutoPlaylistTopPlaylistChange
                        )
                        SwitchPreference(
                            title = { Text(stringResource(R.string.show_cached_auto_playlist)) },
                            icon = { Icon(Icons.Rounded.Cached, null) },
                            checked = autoPlaylistCached,
                            onCheckedChange = onAutoPlaylistCachedChange
                        )
                        SwitchPreference(
                            title = { Text(stringResource(R.string.show_local_auto_playlist)) },
                            icon = { Icon(Icons.Rounded.MusicNote, null) },
                            checked = autoPlaylistLocal,
                            onCheckedChange = onAutoPlaylistLocalChange
                        )
                    }
                }

                SwitchPreference(
                    title = { Text(stringResource(R.string.swipe_song_to_dismiss)) },
                    icon = { Icon(painterResource(R.drawable.queue_music), null) },
                    checked = swipeSongToDismiss,
                    onCheckedChange = onSwipeSongToDismissChange
                )

                if (appDesignVariant == AppDesignVariantType.NEW) {
                    EnumListPreference(
                        title = { Text(stringResource(R.string.default_open_tab)) },
                        icon = { Icon(painterResource(R.drawable.tab), null) },
                        selectedValue = defaultOpenTab,
                        onValueSelected = onDefaultOpenTabChange,
                        valueText = {
                            when (it) {
                                NavigationTab.HOME -> stringResource(R.string.home)
                                NavigationTab.EXPLORE -> stringResource(R.string.explore)
                                NavigationTab.SONGS -> stringResource(R.string.songs)
                                NavigationTab.ARTISTS -> stringResource(R.string.artists)
                                NavigationTab.ALBUMS -> stringResource(R.string.albums)
                                NavigationTab.PLAYLISTS -> stringResource(R.string.playlists)
                                NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                            }
                        }
                    )
                } else {
                    EnumListPreference(
                        title = { Text(stringResource(R.string.default_open_tab)) },
                        icon = { Icon(painterResource(R.drawable.tab), null) },
                        selectedValue = defaultOpenTabOld,
                        onValueSelected = onDefaultOpenTabOldChange,
                        valueText = {
                            when (it) {
                                NavigationTabOld.HOME -> stringResource(R.string.home)
                                NavigationTabOld.EXPLORE -> stringResource(R.string.explore)
                                NavigationTabOld.SONGS -> stringResource(R.string.songs)
                                NavigationTabOld.ARTISTS -> stringResource(R.string.artists)
                                NavigationTabOld.ALBUMS -> stringResource(R.string.albums)
                                NavigationTabOld.PLAYLISTS -> stringResource(R.string.playlists)
                            }
                        }
                    )
                }

                SwitchPreference(
                    title = { Text(stringResource(R.string.slim_navbar)) },
                    icon = { Icon(painterResource(R.drawable.nav_bar), null) },
                    checked = slimNav,
                    onCheckedChange = onSlimNavChange
                )

                EnumListPreference(
                    title = { Text(stringResource(R.string.grid_cell_size)) },
                    icon = { Icon(painterResource(R.drawable.grid_view), null) },
                    selectedValue = gridCellSize,
                    onValueSelected = onGridCellSizeChange,
                    valueText = {
                        when (it) {
                            GridCellSize.SMALL -> stringResource(R.string.small)
                            GridCellSize.BIG -> stringResource(R.string.big)
                        }
                    },
                )

                ListPreference(
                    title = { Text(stringResource(R.string.default_lib_chips)) },
                    icon = { Icon(painterResource(R.drawable.list), null) },
                    selectedValue = defaultChip,
                    values = listOf(
                        LibraryFilter.LIBRARY,
                        LibraryFilter.PLAYLISTS,
                        LibraryFilter.SONGS,
                        LibraryFilter.ALBUMS,
                        LibraryFilter.ARTISTS,
                    ),
                    valueText = {
                        when (it) {
                            LibraryFilter.SONGS -> stringResource(R.string.songs)
                            LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                            LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                            LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                            LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                        }
                    },
                    onValueSelected = onDefaultChipChange,
                )
            }
        }
    }

    // Dialogs
    if (showCornerRadiusDialog) {
        CounterDialog(
            title = stringResource(R.string.thumbnail_corner_radius),
            onDismiss = { showCornerRadiusDialog = false },
            initialValue = thumbnailCornerRadius,
            upperBound = 20,
            lowerBound = 0,
            resetValue = 6,
            unitDisplay = "%",
            onConfirm = {
                showCornerRadiusDialog = false
                onThumbnailCornerRadius(it)
            },
            onCancel = {
                showCornerRadiusDialog = false
            },
            onReset = { onThumbnailCornerRadius(6) },
        )
    }

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
                            if (sliderStyle == SliderStyle.COMPOSE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.COMPOSE)
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
                }
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.appearance)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.back_icon),
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

enum class DarkMode {
    ON, OFF, AUTO
}

enum class NavigationTabOld {
    HOME, EXPLORE, SONGS, ARTISTS, ALBUMS, PLAYLISTS
}

enum class NavigationTab {
    HOME, LIBRARY, EXPLORE, SONGS, ARTISTS, ALBUMS, PLAYLISTS
}

enum class LyricsPosition {
    LEFT, CENTER, RIGHT
}