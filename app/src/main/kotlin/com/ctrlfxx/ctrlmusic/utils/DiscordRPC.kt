/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.ctrlfxx.ctrlmusic.utils

import android.content.Context
import com.ctrlfxx.ctrlmusic.db.entities.Song

/**
 * Ctrl Music ships without Discord integration. This inert stand-in keeps the
 * player service's existing call sites compiling; it never opens a connection.
 */
@Suppress("UNUSED_PARAMETER")
class DiscordRPC(
    val context: Context,
    token: String,
) {
    suspend fun updateSong(
        song: Song,
        currentPlaybackTimeMillis: Long,
        playbackSpeed: Float = 1.0f,
        useDetails: Boolean = false,
        status: String = "online",
        button1Text: String = "",
        button1Visible: Boolean = true,
        button2Text: String = "",
        button2Visible: Boolean = true,
        activityType: String = "listening",
        activityName: String = "",
    ): Result<Unit> = Result.success(Unit)

    fun isRpcRunning(): Boolean = false

    fun closeRPC() {}

    suspend fun close() {}
}
