package com.music.vivi.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores synced or static lyrics for a song.
 *
 * @property id The Song ID.
 * @property lyrics The lyrics textual content (often in a specific format like timed LRC or plain text).
 */
@Entity(tableName = "lyrics")
data class LyricsEntity(@PrimaryKey val id: String, val lyrics: String) {
    companion object {
        const val LYRICS_NOT_FOUND = "LYRICS_NOT_FOUND"
    }
}
