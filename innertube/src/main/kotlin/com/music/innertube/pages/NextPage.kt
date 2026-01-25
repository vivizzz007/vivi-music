package com.music.innertube.pages

import com.music.innertube.models.Album
import com.music.innertube.models.Artist
import com.music.innertube.models.BrowseEndpoint
import com.music.innertube.models.PlaylistPanelVideoRenderer
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.oddElements
import com.music.innertube.models.splitBySeparator
import com.music.innertube.utils.parseTime

data class NextResult(
    val title: String? = null,
    val items: List<SongItem>,
    val currentIndex: Int? = null,
    val lyricsEndpoint: BrowseEndpoint? = null,
    val relatedEndpoint: BrowseEndpoint? = null,
    val continuation: String?,
    val endpoint: WatchEndpoint, // current or continuation next endpoint
)

object NextPage {
    fun fromPlaylistPanelVideoRenderer(renderer: PlaylistPanelVideoRenderer): SongItem? {
        val longByLineRuns = renderer.longBylineText?.runs?.splitBySeparator() ?: return null
        return SongItem(
            id = renderer.videoId ?: return null,
            title =
            renderer.title
                ?.runs
                ?.firstOrNull()
                ?.text ?: return null,
            artists =
            longByLineRuns.firstOrNull()?.oddElements()?.map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            } ?: return null,
            album =
            longByLineRuns
                .getOrNull(1)
                ?.firstOrNull()
                ?.takeIf {
                    it.navigationEndpoint?.browseEndpoint != null
                }?.let {
                    Album(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId!!
                    )
                },
            duration =
            renderer.lengthText
                ?.runs
                ?.firstOrNull()
                ?.text
                ?.parseTime() ?: return null,
            musicVideoType = renderer.navigationEndpoint.musicVideoType,
            thumbnail =
            renderer.thumbnail.thumbnails
                .lastOrNull()
                ?.url ?: return null,
            explicit =
            renderer.badges?.find {
                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
            } != null,
            libraryAddToken = PageHelper.extractFeedbackToken(
                renderer.menu?.menuRenderer?.items?.find {
                    it.toggleMenuServiceItemRenderer?.defaultIcon?.iconType?.startsWith("LIBRARY_") == true
                }?.toggleMenuServiceItemRenderer,
                "LIBRARY_ADD"
            ),
            libraryRemoveToken = PageHelper.extractFeedbackToken(
                renderer.menu?.menuRenderer?.items?.find {
                    it.toggleMenuServiceItemRenderer?.defaultIcon?.iconType?.startsWith("LIBRARY_") == true
                }?.toggleMenuServiceItemRenderer,
                "LIBRARY_SAVED"
            )
        )
    }
}
