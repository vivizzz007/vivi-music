package com.music.vivi.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeLocale
import com.music.vivi.sync.SyncClient
import com.music.vivi.sync.SyncConnectionState
import com.music.vivi.sync.SyncEvent
import com.music.vivi.sync.SyncServer
import kotlinx.coroutines.launch

fun main() = application {
    // Configure the shared YouTube client exactly like the Android App.onCreate() does.
    YouTube.locale = YouTubeLocale(gl = "US", hl = "en")

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
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text(Localization.get(language, "header"), style = MaterialTheme.typography.headlineMedium)
        SearchSection(language)
        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        DeviceSyncSection(language)
        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        LanguageSection(language, onLanguageChange)
        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        AboutSection(language)
    }
}

@Composable
fun SearchSection(language: String) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Text(Localization.get(language, "search"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text(Localization.get(language, "search_placeholder")) },
        )
        Button(onClick = {
            scope.launch {
                loading = true
                error = null
                results = emptyList()
                YouTube.search(query.trim(), YouTube.SearchFilter.FILTER_SONG)
                    .fold(
                        onSuccess = { page -> results = page.items.map { it.title } },
                        onFailure = { error = it.message },
                    )
                loading = false
            }
        }) {
            Text(Localization.get(language, "search_button"))
        }
    }

    when {
        loading -> Text(Localization.get(language, "loading"), Modifier.padding(top = 8.dp))
        error != null -> Text("${Localization.get(language, "error")}: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        else -> Column(Modifier.padding(top = 8.dp)) {
            results.forEach { title -> Text(title, Modifier.padding(vertical = 4.dp)) }
        }
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
        DesktopSettings.save(DesktopSettings.load().copy(serverUrl = serverUrl.trim()))
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
