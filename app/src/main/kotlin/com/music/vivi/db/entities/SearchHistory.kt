package com.music.vivi.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores user search queries locally.
 *
 * @property id Auto-generated ID.
 * @property query The search query string.
 */
@Entity(
    tableName = "search_history",
    indices = [
        Index(
            value = ["query"],
            unique = true
        )
    ]
)
data class SearchHistory(@PrimaryKey(autoGenerate = true) val id: Long = 0, val query: String)
