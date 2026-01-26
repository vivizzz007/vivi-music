package com.music.vivi.db.entities

/**
 * Base sealed class for local media items (Songs, Albums, Artists, Playlists).
 * Provides a common interface for accessing core metadata properties.
 */
sealed class LocalItem {
    abstract val id: String
    abstract val title: String
    abstract val thumbnailUrl: String?
}
