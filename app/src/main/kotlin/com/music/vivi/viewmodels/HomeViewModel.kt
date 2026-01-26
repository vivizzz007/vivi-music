package com.music.vivi.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.innertube.pages.ExplorePage
import com.music.innertube.pages.HomePage
import com.music.innertube.utils.completed
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.HideVideoSongsKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.QuickPicks
import com.music.vivi.constants.QuickPicksKey
import com.music.vivi.constants.YtmSyncKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.LocalItem
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toEnum
import com.music.vivi.models.SimilarRecommendation
import com.music.vivi.utils.SyncUtils
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home Screen.
 *
 * Responsibilities:
 * - Aggregates various content sections: Quick Picks, Forgotten Favorites, Keep Listening.
 * - Fetches "Similar Recommendations" based on local listening habits (Artists/Songs).
 * - Loads the remote YouTube Music Home/Explore pages.
 * - Manages user account state (Avatar, Name, Login status).
 */
@HiltViewModel
public class HomeViewModel @Inject constructor(
    @ApplicationContext public val context: Context,
    public val database: MusicDatabase,
    public val syncUtils: SyncUtils,
) : ViewModel() {
    public val isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val quickPicksEnum = context.dataStore.data.map {
        it[QuickPicksKey].toEnum(QuickPicks.QUICK_PICKS)
    }.distinctUntilChanged()

    private val _quickPicks = MutableStateFlow<List<Song>?>(null)
    public val quickPicks: StateFlow<List<Song>?> = _quickPicks.asStateFlow()

    private val _forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    public val forgottenFavorites: StateFlow<List<Song>?> = _forgottenFavorites.asStateFlow()

    private val _keepListening = MutableStateFlow<List<LocalItem>?>(null)
    public val keepListening: StateFlow<List<LocalItem>?> = _keepListening.asStateFlow()

    private val _similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    public val similarRecommendations: StateFlow<List<SimilarRecommendation>?> = _similarRecommendations.asStateFlow()

    private val _accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    public val accountPlaylists: StateFlow<List<PlaylistItem>?> = _accountPlaylists.asStateFlow()

    private val _homePage = MutableStateFlow<HomePage?>(null)
    public val homePage: StateFlow<HomePage?> = _homePage.asStateFlow()

    private val _explorePage = MutableStateFlow<ExplorePage?>(null)
    public val explorePage: StateFlow<ExplorePage?> = _explorePage.asStateFlow()

    private val _selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    public val selectedChip: StateFlow<HomePage.Chip?> = _selectedChip.asStateFlow()

    private val previousHomePage = MutableStateFlow<HomePage?>(null)

    private val _allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    public val allLocalItems: StateFlow<List<LocalItem>> = _allLocalItems.asStateFlow()

    private val _allYtItems = MutableStateFlow<List<YTItem>>(emptyList())
    public val allYtItems: StateFlow<List<YTItem>> = _allYtItems.asStateFlow()

    public val accountName: MutableStateFlow<String> = MutableStateFlow("Guest")
    public val accountImageUrl: MutableStateFlow<String?> = MutableStateFlow(null)
    public val isLoggedIn: MutableStateFlow<Boolean> = MutableStateFlow(false)

    // Track last processed cookie to avoid unnecessary updates
    private var lastProcessedCookie: String? = null

    // Track if we're currently processing account data
    private var isProcessingAccountData = false

    private suspend fun getQuickPicks() {
        when (quickPicksEnum.first()) {
            QuickPicks.QUICK_PICKS -> _quickPicks.value = database.quickPicks().first().shuffled().take(20)
            QuickPicks.LAST_LISTEN -> {
                val song = database.events().first().firstOrNull()?.song
                if (song != null && database.hasRelatedSongs(song.id)) {
                    _quickPicks.value = database.getRelatedSongs(song.id).first().shuffled().take(20)
                }
            }
        }
    }

    private suspend fun load() {
        isLoading.value = true
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

        getQuickPicks()
        _forgottenFavorites.value = database.forgottenFavorites().first().shuffled().take(20)

        val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2
        val keepListeningSongs = database.mostPlayedSongs(
            fromTimeStamp,
            limit = 15,
            offset = 5
        ).first().shuffled().take(10)
        val keepListeningAlbums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2).first().filter {
            it.album.thumbnailUrl !=
                null
        }.shuffled().take(5)
        val keepListeningArtists = database.mostPlayedArtists(fromTimeStamp).first().filter {
            it.artist.isYouTubeArtist &&
                it.artist.thumbnailUrl != null
        }.shuffled().take(5)
        _keepListening.value = (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()

        if (YouTube.cookie != null) {
            YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
                _accountPlaylists.value = it.items.filterIsInstance<PlaylistItem>().filterNot { it.id == "SE" }
            }.onFailure {
                reportException(it)
            }
        }

        val artistRecommendations = database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
            .filter { it.artist.isYouTubeArtist }
            .shuffled().take(3)
            .mapNotNull {
                val items = mutableListOf<YTItem>()
                YouTube.artist(it.id).onSuccess { page ->
                    items += page.sections.getOrNull(page.sections.size - 2)?.items.orEmpty()
                    items += page.sections.lastOrNull()?.items.orEmpty()
                }
                SimilarRecommendation(
                    title = it,
                    items = items
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .shuffled()
                        .ifEmpty { return@mapNotNull null }
                )
            }

        val songRecommendations = database.mostPlayedSongs(fromTimeStamp, limit = 10).first()
            .filter { it.album != null }
            .shuffled().take(2)
            .mapNotNull { song ->
                val endpoint =
                    YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint
                        ?: return@mapNotNull null
                val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                SimilarRecommendation(
                    title = song,
                    items = (
                        page.songs.shuffled().take(8) +
                            page.albums.shuffled().take(4) +
                            page.artists.shuffled().take(4) +
                            page.playlists.shuffled().take(4)
                        )
                        .filterExplicit(hideExplicit)
                        .shuffled()
                        .ifEmpty { return@mapNotNull null }
                )
            }
        _similarRecommendations.value = (artistRecommendations + songRecommendations).shuffled()

        YouTube.home().onSuccess { page ->
            _homePage.value = page.copy(
                sections = page.sections.map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs))
                }
            )
        }.onFailure {
            reportException(it)
        }

        YouTube.explore().onSuccess { page ->
            _explorePage.value = page.copy(
                newReleaseAlbums = page.newReleaseAlbums.filterExplicit(hideExplicit)
            )
        }.onFailure {
            reportException(it)
        }

        _allLocalItems.value =
            (_quickPicks.value.orEmpty() + _forgottenFavorites.value.orEmpty() + _keepListening.value.orEmpty())
                .filter { it is Song || it is Album }
        _allYtItems.value = _similarRecommendations.value?.flatMap { it.items }.orEmpty() +
            _homePage.value?.sections?.flatMap { it.items }.orEmpty()

        isLoading.value = false
    }

    private val _isLoadingMore = MutableStateFlow(false)
    public fun loadMoreYouTubeItems(continuation: String?) {
        if (continuation == null || _isLoadingMore.value) return
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMore.value = true
            val nextSections = YouTube.home(continuation).getOrNull() ?: run {
                _isLoadingMore.value = false
                return@launch
            }

            _homePage.value = nextSections.copy(
                chips = _homePage.value?.chips,
                sections = (_homePage.value?.sections.orEmpty() + nextSections.sections).map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs))
                }
            )
            _isLoadingMore.value = false
        }
    }

    public fun toggleChip(chip: HomePage.Chip?) {
        if (chip == null || chip == _selectedChip.value && previousHomePage.value != null) {
            _homePage.value = previousHomePage.value
            previousHomePage.value = null
            _selectedChip.value = null
            return
        }

        if (_selectedChip.value == null) {
            previousHomePage.value = _homePage.value
        }

        viewModelScope.launch(Dispatchers.IO) {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val nextSections = YouTube.home(params = chip?.endpoint?.params).getOrNull() ?: return@launch

            _homePage.value = nextSections.copy(
                chips = _homePage.value?.chips,
                sections = nextSections.sections.map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs))
                }
            )
            _selectedChip.value = chip
        }
    }

    public fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            load()
            isRefreshing.value = false
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .first()

            load()

            val isSyncEnabled = context.dataStore.get(YtmSyncKey, true)
            if (isSyncEnabled) {
                syncUtils.runAllSyncs()
            }
        }

        // Listen for cookie changes and reload account data
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .collect { cookie ->
                    // Avoid processing if already processing
                    if (isProcessingAccountData) return@collect

                    // Always process cookie changes, even if same value (for logout/login scenarios)
                    lastProcessedCookie = cookie
                    isProcessingAccountData = true

                    try {
                        if (cookie != null && cookie.isNotEmpty()) {

                            // Update YouTube.cookie manually to ensure it's set
                            YouTube.cookie = cookie

                            // Fetch new account data
                            YouTube.accountInfo().onSuccess { info ->
                                accountName.value = info.name
                                accountImageUrl.value = info.thumbnailUrl
                                isLoggedIn.value = true
                            }.onFailure {
                                if (it.message != "Active account info not found in header") {
                                    reportException(it)
                                }
                                // Keep generic logged in state if we have a valid cookie, even if info fetch fails?
                                // Better to assume logged in if we have a cookie, but verified by info fetch is better.
                                // For now, let's set true here if cookie was valid enough to try.
                                isLoggedIn.value = true
                            }
                        } else {
                            accountName.value = "Guest"
                            accountImageUrl.value = null
                            _accountPlaylists.value = null
                            isLoggedIn.value = false
                        }
                    } finally {
                        isProcessingAccountData = false
                    }
                }
        }
    }
}
