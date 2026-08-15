package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun main() = application {
    // Configure the shared YouTube client exactly like the Android App.onCreate() does,
    // honouring the saved content language/region (or the OS default).
    val initialSettings = DesktopSettings.load()
    YouTube.locale = resolveYouTubeLocale(initialSettings.contentLanguage, initialSettings.contentCountry)
    YouTubeExtractor.cacheDir = File(System.getProperty("user.home"), ".vivimusic/cache").apply { mkdirs() }
    LoginManager.restore()

    var language by remember { mutableStateOf(DesktopSettings.load().language) }
    var themeMode by remember { mutableStateOf(ThemeMode.from(DesktopSettings.load().darkMode)) }
    var accent by remember { mutableStateOf(argbIntToColor(DesktopSettings.load().accentColor)) }

    fun saveTheme() {
        DesktopSettings.save(
            DesktopSettings.load().copy(darkMode = themeMode.key, accentColor = colorToArgbInt(accent))
        )
    }

    Window(onCloseRequest = ::exitApplication, title = "VIVI Music — desktop") {
        AppTheme(mode = themeMode, accent = accent) {
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
) {
    var backStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
    val player = remember { PlayerController() }
    val playerState by player.state.collectAsState()
    val nowPlaying = playerState.current
    val isPlaying = playerState.isPlaying

    var autoPlayNext by remember { mutableStateOf(DesktopSettings.load().autoPlayNext) }
    player.autoPlayNext = autoPlayNext

    var isLoggedIn by remember { mutableStateOf(LoginManager.isLoggedIn()) }
    var accountName by remember { mutableStateOf(DesktopSettings.load().accountName) }
    var sidebarCollapsed by remember { mutableStateOf(DesktopSettings.load().sidebarCollapsed) }
    var contentLanguage by remember { mutableStateOf(DesktopSettings.load().contentLanguage) }
    var contentCountry by remember { mutableStateOf(DesktopSettings.load().contentCountry) }
    var syncedLyrics by remember { mutableStateOf(DesktopSettings.load().syncedLyrics) }

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

    val scope = rememberCoroutineScope()
    var includePreReleases by remember { mutableStateOf(DesktopSettings.load().includePreReleases) }
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }

    fun runUpdateCheck() {
        updateStatus = UpdateStatus.Checking
        scope.launch {
            val result = withContext(Dispatchers.IO) { UpdateChecker.check(includePreReleases) }
            updateStatus = result
        }
    }

    // Automatic update check on startup.
    LaunchedEffect(Unit) { runUpdateCheck() }

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
                    )
                    is Screen.SettingsPlayer -> SettingsPlayerScreen(
                        language = language,
                        onBack = goBack,
                        autoPlayNext = autoPlayNext,
                        onToggleAutoPlayNext = { checked ->
                            autoPlayNext = checked
                            DesktopSettings.save(DesktopSettings.load().copy(autoPlayNext = checked))
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
                    is Screen.Album -> AlbumScreen(
                        browseId = current.browseId,
                        language = language,
                        onBack = goBack,
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onPlayAll = playAll,
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
        Text(
            "${Localization.get(language, "lan_address")}: $lanAddress",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            Localization.get(language, "scan_qr"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        QrCode(lanAddress, size = 180.dp)
        Text(
            Localization.get(language, "lan_hint"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }

    // The desktop is the code generator: the phone only enters this code.
    Button(
        onClick = { syncManager.requestPairingCode() },
        modifier = Modifier.padding(top = 8.dp),
    ) {
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

@Composable
fun UpdateSection(
    language: String,
    status: UpdateStatus,
    includePreReleases: Boolean,
    onTogglePreReleases: (Boolean) -> Unit,
    onCheckUpdates: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf<DownloadProgress?>(null) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var installerCount by remember { mutableStateOf(UpdateDownloader.downloadedInstallers().size) }
    var openError by remember { mutableStateOf<String?>(null) }

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

    when (status) {
        is UpdateStatus.Checking -> Text(Localization.get(language, "checking"), modifier = Modifier.padding(top = 8.dp))
        is UpdateStatus.UpToDate -> Text(Localization.get(language, "up_to_date"), modifier = Modifier.padding(top = 8.dp))
        is UpdateStatus.Available -> {
            Text("${Localization.get(language, "update_available")}: ${status.version}", modifier = Modifier.padding(top = 8.dp))
            val asset = status.asset
            when {
                asset == null -> Button(
                    onClick = {
                        openError = null
                        if (!openUrl(status.url)) openError = Localization.get(language, "open_failed")
                    },
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(Localization.get(language, "download"))
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

/** Formats a millisecond duration as `M:SS` for the pairing-code countdown. */
private fun formatCountdown(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

@Composable
fun AboutSection(language: String, onOpenChangelog: () -> Unit) {
    Text(Localization.get(language, "about"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
    Text(
        "${AppInfo.FULL_VERSION} ${AppInfo.CHANNEL.uppercase()}",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp),
    )
    Text(
        "${Localization.get(language, "mobile")} ${AppInfo.MOBILE_VERSION} · ${Localization.get(language, "de")} ${AppInfo.DE_VERSION}",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 2.dp),
    )
    Button(onClick = onOpenChangelog, modifier = Modifier.padding(top = 8.dp)) {
        Text(Localization.get(language, "changelog"))
    }
}

@Composable
fun PlayerSection(language: String, autoPlayNext: Boolean, onToggleAutoPlayNext: (Boolean) -> Unit) {
    Text(Localization.get(language, "player_audio"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(checked = autoPlayNext, onCheckedChange = onToggleAutoPlayNext)
        Text(Localization.get(language, "autoplay_next"))
    }
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
