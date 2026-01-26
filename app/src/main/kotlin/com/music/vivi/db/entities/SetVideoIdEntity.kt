package com.music.vivi.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caches the association between a Video ID and its "Set Video ID" (YouTube Music specific concept).
 * Used to link audio-only tracks to their music video counterparts.
 */
@Entity(tableName = "set_video_id")
data class SetVideoIdEntity(
    @PrimaryKey(autoGenerate = false)
    val videoId: String = "",
    val setVideoId: String? = null,
)
