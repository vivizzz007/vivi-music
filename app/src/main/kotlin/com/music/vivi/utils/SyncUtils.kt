package com.music.vivi.utils

import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.utils.completed
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.ArtistEntity
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.utils.get
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Manages synchronization between the local database and the remote YouTube Music library.
 *
 * This utility handles bidirectional syncing for:
 * - Liked Songs ("LM" playlist)
 * - Library Songs (Liked videos/In-Library tracks)
 * - Uploaded Songs and Albums
 * - Saved Playlists and Subscribed Artists
 *
 * It uses a "last-write-wins" or "merge" strategy depending on the content type, ensuring that
 * local changes (like offline likes) are pushed to the server and remote changes are pulled down.
 *
 * ## Usage Usually injected via Hilt and called from a Worker or ViewModel:
 * ```kotlin
 * syncUtils.runAllSyncs() // Triggers a full sync
 * ```
 */
@Singleton
public class SyncUtils @Inject constructor(private val database: MusicDatabase) {
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private val isSyncingLikedSongs = MutableStateFlow(false)
    private val isSyncingLibrarySongs = MutableStateFlow(false)
    private val isSyncingUploadedSongs = MutableStateFlow(false)
    private val isSyncingLikedAlbums = MutableStateFlow(false)
    private val isSyncingUploadedAlbums = MutableStateFlow(false)
    private val isSyncingArtists = MutableStateFlow(false)
    private val isSyncingPlaylists = MutableStateFlow(false)

    /**
     * Triggers a full synchronization of all supported content types. This runs asynchronously in
     * the [syncScope] (Dispatchers.IO).
     *
     * @param isFastSync If true, only fetches the first page of results (approx 100 items) for
     * each category to provide instant UI feedback and save bandwidth.
     */
    public fun runAllSyncs(isFastSync: Boolean = false) {
        syncScope.launch {
            launch { syncLikedSongs(isFastSync) }
            launch { syncLibrarySongs(isFastSync) }
            launch { syncUploadedSongs(isFastSync) }
            launch { syncLikedAlbums(isFastSync) }
            launch { syncUploadedAlbums(isFastSync) }
            launch { syncArtistsSubscriptions(isFastSync) }
            launch { syncSavedPlaylists(isFastSync) }
        }
    }

    public fun likeSong(s: SongEntity) {
        syncScope.launch { YouTube.likeVideo(s.id, s.liked) }
    }

    /**
     * Synchronizes "Liked Songs" (Playlist ID: "LM").
     *
     * Logic:
     * 1. Fetches the remote "Liked Songs" playlist.
     * 2. **Push**: Pushes any local pending likes (songs liked offline) to YouTube.
     * 3. **Pull**: Updates the local database with remote likes, including the liked timestamp.
     *
     * Thread-safe: skips execution if a sync is already in progress.
     */
    public suspend fun syncLikedSongs(isFastSync: Boolean = false) {
        if (isSyncingLikedSongs.value) return
        isSyncingLikedSongs.value = true
        try {
            val result = YouTube.playlist("LM").let { if (isFastSync) it else it.completed() }
            result.onSuccess { page ->
                val remoteSongs = page.songs
                val remoteIds = remoteSongs.map { it.id }.toSet()
                val localSongs = database.likedSongsByNameAsc().first()

                // Only reconcile (unlike local) if we have the full remote list
                if (!isFastSync && page.songsContinuation == null) {
                    localSongs.filterNot { it.id in remoteIds }.forEach {
                        try {
                            database.transaction { update(it.song.localToggleLike()) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                remoteSongs.forEachIndexed { index, song ->
                    try {
                        val dbSong = database.song(song.id).firstOrNull()
                        val timestamp = LocalDateTime.now().minusSeconds(index.toLong())
                        val isVideoSong = song.isVideoSong
                        database.transaction {
                            if (dbSong == null) {
                                insert(song.toMediaMetadata()) {
                                    it.copy(
                                            liked = true,
                                            likedDate = timestamp,
                                            isVideo = isVideoSong
                                    )
                                }
                            } else if (!dbSong.song.liked ||
                                            dbSong.song.likedDate != timestamp ||
                                            dbSong.song.isVideo != isVideoSong
                            ) {
                                update(
                                        dbSong.song.copy(
                                                liked = true,
                                                likedDate = timestamp,
                                                isVideo = isVideoSong
                                        )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingLikedSongs.value = false
        }
    }

    public suspend fun syncLibrarySongs(isFastSync: Boolean = false) {
        if (isSyncingLibrarySongs.value) return
        isSyncingLibrarySongs.value = true
        try {
            val result = YouTube.library("FEmusic_liked_videos").let { if (isFastSync) it else it.completed() }
            result.onSuccess { page ->
                val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
                val remoteIds = remoteSongs.map { it.id }.toSet()
                val localSongs = database.songsByNameAsc().first()
                val feedbackTokens = mutableListOf<String>()

                // Only reconcile (remove from library local) if we have the full remote list
                if (!isFastSync && page.continuation == null) {
                    localSongs.filterNot { it.id in remoteIds }.forEach {
                        if (it.song.libraryAddToken != null && it.song.libraryRemoveToken != null) {
                            feedbackTokens.add(it.song.libraryAddToken)
                        } else {
                            try {
                                database.transaction { update(it.song.toggleLibrary()) }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                feedbackTokens.chunked(20).forEach { YouTube.feedback(it) }

                remoteSongs.forEach { song ->
                    try {
                        val dbSong = database.song(song.id).firstOrNull()
                        database.transaction {
                            if (dbSong == null) {
                                insert(song.toMediaMetadata()) { it.toggleLibrary() }
                            } else {
                                if (dbSong.song.inLibrary == null) {
                                    update(dbSong.song.toggleLibrary())
                                }
                                addLibraryTokens(
                                        song.id,
                                        song.libraryAddToken,
                                        song.libraryRemoveToken
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingLibrarySongs.value = false
        }
    }

    public suspend fun syncUploadedSongs(isFastSync: Boolean = false) {
        if (isSyncingUploadedSongs.value) return
        isSyncingUploadedSongs.value = true
        try {
            YouTube.library("FEmusic_library_privately_owned_tracks", tabIndex = 1)
                    .let { if (isFastSync) it else it.completed() }
                    .onSuccess { page ->
                        val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
                        val remoteIds = remoteSongs.map { it.id }.toSet()
                        val localSongs = database.uploadedSongsByNameAsc().first()

                        if (!isFastSync && page.continuation == null) {
                            localSongs.filterNot { it.id in remoteIds }.forEach {
                                database.update(it.song.toggleUploaded())
                            }
                        }

                        remoteSongs.forEach { song ->
                            val dbSong = database.song(song.id).firstOrNull()
                            database.transaction {
                                if (dbSong == null) {
                                    insert(song.toMediaMetadata()) { it.toggleUploaded() }
                                } else if (!dbSong.song.isUploaded) {
                                    update(dbSong.song.toggleUploaded())
                                }
                            }
                        }
                    }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingUploadedSongs.value = false
        }
    }

    public suspend fun syncLikedAlbums(isFastSync: Boolean = false) {
        if (isSyncingLikedAlbums.value) return
        isSyncingLikedAlbums.value = true
        try {
            YouTube.library("FEmusic_liked_albums").let { if (isFastSync) it else it.completed() }.onSuccess { page ->
                val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
                val remoteIds = remoteAlbums.map { it.id }.toSet()
                val localAlbums = database.albumsLikedByNameAsc().first()

                if (!isFastSync && page.continuation == null) {
                    localAlbums.filterNot { it.id in remoteIds }.forEach {
                        database.update(it.album.localToggleLike())
                    }
                }

                remoteAlbums.forEach { album ->
                    val dbAlbum = database.album(album.id).firstOrNull()
                    if (dbAlbum == null) {
                        // Insert minimal album entity from library metadata
                        database.insert(
                            com.music.vivi.db.entities.AlbumEntity(
                                id = album.id,
                                title = album.title,
                                thumbnailUrl = album.thumbnail,
                                year = album.year,
                                songCount = 0, // Will be updated when opened
                                duration = 0,  // Will be updated when opened
                                bookmarkedAt = LocalDateTime.now(),
                                likedDate = LocalDateTime.now()
                            )
                        )
                    } else if (dbAlbum.album.bookmarkedAt == null) {
                        database.update(dbAlbum.album.localToggleLike())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingLikedAlbums.value = false
        }
    }

    public suspend fun syncUploadedAlbums(isFastSync: Boolean = false) {
        if (isSyncingUploadedAlbums.value) return
        isSyncingUploadedAlbums.value = true
        try {
            YouTube.library("FEmusic_library_privately_owned_releases", tabIndex = 1)
                    .let { if (isFastSync) it else it.completed() }
                    .onSuccess { page ->
                        val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
                        val remoteIds = remoteAlbums.map { it.id }.toSet()
                        val localAlbums = database.albumsUploadedByNameAsc().first()

                        if (!isFastSync && page.continuation == null) {
                            localAlbums.filterNot { it.id in remoteIds }.forEach {
                                database.update(it.album.toggleUploaded())
                            }
                        }

                        remoteAlbums.forEach { album ->
                            val dbAlbum = database.album(album.id).firstOrNull()
                            if (dbAlbum == null) {
                                database.insert(
                                    com.music.vivi.db.entities.AlbumEntity(
                                        id = album.id,
                                        title = album.title,
                                        thumbnailUrl = album.thumbnail,
                                        year = album.year,
                                        songCount = 0,
                                        duration = 0,
                                        isUploaded = true
                                    )
                                )
                            } else if (!dbAlbum.album.isUploaded) {
                                database.update(dbAlbum.album.toggleUploaded())
                            }
                        }
                    }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingUploadedAlbums.value = false
        }
    }

    public suspend fun syncArtistsSubscriptions(isFastSync: Boolean = false) {
        if (isSyncingArtists.value) return
        isSyncingArtists.value = true
        try {
            YouTube.library("FEmusic_library_corpus_artists").let { if (isFastSync) it else it.completed() }.onSuccess { page ->
                val remoteArtists = page.items.filterIsInstance<ArtistItem>()
                val remoteIds = remoteArtists.map { it.id }.toSet()
                val localArtists = database.artistsBookmarkedByNameAsc().first()

                if (!isFastSync && page.continuation == null) {
                    localArtists.filterNot { it.id in remoteIds }.forEach {
                        database.update(it.artist.localToggleLike())
                    }
                }

                remoteArtists.forEach { artist ->
                    val dbArtist = database.artist(artist.id).firstOrNull()
                    database.transaction {
                        if (dbArtist == null) {
                            insert(
                                    ArtistEntity(
                                            id = artist.id,
                                            name = artist.title,
                                            thumbnailUrl = artist.thumbnail,
                                            channelId = artist.channelId,
                                            bookmarkedAt = LocalDateTime.now()
                                    )
                            )
                        } else if (dbArtist.artist.bookmarkedAt == null) {
                            update(dbArtist.artist.localToggleLike())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingArtists.value = false
        }
    }

    /**
     * Synchronizes saved playlists.
     *
     * Fetches the list of playlists from the user's library and updates the local [PlaylistEntity]
     * entries. New playlists are inserted, and existing ones are updated with new metadata (title,
     * thumbnail, song count).
     */
    public suspend fun syncSavedPlaylists(isFastSync: Boolean = false) {
        if (isSyncingPlaylists.value) return
        isSyncingPlaylists.value = true
        try {
            coroutineScope {
                val likedJob = async { YouTube.library("FEmusic_liked_playlists").let { if (isFastSync) it else it.completed() } }
                val createdJob = async { YouTube.library("FEmusic_library_privately_owned_playlists").let { if (isFastSync) it else it.completed() } }
                val corpusJob = async { YouTube.library("FEmusic_library_corpus_playlists").let { if (isFastSync) it else it.completed() } }

                val likedResult = likedJob.await()
                val createdResult = createdJob.await()
                val corpusResult = corpusJob.await()

                val likedPage = likedResult.getOrNull()
                val createdPage = createdResult.getOrNull()
                val corpusPage = corpusResult.getOrNull()

                val remotePlaylists = (likedPage?.items.orEmpty() + createdPage?.items.orEmpty() + corpusPage?.items.orEmpty())
                    .filterIsInstance<PlaylistItem>()
                    .filterNot {
                        it.id == "LM" || it.id == "SE"
                    }
                    .distinctBy { it.id }
                    .reversed()

            val remoteIds = remotePlaylists.map { it.id }.toSet()
            val localPlaylists = database.playlistsByNameAsc().first()

            if (!isFastSync && likedPage?.continuation == null && createdPage?.continuation == null && corpusPage?.continuation == null) {
                localPlaylists
                        .filterNot { it.playlist.browseId in remoteIds }
                        .filterNot { it.playlist.browseId == null }
                        .forEach { database.update(it.playlist.localToggleLike()) }
            }

            remotePlaylists.forEach { playlist ->
                var playlistEntity =
                        localPlaylists.find { it.playlist.browseId == playlist.id }?.playlist
                if (playlistEntity == null) {
                    playlistEntity =
                            PlaylistEntity(
                                    name = playlist.title,
                                    browseId = playlist.id,
                                    thumbnailUrl = playlist.thumbnail,
                                    isEditable = playlist.isEditable,
                                    bookmarkedAt = LocalDateTime.now(),
                                    remoteSongCount =
                                            playlist.songCountText?.let {
                                                Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                            },
                                    playEndpointParams = playlist.playEndpoint?.params,
                                    shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                    radioEndpointParams = playlist.radioEndpoint?.params
                            )
                    database.insert(playlistEntity)
                } else {
                    database.update(playlistEntity, playlist)
                }
                syncPlaylist(playlist.id, playlistEntity.id, isFastSync)
            }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncingPlaylists.value = false
        }
    }

    private suspend fun syncPlaylist(browseId: String, playlistId: String, isFastSync: Boolean = false) {
        try {
            val result = YouTube.playlist(browseId).let { if (isFastSync) it else it.completed() }
            result.onSuccess { page ->
                val songs = page.songs.map(SongItem::toMediaMetadata)
                val remoteIds = songs.map { it.id }
                val localIds =
                        database.playlistSongs(playlistId)
                                .first()
                                .sortedBy { it.map.position }
                                .map { it.song.id }

                if (remoteIds == localIds) return@onSuccess
                if (database.playlist(playlistId).firstOrNull() == null) return@onSuccess

                database.transaction {
                    clearPlaylist(playlistId)
                    val songEntities =
                            songs.onEach { song ->
                                if (runBlocking { database.song(song.id).firstOrNull() } == null) {
                                    insert(song)
                                }
                            }
                    val playlistSongMaps =
                            songEntities.mapIndexed { position, song ->
                                PlaylistSongMap(
                                        songId = song.id,
                                        playlistId = playlistId,
                                        position = position,
                                        setVideoId = song.setVideoId
                                )
                            }
                    playlistSongMaps.forEach { insert(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public suspend fun clearAllSyncedContent() {
        try {
            val likedSongs = database.likedSongsByNameAsc().first()
            val librarySongs = database.songsByNameAsc().first()
            val likedAlbums = database.albumsLikedByNameAsc().first()
            val subscribedArtists = database.artistsBookmarkedByNameAsc().first()
            val savedPlaylists = database.playlistsByNameAsc().first()

            likedSongs.forEach {
                try {
                    database.transaction { update(it.song.copy(liked = false, likedDate = null)) }
                } catch (e: Exception,) {
                    e.printStackTrace()
                }
            }
            librarySongs.forEach {
                if (it.song.inLibrary != null) {
                    try {
                        database.transaction { update(it.song.copy(inLibrary = null)) }
                    } catch (e: Exception,) {
                        e.printStackTrace()
                    }
                }
            }
            likedAlbums.forEach {
                try {
                    database.transaction { update(it.album.copy(bookmarkedAt = null)) }
                } catch (e: Exception,) {
                    e.printStackTrace()
                }
            }
            subscribedArtists.forEach {
                try {
                    database.transaction { update(it.artist.copy(bookmarkedAt = null)) }
                } catch (e: Exception,) {
                    e.printStackTrace()
                }
            }
            savedPlaylists.forEach {
                if (it.playlist.browseId != null) {
                    try {
                        database.transaction { delete(it.playlist) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public suspend fun tryAutoSync() {
        runAllSyncs()
    }
}
