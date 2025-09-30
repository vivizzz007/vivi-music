package com.music.vivi.ui.screens.settings


import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.ChipSortTypeKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.DefaultOpenTabKey
import com.music.vivi.constants.DynamicThemeKey
import com.music.vivi.constants.GridItemSize
import com.music.vivi.constants.GridItemsSizeKey
import com.music.vivi.constants.HidePlayerThumbnailKey
import com.music.vivi.constants.LibraryFilter
import com.music.vivi.constants.LyricsClickKey
import com.music.vivi.constants.LyricsScrollKey
import com.music.vivi.constants.LyricsTextPositionKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.constants.PlayerButtonsStyle
import com.music.vivi.constants.PlayerButtonsStyleKey
import com.music.vivi.constants.PureBlackKey
import com.music.vivi.constants.ShowCachedPlaylistKey
import com.music.vivi.constants.ShowDownloadedPlaylistKey
import com.music.vivi.constants.ShowLikedPlaylistKey
import com.music.vivi.constants.ShowTopPlaylistKey
import com.music.vivi.constants.ShowUploadedPlaylistKey
import com.music.vivi.constants.SliderStyle
import com.music.vivi.constants.SliderStyleKey
import com.music.vivi.constants.SlimNavBarKey
import com.music.vivi.constants.SwipeSensitivityKey
import com.music.vivi.constants.SwipeThumbnailKey
import com.music.vivi.constants.SwipeToSongKey
import com.music.vivi.constants.UseNewMiniPlayerDesignKey
import com.music.vivi.constants.UseNewPlayerDesignKey
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.PlayerSliderTrack
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import me.saket.squiggles.SquigglySlider
import kotlin.math.roundToInt


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
            defaultValue = PlayerBackgroundStyle.DEFAULT,
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
    val (showUploadedPlaylist, onShowUploadedPlaylistChange) = rememberPreference(
        ShowUploadedPlaylistKey,
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
                    title = {
//                        Text(stringResource(R.string.appearance))
                    },
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
                            text = "Appearance Settings",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Customize the look and feel of the app",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Theme Section
                item {
                    Text(
                        text = "THEME",
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
                            // Theme Settings Navigation Item
                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.palette), null, modifier = Modifier.size(22.dp)) },
                                title = "Theme Settings",
                                subtitle = "Customize dark mode, dynamic theme, and more",
                                onClick = {
                                    navController.navigate("themeSettings")
                                },
                                showArrow = true
                            )
                        }
                    }
                }

                // Player Section
                item {
                    Text(
                        text = "PLAYER",
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
                            // New Player Design
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.palette), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.new_player_design),
                                        subtitle = stringResource(R.string.new_player_design_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = useNewPlayerDesign,
                                    onCheckedChange = onUseNewPlayerDesignChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Player Background Style - Navigates to separate screen
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.gradient), null, modifier = Modifier.size(22.dp)) },
                                title = stringResource(R.string.player_background_style),
                                subtitle = when (playerBackground) {
                                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                },
                                onClick = {
                                    navController.navigate("playerBackgroundStyle")
                                },
                                showArrow = true
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Hide Player Thumbnail
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.hide_image), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.hide_player_thumbnail),
                                        subtitle = stringResource(R.string.hide_player_thumbnail_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = hidePlayerThumbnail,
                                    onCheckedChange = onHidePlayerThumbnailChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Player Buttons Style with Dialog
                            var showButtonsStyleDialog by remember { mutableStateOf(false) }
                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.palette), null, modifier = Modifier.size(22.dp)) },
                                title = stringResource(R.string.player_buttons_style),
                                subtitle = when (playerButtonsStyle) {
                                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                                    PlayerButtonsStyle.SECONDARY -> stringResource(R.string.secondary_color_style)
                                },
                                onClick = { showButtonsStyleDialog = true },
                                showArrow = true
                            )

                            if (showButtonsStyleDialog) {
                                AlertDialog(
                                    onDismissRequest = { showButtonsStyleDialog = false },
                                    title = { Text(stringResource(R.string.player_buttons_style)) },
                                    text = {
                                        Column {
                                            enumValues<PlayerButtonsStyle>().forEach { value ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            onPlayerButtonsStyleChange(value)
                                                            showButtonsStyleDialog = false
                                                        }
                                                        .padding(vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = value == playerButtonsStyle,
                                                        onClick = {
                                                            onPlayerButtonsStyleChange(value)
                                                            showButtonsStyleDialog = false
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(when (value) {
                                                        PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                                                        PlayerButtonsStyle.SECONDARY -> stringResource(R.string.secondary_color_style)
                                                    })
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showButtonsStyleDialog = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }

                            // Player Slider Style - Navigates to separate screen
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.sliders), null, modifier = Modifier.size(22.dp)) },
                                title = stringResource(R.string.player_slider_style),
                                subtitle = when (sliderStyle) {
                                    SliderStyle.DEFAULT -> stringResource(R.string.default_)
                                    SliderStyle.SQUIGGLY -> stringResource(R.string.squiggly)
                                    SliderStyle.SLIM -> stringResource(R.string.slim)
                                },
                                onClick = {
                                    navController.navigate("playerSliderStyle")
                                },
                                showArrow = true
                            )

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
                                                    )
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

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Swipe Thumbnail
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.swipe), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.enable_swipe_thumbnail),
                                        subtitle = stringResource(R.string.enable_swipe_thumbnail_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = swipeThumbnail,
                                    onCheckedChange = onSwipeThumbnailChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            // Swipe Sensitivity (only shown when swipe thumbnail is enabled)
                            AnimatedVisibility(swipeThumbnail) {
                                Column {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )

                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.tune), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.swipe_sensitivity),
                                        subtitle = stringResource(R.string.sensitivity_percentage, (swipeSensitivity * 100).roundToInt()),
                                        onClick = { showSensitivityDialog = true },
                                        showArrow = true
                                    )

                                    if (showSensitivityDialog) {
                                        var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }

                                        AlertDialog(
                                            onDismissRequest = {
                                                tempSensitivity = swipeSensitivity
                                                showSensitivityDialog = false
                                            },
                                            title = { Text(stringResource(R.string.swipe_sensitivity)) },
                                            text = {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(16.dp)
                                                ) {
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
                                            },
                                            confirmButton = {
                                                Row {
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
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Lyrics Section
                item {
                    Text(
                        text = "LYRICS",
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
                            // Lyrics Position with Dialog
                            var showLyricsPositionDialog by remember { mutableStateOf(false) }
                            // Lyrics Position - Navigates to separate screen
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.lyrics), null, modifier = Modifier.size(22.dp)) },
                                title = stringResource(R.string.lyrics_text_position),
                                subtitle = when (lyricsPosition) {
                                    LyricsPosition.LEFT -> stringResource(R.string.left)
                                    LyricsPosition.CENTER -> stringResource(R.string.center)
                                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                                },
                                onClick = {
                                    navController.navigate("lyricsPosition")
                                },
                                showArrow = true
                            )

                            if (showLyricsPositionDialog) {
                                AlertDialog(
                                    onDismissRequest = { showLyricsPositionDialog = false },
                                    title = { Text(stringResource(R.string.lyrics_text_position)) },
                                    text = {
                                        Column {
                                            enumValues<LyricsPosition>().forEach { value ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            onLyricsPositionChange(value)
                                                            showLyricsPositionDialog = false
                                                        }
                                                        .padding(vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = value == lyricsPosition,
                                                        onClick = {
                                                            onLyricsPositionChange(value)
                                                            showLyricsPositionDialog = false
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(when (value) {
                                                        LyricsPosition.LEFT -> stringResource(R.string.left)
                                                        LyricsPosition.CENTER -> stringResource(R.string.center)
                                                        LyricsPosition.RIGHT -> stringResource(R.string.right)
                                                    })
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showLyricsPositionDialog = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Lyrics Click Change
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.lyrics), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.lyrics_click_change),
                                        subtitle = stringResource(R.string.lyrics_click_change_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = lyricsClick,
                                    onCheckedChange = onLyricsClickChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Lyrics Auto Scroll
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.lyrics), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.lyrics_auto_scroll),
                                        subtitle = stringResource(R.string.lyrics_auto_scroll_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = lyricsScroll,
                                    onCheckedChange = onLyricsScrollChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }
                        }
                    }
                }

                // Interface Section
                item {
                    Text(
                        text = "INTERFACE",
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
                            // Default Open Tab with Dialog
                            var showTabDialog by remember { mutableStateOf(false) }
                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.nav_bar), null, modifier = Modifier.size(22.dp)) },
                                title = stringResource(R.string.default_open_tab),
                                subtitle = when (defaultOpenTab) {
                                    NavigationTab.HOME -> stringResource(R.string.home)
                                    NavigationTab.SEARCH -> stringResource(R.string.search)
                                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                                },
                                onClick = { showTabDialog = true },
                                showArrow = true
                            )

                            if (showTabDialog) {
                                AlertDialog(
                                    onDismissRequest = { showTabDialog = false },
                                    title = { Text(stringResource(R.string.default_open_tab)) },
                                    text = {
                                        Column {
                                            enumValues<NavigationTab>().forEach { value ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            onDefaultOpenTabChange(value)
                                                            showTabDialog = false
                                                        }
                                                        .padding(vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = value == defaultOpenTab,
                                                        onClick = {
                                                            onDefaultOpenTabChange(value)
                                                            showTabDialog = false
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(when (value) {
                                                        NavigationTab.HOME -> stringResource(R.string.home)
                                                        NavigationTab.SEARCH -> stringResource(R.string.search)
                                                        NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                                                    })
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showTabDialog = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Default Library Chip with Dialog
                            var showChipDialog by remember { mutableStateOf(false) }
                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.tab), null, modifier = Modifier.size(22.dp)) },
                                title = stringResource(R.string.default_lib_chips),
                                subtitle = when (defaultChip) {
                                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                                },
                                onClick = { showChipDialog = true },
                                showArrow = true
                            )

                            if (showChipDialog) {
                                AlertDialog(
                                    onDismissRequest = { showChipDialog = false },
                                    title = { Text(stringResource(R.string.default_lib_chips)) },
                                    text = {
                                        Column {
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
                                                            showChipDialog = false
                                                        }
                                                        .padding(vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = value == defaultChip,
                                                        onClick = {
                                                            onDefaultChipChange(value)
                                                            showChipDialog = false
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(when (value) {
                                                        LibraryFilter.SONGS -> stringResource(R.string.songs)
                                                        LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                                        LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                                        LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                                        LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                                                    })
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showChipDialog = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Swipe Song to Add
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.swipe), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.swipe_song_to_add),
                                        subtitle = stringResource(R.string.swipe_song_to_add_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = swipeToSong,
                                    onCheckedChange = onSwipeToSongChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Slim Navigation Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.nav_bar), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.slim_navbar),
                                        subtitle = stringResource(R.string.slim_navbar_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = slimNav,
                                    onCheckedChange = onSlimNavChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Grid Item Size with Dialog
                            var showGridSizeDialog by remember { mutableStateOf(false) }
                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.grid_view), null, modifier = Modifier.size(22.dp)) },
                                title = stringResource(R.string.grid_cell_size),
                                subtitle = when (gridItemSize) {
                                    GridItemSize.BIG -> stringResource(R.string.big)
                                    GridItemSize.SMALL -> stringResource(R.string.small)
                                },
                                onClick = { showGridSizeDialog = true },
                                showArrow = true
                            )

                            if (showGridSizeDialog) {
                                AlertDialog(
                                    onDismissRequest = { showGridSizeDialog = false },
                                    title = { Text(stringResource(R.string.grid_cell_size)) },
                                    text = {
                                        Column {
                                            enumValues<GridItemSize>().forEach { value ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            onGridItemSizeChange(value)
                                                            showGridSizeDialog = false
                                                        }
                                                        .padding(vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = value == gridItemSize,
                                                        onClick = {
                                                            onGridItemSizeChange(value)
                                                            showGridSizeDialog = false
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(when (value) {
                                                        GridItemSize.BIG -> stringResource(R.string.big)
                                                        GridItemSize.SMALL -> stringResource(R.string.small)
                                                    })
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showGridSizeDialog = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Auto Playlists Section
                item {
                    Text(
                        text = "AUTO PLAYLISTS",
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
                            // Show Liked Playlist
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.favorite), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.show_liked_playlist),
                                        subtitle = stringResource(R.string.show_liked_playlist_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = showLikedPlaylist,
                                    onCheckedChange = onShowLikedPlaylistChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Show Downloaded Playlist
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.offline), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.show_downloaded_playlist),
                                        subtitle = stringResource(R.string.show_downloaded_playlist_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = showDownloadedPlaylist,
                                    onCheckedChange = onShowDownloadedPlaylistChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Show Top Playlist
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.trending_up), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.show_top_playlist),
                                        subtitle = stringResource(R.string.show_top_playlist_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = showTopPlaylist,
                                    onCheckedChange = onShowTopPlaylistChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Show Cached Playlist
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.cached), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.show_cached_playlist),
                                        subtitle = stringResource(R.string.show_cached_playlist_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = showCachedPlaylist,
                                    onCheckedChange = onShowCachedPlaylistChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Show Uploaded Playlist
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.backup), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.show_uploaded_playlist),
                                        subtitle = stringResource(R.string.show_uploaded_playlist_desc)
                                    )
                                }
                                ModernSwitch(
                                    checked = showUploadedPlaylist,
                                    onCheckedChange = onShowUploadedPlaylistChange,
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



