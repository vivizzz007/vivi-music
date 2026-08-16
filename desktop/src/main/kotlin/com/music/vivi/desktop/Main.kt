package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Notifications
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
import java.net.URLEncoder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.music.innertube.YouTube
import kotlin.math.abs
import kotlin.system.exitProcess
import com.music.innertube.YouTubeExtractor
import com.music.innertube.models.SongItem
import com.music.vivi.desktop.player.PlayerController
import com.music.vivi.sync.LibrarySnapshot
import com.music.vivi.sync.PlaybackSnapshot
import com.music.vivi.sync.SyncServer
import com.music.vivi.sync.SyncedSong
import com.music.vivi.sync.TrackRef
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main() {
    // Single-instance guard: if another instance is already running (or already
    // starting), exit immediately and keep the first one that started.
    if (!SingleInstance.acquire()) return

    application {
    // Configure the shared YouTube client exactly like the Android App.onCreate() does,
    // honouring the saved content language/region (or the OS default).
    val initialSettings = DesktopSettings.load()
    YouTube.locale = resolveYouTubeLocale(initialSettings.contentLanguage, initialSettings.contentCountry)
    YouTubeExtractor.cacheDir = File(System.getProperty("user.home"), ".vivimusic/cache").apply { mkdirs() }
    LoginManager.restore()
    DesktopSettings.ensureFirstLaunchDate()
    // Dev tools are non-critical: never let their initialization crash the app
    // at startup (which the jpackage launcher reports as "Failed to launch JVM").
    runCatching { DeveloperOptions.load() }

    var language by remember { mutableStateOf(DesktopSettings.load().language) }
    var themeMode by remember { mutableStateOf(ThemeMode.from(DesktopSettings.load().darkMode)) }
    var accent by remember { mutableStateOf(argbIntToColor(DesktopSettings.load().accentColor)) }
    var pureBlack by remember { mutableStateOf(DesktopSettings.load().pureBlack) }

    fun saveTheme() {
        DesktopSettings.update {
            it.copy(
                darkMode = themeMode.key,
                accentColor = colorToArgbInt(accent),
                pureBlack = pureBlack,
            )
        }
    }

    // Live window title: shows CPU/RAM when dev options are on and either the
    // "show in title bar" toggle is set or the display mode is "title bar only".
    val devTitleEnabled by DeveloperOptions.enabled.collectAsState()
    val devTitleVisible by DeveloperOptions.showInTitleBar.collectAsState()
    val devMode by DeveloperOptions.mode.collectAsState()
    val titleStats by SystemMonitor.stats.collectAsState()
    val windowTitle = buildString {
        append("VIVI Music — desktop")
        if (devTitleEnabled && (devTitleVisible || devMode == DevToolsMode.TITLE_BAR)) {
            append(titleStats.titleBarText())
        }
    }

    Window(onCloseRequest = ::exitApplication, title = windowTitle) {
        AppTheme(mode = themeMode, accent = accent, pureBlack = pureBlack) {
            // NOTE: do NOT wrap this in a global SelectionContainer. Popup-based
            // components (DropdownMenu, AlertDialog) inherit the selection
            // registrar and crash with "layouts are not part of the same
            // hierarchy" on pointer events (see Compose CMP-2326). Use targeted
            // SelectionContainer wrappers on individual text instead.
            if (language.isBlank()) {
                LanguageSelectionScreen { selected ->
                    language = selected
                    DesktopSettings.update { it.copy(language = selected) }
                }
            } else {
                App(
                    language = language,
                    onLanguageChange = { selected ->
                        language = selected
                        DesktopSettings.update { it.copy(language = selected) }
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

    // Scheduled automatic backups (weekly): check on startup, then hourly.
    LaunchedEffect(Unit) {
        while (true) {
            runCatching { BackupManager.maybeRunScheduled() }
            delay(3600_000L)
        }
    }

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
            DesktopSettings.update {
                it.copy(queueJson = queueJson.encodeToString(playerState.queue), queueIndex = playerState.index)
            }
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
    var addToPlaylistSong by remember { mutableStateOf<SyncedSong?>(null) }
    val addToPlaylist: (SongItem) -> Unit = { song -> addToPlaylistSong = song.toSyncedSong() }
    // Same, but for the Player / Queue (which carry NowPlaying, not SongItem).
    val addNowPlayingToPlaylist: (NowPlaying) -> Unit = { np ->
        addToPlaylistSong = SyncedSong(id = np.videoId, title = np.title, artist = np.artist, thumbnail = np.thumbnail)
    }
    val playAll: (List<SongItem>) -> Unit = { songs -> player.playAll(songs.map(::songToNowPlaying)) }
    val shuffleAll: (List<SongItem>) -> Unit = { songs ->
        if (!playerState.isShuffle) player.toggleShuffle()
        player.playAll(songs.shuffled().map(::songToNowPlaying))
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        DesktopSnackbar.events.collectLatest { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }
    // Generic in-app notification banner (title + message) for notifications
    // dispatched in "main window" mode.
    var appNotification by remember { mutableStateOf<DesktopNotifier.Notice?>(null) }
    LaunchedEffect(Unit) {
        DesktopNotifier.events.collect { appNotification = it }
    }
    // Auto-dismiss the generic in-app notification after the configured time.
    LaunchedEffect(appNotification) {
        val notice = appNotification ?: return@LaunchedEffect
        val seconds = DesktopSettings.load().inAppNotificationDurationSeconds
        if (seconds > 0) {
            delay(seconds * 1000L)
            appNotification = null
        }
    }
    // Pre-releases are on by default for nightly/alpha/beta/rc builds (those
    // channels only publish pre-releases), and off by default for stable.
    var includePreReleases by remember {
        mutableStateOf(
            DesktopSettings.load().includePreReleases || AppInfo.CHANNEL.lowercase() != "stable"
        )
    }
    var updateIntervalHours by remember { mutableStateOf(DesktopSettings.load().updateCheckIntervalHours) }
    var notificationMode by remember { mutableStateOf(DesktopSettings.load().notificationMode) }
    var notificationDurationSeconds by remember { mutableStateOf(DesktopSettings.load().inAppNotificationDurationSeconds) }
    var saveNotificationHistory by remember { mutableStateOf(DesktopSettings.load().saveNotificationHistory) }
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    // Keep the shared download state (used by both the notification and the
    // Updates screen) in sync with the latest update status.
    LaunchedEffect(updateStatus) { UpdateState.syncWithStatus(updateStatus) }
    val devEnabled by DeveloperOptions.enabled.collectAsState()
    val devMode by DeveloperOptions.mode.collectAsState()
    val overlayMovable by DeveloperOptions.overlayMovable.collectAsState()

    // One-off hint when the developer options get unlocked. Respects the
    // notification mode (in-app banner vs native system notification).
    var showDevNotification by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        DeveloperOptions.unlocked.collect {
            val title = Localization.get(language, "dev_unlocked_title")
            val desc = Localization.get(language, "dev_unlocked_desc")
            if (DesktopSettings.load().notificationMode == "native") {
                DesktopNotifier.notify(title, desc)
            } else {
                NotificationHistory.record(title, desc, "in_app")
                showDevNotification = true
            }
        }
    }

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

    // Update notification, shown once per new version. Where it appears
    // depends on the user's notification mode (in-app vs native system).
    var showUpdateNotification by remember { mutableStateOf(false) }
    var updateNotifiedVersion by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(updateStatus) {
        val available = updateStatus as? UpdateStatus.Available
        if (available != null && available.version != updateNotifiedVersion) {
            updateNotifiedVersion = available.version
            val mode = if (DesktopSettings.load().notificationMode == "native") "native" else "in_app"
            val title = Localization.get(language, "update_available")
            val message = "${Localization.get(language, "current_version")}: ${AppInfo.FULL_VERSION}\n${available.version}"
            NotificationHistory.record(title, message, mode)
            if (mode == "native") {
                NativeNotifier.notify(title, message)
            } else {
                showUpdateNotification = true
            }
        }
    }

    // ---- Device sync (Android <-> desktop) ----
    val syncManager = remember { DesktopSyncManager() }

    // Echo guards: when we apply a remote volume, we must not push the
    // resulting local change straight back to the peer.
    val systemVolumeGuard = remember { VolumeGuard() }
    val volumeGuard = remember { VolumeGuard() }

    // Notify when a phone pairs or un-pairs (respects the notification mode).
    var wasPaired by remember { mutableStateOf(false) }
    LaunchedEffect(syncManager) {
        syncManager.paired.collect { paired ->
            // Keep the display/system awake while paired so the OS sleeping
            // the screen can't tear down the sync socket and unpair the two
            // devices.
            KeepAwake.setEnabled(paired)
            if (paired != wasPaired) {
                wasPaired = paired
                if (paired) {
                    val name = syncManager.peerDeviceName.value.ifBlank { null }
                    DesktopNotifier.notify(
                        Localization.get(language, "device_paired_title"),
                        name ?: Localization.get(language, "device_paired_desc"),
                    )
                } else {
                    DesktopNotifier.notify(
                        Localization.get(language, "device_unpaired_title"),
                        Localization.get(language, "device_unpaired_desc"),
                    )
                }
            }
        }
    }

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
            .collect { player.toPlaybackSnapshot()?.let { syncManager.updatePlayback(it) } }
    }

    // Push the in-app (VIVI) player volume to the peer. Polled (not event-
    // driven) so it also syncs when nothing is playing, and echo-guarded so a
    // locally-applied remote value isn't bounced straight back.
    LaunchedEffect(syncManager) {
        while (true) {
            val v = player.state.value.volume
            val isEcho = System.currentTimeMillis() < volumeGuard.echoUntil &&
                abs(v - volumeGuard.echoValue) < 0.01f
            val changed = volumeGuard.lastPushed == null ||
                abs(v - volumeGuard.lastPushed!!) > 0.001f
            if (!isEcho && changed) {
                val s = player.state.value
                val snapshot = player.toPlaybackSnapshot() ?: PlaybackSnapshot(volume = s.volume)
                // Only mark as pushed when it was actually sent, so a dropped
                // push (echo-suppression window) is retried on the next tick.
                if (syncManager.updatePlayback(snapshot)) {
                    volumeGuard.lastPushed = v
                }
            }
            delay(500L)
        }
    }

    // Push immediately on user seeks so the peer follows to the same position.
    LaunchedEffect(syncManager) {
        player.seekEvents.collect { player.toPlaybackSnapshot()?.let { syncManager.updatePlayback(it) } }
    }

    // Periodic re-sync: while playing, re-push the position every few seconds so
    // the peer auto-corrects drift (buffering / clock skew) instead of waiting
    // for the next discrete seek/play/track event.
    LaunchedEffect(syncManager) {
        while (true) {
            delay(SyncServer.RESYNC_TICK_MS)
            if (player.state.value.isPlaying) {
                player.toPlaybackSnapshot()?.let { syncManager.updatePlayback(it) }
            }
        }
    }

    // Poll the OS system volume and push changes to the peer (so changing the
    // Windows/Linux/mac volume controls the phone's system volume, and vice
    // versa). Echo-suppressed so a locally-applied remote value isn't bounced.
    LaunchedEffect(syncManager) {
        while (true) {
            val sv = SystemVolume.get()
            if (sv != null) {
                val isEcho = System.currentTimeMillis() < systemVolumeGuard.echoUntil &&
                    abs(sv - systemVolumeGuard.echoValue) < 0.02f
                val changed = systemVolumeGuard.lastPushed == null ||
                    abs(sv - systemVolumeGuard.lastPushed!!) > 0.01f
                if (!isEcho && changed) {
                    val s = player.state.value
                    val snapshot = player.toPlaybackSnapshot() ?: PlaybackSnapshot(volume = s.volume)
                    if (syncManager.updatePlayback(snapshot.copy(systemVolume = sv))) {
                        systemVolumeGuard.lastPushed = sv
                    }
                }
            }
            delay(800L)
        }
    }

    // Apply incoming playback snapshots from the peer.
    LaunchedEffect(syncManager) {
        syncManager.incomingPlayback.collect { pb ->
            // App (player) volume sync: mirror the peer's in-app volume slider.
            pb.volume?.let { v ->
                volumeGuard.echoUntil = System.currentTimeMillis() + 1500L
                volumeGuard.echoValue = v
                volumeGuard.lastPushed = v
                if (abs(v - player.state.value.volume) > 0.001f) player.setVolume(v)
            }
            // Native OS system volume sync: mirror the peer's system volume.
            pb.systemVolume?.let { v ->
                systemVolumeGuard.echoUntil = System.currentTimeMillis() + 1500L
                systemVolumeGuard.echoValue = v
                systemVolumeGuard.lastPushed = v
                SystemVolume.set(v)
            }
            val currentId = player.state.value.current?.videoId
            if (currentId != null && pb.trackId != null && pb.trackId == currentId) {
                // Same track: lightweight seek (instant + precise), no restart.
                // Periodic ticks re-send the position; skip the seek when the
                // drift is within tolerance so it doesn't glitch the audio.
                player.seekRemote(
                    syncManager.effectivePosition(pb),
                    pb.isPlaying,
                    SyncServer.RESYNC_TOLERANCE_MS,
                )
            } else {
                // Last-write-wins for the queue: only replace the local queue if
                // the remote edit is newer (or unknown, from an older peer).
                // Volume/position sync above still runs regardless.
                val newerQueue = pb.queueUpdatedAt <= 0L || pb.queueUpdatedAt >= syncManager.queueUpdatedAt()
                if (newerQueue) {
                    val tracks = pb.queue.map { ref ->
                        NowPlaying(videoId = ref.id, title = ref.title, artist = ref.artist.orEmpty(), thumbnail = ref.thumbnail)
                    }
                    if (tracks.isNotEmpty()) {
                        player.applyRemotePlayback(tracks, pb.queueIndex, syncManager.effectivePosition(pb), pb.isPlaying)
                        syncManager.noteQueueApplied(pb)
                    }
                }
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

    // Playlist sync: push the local playlists whenever they change and apply
    // the peer's list (last-write-wins per playlist id).
    LaunchedEffect(syncManager) {
        PlaylistStore.all.collect {
            syncManager.updateLibrary(LibrarySnapshot(playlists = PlaylistStore.toSynced()))
        }
    }
    LaunchedEffect(syncManager) {
        syncManager.incomingLibrary.collect { lib ->
            lib?.playlists?.let { PlaylistStore.applyRemote(it) }
        }
    }

    Row(Modifier.fillMaxSize()) {
        Sidebar(
            language = language,
            current = current,
            collapsed = sidebarCollapsed,
            onToggleCollapsed = {
                sidebarCollapsed = !sidebarCollapsed
                DesktopSettings.update { it.copy(sidebarCollapsed = sidebarCollapsed) }
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
                        onAddToPlaylist = addToPlaylist,
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
                        onAddToPlaylist = addToPlaylist,
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
                        onAddToPlaylist = addToPlaylist,
                        onShuffleAll = shuffleAll,
                    )
                    is Screen.History -> HistoryScreen(
                        language = language,
                        onBack = goBack,
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
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
                            DesktopSettings.update { it.copy(autoPlayNext = checked) }
                        },
                        audioQuality = audioQuality,
                        onAudioQualityChange = { q ->
                            audioQuality = q
                            DesktopSettings.update { it.copy(audioQuality = q) }
                        },
                        rememberShuffleRepeat = rememberShuffleRepeat,
                        onToggleRememberShuffleRepeat = { checked ->
                            rememberShuffleRepeat = checked
                            DesktopSettings.update { it.copy(rememberShuffleRepeat = checked) }
                        },
                        persistentQueue = persistentQueue,
                        onTogglePersistentQueue = { checked ->
                            persistentQueue = checked
                            DesktopSettings.update { it.copy(persistentQueue = checked) }
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
                            DesktopSettings.update { it.copy(contentLanguage = code) }
                            YouTube.locale = resolveYouTubeLocale(contentLanguage, contentCountry)
                        },
                        onContentCountryChange = { code ->
                            contentCountry = code
                            DesktopSettings.update { it.copy(contentCountry = code) }
                            YouTube.locale = resolveYouTubeLocale(contentLanguage, contentCountry)
                        },
                    )
                    is Screen.SettingsLyrics -> SettingsLyricsScreen(
                        language = language,
                        onBack = goBack,
                        syncedLyrics = syncedLyrics,
                        onToggleSyncedLyrics = { checked ->
                            syncedLyrics = checked
                            DesktopSettings.update { it.copy(syncedLyrics = checked) }
                        },
                        lyricsTextSize = lyricsTextSize,
                        onLyricsTextSizeChange = { size ->
                            lyricsTextSize = size
                            DesktopSettings.update { it.copy(lyricsTextSize = size) }
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
                            DesktopSettings.update { it.copy(updateCheckIntervalHours = hours) }
                        },
                        onTogglePreReleases = { checked ->
                            includePreReleases = checked
                            DesktopSettings.update { it.copy(includePreReleases = checked) }
                            runUpdateCheck()
                        },
                        onCheckUpdates = { runUpdateCheck() },
                        onOpenChangelog = { navigate(Screen.Changelog) },
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
                    is Screen.SettingsBackup -> SettingsBackupScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.SettingsNotifications -> SettingsNotificationsScreen(
                        language = language,
                        onBack = goBack,
                        notificationMode = notificationMode,
                        onNotificationModeChange = { mode ->
                            notificationMode = mode
                            DesktopSettings.update { it.copy(notificationMode = mode) }
                        },
                        notificationDurationSeconds = notificationDurationSeconds,
                        onNotificationDurationChange = { secs ->
                            notificationDurationSeconds = secs
                            DesktopSettings.update { it.copy(inAppNotificationDurationSeconds = secs) }
                        },
                        saveHistory = saveNotificationHistory,
                        onSaveHistoryChange = { save ->
                            saveNotificationHistory = save
                            DesktopSettings.update { it.copy(saveNotificationHistory = save) }
                        },
                        onOpenHistory = { navigate(Screen.SettingsNotificationsHistory) },
                    )
                    is Screen.SettingsNotificationsHistory -> NotificationHistoryScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.Album -> AlbumScreen(
                        browseId = current.browseId,
                        language = language,
                        onBack = goBack,
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
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
                        onAddToPlaylist = addToPlaylist,
                    )
                    is Screen.Playlist -> PlaylistScreen(
                        playlistId = current.playlistId,
                        language = language,
                        onBack = goBack,
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                        onPlayAll = playAll,
                        onShuffleAll = shuffleAll,
                    )
                    is Screen.LocalPlaylists -> LocalPlaylistsScreen(
                        language = language,
                        onBack = goBack,
                        onOpenPlaylist = { navigate(Screen.LocalPlaylist(it)) },
                    )
                    is Screen.LocalPlaylist -> LocalPlaylistScreen(
                        playlistId = current.playlistId,
                        language = language,
                        onBack = goBack,
                        onPlay = { s -> player.play(NowPlaying(videoId = s.id, title = s.title, artist = s.artist, thumbnail = s.thumbnail)) },
                        onPlayAll = { songs -> player.playAll(songs.map { NowPlaying(videoId = it.id, title = it.title, artist = it.artist, thumbnail = it.thumbnail) }) },
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
                        loadPhase = playerState.loadPhase,
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
                        onAddToPlaylist = addNowPlayingToPlaylist,
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
                        onAddToPlaylist = addNowPlayingToPlaylist,
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
                if (showDevNotification) {
                    DevUnlockedNotification(
                        language = language,
                        onOpen = {
                            showDevNotification = false
                            navigate(Screen.SettingsDeveloper)
                        },
                        onDismiss = { showDevNotification = false },
                    )
                }
                appNotification?.let { notice ->
                    InAppNotification(
                        title = notice.title,
                        message = notice.message,
                        language = language,
                        onDismiss = { appNotification = null },
                    )
                }
                if (devEnabled && devMode == DevToolsMode.OVERLAY) {
                    DevToolsOverlay(
                        syncManager = syncManager,
                        language = language,
                        movable = overlayMovable,
                    )
                }
                addToPlaylistSong?.let { song ->
                    AddToPlaylistDialog(
                        language = language,
                        song = song,
                        onDismiss = { addToPlaylistSong = null },
                    )
                }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            MiniPlayer(
                nowPlaying = nowPlaying,
                isPlaying = isPlaying,
                isLoading = playerState.isLoading,
                positionMs = playerState.positionMs,
                durationMs = playerState.durationMs,
                onTogglePlay = { player.toggle() },
                onNext = { player.next() },
                // Click toggles the full player: open it, or hide it (go back).
                onOpen = { if (current == Screen.Player) goBack() else navigate(Screen.Player) },
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
        SidebarEntry(Screen.LocalPlaylists, "playlists", Icons.AutoMirrored.Outlined.PlaylistAdd, Icons.AutoMirrored.Filled.PlaylistAdd),
        SidebarEntry(Screen.Queue, "queue", Icons.AutoMirrored.Outlined.QueueMusic, Icons.AutoMirrored.Filled.QueueMusic),
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
    isLoading: Boolean,
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
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
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
            icon = Icons.Filled.Notifications,
            title = Localization.get(language, "notifications"),
            subtitle = Localization.get(
                language,
                if (DesktopSettings.load().notificationMode == "native") "notification_native" else "notification_main_window",
            ),
            onClick = { onOpen(Screen.SettingsNotifications) },
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
            icon = Icons.Filled.SettingsBackupRestore,
            title = Localization.get(language, "backup_restore"),
            onClick = { onOpen(Screen.SettingsBackup) },
        )
        SettingsEntryRow(
            language = language,
            icon = Icons.Filled.Build,
            title = Localization.get(language, "developer_options"),
            subtitle = if (devEnabled) {
                Localization.get(language, "developer_options_enabled")
            } else {
                Localization.get(language, "dev_tools_disabled")
            },
            onClick = { onOpen(Screen.SettingsDeveloper) },
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
    val peerDeviceName by syncManager.peerDeviceName.collectAsState()
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

    // The QR code carries both the LAN relay address and the current 6-digit
    // pairing code (when available), so the phone can auto-fill the code and
    // the user only has to verify it before tapping Pair.
    val qrContent = if (lanAddress.isNotEmpty() && pairCode.isNotEmpty()) {
        "vivimusic://pair?addr=${URLEncoder.encode(lanAddress, "UTF-8")}&code=$pairCode"
    } else {
        lanAddress
    }

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
                QrCode(qrContent, size = 180.dp)
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

    if (paired && peerDeviceName.isNotBlank()) {
        Text(
            "${Localization.get(language, "paired_device")}: $peerDeviceName",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
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
            // Selectable so the user can copy the code if the QR scan fails.
            SelectionContainer {
                Text(
                    "${Localization.get(language, "code_hint")}: $pairCode",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
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
 * Generic non-invasive in-app notification banner (title + message + dismiss),
 * used for notifications dispatched in "main window" mode.
 */
@Composable
fun BoxScope.InAppNotification(
    title: String,
    message: String,
    language: String,
    onDismiss: () -> Unit,
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.widthIn(max = 300.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onDismiss, modifier = Modifier.padding(start = 12.dp)) {
                Text(Localization.get(language, "dismiss"))
            }
        }
    }
}

/**
 * One-off banner shown right after the developer options are unlocked,
 * pointing the user to the settings screen where they can configure them.
 */
@Composable
fun BoxScope.DevUnlockedNotification(
    language: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.widthIn(max = 300.dp)) {
                Text(
                    Localization.get(language, "dev_unlocked_title"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    Localization.get(language, "dev_unlocked_desc"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onOpen, modifier = Modifier.padding(start = 12.dp)) {
                Text(Localization.get(language, "dev_unlocked_open"))
            }
            TextButton(onClick = onDismiss) {
                Text(Localization.get(language, "dismiss"))
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
    val progress by UpdateState.progress.collectAsState()
    val downloadedFile by UpdateState.downloadedFile.collectAsState()
    // Shared with the Updates screen: if the installer for this version is
    // already downloaded (from anywhere), offer to open it, not re-download.
    val existingInstaller = downloadedFile

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
                    if (progress != null) return@Button
                    existingInstaller?.let { file ->
                        scope.launch {
                            prepareAndOpenInstaller(file)
                            onDone()
                        }
                        return@Button
                    }
                    val asset = status.asset
                    if (asset == null) {
                        openUrl(status.url)
                        onDone()
                    } else {
                        scope.launch {
                            val file = UpdateState.download(asset)
                            if (file != null) prepareAndOpenInstaller(file)
                            onDone()
                        }
                    }
                },
                enabled = progress == null,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Text(
                    Localization.get(
                        language,
                        when {
                            existingInstaller != null -> "open_installer"
                            progress != null -> "downloading"
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
    onOpenChangelog: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // Shared download state (also used by the notification banner), so the two
    // surfaces stay in sync.
    val progress by UpdateState.progress.collectAsState()
    val downloadedFile by UpdateState.downloadedFile.collectAsState()
    val installerCount by UpdateState.installerCount.collectAsState()
    var openError by remember { mutableStateOf<String?>(null) }
    var intervalMenuOpen by remember { mutableStateOf(false) }

    Text(Localization.get(language, "updates"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
    Text(
        "${Localization.get(language, "current_version")}: ${AppInfo.FULL_VERSION} (${Localization.get(language, "de")} ${AppInfo.DE_VERSION})",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 4.dp),
    )

    OutlinedButton(onClick = onOpenChangelog, modifier = Modifier.padding(top = 8.dp)) {
        Text(Localization.get(language, "changelog"))
    }

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
                            // Dismiss the popup before mutating the state that
                            // changes this row's layout, or Compose throws
                            // "layouts are not part of the same hierarchy".
                            intervalMenuOpen = false
                            onIntervalChange(hours)
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
                            scope.launch {
                                if (!prepareAndOpenInstaller(downloadedFile!!)) {
                                    openError = Localization.get(language, "open_failed")
                                }
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
                        scope.launch { UpdateState.download(asset) }
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
            onClick = { UpdateState.deleteAllInstallers() },
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

/**
 * Runs the optional "backup before update" (if enabled) and then opens the
 * installer, exiting the app on success so the installer can replace the
 * running files. Returns false when the installer could not be opened. Shared
 * by the update notification and the Updates screen so both behave the same.
 */
private suspend fun prepareAndOpenInstaller(file: File): Boolean {
    val s = DesktopSettings.load()
    if (s.autoBackupEnabled && s.autoBackupBeforeUpdate) {
        withContext(Dispatchers.IO) { BackupManager.autoBackup("before_update") }
    }
    val ok = openFile(file)
    if (ok) exitProcess(0)
    return ok
}

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

/** GitHub mark (octocat) taken from the mobile app's drawable, so it tints
 *  with the accent color and adapts to dark/light mode like a normal icon. */
private val GithubIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Github",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(GITHUB_MARK_PATH).toNodes(),
            fill = SolidColor(Color.White),
        )
    }.build()
}

private const val GITHUB_MARK_PATH =
    "M12,2A10,10 0,0 0,2 12c0,4.42 2.87,8.17 6.84,9.5c0.5,0.08 0.66,-0.23 0.66,-0.5c0,-0.23 0,-0.86 0,-1.69c-2.77,0.6 -3.36,-1.34 -3.36,-1.34c-0.46,-1.16 -1.11,-1.47 -1.11,-1.47c-0.91,-0.62 0.07,-0.6 0.07,-0.6c1,0.07 1.53,1.03 1.53,1.03c0.87,1.52 2.34,1.07 2.91,0.83c0.09,-0.65 0.35,-1.09 0.63,-1.34c-2.22,-0.25 -4.55,-1.11 -4.55,-4.92c0,-1.11 0.38,-2 1.03,-2.71c-0.1,-0.25 -0.45,-1.29 0.1,-2.64c0,0 0.84,-0.27 2.75,1.02c0.79,-0.22 1.65,-0.33 2.5,-0.33c0.85,0 1.71,0.11 2.5,0.33c1.91,-1.29 2.75,-1.02 2.75,-1.02c0.55,1.35 0.2,2.39 0.1,2.64c0.65,0.71 1.03,1.6 1.03,2.71c0,3.82 -2.34,4.66 -4.57,4.91c0.36,0.31 0.69,0.92 0.69,1.85c0,1.34 0,2.42 0,2.74c0,0.27 0.16,0.59 0.67,0.5C19.14,20.16 22,16.42 22,12A10,10 0,0 0,12 2Z"

@Composable
fun AboutSection(language: String, onOpenChangelog: () -> Unit) {
    val firstLaunchDate = remember { DesktopSettings.load().firstLaunchDate }
    var versionCodeTaps by remember { mutableStateOf(0) }
    val devEnabled by DeveloperOptions.enabled.collectAsState()
    val authorImage = remember { loadResourceImage("author.png") }

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
        image = authorImage,
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
        icon = GithubIcon,
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
    icon: ImageVector? = null,
    image: ImageBitmap? = null,
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
        when {
            image != null -> Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(28.dp).clip(CircleShape),
            )
            icon != null -> Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
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

/** Loads a bundled classpath image from `desktop/src/main/resources/images`. */
private fun loadResourceImage(name: String): ImageBitmap? = runCatching {
    val stream = AppInfo::class.java.getResourceAsStream("/images/$name") ?: return null
    stream.use { s -> javax.imageio.ImageIO.read(s)?.toComposeImageBitmap() }
}.getOrNull()

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

/** Mutable echo-suppression state for a volume sync loop. */
private class VolumeGuard {
    var echoUntil = 0L
    var echoValue = -1f
    var lastPushed: Float? = null
}

private fun SongItem.toSyncedSong() = SyncedSong(
    id = id,
    title = title,
    artist = artists.joinToString(", ") { it.name },
    thumbnail = thumbnail,
)

/** Builds a [PlaybackSnapshot] from the current player state (null if nothing plays). */
private fun PlayerController.toPlaybackSnapshot(): PlaybackSnapshot? {
    val s = state.value
    val current = s.current ?: return null
    return PlaybackSnapshot(
        trackId = current.videoId,
        trackTitle = current.title,
        positionMs = s.positionMs,
        isPlaying = s.isPlaying,
        volume = s.volume,
        systemVolume = SystemVolume.get(),
        queue = s.queue.map { np ->
            TrackRef(id = np.videoId, title = np.title, artist = np.artist, thumbnail = np.thumbnail)
        },
        queueIndex = s.index,
    )
}

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
