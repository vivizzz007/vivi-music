package com.music.innertube.pages

import com.music.innertube.models.Album
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.Artist
import com.music.innertube.models.MusicResponsiveListItemRenderer
import com.music.innertube.models.MusicTwoRowItemRenderer
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.models.oddElements
import com.music.innertube.models.splitBySeparator
import com.music.innertube.utils.parseTime

data class ArtistItemsPage(
    val title: String,
    val items: List<YTItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
            return SongItem(
                id = renderer.playlistItemData?.videoId ?: return null,
                title = renderer.flexColumns.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text
                    ?.runs?.firstOrNull()?.text ?: return null,
                artists = renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.let { runs ->
                    // First approach: look for elements with navigationEndpoint
                    val artistsWithEndpoint = runs.mapNotNull { run ->
                        run.navigationEndpoint?.browseEndpoint?.browseId?.let { browseId ->
                            if (browseId.startsWith("UC") || browseId.startsWith("MPLA")) {
                                Artist(name = run.text, id = browseId)
                            } else null
                        }
                    }

                    if (artistsWithEndpoint.isNotEmpty()) {
                        artistsWithEndpoint
                    } else {
                        // Fallback: use oddElements approach
                        runs.oddElements().mapNotNull { run ->
                            when {
                                run.text.matches(Regex("^\\d+.*")) -> null
                                run.text.lowercase() in listOf("song", "songs", "•", "views", "view") -> null
                                run.text.contains("views", ignoreCase = true) -> null
                                run.text.contains("view", ignoreCase = true) -> null
                                run.text.isBlank() || run.text.length <= 1 -> null
                                else -> Artist(name = run.text, id = run.navigationEndpoint?.browseEndpoint?.browseId)
                            }
                        }
                    }
                } ?: emptyList(),
                album = renderer.flexColumns.getOrNull(3)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let {
                    Album(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return null
                    )
                },
                duration = renderer.fixedColumns?.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text
                    ?.runs?.firstOrNull()
                    ?.text?.parseTime() ?: return null,
                thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                endpoint = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint
            )
        }

        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
            return when {
                renderer.isAlbum -> AlbumItem(
                    browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                    playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer
                        ?.content?.musicPlayButtonRenderer?.playNavigationEndpoint
                        ?.anyWatchEndpoint?.playlistId ?: return null,
                    title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                    artists = null,
                    year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                    thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    explicit = renderer.subtitleBadges?.find {
                        it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                    } != null
                )
                // Video
                renderer.isSong -> SongItem(
                    id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                    title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                    artists = renderer.subtitle?.runs?.let { runs ->
                        // First approach: look for elements with navigationEndpoint
                        val artistsWithEndpoint = runs.mapNotNull { run ->
                            run.navigationEndpoint?.browseEndpoint?.browseId?.let { browseId ->
                                if (browseId.startsWith("UC") || browseId.startsWith("MPLA")) {
                                    Artist(name = run.text, id = browseId)
                                } else null
                            }
                        }

                        if (artistsWithEndpoint.isNotEmpty()) {
                            artistsWithEndpoint
                        } else {
                            // Fallback: use splitBySeparator + oddElements approach
                            runs.splitBySeparator().firstOrNull()?.oddElements()?.mapNotNull { run ->
                                when {
                                    run.text.matches(Regex("^\\d+.*")) -> null
                                    run.text.lowercase() in listOf("song", "songs", "•", "views", "view") -> null
                                    run.text.contains("views", ignoreCase = true) -> null
                                    run.text.contains("view", ignoreCase = true) -> null
                                    run.text.isBlank() || run.text.length <= 1 -> null
                                    else -> Artist(name = run.text, id = run.navigationEndpoint?.browseEndpoint?.browseId)
                                }
                            } ?: emptyList()
                        }
                    } ?: emptyList(),
                    album = null,
                    duration = null,
                    thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    endpoint = renderer.navigationEndpoint.watchEndpoint
                )
                renderer.isPlaylist -> PlaylistItem(
                    id = renderer.navigationEndpoint.browseEndpoint?.browseId?.removePrefix("VL") ?: return null,
                    title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                    author = renderer.subtitle?.runs?.getOrNull(2)?.let {
                        Artist(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId
                        )
                    },
                    songCountText = renderer.subtitle?.runs?.getOrNull(4)?.text,
                    thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    playEndpoint = renderer.thumbnailOverlay
                        ?.musicItemThumbnailOverlayRenderer?.content
                        ?.musicPlayButtonRenderer?.playNavigationEndpoint
                        ?.watchPlaylistEndpoint ?: return null,
                    shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                        it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                    }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                    radioEndpoint = renderer.menu.menuRenderer.items.find {
                        it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                    }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null
                )
                else -> null
            }
        }
    }
}