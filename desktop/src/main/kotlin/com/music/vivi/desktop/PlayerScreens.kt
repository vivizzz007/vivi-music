package com.music.vivi.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.lrclib.LrcLib

@Composable
fun PlayerScreen(
    queue: List<NowPlaying>,
    index: Int,
    isPlaying: Boolean,
    positionMs: Long,
    errorKey: String?,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    language: String,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    val np = queue.getOrNull(index)
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
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Text("⏮", fontSize = 28.sp)
            }
            IconButton(onClick = onTogglePlay) {
                Text(if (isPlaying) "⏸" else "▶", fontSize = 40.sp)
            }
            IconButton(onClick = onNext) {
                Text("⏭", fontSize = 28.sp)
            }
        }
        Text(
            formatTime(positionMs),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onOpenLyrics) { Text(Localization.get(language, "lyrics")) }
            Button(onClick = onOpenQueue) { Text("${Localization.get(language, "queue")} (${queue.size})") }
        }
        if (errorKey != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                Localization.get(language, errorKey),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun QueueScreen(
    queue: List<NowPlaying>,
    index: Int,
    language: String,
    onBack: () -> Unit,
    onSkipTo: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onClear: () -> Unit,
    onReorder: (List<NowPlaying>) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val localQueue = remember { mutableStateListOf<NowPlaying>() }
    var hasDragged by remember { mutableStateOf(false) }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localQueue.add(to.index, localQueue.removeAt(from.index))
        hasDragged = true
    }

    // Keep the local copy in sync with the real queue (skip while dragging).
    LaunchedEffect(queue) {
        if (!reorderableState.isAnyItemDragging) {
            localQueue.clear()
            localQueue.addAll(queue)
        }
    }

    // Commit the new order once the drag ends.
    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging && hasDragged) {
            onReorder(localQueue.toList())
            hasDragged = false
        }
    }

    val currentVideoId = queue.getOrNull(index)?.videoId

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Localization.get(language, "queue"), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            if (queue.isNotEmpty()) {
                Button(onClick = onClear) { Text(Localization.get(language, "clear_queue")) }
            }
        }
        if (queue.isEmpty()) {
            Text(
                Localization.get(language, "queue_empty"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            Text(
                Localization.get(language, "drag_to_reorder"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                itemsIndexed(localQueue, key = { _, item -> item.videoId }) { i, item ->
                    val isCurrent = item.videoId == currentVideoId
                    ReorderableItem(state = reorderableState, key = item.videoId) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSkipTo(i) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "⠿",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .draggableHandle()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                            )
                            Text(
                                if (isCurrent) "▶" else "${i + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Thumbnail(item.thumbnail, Modifier.size(44.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    item.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                "✕",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { onRemoveAt(i) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun cleanLyrics(text: String): List<String> =
    text.lines()
        .map { it.replace(Regex("""\[\d{1,2}:\d{1,2}(\.\d{1,3})?\]"""), "").trim() }
        .filter { it.isNotEmpty() }

/** A single synced lyric line with its start time in milliseconds. */
private data class LyricLine(val timeMs: Long, val text: String)

/**
 * Parses LRC synced lyrics. Returns null when the text has no timestamps
 * (plain lyrics). Rich-sync `<mm:ss.xx>` word tags are stripped.
 */
private fun parseLrc(text: String): List<LyricLine>? {
    val timeRegex = Regex("""\[(\d{1,2}):(\d{1,2})(?:\.(\d{1,3}))?]""")
    val lineRegex = Regex("""((\[\d{1,2}:\d{1,2}(?:\.\d{1,3})?]\s*)+)(.*)""")
    val result = mutableListOf<LyricLine>()
    var hasTimestamps = false

    for (raw in text.lines()) {
        val line = raw.trim()
        if (line.isEmpty()) continue
        val match = lineRegex.find(line) ?: continue
        val timeTokens = match.groupValues[1]
        var content = match.groupValues[3]
            .replace(Regex("""<\d{1,2}:\d{2}(?:\.\d{1,3})?>\s*"""), "")
            .trim()
        if (content.isEmpty()) continue

        timeRegex.findAll(timeTokens).forEach { t ->
            val min = t.groupValues[1].toLongOrNull() ?: 0L
            val sec = t.groupValues[2].toLongOrNull() ?: 0L
            val frac = (t.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L)
            result.add(LyricLine(min * 60_000 + sec * 1_000 + frac, content))
            hasTimestamps = true
        }
    }

    if (!hasTimestamps) return null
    return result.sortedBy { it.timeMs }
}

/** Index of the line currently being sung (last line with time <= position). */
private fun currentLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
    var index = -1
    for (i in lines.indices) {
        if (lines[i].timeMs <= positionMs) index = i else break
    }
    return index
}

@Composable
fun LyricsScreen(
    nowPlaying: NowPlaying?,
    positionMs: Long,
    isPlaying: Boolean,
    language: String,
    onBack: () -> Unit,
) {
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
            else -> {
                val lines = remember(lyrics) { parseLrc(lyrics.orEmpty()) }
                if (lines.isNullOrEmpty()) {
                    // Plain (non-synced) lyrics fallback.
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(top = 8.dp),
                    ) {
                        cleanLyrics(lyrics!!).forEach { line ->
                            Text(line, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                } else {
                    val listState = rememberLazyListState()
                    val currentIndex = currentLineIndex(lines, positionMs)
                    LaunchedEffect(currentIndex) {
                        if (currentIndex >= 0) {
                            listState.animateScrollToItem(maxOf(0, currentIndex - 3))
                        }
                    }
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                        itemsIndexed(lines) { i, line ->
                            val isCurrent = i == currentIndex
                            Text(
                                line.text,
                                style = if (isCurrent) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
