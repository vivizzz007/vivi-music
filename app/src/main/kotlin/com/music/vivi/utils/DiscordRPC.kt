package com.music.vivi.utils

import android.content.Context
import com.music.vivi.R
import com.music.vivi.db.entities.Song
import com.my.kizzy.rpc.KizzyRPC
import com.my.kizzy.rpc.RpcImage

/**
 * Handles Discord Rich Presence integration via [KizzyRPC].
 *
 * Updates the user's Discord status to show what they are currently listening to on Vivi Music.
 * Features include:
 * - Song Title, Artist, Album.
 * - Time remaining/elapsed.
 * - Links to YouTube Music and the GitHub project.
 */
class DiscordRPC(val context: Context, token: String) : KizzyRPC(token) {
    /**
     * Updates the Rich Presence status with the current song details.
     *
     * @param song The currently playing [Song].
     * @param currentPlaybackTimeMillis Current playback position in milliseconds.
     * @param playbackSpeed Current playback speed (affects estimated end time).
     * @param useDetails Whether to show detailed status.
     */
    suspend fun updateSong(
        song: Song,
        currentPlaybackTimeMillis: Long,
        playbackSpeed: Float = 1.0f,
        useDetails: Boolean = false,
    ) = runCatching {
        val currentTime = System.currentTimeMillis()

        val adjustedPlaybackTime = (currentPlaybackTimeMillis / playbackSpeed).toLong()
        val calculatedStartTime = currentTime - adjustedPlaybackTime

        val songTitleWithRate = if (playbackSpeed != 1.0f) {
            "${song.song.title} [${String.format("%.2fx", playbackSpeed)}]"
        } else {
            song.song.title
        }

        val remainingDuration = song.song.duration * 1000L - currentPlaybackTimeMillis
        val adjustedRemainingDuration = (remainingDuration / playbackSpeed).toLong()

        setActivity(
            name = context.getString(R.string.app_name).removeSuffix(" Debug"),
            details = songTitleWithRate,
            state = song.artists.joinToString { it.name },
            detailsUrl = "https://music.youtube.com/watch?v=${song.song.id}",
            largeImage = song.song.thumbnailUrl?.let { RpcImage.ExternalImage(it) },
            smallImage = song.artists.firstOrNull()?.thumbnailUrl?.let { RpcImage.ExternalImage(it) },
            largeText = song.album?.title,
            smallText = song.artists.firstOrNull()?.name,
            buttons = listOf(
                "Listen on YouTube Music" to "https://music.youtube.com/watch?v=${song.song.id}",
                "Visit Vivimusic" to "https://github.com/vivizzz007/vivi-music"
            ),
            type = Type.LISTENING,
            statusDisplayType = if (useDetails) StatusDisplayType.DETAILS else StatusDisplayType.STATE,
            since = currentTime,
            startTime = calculatedStartTime,
            endTime = currentTime + adjustedRemainingDuration,
            applicationId = APPLICATION_ID
        )
    }

    companion object {
        private const val APPLICATION_ID = "1411019391843172514"
    }
}
