package com.music.vivi.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.music.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.apache.commons.lang3.RandomStringUtils
import java.time.LocalDateTime

/**
 * Represents an Artist stored in the local database.
 *
 * @property id Unique identifier (YouTube Channel ID or generated local ID).
 * @property name Artist name.
 * @property thumbnailUrl Profile picture URL.
 * @property channelId YouTube Channel ID (redundant with ID usually, but separate if ID is internal).
 * @property lastUpdateTime Last sync timestamp.
 * @property bookmarkedAt Timestamp when user subscribed/bookmarked.
 * @property isLocal Whether this artist is from local files.
 */
@Immutable
@Entity(tableName = "artist")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val channelId: String? = null,
    val lastUpdateTime: LocalDateTime = LocalDateTime.now(),
    val bookmarkedAt: LocalDateTime? = null,
    @ColumnInfo(name = "isLocal", defaultValue = false.toString())
    val isLocal: Boolean = false,
) {
    val isYouTubeArtist: Boolean
        get() = id.startsWith("UC") || id.startsWith("FEmusic_library_privately_owned_artist")

    val isPrivatelyOwnedArtist: Boolean
        get() = id.startsWith("FEmusic_library_privately_owned_artist")

    fun localToggleLike() = copy(
        bookmarkedAt = if (bookmarkedAt != null) null else LocalDateTime.now()
    )

    fun toggleLike() = localToggleLike().also {
        CoroutineScope(Dispatchers.IO).launch {
            if (channelId == null) {
                YouTube.subscribeChannel(YouTube.getChannelId(id), bookmarkedAt == null)
            } else {
                YouTube.subscribeChannel(channelId, bookmarkedAt == null)
            }
            this.cancel()
        }
    }

    companion object {
        fun generateArtistId() = "LA" + RandomStringUtils.insecure().next(8, true, false)
    }
}
