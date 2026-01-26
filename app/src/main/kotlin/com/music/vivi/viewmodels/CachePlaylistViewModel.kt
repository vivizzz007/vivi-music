package com.music.vivi.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.cache.SimpleCache
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.Song
import com.music.vivi.di.DownloadCache
import com.music.vivi.di.PlayerCache
import com.music.vivi.extensions.filterExplicit
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for managing the "Cached" songs smart playlist.
 *
 * Responsibilities:
 * - Monitors the physical cache (PlayerCache & DownloadCache).
 * - Identifies songs that are fully cached but not explicitly "Downloaded" by the user.
 * - Updates the database to mark these songs as "Cached" so they appear in cache lists.
 * - Provides the list of cached songs sorted by download date.
 */
@HiltViewModel
public class CachePlaylistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    @PlayerCache private val playerCache: SimpleCache,
    @DownloadCache private val downloadCache: SimpleCache,
) : ViewModel() {

    private val _cachedSongs = MutableStateFlow<List<Song>>(emptyList())
    public val cachedSongs: StateFlow<List<Song>> = _cachedSongs

    init {
        viewModelScope.launch {
            while (true) {
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val cachedIds = playerCache.keys.mapNotNull { it?.toString() }.toSet()
                val downloadedIds = downloadCache.keys.mapNotNull { it?.toString() }.toSet()
                val pureCacheIds = cachedIds.subtract(downloadedIds)

                val songs = if (pureCacheIds.isNotEmpty()) {
                    database.getSongsByIds(pureCacheIds.toList())
                } else {
                    emptyList()
                }

                val completeSongs = songs.filter {
                    val contentLength = it.format?.contentLength
                    contentLength != null && playerCache.isCached(it.song.id, 0, contentLength)
                }

                if (completeSongs.isNotEmpty()) {
                    database.query {
                        completeSongs.forEach {
                            if (it.song.dateDownload == null) {
                                update(it.song.copy(dateDownload = LocalDateTime.now()))
                            }
                        }
                    }
                }

                _cachedSongs.value = completeSongs
                    .filter { it.song.dateDownload != null }
                    .sortedByDescending { it.song.dateDownload }
                    .filterExplicit(hideExplicit)

                delay(1000)
            }
        }
    }

    public fun removeSongFromCache(songId: String) {
        playerCache.removeResource(songId)
    }
}
