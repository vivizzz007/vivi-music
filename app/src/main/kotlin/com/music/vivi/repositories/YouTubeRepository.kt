package com.music.vivi.repositories

import com.music.innertube.YouTube
import com.music.innertube.pages.AlbumPage
import com.music.innertube.pages.ArtistPage
import com.music.innertube.pages.ExplorePage
import com.music.innertube.pages.HomePage
import com.music.innertube.pages.PlaylistPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeRepository @Inject constructor() {
    private var cachedHomePage: HomePage? = null
    private var cachedExplorePage: ExplorePage? = null
    private val cachedAlbums = mutableMapOf<String, AlbumPage>()
    private val cachedPlaylists = mutableMapOf<String, PlaylistPage>()
    private val cachedArtists = mutableMapOf<String, ArtistPage>()

    suspend fun getHomePage(forceRefresh: Boolean = false): Result<HomePage> {
        if (!forceRefresh && cachedHomePage != null) {
            return Result.success(cachedHomePage!!)
        }
        return YouTube.home().onSuccess {
            cachedHomePage = it
        }
    }

    fun getHomePageFlow(forceRefresh: Boolean = false): Flow<Result<HomePage>> = flow {
        if (!forceRefresh && cachedHomePage != null) {
            emit(Result.success(cachedHomePage!!))
        }
        emit(YouTube.home().onSuccess { cachedHomePage = it })
    }

    suspend fun getExplorePage(forceRefresh: Boolean = false): Result<ExplorePage> {
        if (!forceRefresh && cachedExplorePage != null) {
            return Result.success(cachedExplorePage!!)
        }
        return YouTube.explore().onSuccess {
            cachedExplorePage = it
        }
    }

    fun getExplorePageFlow(forceRefresh: Boolean = false): Flow<Result<ExplorePage>> = flow {
        if (!forceRefresh && cachedExplorePage != null) {
            emit(Result.success(cachedExplorePage!!))
        }
        emit(YouTube.explore().onSuccess { cachedExplorePage = it })
    }

    suspend fun getAlbum(albumId: String, forceRefresh: Boolean = false): Result<AlbumPage> {
        if (!forceRefresh && cachedAlbums.containsKey(albumId)) {
            return Result.success(cachedAlbums[albumId]!!)
        }
        return YouTube.album(albumId).onSuccess {
            cachedAlbums[albumId] = it
        }
    }

    fun getAlbumFlow(albumId: String, forceRefresh: Boolean = false): Flow<Result<AlbumPage>> = flow {
        if (!forceRefresh && cachedAlbums.containsKey(albumId)) {
            emit(Result.success(cachedAlbums[albumId]!!))
        }
        emit(YouTube.album(albumId).onSuccess { cachedAlbums[albumId] = it })
    }

    suspend fun getPlaylist(playlistId: String, forceRefresh: Boolean = false): Result<PlaylistPage> {
        if (!forceRefresh && cachedPlaylists.containsKey(playlistId)) {
            return Result.success(cachedPlaylists[playlistId]!!)
        }
        return YouTube.playlist(playlistId).onSuccess {
            cachedPlaylists[playlistId] = it
        }
    }

    fun getPlaylistFlow(playlistId: String, forceRefresh: Boolean = false): Flow<Result<PlaylistPage>> = flow {
        if (!forceRefresh && cachedPlaylists.containsKey(playlistId)) {
            emit(Result.success(cachedPlaylists[playlistId]!!))
        }
        emit(YouTube.playlist(playlistId).onSuccess { cachedPlaylists[playlistId] = it })
    }

    suspend fun getArtist(artistId: String, forceRefresh: Boolean = false): Result<ArtistPage> {
        if (!forceRefresh && cachedArtists.containsKey(artistId)) {
            return Result.success(cachedArtists[artistId]!!)
        }
        return YouTube.artist(artistId).onSuccess {
            cachedArtists[artistId] = it
        }
    }

    fun getArtistFlow(artistId: String, forceRefresh: Boolean = false): Flow<Result<ArtistPage>> = flow {
        if (!forceRefresh && cachedArtists.containsKey(artistId)) {
            emit(Result.success(cachedArtists[artistId]!!))
        }
        emit(YouTube.artist(artistId).onSuccess { cachedArtists[artistId] = it })
    }

    fun clearCache() {
        cachedHomePage = null
        cachedExplorePage = null
        cachedAlbums.clear()
        cachedPlaylists.clear()
        cachedArtists.clear()
    }
}
