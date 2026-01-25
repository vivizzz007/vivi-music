package com.music.vivi

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.Surface
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.music.vivi.constants.HighRefreshRateKey
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.constants.AppBarHeight
import com.music.vivi.constants.AppLanguageKey
import com.music.vivi.constants.CheckForUpdatesKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.DefaultOpenTabKey
import com.music.vivi.constants.DisableScreenshotKey
import com.music.vivi.constants.AccentColorKey
import com.music.vivi.constants.Material3ExpressiveKey
import com.music.vivi.constants.DynamicThemeKey
import com.music.vivi.constants.MiniPlayerHeight
import com.music.vivi.constants.MiniPlayerBottomSpacing
import com.music.vivi.constants.UseNewMiniPlayerDesignKey
import com.music.vivi.constants.PowerSaverKey
import com.music.vivi.constants.PowerSaverAnimationsKey
import androidx.compose.animation.core.snap

import com.music.vivi.constants.NavigationBarAnimationSpec
import com.music.vivi.constants.NavigationBarHeight
import com.music.vivi.constants.PauseSearchHistoryKey
import com.music.vivi.constants.PureBlackKey
import com.music.vivi.constants.SYSTEM_DEFAULT
import com.music.vivi.constants.SearchSource
import com.music.vivi.constants.SearchSourceKey
import com.music.vivi.constants.SlimNavBarHeight
import com.music.vivi.constants.SlimNavBarKey
import com.music.vivi.constants.StopMusicOnTaskClearKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.SearchHistory
import com.music.vivi.extensions.toEnum
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.DownloadUtil
import com.music.vivi.playback.MusicService
import com.music.vivi.playback.MusicService.MusicBinder
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.AccountSettingsDialog
import com.music.vivi.ui.component.BottomSheetMenu
import com.music.vivi.ui.component.BottomSheetPage
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalBottomSheetPageState
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.TopSearch
import com.music.vivi.ui.component.rememberBottomSheetState
import com.music.vivi.ui.component.shimmer.ShimmerTheme
import com.music.vivi.ui.menu.YouTubeSongMenu
import com.music.vivi.ui.player.BottomSheetPlayer
import com.music.vivi.ui.screens.Screens
import com.music.vivi.ui.screens.navigationBuilder
import com.music.vivi.ui.screens.search.LocalSearchScreen
import com.music.vivi.ui.screens.search.OnlineSearchScreen
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.ui.screens.settings.NavigationTab
import com.music.vivi.ui.theme.ColorSaver
import com.music.vivi.ui.theme.DefaultThemeColor
import com.music.vivi.ui.theme.MusicTheme
import com.music.vivi.ui.theme.extractThemeColor
import com.music.vivi.ui.utils.appBarScrollBehavior
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.resetHeightOffset
import com.music.vivi.update.changelog.ChangelogBottomSheet
import com.music.vivi.update.downloadmanager.DownloadNotificationManager
import com.music.vivi.update.experiment.CrashLogHandler
import com.music.vivi.update.updatenotification.UpdateNotificationManager
// import com.music.vivi.update.updatenotification.UpdateNotificationManager
import com.music.vivi.updatesreen.UpdateStatus
import com.music.vivi.utils.SyncUtils
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.utils.reportException
import com.music.vivi.utils.setAppLocale
import com.music.vivi.viewmodels.HomeViewModel
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import com.music.vivi.update.viewmodelupdate.UpdateViewModel
import com.materialkolor.dynamicColorScheme
import com.materialkolor.PaletteStyle
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext

/**
 * The single Activity of the application.
 *
 * It is responsible for:
 * - Setting up the Jetpack Compose content ([setContent]).
 * - Providing global CompositionLocals (Theme, Database, PlayerConnection).
 * - Initializing global singletons like [DownloadNotificationManager].
 * - Implementing the main [NavHost] for screen navigation.
 * - Binding to the [MusicService] to control playback.
 */
@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    private lateinit var navController: NavHostController
    private var pendingIntent: Intent? = null
    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private var isServiceBound = false

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                if (service is MusicBinder) {
                    playerConnection =
                        PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                    isServiceBound = true
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playerConnection?.dispose()
                playerConnection = null
                isServiceBound = false
            }
        }

    override fun onStart() {
        super.onStart()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000)
            }
        }

//        startService(Intent(this, MusicService::class.java))

        // ⚠️ REMOVE startService() - only use bindService
        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(
                StopMusicOnTaskClearKey,
                false
            ) &&
            playerConnection?.isPlaying?.value == true &&
            isFinishing
        ) {
            stopService(Intent(this, MusicService::class.java))
            if (isServiceBound) {
                unbindService(serviceConnection)
                isServiceBound = false
            }
            playerConnection = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::navController.isInitialized) {
            handleDeepLinkIntent(intent, navController)
        } else {
            pendingIntent = intent
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // download notification
        DownloadNotificationManager.initialize(this)
        // new download manager and update manager
        UpdateNotificationManager.initialize(this)
        // Check for updates on app start (pass VERSION_NAME, not VERSION_CODE)
// crash log when appp crash
        if (savedInstanceState == null) {
            CrashLogHandler.initialize(applicationContext)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale = dataStore[AppLanguageKey]
                ?.takeUnless { it == SYSTEM_DEFAULT }
                ?.let { Locale.forLanguageTag(it) }
                ?: Locale.getDefault()
            setAppLocale(this, locale)
        }

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        setContent {
// new viewmodel for update checking less stress more effect result
            val updateViewModel: UpdateViewModel = hiltViewModel()
            val updateStatus by updateViewModel.updateStatus.collectAsState()

            LaunchedEffect(updateStatus) {
                if (updateStatus is UpdateStatus.UpdateAvailable) {
                    latestVersionName = (updateStatus as UpdateStatus.UpdateAvailable).latestVersion
                }
            }

            val (checkForUpdatesPreference, _) = rememberPreference(CheckForUpdatesKey, true)

            LaunchedEffect(checkForUpdatesPreference) {
                updateViewModel.refreshUpdateStatus()
            }

            val (powerSaver, _) = rememberPreference(PowerSaverKey, false)
            val (powerSaverPureBlack, _) = rememberPreference(
                com.music.vivi.constants.PowerSaverPureBlackKey,
                defaultValue = true
            )
            val (powerSaverHighRefresh, _) = rememberPreference(
                com.music.vivi.constants.PowerSaverHighRefreshRateKey,
                defaultValue = true
            )
            val (powerSaverAnimations, _) = rememberPreference(PowerSaverAnimationsKey, defaultValue = true)

            val disableAnimations = powerSaver && powerSaverAnimations

            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
            val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme = remember(darkTheme, isSystemInDarkTheme, powerSaver, powerSaverPureBlack) {
                if (powerSaver && powerSaverPureBlack) {
                    true
                } else if (darkTheme == DarkMode.AUTO) {
                    isSystemInDarkTheme
                } else {
                    darkTheme == DarkMode.ON
                }
            }

            LaunchedEffect(useDarkTheme) {
                setSystemBarAppearance(useDarkTheme)
            }

            val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
            val pureBlack = remember(pureBlackEnabled, useDarkTheme, powerSaver, powerSaverPureBlack) {
                (pureBlackEnabled || (powerSaver && powerSaverPureBlack)) && useDarkTheme
            }

            val material3Expressive by rememberPreference(Material3ExpressiveKey, defaultValue = false)

            var themeColor by rememberSaveable(stateSaver = ColorSaver) {
                mutableStateOf(DefaultThemeColor)
            }

            val highRefreshRate by rememberPreference(HighRefreshRateKey, false)

            LaunchedEffect(highRefreshRate, powerSaver, powerSaverHighRefresh) {
                setHighRefreshRate(highRefreshRate && !(powerSaver && powerSaverHighRefresh))
            }

            val accentColorInt by rememberPreference(AccentColorKey, defaultValue = DefaultThemeColor.toArgb())
            val accentColor = remember(accentColorInt) { Color(accentColorInt) }

            // Async ColorScheme generation to prevent main thread blocking
            var generatedColorScheme by remember { mutableStateOf<ColorScheme?>(null) }
            val context = LocalContext.current

            LaunchedEffect(themeColor, useDarkTheme, enableDynamicTheme) {
                if (enableDynamicTheme &&
                    themeColor == DefaultThemeColor &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ) {
                    // System colors are fast, can be done on main (context required)
                    generatedColorScheme =
                        if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else if (!enableDynamicTheme || themeColor != DefaultThemeColor) {
                    // MaterialKolor generation is expensive, offload to background
                    withContext(Dispatchers.Default) {
                        try {
                            generatedColorScheme = dynamicColorScheme(
                                seedColor = themeColor,
                                isDark = useDarkTheme,
                                style = PaletteStyle.TonalSpot
                            )
                        } catch (e: Exception) {
                            reportException(e)
                        }
                    }
                }
            }

            LaunchedEffect(playerConnection, enableDynamicTheme, accentColor) {
                val playerConnection = playerConnection
                if (!enableDynamicTheme) {
                    themeColor = accentColor
                    return@LaunchedEffect
                }

                if (playerConnection == null) {
                    themeColor = DefaultThemeColor
                    return@LaunchedEffect
                }

                playerConnection.service.currentMediaMetadata.collectLatest { song ->
                    if (song?.thumbnailUrl != null) {
                        withContext(Dispatchers.IO) {
                            try {
                                val result = imageLoader.execute(
                                    ImageRequest.Builder(this@MainActivity)
                                        .data(song.thumbnailUrl)
                                        .allowHardware(false)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .networkCachePolicy(CachePolicy.ENABLED)
                                        .crossfade(false)
                                        .build()
                                )
                                themeColor = result.image?.toBitmap()?.extractThemeColor()
                                    ?: DefaultThemeColor
                            } catch (e: Exception) {
                                // Fallback to default on error
                                themeColor = DefaultThemeColor
                            }
                        }
                    } else {
                        themeColor = DefaultThemeColor
                    }
                }
            }

            MusicTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
                themeColor = themeColor,
                enableDynamicTheme = enableDynamicTheme,
                overrideColorScheme = generatedColorScheme,
                expressive = material3Expressive
            ) {
                BoxWithConstraints(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface
                        )
                ) {
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val configuration = LocalConfiguration.current
                    val cutoutInsets = WindowInsets.displayCutout
                    val windowsInsets = WindowInsets.systemBars
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                    val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

                    var showChangelogBottomSheet by remember { mutableStateOf(false) }

                    val navController = rememberNavController()
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val (previousTab, setPreviousTab) = rememberSaveable { mutableStateOf("home") }

                    val navigationItems = remember { Screens.MainScreens }
                    val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                    val (useNewMiniPlayerDesign) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
                    val defaultOpenTab = remember {
                        dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
                    }
                    val tabOpenedFromShortcut = remember {
                        when (intent?.action) {
                            ACTION_LIBRARY -> NavigationTab.LIBRARY
                            ACTION_SEARCH -> NavigationTab.SEARCH
                            else -> null
                        }
                    }

                    val topLevelScreens = remember {
                        listOf(
                            Screens.Home.route,
                            Screens.Search.route,
                            Screens.Library.route,
                            "settings"
                        )
                    }

                    val (query, onQueryChange) =
                        rememberSaveable(stateSaver = TextFieldValue.Saver) {
                            mutableStateOf(TextFieldValue())
                        }

                    var active by rememberSaveable {
                        mutableStateOf(false)
                    }

                    val onActiveChange: (Boolean) -> Unit = { newActive ->
                        active = newActive
                        if (!newActive) {
                            focusManager.clearFocus()
                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                onQueryChange(TextFieldValue())
                            }
                        }
                    }

                    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

                    val searchBarFocusRequester = remember { FocusRequester() }

                    val onSearch: (String) -> Unit = remember {
                        { searchQuery ->
                            if (searchQuery.isNotEmpty()) {
                                onActiveChange(false)
                                navController.navigate("search/${URLEncoder.encode(searchQuery, "UTF-8")}")

                                if (dataStore[PauseSearchHistoryKey] != true) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        database.query {
                                            insert(SearchHistory(query = searchQuery))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    var openSearchImmediately: Boolean by remember {
                        mutableStateOf(intent?.action == ACTION_SEARCH)
                    }

                    val inSearchScreen = remember(navBackStackEntry) {
                        navBackStackEntry?.destination?.route?.startsWith("search/") == true
                    }

                    val shouldShowSearchBar = remember(active, navBackStackEntry) {
                        active ||
                            navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                            inSearchScreen
                    }

                    val shouldShowNavigationBar = remember(navBackStackEntry, active) {
                        navBackStackEntry?.destination?.route == null ||
                            navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } &&
                            !active
                    }

                    val isLandscape = remember(configuration) {
                        configuration.screenWidthDp > configuration.screenHeightDp
                    }
                    val showRail = isLandscape && !inSearchScreen

                    val getNavPadding: () -> Dp = remember {
                        {
                            if (shouldShowNavigationBar && !showRail) {
                                if (slimNav) SlimNavBarHeight else NavigationBarHeight
                            } else {
                                0.dp
                            }
                        }
                    }

                    val navigationBarHeight by animateDpAsState(
                        targetValue = if (shouldShowNavigationBar && !showRail) NavigationBarHeight else 0.dp,
                        animationSpec = if (disableAnimations) snap() else NavigationBarAnimationSpec,
                        label = ""
                    )

                    val playerBottomSheetState =
                        rememberBottomSheetState(
                            dismissedBound = 0.dp,
                            collapsedBound = bottomInset +
                                (if (!showRail && shouldShowNavigationBar) getNavPadding() else 0.dp) +
                                (if (useNewMiniPlayerDesign) MiniPlayerBottomSpacing else 0.dp) +
                                MiniPlayerHeight,
                            expandedBound = maxHeight
                        )

                    val playerAwareWindowInsets = remember(
                        bottomInset,
                        shouldShowNavigationBar,
                        playerBottomSheetState.isDismissed,
                        showRail
                    ) {
                        var bottom = bottomInset
                        if (shouldShowNavigationBar && !showRail) {
                            bottom += NavigationBarHeight
                        }
                        if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                    }

                    appBarScrollBehavior(
                        canScroll = {
                            !inSearchScreen &&
                                (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )

                    val searchBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                !inSearchScreen &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            }
                        )
                    val topAppBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                !inSearchScreen &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            }
                        )

                    // Navigation tracking
                    LaunchedEffect(navBackStackEntry) {
                        if (inSearchScreen) {
                            val searchQuery =
                                withContext(Dispatchers.IO) {
                                    if (navBackStackEntry
                                            ?.arguments
                                            ?.getString(
                                                "query"
                                            )!!
                                            .contains(
                                                "%"
                                            )
                                    ) {
                                        navBackStackEntry?.arguments?.getString(
                                            "query"
                                        )!!
                                    } else {
                                        URLDecoder.decode(
                                            navBackStackEntry?.arguments?.getString("query")!!,
                                            "UTF-8"
                                        )
                                    }
                                }
                            onQueryChange(
                                TextFieldValue(
                                    searchQuery,
                                    TextRange(searchQuery.length)
                                )
                            )
                        } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                            onQueryChange(TextFieldValue())
                        }

                        // Reset scroll behavior for main navigation items
                        if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                            if (navigationItems.fastAny { it.route == previousTab }) {
                                searchBarScrollBehavior.state.resetHeightOffset()
                            }
                        }

                        searchBarScrollBehavior.state.resetHeightOffset()
                        topAppBarScrollBehavior.state.resetHeightOffset()

                        // Track previous tab for animations
                        navController.currentBackStackEntry?.destination?.route?.let {
                            setPreviousTab(it)
                        }
                    }

                    LaunchedEffect(active) {
                        if (active) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                            searchBarFocusRequester.requestFocus()
                        }
                    }

                    LaunchedEffect(playerConnection) {
                        val player = playerConnection?.player ?: return@LaunchedEffect
                        if (player.currentMediaItem == null) {
                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else {
                            if (playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.collapseSoft()
                            }
                        }
                    }

                    DisposableEffect(playerConnection, playerBottomSheetState) {
                        val player =
                            playerConnection?.player ?: return@DisposableEffect onDispose { }
                        val listener =
                            object : Player.Listener {
                                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                        mediaItem != null &&
                                        playerBottomSheetState.isDismissed
                                    ) {
                                        playerBottomSheetState.collapseSoft()
                                    }
                                }
                            }
                        player.addListener(listener)
                        onDispose {
                            player.removeListener(listener)
                        }
                    }

                    var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }
                    val isDetailShown by navBackStackEntry?.savedStateHandle?.getStateFlow(
                        "is_detail_shown",
                        false
                    )?.collectAsState()
                        ?: remember { mutableStateOf(false) }

                    LaunchedEffect(navBackStackEntry, isDetailShown) {
                        shouldShowTopBar =
                            !active &&
                            navBackStackEntry?.destination?.route in topLevelScreens &&
                            navBackStackEntry?.destination?.route != "settings" &&
                            isDetailShown != true
                    }

                    val coroutineScope = rememberCoroutineScope()
                    var sharedSong: SongItem? by remember {
                        mutableStateOf(null)
                    }

                    LaunchedEffect(Unit) {
                        if (pendingIntent != null) {
                            handleDeepLinkIntent(pendingIntent!!, navController)
                            pendingIntent = null
                        } else {
                            handleDeepLinkIntent(intent, navController)
                        }
                    }

                    DisposableEffect(Unit) {
                        val listener = Consumer<Intent> { intent ->
                            handleDeepLinkIntent(intent, navController)
                        }

                        addOnNewIntentListener(listener)
                        onDispose { removeOnNewIntentListener(listener) }
                    }

                    val currentTitleRes = remember(navBackStackEntry) {
                        when (navBackStackEntry?.destination?.route) {
                            Screens.Home.route -> R.string.home
                            Screens.Search.route -> R.string.search
                            Screens.Library.route -> R.string.filter_library
                            else -> null
                        }
                    }

                    var showAccountDialog by remember { mutableStateOf(false) }

                    val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                    val insetBg by remember(baseBg) {
                        derivedStateOf {
                            if (playerBottomSheetState.progress > 0f) Color.Transparent else baseBg
                        }
                    }

                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalContentColor provides
                            if (pureBlack) Color.White else contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                        LocalSyncUtils provides syncUtils
                    ) {
                        Scaffold(
                            topBar = {
                                AnimatedVisibility(
                                    visible = shouldShowTopBar,
                                    enter = slideInHorizontally(
                                        initialOffsetX = { -it / 4 },
                                        animationSpec = tween(durationMillis = 100)
                                    ) + fadeIn(animationSpec = tween(durationMillis = 100)),
                                    exit = slideOutHorizontally(
                                        targetOffsetX = { -it / 4 },
                                        animationSpec = tween(durationMillis = 100)
                                    ) + fadeOut(animationSpec = tween(durationMillis = 100))
                                ) {
                                    Row {
                                        TopAppBar(
                                            title = {
                                                Text(
                                                    text = currentTitleRes?.let { stringResource(it) } ?: "",
                                                    style = MaterialTheme.typography.titleLarge
                                                )
                                            },
                                            actions = {
                                                val newsViewModel: com.music.vivi.viewmodels.NewsViewModel =
                                                    hiltViewModel()
                                                val hasUnreadNews by newsViewModel.hasUnreadNews.collectAsState(
                                                    initial = false
                                                )
                                                val (showNewsIcon) = rememberPreference(
                                                    com.music.vivi.constants.ShowNewsIconKey,
                                                    true
                                                )
                                                val isUpdateAvailable = updateStatus is UpdateStatus.UpdateAvailable

                                                if (isUpdateAvailable || showNewsIcon) {
                                                    IconButton(onClick = {
                                                        if (isUpdateAvailable) {
                                                            navController.navigate("settings/update")
                                                        } else {
                                                            navController.navigate("news")
                                                        }
                                                    }) {
                                                        Crossfade(
                                                            targetState = isUpdateAvailable,
                                                            animationSpec = tween(300),
                                                            label = "icon_crossfade"
                                                        ) { updateAvailable ->
                                                            if (updateAvailable) {
                                                                Icon(
                                                                    painter = painterResource(
                                                                        id = R.drawable.rocket_new_update
                                                                    ),
                                                                    contentDescription = stringResource(
                                                                        R.string.update_available
                                                                    ),
                                                                    tint = Color.Red,
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                            } else {
                                                                BadgedBox(badge = {
                                                                    if (hasUnreadNews) Badge()
                                                                }) {
                                                                    Icon(
                                                                        painter = painterResource(
                                                                            R.drawable.newspaper_vivi
                                                                        ),
                                                                        contentDescription = stringResource(
                                                                            R.string.changelog_title
                                                                        ),
                                                                        modifier = Modifier.size(24.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                IconButton(onClick = { navController.navigate("history") }) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.music_history),
                                                        contentDescription = stringResource(R.string.history)
                                                    )
                                                }
                                                IconButton(onClick = { navController.navigate("stats") }) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.stats),
                                                        contentDescription = stringResource(R.string.stats)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { navController.navigate("settings") },
                                                    modifier = Modifier.semantics {
                                                        contentDescription = "Settings"
                                                    }
                                                ) {
                                                    if (accountImageUrl != null) {
                                                        // HIER GEÄNDERT: coil3.compose.AsyncImage statt coil.compose.AsyncImage
                                                        coil3.compose.AsyncImage(
                                                            model = accountImageUrl,
                                                            contentDescription = stringResource(R.string.account),
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                        )
                                                    } else {
                                                        val composition by rememberLottieComposition(
                                                            LottieCompositionSpec.RawRes(R.raw.setting)
                                                        )
                                                        val progress by animateLottieCompositionAsState(
                                                            composition = composition,
                                                            isPlaying = true, // Always playing when screen loads
                                                            iterations = 1, // Play once
                                                            speed = 1.5f
                                                        )

                                                        LottieAnimation(
                                                            composition = composition,
                                                            progress = { progress },
                                                            modifier = Modifier.size(50.dp),
                                                            contentScale = ContentScale.Fit
                                                        )
                                                    }
                                                }
                                            },
                                            scrollBehavior = searchBarScrollBehavior,
                                            colors = TopAppBarDefaults.topAppBarColors(
                                                containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                                scrolledContainerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.windowInsetsPadding(
                                                if (showRail) {
                                                    WindowInsets(left = NavigationBarHeight)
                                                        .add(cutoutInsets.only(WindowInsetsSides.Start))
                                                } else {
                                                    cutoutInsets.only(WindowInsetsSides.Start + WindowInsetsSides.End)
                                                }
                                            )
                                        )
                                    }
                                }
                                AnimatedVisibility(
                                    visible = active || inSearchScreen,
                                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(150)),
                                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(100))
                                ) {
                                    TopSearch(
                                        query = query,
                                        onQueryChange = onQueryChange,
                                        onSearch = onSearch,
                                        active = active,
                                        onActiveChange = onActiveChange,
                                        placeholder = {
                                            Text(
                                                text = stringResource(
                                                    when (searchSource) {
                                                        SearchSource.LOCAL -> R.string.search_library
                                                        SearchSource.ONLINE -> R.string.search_yt_music
                                                    }
                                                )
                                            )
                                        },
                                        leadingIcon = {
                                            IconButton(
                                                onClick = {
                                                    when {
                                                        active -> onActiveChange(false)
                                                        !navigationItems.fastAny {
                                                            it.route ==
                                                                navBackStackEntry?.destination?.route
                                                        } -> {
                                                            navController.navigateUp()
                                                        }

                                                        else -> onActiveChange(true)
                                                    }
                                                },
                                                onLongClick = {
                                                    when {
                                                        active -> {}
                                                        !navigationItems.fastAny {
                                                            it.route ==
                                                                navBackStackEntry?.destination?.route
                                                        } -> {
                                                            navController.backToMain()
                                                        }
                                                        else -> {}
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    painterResource(
                                                        if (active ||
                                                            !navigationItems.fastAny {
                                                                it.route ==
                                                                    navBackStackEntry?.destination?.route
                                                            }
                                                        ) {
                                                            R.drawable.arrow_back
                                                        } else {
                                                            R.drawable.search
                                                        }
                                                    ),
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            Row {
                                                if (active) {
                                                    if (query.text.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = {
                                                                onQueryChange(
                                                                    TextFieldValue(
                                                                        ""
                                                                    )
                                                                )
                                                            }
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.close),
                                                                contentDescription = null
                                                            )
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            searchSource =
                                                                if (searchSource ==
                                                                    SearchSource.ONLINE
                                                                ) {
                                                                    SearchSource.LOCAL
                                                                } else {
                                                                    SearchSource.ONLINE
                                                                }
                                                        }
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(
                                                                when (searchSource) {
                                                                    SearchSource.LOCAL -> R.drawable.library_music
                                                                    SearchSource.ONLINE -> R.drawable.language
                                                                }
                                                            ),
                                                            contentDescription = null
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        focusRequester = searchBarFocusRequester,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .windowInsetsPadding(
                                                if (showRail) {
                                                    WindowInsets(left = NavigationBarHeight)
                                                } else {
                                                    WindowInsets(0.dp)
                                                }
                                            ),
                                        colors = if (pureBlack && active) {
                                            SearchBarDefaults.colors(
                                                containerColor = Color.Black,
                                                dividerColor = Color.DarkGray,
                                                inputFieldColors = TextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.Gray,
                                                    focusedContainerColor = Color.Transparent,
                                                    unfocusedContainerColor = Color.Transparent,
                                                    cursorColor = Color.White,
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent
                                                )
                                            )
                                        } else {
                                            SearchBarDefaults.colors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                            )
                                        }
                                    ) {
                                        Crossfade(
                                            targetState = searchSource,
                                            label = "",
                                            animationSpec = tween(150),
                                            modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .padding(
                                                    bottom = if (!playerBottomSheetState.isDismissed) MiniPlayerHeight else 0.dp
                                                )
                                                .navigationBarsPadding()
                                        ) { searchSource ->
                                            when (searchSource) {
                                                SearchSource.LOCAL ->
                                                    LocalSearchScreen(
                                                        query = query.text,
                                                        navController = navController,
                                                        onDismiss = { onActiveChange(false) },
                                                        pureBlack = pureBlack
                                                    )

                                                SearchSource.ONLINE ->
                                                    OnlineSearchScreen(
                                                        query = query.text,
                                                        onQueryChange = onQueryChange,
                                                        navController = navController,
                                                        onSearch = { searchQuery ->
                                                            navController.navigate(
                                                                "search/${URLEncoder.encode(searchQuery, "UTF-8")}"
                                                            )
                                                            if (dataStore[PauseSearchHistoryKey] != true) {
                                                                lifecycleScope.launch(Dispatchers.IO) {
                                                                    database.query {
                                                                        insert(SearchHistory(query = searchQuery))
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        onDismiss = { onActiveChange(false) },
                                                        pureBlack = pureBlack
                                                    )
                                            }
                                        }
                                    }
                                }
                            },

                            bottomBar = {
                                // now miniplayer wont show in updatescreen
                                val shouldShowMiniPlayer = remember(navBackStackEntry) {
                                    val route = navBackStackEntry?.destination?.route
                                    route != "settings/update"
                                }

                                if (!showRail) {
                                    Box {
                                        // now miniplayer wont show in updatescreen
                                        if (shouldShowMiniPlayer) {
                                            BottomSheetPlayer(
                                                state = playerBottomSheetState,
                                                navController = navController,
                                                pureBlack = pureBlack
                                            )
                                        }
                                        NavigationBar(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .height(bottomInset + getNavPadding())
                                                .offset {
                                                    if (navigationBarHeight == 0.dp) {
                                                        IntOffset(
                                                            x = 0,
                                                            y = (bottomInset + NavigationBarHeight).roundToPx()
                                                        )
                                                    } else {
                                                        val slideOffset =
                                                            (bottomInset + NavigationBarHeight) *
                                                                playerBottomSheetState.progress.coerceIn(
                                                                    0f,
                                                                    1f
                                                                )
                                                        val hideOffset =
                                                            (bottomInset + NavigationBarHeight) *
                                                                (1 - navigationBarHeight / NavigationBarHeight)
                                                        IntOffset(
                                                            x = 0,
                                                            y = (slideOffset + hideOffset).roundToPx()
                                                        )
                                                    }
                                                },
                                            containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                            contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ) {
                                            navigationItems.fastForEach { screen ->
                                                val isSelected =
                                                    navBackStackEntry?.destination?.hierarchy?.any {
                                                        it.route ==
                                                            screen.route
                                                    } ==
                                                        true

                                                NavigationBarItem(
                                                    selected = isSelected,
                                                    icon = {
                                                        Icon(
                                                            painter = painterResource(
                                                                id = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                                                            ),
                                                            contentDescription = null
                                                        )
                                                    },
                                                    label = {
                                                        if (!slimNav) {
                                                            Text(
                                                                text = stringResource(screen.titleId),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        if (screen.route == Screens.Search.route) {
                                                            onActiveChange(true)
                                                        } else if (isSelected) {
                                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                                "scrollToTop",
                                                                true
                                                            )
                                                            coroutineScope.launch {
                                                                searchBarScrollBehavior.state.resetHeightOffset()
                                                            }
                                                        } else {
                                                            navController.navigate(screen.route) {
                                                                popUpTo(navController.graph.startDestinationId) {
                                                                    saveState = true
                                                                }
                                                                launchSingleTop = true
                                                                restoreState = true
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(insetBg)
                                                .fillMaxWidth()
                                                .align(Alignment.BottomCenter)
                                                .height(bottomInsetDp)
                                        )
                                    }
                                } else {
                                    // now miniplayer wont show in updatescreen

                                    if (shouldShowMiniPlayer) {
                                        BottomSheetPlayer(
                                            state = playerBottomSheetState,
                                            navController = navController,
                                            pureBlack = pureBlack
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(insetBg)
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .height(bottomInsetDp)
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(searchBarScrollBehavior.nestedScrollConnection)
                        ) {
                            Row(Modifier.fillMaxSize()) {
                                if (showRail) {
                                    NavigationRail(
                                        containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Spacer(modifier = Modifier.weight(1f))

                                        navigationItems.fastForEach { screen ->
                                            val isSelected =
                                                navBackStackEntry?.destination?.hierarchy?.any {
                                                    it.route ==
                                                        screen.route
                                                } ==
                                                    true
                                            NavigationRailItem(
                                                selected = isSelected,
                                                onClick = {
                                                    if (screen.route == Screens.Search.route) {
                                                        onActiveChange(true)
                                                    } else if (isSelected) {
                                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                                            "scrollToTop",
                                                            true
                                                        )
                                                        coroutineScope.launch {
                                                            searchBarScrollBehavior.state.resetHeightOffset()
                                                        }
                                                    } else {
                                                        navController.navigate(screen.route) {
                                                            popUpTo(navController.graph.startDestinationId) {
                                                                inclusive = false
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = true // ✅ CHANGE TO true
                                                        }
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        painter = painterResource(
                                                            id = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                                                        ),
                                                        contentDescription = null
                                                    )
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Box(Modifier.weight(1f)) {
                                    // NavHost with animations
                                    NavHost(
                                        navController = navController,
                                        startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                            NavigationTab.HOME -> Screens.Home
                                            NavigationTab.LIBRARY -> Screens.Library
                                            else -> Screens.Home
                                        }.route,
                                        // Enter Transition
                                        enterTransition = {
                                            val currentRouteIndex = navigationItems.indexOfFirst {
                                                it.route == targetState.destination.route
                                            }
                                            val previousRouteIndex = navigationItems.indexOfFirst {
                                                it.route == initialState.destination.route
                                            }

                                            if (currentRouteIndex == -1 || currentRouteIndex > previousRouteIndex) {
                                                slideInHorizontally { it / 4 } + fadeIn(tween(150))
                                            } else {
                                                slideInHorizontally { -it / 4 } + fadeIn(tween(150))
                                            }
                                        },
                                        // Exit Transition
                                        exitTransition = {
                                            val currentRouteIndex = navigationItems.indexOfFirst {
                                                it.route == initialState.destination.route
                                            }
                                            val targetRouteIndex = navigationItems.indexOfFirst {
                                                it.route == targetState.destination.route
                                            }

                                            if (targetRouteIndex == -1 || targetRouteIndex > currentRouteIndex) {
                                                slideOutHorizontally { -it / 4 } + fadeOut(tween(100))
                                            } else {
                                                slideOutHorizontally { it / 4 } + fadeOut(tween(100))
                                            }
                                        },
                                        // Pop Enter Transition
                                        popEnterTransition = {
                                            val currentRouteIndex = navigationItems.indexOfFirst {
                                                it.route == targetState.destination.route
                                            }
                                            val previousRouteIndex = navigationItems.indexOfFirst {
                                                it.route == initialState.destination.route
                                            }

                                            if (previousRouteIndex != -1 && previousRouteIndex < currentRouteIndex) {
                                                slideInHorizontally { it / 4 } + fadeIn(tween(150))
                                            } else {
                                                slideInHorizontally { -it / 4 } + fadeIn(tween(150))
                                            }
                                        },
                                        // Pop Exit Transition
                                        popExitTransition = {
                                            val currentRouteIndex = navigationItems.indexOfFirst {
                                                it.route == initialState.destination.route
                                            }
                                            val targetRouteIndex = navigationItems.indexOfFirst {
                                                it.route == targetState.destination.route
                                            }

                                            if (currentRouteIndex != -1 && currentRouteIndex < targetRouteIndex) {
                                                slideOutHorizontally { -it / 4 } + fadeOut(tween(100))
                                            } else {
                                                slideOutHorizontally { it / 4 } + fadeOut(tween(100))
                                            }
                                        },
                                        modifier = Modifier.nestedScroll(
                                            if (navigationItems.fastAny {
                                                    it.route ==
                                                        navBackStackEntry?.destination?.route
                                                } ||
                                                inSearchScreen
                                            ) {
                                                searchBarScrollBehavior.nestedScrollConnection
                                            } else {
                                                topAppBarScrollBehavior.nestedScrollConnection
                                            }
                                        )
                                    ) {
                                        navigationBuilder(
                                            navController,
                                            topAppBarScrollBehavior,
                                            updateStatus
                                        )
                                    }
                                }
                            }
                        }

                        BottomSheetMenu(
                            state = LocalMenuState.current,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        BottomSheetPage(
                            state = LocalBottomSheetPageState.current,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        if (showAccountDialog) {
                            AccountSettingsDialog(
                                navController = navController,
                                onDismiss = {
                                    showAccountDialog = false
                                    homeViewModel.refresh()
                                }
                            )
                        }

                        sharedSong?.let { song ->
                            playerConnection?.let {
                                Dialog(
                                    onDismissRequest = { sharedSong = null },
                                    properties = DialogProperties(usePlatformDefaultWidth = false)
                                ) {
                                    Surface(
                                        modifier = Modifier.padding(24.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = AlertDialogDefaults.containerColor,
                                        tonalElevation = AlertDialogDefaults.TonalElevation
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = { sharedSong = null }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showChangelogBottomSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showChangelogBottomSheet = false }
                        ) {
                            ChangelogBottomSheet()
                        }
                    }
                    LaunchedEffect(shouldShowSearchBar, openSearchImmediately) {
                        if (shouldShowSearchBar && openSearchImmediately) {
                            onActiveChange(true)
                            searchBarFocusRequester.requestFocus()
                            openSearchImmediately = false
                        }
                    }
                }
            }
        }
    }

    private fun handleDeepLinkIntent(intent: Intent, navController: NavHostController) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        val coroutineScope = lifecycleScope

        when (val path = uri.pathSegments.firstOrNull()) {
            "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                if (playlistId.startsWith("OLAK5uy_")) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube.albumSongs(playlistId).onSuccess { songs ->
                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                withContext(Dispatchers.Main) {
                                    navController.navigate("album/$browseId")
                                }
                            }
                        }.onFailure { reportException(it) }
                    }
                } else {
                    navController.navigate("online_playlist/$playlistId")
                }
            }

            "browse" -> uri.lastPathSegment?.let { browseId ->
                navController.navigate("album/$browseId")
            }

            "channel", "c" -> uri.lastPathSegment?.let { artistId ->
                navController.navigate("artist/$artistId")
            }

            else -> {
                val videoId = when {
                    path == "watch" -> uri.getQueryParameter("v")
                    uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
                    else -> null
                }

                val playlistId = uri.getQueryParameter("list")

                videoId?.let {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube.queue(listOf(it), playlistId).onSuccess { queue ->
                            withContext(Dispatchers.Main) {
                                playerConnection?.playQueue(
                                    YouTubeQueue(
                                        WatchEndpoint(videoId = queue.firstOrNull()?.id, playlistId = playlistId),
                                        queue.firstOrNull()?.toMediaMetadata()
                                    )
                                )
                            }
                        }.onFailure {
                            reportException(it)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

    private fun setHighRefreshRate(enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val layoutParams = window.attributes
            if (enable) {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }
                val modes = display?.supportedModes
                val maxRefreshRateMode = modes?.maxByOrNull { it.refreshRate }
                layoutParams.preferredDisplayModeId = maxRefreshRateMode?.modeId ?: 0
            } else {
                layoutParams.preferredDisplayModeId = 0
            }
            window.attributes = layoutParams
        }
    }

    companion object {
        const val ACTION_SEARCH = "com.music.vivi.action.SEARCH"
        const val ACTION_LIBRARY = "com.music.vivi.action.LIBRARY"
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection =
    staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets =
    compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }
