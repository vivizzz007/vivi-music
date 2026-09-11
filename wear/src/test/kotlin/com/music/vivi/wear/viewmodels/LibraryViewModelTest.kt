/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.viewmodels

import com.music.innertube.models.AlbumItem
import com.music.innertube.models.Artist
import com.music.innertube.models.PlaylistItem
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
class LibraryViewModelTest {

    @Test
    fun testInitialStateLoggedOutWhenNoCookie() = runBlocking {
        val viewModel = LibraryViewModel(
            ioDispatcher = Dispatchers.Unconfined,
            isCookiePresent = { false },
        )

        val state = viewModel.state.value
        assertEquals(LibraryTab.PLAYLISTS, state.selectedTab)
        assertFalse("Should not be logged in", state.isLoggedIn)
        assertFalse("Should not be loading", state.isLoading)
        assertTrue("Playlists should be empty", state.playlists.isEmpty())
        assertTrue("Albums should be empty", state.albums.isEmpty())
        assertNull("Error should be null", state.error)
    }

    @Test
    fun testTabSelection() = runBlocking {
        val viewModel = LibraryViewModel(
            ioDispatcher = Dispatchers.Unconfined,
            isCookiePresent = { false },
        )

        assertEquals(LibraryTab.PLAYLISTS, viewModel.state.value.selectedTab)

        viewModel.selectTab(LibraryTab.ALBUMS)
        assertEquals(LibraryTab.ALBUMS, viewModel.state.value.selectedTab)

        viewModel.selectTab(LibraryTab.DOWNLOADS)
        assertEquals(LibraryTab.DOWNLOADS, viewModel.state.value.selectedTab)

        viewModel.selectTab(LibraryTab.PLAYLISTS)
        assertEquals(LibraryTab.PLAYLISTS, viewModel.state.value.selectedTab)
    }

    @Test
    fun testLoggedInFetchesAndMapsPlaylistsAndAlbums() = runBlocking {
        val mockPlaylists = listOf(
            PlaylistItem(
                id = "PL_USER_1",
                title = "Rock Favorites",
                author = Artist(name = "User", id = "user1"),
                songCountText = "42 tracks",
                thumbnail = "https://example.com/rock.jpg",
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null,
            ),
            PlaylistItem(
                id = "SE", // Episode - must be filtered out
                title = "Podcast Episode",
                author = null,
                songCountText = "1 track",
                thumbnail = null,
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null,
            ),
            PlaylistItem(
                id = "SS_shorts_mix", // YouTube Shorts - must be filtered out
                title = "Shorts Mix",
                author = null,
                songCountText = "10 tracks",
                thumbnail = null,
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null,
            ),
            PlaylistItem(
                id = "PL_USER_2",
                title = "Chill Beats",
                author = Artist(name = "User", id = "user1"),
                songCountText = "15 tracks",
                thumbnail = "https://example.com/chill.jpg",
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null,
            ),
        )

        val mockAlbums = listOf(
            AlbumItem(
                browseId = "MPREb_album1",
                playlistId = "OLAK5uy_album1",
                title = "Abbey Road",
                artists = listOf(Artist(name = "The Beatles", id = "beatles")),
                year = 1969,
                thumbnail = "https://example.com/abbey.jpg",
            ),
            AlbumItem(
                browseId = "MPREb_album2",
                playlistId = "OLAK5uy_album2",
                title = "Dark Side of the Moon",
                artists = listOf(Artist(name = "Pink Floyd", id = "floyd")),
                year = 1973,
                thumbnail = "https://example.com/darksidethemoon.jpg",
            ),
        )

        val viewModel = LibraryViewModel(
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylists = { Result.success(mockPlaylists.filter { it.id != "SE" && !it.id.startsWith("SS") }) },
            fetchAlbums = { Result.success(mockAlbums) },
            isCookiePresent = { true },
        )

        val state = viewModel.state.value
        assertTrue("Should be logged in", state.isLoggedIn)
        assertFalse("Should finish loading", state.isLoading)
        assertNull("Error should be null on success", state.error)

        // Verify playlist mapping and filtering
        assertEquals(2, state.playlists.size)
        assertEquals("PL_USER_1", state.playlists[0].id)
        assertEquals("Rock Favorites", state.playlists[0].title)
        assertEquals("PL_USER_2", state.playlists[1].id)
        assertEquals("Chill Beats", state.playlists[1].title)

        // Verify album mapping
        assertEquals(2, state.albums.size)
        assertEquals("Abbey Road", state.albums[0].title)
        assertEquals("OLAK5uy_album1", state.albums[0].playlistId)
        assertEquals("Dark Side of the Moon", state.albums[1].title)
    }

    @Test
    fun testFailurePopulatesError() = runBlocking {
        val viewModel = LibraryViewModel(
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylists = { Result.failure(RuntimeException("Network error connecting to InnerTube")) },
            fetchAlbums = { Result.failure(RuntimeException("Timeout loading albums")) },
            isCookiePresent = { true },
        )

        val state = viewModel.state.value
        assertTrue("Should be logged in", state.isLoggedIn)
        val error = state.error
        assertNotNull("Error should be populated", error)
        assertTrue(error?.contains("Network error") == true || error?.contains("Timeout") == true)
        assertTrue("Playlists should remain empty", state.playlists.isEmpty())
        assertTrue("Albums should remain empty", state.albums.isEmpty())
    }

    @Test
    fun testRefreshTransitionsFromLoggedOutToLoggedIn() = runBlocking {
        var cookieAvailable = false

        val viewModel = LibraryViewModel(
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylists = {
                Result.success(
                    listOf(
                        PlaylistItem(
                            id = "PL_NEW",
                            title = "Refreshed Playlist",
                            author = null,
                            songCountText = "5 tracks",
                            thumbnail = null,
                            playEndpoint = null,
                            shuffleEndpoint = null,
                            radioEndpoint = null,
                        )
                    )
                )
            },
            fetchAlbums = { Result.success(emptyList()) },
            isCookiePresent = { cookieAvailable },
        )

        assertFalse(viewModel.state.value.isLoggedIn)
        assertTrue(viewModel.state.value.playlists.isEmpty())

        // User logs in on phone and syncs credentials
        cookieAvailable = true
        viewModel.refresh()

        assertTrue(viewModel.state.value.isLoggedIn)
        assertEquals(1, viewModel.state.value.playlists.size)
        assertEquals("Refreshed Playlist", viewModel.state.value.playlists[0].title)
    }
}
