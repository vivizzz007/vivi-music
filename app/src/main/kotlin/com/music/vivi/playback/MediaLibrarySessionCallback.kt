/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.playback

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import coil3.imageLoader
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.music.innertube.YouTube
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.vivi.R
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.HideVideoSongsKey
import com.music.vivi.constants.MediaSessionConstants
import com.music.vivi.constants.SongSortType
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.toggleRepeatMode
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.plus
import javax.inject.Inject

@androidx.annotation.OptIn(UnstableApi::class)
class MediaLibrarySessionCallback
@Inject
constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val downloadUtil: DownloadUtil,
) : MediaLibrarySession.Callback {
    private val scope = CoroutineScope(Dispatchers.Main) + Job()
    lateinit var service: MusicService
    var toggleLike: () -> Unit = {}
    var toggleStartRadio: () -> Unit = {}
    var toggleLibrary: () -> Unit = {}

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        return MediaSession.ConnectionResult.accept(
            connectionResult.availableSessionCommands
                .buildUpon()
                .add(MediaSessionConstants.CommandToggleLike)
                .add(MediaSessionConstants.CommandToggleStartRadio)
                .add(MediaSessionConstants.CommandToggleLibrary)
                .add(MediaSessionConstants.CommandToggleShuffle)
                .add(MediaSessionConstants.CommandToggleRepeatMode)
                .build(),
            connectionResult.availablePlayerCommands,
        )
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            MediaSessionConstants.ACTION_TOGGLE_LIKE -> toggleLike()
            MediaSessionConstants.ACTION_TOGGLE_START_RADIO -> toggleStartRadio()
            MediaSessionConstants.ACTION_TOGGLE_LIBRARY -> toggleLibrary()
            MediaSessionConstants.ACTION_TOGGLE_SHUFFLE -> session.player.shuffleModeEnabled =
                !session.player.shuffleModeEnabled

            MediaSessionConstants.ACTION_TOGGLE_REPEAT_MODE -> session.player.toggleRepeatMode()
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    @Deprecated("Deprecated in MediaLibrarySession.Callback")
    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaItemsWithStartPosition> {
        return SettableFuture.create<MediaItemsWithStartPosition>()
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> =
        Futures.immediateFuture(
            LibraryResult.ofItem(
                rootMediaItem(),
                params.withContentStyleHints(),
            ),
        )

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
        scope.future(Dispatchers.IO) {
            val children =
                when (parentId) {
                    MusicService.ROOT -> rootChildren()

                    MusicService.SONG -> database.songsByCreateDateAsc().first()
                        .map { it.toMediaItem(parentId) }

                    MusicService.ARTIST ->
                        database.artistsByCreateDateAsc().first().map { artist ->
                            browsableMediaItem(
                                "${MusicService.ARTIST}/${artist.id}",
                                artist.artist.name,
                                context.resources.getQuantityString(
                                    R.plurals.n_song,
                                    artist.songCount,
                                    artist.songCount
                                ),
                                artist.artist.thumbnailUrl?.toUri(),
                                MediaMetadata.MEDIA_TYPE_ARTIST,
                                singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
                            )
                        }

                    MusicService.ALBUM ->
                        database.albumsByCreateDateAsc().first().map { album ->
                            browsableMediaItem(
                                "${MusicService.ALBUM}/${album.id}",
                                album.album.title,
                                album.artists.joinToString {
                                    it.name
                                },
                                album.album.thumbnailUrl?.toUri(),
                                MediaMetadata.MEDIA_TYPE_ALBUM,
                                singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
                            )
                        }

                    MusicService.PLAYLIST -> {
                        val likedSongCount = database.likedSongsCount().first()
                        val downloadedSongCount = downloadUtil.downloads.value.size
                        val youtubePlaylists = try {
                            YouTube.home().getOrNull()?.sections
                                ?.flatMap { it.items }
                                ?.filterIsInstance<PlaylistItem>()
                                ?.take(10)
                                ?: emptyList()
                        } catch (e: Exception) {
                            reportException(e)
                            emptyList()
                        }

                        listOf(
                            likedSongsMediaItem(likedSongCount),
                            downloadedSongsMediaItem(downloadedSongCount),
                        ) +
                            database.playlistsByCreateDateAsc().first().map { playlist ->
                                playlist.toBrowsableMediaItem()
                            } +
                            youtubePlaylists.map { ytPlaylist ->
                                ytPlaylist.toBrowsableMediaItem()
                            }
                    }

                    else ->
                        when {
                            parentId.startsWith("${MusicService.ARTIST}/") ->
                                database.artistSongsByCreateDateAsc(parentId.removePrefix("${MusicService.ARTIST}/"))
                                    .first().map {
                                        it.toMediaItem(parentId)
                                    }

                            parentId.startsWith("${MusicService.ALBUM}/") ->
                                database.albumSongs(parentId.removePrefix("${MusicService.ALBUM}/"))
                                    .first().map {
                                        it.toMediaItem(parentId)
                                    }

                            parentId.startsWith("${MusicService.PLAYLIST}/") -> {
                                val playlistId = parentId.removePrefix("${MusicService.PLAYLIST}/")
                                val songs = when (playlistId) {
                                    PlaylistEntity.LIKED_PLAYLIST_ID -> database.likedSongs(
                                        SongSortType.CREATE_DATE,
                                        true
                                    )

                                    PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> {
                                        val downloads = downloadUtil.downloads.value
                                        database
                                            .allSongs()
                                            .flowOn(Dispatchers.IO)
                                            .map { songs ->
                                                songs.filter {
                                                    downloads[it.id]?.state == Download.STATE_COMPLETED
                                                }
                                            }.map { songs ->
                                                songs
                                                    .map { it to downloads[it.id] }
                                                    .sortedBy { it.second?.updateTimeMs ?: 0L }
                                                    .map { it.first }
                                            }
                                    }

                                    else ->
                                        database.playlistSongs(playlistId).map { list ->
                                            list.map { it.song }
                                        }
                                }.first()

                                listOf(shuffleMediaItem(parentId)) + songs.map { it.toMediaItem(parentId) }
                            }

                            parentId.startsWith("${MusicService.YOUTUBE_PLAYLIST}/") -> {
                                val playlistId = parentId.removePrefix("${MusicService.YOUTUBE_PLAYLIST}/")
                                try {
                                    val songs = YouTube.playlist(playlistId).getOrNull()?.songs
                                        ?.take(100)
                                        ?.filterExplicit(context.dataStore.get(HideExplicitKey, false))
                                        ?.filterVideoSongs(context.dataStore.get(HideVideoSongsKey, false))
                                        ?: emptyList()

                                    listOf(shuffleMediaItem(parentId)) + songs.map { it.toMediaItem(parentId) }
                                } catch (e: Exception) {
                                    reportException(e)
                                    emptyList()
                                }
                            }

                            else -> emptyList()
                        }
                }

            LibraryResult.ofItemList(
                children.paginate(page, pageSize),
                params.withContentStyleHints(),
            )
        }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> =
        scope.future(Dispatchers.IO) {
            getMediaItem(mediaId)?.let {
                LibraryResult.ofItem(it, null)
            } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
        }

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        session.notifySearchResultChanged(browser, query, 1, params)
        return Futures.immediateFuture(LibraryResult.ofVoid())
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return scope.future(Dispatchers.IO) {
            if (query.isEmpty()) {
                return@future LibraryResult.ofItemList(emptyList(), params.withContentStyleHints())
            }

            try {
                val searchResults = mutableListOf<MediaItem>()

                val localSongs = database.allSongs().first().filter { song ->
                    song.song.title.contains(query, ignoreCase = true) ||
                    song.artists.any { it.name.contains(query, ignoreCase = true) } ||
                    song.album?.title?.contains(query, ignoreCase = true) == true
                }
                
                val artistSongs = database.searchArtists(query).first().flatMap { artist ->
                    database.artistSongsByCreateDateAsc(artist.id).first()
                }
                
                val albumSongs = database.searchAlbums(query).first().flatMap { album ->
                    database.albumSongs(album.id).first()
                }
                
                val playlistSongs = database.searchPlaylists(query).first().flatMap { playlist ->
                    database.playlistSongs(playlist.id).first().map { it.song }
                }

                val allLocalSongs = (localSongs + artistSongs + albumSongs + playlistSongs)
                    .distinctBy { it.id }
                
                allLocalSongs.forEach { song ->
                    searchResults.add(song.toMediaItem(
                        path = "${MusicService.SEARCH}/$query",
                        isPlayable = true,
                        isBrowsable = false
                    ))
                }

                try {
                    val onlineResults = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                        .getOrNull()
                        ?.items
                        ?.filterIsInstance<SongItem>()
                        ?.filterExplicit(context.dataStore.get(HideExplicitKey, false))
                        ?.filterVideoSongs(context.dataStore.get(HideVideoSongsKey, false))
                        ?.filter { onlineSong ->
                            !allLocalSongs.any { localSong ->
                                localSong.id == onlineSong.id ||
                                (localSong.song.title.equals(onlineSong.title, ignoreCase = true) &&
                                 localSong.artists.any { artist ->
                                     onlineSong.artists.any {
                                         it.name.equals(artist.name, ignoreCase = true)
                                     }
                                 })
                            }
                        } ?: emptyList()

                    onlineResults.forEach { songItem ->
                        try {
                            database.query { insert(songItem.toMediaMetadata()) }
                        } catch (e: Exception) {
                        }
                        
                        searchResults.add(songItem.toMediaItem("${MusicService.SEARCH}/$query"))
                    }
                } catch (e: Exception) {
                    reportException(e)
                }
                
                LibraryResult.ofItemList(searchResults.paginate(page, pageSize), params.withContentStyleHints())
                
            } catch (e: Exception) {
                reportException(e)
                LibraryResult.ofItemList(emptyList(), params.withContentStyleHints())
            }
        }
    }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<MediaItemsWithStartPosition> =
        scope.future {
            val defaultResult = MediaItemsWithStartPosition(emptyList(), startIndex, startPositionMs)
            val path = mediaItems.firstOrNull()?.mediaId?.split("/")
                ?: return@future defaultResult

            when (path.firstOrNull()) {
                MusicService.SONG -> {
                    val songId = path.getOrNull(1) ?: return@future defaultResult
                    val allSongs = database.songsByCreateDateAsc().first()
                    MediaItemsWithStartPosition(
                        allSongs.map { it.toMediaItem() },
                        allSongs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
                        startPositionMs
                    )
                }

                MusicService.ARTIST -> {
                    val songId = path.getOrNull(2) ?: return@future defaultResult
                    val artistId = path.getOrNull(1) ?: return@future defaultResult
                    val songs = database.artistSongsByCreateDateAsc(artistId).first()
                    MediaItemsWithStartPosition(
                        songs.map { it.toMediaItem() },
                        songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
                        startPositionMs
                    )
                }

                MusicService.ALBUM -> {
                    val songId = path.getOrNull(2) ?: return@future defaultResult
                    val albumId = path.getOrNull(1) ?: return@future defaultResult
                    val albumWithSongs = database.albumWithSongs(albumId).first() ?: return@future defaultResult
                    MediaItemsWithStartPosition(
                        albumWithSongs.songs.map { it.toMediaItem() },
                        albumWithSongs.songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
                        startPositionMs
                    )
                }

                MusicService.PLAYLIST -> {
                    val songId = path.getOrNull(2) ?: return@future defaultResult
                    val playlistId = path.getOrNull(1) ?: return@future defaultResult
                    val songs = when (playlistId) {
                        PlaylistEntity.LIKED_PLAYLIST_ID -> database.likedSongs(SongSortType.CREATE_DATE, descending = true)
                        PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> {
                            val downloads = downloadUtil.downloads.value
                            database
                                .allSongs()
                                .flowOn(Dispatchers.IO)
                                .map { songs ->
                                    songs.filter {
                                        downloads[it.id]?.state == Download.STATE_COMPLETED
                                    }
                                }.map { songs ->
                                    songs
                                        .map { it to downloads[it.id] }
                                        .sortedBy { it.second?.updateTimeMs ?: 0L }
                                        .map { it.first }
                                }
                        }
                        else -> database.playlistSongs(playlistId).map { list ->
                            list.map { it.song }
                        }
                    }.first()

                    // Check if this is a shuffle action
                    if (songId == MusicService.SHUFFLE_ACTION) {
                        MediaItemsWithStartPosition(
                            songs.shuffled().map { it.toMediaItem() },
                            0,
                            C.TIME_UNSET
                        )
                    } else {
                        MediaItemsWithStartPosition(
                            songs.map { it.toMediaItem() },
                            songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
                            startPositionMs
                        )
                    }
                }

                MusicService.YOUTUBE_PLAYLIST -> {
                    val songId = path.getOrNull(2) ?: return@future defaultResult
                    val playlistId = path.getOrNull(1) ?: return@future defaultResult

                    val songs = try {
                        YouTube.playlist(playlistId).getOrNull()?.songs?.map {
                            it.toMediaItem()
                        } ?: emptyList()
                    } catch (e: Exception) {
                        reportException(e)
                        return@future defaultResult
                    }

                    // Check if this is a shuffle action
                    if (songId == MusicService.SHUFFLE_ACTION) {
                        MediaItemsWithStartPosition(
                            songs.shuffled(),
                            0,
                            C.TIME_UNSET
                        )
                    } else {
                        MediaItemsWithStartPosition(
                            songs,
                            songs.indexOfFirst { it.mediaId.endsWith(songId) }.takeIf { it != -1 } ?: 0,
                            C.TIME_UNSET
                        )
                    }
                }

                MusicService.SEARCH -> {
                    val songId = path.lastOrNull() ?: return@future defaultResult
                    val searchQuery = mediaItems.firstOrNull()?.mediaId
                        ?.removePrefix("${MusicService.SEARCH}/")
                        ?.removeSuffix("/$songId")
                        ?.takeIf { it.isNotBlank() }
                        ?: return@future defaultResult
                    
                    val searchResults = mutableListOf<Song>()

                    val localSongs = database.allSongs().first().filter { song ->
                        song.song.title.contains(searchQuery, ignoreCase = true) ||
                        song.artists.any { it.name.contains(searchQuery, ignoreCase = true) } ||
                        song.album?.title?.contains(searchQuery, ignoreCase = true) == true
                    }
                    
                    val artistSongs = database.searchArtists(searchQuery).first().flatMap { artist ->
                        database.artistSongsByCreateDateAsc(artist.id).first()
                    }
                    
                    val albumSongs = database.searchAlbums(searchQuery).first().flatMap { album ->
                        database.albumSongs(album.id).first()
                    }
                    
                    val playlistSongs = database.searchPlaylists(searchQuery).first().flatMap { playlist ->
                        database.playlistSongs(playlist.id).first().map { it.song }
                    }

                    val allLocalSongs = (localSongs + artistSongs + albumSongs + playlistSongs)
                        .distinctBy { it.id }
                    
                    searchResults.addAll(allLocalSongs)
                    
                    try {
                        val onlineResults = YouTube.search(searchQuery, YouTube.SearchFilter.FILTER_SONG)
                            .getOrNull()
                            ?.items
                            ?.filterIsInstance<SongItem>()
                            ?.filterExplicit(context.dataStore.get(HideExplicitKey, false))
                            ?.filterVideoSongs(context.dataStore.get(HideVideoSongsKey, false))
                            ?.filter { onlineSong ->
                                !allLocalSongs.any { localSong ->
                                    localSong.id == onlineSong.id ||
                                    (localSong.song.title.equals(onlineSong.title, ignoreCase = true) &&
                                     localSong.artists.any { artist ->
                                         onlineSong.artists.any {
                                             it.name.equals(artist.name, ignoreCase = true)
                                         }
                                     })
                                }
                            } ?: emptyList()

                        onlineResults.forEach { songItem ->
                            try {
                                database.query { insert(songItem.toMediaMetadata()) }
                                database.song(songItem.id).first()?.let { newSong ->
                                    searchResults.add(newSong)
                                }
                            } catch (e: Exception) {
                            }
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                    
                    if (searchResults.isEmpty()) {
                        return@future defaultResult
                    }
                    
                    val targetIndex = searchResults.indexOfFirst { it.id == songId }
                    
                    MediaItemsWithStartPosition(
                        searchResults.map { it.toMediaItem() },
                        if (targetIndex >= 0) targetIndex else 0,
                        C.TIME_UNSET
                    )
                }

                else -> defaultResult
            }
        }

    private fun drawableUri(
        @DrawableRes id: Int,
    ) = Uri
        .Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(context.resources.getResourcePackageName(id))
        .appendPath(context.resources.getResourceTypeName(id))
        .appendPath(context.resources.getResourceEntryName(id))
        .build()

    private fun rootMediaItem() = MediaItem
        .Builder()
        .setMediaId(MusicService.ROOT)
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(context.getString(R.string.app_name))
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .build(),
        ).build()

    private fun rootChildren() = listOf(
        browsableMediaItem(
            MusicService.SONG,
            context.getString(R.string.songs),
            null,
            drawableUri(R.drawable.music_note),
            MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
            singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM,
        ),
        browsableMediaItem(
            MusicService.ARTIST,
            context.getString(R.string.artists),
            null,
            drawableUri(R.drawable.artist),
            MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
            singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM,
        ),
        browsableMediaItem(
            MusicService.ALBUM,
            context.getString(R.string.albums),
            null,
            drawableUri(R.drawable.album),
            MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
            singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM,
        ),
        browsableMediaItem(
            MusicService.PLAYLIST,
            context.getString(R.string.playlists),
            null,
            drawableUri(R.drawable.queue_music),
            MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
            singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM,
        ),
    )

    private suspend fun getMediaItem(mediaId: String): MediaItem? {
        val path = mediaId.split("/")
        val type = path.firstOrNull() ?: return null

        return when (type) {
            MusicService.ROOT -> rootMediaItem()

            MusicService.SONG -> {
                if (path.size == 1) {
                    rootChildren().first { it.mediaId == MusicService.SONG }
                } else {
                    database.song(path[1]).first()?.toMediaItem(MusicService.SONG)
                }
            }

            MusicService.ARTIST -> {
                val artistId = path.getOrNull(1) ?: return null
                if (path.size == 2) {
                    database.artist(artistId).first()?.let { artist ->
                        browsableMediaItem(
                            "${MusicService.ARTIST}/${artist.id}",
                            artist.artist.name,
                            context.resources.getQuantityString(
                                R.plurals.n_song,
                                artist.songCount,
                                artist.songCount
                            ),
                            artist.artist.thumbnailUrl?.toUri(),
                            MediaMetadata.MEDIA_TYPE_ARTIST,
                            singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
                        )
                    }
                } else {
                    val songId = path.lastOrNull() ?: return null
                    database.song(songId).first()?.toMediaItem("${MusicService.ARTIST}/$artistId")
                }
            }

            MusicService.ALBUM -> {
                val albumId = path.getOrNull(1) ?: return null
                if (path.size == 2) {
                    database.album(albumId).first()?.let { album ->
                        browsableMediaItem(
                            "${MusicService.ALBUM}/${album.id}",
                            album.album.title,
                            album.artists.joinToString { it.name },
                            album.album.thumbnailUrl?.toUri(),
                            MediaMetadata.MEDIA_TYPE_ALBUM,
                            singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
                        )
                    }
                } else {
                    val songId = path.lastOrNull() ?: return null
                    database.song(songId).first()?.toMediaItem("${MusicService.ALBUM}/$albumId")
                }
            }

            MusicService.PLAYLIST -> {
                val playlistId = path.getOrNull(1) ?: return null
                if (path.size == 2) {
                    playlistMediaItem(playlistId)
                } else {
                    val songId = path.lastOrNull() ?: return null
                    if (songId == MusicService.SHUFFLE_ACTION) {
                        shuffleMediaItem("${MusicService.PLAYLIST}/$playlistId")
                    } else {
                        database.song(songId).first()?.toMediaItem("${MusicService.PLAYLIST}/$playlistId")
                    }
                }
            }

            MusicService.YOUTUBE_PLAYLIST -> {
                val playlistId = path.getOrNull(1) ?: return null
                if (path.size == 2) {
                    youtubePlaylistMediaItem(playlistId)
                } else {
                    val songId = path.lastOrNull() ?: return null
                    if (songId == MusicService.SHUFFLE_ACTION) {
                        shuffleMediaItem("${MusicService.YOUTUBE_PLAYLIST}/$playlistId")
                    } else {
                        database.song(songId).first()?.toMediaItem("${MusicService.YOUTUBE_PLAYLIST}/$playlistId")
                            ?: youtubePlaylistSongMediaItem(playlistId, songId)
                    }
                }
            }

            MusicService.SEARCH -> {
                val songId = path.lastOrNull() ?: return null
                database.song(songId).first()?.toMediaItem(
                    mediaId.removeSuffix("/$songId")
                )
            }

            else -> database.song(mediaId).first()?.toMediaItem()
        }
    }

    private suspend fun playlistMediaItem(playlistId: String): MediaItem? =
        when (playlistId) {
            PlaylistEntity.LIKED_PLAYLIST_ID -> likedSongsMediaItem(database.likedSongsCount().first())
            PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> downloadedSongsMediaItem(downloadUtil.downloads.value.size)
            else -> database.playlist(playlistId).first()?.toBrowsableMediaItem()
        }

    private suspend fun youtubePlaylistMediaItem(playlistId: String): MediaItem =
        try {
            YouTube.playlist(playlistId).getOrNull()?.playlist?.toBrowsableMediaItem()
        } catch (e: Exception) {
            reportException(e)
            null
        } ?: browsableMediaItem(
            "${MusicService.YOUTUBE_PLAYLIST}/$playlistId",
            playlistId,
            "YouTube Music",
            null,
            MediaMetadata.MEDIA_TYPE_PLAYLIST,
            singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
        )

    private suspend fun youtubePlaylistSongMediaItem(
        playlistId: String,
        songId: String,
    ): MediaItem? =
        try {
            YouTube.playlist(playlistId).getOrNull()?.songs
                ?.firstOrNull { it.id == songId }
                ?.toMediaItem("${MusicService.YOUTUBE_PLAYLIST}/$playlistId")
        } catch (e: Exception) {
            reportException(e)
            null
        }

    private fun likedSongsMediaItem(songCount: Int) = browsableMediaItem(
        "${MusicService.PLAYLIST}/${PlaylistEntity.LIKED_PLAYLIST_ID}",
        context.getString(R.string.liked_songs),
        context.resources.getQuantityString(
            R.plurals.n_song,
            songCount,
            songCount
        ),
        drawableUri(R.drawable.favorite),
        MediaMetadata.MEDIA_TYPE_PLAYLIST,
        singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
    )

    private fun downloadedSongsMediaItem(songCount: Int) = browsableMediaItem(
        "${MusicService.PLAYLIST}/${PlaylistEntity.DOWNLOADED_PLAYLIST_ID}",
        context.getString(R.string.downloaded_songs),
        context.resources.getQuantityString(
            R.plurals.n_song,
            songCount,
            songCount
        ),
        drawableUri(R.drawable.download),
        MediaMetadata.MEDIA_TYPE_PLAYLIST,
        singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
    )

    private fun com.music.vivi.db.entities.Playlist.toBrowsableMediaItem() = browsableMediaItem(
        "${MusicService.PLAYLIST}/$id",
        playlist.name,
        context.resources.getQuantityString(
            R.plurals.n_song,
            songCount,
            songCount
        ),
        thumbnails.firstOrNull()?.toUri(),
        MediaMetadata.MEDIA_TYPE_PLAYLIST,
        singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
    )

    private fun PlaylistItem.toBrowsableMediaItem() = browsableMediaItem(
        "${MusicService.YOUTUBE_PLAYLIST}/$id",
        title,
        author?.name ?: "YouTube Music",
        thumbnail?.toUri(),
        MediaMetadata.MEDIA_TYPE_PLAYLIST,
        singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
    )

    private fun shuffleMediaItem(parentId: String) = MediaItem.Builder()
        .setMediaId("$parentId/${MusicService.SHUFFLE_ACTION}")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(context.getString(R.string.shuffle))
                .setArtworkUri(drawableUri(R.drawable.shuffle))
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setExtras(playableMediaItemExtras())
                .build()
        ).build()

    private fun browsableMediaItem(
        id: String,
        title: String,
        subtitle: String?,
        iconUri: Uri?,
        mediaType: Int = MediaMetadata.MEDIA_TYPE_MUSIC,
        singleItemStyle: Int = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
    ) = MediaItem
        .Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtist(subtitle)
                .setArtworkUri(iconUri)
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(mediaType)
                .setExtras(
                    contentStyleExtras(
                        browsableStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
                        playableStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
                        singleItemStyle = singleItemStyle,
                    )
                )
                .build(),
        ).build()

    private fun Song.toMediaItem(path: String, isPlayable: Boolean = true, isBrowsable: Boolean = false): MediaItem {
        val artworkUri = song.thumbnailUrl?.let {
            val snapshot = context.imageLoader.diskCache?.openSnapshot(it)
            if (snapshot != null) {
                snapshot.use { snapshot -> snapshot.data.toFile().toUri() }
            } else {
                it.toUri()
            }
        }

        return MediaItem
            .Builder()
            .setMediaId("$path/$id")
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(song.title)
                    .setSubtitle(artists.joinToString { it.name })
                    .setArtist(artists.joinToString { it.name })
                    .setArtworkUri(artworkUri)
                    .setIsPlayable(isPlayable)
                    .setIsBrowsable(isBrowsable)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setExtras(playableMediaItemExtras())
                    .build(),
            ).build()
    }

    private fun SongItem.toMediaItem(path: String) = MediaItem.Builder()
        .setMediaId("$path/$id")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(artists.joinToString(", ") { it.name })
                .setArtist(artists.joinToString(", ") { it.name })
                .setArtworkUri(thumbnail.toUri())
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setExtras(playableMediaItemExtras())
                .build()
        )
        .build()

    private fun MediaLibraryService.LibraryParams?.withContentStyleHints(): MediaLibraryService.LibraryParams {
        val extras = Bundle(this?.extras ?: Bundle()).apply {
            putInt(
                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
            )
            putInt(
                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
            )
        }

        return MediaLibraryService.LibraryParams.Builder()
            .setOffline(this?.isOffline ?: false)
            .setRecent(this?.isRecent ?: false)
            .setSuggested(this?.isSuggested ?: false)
            .setExtras(extras)
            .build()
    }

    private fun playableMediaItemExtras() = contentStyleExtras(
        singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
    )

    private fun contentStyleExtras(
        browsableStyle: Int? = null,
        playableStyle: Int? = null,
        singleItemStyle: Int? = null,
    ) = Bundle().apply {
        browsableStyle?.let {
            putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, it)
        }
        playableStyle?.let {
            putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, it)
        }
        singleItemStyle?.let {
            putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_SINGLE_ITEM, it)
        }
    }

    private fun <T> List<T>.paginate(page: Int, pageSize: Int): List<T> {
        if (page < 0 || pageSize < 1 || isEmpty()) return emptyList()
        if (pageSize == Int.MAX_VALUE) return this

        val fromIndex = page.toLong() * pageSize
        if (fromIndex >= size) return emptyList()

        return subList(fromIndex.toInt(), minOf(fromIndex.toInt() + pageSize, size))
    }
}
