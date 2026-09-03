/**
 * vivimusic Project (C) 2026
 * OuterTune Project Copyright (C) 2025
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.utils.completed
import com.music.innertube.utils.parseCookieString
import com.music.lastfm.LastFM
import com.music.spotify.Spotify
import com.music.spotify.SpotifyAuth
import com.music.spotify.SpotifyMapper
import com.music.spotify.models.SpotifyTrack
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.LastFMUseSendLikes
import com.music.vivi.constants.LastFullSyncKey
import com.music.vivi.constants.SYNC_COOLDOWN
import com.music.vivi.constants.SpotifyAutoSyncKey
import com.music.vivi.constants.SpotifySessionKey
import com.music.vivi.viewmodels.SpotifySession
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.ArtistEntity
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.extensions.collectLatest
import com.music.vivi.extensions.isInternetConnected
import com.music.vivi.extensions.isSyncEnabled
import com.music.vivi.models.toMediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import android.widget.Toast
import com.music.vivi.R
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncOperation {
    data object FullSync : SyncOperation()
    data object LikedSongs : SyncOperation()
    data object LibrarySongs : SyncOperation()
    data object UploadedSongs : SyncOperation()
    data object LikedAlbums : SyncOperation()
    data object UploadedAlbums : SyncOperation()
    data object ArtistsSubscriptions : SyncOperation()
    data object SavedPlaylists : SyncOperation()
    data object AutoSyncPlaylists : SyncOperation()
    data object AutoSyncSpotifyPlaylists : SyncOperation()
    data class SinglePlaylist(val browseId: String, val playlistId: String) : SyncOperation()
    data class SingleSpotifyPlaylist(val playlistId: String) : SyncOperation()
    data class LikeSong(val song: SongEntity) : SyncOperation()
    data object CleanupDuplicates : SyncOperation()
    data object ClearAllSynced : SyncOperation()
}

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data class Error(val message: String) : SyncStatus()
    data object Completed : SyncStatus()
}

data class SyncState(
    val overallStatus: SyncStatus = SyncStatus.Idle,
    val likedSongs: SyncStatus = SyncStatus.Idle,
    val librarySongs: SyncStatus = SyncStatus.Idle,
    val uploadedSongs: SyncStatus = SyncStatus.Idle,
    val likedAlbums: SyncStatus = SyncStatus.Idle,
    val uploadedAlbums: SyncStatus = SyncStatus.Idle,
    val artists: SyncStatus = SyncStatus.Idle,
    val playlists: SyncStatus = SyncStatus.Idle,
    val currentOperation: String = ""
)

@Singleton
class SyncUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable !is CancellationException) {
            Timber.e(throwable, "Sync coroutine exception")
        }
    }

    private val syncJob = SupervisorJob()
    private val syncScope = CoroutineScope(Dispatchers.IO + syncJob + exceptionHandler)

    private val syncChannel = Channel<SyncOperation>(Channel.BUFFERED)
    private var processingJob: Job? = null

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var lastfmSendLikes = false

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val DB_OPERATION_DELAY_MS = 50L
    }

    init {
        context.dataStore.data
            .map { it[LastFMUseSendLikes] ?: false }
            .distinctUntilChanged()
            .collectLatest(syncScope) {
                lastfmSendLikes = it
            }

        startProcessingQueue()
    }

    private fun startProcessingQueue() {
        processingJob = syncScope.launch {
            for (operation in syncChannel) {
                try {
                    processOperation(operation)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Error processing sync operation: $operation")
                }
            }
        }
    }

    private suspend fun processOperation(operation: SyncOperation) {
        when (operation) {
            is SyncOperation.FullSync -> executeFullSync()
            is SyncOperation.LikedSongs -> executeSyncLikedSongs()
            is SyncOperation.LibrarySongs -> executeSyncLibrarySongs()
            is SyncOperation.UploadedSongs -> executeSyncUploadedSongs()
            is SyncOperation.LikedAlbums -> executeSyncLikedAlbums()
            is SyncOperation.UploadedAlbums -> executeSyncUploadedAlbums()
            is SyncOperation.ArtistsSubscriptions -> executeSyncArtistsSubscriptions()
            is SyncOperation.SavedPlaylists -> executeSyncSavedPlaylists()
            is SyncOperation.AutoSyncPlaylists -> executeSyncAutoSyncPlaylists()
            is SyncOperation.AutoSyncSpotifyPlaylists -> executeSyncSpotifyPlaylists()
            is SyncOperation.SinglePlaylist -> executeSyncPlaylist(operation.browseId, operation.playlistId)
            is SyncOperation.SingleSpotifyPlaylist -> executeSyncSingleSpotifyPlaylist(operation.playlistId)
            is SyncOperation.LikeSong -> executeLikeSong(operation.song)
            is SyncOperation.CleanupDuplicates -> executeCleanupDuplicatePlaylists()
            is SyncOperation.ClearAllSynced -> executeClearAllSyncedContent()
        }
    }

    private suspend fun isLoggedIn(): Boolean {
        return try {
            val cookie = context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .first()
            cookie?.let { "SAPISID" in parseCookieString(it) } ?: false
        } catch (e: Exception) {
            Timber.e(e, "Error checking login status")
            false
        }
    }

    private suspend fun <T> withRetry(
        maxRetries: Int = MAX_RETRIES,
        initialDelay: Long = INITIAL_RETRY_DELAY_MS,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelay
        repeat(maxRetries) { attempt ->
            try {
                return Result.success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Attempt ${attempt + 1}/$maxRetries failed")
                if (attempt == maxRetries - 1) {
                    return Result.failure(e)
                }
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        return Result.failure(Exception("Max retries exceeded"))
    }

    private fun updateState(update: SyncState.() -> SyncState) {
        _syncState.value = _syncState.value.update()
    }

    // Public API methods - Queue operations

    fun performFullSync() {
        syncScope.launch {
            syncChannel.send(SyncOperation.FullSync)
        }
    }

    suspend fun performFullSyncSuspend() {
        if (!isLoggedIn()) {
            Timber.w("Skipping full sync - user not logged in")
            return
        }
        executeFullSync()
    }

    fun tryAutoSync() {
        syncScope.launch {
            if (context.isInternetConnected()) {
                val spotifyAutoSync = context.dataStore.get(SpotifyAutoSyncKey, true)
                if (spotifyAutoSync) {
                    val spotifySession = getSpotifySession()
                    if (spotifySession != null) {
                        syncChannel.send(SyncOperation.AutoSyncSpotifyPlaylists)
                    }
                }
            }

            if (!isLoggedIn()) {
                Timber.d("Skipping auto sync - user not logged in")
                return@launch
            }

            if (!context.isSyncEnabled() || !context.isInternetConnected()) {
                return@launch
            }

            val lastSync = context.dataStore.get(LastFullSyncKey, 0L)
            val currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            if (lastSync > 0 && (currentTime - lastSync) < SYNC_COOLDOWN) {
                return@launch
            }

            syncChannel.send(SyncOperation.FullSync)

            context.dataStore.edit { settings ->
                settings[LastFullSyncKey] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            }
        }
    }

    fun runAllSyncs() {
        performFullSync()
    }

    fun likeSong(s: SongEntity) {
        syncScope.launch {
            syncChannel.send(SyncOperation.LikeSong(s))
        }
    }

    fun syncLikedSongs() {
        syncScope.launch {
            syncChannel.send(SyncOperation.LikedSongs)
        }
    }

    fun syncLibrarySongs() {
        syncScope.launch {
            syncChannel.send(SyncOperation.LibrarySongs)
        }
    }

    fun syncUploadedSongs() {
        syncScope.launch {
            syncChannel.send(SyncOperation.UploadedSongs)
        }
    }

    fun syncLikedAlbums() {
        syncScope.launch {
            syncChannel.send(SyncOperation.LikedAlbums)
        }
    }

    fun syncUploadedAlbums() {
        syncScope.launch {
            syncChannel.send(SyncOperation.UploadedAlbums)
        }
    }

    fun syncArtistsSubscriptions() {
        syncScope.launch {
            syncChannel.send(SyncOperation.ArtistsSubscriptions)
        }
    }

    fun syncSavedPlaylists() {
        syncScope.launch {
            syncChannel.send(SyncOperation.SavedPlaylists)
        }
    }

    fun syncAutoSyncPlaylists() {
        syncScope.launch {
            syncChannel.send(SyncOperation.AutoSyncPlaylists)
        }
    }

    fun syncSpotifyPlaylists() {
        syncScope.launch {
            syncChannel.send(SyncOperation.AutoSyncSpotifyPlaylists)
        }
    }

    fun syncSpotifyPlaylist(playlistId: String) {
        syncScope.launch {
            syncChannel.send(SyncOperation.SingleSpotifyPlaylist(playlistId))
        }
    }

    suspend fun syncSpotifyPlaylistSuspend(playlistId: String) = executeSyncSingleSpotifyPlaylist(playlistId)

    fun syncPlaylist(browseId: String, playlistId: String) {
        syncScope.launch {
            executeSyncPlaylist(browseId, playlistId)
        }
    }

    fun syncAllAlbums() {
        syncScope.launch {
            syncChannel.send(SyncOperation.LikedAlbums)
            syncChannel.send(SyncOperation.UploadedAlbums)
        }
    }

    fun syncAllArtists() {
        syncScope.launch {
            syncChannel.send(SyncOperation.ArtistsSubscriptions)
        }
    }

    fun cleanupDuplicatePlaylists() {
        syncScope.launch {
            syncChannel.send(SyncOperation.CleanupDuplicates)
        }
    }

    fun clearAllSyncedContent() {
        syncScope.launch {
            syncChannel.send(SyncOperation.ClearAllSynced)
        }
    }

    // Suspend versions for direct calls

    suspend fun syncLikedSongsSuspend() = executeSyncLikedSongs()
    suspend fun syncLibrarySongsSuspend() = executeSyncLibrarySongs()
    suspend fun syncUploadedSongsSuspend() = executeSyncUploadedSongs()
    suspend fun syncLikedAlbumsSuspend() = executeSyncLikedAlbums()
    suspend fun syncUploadedAlbumsSuspend() = executeSyncUploadedAlbums()
    suspend fun syncArtistsSubscriptionsSuspend() = executeSyncArtistsSubscriptions()
    suspend fun syncSavedPlaylistsSuspend() = executeSyncSavedPlaylists()
    suspend fun syncAutoSyncPlaylistsSuspend() = executeSyncAutoSyncPlaylists()
    suspend fun cleanupDuplicatePlaylistsSuspend() = executeCleanupDuplicatePlaylists()
    suspend fun clearAllSyncedContentSuspend() = executeClearAllSyncedContent()

    suspend fun syncAllAlbumsSuspend() {
        executeSyncLikedAlbums()
        executeSyncUploadedAlbums()
    }

    suspend fun syncAllArtistsSuspend() {
        executeSyncArtistsSubscriptions()
    }

    // Private execution methods

    private suspend fun executeFullSync() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) {
            Timber.w("Skipping full sync - user not logged in")
            return@withContext
        }

        updateState { copy(overallStatus = SyncStatus.Syncing, currentOperation = "Starting full sync") }

        try {
            // Sync in sequence to avoid overwhelming the API and database
            executeSyncLikedSongs()
            delay(DB_OPERATION_DELAY_MS)

            executeSyncLibrarySongs()
            delay(DB_OPERATION_DELAY_MS)

            executeSyncUploadedSongs()
            delay(DB_OPERATION_DELAY_MS)

            executeSyncLikedAlbums()
            delay(DB_OPERATION_DELAY_MS)

            executeSyncUploadedAlbums()
            delay(DB_OPERATION_DELAY_MS)

            executeSyncArtistsSubscriptions()
            delay(DB_OPERATION_DELAY_MS)

            executeSyncSavedPlaylists()
            delay(DB_OPERATION_DELAY_MS)

            executeSyncAutoSyncPlaylists()

            updateState { copy(overallStatus = SyncStatus.Completed, currentOperation = "") }
            Timber.d("Full sync completed successfully")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Error during full sync")
            updateState { copy(overallStatus = SyncStatus.Error(e.message ?: "Unknown error"), currentOperation = "") }
        }
    }

    private suspend fun executeLikeSong(s: SongEntity) = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) {
            Timber.w("Skipping likeSong - user not logged in")
            return@withContext
        }

        withRetry {
            YouTube.likeVideo(s.id, s.liked)
        }.onFailure { e ->
            Timber.e(e, "Failed to like song on YouTube: ${s.id}")
        }

        if (lastfmSendLikes) {
            try {
                val dbSong = database.song(s.id).firstOrNull()
                LastFM.setLoveStatus(
                    artist = dbSong?.artists?.joinToString { a -> a.name } ?: "",
                    track = s.title,
                    love = s.liked
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to update LastFM love status")
            }
        }
    }

    private suspend fun executeSyncLikedSongs() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncLikedSongs - user not logged in")
            return@withContext
        }

        updateState { copy(likedSongs = SyncStatus.Syncing, currentOperation = "Syncing liked songs") }

        withRetry {
            YouTube.playlist("LM").completed()
        }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteSongs = page.songs
                    val remoteIds = remoteSongs.map { it.id }.toSet()
                    val localSongs = database.likedSongsByNameAsc().first()

                    // Remove likes from songs not in remote
                    localSongs.filterNot { it.id in remoteIds }.forEach { song ->
                        try {
                            database.update(song.song.localToggleLike())
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to update song: ${song.id}")
                        }
                    }

                    // Add/update songs from remote
                    val now = LocalDateTime.now()
                    remoteSongs.forEachIndexed { index, song ->
                        try {
                            val dbSong = database.song(song.id).firstOrNull()
                            val timestamp = now.minusSeconds(index.toLong())
                            val isVideoSong = song.isVideoSong

                            database.transaction {
                                if (dbSong == null) {
                                    insert(song.toMediaMetadata()) {
                                        it.copy(liked = true, likedDate = timestamp, isVideo = isVideoSong)
                                    }
                                } else if (!dbSong.song.liked || dbSong.song.likedDate != timestamp || dbSong.song.isVideo != isVideoSong) {
                                    update(dbSong.song.copy(liked = true, likedDate = timestamp, isVideo = isVideoSong))
                                }
                            }
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to process song: ${song.id}")
                        }
                    }

                    updateState { copy(likedSongs = SyncStatus.Completed) }
                    Timber.d("Synced ${remoteSongs.size} liked songs")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing liked songs")
                    updateState { copy(likedSongs = SyncStatus.Error(e.message ?: "Unknown error")) }
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to fetch liked songs from YouTube")
                updateState { copy(likedSongs = SyncStatus.Error(e.message ?: "Unknown error")) }
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to sync liked songs after retries")
            updateState { copy(likedSongs = SyncStatus.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun executeSyncLibrarySongs() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncLibrarySongs - user not logged in")
            return@withContext
        }

        updateState { copy(librarySongs = SyncStatus.Syncing, currentOperation = "Syncing library songs") }

        withRetry {
            YouTube.library("FEmusic_liked_videos").completed()
        }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
                    val remoteIds = remoteSongs.map { it.id }.toSet()
                    val localSongs = database.songsByNameAsc().first()

                    localSongs.filterNot { it.id in remoteIds }.forEach { song ->
                        try {
                            database.update(song.song.toggleLibrary())
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to update song: ${song.id}")
                        }
                    }

                    remoteSongs.forEach { song ->
                        try {
                            val dbSong = database.song(song.id).firstOrNull()
                            database.transaction {
                                if (dbSong == null) {
                                    insert(song.toMediaMetadata()) { it.toggleLibrary() }
                                } else if (dbSong.song.inLibrary == null) {
                                    update(dbSong.song.toggleLibrary())
                                }
                            }
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to process song: ${song.id}")
                        }
                    }

                    updateState { copy(librarySongs = SyncStatus.Completed) }
                    Timber.d("Synced ${remoteSongs.size} library songs")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing library songs")
                    updateState { copy(librarySongs = SyncStatus.Error(e.message ?: "Unknown error")) }
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to fetch library songs from YouTube")
                updateState { copy(librarySongs = SyncStatus.Error(e.message ?: "Unknown error")) }
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to sync library songs after retries")
            updateState { copy(librarySongs = SyncStatus.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun executeSyncUploadedSongs() = withContext(Dispatchers.IO) {
        Timber.d("[UPLOAD_DEBUG] executeSyncUploadedSongs() started")
        if (!isLoggedIn()) {
            Timber.w("[UPLOAD_DEBUG] Skipping syncUploadedSongs - user not logged in")
            return@withContext
        }
        Timber.d("[UPLOAD_DEBUG] User is logged in, proceeding with sync")

        updateState { copy(uploadedSongs = SyncStatus.Syncing, currentOperation = "Syncing uploaded songs") }

        withRetry {
            Timber.d("[UPLOAD_DEBUG] Calling YouTube.library(FEmusic_library_privately_owned_tracks, tabIndex=1)")
            // Uploaded songs are in Tab 1 ("Uploads"), not Tab 0 ("Library")
            YouTube.library("FEmusic_library_privately_owned_tracks", tabIndex = 1).completed()
        }.onSuccess { result ->
            Timber.d("[UPLOAD_DEBUG] withRetry succeeded, result isSuccess=${result.isSuccess}")
            result.onSuccess { page ->
                try {
                    Timber.d("[UPLOAD_DEBUG] Page received, total items: ${page.items.size}")
                    page.items.forEachIndexed { index, item ->
                        Timber.d("[UPLOAD_DEBUG] Page item $index: type=${item::class.simpleName}, id=${(item as? SongItem)?.id ?: "N/A"}")
                    }
                    val remoteSongs = page.items.filterIsInstance<SongItem>().reversed()
                    Timber.d("[UPLOAD_DEBUG] Filtered to ${remoteSongs.size} SongItems")
                    remoteSongs.forEachIndexed { index, song ->
                        Timber.d("[UPLOAD_DEBUG] Remote song $index: id=${song.id}, title=${song.title}, artists=${song.artists.map { it.name }}")
                    }
                    val remoteIds = remoteSongs.map { it.id }.toSet()
                    val localSongs = database.uploadedSongsByNameAsc().first()
                    Timber.d("[UPLOAD_DEBUG] Local uploaded songs count: ${localSongs.size}")

                    val songsToRemove = localSongs.filterNot { it.id in remoteIds }
                    Timber.d("[UPLOAD_DEBUG] Songs to remove from uploaded: ${songsToRemove.size}")
                    songsToRemove.forEach { song ->
                        try {
                            Timber.d("[UPLOAD_DEBUG] Removing uploaded flag from: ${song.id}")
                            database.update(song.song.toggleUploaded())
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "[UPLOAD_DEBUG] Failed to update song: ${song.id}")
                        }
                    }

                    remoteSongs.forEach { song ->
                        try {
                            val dbSong = database.song(song.id).firstOrNull()
                            Timber.d("[UPLOAD_DEBUG] Processing remote song ${song.id}: exists in db=${dbSong != null}, isUploaded=${dbSong?.song?.isUploaded}")
                            database.transaction {
                                if (dbSong == null) {
                                    Timber.d("[UPLOAD_DEBUG] Inserting new song: ${song.id}")
                                    insert(song.toMediaMetadata()) { it.toggleUploaded() }
                                } else if (!dbSong.song.isUploaded) {
                                    Timber.d("[UPLOAD_DEBUG] Updating existing song to uploaded: ${song.id}")
                                    update(dbSong.song.toggleUploaded())
                                } else {
                                    Timber.d("[UPLOAD_DEBUG] Song already marked as uploaded: ${song.id}")
                                }
                            }
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "[UPLOAD_DEBUG] Failed to process song: ${song.id}")
                        }
                    }

                    updateState { copy(uploadedSongs = SyncStatus.Completed) }
                    Timber.d("[UPLOAD_DEBUG] Synced ${remoteSongs.size} uploaded songs successfully")
                } catch (e: Exception) {
                    Timber.e(e, "[UPLOAD_DEBUG] Error processing uploaded songs")
                    updateState { copy(uploadedSongs = SyncStatus.Error(e.message ?: "Unknown error")) }
                }
            }.onFailure { e ->
                Timber.e(e, "[UPLOAD_DEBUG] Failed to fetch uploaded songs from YouTube")
                updateState { copy(uploadedSongs = SyncStatus.Error(e.message ?: "Unknown error")) }
            }
        }.onFailure { e ->
            Timber.e(e, "[UPLOAD_DEBUG] Failed to sync uploaded songs after retries")
            updateState { copy(uploadedSongs = SyncStatus.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun executeSyncLikedAlbums() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncLikedAlbums - user not logged in")
            return@withContext
        }

        updateState { copy(likedAlbums = SyncStatus.Syncing, currentOperation = "Syncing liked albums") }

        withRetry {
            YouTube.library("FEmusic_liked_albums").completed()
        }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
                    val remoteIds = remoteAlbums.map { it.id }.toSet()
                    val localAlbums = database.albumsLikedByNameAsc().first()

                    localAlbums.filterNot { it.id in remoteIds }.forEach { album ->
                        try {
                            database.update(album.album.localToggleLike())
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to update album: ${album.id}")
                        }
                    }

                    remoteAlbums.forEach { album ->
                        try {
                            val dbAlbum = database.album(album.id).firstOrNull()
                            if (dbAlbum == null) {
                                // New album — fetch full details and insert
                                YouTube.album(album.browseId).onSuccess { albumPage ->
                                    database.insert(albumPage)
                                    database.album(album.id).firstOrNull()?.let { newDbAlbum ->
                                        database.update(newDbAlbum.album.localToggleLike())
                                    }
                                }
                            } else if (dbAlbum.album.bookmarkedAt == null) {
                                // Already cached but not liked yet — just mark liked
                                database.update(dbAlbum.album.localToggleLike())
                            }
                            // else: already cached and liked — skip network call
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to process album: ${album.id}")
                        }
                    }

                    updateState { copy(likedAlbums = SyncStatus.Completed) }
                    Timber.d("Synced ${remoteAlbums.size} liked albums")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing liked albums")
                    updateState { copy(likedAlbums = SyncStatus.Error(e.message ?: "Unknown error")) }
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to fetch liked albums from YouTube")
                updateState { copy(likedAlbums = SyncStatus.Error(e.message ?: "Unknown error")) }
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to sync liked albums after retries")
            updateState { copy(likedAlbums = SyncStatus.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun executeSyncUploadedAlbums() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncUploadedAlbums - user not logged in")
            return@withContext
        }

        updateState { copy(uploadedAlbums = SyncStatus.Syncing, currentOperation = "Syncing uploaded albums") }

        withRetry {
            YouTube.library("FEmusic_library_privately_owned_releases").completed()
        }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteAlbums = page.items.filterIsInstance<AlbumItem>().reversed()
                    val remoteIds = remoteAlbums.map { it.id }.toSet()
                    val localAlbums = database.albumsUploadedByNameAsc().first()

                    localAlbums.filterNot { it.id in remoteIds }.forEach { album ->
                        try {
                            database.update(album.album.toggleUploaded())
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to update album: ${album.id}")
                        }
                    }

                    remoteAlbums.forEach { album ->
                        try {
                            val dbAlbum = database.album(album.id).firstOrNull()
                            if (dbAlbum == null) {
                                // New uploaded album — fetch full details and insert
                                YouTube.album(album.browseId).onSuccess { albumPage ->
                                    database.insert(albumPage)
                                    database.album(album.id).firstOrNull()?.let { newDbAlbum ->
                                        database.update(newDbAlbum.album.toggleUploaded())
                                    }
                                }.onFailure { reportException(it) }
                            } else if (!dbAlbum.album.isUploaded) {
                                // Already cached but not marked uploaded — just update flag
                                database.update(dbAlbum.album.toggleUploaded())
                            }
                            // else: already cached and marked uploaded — skip network call
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to process album: ${album.id}")
                        }
                    }

                    updateState { copy(uploadedAlbums = SyncStatus.Completed) }
                    Timber.d("Synced ${remoteAlbums.size} uploaded albums")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing uploaded albums")
                    updateState { copy(uploadedAlbums = SyncStatus.Error(e.message ?: "Unknown error")) }
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to fetch uploaded albums from YouTube")
                updateState { copy(uploadedAlbums = SyncStatus.Error(e.message ?: "Unknown error")) }
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to sync uploaded albums after retries")
            updateState { copy(uploadedAlbums = SyncStatus.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun executeSyncArtistsSubscriptions() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncArtistsSubscriptions - user not logged in")
            return@withContext
        }

        updateState { copy(artists = SyncStatus.Syncing, currentOperation = "Syncing artist subscriptions") }

        withRetry {
            YouTube.library("FEmusic_library_corpus_artists").completed()
        }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remoteArtists = page.items.filterIsInstance<ArtistItem>()
                    val remoteIds = remoteArtists.map { it.id }.toSet()
                    val localArtists = database.artistsBookmarkedByNameAsc().first()

                    localArtists.filterNot { it.id in remoteIds }.forEach { artist ->
                        try {
                            database.update(artist.artist.localToggleLike())
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to update artist: ${artist.id}")
                        }
                    }

                    remoteArtists.forEach { artist ->
                        try {
                            val dbArtist = database.artist(artist.id).firstOrNull()
                            // Use cached channelId first — only call YouTube.getChannelId() for new artists
                            val channelId = artist.channelId
                                ?: dbArtist?.artist?.channelId
                                ?: if (dbArtist == null && artist.id.startsWith("UC")) {
                                    try {
                                        YouTube.getChannelId(artist.id).takeIf { it.isNotEmpty() }
                                    } catch (e: Exception) {
                                        null
                                    }
                                } else null

                            database.transaction {
                                if (dbArtist == null) {
                                    insert(
                                        ArtistEntity(
                                            id = artist.id,
                                            name = artist.title,
                                            thumbnailUrl = artist.thumbnail,
                                            channelId = channelId,
                                            bookmarkedAt = LocalDateTime.now()
                                        )
                                    )
                                } else {
                                    val existing = dbArtist.artist
                                    val needsChannelIdUpdate = existing.channelId == null && channelId != null
                                    if (existing.bookmarkedAt == null || needsChannelIdUpdate ||
                                        existing.name != artist.title || existing.thumbnailUrl != artist.thumbnail) {
                                        update(
                                            existing.copy(
                                                name = artist.title,
                                                thumbnailUrl = artist.thumbnail,
                                                channelId = channelId ?: existing.channelId,
                                                bookmarkedAt = existing.bookmarkedAt ?: LocalDateTime.now(),
                                                lastUpdateTime = LocalDateTime.now()
                                            )
                                        )
                                    }
                                }
                            }
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to process artist: ${artist.id}")
                        }
                    }

                    updateState { copy(artists = SyncStatus.Completed) }
                    Timber.d("Synced ${remoteArtists.size} artist subscriptions")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing artist subscriptions")
                    updateState { copy(artists = SyncStatus.Error(e.message ?: "Unknown error")) }
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to fetch artist subscriptions from YouTube")
                updateState { copy(artists = SyncStatus.Error(e.message ?: "Unknown error")) }
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to sync artist subscriptions after retries")
            updateState { copy(artists = SyncStatus.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun executeSyncSavedPlaylists() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncSavedPlaylists - user not logged in")
            return@withContext
        }

        updateState { copy(playlists = SyncStatus.Syncing, currentOperation = "Syncing saved playlists") }

        withRetry {
            YouTube.library("FEmusic_liked_playlists").completed()
        }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val remotePlaylists = page.items.filterIsInstance<PlaylistItem>()
                        .filterNot { it.id == "LM" || it.id == "SE" }
                        .reversed()
                    val remoteIds = remotePlaylists.map { it.id }.toSet()

                    val localPlaylists = database.playlistsByNameAsc().first()
                    localPlaylists.filterNot { it.playlist.browseId in remoteIds }
                        .filterNot { it.playlist.browseId == null }
                        .forEach { playlist ->
                            try {
                                database.update(playlist.playlist.localToggleLike())
                                delay(DB_OPERATION_DELAY_MS)
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to update playlist: ${playlist.id}")
                            }
                        }

                    for (playlist in remotePlaylists) {
                        try {
                            var playlistEntity = localPlaylists.find { it.playlist.browseId == playlist.id }?.playlist

                            if (playlistEntity == null) {
                                playlistEntity = PlaylistEntity(
                                    name = playlist.title,
                                    browseId = playlist.id,
                                    thumbnailUrl = playlist.thumbnail,
                                    isEditable = playlist.isEditable,
                                    bookmarkedAt = LocalDateTime.now(),
                                    remoteSongCount = playlist.songCountText?.let {
                                        Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                    },
                                    playEndpointParams = playlist.playEndpoint?.params,
                                    shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                    radioEndpointParams = playlist.radioEndpoint?.params
                                )
                                database.insert(playlistEntity)
                                Timber.d("syncSavedPlaylists: Created new playlist ${playlist.title} (${playlist.id})")
                                executeSyncPlaylist(playlist.id, playlistEntity.id)
                            } else {
                                database.update(playlistEntity, playlist)
                                Timber.d("syncSavedPlaylists: Updated existing playlist ${playlist.title} (${playlist.id})")
                                if (playlistEntity.isAutoSync) {
                                    executeSyncPlaylist(playlist.id, playlistEntity.id)
                                } else {
                                    Timber.d("syncSavedPlaylists: Skipping full sync for cached playlist ${playlist.title} (isAutoSync=false)")
                                }
                            }
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to sync playlist ${playlist.title}")
                        }
                    }

                    updateState { copy(playlists = SyncStatus.Completed) }
                    Timber.d("Synced ${remotePlaylists.size} saved playlists")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing saved playlists")
                    updateState { copy(playlists = SyncStatus.Error(e.message ?: "Unknown error")) }
                }
            }.onFailure { e ->
                Timber.e(e, "syncSavedPlaylists: Failed to fetch playlists from YouTube")
                updateState { copy(playlists = SyncStatus.Error(e.message ?: "Unknown error")) }
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to sync saved playlists after retries")
            updateState { copy(playlists = SyncStatus.Error(e.message ?: "Unknown error")) }
        }
    }

    private suspend fun executeSyncAutoSyncPlaylists() = withContext(Dispatchers.IO) {
        if (!isLoggedIn()) {
            Timber.w("Skipping syncAutoSyncPlaylists - user not logged in")
            return@withContext
        }

        try {
            val autoSyncPlaylists = database.playlistsByNameAsc().first()
                .filter { it.playlist.isAutoSync && it.playlist.browseId != null }

            Timber.d("syncAutoSyncPlaylists: Found ${autoSyncPlaylists.size} playlists to sync")

            autoSyncPlaylists.forEach { playlist ->
                try {
                    executeSyncPlaylist(playlist.playlist.browseId!!, playlist.playlist.id)
                    delay(DB_OPERATION_DELAY_MS)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync playlist ${playlist.playlist.name}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error syncing auto-sync playlists")
        }
    }

    private suspend fun executeSyncPlaylist(browseId: String, playlistId: String) = withContext(Dispatchers.IO) {
        Timber.d("syncPlaylist: Starting sync for browseId=$browseId, playlistId=$playlistId")

        withRetry {
            YouTube.playlist(browseId).completed()
        }.onSuccess { result ->
            result.onSuccess { page ->
                try {
                    val songs = page.songs.map(SongItem::toMediaMetadata)
                    Timber.d("syncPlaylist: Fetched ${songs.size} songs from remote")

                    if (songs.isEmpty()) {
                        Timber.w("syncPlaylist: Remote playlist is empty, clearing local playlist")
                        database.withTransaction {
                            database.clearPlaylist(playlistId)
                        }
                        return@onSuccess
                    }

                    val remoteIds = songs.map { it.id }
                    val localIds = database.playlistSongs(playlistId).first()
                        .sortedBy { it.map.position }
                        .map { it.song.id }

                    if (remoteIds == localIds) {
                        Timber.d("syncPlaylist: Local and remote are in sync, no changes needed")
                        return@onSuccess
                    }

                    Timber.d("syncPlaylist: Updating local playlist (remote: ${remoteIds.size}, local: ${localIds.size})")

                    database.withTransaction {
                        database.clearPlaylist(playlistId)
                        songs.forEachIndexed { idx, song ->
                            if (database.song(song.id).firstOrNull() == null) {
                                database.insert(song)
                            }
                            database.insert(
                                PlaylistSongMap(
                                    songId = song.id,
                                    playlistId = playlistId,
                                    position = idx,
                                    setVideoId = song.setVideoId
                                )
                            )
                        }
                    }
                    Timber.d("syncPlaylist: Successfully synced playlist")
                } catch (e: Exception) {
                    Timber.e(e, "Error processing playlist sync")
                }
            }.onFailure { e ->
                Timber.e(e, "syncPlaylist: Failed to fetch playlist from YouTube")
            }
        }.onFailure { e ->
            Timber.e(e, "syncPlaylist: Failed after retries")
        }
    }

    private suspend fun getSpotifySession(): SpotifySession? {
        val sessionJson = context.dataStore.data.map { it[SpotifySessionKey] }.firstOrNull() ?: return null
        return runCatching {
            Json.decodeFromString<SpotifySession>(sessionJson)
        }.getOrNull()
    }

    private suspend fun ensureSpotifyAuthenticated(session: SpotifySession): SpotifySession? {
        if (session.accessToken != null && session.expiresAt > System.currentTimeMillis() + 60_000L) {
            Spotify.accessToken = session.accessToken
            return session
        }
        return try {
            val token = SpotifyAuth.fetchAccessToken(session.spDc, session.spKey.orEmpty()).getOrThrow()
            Spotify.accessToken = token.accessToken
            val updated = session.copy(
                accessToken = token.accessToken,
                expiresAt = token.accessTokenExpirationTimestampMs
            )
            context.dataStore.edit { prefs ->
                prefs[SpotifySessionKey] = Json.encodeToString(updated)
            }
            updated
        } catch (e: Exception) {
            Timber.e(e, "Failed to authenticate with Spotify during sync")
            null
        }
    }

    private suspend fun executeSyncSpotifyPlaylists() = withContext(Dispatchers.IO) {
        val session = getSpotifySession() ?: return@withContext
        val authSession = ensureSpotifyAuthenticated(session) ?: return@withContext

        try {
            val spotifyPlaylists = database.playlistsByNameAsc().first()
                .filter { it.playlist.id.startsWith("SPOTIFY_") && it.playlist.isAutoSync }

            Timber.d("syncSpotifyPlaylists: Found ${spotifyPlaylists.size} Spotify playlists to sync")
            for (playlist in spotifyPlaylists) {
                executeSyncSingleSpotifyPlaylist(playlist.playlist.id)
                delay(DB_OPERATION_DELAY_MS)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error syncing Spotify playlists")
        }
    }

    private suspend fun executeSyncSingleSpotifyPlaylist(localPlaylistId: String) = withContext(Dispatchers.IO) {
        val session = getSpotifySession() ?: return@withContext
        val authSession = ensureSpotifyAuthenticated(session) ?: return@withContext

        try {
            val tracks = mutableListOf<SpotifyTrack>()
            if (localPlaylistId == "SPOTIFY_LIKED_SONGS") {
                var offset = 0
                val limit = 50
                while (true) {
                    val page = Spotify.likedSongs(limit = limit, offset = offset).getOrThrow()
                    tracks.addAll(page.items.map { it.track })
                    offset += limit
                    if (offset >= page.total) break
                }
            } else if (localPlaylistId.startsWith("SPOTIFY_PLAYLIST_")) {
                val spotifyId = localPlaylistId.removePrefix("SPOTIFY_PLAYLIST_")
                var offset = 0
                val limit = 100
                while (true) {
                    val page = Spotify.playlistTracks(spotifyId, limit = limit, offset = offset).getOrThrow()
                    tracks.addAll(page.items.mapNotNull { it.track })
                    offset += limit
                    if (offset >= page.total) break
                }
            } else {
                return@withContext
            }

            if (tracks.isEmpty()) {
                database.withTransaction {
                    database.clearPlaylist(localPlaylistId)
                }
                return@withContext
            }

            val matchedList = mutableListOf<com.music.vivi.models.MediaMetadata>()
            for (track in tracks) {
                val artist = track.artists.firstOrNull()?.name.orEmpty()
                val title = track.name
                val query = if (artist.isEmpty()) title else "$artist $title"

                val existingLocal = database.searchSongs(query, 1).first().firstOrNull()
                if (existingLocal != null) {
                    matchedList.add(existingLocal.toMediaMetadata())
                    continue
                }

                try {
                    val searchResult = YouTube.search(
                        query = query,
                        filter = YouTube.SearchFilter.FILTER_SONG
                    ).getOrNull()

                    val candidates = searchResult?.items
                        ?.filterIsInstance<SongItem>()
                        ?.distinctBy { it.id }
                        .orEmpty()

                    val best = candidates.maxByOrNull { candidate ->
                        SpotifyMapper.matchScore(
                            spotifyTitle = track.name,
                            spotifyArtist = track.artists.joinToString(" ") { it.name },
                            spotifyDurationMs = track.durationMs,
                            candidateTitle = candidate.title,
                            candidateArtist = candidate.artists.joinToString(" ") { it.name },
                            candidateDurationSec = candidate.duration
                        )
                    }

                    if (best != null) {
                        matchedList.add(best.toMediaMetadata())
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to match Spotify track $title")
                }
            }

            database.withTransaction {
                database.clearPlaylist(localPlaylistId)
                matchedList.forEachIndexed { idx, song ->
                    if (database.song(song.id).firstOrNull() == null) {
                        database.insert(song)
                    }
                    database.insert(
                        PlaylistSongMap(
                            songId = song.id,
                            playlistId = localPlaylistId,
                            position = idx,
                            setVideoId = song.setVideoId
                        )
                    )
                }
                database.playlist(localPlaylistId).first()?.playlist?.let {
                    database.update(it.copy(lastUpdateTime = LocalDateTime.now()))
                }
            }
            Timber.d("Successfully synced Spotify playlist $localPlaylistId with ${matchedList.size} tracks")
        } catch (e: Exception) {
            Timber.e(e, "Error executing Spotify playlist sync for $localPlaylistId")
        }
    }

    private suspend fun executeCleanupDuplicatePlaylists() = withContext(Dispatchers.IO) {
        try {
            val allPlaylists = database.playlistsByNameAsc().first()
            val browseIdGroups = allPlaylists
                .filter { it.playlist.browseId != null }
                .groupBy { it.playlist.browseId }

            for ((browseId, playlists) in browseIdGroups) {
                if (playlists.size > 1) {
                    Timber.w("Found ${playlists.size} duplicate playlists for browseId: $browseId")
                    val toKeep = playlists.maxByOrNull { it.songCount } ?: playlists.first()

                    playlists.filter { it.id != toKeep.id }.forEach { duplicate ->
                        try {
                            Timber.d("Removing duplicate playlist: ${duplicate.playlist.name} (${duplicate.id})")
                            database.clearPlaylist(duplicate.id)
                            database.delete(duplicate.playlist)
                            delay(DB_OPERATION_DELAY_MS)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to remove duplicate playlist: ${duplicate.id}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up duplicate playlists")
        }
    }

    private suspend fun executeClearAllSyncedContent() = withContext(Dispatchers.IO) {
        Timber.d("clearAllSyncedContent: Starting cleanup")

        updateState { copy(overallStatus = SyncStatus.Syncing, currentOperation = "Clearing synced content") }

        try {
            database.withTransaction {
                // Clear liked songs
                val likedSongs = database.likedSongsByNameAsc().first()
                likedSongs.forEach {
                    database.update(it.song.copy(liked = false, likedDate = null))
                }

                // Clear library songs
                val librarySongs = database.songsByNameAsc().first()
                librarySongs.forEach {
                    if (it.song.inLibrary != null) {
                        database.update(it.song.copy(inLibrary = null))
                    }
                }

                // Clear liked albums
                val likedAlbums = database.albumsLikedByNameAsc().first()
                likedAlbums.forEach {
                    database.update(it.album.copy(bookmarkedAt = null))
                }

                // Clear subscribed artists
                val subscribedArtists = database.artistsBookmarkedByNameAsc().first()
                subscribedArtists.forEach {
                    database.update(it.artist.copy(bookmarkedAt = null))
                }

                // Delete synced playlists
                val savedPlaylists = database.playlistsByNameAsc().first()
                savedPlaylists.forEach {
                    if (it.playlist.browseId != null) {
                        database.clearPlaylist(it.playlist.id)
                        database.delete(it.playlist)
                    }
                }

                // Clear uploaded songs
                val uploadedSongs = database.uploadedSongsByNameAsc().first()
                uploadedSongs.forEach {
                    database.update(it.song.copy(isUploaded = false))
                }

                // Clear uploaded albums
                val uploadedAlbums = database.albumsUploadedByCreateDateAsc().first()
                uploadedAlbums.forEach {
                    database.update(it.album.copy(isUploaded = false))
                }
            }

            // Reset sync timestamp
            context.dataStore.edit { settings ->
                settings[LastFullSyncKey] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            }

            updateState { copy(overallStatus = SyncStatus.Completed, currentOperation = "") }
            Timber.d("clearAllSyncedContent: Cleanup completed successfully")
        } catch (e: Exception) {
            Timber.e(e, "clearAllSyncedContent: Error during cleanup")
            updateState { copy(overallStatus = SyncStatus.Error(e.message ?: "Unknown error"), currentOperation = "") }
        }
    }

    fun cancelAllSyncs() {
        processingJob?.cancel()
        startProcessingQueue()
        updateState { SyncState() }
    }

    suspend fun syncLocalPlaylistToSpotify(
        playlistId: String,
        onProgress: ((String) -> Unit)? = null,
        onComplete: ((Boolean, String) -> Unit)? = null,
    ) = withContext(Dispatchers.IO) {
        val session = getSpotifySession()
        if (session == null) {
            val msg = context.getString(R.string.spotify_not_connected)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onComplete?.invoke(false, msg)
            }
            return@withContext
        }

        val authSession = ensureSpotifyAuthenticated(session)
        if (authSession == null) {
            val msg = context.getString(R.string.spotify_auth_failed)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onComplete?.invoke(false, msg)
            }
            return@withContext
        }

        try {
            val playlist = database.playlist(playlistId).first()
            val playlistName = playlist?.playlist?.name ?: "Vivi Playlist"
            val songs = database.playlistSongs(playlistId).first()

            if (songs.isEmpty()) {
                val msg = "Playlist is empty"
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(false, msg)
                }
                return@withContext
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Syncing \"$playlistName\" to Spotify...", Toast.LENGTH_SHORT).show()
            }

            val linkedKey = androidx.datastore.preferences.core.stringPreferencesKey("spotify_linked_$playlistId")
            val linkedSpotifyId = context.dataStore.data.map { it[linkedKey] }.firstOrNull()

            val targetSpotifyId = if (linkedSpotifyId.isNullOrBlank()) {
                val created = Spotify.createPlaylist(
                    name = playlistName,
                    description = "Synced from Vivi Music"
                ).getOrThrow()
                context.dataStore.edit { prefs ->
                    prefs[linkedKey] = created.id
                }
                created.id
            } else {
                linkedSpotifyId
            }

            val trackUris = mutableListOf<String>()
            for ((index, playlistSong) in songs.withIndex()) {
                val artistName = playlistSong.song.artists.firstOrNull()?.name.orEmpty()
                val title = playlistSong.song.song.title
                val query = if (artistName.isNotBlank()) "$artistName $title" else title
                onProgress?.invoke("Matching track ${index + 1}/${songs.size}: $query")
                val uri = Spotify.searchTrack(query).getOrNull()
                if (uri != null) {
                    trackUris.add(uri)
                }
                delay(50)
            }

            // Fetch existing tracks in the Spotify playlist to prevent duplicate additions
            val existingUris = if (!linkedSpotifyId.isNullOrBlank()) {
                val existingTracks = Spotify.playlistTracks(targetSpotifyId, limit = 100).getOrNull()
                existingTracks?.items?.mapNotNull { it.track?.uri }?.toSet() ?: emptySet()
            } else {
                emptySet()
            }

            val newUrisToAdd = trackUris.filter { it !in existingUris }

            if (newUrisToAdd.isNotEmpty()) {
                Spotify.addTracksToPlaylist(targetSpotifyId, newUrisToAdd).getOrThrow()
                val successMsg = "Successfully synced ${newUrisToAdd.size} new tracks to Spotify!"
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(true, successMsg)
                }
            } else {
                val upToDateMsg = if (trackUris.isNotEmpty()) "Spotify playlist is already up to date" else "No matching songs found on Spotify"
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, upToDateMsg, Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(true, upToDateMsg)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "syncLocalPlaylistToSpotify failed")
            val errorMsg = e.message ?: "Sync to Spotify failed"
            withContext(Dispatchers.Main) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                onComplete?.invoke(false, errorMsg)
            }
        }
    }
}
