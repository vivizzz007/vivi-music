package com.music.vivi.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.YTItem
import com.music.vivi.constants.PlaylistSongSortDescendingKey
import com.music.vivi.constants.PlaylistSongSortType
import com.music.vivi.constants.PlaylistSongSortTypeKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.PlaylistSong
import com.music.vivi.extensions.reversed
import com.music.vivi.extensions.toEnum
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for a User-created Local Playlist.
 * Handles fetching playlist details and sorting its songs based on user preference.
 */
@HiltViewModel
public class LocalPlaylistViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _playlistId = MutableStateFlow(savedStateHandle.get<String>("playlistId"))
    public val playlistId: StateFlow<String?> = _playlistId.asStateFlow()

    public val relatedItems: MutableStateFlow<List<YTItem>> = MutableStateFlow(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    public val playlist: StateFlow<com.music.vivi.db.entities.Playlist?> = _playlistId.filterNotNull().flatMapLatest { id ->
        database.playlist(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    public val playlistSongs: StateFlow<List<PlaylistSong>> =
        combine(
            _playlistId.filterNotNull(),
            context.dataStore.data
                .map {
                    it[PlaylistSongSortTypeKey].toEnum(PlaylistSongSortType.CUSTOM) to
                        (
                            it[PlaylistSongSortDescendingKey]
                                ?: true
                            )
                }.distinctUntilChanged()
        ) { id, sortOptions ->
            Triple(id, sortOptions.first, sortOptions.second)
        }.flatMapLatest { (id, sortType, sortDescending) ->
            database.playlistSongs(id).map { songs ->
                sortSongs(songs, sortType, sortDescending)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            playlistSongs.collect { songs ->
                if (songs.isNotEmpty()) {
                    fetchRelatedItems(songs)
                }
            }
        }
    }

    private fun fetchRelatedItems(songs: List<PlaylistSong>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get the most common artists and a sample of songs from the playlist
                val artistCounts = songs
                    .flatMap { it.song.artists }
                    .groupingBy { it.id }
                    .eachCount()
                    .filter { it.key != null }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(3)

                val playlists = mutableListOf<YTItem>()
                val albums = mutableListOf<YTItem>()
                
                // Fetch related playlists from top artists
                for ((artistId, _) in artistCounts) {
                    if (artistId == null) continue
                    
                    YouTube.artist(artistId).onSuccess { artistPage ->
                        // Collect playlists and albums separately
                        artistPage.sections.forEach { section ->
                            val sectionItems = section.items
                            playlists.addAll(
                                sectionItems.filterIsInstance<com.music.innertube.models.PlaylistItem>().take(4)
                            )
                            albums.addAll(
                                sectionItems.filterIsInstance<com.music.innertube.models.AlbumItem>().take(2)
                            )
                        }
                    }
                    
                    // Limit API calls
                    if (playlists.size >= 8) break
                }
                
                // Combine playlists first, then albums
                val relatedItemsList = mutableListOf<YTItem>()
                relatedItemsList.addAll(playlists.take(8))
                relatedItemsList.addAll(albums.take(4))
                
                // If we don't have enough items, try getting related playlists from a random song
                if (relatedItemsList.size < 6 && songs.isNotEmpty()) {
                    val randomSongs = songs.shuffled().take(2)
                    for (randomSong in randomSongs) {
                        YouTube.next(com.music.innertube.models.WatchEndpoint(videoId = randomSong.song.song.id))
                            .onSuccess { nextPage ->
                                // From the radio/next, extract related content
                                relatedItemsList.addAll(
                                    nextPage.items.take(3).map { it }
                                )
                            }
                        if (relatedItemsList.size >= 12) break
                    }
                }
                
                // Prioritize playlists and albums over other content
                val finalItems = relatedItemsList
                    .distinctBy { it.id }
                    .sortedBy { item ->
                        when (item) {
                            is com.music.innertube.models.PlaylistItem -> 0 // Playlists first
                            is com.music.innertube.models.AlbumItem -> 1     // Albums second
                            is com.music.innertube.models.ArtistItem -> 2    // Artists third
                            else -> 3                                         // Songs last
                        }
                    }
                    .take(12)
                
                // Update the related items
                relatedItems.value = finalItems
                    
            } catch (e: Exception) {
                reportException(e)
                relatedItems.value = emptyList()
            }
        }
    }

    private fun sortSongs(
        songs: List<PlaylistSong>,
        sortType: PlaylistSongSortType,
        sortDescending: Boolean,
    ): List<PlaylistSong> = when (sortType) {
        PlaylistSongSortType.CUSTOM -> songs
        PlaylistSongSortType.CREATE_DATE -> songs.sortedBy { it.map.id }
        PlaylistSongSortType.NAME -> {
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            songs.sortedWith(compareBy(collator) { it.song.song.title })
        }
        PlaylistSongSortType.ARTIST -> {
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            songs
                .sortedWith(compareBy(collator) { song -> song.song.artists.joinToString("") { it.name } })
                .groupBy { it.song.album?.title }
                .flatMap { (_, songsByAlbum) ->
                    songsByAlbum.sortedBy {
                        it.song.artists.joinToString(
                            ""
                        ) { it.name }
                    }
                }
        }
        PlaylistSongSortType.PLAY_TIME -> songs.sortedBy { it.song.song.totalPlayTime }
    }.reversed(sortDescending && sortType != PlaylistSongSortType.CUSTOM)

    public fun setPlaylistId(id: String) {
        if (_playlistId.value != id) {
            _playlistId.value = id
            relatedItems.value = emptyList()
        }
    }
}
