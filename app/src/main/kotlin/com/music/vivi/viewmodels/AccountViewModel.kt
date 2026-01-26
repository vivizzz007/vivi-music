package com.music.vivi.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.utils.completed
import com.music.vivi.ui.utils.resize
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Enum defining the types of content displayable in the Account section.
 */
public enum class AccountContentType {
    PLAYLISTS,
    ALBUMS,
    ARTISTS,
}

/**
 * ViewModel for managing the Account/Library dashboard.
 * Fetches and displays user-specific content from YouTube Music (Private Library).
 */
@HiltViewModel
public class AccountViewModel @Inject constructor() : ViewModel() {
    public val playlists: MutableStateFlow<List<PlaylistItem>?> = MutableStateFlow(null)
    public val albums: MutableStateFlow<List<AlbumItem>?> = MutableStateFlow(null)
    public val artists: MutableStateFlow<List<ArtistItem>?> = MutableStateFlow(null)

    // Selected content type for chips
    public val selectedContentType: MutableStateFlow<AccountContentType> =
        MutableStateFlow(AccountContentType.PLAYLISTS)

    init {
        viewModelScope.launch {
            YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
                playlists.value = it.items.filterIsInstance<PlaylistItem>()
                    .filterNot { it.id == "SE" }
            }.onFailure {
                reportException(it)
            }
            YouTube.library("FEmusic_liked_albums").completed().onSuccess {
                albums.value = it.items.filterIsInstance<AlbumItem>()
            }.onFailure {
                reportException(it)
            }
            YouTube.library("FEmusic_library_corpus_artists").completed().onSuccess {
                artists.value = it.items.filterIsInstance<ArtistItem>().map { artist ->
                    artist.copy(
                        thumbnail = artist.thumbnail?.resize(544, 544)
                    )
                }
            }.onFailure {
                reportException(it)
            }
        }
    }

    public fun setSelectedContentType(contentType: AccountContentType) {
        selectedContentType.value = contentType
    }
}
