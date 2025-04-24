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
import androidx.compose.material.icons.rounded.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.AppDesignVariantKey
import com.music.vivi.constants.AppDesignVariantType
import com.music.vivi.constants.ChipSortTypeKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.DefaultOpenTabKey
import com.music.vivi.constants.DefaultOpenTabOldKey
import com.music.vivi.constants.DynamicThemeKey
import com.music.vivi.constants.GridCellSize
import com.music.vivi.constants.GridCellSizeKey
import com.music.vivi.constants.LibraryFilter
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.constants.PlayerStyle
import com.music.vivi.constants.PlayerStyleKey
import com.music.vivi.constants.PureBlackKey
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
import com.music.vivi.ui.component.PreferenceEntry
import com.music.vivi.ui.component.PreferenceGroupTitle
import com.music.vivi.ui.component.SwitchPreference
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import me.saket.squiggles.SquigglySlider

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
    val (swipeSongToDismiss, onSwipeSongToDismissChange) = rememberPreference(SwipeSongToDismissKey, defaultValue = true)
    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(SliderStyleKey, defaultValue = SliderStyle.DEFAULT)
    val (defaultOpenTabOld, onDefaultOpenTabOldChange) = rememberEnumPreference(DefaultOpenTabOldKey, defaultValue = NavigationTabOld.HOME)
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(DefaultOpenTabKey, defaultValue = NavigationTab.HOME)
    val (gridCellSize, onGridCellSizeChange) = rememberEnumPreference(GridCellSizeKey, defaultValue = GridCellSize.SMALL)
    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(key = ChipSortTypeKey, defaultValue = LibraryFilter.LIBRARY)
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(SwipeThumbnailKey, defaultValue = true)
    val (slimNav, onSlimNavChange) = rememberPreference(SlimNavBarKey, defaultValue = true)
    val (thumbnailCornerRadius, onThumbnailCornerRadius) = rememberPreference (ThumbnailCornerRadiusV2Key , defaultValue = 6)
    val (playerStyle, onPlayerStyle) = rememberEnumPreference (PlayerStyleKey , defaultValue = PlayerStyle.NEW)

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkMode, isSystemInDarkTheme) {
        if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
    }

    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.BLUR,
        )


    var showCornerRadiusDialog by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showCornerRadiusDialog) {
            CounterDialog(
                title = stringResource(R.string.thumbnail_corner_radius),
                onDismiss = { showCornerRadiusDialog = false },
                initialValue = thumbnailCornerRadius,
                upperBound = 10,
                lowerBound = 0,
                unitDisplay = "0%",
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
    }

    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
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

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            visible = true
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {

            Image(
                painter = painterResource(id = R.drawable.appearence_box),
                contentDescription = stringResource(R.string.appearenceimg),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }




        PreferenceGroupTitle(
            title = stringResource(R.string.theme)
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_dynamic_theme)) },
            icon = { Icon(painterResource(R.drawable.theme_icon), null) },
            checked = dynamicTheme,
            onCheckedChange = onDynamicThemeChange
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.dark_theme)) },
            icon = { Icon(painterResource(R.drawable.darkmode_icon), null) },
            selectedValue = darkMode,
            onValueSelected = onDarkModeChange,
            valueText = {
                when (it) {
                    DarkMode.ON -> stringResource(R.string.dark_theme_on)
                    DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                    DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                }
            }
        )

        AnimatedVisibility(useDarkTheme) {
            SwitchPreference(
                title = { Text(stringResource(R.string.pure_black)) },
                icon = { Icon(painterResource(R.drawable.contrast_icon), null) },
                checked = pureBlack,
                onCheckedChange = onPureBlackChange
            )
        }

        EnumListPreference(
            title = { Text(stringResource(R.string.app_design_variant)) },
            icon =
                { Icon(painterResource(R.drawable.design_icon),null)
                   },
            selectedValue = appDesignVariant,
            onValueSelected = onAppDesignVariantChange,
            valueText = {
                when (it) {
                    AppDesignVariantType.NEW -> stringResource(R.string.player_style_new)
                    AppDesignVariantType.OLD -> stringResource(R.string.player_style_old)
                }
            }
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.player)
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


        EnumListPreference(
            title = { Text(stringResource(R.string.player_background_style)) },
            icon = { Icon(painterResource(R.drawable.music_icon), null) },
            selectedValue = playerBackground,
            onValueSelected = onPlayerBackgroundChange,
            valueText = {
                when (it) {
                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)

                    PlayerBackgroundStyle.BLURMOV ->  stringResource(R.string.blurmv)
                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.blur)
                    PlayerBackgroundStyle.MONETBLACK -> stringResource(R.string.monetblack)
                }
            }
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
            icon = { Icon(painterResource(R.drawable.swipe_icon), null) },
            checked = swipeThumbnail,
            onCheckedChange = onSwipeThumbnailChange,
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.thumbnail_corner_radius)) },
            description = "$thumbnailCornerRadius" + "0%",
            icon = { Icon(Icons.Rounded.Image, null) },
            onClick = { showCornerRadiusDialog = true }
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.player_slider_style)) },
            description = when (sliderStyle) {

                SliderStyle.DEFAULT -> stringResource(R.string.default_)
                SliderStyle.COMPOSE -> stringResource(R.string.compose)
            },
            icon = { Icon(painterResource(R.drawable.slider_icon), null) },
            onClick = {
                showSliderOptionDialog = true
            }
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.misc)
        )

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
            values =
            listOf(
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
    HOME, EXPLORE ,SONGS, ARTISTS, ALBUMS, PLAYLISTS
}

enum class NavigationTab {
     HOME, LIBRARY, EXPLORE ,SONGS, ARTISTS, ALBUMS, PLAYLISTS
}

enum class LyricsPosition {
    LEFT, CENTER, RIGHT
}