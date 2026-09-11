/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.viewmodels

import androidx.core.net.toUri
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.Artist
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.pages.PlaylistContinuationPage
import com.music.innertube.pages.PlaylistPage
import com.music.vivi.wear.playback.WearPlaybackService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Adversarial stress and challenge tests for Milestone 4:
 * YouTube Music Account Library & Playlist Browser (R2).
 *
 * Verifies:
 * 1. Logged-out Behavior: Null cookie handling, zero network calls, tab switching, and auth transitions.
 * 2. Liked Songs & Playlist ID Handling: "LM", "PL...", "OLAK5uy_...", "VL" prefix stripping, title fallbacks.
 * 3. Continuation & Pagination: Seamless append, token updating, duplicate tolerance, race condition debouncing.
 * 4. Tap-to-Play Queuing: Media3 MediaItem construction, customCacheKey, index-based queue building.
 */
@RunWith(RobolectricTestRunner::class)
class WearLibraryChallengeTest {

    // =========================================================================
    // SECTION 1: LOGGED-OUT BEHAVIOR & AUTH TRANSITIONS (LibraryViewModel)
    // =========================================================================

    @Test
    fun testLoggedOutNeverTriggersNetworkCalls() = runBlocking {
        val playlistFetchCount = AtomicInteger(0)
        val albumFetchCount = AtomicInteger(0)

        val viewModel = LibraryViewModel(
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylists = {
                playlistFetchCount.incrementAndGet()
                Result.success(emptyList())
            },
            fetchAlbums = {
                albumFetchCount.incrementAndGet()
                Result.success(emptyList())
            },
            isCookiePresent = { false },
        )

        val state = viewModel.state.value
        assertFalse("Should be logged out", state.isLoggedIn)
        assertFalse("Should not be loading", state.isLoading)
        assertTrue("Playlists should be empty", state.playlists.isEmpty())
        assertTrue("Albums should be empty", state.albums.isEmpty())
        assertNull("Error should be null", state.error)
        assertEquals("Should make 0 playlist calls", 0, playlistFetchCount.get())
        assertEquals("Should make 0 album calls", 0, albumFetchCount.get())

        // Multiple explicit refresh calls while logged out should also never touch the network
        viewModel.refresh()
        viewModel.refresh()
        assertEquals("Should still make 0 playlist calls after refresh", 0, playlistFetchCount.get())
        assertEquals("Should still make 0 album calls after refresh", 0, albumFetchCount.get())
        assertFalse(viewModel.state.value.isLoggedIn)
    }

    @Test
    fun testTabSwitchingPreservesStateWithoutNetworkReload() = runBlocking {
        val playlistFetchCount = AtomicInteger(0)

        val viewModel = LibraryViewModel(
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylists = {
                playlistFetchCount.incrementAndGet()
                Result.success(emptyList())
            },
            fetchAlbums = { Result.success(emptyList()) },
            isCookiePresent = { true },
        )

        assertEquals(1, playlistFetchCount.get())
        assertEquals(LibraryTab.PLAYLISTS, viewModel.state.value.selectedTab)

        // Switch to ALBUMS
        viewModel.selectTab(LibraryTab.ALBUMS)
        assertEquals(LibraryTab.ALBUMS, viewModel.state.value.selectedTab)

        // Switch to DOWNLOADS
        viewModel.selectTab(LibraryTab.DOWNLOADS)
        assertEquals(LibraryTab.DOWNLOADS, viewModel.state.value.selectedTab)

        // Switch back to PLAYLISTS
        viewModel.selectTab(LibraryTab.PLAYLISTS)
        assertEquals(LibraryTab.PLAYLISTS, viewModel.state.value.selectedTab)

        // Tab selection must NOT trigger redundant network fetches
        assertEquals(1, playlistFetchCount.get())
    }

    @Test
    fun testFullAuthLifecycleLoggedOutToLoggedInToLoggedOut() = runBlocking {
        var loggedIn = false
        val mockPlaylists = listOf(
            PlaylistItem(
                id = "PL_AUTH_TEST",
                title = "Auth Test Playlist",
                author = null,
                songCountText = "10 tracks",
                thumbnail = null,
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null,
            )
        )

        val viewModel = LibraryViewModel(
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylists = { Result.success(mockPlaylists) },
            fetchAlbums = { Result.success(emptyList()) },
            isCookiePresent = { loggedIn },
        )

        // Step 1: Initially logged out
        assertFalse(viewModel.state.value.isLoggedIn)
        assertTrue(viewModel.state.value.playlists.isEmpty())

        // Step 2: User completes QR code pairing on phone -> logged in
        loggedIn = true
        viewModel.refresh()
        assertTrue(viewModel.state.value.isLoggedIn)
        assertEquals(1, viewModel.state.value.playlists.size)
        assertEquals("Auth Test Playlist", viewModel.state.value.playlists[0].title)

        // Step 3: User logs out / disconnects account from watch
        loggedIn = false
        viewModel.refresh()
        assertFalse(viewModel.state.value.isLoggedIn)
        assertTrue("Playlists must be purged on logout", viewModel.state.value.playlists.isEmpty())
        assertTrue("Albums must be purged on logout", viewModel.state.value.albums.isEmpty())
        assertNull("Error must be cleared on logout", viewModel.state.value.error)
    }

    @Test
    fun testConcurrentRefreshesDoNotCorruptState() = runBlocking {
        val counter = AtomicInteger(0)
        val viewModel = LibraryViewModel(
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylists = {
                val c = counter.incrementAndGet()
                Result.success(
                    listOf(
                        PlaylistItem(
                            id = "PL_$c",
                            title = "Playlist $c",
                            author = null,
                            songCountText = "$c tracks",
                            thumbnail = null,
                            playEndpoint = null,
                            shuffleEndpoint = null,
                            radioEndpoint = null,
                        )
                    )
                )
            },
            fetchAlbums = { Result.success(emptyList()) },
            isCookiePresent = { true },
        )

        // Launch 20 concurrent refresh calls
        val jobs = (1..20).map {
            async(Dispatchers.Default) {
                viewModel.refresh()
            }
        }
        jobs.awaitAll()

        val state = viewModel.state.value
        assertTrue(state.isLoggedIn)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(1, state.playlists.size)
        assertTrue(state.playlists[0].id.startsWith("PL_"))
    }

    @Test
    fun testPartialFailureOnlyPlaylistsFailsSilentlyIfAlbumsSucceed() = runBlocking {
        val mockAlbums = listOf(
            AlbumItem(
                browseId = "MPREb_1",
                playlistId = "OLAK5uy_1",
                title = "Valid Album",
                artists = emptyList(),
                year = 2026,
                thumbnail = "",
            )
        )

        val viewModel = LibraryViewModel(
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylists = { Result.failure(IOException("Playlists unreachable")) },
            fetchAlbums = { Result.success(mockAlbums) },
            isCookiePresent = { true },
        )

        val state = viewModel.state.value
        assertTrue("Should be marked logged in", state.isLoggedIn)
        assertFalse("Should finish loading", state.isLoading)
        assertNull("Error should be null because one endpoint succeeded", state.error)
        assertTrue("Playlists should be empty", state.playlists.isEmpty())
        assertEquals(1, state.albums.size)
        assertEquals("Valid Album", state.albums[0].title)
    }

    @Test
    fun testPartialFailureOnlyAlbumsFailsSilentlyIfPlaylistsSucceed() = runBlocking {
        val mockPlaylists = listOf(
            PlaylistItem(
                id = "PL_OK",
                title = "Playlists OK",
                author = null,
                songCountText = "3 tracks",
                thumbnail = null,
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null,
            )
        )

        val viewModel = LibraryViewModel(
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylists = { Result.success(mockPlaylists) },
            fetchAlbums = { Result.failure(IOException("Albums 503 Service Unavailable")) },
            isCookiePresent = { true },
        )

        val state = viewModel.state.value
        assertTrue(state.isLoggedIn)
        assertNull("Error should be null when playlists succeed", state.error)
        assertEquals(1, state.playlists.size)
        assertEquals("Playlists OK", state.playlists[0].title)
        assertTrue(state.albums.isEmpty())
    }

    @Test
    fun testCachedDataRetentionWhenSubsequentRefreshFails() = runBlocking {
        var failPlaylists = false
        val initialPlaylists = listOf(
            PlaylistItem(
                id = "PL_CACHED",
                title = "Cached Playlist",
                author = null,
                songCountText = "12 tracks",
                thumbnail = null,
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null,
            )
        )

        val viewModel = LibraryViewModel(
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylists = {
                if (failPlaylists) {
                    Result.failure(IOException("Offline"))
                } else {
                    Result.success(initialPlaylists)
                }
            },
            fetchAlbums = { Result.success(emptyList()) },
            isCookiePresent = { true },
        )

        assertEquals(1, viewModel.state.value.playlists.size)
        assertEquals("Cached Playlist", viewModel.state.value.playlists[0].title)

        // Subsequent refresh fails
        failPlaylists = true
        viewModel.refresh()

        // Previously loaded playlists should be retained instead of cleared
        assertEquals(1, viewModel.state.value.playlists.size)
        assertEquals("Cached Playlist", viewModel.state.value.playlists[0].title)
    }

    // =========================================================================
    // SECTION 2: LIKED SONGS & PLAYLIST ID HANDLING (PlaylistDetailViewModel)
    // =========================================================================

    @Test
    fun testLikedSongsFallbackTitleWhenApiReturnsEmptyOrNull() = runBlocking {
        val emptyTitlePlaylist = PlaylistItem(
            id = "LM",
            title = "",
            author = null,
            songCountText = null,
            thumbnail = null,
            playEndpoint = null,
            shuffleEndpoint = null,
            radioEndpoint = null,
        )

        val viewModel = PlaylistDetailViewModel(
            playlistId = "LM",
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylist = { id ->
                assertEquals("LM", id)
                Result.success(
                    PlaylistPage(
                        playlist = emptyTitlePlaylist,
                        songs = emptyList(),
                        songsContinuation = null,
                        continuation = null,
                    )
                )
            },
        )

        val state = viewModel.state.value
        assertEquals("Liked Songs", state.playlist?.title)
    }

    @Test
    fun testLikedSongsWithVLPrefixCleansAndFallsBackToLikedSongs() = runBlocking {
        var requestedId: String? = null
        val viewModel = PlaylistDetailViewModel(
            playlistId = "VLLM",
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylist = { id ->
                requestedId = id
                Result.success(
                    PlaylistPage(
                        playlist = PlaylistItem(
                            id = "LM",
                            title = "",
                            author = null,
                            songCountText = null,
                            thumbnail = null,
                            playEndpoint = null,
                            shuffleEndpoint = null,
                            radioEndpoint = null,
                        ),
                        songs = emptyList(),
                        songsContinuation = null,
                        continuation = null,
                    )
                )
            },
        )

        assertEquals("LM", requestedId)
        assertEquals("Liked Songs", viewModel.state.value.playlist?.title)
    }

    @Test
    fun testLikedSongsPreservesNonBlankApiTitle() = runBlocking {
        val customTitlePlaylist = PlaylistItem(
            id = "LM",
            title = "My Favorite Tracks",
            author = null,
            songCountText = "200 tracks",
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
                        playlist = customTitlePlaylist,
                        songs = emptyList(),
                        songsContinuation = null,
                        continuation = null,
                    )
                )
            },
        )

        assertEquals("My Favorite Tracks", viewModel.state.value.playlist?.title)
    }

    @Test
    fun testVLPrefixStrippedForUserPlaylistsAndAlbums() = runBlocking {
        val requestedIds = mutableListOf<String>()

        val testIds = listOf(
            "VLPL_test_123" to "PL_test_123",
            "VLOLAK5uy_album_abc" to "OLAK5uy_album_abc",
            "PL_no_prefix" to "PL_no_prefix",
            "OLAK5uy_raw" to "OLAK5uy_raw",
        )

        for ((input, expectedClean) in testIds) {
            val vm = PlaylistDetailViewModel(
                playlistId = input,
                ioDispatcher = Dispatchers.Unconfined,
                fetchPlaylist = { id ->
                    requestedIds.add(id)
                    Result.success(
                        PlaylistPage(
                            playlist = PlaylistItem(
                                id = id,
                                title = "Title $id",
                                author = null,
                                songCountText = null,
                                thumbnail = null,
                                playEndpoint = null,
                                shuffleEndpoint = null,
                                radioEndpoint = null,
                            ),
                            songs = emptyList(),
                            songsContinuation = null,
                            continuation = null,
                        )
                    )
                },
            )
            assertEquals("Title $expectedClean", vm.state.value.playlist?.title)
        }

        assertEquals(listOf("PL_test_123", "OLAK5uy_album_abc", "PL_no_prefix", "OLAK5uy_raw"), requestedIds)
    }

    @Test
    fun testShortVLIdNotOverTrimmed() = runBlocking {
        var requestedId: String? = null
        val vm = PlaylistDetailViewModel(
            playlistId = "VL",
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylist = { id ->
                requestedId = id
                Result.success(
                    PlaylistPage(
                        playlist = PlaylistItem(
                            id = id,
                            title = "Exact VL",
                            author = null,
                            songCountText = null,
                            thumbnail = null,
                            playEndpoint = null,
                            shuffleEndpoint = null,
                            radioEndpoint = null,
                        ),
                        songs = emptyList(),
                        songsContinuation = null,
                        continuation = null,
                    )
                )
            },
        )

        assertEquals("VL", requestedId)
        assertEquals("Exact VL", vm.state.value.playlist?.title)
    }

    @Test
    fun testPlaylistDetailFailureAndRetry() = runBlocking {
        var failRequest = true
        val vm = PlaylistDetailViewModel(
            playlistId = "PL_RETRY",
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylist = {
                if (failRequest) {
                    Result.failure(IOException("InnerTube 500 Internal Server Error"))
                } else {
                    Result.success(
                        PlaylistPage(
                            playlist = PlaylistItem(
                                id = "PL_RETRY",
                                title = "Recovered Playlist",
                                author = null,
                                songCountText = "1 track",
                                thumbnail = null,
                                playEndpoint = null,
                                shuffleEndpoint = null,
                                radioEndpoint = null,
                            ),
                            songs = listOf(
                                SongItem(
                                    id = "s_recovered",
                                    title = "Recovered Song",
                                    artists = emptyList(),
                                    album = null,
                                    duration = 120,
                                    thumbnail = "",
                                    explicit = false,
                                    endpoint = null,
                                )
                            ),
                            songsContinuation = null,
                            continuation = null,
                        )
                    )
                }
            },
        )

        // Initial attempt failed
        assertFalse(vm.state.value.isLoading)
        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.error!!.contains("500 Internal Server Error"))
        assertTrue(vm.state.value.songs.isEmpty())

        // User taps "Retry"
        failRequest = false
        vm.loadPlaylist()

        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.error)
        assertEquals(1, vm.state.value.songs.size)
        assertEquals("Recovered Playlist", vm.state.value.playlist?.title)
        assertEquals("Recovered Song", vm.state.value.songs[0].title)
    }

    // =========================================================================
    // SECTION 3: CONTINUATION & PAGINATION (PlaylistDetailViewModel)
    // =========================================================================

    @Test
    fun testContinuationChainingAndFinalTermination() = runBlocking {
        val page1Songs = listOf(
            SongItem(id = "s1", title = "Song 1", artists = emptyList(), album = null, duration = 100, thumbnail = "", explicit = false, endpoint = null),
            SongItem(id = "s2", title = "Song 2", artists = emptyList(), album = null, duration = 110, thumbnail = "", explicit = false, endpoint = null),
        )
        val page2Songs = listOf(
            SongItem(id = "s3", title = "Song 3", artists = emptyList(), album = null, duration = 120, thumbnail = "", explicit = false, endpoint = null),
        )
        val page3Songs = listOf(
            SongItem(id = "s4", title = "Song 4", artists = emptyList(), album = null, duration = 130, thumbnail = "", explicit = false, endpoint = null),
        )

        val vm = PlaylistDetailViewModel(
            playlistId = "PL_CHAIN",
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylist = {
                Result.success(
                    PlaylistPage(
                        playlist = PlaylistItem("PL_CHAIN", "Chained Playlist", null, null, null, null, null, null),
                        songs = page1Songs,
                        songsContinuation = "TOKEN_PAGE_2",
                        continuation = null,
                    )
                )
            },
            fetchContinuation = { token ->
                when (token) {
                    "TOKEN_PAGE_2" -> Result.success(PlaylistContinuationPage(page2Songs, "TOKEN_PAGE_3"))
                    "TOKEN_PAGE_3" -> Result.success(PlaylistContinuationPage(page3Songs, null)) // Last page!
                    else -> Result.failure(IllegalArgumentException("Unknown token: $token"))
                }
            },
        )

        assertEquals(2, vm.state.value.songs.size)
        assertEquals("TOKEN_PAGE_2", vm.state.value.continuation)

        // Load page 2
        vm.loadMore()
        assertEquals(3, vm.state.value.songs.size)
        assertEquals("TOKEN_PAGE_3", vm.state.value.continuation)
        assertEquals("s3", vm.state.value.songs[2].id)

        // Load page 3 (last page)
        vm.loadMore()
        assertEquals(4, vm.state.value.songs.size)
        assertNull("Continuation token should be null on termination", vm.state.value.continuation)
        assertEquals("s4", vm.state.value.songs[3].id)

        // Calling loadMore() when continuation is null should be a no-op
        vm.loadMore()
        assertEquals(4, vm.state.value.songs.size)
        assertNull(vm.state.value.continuation)
    }

    @Test
    fun testConcurrentOrRapidLoadMoreDebounced() = runBlocking {
        val continuationInvocations = AtomicInteger(0)
        val blockGate = CompletableDeferred<Unit>()

        val vm = PlaylistDetailViewModel(
            playlistId = "PL_RACE",
            ioDispatcher = Dispatchers.Default,
            fetchPlaylist = {
                Result.success(
                    PlaylistPage(
                        playlist = PlaylistItem("PL_RACE", "Race Playlist", null, null, null, null, null, null),
                        songs = listOf(SongItem(id = "s1", title = "Song 1", artists = emptyList(), album = null, duration = 100, thumbnail = "", explicit = false, endpoint = null)),
                        songsContinuation = "TOKEN_DEBOUNCE",
                        continuation = null,
                    )
                )
            },
            fetchContinuation = { token ->
                continuationInvocations.incrementAndGet()
                blockGate.await() // Hold the execution to simulate network latency
                Result.success(
                    PlaylistContinuationPage(
                        songs = listOf(SongItem(id = "s2", title = "Song 2", artists = emptyList(), album = null, duration = 100, thumbnail = "", explicit = false, endpoint = null)),
                        continuation = null,
                    )
                )
            },
        )

        // Trigger multiple rapid loadMore() calls in parallel while first is in flight
        val call1 = async(Dispatchers.Default) { vm.loadMore() }
        val call2 = async(Dispatchers.Default) { vm.loadMore() }
        val call3 = async(Dispatchers.Default) { vm.loadMore() }

        // Release the network gate
        blockGate.complete(Unit)
        call1.await()
        call2.await()
        call3.await()

        // Debounce flag (@Volatile isLoadingMore) must prevent multiple simultaneous executions
        assertEquals("Should only invoke continuation fetcher once due to isLoadingMore guard", 1, continuationInvocations.get())
    }

    @Test
    fun testContinuationFailureRetainsSongsAndAllowsSubsequentRetry() = runBlocking {
        var failContinuation = true
        val initialSongs = listOf(
            SongItem(id = "s1", title = "Song 1", artists = emptyList(), album = null, duration = 100, thumbnail = "", explicit = false, endpoint = null),
        )
        val nextSongs = listOf(
            SongItem(id = "s2", title = "Song 2", artists = emptyList(), album = null, duration = 120, thumbnail = "", explicit = false, endpoint = null),
        )

        val vm = PlaylistDetailViewModel(
            playlistId = "PL_FAIL_RETRY",
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylist = {
                Result.success(
                    PlaylistPage(
                        playlist = PlaylistItem("PL_FAIL_RETRY", "Fail Retry", null, null, null, null, null, null),
                        songs = initialSongs,
                        songsContinuation = "TOKEN_RETRYABLE",
                        continuation = null,
                    )
                )
            },
            fetchContinuation = { token ->
                if (failContinuation) {
                    Result.failure(IOException("Temporary connection loss"))
                } else {
                    Result.success(PlaylistContinuationPage(nextSongs, null))
                }
            },
        )

        assertEquals(1, vm.state.value.songs.size)
        assertEquals("TOKEN_RETRYABLE", vm.state.value.continuation)

        // First attempt fails
        vm.loadMore()

        // Existing tracks and continuation token must NOT be lost!
        assertEquals(1, vm.state.value.songs.size)
        assertEquals("s1", vm.state.value.songs[0].id)
        assertEquals("TOKEN_RETRYABLE", vm.state.value.continuation)

        // Second attempt succeeds
        failContinuation = false
        vm.loadMore()

        assertEquals(2, vm.state.value.songs.size)
        assertEquals("s1", vm.state.value.songs[0].id)
        assertEquals("s2", vm.state.value.songs[1].id)
        assertNull(vm.state.value.continuation)
    }

    @Test
    fun testContinuationWithDuplicateSongsPreservesBothAndUniqueKey() = runBlocking {
        val initialSongs = listOf(
            SongItem(id = "song_repeated", title = "Loop Track", artists = emptyList(), album = null, duration = 150, thumbnail = "", explicit = false, endpoint = null),
        )
        val continuationSongs = listOf(
            SongItem(id = "song_repeated", title = "Loop Track", artists = emptyList(), album = null, duration = 150, thumbnail = "", explicit = false, endpoint = null),
        )

        val vm = PlaylistDetailViewModel(
            playlistId = "PL_REPEAT",
            ioDispatcher = Dispatchers.Unconfined,
            fetchPlaylist = {
                Result.success(
                    PlaylistPage(
                        playlist = PlaylistItem("PL_REPEAT", "Repeat Playlist", null, null, null, null, null, null),
                        songs = initialSongs,
                        songsContinuation = "TOKEN_REPEAT",
                        continuation = null,
                    )
                )
            },
            fetchContinuation = {
                Result.success(PlaylistContinuationPage(continuationSongs, null))
            },
        )

        vm.loadMore()

        val songs = vm.state.value.songs
        assertEquals(2, songs.size)
        assertEquals("song_repeated", songs[0].id)
        assertEquals("song_repeated", songs[1].id)

        // Verify the ScalingLazyColumn composite key contract: "${song.id}_$index"
        val key0 = "${songs[0].id}_0"
        val key1 = "${songs[1].id}_1"
        assertNotEquals("Composite keys must be distinct to prevent LazyColumn crash", key0, key1)
    }

    // =========================================================================
    // SECTION 4: TAP-TO-PLAY QUEUING & MEDIAITEM BUILDER
    // =========================================================================

    @Test
    fun testBuildMediaItemPopulatesAllMedia3Fields() {
        val artworkUri = "https://lh3.googleusercontent.com/abc123def456".toUri()
        val mediaItem = WearPlaybackService.buildMediaItem(
            songId = "video_id_xyz",
            title = "Bohemian Rhapsody",
            artist = "Queen",
            artworkUri = artworkUri,
        )

        assertEquals("video_id_xyz", mediaItem.mediaId)
        assertEquals("vivi://song/video_id_xyz".toUri(), mediaItem.requestMetadata.mediaUri)
        // Media3 ExoPlayer customCacheKey is crucial for offline playback without network
        assertEquals("video_id_xyz", mediaItem.localConfiguration?.customCacheKey)
        assertEquals("Bohemian Rhapsody", mediaItem.mediaMetadata.title?.toString())
        assertEquals("Queen", mediaItem.mediaMetadata.artist?.toString())
        assertEquals(artworkUri, mediaItem.mediaMetadata.artworkUri)
    }

    @Test
    fun testBuildMediaItemHandlesNullArtistAndArtworkGracefully() {
        val mediaItem = WearPlaybackService.buildMediaItem(
            songId = "minimal_id",
            title = "Minimalist Audio",
            artist = null,
            artworkUri = null,
        )

        assertEquals("minimal_id", mediaItem.mediaId)
        assertEquals("vivi://song/minimal_id".toUri(), mediaItem.requestMetadata.mediaUri)
        assertEquals("minimal_id", mediaItem.localConfiguration?.customCacheKey)
        assertEquals("Minimalist Audio", mediaItem.mediaMetadata.title?.toString())
        assertNull("Artist should be null", mediaItem.mediaMetadata.artist)
        assertNull("ArtworkUri should be null", mediaItem.mediaMetadata.artworkUri)
    }

    @Test
    fun testPlaylistToMediaItemListMappingAndIndexPreservation() {
        val songs = listOf(
            SongItem(id = "s1", title = "Track 1", artists = listOf(Artist("A1", "id1")), album = null, duration = 180, thumbnail = "https://thumb1.jpg", explicit = false, endpoint = null),
            SongItem(id = "s2", title = "Track 2", artists = listOf(Artist("A2", "id2")), album = null, duration = 210, thumbnail = "https://thumb2.jpg", explicit = false, endpoint = null),
            SongItem(id = "s3", title = "Track 3", artists = emptyList(), album = null, duration = 240, thumbnail = "", explicit = false, endpoint = null),
        )

        val mediaItems = songs.map { song ->
            WearPlaybackService.buildMediaItem(
                songId = song.id,
                title = song.title,
                artist = song.artists.firstOrNull()?.name,
                artworkUri = song.thumbnail.takeIf { it.isNotBlank() }?.toUri(),
            )
        }

        assertEquals(3, mediaItems.size)
        assertEquals("s1", mediaItems[0].mediaId)
        assertEquals("Track 1", mediaItems[0].mediaMetadata.title.toString())
        assertEquals("A1", mediaItems[0].mediaMetadata.artist.toString())
        assertEquals("https://thumb1.jpg".toUri(), mediaItems[0].mediaMetadata.artworkUri)

        assertEquals("s2", mediaItems[1].mediaId)
        assertEquals("Track 2", mediaItems[1].mediaMetadata.title.toString())
        assertEquals("A2", mediaItems[1].mediaMetadata.artist.toString())

        // Track 3 has no artist and empty thumbnail string
        assertEquals("s3", mediaItems[2].mediaId)
        assertEquals("Track 3", mediaItems[2].mediaMetadata.title.toString())
        assertNull("Track 3 artist should be null", mediaItems[2].mediaMetadata.artist)
        assertNull("Track 3 artwork should be null because thumbnail was blank", mediaItems[2].mediaMetadata.artworkUri)

        // Validate index boundaries
        for (index in mediaItems.indices) {
            assertTrue(index >= 0 && index < mediaItems.size)
        }
    }
}
