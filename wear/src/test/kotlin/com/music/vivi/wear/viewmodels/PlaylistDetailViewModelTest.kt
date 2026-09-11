/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.viewmodels

import com.music.innertube.models.Artist
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.pages.PlaylistContinuationPage
import com.music.innertube.pages.PlaylistPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaylistDetailViewModelTest {

    @Test
    fun testInitialStateLoadsPlaylistTracks() = runBlocking {
        val mockSongs = listOf(
            SongItem(
                id = "song_1",
                title = "Come Together",
                artists = listOf(Artist(name = "The Beatles", id = "beatles")),
                album = null,
                duration = 259,
                thumbnail = "https://example.com/song1.jpg",
                explicit = false,
                endpoint = null,
            ),
            SongItem(
                id = "song_2",
                title = "Something",
                artists = listOf(Artist(name = "The Beatles", id = "beatles")),
                album = null,
                duration = 182,
                thumbnail = "https://example.com/song2.jpg",
                explicit = false,
                endpoint = null,
            ),
        )

        val mockPlaylist = PlaylistItem(
            id = "PL_TEST_123",
            title = "Abbey Road Album",
            author = Artist(name = "The Beatles", id = "beatles"),
            songCountText = "2 tracks",
            thumbnail = "https://example.com/abbey.jpg",
            playEndpoint = null,
            shuffleEndpoint = null,
            radioEndpoint = null,
        )

        val viewModel = PlaylistDetailViewModel(
            playlistId = "PL_TEST_123",
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylist = {
                Result.success(
                    PlaylistPage(
                        playlist = mockPlaylist,
                        songs = mockSongs,
                        songsContinuation = "CONT_TOKEN_1",
                        continuation = null,
                    )
                )
            },
        )

        val state = viewModel.state.value
        assertFalse("Loading should be false", state.isLoading)
        assertNull("Error should be null", state.error)
        assertEquals("Abbey Road Album", state.playlist?.title)
        assertEquals(2, state.songs.size)
        assertEquals("song_1", state.songs[0].id)
        assertEquals("Come Together", state.songs[0].title)
        assertEquals("The Beatles", state.songs[0].artists.first().name)
        assertEquals(259, state.songs[0].duration)
        assertEquals("CONT_TOKEN_1", state.continuation)
    }

    @Test
    fun testLikedSongsDefaultTitle() = runBlocking {
        val mockPlaylist = PlaylistItem(
            id = "LM",
            title = "", // InnerTube often returns empty title for Liked Songs
            author = null,
            songCountText = "50 tracks",
            thumbnail = null,
            playEndpoint = null,
            shuffleEndpoint = null,
            radioEndpoint = null,
        )

        val viewModel = PlaylistDetailViewModel(
            playlistId = "LM",
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylist = {
                Result.success(
                    PlaylistPage(
                        playlist = mockPlaylist,
                        songs = listOf(
                            SongItem(
                                id = "liked_song_1",
                                title = "Favorite Track",
                                artists = listOf(Artist(name = "Artist", id = "a1")),
                                album = null,
                                duration = 200,
                                thumbnail = "https://example.com/liked.jpg",
                                explicit = false,
                                endpoint = null,
                            )
                        ),
                        songsContinuation = null,
                        continuation = null,
                    )
                )
            },
        )

        val state = viewModel.state.value
        assertEquals("Liked Songs", state.playlist?.title)
        assertEquals(1, state.songs.size)
        assertEquals("Favorite Track", state.songs[0].title)
    }

    @Test
    fun testLoadMoreAppendsContinuationSongs() = runBlocking {
        val initialSongs = listOf(
            SongItem(
                id = "s1",
                title = "Track 1",
                artists = emptyList(),
                album = null,
                duration = 180,
                thumbnail = "https://example.com/s1.jpg",
                explicit = false,
                endpoint = null,
            )
        )

        val moreSongs = listOf(
            SongItem(
                id = "s2",
                title = "Track 2",
                artists = emptyList(),
                album = null,
                duration = 210,
                thumbnail = "https://example.com/s2.jpg",
                explicit = false,
                endpoint = null,
            )
        )

        val viewModel = PlaylistDetailViewModel(
            playlistId = "PL_LARGE",
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylist = {
                Result.success(
                    PlaylistPage(
                        playlist = PlaylistItem(
                            id = "PL_LARGE",
                            title = "Large Playlist",
                            author = null,
                            songCountText = null,
                            thumbnail = null,
                            playEndpoint = null,
                            shuffleEndpoint = null,
                            radioEndpoint = null,
                        ),
                        songs = initialSongs,
                        songsContinuation = "TOKEN_STEP_1",
                        continuation = null,
                    )
                )
            },
            fetchContinuation = { token ->
                assertEquals("TOKEN_STEP_1", token)
                Result.success(
                    PlaylistContinuationPage(
                        songs = moreSongs,
                        continuation = "TOKEN_STEP_2",
                    )
                )
            },
        )

        assertEquals(1, viewModel.state.value.songs.size)
        assertEquals("TOKEN_STEP_1", viewModel.state.value.continuation)

        viewModel.loadMore()

        val stateAfterLoadMore = viewModel.state.value
        assertEquals(2, stateAfterLoadMore.songs.size)
        assertEquals("s1", stateAfterLoadMore.songs[0].id)
        assertEquals("s2", stateAfterLoadMore.songs[1].id)
        assertEquals("TOKEN_STEP_2", stateAfterLoadMore.continuation)
    }

    @Test
    fun testErrorHandlingWhenPlaylistFails() = runBlocking {
        val viewModel = PlaylistDetailViewModel(
            playlistId = "INVALID_ID",
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylist = {
                Result.failure(IllegalArgumentException("Playlist not found"))
            },
        )

        val state = viewModel.state.value
        assertFalse("Loading should be false", state.isLoading)
        assertNotNull("Error message should be set", state.error)
        assertTrue(state.error!!.contains("Playlist not found"))
        assertTrue("Songs should be empty", state.songs.isEmpty())
        assertNull("Playlist should be null", state.playlist)
    }
}
