package com.music.vivi.livemedia

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.music.vivi.R
import java.util.Locale

fun buildArtisAlbumTitle(
    showArtistName: Boolean,
    showAlbumName: Boolean,
    musicState: MusicState
): String {
    val parts = mutableListOf<String>()

    val showArtist = showArtistName && musicState.artist.isNotBlank() && musicState.artist != MusicState.EMPTY_ARTIST
    val showAlbum = showAlbumName && musicState.albumName.isNotBlank() && musicState.albumName != MusicState.EMPTY_ALBUM

    if (showArtist) parts.add(musicState.artist)
    if (showAlbum) parts.add(musicState.albumName)

    val result = parts.joinToString(" - ")
    val MAX_LENGTH = 70

    return if (result.length > MAX_LENGTH) {
        result.take(MAX_LENGTH) + "..."
    } else {
        result
    }
}

fun formatMusicProgress(currentPosition: Int, duration: Int): String {
    val positionStr = formatTime(currentPosition)
    val durationStr = formatTime(duration)
    return "$positionStr / $durationStr"
}

fun formatTime(millis: Int): String {
    if (millis <= 0) return "0:00"

    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

fun combineProviderAndTimestamp(
    showTimestamp: Boolean,
    position: Int,
    duration: Int
) = buildList {
    if (showTimestamp) add(formatMusicProgress(position, duration))
}.joinToString(" • ").ifBlank { null }

fun providePillText(
    title: String,
    position: Int,
    duration: Int,
    isPlaying: Boolean,
    pillContent: String
): String {
    val showTime = isPlaying && duration > 0
    val truncatedTitle = title.take(7).trimEnd()

    return when (pillContent) {
        "ELAPSED" -> if (showTime) formatTime(position) else truncatedTitle
        "REMAINING" -> if (showTime) formatTime(duration - position) else truncatedTitle
        else -> truncatedTitle
    }
}

fun buildBaseBigTextStyle() = NotificationCompat.BigTextStyle()

fun <T> createAction(
    icon: Int,
    title: String,
    action: String,
    requestCode: Int,
    packageContext: Context,
    cls: Class<T>,
): NotificationCompat.Action {
    val intent = Intent(packageContext, cls).setAction(action)
    val pendingIntent = PendingIntent.getService(
        packageContext,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    return NotificationCompat.Action(icon, title, pendingIntent)
}

fun PackageManager.getAppName(packageName: String): String {
    return try {
        getApplicationLabel(getApplicationInfo(packageName, 0)).toString()
    } catch (e: Exception) {
        packageName
    }
}

fun getAppIcon(): Int {
    return R.drawable.vivi_music_icon // Use the custom circle music icon
}
