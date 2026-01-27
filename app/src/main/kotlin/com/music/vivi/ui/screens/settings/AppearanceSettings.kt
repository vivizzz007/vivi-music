package com.music.vivi.ui.screens.settings

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.*
import com.music.vivi.playback.MusicService
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.PlayerSliderTrack
import com.music.vivi.ui.component.RoundedCheckbox
import com.music.vivi.ui.component.WavySlider
import com.music.vivi.ui.theme.DefaultThemeColor
import com.music.vivi.ui.theme.ShapeStyle
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.Material3ExpressiveSettingsGroup
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.update.widget.MusicPlayerWidgetReceiver
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import kotlin.math.roundToInt

private enum class AppearanceScreenType {
    MAIN, THEME, PLAYER, LYRICS, UI
}



@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalAnimationApi::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var currentScreen by rememberSaveable { mutableStateOf(AppearanceScreenType.MAIN) }

    // Common Preferences (lifted up for access)
    val (settingsShapeTertiary, _) = rememberPreference(SettingsShapeColorTertiaryKey, false)
    val (darkMode, _) = rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkMode, isSystemInDarkTheme) {
        if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
    }

    val (iconBgColor, iconStyleColor) = if (settingsShapeTertiary) {
        if (useDarkTheme) {
            Pair(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
        } else {
            Pair(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        }
    } else {
        Pair(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), MaterialTheme.colorScheme.primary)
    }

    // Handle Back Navigation
    BackHandler(enabled = currentScreen != AppearanceScreenType.MAIN) {
        currentScreen = AppearanceScreenType.MAIN
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (currentScreen != AppearanceScreenType.MAIN) {
                        Text(
                           text = when (currentScreen) {
                               AppearanceScreenType.THEME -> stringResource(R.string.theme)
                               AppearanceScreenType.PLAYER -> stringResource(R.string.player_design)
                               AppearanceScreenType.LYRICS -> stringResource(R.string.lyrics)
                               AppearanceScreenType.UI -> stringResource(R.string.ui_interface)
                               else -> ""
                           },
                           style = MaterialTheme.typography.titleLarge,
                           fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentScreen != AppearanceScreenType.MAIN) {
                                currentScreen = AppearanceScreenType.MAIN
                            } else {
                                onBack?.invoke() ?: navController.navigateUp()
                            }
                        },
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState != AppearanceScreenType.MAIN) {
                    slideInHorizontally { it } + fadeIn() with slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() with slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "AppearanceNavigation",
            modifier = Modifier.padding(paddingValues)
        ) { screen ->
            when (screen) {
                AppearanceScreenType.MAIN -> AppearanceMainScreen(
                    iconBgColor = iconBgColor,
                    iconStyleColor = iconStyleColor,
                    onNavigate = { currentScreen = it }
                )
                AppearanceScreenType.THEME -> ThemeSettingsScreen(
                    iconBgColor = iconBgColor,
                    iconStyleColor = iconStyleColor,
                    darkMode = darkMode,
                    context = context
                )
                AppearanceScreenType.PLAYER -> PlayerDesignSettingsScreen(
                    iconBgColor = iconBgColor,
                    iconStyleColor = iconStyleColor
                )
                AppearanceScreenType.LYRICS -> LyricsSettingsScreen(
                    iconBgColor = iconBgColor,
                    iconStyleColor = iconStyleColor
                )
                AppearanceScreenType.UI -> UISettingsScreen(
                    iconBgColor = iconBgColor,
                    iconStyleColor = iconStyleColor
                )
            }
        }
    }
}

@Composable
private fun AppearanceMainScreen(
    iconBgColor: Color,
    iconStyleColor: Color,
    onNavigate: (AppearanceScreenType) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        state = rememberLazyListState()
    ) {
        item {
            Text(
                text = stringResource(R.string.appearance),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 24.dp, top = 16.dp)
            )
        }

        item {
            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items = listOf(
                    {
                        ModernInfoItem(
                            icon = { Icon(painterResource(R.drawable.palette), null, modifier = Modifier.size(22.dp)) },
                            title = stringResource(R.string.theme),
                            subtitle = stringResource(R.string.customize_colors),
                            onClick = { onNavigate(AppearanceScreenType.THEME) },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor
                        )
                    },
                    {
                        ModernInfoItem(
                            icon = { Icon(painterResource(R.drawable.play), null, modifier = Modifier.size(22.dp)) },
                            title = stringResource(R.string.player_design),
                            subtitle = stringResource(R.string.customize_player_layout),
                            onClick = { onNavigate(AppearanceScreenType.PLAYER) },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor
                        )
                    },
                    {
                        ModernInfoItem(
                            icon = { Icon(painterResource(R.drawable.lyrics), null, modifier = Modifier.size(22.dp)) },
                            title = stringResource(R.string.lyrics),
                            subtitle = stringResource(R.string.customize_lyrics_style),
                            onClick = { onNavigate(AppearanceScreenType.LYRICS) },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor
                        )
                    },
                    {
                        ModernInfoItem(
                            icon = { Icon(painterResource(R.drawable.grid_view), null, modifier = Modifier.size(22.dp)) },
                            title = stringResource(R.string.ui_interface),
                            subtitle = stringResource(R.string.customize_grid_navigation),
                            onClick = { onNavigate(AppearanceScreenType.UI) },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor
                        )
                    }
                )
            )
        }
    }
}

@Composable
private fun ThemeSettingsScreen(
    iconBgColor: Color,
    iconStyleColor: Color,
    darkMode: DarkMode,
    context: android.content.Context
) {
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(DynamicThemeKey, true)
    val (appShape, onAppShapeChange) = rememberEnumPreference(AppShapeKey, ShapeStyle.Default)
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, false)
    val (darkModePref, onDarkModeChange) = rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
    val (appFont, onAppFontChange) = rememberPreference(AppFontKey, "Roboto Flex")
    val (accentColorInt, onAccentColorChange) = rememberPreference(AccentColorKey, DefaultThemeColor.toArgb())

    var showFontDialog by rememberSaveable { mutableStateOf(false) }
    var showShapeDialog by rememberSaveable { mutableStateOf(false) }

    if (showShapeDialog) {
        DefaultDialog(
            onDismiss = { showShapeDialog = false },
            content = {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ShapeStyle.entries.forEach { shape ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAppShapeChange(shape)
                                    showShapeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoundedCheckbox(
                                checked = appShape == shape,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(shape.name)
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showShapeDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showFontDialog) {
        DefaultDialog(
            onDismiss = { showFontDialog = false },
            content = {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val fonts = listOf("Roboto Flex", "Outfit", "Space Grotesk", "Manrope")
                    fonts.forEach { font ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAppFontChange(font)
                                    showFontDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoundedCheckbox(
                                checked = appFont == font,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(font)
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showFontDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items = listOf(
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(painterResource(R.drawable.dark_mode), null, Modifier.size(22.dp)) },
                                    title = stringResource(R.string.dark_mode),
                                    subtitle = when (darkModePref) {
                                        DarkMode.ON -> stringResource(R.string.on)
                                        DarkMode.OFF -> stringResource(R.string.off)
                                        DarkMode.AUTO -> stringResource(R.string.system_default)
                                    },
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                            // Logic to cycle DarkMode would be here, but using simple Switch for now or custom cycler
                            // Reusing existing preference logic if possible, or simple "Click to cycle"
                        }
                    },
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(painterResource(R.drawable.palette), null, Modifier.size(22.dp)) },
                                    title = stringResource(R.string.system_colors),
                                    subtitle = stringResource(R.string.use_system_monet_colors),
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                            ModernSwitch(checked = dynamicTheme, onCheckedChange = onDynamicThemeChange)
                        }
                    },
                    {
                        ModernInfoItem(
                            icon = { Icon(painterResource(R.drawable.edit), null, Modifier.size(22.dp)) },
                            title = "Shape Style",
                            subtitle = appShape.name,
                            onClick = { showShapeDialog = true },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor
                        )
                    },
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(painterResource(R.drawable.contrast), null, Modifier.size(22.dp)) },
                                    title = stringResource(R.string.pure_black),
                                    subtitle = stringResource(R.string.pitch_black_background_for_oled_displays),
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                            ModernSwitch(checked = pureBlack, onCheckedChange = onPureBlackChange)
                        }
                    },
                    {
                        ModernInfoItem(
                            icon = { Icon(painterResource(R.drawable.short_text), null, Modifier.size(22.dp)) },
                            title = "App Font",
                            subtitle = appFont,
                            onClick = { showFontDialog = true },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor
                        )
                    }
                )
            )
        }
    }
}

@Composable
private fun PlayerDesignSettingsScreen(
    iconBgColor: Color,
    iconStyleColor: Color
) {
    val (cdCoverMode, onCdCoverModeChange) = rememberPreference(CDCoverModeKey, false)
    val (zenMode, onZenModeChange) = rememberPreference(ZenModeKey, false)
    val (playerBackground, onPlayerBackgroundChange) = rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.GRADIENT)
    val (rotatingThumbnail, onRotatingThumbnailChange) = rememberPreference(RotatingThumbnailKey, false)
    val (useNewPlayerDesign, onUseNewPlayerDesignChange) = rememberPreference(UseNewPlayerDesignKey, true)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items = listOf(
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(painterResource(R.drawable.album), null, Modifier.size(22.dp)) },
                                    title = "CD Cover Mode",
                                    subtitle = "Show spinning disc animation with cover art",
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                            ModernSwitch(checked = cdCoverMode, onCheckedChange = onCdCoverModeChange)
                        }
                    },
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(painterResource(R.drawable.bedtime), null, Modifier.size(22.dp)) },
                                    title = "Zen Mode (AOD)",
                                    subtitle = "OLED friendly always-on screen for playback",
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                            ModernSwitch(checked = zenMode, onCheckedChange = onZenModeChange)
                        }
                    },
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(painterResource(R.drawable.image), null, Modifier.size(22.dp)) },
                                    title = stringResource(R.string.player_background),
                                    subtitle = playerBackground.name.lowercase().replace("_", " ").capitalize(), // Simple formatting
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                             // Use dialog for this in real impl
                        }
                    },
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(painterResource(R.drawable.sync), null, Modifier.size(22.dp)) },
                                    title = stringResource(R.string.rotating_thumbnail),
                                    subtitle = stringResource(R.string.rotate_artwork_when_playing),
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                            ModernSwitch(checked = rotatingThumbnail, onCheckedChange = onRotatingThumbnailChange)
                        }
                    }
                )
            )
        }
    }
}

@Composable
private fun LyricsSettingsScreen(
    iconBgColor: Color,
    iconStyleColor: Color
) {
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, 28f)
    val (lyricsWorkForWord, onLyricsWorkForWordChange) = rememberPreference(LyricsWordForWordKey, true)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
             Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items = listOf(
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(painterResource(R.drawable.lyrics), null, Modifier.size(22.dp)) },
                                    title = stringResource(R.string.lyrics_word_by_word),
                                    subtitle = stringResource(R.string.highlight_lyrics_word_by_word),
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                            ModernSwitch(checked = lyricsWorkForWord, onCheckedChange = onLyricsWorkForWordChange)
                        }
                    }
                    // Add other lyrics settings here
                )
            )
        }
    }
}

@Composable
private fun UISettingsScreen(
    iconBgColor: Color,
    iconStyleColor: Color
) {
     val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(GridItemsSizeKey, GridItemSize.SMALL)
     val (swipeToSong, onSwipeToSongChange) = rememberPreference(SwipeToSongKey, false)

     LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
             Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items = listOf(
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(painterResource(R.drawable.grid_view), null, Modifier.size(22.dp)) },
                                    title = stringResource(R.string.grid_item_size),
                                    subtitle = gridItemSize.name,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                            // Dialog logic
                        }
                    },
                    {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                ModernInfoItem(
                                    icon = { Icon(painterResource(R.drawable.swipe), null, Modifier.size(22.dp)) },
                                    title = stringResource(R.string.swipe_to_change_track),
                                    subtitle = stringResource(R.string.swipe_to_change_track_description),
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                            ModernSwitch(checked = swipeToSong, onCheckedChange = onSwipeToSongChange)
                        }
                    }
                )
             )
        }
     }
}
