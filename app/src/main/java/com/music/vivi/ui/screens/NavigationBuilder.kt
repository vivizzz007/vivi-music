package com.music.vivi.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.maloy.muzza.ui.screens.artist.ArtistItemsScreen
import com.maloy.muzza.ui.screens.artist.ArtistScreen
import com.maloy.muzza.ui.screens.artist.ArtistSongsScreen
import com.maloy.muzza.ui.screens.library.LibraryAlbumsScreen
import com.maloy.muzza.ui.screens.library.LibraryArtistsScreen
import com.maloy.muzza.ui.screens.library.LibraryPlaylistsScreen
import com.maloy.muzza.ui.screens.playlist.LocalPlaylistScreen
import com.maloy.muzza.ui.screens.playlist.OnlinePlaylistScreen
import com.maloy.muzza.ui.screens.search.OnlineSearchResult
import com.maloy.muzza.ui.screens.settings.AboutScreen
import com.maloy.muzza.ui.screens.settings.AppearanceSettings
import com.maloy.muzza.ui.screens.settings.BackupAndRestore
import com.maloy.muzza.ui.screens.settings.DiscordLoginScreen
import com.maloy.muzza.ui.screens.settings.DiscordSettings
import com.maloy.muzza.ui.screens.settings.LyricsSettings
import com.maloy.muzza.ui.screens.settings.PlayerSettings
import com.maloy.muzza.ui.screens.settings.PrivacySettings
import com.maloy.muzza.ui.screens.settings.SettingsScreen
import com.maloy.muzza.ui.screens.settings.StorageSettings
import com.maloy.muzza.ui.screens.library.LibraryScreen
import com.maloy.muzza.ui.screens.library.LibrarySongsScreen
import com.maloy.muzza.ui.screens.playlist.AutoPlaylistScreen
import com.maloy.muzza.ui.screens.settings.AccountSettings
import com.maloy.muzza.ui.screens.settings.NotificationSettings
import com.maloy.muzza.ui.screens.settings.ContentSettings
import com.maloy.muzza.ui.screens.settings.import_from_spotify.ImportFromSpotifyScreen

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    composable(Screens.Home.route) {
        HomeScreen(navController)
    }
    composable(Screens.Explore.route) {
        ExploreScreen(navController)
    }
    composable(Screens.Library.route) {
        LibraryScreen(navController)
    }
    composable(Screens.Songs.route) {
        LibrarySongsScreen(navController)
    }
    composable(Screens.Artists.route) {
        LibraryArtistsScreen(navController)
    }
    composable(Screens.Albums.route) {
        LibraryAlbumsScreen(navController)
    }
    composable(Screens.Playlists.route) {
        LibraryPlaylistsScreen(navController)
    }
    composable("history") {
        HistoryScreen(navController)
    }
    composable("stats") {
        StatsScreen(navController)
    }
    composable("mood_and_genres") {
        MoodAndGenresScreen(navController, scrollBehavior)
    }
    composable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }
    composable(
        route = "search/{query}",
        arguments = listOf(
            navArgument("query") {
                type = NavType.StringType
            }
        ),
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        }
    ) {
        OnlineSearchResult(navController)
    }
    composable(
        route = "album/{albumId}",
        arguments = listOf(
            navArgument("albumId") {
                type = NavType.StringType
            },
        )
    ) {
        AlbumScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            }
        )
    ) { backStackEntry ->
        val artistId = backStackEntry.arguments?.getString("artistId")!!
        if (artistId.startsWith("LA")) {
            ArtistSongsScreen(navController, scrollBehavior)
        } else {
            ArtistScreen(navController, scrollBehavior)
        }
    }
    composable(
        route = "artist/{artistId}/songs",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            }
        )
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            }
        )
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }
    composable(
        route = "online_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            }
        )
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "local_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            }
        )
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            }
        )
    ) {
        YouTubeBrowseScreen(navController, scrollBehavior)
    }
    composable(
        route = "auto_playlist/{playlist}",
        arguments = listOf(
            navArgument("playlist") {
                type = NavType.StringType
            }
        )
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }
    composable("settings") {
        SettingsScreen( navController, scrollBehavior)
    }
    composable("settings/appearance") {
        AppearanceSettings(navController, scrollBehavior)
    }
    composable("settings/account") {
        AccountSettings(navController, scrollBehavior)
    }
    composable("settings/content") {
        ContentSettings(navController, scrollBehavior)
    }
    composable("settings/import_from_spotify/ImportFromSpotify") {
        ImportFromSpotifyScreen(navController, scrollBehavior)
    }
    composable("settings/player") {
        PlayerSettings(navController, scrollBehavior)
    }
    composable("settings/player/lyrics") {
        LyricsSettings(navController, scrollBehavior)
    }
    composable("settings/content/notification") {
        NotificationSettings(navController, scrollBehavior)
    }
    composable("settings/storage") {
        StorageSettings(navController, scrollBehavior)
    }
    composable("settings/privacy") {
        PrivacySettings(navController, scrollBehavior)
    }
    composable("settings/backup_restore") {
        BackupAndRestore(navController, scrollBehavior)
    }
    composable("settings/discord") {
        DiscordSettings(navController, scrollBehavior)
    }
    composable("settings/discord/login") {
        DiscordLoginScreen(navController)
    }
    composable("settings/about") {
        AboutScreen(navController, scrollBehavior)
    }
    composable("login") {
        LoginScreen(navController)
    }
    composable("YouTubePlaylists") {
        YouTubePlaylistsScreen(navController, scrollBehavior)
    }
    composable("setup_wizard") {
        SetupWizard(navController)
    }
}
