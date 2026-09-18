package com.music.vivi.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.spotify.Spotify
import com.music.spotify.SpotifyAuth
import com.music.spotify.SpotifyMapper
import com.music.spotify.models.SpotifyPlaylist
import com.music.spotify.models.SpotifyTrack
import com.music.vivi.constants.SpotifySessionKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.ArtistEntity
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.db.entities.Song
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.models.MediaMetadata
import com.music.vivi.models.toMediaMetadata
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.reportException
import com.music.vivi.R
import android.widget.Toast
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

import com.music.vivi.constants.SpotifyPlaylistsCacheKey
import java.io.File

import com.music.vivi.utils.SyncUtils
import com.music.vivi.db.entities.Playlist
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@Serializable
data class SpotifySession(
    val spDc: String = "",
    val spKey: String? = null,
    val accessToken: String? = null,
    val expiresAt: Long = 0,
    val accountName: String? = null,
    val accountAvatarUrl: String? = null,
    val refreshToken: String? = null,
    val clientId: String? = null,
)

@Serializable
data class SpotifyCachedProfileData(
    val accountName: String = "",
    val accountAvatarUrl: String? = null,
    val likedSongsCount: Int = 0,
    val playlists: List<SpotifyPlaylist> = emptyList(),
)

data class SpotifyImportUiState(
    val isAuthenticated: Boolean = false,
    val accountName: String = "",
    val accountAvatarUrl: String? = null,
    val playlists: List<SpotifyPlaylist> = emptyList(),
    val likedSongsCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class SpotifyImportProgress(
    val playlistName: String,
    val currentSongIndex: Int,
    val totalSongs: Int,
    val percent: Float,
    val isFinished: Boolean = false
)

data class SpotifyPushProgress(
    val playlistName: String,
    val percent: Float,
    val status: String,
    val isFinished: Boolean = false,
)

private data class PlaylistImportData(
    val title: String,
    val songs: List<Song>,
    val localPlaylistId: String,
    val thumbnailUrl: String?
)

@HiltViewModel
class SpotifyImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpotifyImportUiState(isLoading = true))
    val uiState: StateFlow<SpotifyImportUiState> = _uiState.asStateFlow()

    private val _importProgress = MutableStateFlow<SpotifyImportProgress?>(null)
    val importProgress: StateFlow<SpotifyImportProgress?> = _importProgress.asStateFlow()
    val pushProgress: StateFlow<SpotifyPushProgress?> = _pushProgress.asStateFlow()
    val isImportMinimized: StateFlow<Boolean> = _isImportMinimized.asStateFlow()
    val isPushMinimized: StateFlow<Boolean> = _isPushMinimized.asStateFlow()

    val localPlaylists: StateFlow<List<Playlist>> = database.playlistsByNameAsc()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val _importProgress = MutableStateFlow<SpotifyImportProgress?>(null)
        private val _pushProgress = MutableStateFlow<SpotifyPushProgress?>(null)
        private val _isImportMinimized = MutableStateFlow(false)
        private val _isPushMinimized = MutableStateFlow(false)
        private var importJob: Job? = null
        private var pushJob: Job? = null
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    init {
        restoreSession()
    }

    fun minimizeImport() {
        _isImportMinimized.value = true
    }

    fun restoreImportDialog() {
        _isImportMinimized.value = false
    }

    fun minimizePush() {
        _isPushMinimized.value = true
    }

    fun restorePushDialog() {
        _isPushMinimized.value = false
    }

    fun pushLocalPlaylists(playlistIds: List<String>) {
        if (playlistIds.isEmpty()) return
        _isPushMinimized.value = false
        pushJob?.cancel()
        pushJob = backgroundScope.launch {
            try {
                ensureAuthenticated()
                _pushProgress.value = SpotifyPushProgress(
                    playlistName = "",
                    percent = 0f,
                    status = context.getString(R.string.push_in_progress),
                    isFinished = false,
                )
                syncUtils.pushMultipleLocalPlaylistsToSpotify(
                    playlistIds = playlistIds,
                    onProgress = { name, percent, status ->
                        _pushProgress.value = SpotifyPushProgress(
                            playlistName = name,
                            percent = percent,
                            status = status,
                            isFinished = false,
                        )
                    },
                    onComplete = { succeeded, total ->
                        val msg = "Successfully pushed $succeeded of $total playlists to Spotify!"
                        _pushProgress.value = SpotifyPushProgress(
                            playlistName = "",
                            percent = 1f,
                            status = msg,
                            isFinished = true,
                        )
                        loadSources()
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                _pushProgress.value = SpotifyPushProgress(
                    playlistName = "",
                    percent = 1f,
                    status = e.message ?: "Failed to push to Spotify",
                    isFinished = true,
                )
            }
        }
    }

    fun cancelPush() {
        pushJob?.cancel()
        pushJob = null
        _pushProgress.value = null
        _isPushMinimized.value = false
    }

    fun dismissPushProgress() {
        _pushProgress.value = null
        _isPushMinimized.value = false
    }

    private suspend fun getSession(): SpotifySession? {
        val prefs = context.dataStore.data.first()
        val sessionJson = prefs[SpotifySessionKey]
        return if (!sessionJson.isNullOrBlank()) {
            json.decodeFromString<SpotifySession>(sessionJson)
        } else {
            null
        }
    }

    private suspend fun saveSession(session: SpotifySession) {
        context.dataStore.edit { prefs ->
            prefs[SpotifySessionKey] = json.encodeToString(session)
        }
    }

    private suspend fun ensureAuthenticated(): SpotifySession {
        val session = getSession() ?: throw IllegalStateException("Not connected to Spotify")
        if (session.accessToken != null && session.expiresAt > System.currentTimeMillis() + 60_000L) {
            Spotify.accessToken = session.accessToken
            return session
        }
        if (!session.refreshToken.isNullOrBlank()) {
            return refreshWithRefreshToken(session.refreshToken, session.clientId ?: SpotifyAuth.DEFAULT_CLIENT_ID)
        }
        if (session.spDc.isNotBlank()) {
            return refreshWithCookies(session.spDc, session.spKey.orEmpty())
        }
        throw IllegalStateException("No valid credentials for Spotify")
    }

    private suspend fun refreshWithRefreshToken(refreshToken: String, clientId: String): SpotifySession =
        withContext(Dispatchers.IO) {
            val token = SpotifyAuth.refreshAccessToken(clientId, refreshToken).getOrThrow()
            Spotify.accessToken = token.access_token
            val profile = Spotify.me().getOrNull()

            val session = getSession()
            val newSession = (session ?: SpotifySession()).copy(
                accessToken = token.access_token,
                expiresAt = System.currentTimeMillis() + (token.expires_in * 1000L),
                refreshToken = token.refresh_token ?: refreshToken,
                clientId = clientId,
                accountName = profile?.displayName ?: session?.accountName,
                accountAvatarUrl = profile?.images?.firstOrNull()?.url ?: session?.accountAvatarUrl
            )
            saveSession(newSession)
            _uiState.update {
                it.copy(
                    isAuthenticated = true,
                    accountName = newSession.accountName.orEmpty(),
                    accountAvatarUrl = newSession.accountAvatarUrl,
                )
            }
            newSession
        }

    fun connectWithOAuthCode(
        code: String,
        codeVerifier: String,
        clientId: String = SpotifyAuth.DEFAULT_CLIENT_ID,
        redirectUri: String = SpotifyAuth.DEFAULT_REDIRECT_URI,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val token = SpotifyAuth.exchangeCode(clientId, redirectUri, code, codeVerifier).getOrThrow()
                Spotify.accessToken = token.access_token
                val profile = Spotify.me().getOrNull()

                val newSession = SpotifySession(
                    accessToken = token.access_token,
                    expiresAt = System.currentTimeMillis() + (token.expires_in * 1000L),
                    refreshToken = token.refresh_token,
                    clientId = clientId,
                    accountName = profile?.displayName,
                    accountAvatarUrl = profile?.images?.firstOrNull()?.url
                )
                saveSession(newSession)
                _uiState.update {
                    it.copy(
                        isAuthenticated = true,
                        accountName = newSession.accountName.orEmpty(),
                        accountAvatarUrl = newSession.accountAvatarUrl,
                    )
                }
                newSession
            }.onSuccess {
                loadSources()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to log in to Spotify"
                    )
                }
            }
        }
    }

    private suspend fun refreshWithCookies(spDc: String, spKey: String): SpotifySession =
        withContext(Dispatchers.IO) {
            val token = SpotifyAuth.fetchAccessToken(spDc, spKey).getOrThrow()
            Spotify.accessToken = token.accessToken
            val profile = Spotify.me().getOrNull()

            val newSession = SpotifySession(
                spDc = spDc,
                spKey = spKey,
                accessToken = token.accessToken,
                expiresAt = token.accessTokenExpirationTimestampMs,
                accountName = profile?.displayName,
                accountAvatarUrl = profile?.images?.firstOrNull()?.url
            )
            saveSession(newSession)
            _uiState.update {
                it.copy(
                    isAuthenticated = true,
                    accountName = newSession.accountName.orEmpty(),
                    accountAvatarUrl = newSession.accountAvatarUrl,
                )
            }
            newSession
        }

    fun restoreSession() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = context.dataStore.data.first()
                val session = getSession()

                // 1. Immediately restore cached profile data if available
                val cachedJson = prefs[SpotifyPlaylistsCacheKey]
                val cachedData = if (!cachedJson.isNullOrBlank()) {
                    runCatching { json.decodeFromString<SpotifyCachedProfileData>(cachedJson) }.getOrNull()
                } else null

                if (cachedData != null && (cachedData.playlists.isNotEmpty() || cachedData.likedSongsCount > 0 || cachedData.accountName.isNotBlank())) {
                    _uiState.update {
                        it.copy(
                            isAuthenticated = session != null,
                            accountName = cachedData.accountName.ifBlank { session?.accountName.orEmpty() },
                            accountAvatarUrl = cachedData.accountAvatarUrl ?: session?.accountAvatarUrl,
                            playlists = cachedData.playlists,
                            likedSongsCount = cachedData.likedSongsCount,
                            isLoading = false,
                        )
                    }
                }

                if (session == null) {
                    _uiState.update { it.copy(isAuthenticated = false, isLoading = false) }
                    return@launch
                }

                if (session.accessToken != null && session.expiresAt > System.currentTimeMillis() + 60_000L) {
                    Spotify.accessToken = session.accessToken
                    _uiState.update {
                        it.copy(
                            isAuthenticated = true,
                            accountName = it.accountName.ifBlank { session.accountName.orEmpty() },
                            accountAvatarUrl = it.accountAvatarUrl ?: session.accountAvatarUrl,
                            isLoading = it.playlists.isEmpty()
                        )
                    }
                    loadSources()
                } else if (!session.refreshToken.isNullOrBlank()) {
                    runCatching { refreshWithRefreshToken(session.refreshToken, session.clientId ?: SpotifyAuth.DEFAULT_CLIENT_ID) }
                        .onSuccess { loadSources() }
                        .onFailure { error ->
                            if (_uiState.value.playlists.isEmpty()) {
                                logout()
                                _uiState.update {
                                    it.copy(
                                        isAuthenticated = false,
                                        isLoading = false,
                                        errorMessage = "Session expired. Please log in again."
                                    )
                                }
                            }
                        }
                } else {
                    runCatching { refreshWithCookies(session.spDc, session.spKey.orEmpty()) }
                        .onSuccess { loadSources() }
                        .onFailure { error ->
                            if (_uiState.value.playlists.isEmpty()) {
                                logout()
                                _uiState.update {
                                    it.copy(
                                        isAuthenticated = false,
                                        isLoading = false,
                                        errorMessage = "Session expired. Please log in again."
                                    )
                                }
                            }
                        }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to restore session"
                    )
                }
            }
        }
    }

    fun connectWithCookies(spDc: String, spKey: String) {
        if (spDc.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { refreshWithCookies(spDc, spKey) }
                .onSuccess { loadSources() }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to connect to Spotify"
                        )
                    }
                }
        }
    }

    fun loadSources() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_uiState.value.playlists.isEmpty()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            try {
                ensureAuthenticated()
                val meResult = Spotify.me().getOrThrow()
                val likedSongsResult = Spotify.likedSongs(limit = 1, offset = 0).getOrThrow()

                val playlistsList = mutableListOf<SpotifyPlaylist>()
                var offset = 0
                val limit = 50
                while (true) {
                    val page = spotifyCallWithTokenRetry {
                        Spotify.myPlaylists(limit = limit, offset = offset).getOrThrow()
                    }
                    playlistsList.addAll(page.items)
                    offset += limit
                    if (page.items.isEmpty() || offset >= page.total) break
                }

                val enrichedPlaylists = playlistsList.map { pl ->
                    val total = pl.tracks?.total
                    if (total != null && total > 0) {
                        pl
                    } else {
                        val trackCount = runCatching {
                            Spotify.playlistTracks(pl.id, limit = 1).getOrNull()?.total
                        }.getOrNull()
                        if (trackCount != null && trackCount > 0) {
                            pl.copy(tracks = com.music.spotify.models.SpotifyPlaylistTracksRef(total = trackCount))
                        } else pl
                    }
                }

                val accountName = meResult.displayName.orEmpty()
                val accountAvatarUrl = meResult.images.firstOrNull()?.url
                val likedCount = likedSongsResult.total

                _uiState.update {
                    it.copy(
                        playlists = enrichedPlaylists,
                        likedSongsCount = likedCount,
                        isLoading = false,
                        accountName = accountName,
                        accountAvatarUrl = accountAvatarUrl
                    )
                }

                // Cache profile and playlists to DataStore for instant display on next app launch
                runCatching {
                    val cacheData = SpotifyCachedProfileData(
                        accountName = accountName,
                        accountAvatarUrl = accountAvatarUrl,
                        likedSongsCount = likedCount,
                        playlists = enrichedPlaylists
                    )
                    context.dataStore.edit { prefs ->
                        prefs[SpotifyPlaylistsCacheKey] = json.encodeToString(cacheData)
                    }
                }
            } catch (e: Exception) {
                reportException(e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (it.playlists.isEmpty()) e.message ?: "Failed to fetch playlists" else null
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                prefs.remove(SpotifySessionKey)
                prefs.remove(SpotifyPlaylistsCacheKey)
            }
            Spotify.accessToken = null
            _uiState.update {
                SpotifyImportUiState(
                    isAuthenticated = false,
                    isLoading = false
                )
            }
        }
    }

    private suspend fun <T> spotifyCallWithTokenRetry(block: suspend () -> T): T =
        runCatching { block() }
            .getOrElse { error ->
                if ((error as? Spotify.SpotifyException)?.statusCode != 401) {
                    throw error
                }
                ensureAuthenticated()
                block()
            }

    suspend fun fetchPlaylistTracks(playlistId: String): List<Song> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<SpotifyTrack>()
        var offset = 0
        val limit = 100
        while (true) {
            val page = spotifyCallWithTokenRetry {
                Spotify.playlistTracks(playlistId, limit = limit, offset = offset).getOrThrow()
            }
            tracks.addAll(page.items.mapNotNull { it.track })
            offset += limit
            if (page.items.isEmpty() || offset >= page.total) break
        }

        return@withContext tracks.map { track ->
            Song(
                song = SongEntity(
                    id = "",
                    title = track.name,
                    duration = track.durationMs / 1000,
                    thumbnailUrl = SpotifyMapper.getTrackThumbnail(track),
                ),
                artists = track.artists.map { ArtistEntity(id = "", name = it.name) },
            )
        }
    }

    suspend fun fetchLikedSongsTracks(): List<Song> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<SpotifyTrack>()
        var offset = 0
        val limit = 50
        while (true) {
            val page = spotifyCallWithTokenRetry {
                Spotify.likedSongs(limit = limit, offset = offset).getOrThrow()
            }
            tracks.addAll(page.items.map { it.track })
            offset += limit
            if (page.items.isEmpty() || offset >= page.total) break
        }

        return@withContext tracks.map { track ->
            Song(
                song = SongEntity(
                    id = "",
                    title = track.name,
                    duration = track.durationMs / 1000,
                    thumbnailUrl = SpotifyMapper.getTrackThumbnail(track),
                ),
                artists = track.artists.map { ArtistEntity(id = "", name = it.name) },
            )
        }
    }

    fun startImport(selectedIds: List<String>) {
        _isImportMinimized.value = false
        importJob?.cancel()
        importJob = backgroundScope.launch(Dispatchers.IO) {
            _importProgress.update { null }

            try {
                selectedIds.forEachIndexed { playlistIndex, id ->
                    val importData = if (id == "liked_songs") {
                        val songs = fetchLikedSongsTracks()
                        PlaylistImportData(
                            title = context.getString(R.string.spotify_liked_songs),
                            songs = songs,
                            localPlaylistId = "SPOTIFY_LIKED_SONGS",
                            thumbnailUrl = null
                        )
                    } else {
                        val playlist = _uiState.value.playlists.firstOrNull { it.id == id }
                        if (playlist != null) {
                            val songs = fetchPlaylistTracks(playlist.id)
                            PlaylistImportData(
                                title = playlist.name,
                                songs = songs,
                                localPlaylistId = "SPOTIFY_PLAYLIST_${playlist.id}",
                                thumbnailUrl = SpotifyMapper.getPlaylistThumbnail(playlist)
                            )
                        } else {
                            null
                        }
                    } ?: return@forEachIndexed

                    val totalSongs = importData.songs.size
                    if (totalSongs == 0) {
                        database.withTransaction {
                            val existing = playlist(importData.localPlaylistId).first()
                            val now = LocalDateTime.now()
                            val entity = existing?.playlist?.copy(
                                name = importData.title,
                                bookmarkedAt = existing.playlist.bookmarkedAt ?: now,
                                lastUpdateTime = now,
                                thumbnailUrl = importData.thumbnailUrl,
                                isEditable = true,
                                isAutoSync = true,
                            ) ?: PlaylistEntity(
                                id = importData.localPlaylistId,
                                name = importData.title,
                                bookmarkedAt = now,
                                lastUpdateTime = now,
                                thumbnailUrl = importData.thumbnailUrl,
                                isEditable = true,
                                isAutoSync = true,
                            )
                            if (existing == null) insert(entity) else update(entity)
                            clearPlaylist(importData.localPlaylistId)
                        }

                        _importProgress.update {
                            SpotifyImportProgress(
                                playlistName = importData.title,
                                currentSongIndex = 0,
                                totalSongs = 0,
                                percent = 1.0f
                            )
                        }
                        return@forEachIndexed
                    }

                    _importProgress.update {
                        SpotifyImportProgress(
                            playlistName = importData.title,
                            currentSongIndex = 0,
                            totalSongs = totalSongs,
                            percent = 0.0f
                        )
                    }

                    val completedCount = AtomicInteger(0)
                    val semaphore = Semaphore(2)

                    val matchedMedia = importData.songs.mapIndexed { _, song ->
                        async {
                            semaphore.withPermit {
                                var resultMedia: MediaMetadata? = null
                                try {
                                    val artist = song.artists.firstOrNull()?.name.orEmpty()
                                    val cleanTitle = Spotify.cleanTrackSearchQuery(song.title)
                                    val query = if (artist.isEmpty()) cleanTitle else "$artist $cleanTitle"

                                    // 1. Check local DB first to avoid unnecessary network calls
                                    val existingLocal = database.searchSongs(query, 1).first().firstOrNull()
                                    if (existingLocal != null) {
                                        resultMedia = existingLocal.toMediaMetadata()
                                    } else {
                                        // Pacing delay to avoid YouTube rate limit
                                        delay(100)
                                        val searchResult = YouTube.search(
                                            query = query,
                                            filter = YouTube.SearchFilter.FILTER_SONG,
                                        ).getOrNull()

                                        val candidates = searchResult?.items
                                            ?.filterIsInstance<SongItem>()
                                            ?.distinctBy { it.id }
                                            .orEmpty()

                                        val best = candidates.maxByOrNull { candidate ->
                                            SpotifyMapper.matchScore(
                                                spotifyTitle = song.title,
                                                spotifyArtist = song.artists.joinToString(" ") { it.name },
                                                spotifyDurationMs = song.song.duration * 1000,
                                                candidateTitle = candidate.title,
                                                candidateArtist = candidate.artists.joinToString(" ") { it.name },
                                                candidateDurationSec = candidate.duration,
                                            )
                                        }

                                        if (best != null) {
                                            resultMedia = best.toMediaMetadata()
                                        }
                                    }
                                } catch (e: Exception) {
                                    reportException(e)
                                } finally {
                                    val done = completedCount.incrementAndGet()
                                    _importProgress.update {
                                        SpotifyImportProgress(
                                            playlistName = importData.title,
                                            currentSongIndex = done,
                                            totalSongs = totalSongs,
                                            percent = done.toFloat() / totalSongs
                                        )
                                    }
                                }
                                resultMedia
                            }
                        }
                    }.awaitAll().filterNotNull()

                    database.withTransaction {
                        val existing = playlist(importData.localPlaylistId).first()
                        val now = LocalDateTime.now()
                        val entity = existing?.playlist?.copy(
                            name = importData.title,
                            bookmarkedAt = existing.playlist.bookmarkedAt ?: now,
                            lastUpdateTime = now,
                            thumbnailUrl = importData.thumbnailUrl,
                            isEditable = true,
                            isAutoSync = true,
                        ) ?: PlaylistEntity(
                            id = importData.localPlaylistId,
                            name = importData.title,
                            bookmarkedAt = now,
                            lastUpdateTime = now,
                            thumbnailUrl = importData.thumbnailUrl,
                            isEditable = true,
                            isAutoSync = true,
                        )

                        if (existing == null) {
                            insert(entity)
                        } else {
                            update(entity)
                        }

                        matchedMedia.forEach { metadata ->
                            insert(metadata)
                        }

                        clearPlaylist(importData.localPlaylistId)
                        matchedMedia.forEachIndexed { index, metadata ->
                            insert(
                                PlaylistSongMap(
                                    playlistId = importData.localPlaylistId,
                                    songId = metadata.id,
                                    position = index,
                                    setVideoId = metadata.setVideoId,
                                )
                            )
                        }
                        update(entity.copy(lastUpdateTime = now))
                    }
                }
                _importProgress.update { old ->
                    old?.copy(isFinished = true) ?: SpotifyImportProgress(
                        playlistName = "",
                        currentSongIndex = 0,
                        totalSongs = 0,
                        percent = 1.0f,
                        isFinished = true
                    )
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Spotify import completed!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    reportException(e)
                    _uiState.update { it.copy(errorMessage = e.message ?: "Failed to import playlists") }
                }
            } finally {
                if (_importProgress.value?.isFinished != true) {
                    _importProgress.update { null }
                }
            }
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        importJob = null
        _importProgress.update { null }
        _isImportMinimized.value = false
    }

    fun dismissImportProgress() {
        _importProgress.update { null }
        _isImportMinimized.value = false
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
