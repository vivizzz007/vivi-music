package com.music.vivi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.launch

fun main() = application {
    // Configure the shared YouTube client exactly like the Android App.onCreate() does.
    YouTube.locale = YouTubeLocale(gl = "US", hl = "en")

    Window(onCloseRequest = ::exitApplication, title = "VIVI Music — desktop") {
        App()
    }
}

@Composable
fun App() {
    MaterialTheme {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text("VIVI Music (desktop)", style = MaterialTheme.typography.headlineMedium)
            SearchSection()
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            DeviceSyncSection()
        }
    }
}

@Composable
fun SearchSection() {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Text("Ricerca", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("Cerca su YouTube Music") },
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
            Text("Cerca")
        }
    }

    when {
        loading -> Text("Caricamento…", Modifier.padding(top = 8.dp))
        error != null -> Text("Errore: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        else -> Column(Modifier.padding(top = 8.dp)) {
            results.forEach { title -> Text(title, Modifier.padding(vertical = 4.dp)) }
        }
    }
}

@Composable
fun DeviceSyncSection() {
    var serverUrl by remember {
        mutableStateOf(DesktopSettings.load().serverUrl.ifBlank { "wss://localhost:8080" })
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
        c.connectionState.collect { connectionState = it }
    }

    LaunchedEffect(client) {
        val c = client ?: return@LaunchedEffect
        c.events.collect { event ->
            when (event) {
                is SyncEvent.Connected -> {
                    status = "Connesso"
                    if (DesktopSettings.load().pairId.isNotEmpty()) c.pullSnapshot()
                }
                is SyncEvent.Disconnected -> status = "Disconnesso"
                is SyncEvent.PairCode -> {
                    pairCode = event.code
                    status = "Codice generato: ${event.code}"
                }
                is SyncEvent.Paired -> {
                    status = "Accoppiato con ${event.peerDeviceName}"
                    DesktopSettings.save(DesktopSettings.load().copy(pairId = event.pairId))
                }
                is SyncEvent.SnapshotReceived -> {
                    status = "Snapshot ricevuto da ${event.fromDeviceId}"
                    syncedSettings = event.snapshot.settings
                    DesktopSettings.save(DesktopSettings.load().copy(settings = event.snapshot.settings))
                }
                is SyncEvent.Error -> status = "Errore: ${event.message}"
            }
        }
        c.connect()
    }

    Text("Device sync", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text("Relay server (wss://)") },
        )
        Button(onClick = { connect() }) { Text("Connetti") }
    }

    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            if (client?.connectionState?.value != SyncConnectionState.CONNECTED) connect()
            client?.requestPairingCode()
        }) { Text("Genera codice") }
        OutlinedTextField(
            value = joinCode,
            onValueChange = { joinCode = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("Codice a 6 cifre") },
        )
        Button(onClick = {
            if (client?.connectionState?.value != SyncConnectionState.CONNECTED) connect()
            client?.joinPair(joinCode)
        }) { Text("Accoppia") }
    }

    if (pairCode.isNotEmpty()) {
        Text(
            "Codice da inserire sull'altro dispositivo: $pairCode",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }

    Text("Stato: $connectionState — $status", modifier = Modifier.padding(top = 8.dp))

    if (syncedSettings.isNotEmpty()) {
        Text("Impostazioni sincronizzate", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Column(Modifier.padding(top = 4.dp)) {
            syncedSettings.entries.sortedBy { it.key }.forEach { (k, v) ->
                Text("$k = $v", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}
