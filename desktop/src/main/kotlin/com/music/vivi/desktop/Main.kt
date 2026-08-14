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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.music.innertube.YouTube
import com.music.innertube.YouTubeExtractor
import com.music.innertube.models.SongItem
import com.music.innertube.models.YouTubeLocale
import com.music.vivi.sync.SyncClient
import com.music.vivi.sync.SyncConnectionState
import com.music.vivi.sync.SyncEvent
import com.music.vivi.desktop.player.PlayerController
import com.music.vivi.sync.SyncServer
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun main() = application {
    // Configure the shared YouTube client exactly like the Android App.onCreate() does.
    YouTube.locale = YouTubeLocale(gl = "US", hl = "en")
    YouTubeExtractor.cacheDir = File(System.getProperty("user.home"), ".vivimusic/cache").apply { mkdirs() }

    var language by remember { mutableStateOf(DesktopSettings.load().language) }

    Window(onCloseRequest = ::exitApplication, title = "VIVI Music — desktop") {
        MaterialTheme {
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
                )
            }
        }
    }
}

@Composable
fun App(language: String, onLanguageChange: (String) -> Unit) {
    var backStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
    val player = remember { PlayerController() }
    val playerState by player.state.collectAsState()
    val nowPlaying = playerState.current
    val isPlaying = playerState.isPlaying

    val current = backStack.last()

    val navigate: (Screen) -> Unit = { backStack = backStack + it }
    val openRoot: (Screen) -> Unit = { backStack = listOf(it) }
    val goBack: () -> Unit = { if (backStack.size > 1) backStack = backStack.dropLast(1) }
    val playSong: (SongItem) -> Unit = { song ->
        player.play(
            NowPlaying(
                videoId = song.id,
                title = song.title,
                artist = song.artists.joinToString(", ") { it.name },
                thumbnail = song.thumbnail,
            )
        )
    }

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

    Row(Modifier.fillMaxSize()) {
        Sidebar(language, current, openRoot)
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (current) {
                    is Screen.Home -> HomeScreen(
                        language = language,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                    )
                    is Screen.Search -> SearchScreen(
                        language = language,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                    )
                    is Screen.Library -> LibraryScreen(language)
                    is Screen.Settings -> SettingsScreen(
                        language = language,
                        onLanguageChange = onLanguageChange,
                        updateStatus = updateStatus,
                        includePreReleases = includePreReleases,
                        onTogglePreReleases = { checked ->
                            includePreReleases = checked
                            DesktopSettings.save(DesktopSettings.load().copy(includePreReleases = checked))
                            runUpdateCheck()
                        },
                        onCheckUpdates = { runUpdateCheck() },
                    )
                    is Screen.Album -> AlbumScreen(
                        browseId = current.browseId,
                        language = language,
                        onBack = goBack,
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onPlaySong = playSong,
                    )
                    is Screen.Artist -> ArtistScreen(
                        browseId = current.browseId,
                        language = language,
                        onBack = goBack,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                    )
                    is Screen.Playlist -> PlaylistScreen(
                        playlistId = current.playlistId,
                        language = language,
                        onBack = goBack,
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onPlaySong = playSong,
                    )
                    is Screen.Player -> PlayerScreen(
                        nowPlaying = nowPlaying,
                        isPlaying = isPlaying,
                        positionMs = playerState.positionMs,
                        errorKey = playerState.errorKey,
                        onTogglePlay = { player.toggle() },
                        language = language,
                        onOpenLyrics = { navigate(Screen.Lyrics) },
                    )
                    is Screen.Lyrics -> LyricsScreen(
                        nowPlaying = nowPlaying,
                        language = language,
                        onBack = goBack,
                    )
                }
            }
            MiniPlayer(
                nowPlaying = nowPlaying,
                isPlaying = isPlaying,
                onTogglePlay = { player.toggle() },
                onOpen = { navigate(Screen.Player) },
            )
        }
    }
}

@Composable
fun Sidebar(language: String, current: Screen, onSelect: (Screen) -> Unit) {
    val entries = listOf(
        Screen.Home to "home",
        Screen.Search to "search",
        Screen.Library to "library",
        Screen.Settings to "settings",
    )
    Column(Modifier.width(200.dp).fillMaxHeight().padding(12.dp)) {
        Text(
            "VIVI Music",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Spacer(Modifier.height(12.dp))
        entries.forEach { (screen, key) ->
            val selected = current == screen
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(screen) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    Localization.get(language, key),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun MiniPlayer(nowPlaying: NowPlaying?, isPlaying: Boolean, onTogglePlay: () -> Unit, onOpen: () -> Unit) {
    val np = nowPlaying ?: return
    Surface(tonalElevation = 4.dp, shadowElevation = 4.dp) {
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
                Text(if (isPlaying) "⏸" else "▶", fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    language: String,
    onLanguageChange: (String) -> Unit,
    updateStatus: UpdateStatus,
    includePreReleases: Boolean,
    onTogglePreReleases: (Boolean) -> Unit,
    onCheckUpdates: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text(Localization.get(language, "settings"), style = MaterialTheme.typography.headlineMedium)
        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        LanguageSection(language, onLanguageChange)
        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        DeviceSyncSection(language)
        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        UpdateSection(language, updateStatus, includePreReleases, onTogglePreReleases, onCheckUpdates)
        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        AboutSection(language)
    }
}

@Composable
fun DeviceSyncSection(language: String) {
    var serverUrl by remember {
        val saved = DesktopSettings.load().serverUrl
        // Default to the same relay the Android app uses; treat the old
        // hardcoded localhost placeholder as "not set" so it gets migrated.
        mutableStateOf(if (saved.isBlank() || saved == "wss://localhost:8080") SyncServer.DEFAULT_URL else saved)
    }
    var joinCode by remember { mutableStateOf("") }
    var client by remember { mutableStateOf<SyncClient?>(null) }
    var connectionState by remember { mutableStateOf(SyncConnectionState.DISCONNECTED) }
    var status by remember { mutableStateOf("") }
    var pairCode by remember { mutableStateOf("") }
    var syncedSettings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    fun connect() {
        client?.disconnect()
        val created = SyncClient(
            serverUrl = serverUrl.trim(),
            deviceId = DesktopSettings.newDeviceId(),
            deviceName = "Desktop",
        )
        client = created
        // Persist only real relay URLs; the ephemeral local LAN address must
        // not become the saved default.
        if (!serverUrl.startsWith("ws://localhost")) {
            DesktopSettings.save(DesktopSettings.load().copy(serverUrl = serverUrl.trim()))
        }
    }

    val lanRelay = remember { LanSyncRelay() }
    var lanRunning by remember { mutableStateOf(false) }
    var lanAddress by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose { lanRelay.stop() }
    }

    fun startLan() {
        scope.launch {
            val p = lanRelay.start()
            lanAddress = "ws://${lanIpAddress()}:$p"
            lanRunning = true
            serverUrl = "ws://localhost:$p"
            connect()
        }
    }

    fun stopLan() {
        lanRelay.stop()
        lanRunning = false
        lanAddress = ""
        client?.disconnect()
        client = null
    }

    LaunchedEffect(client) {
        val c = client ?: return@LaunchedEffect
        c.connect()
        c.connectionState.collect {
            connectionState = it
            if (it == SyncConnectionState.ERROR) {
                status = Localization.get(language, "connection_failed")
            }
        }
    }

    LaunchedEffect(client) {
        val c = client ?: return@LaunchedEffect
        c.events.collect { event ->
            when (event) {
                is SyncEvent.Connected -> {
                    status = Localization.get(language, "connected")
                    if (DesktopSettings.load().pairId.isNotEmpty()) c.pullSnapshot()
                }
                is SyncEvent.Disconnected -> status = Localization.get(language, "disconnected")
                is SyncEvent.PairCode -> {
                    pairCode = event.code
                    status = "${Localization.get(language, "code_generated")}: ${event.code}"
                }
                is SyncEvent.Paired -> {
                    status = "${Localization.get(language, "paired_with")} ${event.peerDeviceName}"
                    DesktopSettings.save(DesktopSettings.load().copy(pairId = event.pairId))
                }
                is SyncEvent.SnapshotReceived -> {
                    status = "${Localization.get(language, "snapshot_received")} ${event.fromDeviceId}"
                    syncedSettings = event.snapshot.settings
                    DesktopSettings.save(DesktopSettings.load().copy(settings = event.snapshot.settings))
                }
                is SyncEvent.Error -> status = "${Localization.get(language, "error")}: ${event.message}"
            }
        }
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
        Button(onClick = { connect() }) { Text(Localization.get(language, "connect")) }
    }

    Text(Localization.get(language, "lan_sync"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
    Button(
        onClick = { if (lanRunning) stopLan() else startLan() },
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
            Localization.get(language, "lan_hint"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            if (client?.connectionState?.value != SyncConnectionState.CONNECTED) connect()
            client?.requestPairingCode()
        }) { Text(Localization.get(language, "generate_code")) }
        OutlinedTextField(
            value = joinCode,
            onValueChange = { joinCode = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text(Localization.get(language, "code_placeholder")) },
        )
        Button(onClick = {
            if (client?.connectionState?.value != SyncConnectionState.CONNECTED) connect()
            client?.joinPair(joinCode)
        }) { Text(Localization.get(language, "pair")) }
    }

    if (pairCode.isNotEmpty()) {
        Text(
            "${Localization.get(language, "code_hint")}: $pairCode",
            style = MaterialTheme.typography.titleMedium,
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
            Button(onClick = { openUrl(status.url) }, modifier = Modifier.padding(top = 4.dp)) {
                Text(Localization.get(language, "download"))
            }
        }
        is UpdateStatus.Failed -> Text(
            "${Localization.get(language, "update_failed")}: ${status.message}",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp),
        )
        is UpdateStatus.Idle -> Unit
    }
}

private fun openUrl(url: String) {
    runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(url)) }
}

@Composable
fun AboutSection(language: String) {
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
}
