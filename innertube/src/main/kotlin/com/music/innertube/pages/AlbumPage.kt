/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.innertube.pages

import com.music.innertube.models.Album
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.Artist
import com.music.innertube.models.MusicResponsiveHeaderRenderer
import com.music.innertube.models.MusicResponsiveListItemRenderer
import com.music.innertube.models.SongItem
import com.music.innertube.models.getItems
import com.music.innertube.models.oddElements
import com.music.innertube.models.response.BrowseResponse
import com.music.innertube.models.splitBySeparator
import com.music.innertube.utils.parseTime

data class AlbumPage(
    val album: AlbumItem,
    val songs: List<SongItem>,
    val otherVersions: List<AlbumItem>,
    val releasesForYou: List<AlbumItem> = emptyList(),
    val description: String? = null,
    val descriptionRuns: List<com.music.innertube.models.Run>? = null,
) {
    companion object {
        fun getPlaylistId(response: BrowseResponse): String? {
            val canonicalUrl = response.microformat?.microformatDataRenderer?.urlCanonical
            var playlistId = if (canonicalUrl != null && "list=" in canonicalUrl) {
                canonicalUrl.substringAfter("list=").substringBefore('&')
            } else null

            if (playlistId == null) {
                playlistId = response.header?.musicDetailHeaderRenderer?.menu?.menuRenderer?.topLevelButtons?.firstOrNull()
                    ?.buttonRenderer?.navigationEndpoint?.watchPlaylistEndpoint?.playlistId
            }
            if (playlistId == null) {
                val header = getHeader(response)
                playlistId = header?.buttons?.firstNotNullOfOrNull { button ->
                    button.musicPlayButtonRenderer?.playNavigationEndpoint?.watchPlaylistEndpoint?.playlistId
                        ?: button.menuRenderer?.items?.firstNotNullOfOrNull { item ->
                            item.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint?.playlistId
                        }
                }
            }
            return playlistId
        }

        fun getTitle(response: BrowseResponse): String? {
            val header = getHeader(response)
            return header?.title?.runs?.firstOrNull()?.text
                ?: response.header?.musicDetailHeaderRenderer?.title?.runs?.firstOrNull()?.text
                ?: response.header?.musicImmersiveHeaderRenderer?.title?.runs?.firstOrNull()?.text
                ?: response.header?.musicVisualHeaderRenderer?.title?.runs?.firstOrNull()?.text
                ?: response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text
        }

        fun getYear(response: BrowseResponse): Int? {
            val header = getHeader(response)
            return header?.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull()
                ?: header?.secondSubtitle?.runs?.lastOrNull()?.text?.toIntOrNull()
                ?: response.header?.musicDetailHeaderRenderer?.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull()
                ?: response.header?.musicHeaderRenderer?.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull()
        }

        fun getThumbnail(response: BrowseResponse): String? {
            val header = getHeader(response)
            return header?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: header?.thumbnail?.croppedSquareThumbnailRenderer?.getThumbnailUrl()
                ?: response.background?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: response.header?.musicDetailHeaderRenderer?.thumbnail?.croppedSquareThumbnailRenderer?.getThumbnailUrl()
                ?: response.header?.musicDetailHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: response.header?.musicVisualHeaderRenderer?.foregroundThumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: response.header?.musicImmersiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: response.header?.musicHeaderRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url
        }

        fun getArtists(response: BrowseResponse): List<Artist> {
            val header = getHeader(response)
            val artists = header?.straplineTextOne?.runs?.oddElements()?.map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            } ?: header?.subtitle?.runs?.splitBySeparator()?.firstOrNull()?.oddElements()?.map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            } ?: response.header?.musicDetailHeaderRenderer?.subtitle?.runs?.splitBySeparator()?.getOrNull(1)?.oddElements()?.map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            } ?: emptyList()

            return artists
        }

        private fun getHeader(response: BrowseResponse): MusicResponsiveHeaderRenderer? {
            val tabs = response.contents?.singleColumnBrowseResultsRenderer?.tabs
                ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs
            val section =
                tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            return section?.musicResponsiveHeaderRenderer
                ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull { it.musicResponsiveHeaderRenderer }?.firstOrNull()
                ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull { it.musicResponsiveHeaderRenderer }?.firstOrNull()
                ?: response.header?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer
        }

        fun getSongs(response: BrowseResponse, album: AlbumItem): List<SongItem> {
            val tabs = response.contents?.singleColumnBrowseResultsRenderer?.tabs ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs
            val sectionContents = tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
                ?: response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents
                ?: emptyList()

            val shelfSongs = sectionContents.firstNotNullOfOrNull { content ->
                content.musicPlaylistShelfRenderer?.contents?.getItems()
                    ?: content.musicShelfRenderer?.contents?.getItems()
            }?.mapNotNull { getSong(it, album) }

            return shelfSongs ?: emptyList()
        }

        fun getSong(renderer: MusicResponsiveListItemRenderer, album: AlbumItem? = null): SongItem? {
            // Extract library tokens using the new method that properly handles multiple toggle items
            val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

            val videoId = renderer.playlistItemData?.videoId
                ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
                ?: renderer.overlay?.musicItemThumbnailOverlayRenderer
                    ?.content?.musicPlayButtonRenderer
                    ?.playNavigationEndpoint?.watchEndpoint?.videoId
                ?: renderer.flexColumns.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text?.runs?.firstOrNull()
                    ?.navigationEndpoint?.watchEndpoint?.videoId
                ?: return null

            val title = renderer.flexColumns.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text?.runs?.firstOrNull()?.text
                ?: PageHelper.extractRuns(renderer.flexColumns, "MUSIC_VIDEO").firstOrNull()?.text
                ?: return null

            val parsedArtists = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_PAGE_TYPE_ARTIST").map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            }
            val artists = if (parsedArtists.isNotEmpty()) {
                parsedArtists
            } else {
                album?.artists ?: emptyList()
            }

            val albumObj = album?.let {
                Album(it.title, it.browseId)
            } ?: renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let {
                Album(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId ?: ""
                )
            }

            val duration = renderer.fixedColumns?.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                ?.text?.parseTime()

            val thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                ?: album?.thumbnail
                ?: ""

            return SongItem(
                id = videoId,
                title = title,
                artists = artists,
                album = albumObj,
                duration = duration,
                musicVideoType = renderer.musicVideoType,
                thumbnail = thumbnail,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                libraryAddToken = libraryTokens.addToken,
                libraryRemoveToken = libraryTokens.removeToken
            )
        }
    }
}
