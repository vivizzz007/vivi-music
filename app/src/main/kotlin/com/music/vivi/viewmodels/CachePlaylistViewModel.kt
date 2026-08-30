/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.cache.SimpleCache
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.HideVideoSongsKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.Song
import com.music.vivi.di.DownloadCache
import com.music.vivi.di.PlayerCache
import com.music.vivi.extensions.filterExplicit
import com.music.vivi.extensions.filterVideoSongs
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class CachePlaylistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    @PlayerCache private val playerCache: SimpleCache,
    @DownloadCache private val downloadCache: SimpleCache
) : ViewModel() {

    private val _cachedSongs = MutableStateFlow<List<Song>>(emptyList())
    val cachedSongs: StateFlow<List<Song>> = _cachedSongs

    init {
        viewModelScope.launch {
            while (true) {
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

                val candidateIds = playerCache.keys.toSet() + downloadCache.keys.toSet()
                val songs =
                    if (candidateIds.isNotEmpty()) {
                        database.getSongsByIds(candidateIds.toList())
                    } else {
                        emptyList()
                    }

                val flagged = songs.filter { it.song.dateDownload != null }
                val stillValid = mutableListOf<Song>()

                for (song in flagged) {
                    val contentLength = song.format?.contentLength
                    val stillCached =
                        song.song.isDownloaded ||
                            (
                                contentLength != null &&
                                    (
                                        downloadCache.isCached(song.song.id, 0, contentLength) ||
                                            playerCache.isCached(song.song.id, 0, contentLength)
                                    )
                            )
                    if (stillCached) {
                        stillValid += song
                    } else {
                        database.query { update(song.song.copy(dateDownload = null)) }
                    }
                }

                _cachedSongs.value =
                    stillValid
                        .sortedByDescending { it.song.dateDownload }
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)

                delay(1000)
            }
        }
    }

    fun removeSongFromCache(songId: String) {
        playerCache.removeResource(songId)
    }
}
