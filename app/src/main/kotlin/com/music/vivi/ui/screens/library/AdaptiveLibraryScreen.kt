package com.music.vivi.ui.screens.library

import android.os.Parcelable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.music.vivi.ui.screens.AlbumScreen
import com.music.vivi.ui.screens.artist.ArtistScreen
import com.music.vivi.ui.screens.playlist.AutoPlaylistScreen
import com.music.vivi.ui.screens.playlist.CachePlaylistScreen
import com.music.vivi.ui.screens.playlist.LocalPlaylistScreen
import com.music.vivi.ui.screens.playlist.OnlinePlaylistScreen
import com.music.vivi.ui.screens.playlist.TopPlaylistScreen
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class LibraryRoute : Parcelable {
    data class Artist(val id: String) : LibraryRoute()
    data class Album(val id: String) : LibraryRoute()
    data class LocalPlaylist(val id: String) : LibraryRoute()
    data class OnlinePlaylist(val id: String) : LibraryRoute()
    data class AutoPlaylist(val playlistParam: String) : LibraryRoute()
    data class CachePlaylist(val param: String) : LibraryRoute()
    data class TopPlaylist(val topParam: String) : LibraryRoute()
}

/**
 * A Master-Detail adaptive layout for the Library screen.
 * Uses [ListDetailPaneScaffold] to show the list of library items on the left/top
 * and the details (Artist, Album, Playlist) on the right/bottom or as a separate screen depending on window size.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveLibraryScreen(navController: NavController) {
    // FIX: Changed generic type to Any to satisfy compiler expectation
    val navigator = rememberListDetailPaneScaffoldNavigator<Any>()

    // Communicate detail pane visibility to MainActivity via savedStateHandle
    // We only hide the global top bar if the detail pane is active AND the list pane is hidden (single pane mode)
    val isDetailShown = navigator.currentDestination != null &&
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden

    LaunchedEffect(isDetailShown) {
        navController.currentBackStackEntry?.savedStateHandle?.set("is_detail_shown", isDetailShown)
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            LibraryScreen(
                navController = navController,
                onNavigate = { route ->
                    when {
                        route.startsWith("artist/") -> {
                            val id = route.removePrefix("artist/")
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, LibraryRoute.Artist(id))
                        }
                        route.startsWith("album/") -> {
                            val id = route.removePrefix("album/")
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, LibraryRoute.Album(id))
                        }
                        route.startsWith("local_playlist/") -> {
                            val id = route.removePrefix("local_playlist/")
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, LibraryRoute.LocalPlaylist(id))
                        }
                        route.startsWith("online_playlist/") -> {
                            val id = route.removePrefix("online_playlist/")
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, LibraryRoute.OnlinePlaylist(id))
                        }
                        route.startsWith("auto_playlist/") -> {
                            val param = route.removePrefix("auto_playlist/")
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, LibraryRoute.AutoPlaylist(param))
                        }
                        route.startsWith("cache_playlist/") -> {
                            val param = route.removePrefix("cache_playlist/")
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, LibraryRoute.CachePlaylist(param))
                        }
                        route.startsWith("top_playlist/") -> {
                            val param = route.removePrefix("top_playlist/")
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, LibraryRoute.TopPlaylist(param))
                        }
                        else -> {
                            navController.navigate(route)
                        }
                    }
                }
            )
        },
        detailPane = {
            // FIX: Cast content to LibraryRoute
            val content = navigator.currentDestination?.content as? LibraryRoute
            // Use pinned scroll behavior for the detail pane to avoid conflict with list pane
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

            when (content) {
                is LibraryRoute.Artist -> {
                    ArtistScreen(
                        navController = navController,
                        scrollBehavior = scrollBehavior,
                        artistId = content.id,
                        onBack = { navigator.navigateBack() }
                    )
                }
                is LibraryRoute.Album -> {
                    AlbumScreen(
                        navController = navController,
                        scrollBehavior = scrollBehavior,
                        albumId = content.id,
                        onBack = { navigator.navigateBack() }
                    )
                }
                is LibraryRoute.LocalPlaylist -> {
                    LocalPlaylistScreen(
                        navController = navController,
                        scrollBehavior = scrollBehavior,
                        playlistId = content.id,
                        onBack = { navigator.navigateBack() }
                    )
                }
                is LibraryRoute.OnlinePlaylist -> {
                    OnlinePlaylistScreen(
                        navController = navController,
                        scrollBehavior = scrollBehavior,
                        playlistId = content.id,
                        onBack = { navigator.navigateBack() }
                    )
                }
                is LibraryRoute.AutoPlaylist -> {
                    AutoPlaylistScreen(
                        navController = navController,
                        scrollBehavior = scrollBehavior,
                        playlistId = content.playlistParam,
                        onBack = { navigator.navigateBack() }
                    )
                }
                is LibraryRoute.CachePlaylist -> {
                    CachePlaylistScreen(
                        navController = navController,
                        scrollBehavior = scrollBehavior,
                        onBack = { navigator.navigateBack() }
                    )
                }
                is LibraryRoute.TopPlaylist -> {
                    TopPlaylistScreen(
                        navController = navController,
                        scrollBehavior = scrollBehavior,
                        topParam = content.topParam,
                        onBack = { navigator.navigateBack() }
                    )
                }
                null -> {
                    // Empty state
                }
            }
        }
    )
}
