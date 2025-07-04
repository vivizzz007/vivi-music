package com.music.vivi

import android.Manifest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.net.toUri
//import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.imageLoader
import coil.request.ImageRequest
import com.valentinilk.shimmer.LocalShimmerTheme
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.constants.AppBarHeight
import com.music.vivi.constants.AppDesignVariantKey
import com.music.vivi.constants.AppDesignVariantType
import com.music.vivi.constants.AutoSyncLocalSongsKey
import com.music.vivi.constants.ChipSortTypeKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.DefaultOpenTabKey
import com.music.vivi.constants.DefaultOpenTabOldKey
import com.music.vivi.constants.DisableScreenshotKey
import com.music.vivi.constants.DynamicThemeKey
import com.music.vivi.constants.FirstSetupPassed
import com.music.vivi.constants.LibraryFilter
import com.music.vivi.constants.MiniPlayerHeight
import com.music.vivi.constants.NavigationBarAnimationSpec
import com.music.vivi.constants.NavigationBarHeight
import com.music.vivi.constants.PauseSearchHistoryKey
import com.music.vivi.constants.PureBlackKey
import com.music.vivi.constants.SearchSource
import com.music.vivi.constants.SearchSourceKey
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
import com.music.vivi.ui.component.BottomSheetMenu
import com.music.vivi.ui.component.IconButton
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
import com.music.vivi.ui.screens.settings.NavigationTabOld
import com.music.vivi.ui.screens.settings.updateLanguage
import com.music.vivi.ui.theme.ColorSaver
import com.music.vivi.ui.theme.DefaultThemeColor
import com.music.vivi.ui.theme.viviTheme
import com.music.vivi.ui.theme.extractThemeColor
import com.music.vivi.utils.SyncUtils
import com.music.vivi.ui.utils.appBarScrollBehavior
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.resetHeightOffset
//import com.music.vivi.utils.Updater
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.utils.reportException
import com.music.vivi.utils.urlEncode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.net.URLDecoder
import javax.inject.Inject
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import kotlin.time.Duration.Companion.days
import java.util.regex.Pattern
import androidx.compose.runtime.DisposableEffect
import android.content.*

import android.net.Network
import android.net.Uri

import android.os.*
import android.util.Log
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.outlined.SignalWifiOff

import androidx.compose.material3.*
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.*

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.*
import coil.ImageLoader
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import com.airbnb.lottie.compose.*

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.concurrent.thread


// Add your own imports for your project below:
import com.music.vivi.*
import com.music.vivi.models.MediaMetadata
import androidx.media3.common.MediaMetadata.Builder
import com.music.vivi.ui.*

import com.music.vivi.ui.player.*
import com.music.vivi.ui.theme.*
import com.music.vivi.utils.*

import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

import com.google.zxing.integration.android.IntentIntegrator
import java.util.*
import androidx.core.util.Consumer // Add this
import coil.compose.AsyncImagePainter
import coil.compose.LocalImageLoader
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton


@Suppress("NAME_SHADOWING")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase
    @Inject
    lateinit var downloadUtil: DownloadUtil
    @Inject
    lateinit var syncUtils: SyncUtils

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicBinder) {
                playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection?.dispose()
            playerConnection = null
        }
    }

    // QR Scanner related variables
    private var showQRScanner by mutableStateOf(false)
    private var showQRResultDialog by mutableStateOf(false)
    private var qrScannedUrl by mutableStateOf("")

    // Permission handling
    private val mediaPermissionLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)
    private lateinit var navController: NavHostController

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startService(Intent(this, MusicService::class.java))
        } else {
            startService(Intent(this, MusicService::class.java))
        }
        bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(StopMusicOnTaskClearKey, false) && playerConnection?.isPlaying?.value == true && isFinishing) {
            stopService(Intent(this, MusicService::class.java))
            unbindService(serviceConnection)
            playerConnection = null
        }
    }

    private fun handleYoutubeUrl(url: String) {
        lifecycleScope.launch {
            try {
                when {
                    url.contains("youtube.com/watch") || url.contains("youtu.be/") -> {
                        extractVideoId(url)?.let { videoId ->
                            YouTube.queue(listOf(videoId)).onSuccess { songs ->
                                songs.firstOrNull()?.let { song ->
                                    playerConnection?.playQueue(
                                        YouTubeQueue(
                                            WatchEndpoint(videoId = song.id),
                                            song.toMediaMetadata()
                                        )
                                    )
                                }
                            }
                        }
                    }
                    url.contains("youtube.com/playlist") -> {
                        extractPlaylistId(url)?.let { playlistId ->
                            YouTube.playlist(playlistId).onSuccess {
                                navController.navigate("online_playlist/$playlistId")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("QRScanner", "Error handling YouTube URL", e)
                Toast.makeText(this@MainActivity, "Error playing music", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun extractVideoId(url: String): String? {
        val pattern = Regex("(?:youtube\\.com\\/watch\\?v=|youtu\\.be\\/)([^&\\n?]+)")
        return pattern.find(url)?.groupValues?.get(1)
    }

    private fun extractPlaylistId(url: String): String? {
        val pattern = Regex("list=([^&\\n?]+)")
        return pattern.find(url)?.groupValues?.get(1)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch {
            dataStore.data.collectLatest { preferences ->
                if (preferences[DisableScreenshotKey] == true) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        val sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedLanguage = sharedPreferences.getString("app_language", Locale.getDefault().language) ?: "en"
        updateLanguage(this, savedLanguage)

        setContent {
            val context = LocalContext.current
            var showNetworkBottomSheet by rememberSaveable { mutableStateOf(false) }
            var isNetworkAvailable by rememberSaveable { mutableStateOf(true) }

            DisposableEffect(Unit) {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        isNetworkAvailable = true
                        showNetworkBottomSheet = false
                    }
                    override fun onLost(network: Network) {
                        isNetworkAvailable = false
                        showNetworkBottomSheet = true
                    }
                }

                connectivityManager.registerNetworkCallback(
                    NetworkRequest.Builder().build(),
                    networkCallback
                )

                onDispose {
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                }
            }

            val (appDesignVariant) = rememberEnumPreference(AppDesignVariantKey, AppDesignVariantType.NEW)
            val (defaultOpenTabOld) = rememberEnumPreference(DefaultOpenTabOldKey, NavigationTabOld.HOME)
            navController = rememberNavController()

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val inSelectMode = navBackStackEntry?.savedStateHandle?.getStateFlow("inSelectMode", false)?.collectAsState()
            val enableDynamicTheme by rememberPreference(DynamicThemeKey, false)
            val darkTheme by rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
            val pureBlack by rememberPreference(PureBlackKey, false)
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            }

            LaunchedEffect(useDarkTheme) {
                setSystemBarAppearance(useDarkTheme)
            }

            var themeColor by rememberSaveable(stateSaver = ColorSaver) {
                mutableStateOf(DefaultThemeColor)
            }

            LaunchedEffect(playerConnection, enableDynamicTheme, isSystemInDarkTheme) {
                val playerConnection = playerConnection
                if (!enableDynamicTheme || playerConnection == null) {
                    themeColor = DefaultThemeColor
                    return@LaunchedEffect
                }
                playerConnection.service.currentMediaMetadata.collectLatest { song ->
                    themeColor = if (song != null) {
                        withContext(Dispatchers.IO) {
                            val result = imageLoader.execute(
                                ImageRequest.Builder(this@MainActivity)
                                    .data(song.thumbnailUrl)
                                    .allowHardware(false)
                                    .build()
                            )
                            (result.drawable as? BitmapDrawable)?.bitmap?.extractThemeColor() ?: DefaultThemeColor
                        }
                    } else DefaultThemeColor
                }
            }

            if (checkSelfPermission(mediaPermissionLevel) == PackageManager.PERMISSION_GRANTED) {
                val (autoSyncLocalSongs) = rememberPreference(AutoSyncLocalSongsKey, false)
                if (autoSyncLocalSongs == true) {
                    // Not implemented
                }
            } else if (checkSelfPermission(mediaPermissionLevel) == PackageManager.PERMISSION_DENIED) {
                permissionLauncher.launch(mediaPermissionLevel)
            }

            viviTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
                themeColor = themeColor
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface
                        )
                ) {
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val windowsInsets = WindowInsets.systemBars
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }

                    val (slimNav) = rememberPreference(SlimNavBarKey, true)
                    val navigationItems = remember(appDesignVariant) {
                        if (appDesignVariant == AppDesignVariantType.NEW) {
                            Screens.MainScreens
                        } else {
                            Screens.MainScreensOld
                        }
                    }

                    val defaultOpenTab = remember {
                        if (appDesignVariant == AppDesignVariantType.NEW)
                            dataStore[DefaultOpenTabKey].toEnum(NavigationTab.HOME)
                        else
                            dataStore[DefaultOpenTabKey].toEnum(NavigationTab.HOME)
                    }

                    val tabOpenedFromShortcut = remember {
                        when (intent?.action) {
                            ACTION_SONGS -> if (appDesignVariant == AppDesignVariantType.NEW) NavigationTab.LIBRARY else NavigationTabOld.SONGS
                            ACTION_ARTISTS -> if (appDesignVariant == AppDesignVariantType.NEW) NavigationTab.LIBRARY else NavigationTabOld.ARTISTS
                            ACTION_ALBUMS -> if (appDesignVariant == AppDesignVariantType.NEW) NavigationTab.LIBRARY else NavigationTabOld.ALBUMS
                            ACTION_PLAYLISTS -> if (appDesignVariant == AppDesignVariantType.NEW) NavigationTab.LIBRARY else NavigationTabOld.PLAYLISTS
                            else -> null
                        }
                    }

                    if (tabOpenedFromShortcut != null && appDesignVariant == AppDesignVariantType.NEW) {
                        var filter by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
                        filter = when (intent?.action) {
                            ACTION_SONGS -> LibraryFilter.SONGS
                            ACTION_ARTISTS -> LibraryFilter.ARTISTS
                            ACTION_ALBUMS -> LibraryFilter.ALBUMS
                            ACTION_PLAYLISTS -> LibraryFilter.PLAYLISTS
                            else -> LibraryFilter.LIBRARY
                        }
                    }

                    val topLevelScreens = listOf(
                        Screens.Home.route,
                        Screens.Explore.route,
                        Screens.Library.route,
                        Screens.Songs.route,
                        Screens.Artists.route,
                        Screens.Albums.route,
                        Screens.Playlists.route,
                        "settings"
                    )

                    val (query, onQueryChange) = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                        mutableStateOf(TextFieldValue())
                    }

                    var active by rememberSaveable { mutableStateOf(false) }
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

                    val onSearch: (String) -> Unit = {
                        if (it.isNotEmpty()) {
                            onActiveChange(false)
                            navController.navigate("search/${it.urlEncode()}")
                            if (dataStore[PauseSearchHistoryKey] != true) {
                                database.query { insert(SearchHistory(query = it)) }
                            }
                        }
                    }

                    var openSearchImmediately by remember {
                        mutableStateOf(intent?.action == ACTION_SEARCH)
                    }

                    val shouldShowSearchBar = remember(active, navBackStackEntry, inSelectMode?.value, showQRScanner) {
                        !showQRScanner && (active || navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                navBackStackEntry?.destination?.route?.startsWith("search/") == true) &&
                                inSelectMode?.value != true
                    }

                    val shouldShowNavigationBar = remember(navBackStackEntry, active, showQRScanner) {
                        !showQRScanner && (navBackStackEntry?.destination?.route == null ||
                                navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } && !active)
                    }

                    val navigationBarHeight by animateDpAsState(
                        targetValue = if (shouldShowNavigationBar) NavigationBarHeight else 0.dp,
                        animationSpec = NavigationBarAnimationSpec,
                        label = ""
                    )

                    val playerBottomSheetState = rememberBottomSheetState(
                        dismissedBound = 0.dp,
                        collapsedBound = bottomInset + (if (shouldShowNavigationBar) NavigationBarHeight else 0.dp) + MiniPlayerHeight,
                        expandedBound = maxHeight,
                    )

                    val playerAwareWindowInsets = remember(bottomInset, shouldShowNavigationBar, playerBottomSheetState.isDismissed) {
                        var bottom = bottomInset
                        if (shouldShowNavigationBar) bottom += NavigationBarHeight
                        if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                    }

                    val searchBarScrollBehavior = appBarScrollBehavior(
                        canScroll = {
                            navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )

                    val topAppBarScrollBehavior = appBarScrollBehavior(
                        canScroll = {
                            navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )

                    val (firstSetupPassed) = rememberPreference(FirstSetupPassed, false)

                    LaunchedEffect(Unit) {
                        if (!firstSetupPassed) {
                            navController.navigate("setup_wizard")
                        }
                    }

                    LaunchedEffect(navBackStackEntry) {
                        if (navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                            val searchQuery = withContext(Dispatchers.IO) {
                                if (navBackStackEntry?.arguments?.getString("query")?.contains("%") == true) {
                                    navBackStackEntry?.arguments?.getString("query")!!
                                } else {
                                    URLDecoder.decode(navBackStackEntry?.arguments?.getString("query")!!, "UTF-8")
                                }
                            }
                            onQueryChange(TextFieldValue(searchQuery, TextRange(searchQuery.length)))
                        } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                            onQueryChange(TextFieldValue())
                        }
                        searchBarScrollBehavior.state.resetHeightOffset()
                        topAppBarScrollBehavior.state.resetHeightOffset()
                    }

                    LaunchedEffect(active) {
                        if (active) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                        }
                    }

                    LaunchedEffect(playerConnection) {
                        val player = playerConnection?.player ?: return@LaunchedEffect
                        if (player.currentMediaItem == null) {
                            if (!playerBottomSheetState.isDismissed) playerBottomSheetState.dismiss()
                        } else {
                            if (playerBottomSheetState.isDismissed) playerBottomSheetState.collapseSoft()
                        }
                    }

                    DisposableEffect(playerConnection, playerBottomSheetState) {
                        val player = playerConnection?.player ?: return@DisposableEffect onDispose { }
                        val listener = object : Player.Listener {
                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                    mediaItem != null && playerBottomSheetState.isDismissed) {
                                    playerBottomSheetState.collapseSoft()
                                }
                            }
                        }
                        player.addListener(listener)
                        onDispose { player.removeListener(listener) }
                    }

                    val coroutineScope = rememberCoroutineScope()
                    var sharedSong: SongItem? by remember { mutableStateOf(null) }

                    DisposableEffect(Unit) {
                        val listener = Consumer<Intent> { intent ->
                            val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return@Consumer
                            when (val path = uri.pathSegments.firstOrNull()) {
                                "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                                    if (playlistId.startsWith("OLAK5uy_")) {
                                        coroutineScope.launch {
                                            YouTube.albumSongs(playlistId).onSuccess { songs ->
                                                songs.firstOrNull()?.album?.id?.let { browseId ->
                                                    navController.navigate("album/$browseId")
                                                }
                                            }.onFailure { reportException(it) }
                                        }
                                    } else {
                                        navController.navigate("online_playlist/$playlistId")
                                    }
                                }
                                "channel", "c" -> uri.lastPathSegment?.let { artistId ->
                                    navController.navigate("artist/$artistId")
                                }
                                else -> when {
                                    path == "watch" -> uri.getQueryParameter("v")
                                    uri.host == "youtu.be" -> path
                                    else -> null
                                }?.let { videoId ->
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            YouTube.queue(listOf(videoId))
                                        }.onSuccess {
                                            playerConnection?.playQueue(
                                                YouTubeQueue(
                                                    WatchEndpoint(videoId = it.firstOrNull()?.id),
                                                    it.firstOrNull()?.toMediaMetadata()
                                                )
                                            )
                                        }.onFailure { reportException(it) }
                                    }
                                }
                            }
                        }

                        addOnNewIntentListener(listener)
                        onDispose { removeOnNewIntentListener(listener) }
                    }

                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalContentColor provides if (pureBlack) Color.White else contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                        LocalSyncUtils provides syncUtils
                    ) {
                        // QR Scanner takes full priority when active
                        if (showQRScanner) {
                            YouTubeMusicQRScanner(
                                onBackClick = { showQRScanner = false },
                                onLinkScanned = { url ->
                                    showQRScanner = false
                                    qrScannedUrl = url
                                    showQRResultDialog = true
                                }
                            )
                        } else {
                            // Normal app content when QR scanner is not active
                            if (appDesignVariant == AppDesignVariantType.NEW) {
                                NavHost(
                                    navController = navController,
                                    startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                        NavigationTab.HOME -> Screens.Home
                                        NavigationTab.EXPLORE -> Screens.Explore
                                        NavigationTabOld.SONGS -> Screens.Songs
                                        NavigationTabOld.ARTISTS -> Screens.Artists
                                        NavigationTabOld.ALBUMS -> Screens.Albums
                                        NavigationTabOld.PLAYLISTS -> Screens.Playlists
                                        else -> Screens.Library
                                    }.route,
                                    enterTransition = {
                                        if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                                            fadeIn(tween(250))
                                        } else {
                                            fadeIn(tween(250)) + slideInHorizontally { it / 2 }
                                        }
                                    },
                                    exitTransition = {
                                        if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                                            fadeOut(tween(200))
                                        } else {
                                            fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
                                        }
                                    },
                                    popEnterTransition = {
                                        if ((initialState.destination.route in topLevelScreens || initialState.destination.route?.startsWith(
                                                "search/"
                                            ) == true) && targetState.destination.route in topLevelScreens
                                        ) {
                                            fadeIn(tween(250))
                                        } else {
                                            fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
                                        }
                                    },
                                    popExitTransition = {
                                        if ((initialState.destination.route in topLevelScreens || initialState.destination.route?.startsWith(
                                                "search/"
                                            ) == true) && targetState.destination.route in topLevelScreens
                                        ) {
                                            fadeOut(tween(200))
                                        } else {
                                            fadeOut(tween(200)) + slideOutHorizontally { it / 2 }
                                        }
                                    },
                                    modifier = Modifier.nestedScroll(
                                        if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                            navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                                            searchBarScrollBehavior.nestedScrollConnection
                                        } else {
                                            topAppBarScrollBehavior.nestedScrollConnection
                                        }
                                    )
                                ) {
                                    navigationBuilder(navController, topAppBarScrollBehavior)
                                }
                            } else {
                                NavHost(
                                    navController = navController,
                                    startDestination = when (tabOpenedFromShortcut ?: defaultOpenTabOld) {
                                        NavigationTabOld.HOME -> Screens.Home
                                        NavigationTabOld.EXPLORE -> Screens.Explore
                                        NavigationTabOld.SONGS -> Screens.Songs
                                        NavigationTabOld.ARTISTS -> Screens.Artists
                                        NavigationTabOld.ALBUMS -> Screens.Albums
                                        NavigationTabOld.PLAYLISTS -> Screens.Playlists
                                        else -> Screens.Library
                                    }.route,
                                    enterTransition = {
                                        if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                                            fadeIn(tween(250))
                                        } else {
                                            fadeIn(tween(250)) + slideInHorizontally { it / 2 }
                                        }
                                    },
                                    exitTransition = {
                                        if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                                            fadeOut(tween(200))
                                        } else {
                                            fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
                                        }
                                    },
                                    popEnterTransition = {
                                        if ((initialState.destination.route in topLevelScreens || initialState.destination.route?.startsWith(
                                                "search/"
                                            ) == true) && targetState.destination.route in topLevelScreens
                                        ) {
                                            fadeIn(tween(250))
                                        } else {
                                            fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
                                        }
                                    },
                                    popExitTransition = {
                                        if ((initialState.destination.route in topLevelScreens || initialState.destination.route?.startsWith(
                                                "search/"
                                            ) == true) && targetState.destination.route in topLevelScreens
                                        ) {
                                            fadeOut(tween(200))
                                        } else {
                                            fadeOut(tween(200)) + slideOutHorizontally { it / 2 }
                                        }
                                    },
                                    modifier = Modifier.nestedScroll(
                                        if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                            navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                                            searchBarScrollBehavior.nestedScrollConnection
                                        } else {
                                            topAppBarScrollBehavior.nestedScrollConnection
                                        }
                                    )
                                ) {
                                    navigationBuilder(navController, topAppBarScrollBehavior)
                                }
                            }

                            val currentTitle = remember(navBackStackEntry) {
                                when (navBackStackEntry?.destination?.route) {
                                    Screens.Home.route -> R.string.home
                                    Screens.Explore.route -> R.string.explore
                                    Screens.Library.route -> R.string.filter_library
                                    Screens.Songs.route -> R.string.songs
                                    Screens.Artists.route -> R.string.artists
                                    Screens.Albums.route -> R.string.albums
                                    Screens.Playlists.route -> R.string.playlists
                                    else -> null
                                }
                            }

                            if (!active && navBackStackEntry?.destination?.route in topLevelScreens &&
                                navBackStackEntry?.destination?.route != "settings") {
                                TopAppBar(
                                    title = {
                                        Text(
                                            text = currentTitle?.let { stringResource(it) } ?: "",
                                            style = MaterialTheme.typography.titleLarge,
                                        )
                                    },
                                    actions = {
                                        // Search icon (rightmost)
                                        IconButton(
                                            onClick = { onActiveChange(true) }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.search_icon),
                                                contentDescription = stringResource(R.string.search)
                                            )
                                        }

                                        // QR icon with Lottie animation (center)
                                        var isQrIconPlaying by remember { mutableStateOf(false) }
                                        val qrComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.qrscanme))
                                        val qrProgress by animateLottieCompositionAsState(
                                            composition = qrComposition,
                                            isPlaying = isQrIconPlaying,
                                            iterations = LottieConstants.IterateForever,
                                            speed = 0.75f
                                        )

                                        IconButton(
                                            onClick = { showQRScanner = true }
                                        ) {
                                            LottieAnimation(
                                                composition = qrComposition,
                                                progress = qrProgress,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        // Settings icon (leftmost in actions)
                                        IconButton(
                                            onClick = { navController.navigate("settings") }
                                        ) {
                                            BadgedBox(
                                                badge = {
                                                    if (latestVersionName != BuildConfig.VERSION_NAME) {
                                                        Badge()
                                                    }
                                                }
                                            ) {
                                                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.setting))
                                                val progress by animateLottieCompositionAsState(composition)
                                                LottieAnimation(
                                                    composition = composition,
                                                    progress = progress,
                                                    modifier = Modifier.size(45.dp)
                                                )
                                            }
                                        }
                                    },
                                    scrollBehavior = searchBarScrollBehavior
                                )

                            } else if (active || navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                                TopSearch(
                                    query = query,
                                    onQueryChange = onQueryChange,
                                    onSearch = onSearch,
                                    active = active,
                                    onActiveChange = onActiveChange,
                                    scrollBehavior = searchBarScrollBehavior,
                                    placeholder = {
                                        Text(text = stringResource(
                                            when (searchSource) {
                                                SearchSource.LOCAL -> R.string.search_library
                                                SearchSource.ONLINE -> R.string.search_yt_music
                                            }
                                        ))
                                    },
                                    leadingIcon = {
                                        IconButton(
                                            onClick = {
                                                when {
                                                    active -> onActiveChange(false)
                                                    !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                        navController.navigateUp()
                                                    }
                                                    else -> onActiveChange(true)
                                                }
                                            },
                                            onLongClick = {
                                                when {
                                                    active -> {}
                                                    !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                        navController.backToMain()
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painterResource(
                                                    if (active || !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                                        R.drawable.back_icon
                                                    } else {
                                                        R.drawable.search_icon
                                                    }
                                                ),
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (active) {
                                            if (query.text.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { onQueryChange(TextFieldValue("")) }
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.close),
                                                        contentDescription = null
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { searchSource = searchSource.toggle() }
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
                                    },
                                    modifier = Modifier
                                        .focusRequester(searchBarFocusRequester)
                                        .align(Alignment.TopCenter),
                                    focusRequester = searchBarFocusRequester,
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
                                                unfocusedIndicatorColor = Color.Transparent,
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
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(bottom = if (!playerBottomSheetState.isDismissed) MiniPlayerHeight else 0.dp)
                                            .navigationBarsPadding()
                                    ) { searchSource ->
                                        when (searchSource) {
                                            SearchSource.LOCAL -> LocalSearchScreen(
                                                query = query.text,
                                                navController = navController,
                                                onDismiss = { onActiveChange(false) }
                                            )
                                            SearchSource.ONLINE -> OnlineSearchScreen(
                                                query = query.text,
                                                onQueryChange = onQueryChange,
                                                navController = navController,
                                                onSearch = {
                                                    navController.navigate("search/${it.urlEncode()}")
                                                    if (dataStore[PauseSearchHistoryKey] != true) {
                                                        database.query { insert(SearchHistory(query = it)) }
                                                    }
                                                },
                                                onDismiss = { onActiveChange(false) }
                                            )
                                        }
                                    }
                                }
                            }

                            Box {
                                if (firstSetupPassed) {
                                    BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController
                                    )
                                }
                            }

                            if (shouldShowNavigationBar) {
                                NavigationBar(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset {
                                            if (navigationBarHeight == 0.dp) {
                                                IntOffset(x = 0, y = (bottomInset + NavigationBarHeight).roundToPx())
                                            } else {
                                                val slideOffset = (bottomInset + NavigationBarHeight) *
                                                        playerBottomSheetState.progress.coerceIn(0f, 1f)
                                                val hideOffset = (bottomInset + NavigationBarHeight) *
                                                        (1 - navigationBarHeight / NavigationBarHeight)
                                                IntOffset(x = 0, y = (slideOffset + hideOffset).roundToPx())
                                            }
                                        },
                                    containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    navigationItems.fastForEach { screen ->
                                        val isSelected = navBackStackEntry?.destination?.hierarchy?.any {
                                            it.route == screen.route
                                        } == true

                                        NavigationBarItem(
                                            selected = isSelected,
                                            icon = {
                                                Icon(
                                                    painter = painterResource(
                                                        id = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                                                    ),
                                                    contentDescription = null,
                                                )
                                            },
                                            label = {
                                                if (slimNav) {
                                                    Text(
                                                        text = stringResource(screen.titleId),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            },
                                            onClick = {
                                                if (isSelected) {
                                                    navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
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
                            }

                            BottomSheetMenu(
                                state = LocalMenuState.current,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }

                        sharedSong?.let { song ->
                            playerConnection?.let { _ ->
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
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

                        // QR Code Scanner Result Dialog
// QR Code Scanner Result Dialog
                        // QR Code Scanner Result Dialog
                        // QR Code Scanner Result Dialog
                        // QR Code Scanner Result Dialog
                        if (showQRResultDialog) {
                            val youtubeUrlPattern = remember {
                                Regex(
                                    "(?:https?:\\/\\/)?(?:www\\.)?(?:youtube\\.com|youtu\\.be)\\/" +
                                            "(?:watch\\?v=|playlist\\?list=|music\\.)([a-zA-Z0-9_-]+)"
                                )
                            }
                            val matchResult = remember(qrScannedUrl) { youtubeUrlPattern.find(qrScannedUrl) }
                            val isPlaylist = remember(qrScannedUrl) {
                                qrScannedUrl.contains("playlist") || qrScannedUrl.contains("list=")
                            }

                            // Extract video ID for thumbnail
                            val videoId = remember(qrScannedUrl) {
                                if (matchResult != null && !isPlaylist) {
                                    val pattern = Regex("(?:youtube\\.com\\/watch\\?v=|youtu\\.be\\/)([^&\\n?]+)")
                                    pattern.find(qrScannedUrl)?.groupValues?.get(1)
                                } else null
                            }

                            // Image loading state
                            val imageLoader = LocalImageLoader.current
                            var thumbnailState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

                            ModalBottomSheet(
                                onDismissRequest = { showQRResultDialog = false },
                                containerColor = if (matchResult != null)
                                    MaterialTheme.colorScheme.surface
                                else
                                    MaterialTheme.colorScheme.errorContainer,
                                contentColor = if (matchResult != null)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (matchResult != null) {
                                            if (isPlaylist) "Playlist Found" else "Song Found"
                                        } else "Invalid Link",
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )

                                    // Show thumbnail image if available (for videos only)
                                    if (videoId != null) {
                                        val thumbnailUrl = remember(videoId) {
                                            "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(200.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(thumbnailUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Video thumbnail",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                onState = { state -> thumbnailState = state }
                                            )

                                            when (thumbnailState) {
                                                is AsyncImagePainter.State.Loading -> {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.align(Alignment.Center)
                                                    )
                                                }
                                                is AsyncImagePainter.State.Error -> {
                                                    Icon(
                                                        imageVector = Icons.Default.BrokenImage,
                                                        contentDescription = "Error loading thumbnail",
                                                        modifier = Modifier.align(Alignment.Center),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                                else -> {}
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }

                                    Text(
                                        text = if (matchResult != null) {
                                            if (isPlaylist) "Do you want to add this playlist?"
                                            else " play this song?"
                                        } else {
                                            "The scanned QR code doesn't contain a valid YouTube Music link"
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(bottom = 24.dp)
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Close button - now using primary color
                                        Button(
                                            onClick = { showQRResultDialog = false },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Close")
                                        }

                                        if (matchResult != null) {
                                            Spacer(modifier = Modifier.width(16.dp))
                                            // Action button - now using primary color
                                            Button(
                                                onClick = {
                                                    showQRResultDialog = false
                                                    handleYoutubeUrl(qrScannedUrl)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(if (isPlaylist) "Open" else "Play")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Network Bottom Sheet
                        if (showNetworkBottomSheet) {
                            ModalBottomSheet(
                                onDismissRequest = { showNetworkBottomSheet = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                NoNetworkBottomSheet(
                                    onCancel = { showNetworkBottomSheet = false },
                                    onRetry = {
                                        val connectivityManager = context.getSystemService(
                                            Context.CONNECTIVITY_SERVICE
                                        ) as ConnectivityManager
                                        isNetworkAvailable = connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
                                        showNetworkBottomSheet = !isNetworkAvailable
                                    },
                                    isNetworkAvailable = isNetworkAvailable
                                )
                            }
                        }

                        LaunchedEffect(shouldShowSearchBar, openSearchImmediately) {
                            if (shouldShowSearchBar && openSearchImmediately) {
                                onActiveChange(true)
                                try {
                                    delay(100)
                                    searchBarFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                                openSearchImmediately = false
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }

    companion object {
        const val ACTION_SEARCH = "com.music.vivi.action.SEARCH"
        const val ACTION_SONGS = "com.music.vivi.action.SONGS"
        const val ACTION_ARTISTS = "com.music.vivi.action.ARTISTS"
        const val ACTION_ALBUMS = "com.music.vivi.action.ALBUMS"
        const val ACTION_PLAYLISTS = "com.music.vivi.action.PLAYLISTS"
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoNetworkBottomSheet(
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    isNetworkAvailable: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showProgress by remember { mutableStateOf(false) }
    var retryAttempted by remember { mutableStateOf(false) }
    var networkRestored by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    val errorComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lostconnection))
    val retryComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.searchingnetwork))
    val successComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.gotnetwork))

    val errorProgress by animateLottieCompositionAsState(errorComposition, iterations = LottieConstants.IterateForever)
    val retryProgress by animateLottieCompositionAsState(retryComposition, iterations = LottieConstants.IterateForever)
    val successProgress by animateLottieCompositionAsState(successComposition, iterations = 1)

    val showSheetState = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(isNetworkAvailable) {
        if (!isNetworkAvailable) {
            showSheetState.value = true
        } else {
            if (showSheetState.value) {
                networkRestored = true
                showSuccessAnimation = true
                delay(5000)
                showSuccessAnimation = false
                networkRestored = false
                showSheetState.value = false
            }
        }
    }

    LaunchedEffect(showSheetState.value) {
        if (showSheetState.value) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    if (showSheetState.value) {
        ModalBottomSheet(
            onDismissRequest = onCancel,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated status box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        networkRestored && showSuccessAnimation -> {
                            LottieAnimation(successComposition, successProgress, modifier = Modifier.fillMaxSize())
                        }
                        showProgress -> {
                            LottieAnimation(retryComposition, retryProgress, modifier = Modifier.fillMaxSize())
                        }
                        else -> {
                            LottieAnimation(errorComposition, errorProgress, modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                // Status Texts
                when {
                    networkRestored && showSuccessAnimation -> {
                        Spacer(Modifier.height(8.dp))
                        Text("Connection Restored!", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text("You're back online", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    showProgress -> {
                        Spacer(Modifier.height(16.dp))
                        Text("Reconnecting...", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("Checking network connection", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (retryAttempted) "Still Offline" else "Connection Lost",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Check your network settings", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(Modifier.height(32.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        showProgress = true
                                        retryAttempted = false
                                        delay(3000)

                                        val connectivityManager =
                                            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                                        val networkAvailable =
                                            connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true

                                        showProgress = false
                                        if (networkAvailable) {
                                            networkRestored = true
                                            showSuccessAnimation = true
                                            delay(5000)
                                            showSuccessAnimation = false
                                            networkRestored = false
                                            showSheetState.value = false
                                        } else {
                                            retryAttempted = true
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Retry")
                            }

                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }



@Composable
fun NotificationPermissionPreference() {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }
    val checkNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PermissionChecker.PERMISSION_GRANTED
        } else {
            true
        }
    }
    LaunchedEffect(Unit) {
        permissionGranted = checkNotificationPermission()
    }
    SwitchPreference(
        title = { Text(stringResource(R.string.enable_notifications)) },
        icon = {
            Icon(
                painter = painterResource(id = if (permissionGranted) R.drawable.notification_on else R.drawable.notifications_off),
                contentDescription = null
            )
        },
        checked = permissionGranted,
        onCheckedChange = { checked ->
            if (checked && !permissionGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    )
}

@Composable
fun SwitchPreference(
    title: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(24.dp)) {
                icon()
            }
            Spacer(Modifier.width(16.dp))
            Box(Modifier.weight(1f)) {
                title()
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}