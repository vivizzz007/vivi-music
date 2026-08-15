package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.music.innertube.YouTube
import kotlin.system.exitProcess
import com.music.innertube.YouTubeExtractor
import com.music.innertube.models.SongItem
import com.music.vivi.desktop.player.PlayerController
import com.music.vivi.sync.PlaybackSnapshot
import com.music.vivi.sync.SyncServer
import com.music.vivi.sync.TrackRef
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main() = application {
    // Configure the shared YouTube client exactly like the Android App.onCreate() does,
    // honouring the saved content language/region (or the OS default).
    val initialSettings = DesktopSettings.load()
    YouTube.locale = resolveYouTubeLocale(initialSettings.contentLanguage, initialSettings.contentCountry)
    YouTubeExtractor.cacheDir = File(System.getProperty("user.home"), ".vivimusic/cache").apply { mkdirs() }
    LoginManager.restore()
    DesktopSettings.ensureFirstLaunchDate()
    DeveloperOptions.load()

    var language by remember { mutableStateOf(DesktopSettings.load().language) }
    var themeMode by remember { mutableStateOf(ThemeMode.from(DesktopSettings.load().darkMode)) }
    var accent by remember { mutableStateOf(argbIntToColor(DesktopSettings.load().accentColor)) }
    var pureBlack by remember { mutableStateOf(DesktopSettings.load().pureBlack) }

    fun saveTheme() {
        DesktopSettings.save(
            DesktopSettings.load().copy(
                darkMode = themeMode.key,
                accentColor = colorToArgbInt(accent),
                pureBlack = pureBlack,
            )
        )
    }

    Window(onCloseRequest = ::exitApplication, title = "VIVI Music — desktop") {
        AppTheme(mode = themeMode, accent = accent, pureBlack = pureBlack) {
            // Make all text selectable (copyable) across the whole app:
            // errors, options, settings, LAN server details, etc.
            SelectionContainer {
                if (language.isBlank()) {
                    LanguageSelectionScreen { selected ->
                        language = selected
                        DesktopSettings.save(DesktopSettings.load().copy(language = selected))
                    }
                } else {
                    App(
                        language = language,
                        onLanguageChange = { selected ->
                            language = selected
                            DesktopSettings.save(DesktopSettings.load().copy(language = selected))
                        },
                        themeMode = themeMode,
                        accent = accent,
                        onThemeModeChange = {
                            themeMode = it
                            saveTheme()
                        },
                        onAccentChange = {
                            accent = it
                            saveTheme()
                        },
                        pureBlack = pureBlack,
                        onPureBlackChange = {
                            pureBlack = it
                            saveTheme()
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun App(
    language: String,
    onLanguageChange: (String) -> Unit,
    themeMode: ThemeMode,
    accent: Color,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentChange: (Color) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
) {
    var backStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
    val player = remember { PlayerController() }
    val playerState by player.state.collectAsState()
    val nowPlaying = playerState.current
    val isPlaying = playerState.isPlaying

    var autoPlayNext by remember { mutableStateOf(DesktopSettings.load().autoPlayNext) }
    player.autoPlayNext = autoPlayNext

    // Guest sessions need a visitorData (like the Android app) or YouTube flags
    // the requests as bots and 403s audio playback.
    LaunchedEffect(Unit) { GuestSession.ensure() }

    var isLoggedIn by remember { mutableStateOf(LoginManager.isLoggedIn()) }
    var accountName by remember { mutableStateOf(DesktopSettings.load().accountName) }
    var sidebarCollapsed by remember { mutableStateOf(DesktopSettings.load().sidebarCollapsed) }
    var contentLanguage by remember { mutableStateOf(DesktopSettings.load().contentLanguage) }
    var contentCountry by remember { mutableStateOf(DesktopSettings.load().contentCountry) }
    var syncedLyrics by remember { mutableStateOf(DesktopSettings.load().syncedLyrics) }
    var audioQuality by remember { mutableStateOf(DesktopSettings.load().audioQuality) }
    var rememberShuffleRepeat by remember { mutableStateOf(DesktopSettings.load().rememberShuffleRepeat) }
    var persistentQueue by remember { mutableStateOf(DesktopSettings.load().persistentQueue) }
    var lyricsTextSize by remember { mutableStateOf(DesktopSettings.load().lyricsTextSize) }

    // Persistent queue: restore the saved queue on startup (paused, not auto-played).
    LaunchedEffect(Unit) {
        val s = DesktopSettings.load()
        if (s.persistentQueue && s.queueJson.isNotBlank()) {
            runCatching { queueJson.decodeFromString<List<NowPlaying>>(s.queueJson) }
                .getOrNull()
                ?.let { player.restoreQueue(it, s.queueIndex) }
        }
    }

    // Persistent queue: save the queue whenever it changes.
    LaunchedEffect(playerState.queue, playerState.index, persistentQueue) {
        if (persistentQueue && playerState.queue.isNotEmpty()) {
            val s = DesktopSettings.load()
            DesktopSettings.save(
                s.copy(queueJson = queueJson.encodeToString(playerState.queue), queueIndex = playerState.index)
            )
        }
    }

    val current = backStack.last()

    val navigate: (Screen) -> Unit = { backStack = backStack + it }
    val openRoot: (Screen) -> Unit = { backStack = listOf(it) }
    val goBack: () -> Unit = { if (backStack.size > 1) backStack = backStack.dropLast(1) }
    fun songToNowPlaying(song: SongItem): NowPlaying = NowPlaying(
        videoId = song.id,
        title = song.title,
        artist = song.artists.joinToString(", ") { it.name },
        thumbnail = song.thumbnail,
    )

    val playSong: (SongItem) -> Unit = { song -> player.play(songToNowPlaying(song)) }
    val addToQueue: (SongItem) -> Unit = { song -> player.addToQueue(songToNowPlaying(song)) }
    val playAll: (List<SongItem>) -> Unit = { songs -> player.playAll(songs.map(::songToNowPlaying)) }
    val shuffleAll: (List<SongItem>) -> Unit = { songs ->
        if (!playerState.isShuffle) player.toggleShuffle()
        player.playAll(songs.shuffled().map(::songToNowPlaying))
    }

    val scope = rememberCoroutineScope()
    // Pre-releases are on by default for nightly/alpha/beta/rc builds (those
    // channels only publish pre-releases), and off by default for stable.
    var includePreReleases by remember {
        mutableStateOf(
            DesktopSettings.load().includePreReleases || AppInfo.CHANNEL.lowercase() != "stable"
        )
    }
    var updateIntervalHours by remember { mutableStateOf(DesktopSettings.load().updateCheckIntervalHours) }
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    val devEnabled by DeveloperOptions.enabled.collectAsState()
    val devMode by DeveloperOptions.mode.collectAsState()

    fun runUpdateCheck() {
        updateStatus = UpdateStatus.Checking
        scope.launch {
            val result = withContext(Dispatchers.IO) { UpdateChecker.check(includePreReleases) }
            updateStatus = result
        }
    }

    // Automatic update check on startup.
    LaunchedEffect(Unit) { runUpdateCheck() }

    // Periodic update check, at the user-selected interval (0 = manual only).
    LaunchedEffect(updateIntervalHours) {
        if (updateIntervalHours <= 0) return@LaunchedEffect
        while (true) {
            delay(updateIntervalHours * 3_600_000L)
            runUpdateCheck()
        }
    }

    // Non-invasive update notification, shown once per new version.
    var showUpdateNotification by remember { mutableStateOf(false) }
    var updateNotifiedVersion by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(updateStatus) {
        val available = updateStatus as? UpdateStatus.Available
        if (available != null && available.version != updateNotifiedVersion) {
            updateNotifiedVersion = available.version
            showUpdateNotification = true
        }
    }

    // ---- Device sync (Android <-> desktop) ----
    val syncManager = remember { DesktopSyncManager() }

    // Push the local playback state to the peer when the track, play/pause
    // state, or queue changes (position is sent as a best-effort snapshot).
    LaunchedEffect(syncManager) {
        player.state
            .map { s ->
                PlaybackSyncKey(
                    trackId = s.current?.videoId,
                    isPlaying = s.isPlaying,
                    index = s.index,
                    queue = s.queue.map { it.videoId },
                )
            }
            .distinctUntilChanged()
            .collect {
                val s = player.state.value
                val current = s.current
                if (current != null) {
                    syncManager.updatePlayback(
                        PlaybackSnapshot(
                            trackId = current.videoId,
                            trackTitle = current.title,
                            positionMs = s.positionMs,
                            isPlaying = s.isPlaying,
                            queue = s.queue.map { np ->
                                TrackRef(id = np.videoId, title = np.title, artist = np.artist, thumbnail = np.thumbnail)
                            },
                            queueIndex = s.index,
                        )
                    )
                }
            }
    }

    // Apply incoming playback snapshots from the peer.
    LaunchedEffect(syncManager) {
        syncManager.incomingPlayback.collect { pb ->
            val tracks = pb.queue.map { ref ->
                NowPlaying(videoId = ref.id, title = ref.title, artist = ref.artist.orEmpty(), thumbnail = ref.thumbnail)
            }
            if (tracks.isNotEmpty()) {
                player.applyRemotePlayback(tracks, pb.queueIndex, pb.positionMs, pb.isPlaying)
            }
        }
    }

    // Apply incoming settings snapshots from the peer.
    LaunchedEffect(syncManager) {
        syncManager.incomingSettings.collect { settings ->
            settings["darkMode"]?.let { mode ->
                onThemeModeChange(
                    when (mode) {
                        "ON" -> ThemeMode.DARK
                        "OFF" -> ThemeMode.LIGHT
                        else -> ThemeMode.SYSTEM
                    }
                )
            }
            settings["appLanguage"]?.let { lang ->
                val normalized = Languages.fromMobileCode(lang)
                if (lang != "SYSTEM_DEFAULT" && Languages.all.any { it.code == normalized }) {
                    onLanguageChange(normalized)
                }
            }
            settings["selectedThemeColor"]?.toIntOrNull()?.let { argb ->
                onAccentChange(argbIntToColor(argb))
            }
        }
    }

    // Push the local settings when they change (also once on startup).
    LaunchedEffect(syncManager, language, themeMode, accent) {
        syncManager.updateSettings(desktopSettingsMap(language, themeMode, accent))
    }

    Row(Modifier.fillMaxSize()) {
        Sidebar(
            language = language,
            current = current,
            collapsed = sidebarCollapsed,
            onToggleCollapsed = {
                sidebarCollapsed = !sidebarCollapsed
                DesktopSettings.save(DesktopSettings.load().copy(sidebarCollapsed = sidebarCollapsed))
            },
            onSelect = openRoot,
        )
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (current) {
                    is Screen.Home -> HomeScreen(
                        language = language,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onPlayAll = playAll,
                        onOpenBrowse = { browseId, params -> navigate(Screen.Browse(browseId, params)) },
                    )
                    is Screen.Search -> SearchScreen(
                        language = language,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                    )
                    is Screen.Library -> LibraryScreen(
                        language = language,
                        isLoggedIn = isLoggedIn,
                        onOpenLogin = { navigate(Screen.Login) },
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onShuffleAll = shuffleAll,
                    )
                    is Screen.History -> HistoryScreen(
                        language = language,
                        onBack = goBack,
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                    )
                    is Screen.Settings -> SettingsScreen(
                        language = language,
                        themeMode = themeMode,
                        isLoggedIn = isLoggedIn,
                        accountName = accountName,
                        updateStatus = updateStatus,
                        onOpen = navigate,
                    )
                    is Screen.SettingsLanguage -> SettingsLanguageScreen(
                        language = language,
                        onBack = goBack,
                        onLanguageChange = onLanguageChange,
                    )
                    is Screen.SettingsAppearance -> SettingsAppearanceScreen(
                        language = language,
                        onBack = goBack,
                        themeMode = themeMode,
                        accent = accent,
                        onThemeModeChange = onThemeModeChange,
                        onAccentChange = onAccentChange,
                        pureBlack = pureBlack,
                        onPureBlackChange = onPureBlackChange,
                    )
                    is Screen.SettingsPlayer -> SettingsPlayerScreen(
                        language = language,
                        onBack = goBack,
                        autoPlayNext = autoPlayNext,
                        onToggleAutoPlayNext = { checked ->
                            autoPlayNext = checked
                            DesktopSettings.save(DesktopSettings.load().copy(autoPlayNext = checked))
                        },
                        audioQuality = audioQuality,
                        onAudioQualityChange = { q ->
                            audioQuality = q
                            DesktopSettings.save(DesktopSettings.load().copy(audioQuality = q))
                        },
                        rememberShuffleRepeat = rememberShuffleRepeat,
                        onToggleRememberShuffleRepeat = { checked ->
                            rememberShuffleRepeat = checked
                            DesktopSettings.save(DesktopSettings.load().copy(rememberShuffleRepeat = checked))
                        },
                        persistentQueue = persistentQueue,
                        onTogglePersistentQueue = { checked ->
                            persistentQueue = checked
                            DesktopSettings.save(DesktopSettings.load().copy(persistentQueue = checked))
                        },
                    )
                    is Screen.SettingsAccount -> SettingsAccountScreen(
                        language = language,
                        onBack = goBack,
                        isLoggedIn = isLoggedIn,
                        accountName = accountName,
                        onOpenLogin = { navigate(Screen.Login) },
                        onLogout = {
                            LoginManager.logout()
                            isLoggedIn = false
                            accountName = ""
                        },
                    )
                    is Screen.SettingsDevices -> SettingsDevicesScreen(
                        language = language,
                        onBack = goBack,
                        syncManager = syncManager,
                    )
                    is Screen.SettingsContent -> SettingsContentScreen(
                        language = language,
                        onBack = goBack,
                        contentLanguage = contentLanguage,
                        contentCountry = contentCountry,
                        onContentLanguageChange = { code ->
                            contentLanguage = code
                            DesktopSettings.save(DesktopSettings.load().copy(contentLanguage = code))
                            YouTube.locale = resolveYouTubeLocale(contentLanguage, contentCountry)
                        },
                        onContentCountryChange = { code ->
                            contentCountry = code
                            DesktopSettings.save(DesktopSettings.load().copy(contentCountry = code))
                            YouTube.locale = resolveYouTubeLocale(contentLanguage, contentCountry)
                        },
                    )
                    is Screen.SettingsLyrics -> SettingsLyricsScreen(
                        language = language,
                        onBack = goBack,
                        syncedLyrics = syncedLyrics,
                        onToggleSyncedLyrics = { checked ->
                            syncedLyrics = checked
                            DesktopSettings.save(DesktopSettings.load().copy(syncedLyrics = checked))
                        },
                        lyricsTextSize = lyricsTextSize,
                        onLyricsTextSizeChange = { size ->
                            lyricsTextSize = size
                            DesktopSettings.save(DesktopSettings.load().copy(lyricsTextSize = size))
                        },
                    )
                    is Screen.SettingsPrivacy -> SettingsPrivacyScreen(
                        language = language,
                        onBack = goBack,
                        isLoggedIn = isLoggedIn,
                        onLogout = {
                            LoginManager.logout()
                            isLoggedIn = false
                            accountName = ""
                        },
                    )
                    is Screen.SettingsStorage -> SettingsStorageScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.SettingsUpdates -> SettingsUpdatesScreen(
                        language = language,
                        onBack = goBack,
                        updateStatus = updateStatus,
                        includePreReleases = includePreReleases,
                        updateIntervalHours = updateIntervalHours,
                        onIntervalChange = { hours ->
                            updateIntervalHours = hours
                            DesktopSettings.save(DesktopSettings.load().copy(updateCheckIntervalHours = hours))
                        },
                        onTogglePreReleases = { checked ->
                            includePreReleases = checked
                            DesktopSettings.save(DesktopSettings.load().copy(includePreReleases = checked))
                            runUpdateCheck()
                        },
                        onCheckUpdates = { runUpdateCheck() },
                    )
                    is Screen.SettingsAbout -> SettingsAboutScreen(
                        language = language,
                        onBack = goBack,
                        onOpenChangelog = { navigate(Screen.Changelog) },
                    )
                    is Screen.SettingsDeveloper -> SettingsDeveloperScreen(
                        language = language,
                        onBack = goBack,
                        syncManager = syncManager,
                    )
                    is Screen.Album -> AlbumScreen(
                        browseId = current.browseId,
                        language = language,
                        onBack = goBack,
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onPlayAll = playAll,
                        onShuffleAll = shuffleAll,
                    )
                    is Screen.Artist -> ArtistScreen(
                        browseId = current.browseId,
                        language = language,
                        onBack = goBack,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                    )
                    is Screen.Playlist -> PlaylistScreen(
                        playlistId = current.playlistId,
                        language = language,
                        onBack = goBack,
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onPlayAll = playAll,
                        onShuffleAll = shuffleAll,
                    )
                    is Screen.Browse -> BrowseScreen(
                        browseId = current.browseId,
                        params = current.params,
                        language = language,
                        onBack = goBack,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                    )
                    is Screen.Player -> PlayerScreen(
                        queue = playerState.queue,
                        index = playerState.index,
                        isPlaying = isPlaying,
                        positionMs = playerState.positionMs,
                        durationMs = playerState.durationMs,
                        volume = playerState.volume,
                        isShuffle = playerState.isShuffle,
                        repeatMode = playerState.repeatMode,
                        errorKey = playerState.errorKey,
                        errorDetail = playerState.errorDetail,
                        onTogglePlay = { player.toggle() },
                        onNext = { player.next() },
                        onPrevious = { player.previous() },
                        onSeek = { player.seekTo(it) },
                        onVolume = { player.setVolume(it) },
                        onToggleShuffle = { player.toggleShuffle() },
                        onCycleRepeat = { player.cycleRepeatMode() },
                        language = language,
                        onOpenLyrics = { navigate(Screen.Lyrics) },
                        onOpenQueue = { navigate(Screen.Queue) },
                    )
                    is Screen.Lyrics -> LyricsScreen(
                        nowPlaying = nowPlaying,
                        positionMs = playerState.positionMs,
                        isPlaying = isPlaying,
                        language = language,
                        synced = syncedLyrics,
                        textSizeSp = lyricsTextSize,
                        onBack = goBack,
                    )
                    is Screen.Queue -> QueueScreen(
                        queue = playerState.queue,
                        index = playerState.index,
                        language = language,
                        onBack = goBack,
                        onSkipTo = { player.skipTo(it) },
                        onRemoveAt = { player.removeAt(it) },
                        onClear = { player.clearQueue() },
                        onReorder = { player.reorder(it) },
                    )
                    is Screen.Changelog -> ChangelogScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.Login -> LoginScreen(
                        language = language,
                        onBack = goBack,
                        onLoggedIn = {
                            isLoggedIn = true
                            accountName = DesktopSettings.load().accountName
                        },
                    )
                }
                if (showUpdateNotification && updateStatus is UpdateStatus.Available) {
                    UpdateNotification(
                        status = updateStatus as UpdateStatus.Available,
                        language = language,
                        onDismiss = { showUpdateNotification = false },
                        onDone = { showUpdateNotification = false },
                    )
                }
                if (devEnabled && devMode == DevToolsMode.OVERLAY) {
                    DevToolsOverlay(
                        syncManager = syncManager,
                        language = language,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    )
                }
            }
            MiniPlayer(
                nowPlaying = nowPlaying,
                isPlaying = isPlaying,
                positionMs = playerState.positionMs,
                durationMs = playerState.durationMs,
                onTogglePlay = { player.toggle() },
                onNext = { player.next() },
                onOpen = { navigate(Screen.Player) },
                onOpenQueue = { navigate(Screen.Queue) },
            )
        }
    }

    // Developer tools in a dedicated window (closing it falls back to overlay).
    if (devEnabled && devMode == DevToolsMode.WINDOW) {
        Window(
            onCloseRequest = { DeveloperOptions.setMode(DevToolsMode.OVERLAY) },
            title = "VIVI Music DE — Developer tools",
        ) {
            AppTheme(mode = themeMode, accent = accent, pureBlack = pureBlack) {
                SelectionContainer {
                    DevToolsPanel(syncManager = syncManager, language = language)
                }
            }
        }
    }
}

private data class SidebarEntry(
    val screen: Screen,
    val key: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

/**
 * Collapsible / expandable navigation sidebar (the desktop counterpart of the
 * mobile bottom navigation bar). Selected items use the Material 3 pill style
 * (`secondaryContainer`), matching the mobile `NavigationBarItem` look.
 */
@Composable
fun Sidebar(
    language: String,
    current: Screen,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    onSelect: (Screen) -> Unit,
) {
    val entries = listOf(
        SidebarEntry(Screen.Home, "home", Icons.Outlined.Home, Icons.Filled.Home),
        SidebarEntry(Screen.Search, "search", Icons.Outlined.Search, Icons.Filled.Search),
        SidebarEntry(Screen.Library, "library", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
        SidebarEntry(Screen.History, "history", Icons.Outlined.History, Icons.Filled.History),
        SidebarEntry(Screen.Settings, "settings", Icons.Outlined.Settings, Icons.Filled.Settings),
    )

    val width by animateDpAsState(if (collapsed) 72.dp else 224.dp, label = "sidebarWidth")

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier
                .width(width)
                .fillMaxHeight()
                .padding(horizontal = if (collapsed) 8.dp else 12.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleCollapsed) {
                    Icon(
                        if (collapsed) Icons.Filled.Menu else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                    )
                }
                if (!collapsed) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "VIVI Music",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            entries.forEach { entry ->
                val selected = current == entry.screen
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        )
                        .clickable { onSelect(entry.screen) }
                        .padding(horizontal = if (collapsed) 0.dp else 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
                ) {
                    Icon(
                        if (selected) entry.selectedIcon else entry.icon,
                        contentDescription = Localization.get(language, entry.key),
                        tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!collapsed) {
                        Spacer(Modifier.width(16.dp))
                        Text(
                            Localization.get(language, entry.key),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun MiniPlayer(
    nowPlaying: NowPlaying?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    val np = nowPlaying ?: return
    Surface(tonalElevation = 4.dp, shadowElevation = 4.dp) {
        Column {
            if (durationMs > 0) {
                LinearProgressIndicator(
                    progress = { (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Thumbnail(np.thumbnail, Modifier.size(44.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(np.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        np.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(onClick = onOpenQueue) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    language: String,
    themeMode: ThemeMode,
    isLoggedIn: Boolean,
    accountName: String,
    updateStatus: UpdateStatus,
    onOpen: (Screen) -> Unit,
) {
    val devEnabled by DeveloperOptions.enabled.collectAsState()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text(Localization.get(language, "settings"), style = MaterialTheme.typography.headlineMedium)
        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        SettingsEntryRow(
            language = language,
            icon = Icons.Filled.Translate,
            title = Localization.get(language, "language"),
            subtitle = Languages.name(language),
            onClick = { onOpen(Screen.SettingsLanguage) },
        )
        SettingsEntryRow(
            language = language,
            icon = Icons.Filled.Refresh,
            title = Localization.get(language, "updates"),
            subtitle = if (updateStatus is UpdateStatus.Available) {
                Localization.get(language, "update_available")
            } else {
                AppInfo.FULL_VERSION
            },
            onClick = { onOpen(Screen.SettingsUpdates) },
        )
        SettingsEntryRow(
            language = language,
            icon = Icons.Filled.Palette,
            title = Localization.get(language, "appearance"),
            subtitle = when (themeMode) {
                ThemeMode.SYSTEM -> Localization.get(language, "theme_system")
                ThemeMode.LIGHT -> Localization.get(language, "theme_light")
                ThemeMode.DARK -> Localization.get(language, "theme_dark")
            },
            onClick = { onOpen(Screen.SettingsAppearance) },
        )
        SettingsEntryRow(
            language = language,
            icon = Icons.Filled.GraphicEq,
            title = Localization.get(language, "player_audio"),
            onClick = { onOpen(Screen.SettingsPlayer) },
        )
        SettingsEntryRow(
            language = language,
            icon = Icons.Filled.Person,
            title = Localization.get(language, "account"),
            subtitle = if (isLoggedIn) accountName.ifBlank { "YouTube" } else Localization.get(language, "not_logged_in"),
            onClick = { onOpen(Screen.SettingsAccount) },
        )
        SettingsEntryRow(
            language = language,
            icon = Icons.Filled.Devices,
            title = Localization.get(language, "device_sync"),
            onClick = { onOpen(Screen.SettingsDevices) },
        )
        SettingsEntryRow(
            language = language,
            icon = Icons.Filled.Language,
            title = Localization.get(language, "content"),
            onClick = { onOpen(Screen.SettingsContent) },
        )
        SettingsEntryRow(
            language = language,
            icon = Icons.Filled.Lyrics,
            title = Localization.get(language, "lyrics"),
            onClick = { onOpen(Screen.SettingsLyrics) },
        )
        SettingsEntryRow(
            language = language,
            icon = Icons.Filled.Security,
            title = Localization.get(language, "privacy"),
            onClick = { onOpen(Screen.SettingsPrivacy) },
        )
        SettingsEntryRow(
            language = language,
            icon = Icons.Filled.Storage,
            title = Localization.get(language, "storage"),
            onClick = { onOpen(Screen.SettingsStorage) },
        )
        if (devEnabled) {
            SettingsEntryRow(
                language = language,
                icon = Icons.Filled.Build,
                title = Localization.get(language, "developer_options"),
                onClick = { onOpen(Screen.SettingsDeveloper) },
            )
        }
        SettingsEntryRow(
            language = language,
            icon = Icons.Filled.Info,
            title = Localization.get(language, "about"),
            onClick = { onOpen(Screen.SettingsAbout) },
        )
    }
}

@Composable
private fun SettingsEntryRow(
    language: String,
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun DeviceSyncSection(language: String, syncManager: DesktopSyncManager) {
    var serverUrl by remember {
        val saved = DesktopSettings.load().serverUrl
        // Default to the same relay the Android app uses; treat the old
        // hardcoded localhost placeholder as "not set" so it gets migrated.
        mutableStateOf(if (saved.isBlank() || saved == "wss://localhost:8080") SyncServer.DEFAULT_URL else saved)
    }
    val connectionState by syncManager.connectionState.collectAsState()
    val status by syncManager.status.collectAsState()
    val pairCode by syncManager.pairCode.collectAsState()
    val pairCodeExpiresAt by syncManager.pairCodeExpiresAt.collectAsState()
    val paired by syncManager.paired.collectAsState()
    val lanRunning by syncManager.lanRunning.collectAsState()
    val lanAddress by syncManager.lanAddress.collectAsState()
    val syncedSettings by syncManager.syncedSettings.collectAsState()

    // Ticking clock for the pairing-code expiry countdown.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pairCodeExpiresAt) {
        while (pairCodeExpiresAt > 0L) {
            nowMs = System.currentTimeMillis()
            if (nowMs >= pairCodeExpiresAt) break
            delay(1000)
        }
        nowMs = System.currentTimeMillis()
    }
    val remainingMs = (pairCodeExpiresAt - nowMs).coerceAtLeast(0L)

    Text(Localization.get(language, "device_sync"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text(Localization.get(language, "relay_server")) },
        )
        Button(onClick = { syncManager.connect(serverUrl) }) { Text(Localization.get(language, "connect")) }
    }

    Text(Localization.get(language, "lan_sync"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
    Button(
        onClick = { if (lanRunning) syncManager.stopLan() else syncManager.startLan() },
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Text(Localization.get(language, if (lanRunning) "stop_lan" else "start_lan"))
    }
    if (lanRunning && lanAddress.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                Modifier.widthIn(max = 200.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                QrCode(lanAddress, size = 180.dp)
                Text(
                    "${Localization.get(language, "lan_address")}: $lanAddress",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    Localization.get(language, "scan_qr"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            // Pairing code + generate button sit to the right of the QR code.
            PairingCodePanel(
                language = language,
                pairCode = pairCode,
                remainingMs = remainingMs,
                onGenerate = { syncManager.requestPairingCode() },
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            Localization.get(language, "lan_hint"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    } else {
        // When the LAN server is off, the code can still be generated against
        // the relay configured in the field above.
        PairingCodePanel(
            language = language,
            pairCode = pairCode,
            remainingMs = remainingMs,
            onGenerate = { syncManager.requestPairingCode() },
            modifier = Modifier.padding(top = 8.dp),
        )
    }

    if (paired) {
        Button(onClick = { syncManager.unpair() }, modifier = Modifier.padding(top = 8.dp)) {
            Text(Localization.get(language, "unpair"))
        }
    }

    Text("${Localization.get(language, "status")}: $connectionState — $status", modifier = Modifier.padding(top = 8.dp))

    if (syncedSettings.isNotEmpty()) {
        Text(Localization.get(language, "synced_settings"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Column(Modifier.padding(top = 4.dp)) {
            syncedSettings.entries.sortedBy { it.key }.forEach { (k, v) ->
                Text("$k = $v", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}

@Composable
private fun PairingCodePanel(
    language: String,
    pairCode: String,
    remainingMs: Long,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        // The desktop is the code generator: the phone only enters this code.
        Button(onClick = onGenerate) {
            Text(Localization.get(language, if (pairCode.isNotEmpty()) "generate_new_code" else "generate_code"))
        }
        if (pairCode.isNotEmpty()) {
            Text(
                "${Localization.get(language, "code_hint")}: $pairCode",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                if (remainingMs > 0L) {
                    "${Localization.get(language, "code_expires_in")} ${formatCountdown(remainingMs)}"
                } else {
                    Localization.get(language, "code_expired")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun AccountSection(
    language: String,
    isLoggedIn: Boolean,
    accountName: String,
    onOpenLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    Text(Localization.get(language, "account"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
    if (isLoggedIn) {
        Text(
            "${Localization.get(language, "logged_in_as")}: ${accountName.ifBlank { "YouTube" }}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onLogout, modifier = Modifier.padding(top = 8.dp)) {
            Text(Localization.get(language, "logout"))
        }
    } else {
        Text(
            Localization.get(language, "not_logged_in"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onOpenLogin, modifier = Modifier.padding(top = 8.dp)) {
            Text(Localization.get(language, "login"))
        }
    }
}

@Composable
fun LanguageSection(language: String, onLanguageChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Text(Localization.get(language, "language"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

    Box(Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = { expanded = true }) {
            Text(Languages.name(language))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Languages.all.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.name) },
                    onClick = {
                        expanded = false
                        onLanguageChange(lang.code)
                    },
                )
            }
        }
    }
}

@Composable
fun LanguageSelectionScreen(onSelect: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Choose your language", style = MaterialTheme.typography.headlineMedium)
        Text(
            "VIVI Music DE is available in the following languages. You can change this later from the Language menu.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Column(Modifier.padding(top = 16.dp)) {
            Languages.all.forEach { lang ->
                Text(
                    lang.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(lang.code) }
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * Non-invasive banner shown when a newer desktop release is available, with
 * "Install now" (download + launch the installer) and a dismiss button.
 */
@Composable
fun BoxScope.UpdateNotification(
    status: UpdateStatus.Available,
    language: String,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<DownloadProgress?>(null) }
    // If the installer for this exact version is already downloaded, offer to
    // open it directly instead of downloading it again.
    val existingInstaller = remember(status) {
        status.asset?.let { UpdateDownloader.downloadedInstaller(it.fileName) }
    }

    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.widthIn(max = 300.dp)) {
                Text(
                    Localization.get(language, "update_available"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    status.version,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                progress?.let { p ->
                    LinearProgressIndicator(
                        progress = { p.percent / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
            Button(
                onClick = {
                    if (downloading) return@Button
                    existingInstaller?.let { file ->
                        if (openFile(file)) exitProcess(0)
                        onDone()
                        return@Button
                    }
                    val asset = status.asset
                    if (asset == null) {
                        openUrl(status.url)
                        onDone()
                    } else {
                        scope.launch {
                            downloading = true
                            progress = DownloadProgress(0, asset.sizeBytes, 0)
                            val file = withContext(Dispatchers.IO) {
                                runCatching {
                                    UpdateDownloader.download(asset.downloadUrl, asset.fileName) { p -> progress = p }
                                }.getOrNull()
                            }
                            progress = null
                            downloading = false
                            if (file != null && openFile(file)) {
                                exitProcess(0)
                            }
                            onDone()
                        }
                    }
                },
                enabled = !downloading,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Text(
                    Localization.get(
                        language,
                        when {
                            existingInstaller != null -> "open_installer"
                            downloading -> "downloading"
                            else -> "install_now"
                        },
                    )
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = Localization.get(language, "dismiss"))
            }
        }
    }
}

@Composable
fun UpdateSection(
    language: String,
    status: UpdateStatus,
    includePreReleases: Boolean,
    updateIntervalHours: Int,
    onIntervalChange: (Int) -> Unit,
    onTogglePreReleases: (Boolean) -> Unit,
    onCheckUpdates: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf<DownloadProgress?>(null) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var installerCount by remember { mutableStateOf(UpdateDownloader.downloadedInstallers().size) }
    var openError by remember { mutableStateOf<String?>(null) }
    var intervalMenuOpen by remember { mutableStateOf(false) }

    Text(Localization.get(language, "updates"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
    Text(
        "${Localization.get(language, "current_version")}: ${AppInfo.FULL_VERSION} (${Localization.get(language, "de")} ${AppInfo.DE_VERSION})",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 4.dp),
    )

    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onCheckUpdates, enabled = status != UpdateStatus.Checking) {
            Text(Localization.get(language, "check_updates"))
        }
        Switch(checked = includePreReleases, onCheckedChange = onTogglePreReleases)
        Text(Localization.get(language, "include_prereleases"))
    }

    // Automatic check frequency.
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(Localization.get(language, "update_check_interval"), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { intervalMenuOpen = true }) {
                Text(intervalLabel(language, updateIntervalHours))
            }
            DropdownMenu(expanded = intervalMenuOpen, onDismissRequest = { intervalMenuOpen = false }) {
                updateCheckIntervalOptions().forEach { hours ->
                    DropdownMenuItem(
                        text = { Text(intervalLabel(language, hours)) },
                        onClick = {
                            onIntervalChange(hours)
                            intervalMenuOpen = false
                        },
                    )
                }
            }
        }
    }

    when (status) {
        is UpdateStatus.Checking -> Text(Localization.get(language, "checking"), modifier = Modifier.padding(top = 8.dp))
        is UpdateStatus.UpToDate -> Text(Localization.get(language, "up_to_date"), modifier = Modifier.padding(top = 8.dp))
        is UpdateStatus.Available -> {
            Text("${Localization.get(language, "update_available")}: ${status.version}", modifier = Modifier.padding(top = 8.dp))
            val asset = status.asset
            when {
                asset == null -> {
                    Text(
                        Localization.get(language, "no_installer"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Button(
                        onClick = {
                            openError = null
                            if (!openUrl(status.url)) openError = Localization.get(language, "open_failed")
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(Localization.get(language, "open_release_page"))
                    }
                }
                downloadedFile != null -> {
                    Text(
                        "${Localization.get(language, "downloaded")}: ${downloadedFile!!.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Button(
                        onClick = {
                            openError = null
                            if (openFile(downloadedFile!!)) {
                                // The app must close so the installer can replace
                                // the running files (updates cannot install otherwise).
                                exitProcess(0)
                            } else {
                                openError = Localization.get(language, "open_failed")
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(Localization.get(language, "open_installer"))
                    }
                }
                progress != null -> {
                    val p = progress!!
                    Text(
                        "${Localization.get(language, "downloading")}: ${p.percent}% · ${formatBytes(p.downloadedBytes)} / ${formatBytes(p.totalBytes)} · ${formatSpeed(p.speedBytesPerSecond)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    LinearProgressIndicator(
                        progress = { p.percent / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                else -> Button(
                    onClick = {
                        scope.launch {
                            progress = DownloadProgress(0, asset.sizeBytes, 0)
                            val file = withContext(Dispatchers.IO) {
                                runCatching { UpdateDownloader.download(asset.downloadUrl, asset.fileName) { p -> progress = p } }.getOrNull()
                            }
                            progress = null
                            if (file != null) {
                                downloadedFile = file
                                installerCount = UpdateDownloader.downloadedInstallers().size
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text("${Localization.get(language, "download")} (${formatBytes(asset.sizeBytes)})")
                }
            }
        }
        is UpdateStatus.Failed -> Text(
            "${Localization.get(language, "update_failed")}: ${status.message}",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp),
        )
        is UpdateStatus.Idle -> Unit
    }

    openError?.let {
        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
    }

    // Downloaded installer management.
    Text(
        "${Localization.get(language, "installers_downloaded")}: $installerCount",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp),
    )
    if (installerCount > 0) {
        Button(
            onClick = {
                UpdateDownloader.deleteAll()
                installerCount = 0
                downloadedFile = null
            },
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(Localization.get(language, "delete_installers"))
        }
    }
}

private fun openUrl(url: String): Boolean {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        if (runCatching { Desktop.getDesktop().browse(URI(url)) }.isSuccess) return true
    }
    return runCatching {
        val cmd = when (Platform.os) {
            DesktopOs.WINDOWS -> listOf("cmd", "/c", "start", "", url)
            DesktopOs.MACOS -> listOf("open", url)
            DesktopOs.LINUX -> listOf("xdg-open", url)
        }
        ProcessBuilder(cmd).start()
        true
    }.getOrDefault(false)
}

private fun openFile(file: File): Boolean {
    if (!file.exists()) return false
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
        if (runCatching { Desktop.getDesktop().open(file) }.isSuccess) return true
    }
    return runCatching {
        val path = file.absolutePath
        val cmd = when (Platform.os) {
            DesktopOs.WINDOWS -> listOf("cmd", "/c", "start", "", path)
            DesktopOs.MACOS -> listOf("open", path)
            DesktopOs.LINUX -> listOf("xdg-open", path)
        }
        ProcessBuilder(cmd).start()
        true
    }.getOrDefault(false)
}

private fun formatSpeed(bps: Long): String =
    if (bps <= 0) "0 B/s" else "${formatBytes(bps)}/s"

/** Available update-check intervals, in hours (0 = manual only). */
private fun updateCheckIntervalOptions(): List<Int> = listOf(0, 6, 12, 24, 72, 168)

/** Localized label for an update-check interval. */
private fun intervalLabel(language: String, hours: Int): String = when (hours) {
    0 -> Localization.get(language, "interval_manual")
    6 -> Localization.get(language, "interval_6h")
    12 -> Localization.get(language, "interval_12h")
    24 -> Localization.get(language, "interval_24h")
    72 -> Localization.get(language, "interval_3d")
    168 -> Localization.get(language, "interval_7d")
    else -> "$hours h"
}

/** Formats a millisecond duration as `M:SS` for the pairing-code countdown. */
private fun formatCountdown(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

@Composable
fun AboutSection(language: String, onOpenChangelog: () -> Unit) {
    val firstLaunchDate = remember { DesktopSettings.load().firstLaunchDate }
    var versionCodeTaps by remember { mutableStateOf(0) }
    val devEnabled by DeveloperOptions.enabled.collectAsState()

    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "VIVI MUSIC DE",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ) {
            Text(
                "v${AppInfo.FULL_VERSION} • ${AppInfo.CHANNEL.uppercase()}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    Button(
        onClick = onOpenChangelog,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Text(Localization.get(language, "changelog"))
    }

    AboutSectionHeader(Localization.get(language, "developer_section"))
    AboutInfoRow(
        icon = Icons.Filled.Code,
        title = "PiBOH",
        description = Localization.get(language, "app_developer") + " (DE)",
        onClick = { openUrl("https://github.com/PiBOH") },
    )
    AboutInfoRow(
        icon = Icons.Filled.Public,
        title = Localization.get(language, "website"),
        onClick = { openUrl("https://piboh.github.io/") },
    )

    AboutSectionHeader(Localization.get(language, "community_section"))
    AboutInfoRow(
        icon = Icons.Filled.Code,
        title = Localization.get(language, "github_repository"),
        onClick = { openUrl("https://github.com/PiBOH/vivi-music") },
    )
    AboutInfoRow(
        icon = Icons.Filled.Send,
        title = Localization.get(language, "telegram_channel"),
        onClick = { openUrl("https://t.me/vivimusicapp") },
    )

    AboutSectionHeader(Localization.get(language, "app_info_section"))
    AboutInfoRow(
        icon = Icons.Filled.DateRange,
        title = Localization.get(language, "installed_date_title"),
        description = formatInstalledDate(firstLaunchDate, language),
    )
    AboutInfoRow(
        icon = Icons.Filled.Info,
        title = Localization.get(language, "version_code"),
        description = AppInfo.VERSION_CODE.toString(),
        onClick = {
            if (!devEnabled) {
                versionCodeTaps++
                if (versionCodeTaps >= 7) {
                    DeveloperOptions.setEnabled(true)
                    versionCodeTaps = 0
                }
            }
        },
    )
    Text(
        if (devEnabled) {
            Localization.get(language, "developer_options_enabled")
        } else if (versionCodeTaps > 0) {
            Localization.get(language, "tap_version_code_hint") + " (${7 - versionCodeTaps})"
        } else {
            ""
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
    AboutInfoRow(
        icon = Icons.Filled.Description,
        title = Localization.get(language, "license"),
        onClick = { openUrl("https://github.com/PiBOH/vivi-music/blob/main/LICENSE") },
    )
}

@Composable
private fun AboutSectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun AboutInfoRow(
    icon: ImageVector,
    title: String,
    description: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val base = Modifier.fillMaxWidth()
    val rowModifier = if (onClick != null) base.clickable(onClick = onClick) else base
    Row(
        rowModifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (description != null) {
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatInstalledDate(epochMs: Long, language: String): String {
    if (epochMs <= 0) return Localization.get(language, "unknown")
    return try {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(epochMs))
    } catch (_: Exception) {
        "—"
    }
}

@Composable
fun PlayerSection(
    language: String,
    autoPlayNext: Boolean,
    onToggleAutoPlayNext: (Boolean) -> Unit,
    audioQuality: String,
    onAudioQualityChange: (String) -> Unit,
    rememberShuffleRepeat: Boolean,
    onToggleRememberShuffleRepeat: (Boolean) -> Unit,
    persistentQueue: Boolean,
    onTogglePersistentQueue: (Boolean) -> Unit,
) {
    var qualityExpanded by remember { mutableStateOf(false) }

    Text(Localization.get(language, "player_audio"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

    Text(Localization.get(language, "audio_quality"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    Box(Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = { qualityExpanded = true }) {
            Text(audioQualityLabel(language, audioQuality))
        }
        DropdownMenu(expanded = qualityExpanded, onDismissRequest = { qualityExpanded = false }) {
            listOf("auto", "high", "low").forEach { q ->
                DropdownMenuItem(
                    text = { Text(audioQualityLabel(language, q)) },
                    onClick = { qualityExpanded = false; onAudioQualityChange(q) },
                )
            }
        }
    }

    SettingSwitch(language, "autoplay_next", autoPlayNext, onToggleAutoPlayNext)
    SettingSwitch(language, "remember_shuffle_repeat", rememberShuffleRepeat, onToggleRememberShuffleRepeat)
    SettingSwitch(language, "persistent_queue", persistentQueue, onTogglePersistentQueue)
}

@Composable
private fun SettingSwitch(language: String, key: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        Text(Localization.get(language, key))
    }
}

private fun audioQualityLabel(language: String, quality: String): String = when (quality) {
    "high" -> Localization.get(language, "audio_quality_high")
    "low" -> Localization.get(language, "audio_quality_low")
    else -> Localization.get(language, "audio_quality_auto")
}

private fun dirSize(dir: File): Long =
    dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    return "%.1f MB".format(kb / 1024.0)
}

@Composable
fun StorageSection(language: String) {
    val scope = rememberCoroutineScope()
    val cacheDir = remember { File(System.getProperty("user.home"), ".vivimusic/cache") }
    var sizeText by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            sizeText = withContext(Dispatchers.IO) { formatBytes(dirSize(cacheDir)) }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Text(Localization.get(language, "storage"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
    Text(
        "${Localization.get(language, "cache_size")}: ${sizeText ?: "…"}",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
    Button(
        onClick = {
            scope.launch {
                withContext(Dispatchers.IO) { cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
                sizeText = withContext(Dispatchers.IO) { formatBytes(dirSize(cacheDir)) }
            }
        },
        modifier = Modifier.padding(top = 8.dp),
    ) { Text(Localization.get(language, "clear_cache")) }
}

/** JSON codec for persisting the queue between sessions. */
private val queueJson = Json { ignoreUnknownKeys = true }

/** Key used to detect discrete playback changes worth syncing (no per-frame pushes). */
private data class PlaybackSyncKey(
    val trackId: String?,
    val isPlaying: Boolean,
    val index: Int,
    val queue: List<String>,
)

/** Maps the desktop theme/language/accent onto the Android shared-preference keys. */
private fun desktopSettingsMap(language: String, themeMode: ThemeMode, accent: Color): Map<String, String> = mapOf(
    "appLanguage" to Languages.toMobileCode(language).ifBlank { "SYSTEM_DEFAULT" },
    "darkMode" to when (themeMode) {
        ThemeMode.SYSTEM -> "AUTO"
        ThemeMode.LIGHT -> "OFF"
        ThemeMode.DARK -> "ON"
    },
    "selectedThemeColor" to colorToArgbInt(accent).toString(),
    "pureBlack" to "false",
    "dynamicTheme" to "false",
)
