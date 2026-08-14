package com.music.vivi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        var query by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<List<String>>(emptyList()) }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("VIVI Music (desktop)", style = MaterialTheme.typography.headlineMedium)

            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                else -> LazyColumn(Modifier.padding(top = 8.dp)) {
                    items(results) { title -> Text(title, Modifier.padding(vertical = 4.dp)) }
                }
            }
        }
    }
}
