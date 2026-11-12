/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.ctrlfxx.ctrlmusic.ui.screens

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.ctrlfxx.ctrlmusic.constants.DarkModeKey
import com.ctrlfxx.ctrlmusic.constants.PureBlackKey
import com.ctrlfxx.ctrlmusic.ui.screens.artist.ArtistAlbumsScreen
import com.ctrlfxx.ctrlmusic.ui.screens.artist.ArtistItemsScreen
import com.ctrlfxx.ctrlmusic.ui.screens.artist.ArtistScreen
import com.ctrlfxx.ctrlmusic.ui.screens.artist.ArtistSongsScreen
import com.ctrlfxx.ctrlmusic.ui.screens.equalizer.EqScreen
import com.ctrlfxx.ctrlmusic.ui.screens.library.LibraryScreen
import com.ctrlfxx.ctrlmusic.ui.screens.playlist.AutoPlaylistScreen
import com.ctrlfxx.ctrlmusic.ui.screens.playlist.CachePlaylistScreen
import com.ctrlfxx.ctrlmusic.ui.screens.playlist.LocalPlaylistScreen
import com.ctrlfxx.ctrlmusic.ui.screens.playlist.OnlinePlaylistScreen
import com.ctrlfxx.ctrlmusic.ui.screens.playlist.TopPlaylistScreen
import com.ctrlfxx.ctrlmusic.ui.screens.search.OnlineSearchResult
import com.ctrlfxx.ctrlmusic.ui.screens.search.SearchScreen
import com.ctrlfxx.ctrlmusic.ui.screens.settings.AboutScreen
import com.ctrlfxx.ctrlmusic.ui.screens.settings.AppearanceSettings
import com.ctrlfxx.ctrlmusic.ui.screens.settings.MotionartworkSelection
import com.ctrlfxx.ctrlmusic.ui.screens.settings.FontSelectionScreen
import com.ctrlfxx.ctrlmusic.ui.screens.settings.BackupAndRestore
import com.ctrlfxx.ctrlmusic.ui.screens.settings.AutoBackupSettings
import com.ctrlfxx.ctrlmusic.ui.screens.settings.SpotifyLoginScreen
import com.ctrlfxx.ctrlmusic.ui.screens.settings.SpotifyPlaylistScreen
import com.ctrlfxx.ctrlmusic.viewmodels.SpotifyViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.ctrlfxx.ctrlmusic.ui.screens.settings.ContentSettings
import com.ctrlfxx.ctrlmusic.ui.screens.settings.DarkMode
import com.ctrlfxx.ctrlmusic.ui.screens.settings.PlayerSettings
import com.ctrlfxx.ctrlmusic.ui.screens.settings.JioSettings
import com.ctrlfxx.ctrlmusic.ui.screens.settings.SponsorBlockSettings
import com.ctrlfxx.ctrlmusic.ui.screens.settings.PrivacySettings
import com.ctrlfxx.ctrlmusic.ui.screens.settings.RomanizationSettings
import com.ctrlfxx.ctrlmusic.ui.screens.settings.SettingsScreen
import com.ctrlfxx.ctrlmusic.ui.screens.settings.AccountSettingsScreen
import com.ctrlfxx.ctrlmusic.ui.screens.ListeningSummaryScreen
import com.ctrlfxx.ctrlmusic.ui.screens.DetailedListeningHistoryScreen
import com.ctrlfxx.ctrlmusic.ui.screens.settings.StorageSettings
import com.ctrlfxx.ctrlmusic.ui.screens.settings.ThemeSettings
import com.ctrlfxx.ctrlmusic.ui.screens.settings.integrations.IntegrationScreen
import com.ctrlfxx.ctrlmusic.ui.screens.settings.integrations.ListenTogetherSettings
import com.ctrlfxx.ctrlmusic.ui.screens.recognition.RecognitionScreen
import com.ctrlfxx.ctrlmusic.ui.screens.recognition.RecognitionHistoryScreen
import com.ctrlfxx.ctrlmusic.ui.screens.settings.UpdateSettings
import com.ctrlfxx.ctrlmusic.ui.screens.settings.NotificationPermission
import com.ctrlfxx.ctrlmusic.ui.screens.wrapped.WrappedScreen
import com.ctrlfxx.ctrlmusic.extras.updater.UpdateScreen
import com.ctrlfxx.ctrlmusic.utils.rememberEnumPreference
import com.ctrlfxx.ctrlmusic.utils.rememberPreference
import com.ctrlfxx.ctrlmusic.extras.changelog.ChangelogScreen
import com.ctrlfxx.ctrlmusic.extras.commitscreen.CommitScreen
import com.ctrlfxx.ctrlmusic.ui.screens.equalizer.axion.AxionEqScreen
import com.ctrlfxx.ctrlmusic.ui.screens.settings.SpotifyScreen
import com.ctrlfxx.ctrlmusic.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
    homeViewModel: HomeViewModel
) {
    composable(Screens.Home.route) {
        HomeScreen(
            navController = navController,
            snackbarHostState = snackbarHostState,
            viewModel = homeViewModel
        )
    }

    composable(Screens.Search.route) {
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.ON)
        val isSystemInDarkTheme = true /* Ctrl Music: dark only */
        val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }
        val pureBlack = remember(pureBlackEnabled, useDarkTheme) {
            pureBlackEnabled && useDarkTheme
        }
        SearchScreen(
            navController = navController,
            pureBlack = pureBlack
        )
    }

    composable(Screens.Library.route) {
        LibraryScreen(navController)
    }



    composable(Screens.ListenTogether.route) {
        ListenTogetherScreen(navController, showTopBar = false)
    }

    composable(
        route = "listen_together_from_topbar",
    ) {
        ListenTogetherScreen(navController, showTopBar = true)
    }

    composable("listen_together/chat") {
        CommentTogetherScreen(navController)
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

    composable("account") {
        AccountScreen(navController, scrollBehavior)
    }

    composable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }

    composable("charts_screen") {
        ChartsScreen(navController)
    }

    composable(
        route = "browse/{browseId}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
            }
        )
    ) {
        BrowseScreen(
            navController,
            scrollBehavior,
            it.arguments?.getString("browseId")
        )
    }

    composable(
        route = "search/{query}",
        arguments = listOf(
            navArgument("query") {
                type = NavType.StringType
            },
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
        },
    ) {
        OnlineSearchResult(navController)
    }

    composable(
        route = "album/{albumId}",
        arguments = listOf(
            navArgument("albumId") {
                type = NavType.StringType
            },
        ),
    ) {
        AlbumScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}/songs",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}/albums",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            }
        )
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
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
            },
        ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }

    composable(
        route = "online_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }

    composable(
        route = "local_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }

    composable(
        route = "auto_playlist/{playlist}",
        arguments = listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }

    composable(
        route = "cache_playlist/{playlist}",
        arguments = listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }

    composable(
        route = "top_playlist/{top}",
        arguments = listOf(
            navArgument("top") {
                type = NavType.StringType
            },
        ),
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
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
            },
        ),
    ) {
        YouTubeBrowseScreen(navController)
    }

    composable("settings") {
        SettingsScreen(navController, scrollBehavior)
    }

    composable("settings/update") {
       UpdateSettings(navController, scrollBehavior)
    }

    composable("settings/update/notification_permission") {
        NotificationPermission(navController, scrollBehavior)
    }

    composable("settings/account") {
        AccountSettingsScreen(navController, scrollBehavior, homeViewModel)
    }
    composable("settings/listening_summary") {
        ListeningSummaryScreen(navController)
    }

    composable(
        route = "detailed_listening_history/{startTimestamp}",
        arguments = listOf(
            navArgument("startTimestamp") {
                type = NavType.StringType
            },
        ),
    ) {
        DetailedListeningHistoryScreen(navController)
    }

    composable("settings/appearance") {
        AppearanceSettings(navController, scrollBehavior, activity, snackbarHostState)
    }

    composable("settings/appearance/theme") {
        ThemeSettings(navController)
    }

    composable("settings/player/motionartwork") {
        MotionartworkSelection(navController, scrollBehavior)
    }

    composable("settings/appearance/font") {
        FontSelectionScreen(navController, scrollBehavior)
    }

    composable("settings/content") {
        ContentSettings(navController, scrollBehavior)
    }

    composable("settings/content/romanization") {
        RomanizationSettings(navController, scrollBehavior)
    }

    
    composable("settings/player") {
        PlayerSettings(navController, scrollBehavior)
    }

    composable("settings/player/jio") {
        JioSettings(navController, scrollBehavior)
    }

    composable("settings/player/sponsorblock") {
        SponsorBlockSettings(navController, scrollBehavior)
    }

    composable("settings/storage") {
        StorageSettings(navController, scrollBehavior)
    }

    composable("settings/equalizer") {
        AxionEqScreen(onBackClick = { navController.navigateUp() })
    }

    composable("settings/privacy") {
        PrivacySettings(navController, scrollBehavior)
    }

    composable("settings/backup_restore") {
        BackupAndRestore(navController, scrollBehavior)
    }

    composable("settings/backup_restore/autobackup") {
        AutoBackupSettings(navController, scrollBehavior)
    }

    composable("settings/spotify") {
        val rootEntry = remember(it) { navController.getBackStackEntry(navController.graph.id) }
        val viewModel: SpotifyViewModel = hiltViewModel(rootEntry)
        SpotifyScreen(navController, scrollBehavior, viewModel)
    }

    composable("settings/integrations") {
        IntegrationScreen(navController, scrollBehavior)
    }



    composable("settings/integrations/spotify") {
        val rootEntry = remember(it) { navController.getBackStackEntry(navController.graph.id) }
        val viewModel: SpotifyViewModel = hiltViewModel(rootEntry)
        SpotifyScreen(navController, scrollBehavior, viewModel)
    }

    composable("settings/integrations/spotify/login") {
        val rootEntry = remember(it) { navController.getBackStackEntry(navController.graph.id) }
        val viewModel: SpotifyViewModel = hiltViewModel(rootEntry)
        SpotifyLoginScreen(
            navController = navController,
            onCookiesCaptured = { spDc, spKey ->
                viewModel.connectWithCookies(spDc, spKey)
            }
        )
    }

    composable("settings/integrations/spotify/playlists") {
        val rootEntry = remember(it) { navController.getBackStackEntry(navController.graph.id) }
        val viewModel: SpotifyViewModel = hiltViewModel(rootEntry)
        SpotifyPlaylistScreen(navController, viewModel)
    }

    composable(route = "settings/integrations/listen_together") {
        ListenTogetherSettings(navController, scrollBehavior)
    }


    composable("settings/about") {
        AboutScreen(navController, scrollBehavior)
    }

    composable("update") {
        UpdateScreen(navController)
    }

    composable("login") {
        LoginScreen(navController)
    }

    composable("wrapped") {
        WrappedScreen(navController)
    }

    dialog("equalizer") {
        EqScreen(navController = navController)
    }

    composable("recognition") {
        RecognitionScreen(navController)
    }

    composable("recognition_history") {
        RecognitionHistoryScreen(navController)
    }
    composable("settings/changelog") {
        ChangelogScreen(navController,scrollBehavior)
    }
    composable("settings/commits") {
        CommitScreen(navController, scrollBehavior)
    }
}
