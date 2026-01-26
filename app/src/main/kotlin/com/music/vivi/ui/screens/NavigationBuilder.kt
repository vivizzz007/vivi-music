package com.music.vivi.ui.screens

import com.music.vivi.ui.screens.settings.AccountSettings
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
// NEW: Import for BuildConfig and AccountSettings
import com.music.vivi.support.SupportScreen
import com.music.vivi.support.ViviIssueScreen
import com.music.vivi.ui.screens.artist.ArtistAlbumsScreen
import com.music.vivi.ui.screens.artist.ArtistItemsScreen
import com.music.vivi.ui.screens.artist.ArtistScreen
import com.music.vivi.ui.screens.artist.ArtistSongsScreen
import com.music.vivi.ui.screens.library.AdaptiveLibraryScreen
import com.music.vivi.ui.screens.playlist.AutoPlaylistScreen
import com.music.vivi.ui.screens.playlist.LocalPlaylistScreen
import com.music.vivi.ui.screens.playlist.OnlinePlaylistScreen
import com.music.vivi.ui.screens.playlist.TopPlaylistScreen
import com.music.vivi.ui.screens.playlist.CachePlaylistScreen
import com.music.vivi.ui.screens.search.OnlineSearchResult
import com.music.vivi.ui.screens.settings.AboutScreen
// NEW: Import
import com.music.vivi.ui.screens.settings.AppearanceSettings
import com.music.vivi.ui.screens.settings.AdaptiveSettingsScreen
import com.music.vivi.ui.screens.settings.BackupAndRestore
import com.music.vivi.ui.screens.settings.ContentSettings
// import com.music.vivi.ui.screens.settings.integrations.DiscordLoginScreen
import com.music.vivi.ui.screens.settings.integrations.DiscordSettings
import com.music.vivi.ui.screens.settings.integrations.IntegrationScreen
import com.music.vivi.ui.screens.settings.integrations.LastFMSettings
import com.music.vivi.ui.screens.settings.PlayerSettings
import com.music.vivi.ui.screens.settings.PowerSaverSettings
import com.music.vivi.ui.screens.settings.PrivacySettings
import com.music.vivi.ui.screens.settings.RomanizationSettings
import com.music.vivi.ui.screens.settings.StorageSettings
import com.music.vivi.update.account.FunAccountViviSetting
import com.music.vivi.update.betaupdate.ViviDpiSettings
import com.music.vivi.update.changelog.ChangelogScreen
import com.music.vivi.ui.screens.NewsScreen
import com.music.vivi.ui.screens.settings.DiscordLoginScreen
import com.music.vivi.update.contribution.ContributionScreen
import com.music.vivi.update.experiment.ExperimentalSettingsScreen
import com.music.vivi.update.updatenotification.UpdateInfoScreen
import com.music.vivi.updatesreen.UpdateStatus
import com.music.vivi.updatesreen.ViviUpdatesScreen

/**
 * The main Navigation Graph builder for the Vivi Music application.
 * Defines all the composable routes and their arguments.
 */
@OptIn(ExperimentalMaterial3Api::class)
public fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    updateStatus: UpdateStatus,
) {
    composable(Screens.Home.route) {
        HomeScreen(navController)
    }
    composable(
        Screens.Library.route
    ) {
        AdaptiveLibraryScreen(navController)
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
        arguments =
        listOf(
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
        arguments =
        listOf(
            navArgument("albumId") {
                type = NavType.StringType
            }
        )
    ) {
        AlbumScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}",
        arguments =
        listOf(
            navArgument("artistId") {
                type = NavType.StringType
            }
        )
    ) {
        ArtistScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/songs",
        arguments =
        listOf(
            navArgument("artistId") {
                type = NavType.StringType
            }
        )
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
        arguments =
        listOf(
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
        arguments =
        listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            }
        )
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "local_playlist/{playlistId}",
        arguments =
        listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            }
        )
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "auto_playlist/{playlist}",
        arguments =
        listOf(
            navArgument("playlist") {
                type = NavType.StringType
            }
        )
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "cache_playlist/{playlist}",
        arguments =
        listOf(
            navArgument("playlist") {
                type = NavType.StringType
            }
        )
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "top_playlist/{top}",
        arguments =
        listOf(
            navArgument("top") {
                type = NavType.StringType
            }
        )
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments =
        listOf(
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
        YouTubeBrowseScreen(navController)
    }
    composable("settings") {
        AdaptiveSettingsScreen(navController)
    }
    composable("settings/appearance") {
        AppearanceSettings(navController, scrollBehavior)
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
    composable("settings/storage") {
        StorageSettings(navController, scrollBehavior)
    }
    composable("settings/privacy") {
        PrivacySettings(navController, scrollBehavior)
    }
    composable("settings/power_saver") {
        PowerSaverSettings(navController, scrollBehavior)
    }
    composable("settings/backup_restore") {
        BackupAndRestore(navController, scrollBehavior)
    }
    composable("settings/integrations") {
        IntegrationScreen(navController, scrollBehavior)
    }
    composable("settings/integrations/discord") {
        DiscordSettings(navController, scrollBehavior)
    }
    composable("settings/integrations/lastfm") {
        LastFMSettings(navController, scrollBehavior)
    }
    composable("settings/discord/login") {
        DiscordLoginScreen(navController)
    }

    // ADD NEW: Account Settings Route
    composable("settings/account_settings") {
        AccountSettings(
            navController = navController,
            onBack = { navController.popBackStack() }
        )
    }

    composable("settings/about") {
        AboutScreen(navController, scrollBehavior)
    }
    composable("login") {
        LoginScreen(navController)
    }

    // Navigation Builder
    composable("settings/software_updates") {
        ViviUpdatesScreen(navController, scrollBehavior)
    }

    composable("settings/update") {
        UpdateScreen(navController)
    }
    composable("settings/experimental") {
        ExperimentalSettingsScreen(navController)
    }
    composable("settings/dpi") {
        ViviDpiSettings(navController, scrollBehavior)
    }
    composable("settings/changelog") {
        ChangelogScreen(navController, scrollBehavior)
    }
    composable("news") {
        NewsScreen(navController, scrollBehavior)
    }
    composable("settings/support") {
        SupportScreen(navController, scrollBehavior)
    }
    composable("settings/report_issue") {
        ViviIssueScreen(navController, scrollBehavior)
    }

    composable("settings/account_view") {
        FunAccountViviSetting(navController, scrollBehavior)
    }

    composable("settings/updateinfo") {
        UpdateInfoScreen(navController, scrollBehavior)
    }
    composable("settings/contribution") {
        ContributionScreen(navController, scrollBehavior)
    }
}
