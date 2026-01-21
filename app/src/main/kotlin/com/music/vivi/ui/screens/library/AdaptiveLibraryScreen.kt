package com.music.vivi.ui.screens.library

import android.os.Parcelable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.music.vivi.ui.screens.AlbumScreen
import com.music.vivi.ui.screens.artist.ArtistScreen
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class LibraryRoute : Parcelable {
    data class Artist(val id: String) : LibraryRoute()
    data class Album(val id: String) : LibraryRoute()
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveLibraryScreen(
    navController: NavController,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<LibraryRoute>()

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
                        else -> {
                            navController.navigate(route)
                        }
                    }
                }
            )
        },
        detailPane = {
            val content = navigator.currentDestination?.content
            // Use pinned scroll behavior for the detail pane to avoid conflict with list pane
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            
            when (content) {
                is LibraryRoute.Artist -> {
                    ArtistScreen(
                        navController = navController,
                        scrollBehavior = scrollBehavior,
                        artistId = content.id
                    )
                }
                is LibraryRoute.Album -> {
                    AlbumScreen(
                        navController = navController,
                        scrollBehavior = scrollBehavior,
                        albumId = content.id
                    )
                }
                null -> {
                    // Empty state or placeholder if needed
                }
            }
        }
    )
}
