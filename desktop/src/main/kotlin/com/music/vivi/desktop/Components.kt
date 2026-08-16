package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem

/** Square-ish artwork with a neutral placeholder behind it while loading. */
@Composable
fun Thumbnail(url: String?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun subtitleOf(item: YTItem): String = when (item) {
    is SongItem -> item.artists.joinToString(", ") { it.name }
    is AlbumItem -> buildString {
        item.artists?.joinToString(", ") { it.name }?.takeIf { it.isNotBlank() }?.let { append(it) }
        if (item.year != null) {
            if (isNotEmpty()) append(" • ")
            append(item.year)
        }
    }
    is ArtistItem -> "Artist"
    is PlaylistItem -> item.author?.name ?: item.songCountText.orEmpty()
}

/** Compact tile used in the Home/Search/Artist carousels and Browse grid. */
@Composable
fun YtItemCard(item: YTItem, onClick: () -> Unit, width: Dp? = 140.dp, modifier: Modifier = Modifier) {
    val root = if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()
    Column(root.then(modifier).clickable(onClick = onClick)) {
        Thumbnail(item.thumbnail, Modifier.fillMaxWidth().aspectRatio(1f))
        Text(
            item.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val subtitle = subtitleOf(item)
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One song in a vertical list (album / playlist / search songs). */
@Composable
fun SongRow(
    song: SongItem,
    onClick: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(song.thumbnail, Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                song.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onAddToPlaylist != null) {
            Icon(
                Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = "Add to playlist",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onAddToPlaylist() }
                    .padding(2.dp),
            )
        }
        if (onAddToQueue != null) {
            Text(
                "＋",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onAddToQueue() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        song.duration?.let {
            Text(it.let(::formatDuration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LoadingBox(language: String) {
    Box(Modifier.fillMaxSize().padding(16.dp)) { Text(Localization.get(language, "loading")) }
}

@Composable
fun ErrorBox(language: String, message: String?) {
    Box(Modifier.fillMaxSize().padding(16.dp)) {
        Text("${Localization.get(language, "error")}: $message", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun BackButton(language: String, onClick: () -> Unit) {
    Text(
        "‹ ${Localization.get(language, "back")}",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 8.dp),
    )
}

/** Route a tap on any [YTItem] to the matching screen/action. */
fun onItemClick(
    item: YTItem,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
) {
    when (item) {
        is AlbumItem -> onOpenAlbum(item.browseId)
        is ArtistItem -> onOpenArtist(item.id)
        is PlaylistItem -> onOpenPlaylist(item.id)
        is SongItem -> onPlaySong(item)
    }
}

/**
 * Section header in the style of the Android app's `NavigationTitle`: an
 * optional label above a bold, primary-coloured title, with an optional
 * "Play all" button and a chevron when the whole header is clickable.
 */
@Composable
fun SectionHeader(
    title: String,
    language: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    onClick: (() -> Unit)? = null,
    onPlayAll: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            if (!label.isNullOrBlank()) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (onPlayAll != null) {
            OutlinedButton(
                onClick = onPlayAll,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp),
            ) {
                Text(Localization.get(language, "play_all"), style = MaterialTheme.typography.labelSmall)
            }
        }

        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** A single Mood & genres chip, styled like the Android app's button. */
@Composable
fun MoodAndGenresButton(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
