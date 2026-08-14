package com.music.vivi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.lrclib.LrcLib

@Composable
fun PlayerScreen(
    nowPlaying: NowPlaying?,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    language: String,
    onOpenLyrics: () -> Unit,
) {
    val np = nowPlaying
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (np == null) {
            Text(
                Localization.get(language, "nothing_playing"),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 32.dp),
            )
            return@Column
        }
        Spacer(Modifier.height(16.dp))
        Thumbnail(np.thumbnail, Modifier.size(240.dp))
        Spacer(Modifier.height(24.dp))
        Text(np.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(np.artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onTogglePlay) {
                Text(if (isPlaying) "⏸" else "▶", fontSize = 32.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenLyrics) { Text(Localization.get(language, "lyrics")) }
        Spacer(Modifier.height(16.dp))
        Text(
            Localization.get(language, "playback_soon"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun cleanLyrics(text: String): List<String> =
    text.lines()
        .map { it.replace(Regex("""\[\d{1,2}:\d{1,2}(\.\d{1,3})?\]"""), "").trim() }
        .filter { it.isNotEmpty() }

@Composable
fun LyricsScreen(nowPlaying: NowPlaying?, language: String, onBack: () -> Unit) {
    var lyrics by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val np = nowPlaying

    LaunchedEffect(np?.videoId) {
        if (np == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        LrcLib.getLyrics(title = np.title, artist = np.artist, duration = -1).fold(
            onSuccess = { lyrics = it; error = null },
            onFailure = { error = it.message },
        )
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Text(Localization.get(language, "lyrics"), style = MaterialTheme.typography.titleLarge)
        when {
            np == null -> Text(
                Localization.get(language, "nothing_playing"),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            loading -> LoadingBox(language)
            error != null || lyrics == null -> Text(
                Localization.get(language, "no_lyrics"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            else -> Column(
                Modifier.verticalScroll(rememberScrollState()).padding(top = 8.dp),
            ) {
                cleanLyrics(lyrics!!).forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}
