package com.music.vivi.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
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
import com.music.vivi.changelog.ChangelogScreen
import com.music.vivi.support.SupportScreen
import com.music.vivi.support.ViviIssueScreen
import com.music.vivi.ui.screens.artist.ArtistAlbumsScreen
import com.music.vivi.ui.screens.artist.ArtistItemsScreen
import com.music.vivi.ui.screens.artist.ArtistScreen
import com.music.vivi.ui.screens.artist.ArtistSongsScreen
import com.music.vivi.ui.screens.library.LibraryScreen
import com.music.vivi.ui.screens.playlist.AutoPlaylistScreen
import com.music.vivi.ui.screens.playlist.LocalPlaylistScreen
import com.music.vivi.ui.screens.playlist.OnlinePlaylistScreen
import com.music.vivi.ui.screens.playlist.TopPlaylistScreen
import com.music.vivi.ui.screens.playlist.CachePlaylistScreen
import com.music.vivi.ui.screens.search.OnlineSearchResult
import com.music.vivi.ui.screens.settings.AboutScreen
import com.music.vivi.ui.screens.settings.AppearanceSettings
import com.music.vivi.ui.screens.settings.BackupAndRestore
import com.music.vivi.ui.screens.settings.ContentSettings
import com.music.vivi.ui.screens.settings.DiscordLoginScreen
import com.music.vivi.ui.screens.settings.DiscordSettings
import com.music.vivi.ui.screens.settings.PlayerSettings
import com.music.vivi.ui.screens.settings.PrivacySettings
import com.music.vivi.ui.screens.settings.RomanizationSettings
import com.music.vivi.ui.screens.settings.SettingsScreen
import com.music.vivi.ui.screens.settings.StorageSettings
import com.music.vivi.ui.screens.settings.UpdaterScreen
import com.music.vivi.update.UpdateDetailsScreen
import com.music.vivi.update.account.AccountViewScreen
import com.music.vivi.update.betaupdate.ViviDpiSettings
import com.music.vivi.update.experiment.ExperimentalSettingsScreen
import com.music.vivi.update.settingstyle.AudioQualityScreen
import com.music.vivi.update.settingstyle.DefaultOpenTabScreen
import com.music.vivi.update.settingstyle.GridItemSizeScreen
import com.music.vivi.update.settingstyle.LibraryChipScreen
import com.music.vivi.update.settingstyle.LyricsPositionScreen
import com.music.vivi.update.settingstyle.LyricsProviderSettings
import com.music.vivi.update.settingstyle.PlayerBackgroundStyleScreen
import com.music.vivi.update.settingstyle.PlayerButtonsStyleScreen
import com.music.vivi.update.settingstyle.PlayerSliderStyleScreen
import com.music.vivi.update.settingstyle.ThemeSettingsScreen
import com.music.vivi.updatesreen.SoftwareUpdatesScreen

@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    composable(Screens.Home.route) {
        HomeScreen(navController)
    }
    composable(
        Screens.Library.route,
    ) {
        LibraryScreen(navController)
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
        arguments =
        listOf(
            navArgument("albumId") {
                type = NavType.StringType
            },
        ),
    ) {
        AlbumScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}",
        arguments =
        listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/songs",
        arguments =
        listOf(
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
            },
        ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }
    composable(
        route = "online_playlist/{playlistId}",
        arguments =
        listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "local_playlist/{playlistId}",
        arguments =
        listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "auto_playlist/{playlist}",
        arguments =
        listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
            },
        ),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "top_playlist/{top}",
        arguments =
        listOf(
            navArgument("top") {
                type = NavType.StringType
            },
        ),
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
            },
        ),
    ) {
        YouTubeBrowseScreen(navController)
    }
    composable("settings") {
        SettingsScreen(navController, scrollBehavior, latestVersionName)
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
    composable("settings/backup_restore") {
        BackupAndRestore(navController, scrollBehavior)
    }
    composable("settings/discord") {
        DiscordSettings(navController, scrollBehavior)
    }
    composable("settings/discord/login") {
        DiscordLoginScreen(navController)
    }
    composable("settings/updater") {
        UpdaterScreen(navController, scrollBehavior)
    }
    composable("settings/about") {
        AboutScreen(navController, scrollBehavior)
    }
    composable("login") {
        LoginScreen(navController)
    }


    // Navigation Builder
    composable("settings/software_updates") {
        SoftwareUpdatesScreen(navController, scrollBehavior)
    }

    composable("settings/update") {
        UpdateScreen(navController)
    }
    composable("settings/experimental") {
        ExperimentalSettingsScreen(navController = navController)
    }
    composable("settings/dpi") {
        ViviDpiSettings(navController, scrollBehavior)
    }
    composable("settings/changelog") {
        ChangelogScreen(navController = navController, scrollBehavior = scrollBehavior)
    }
    composable("settings/support") {
        SupportScreen(
            navController = navController,
            scrollBehavior = scrollBehavior
        )
    }
    composable("settings/report_issue") {
        ViviIssueScreen(
            navController = navController,
            scrollBehavior = scrollBehavior
        )
    }
    composable("settings/account_view") {
        AccountViewScreen(
            navController = navController,
            onBack = { navController.popBackStack() },
            latestVersionName = latestVersionName
        )
    }
    composable("audioQuality") {
        AudioQualityScreen(
            navController = navController,
            scrollBehavior = scrollBehavior
        )
    }
    composable("settings/content/lyrics") {
        LyricsProviderSettings(
            navController = navController,
            scrollBehavior = scrollBehavior
        )
    }
    composable("themeSettings") {
        ThemeSettingsScreen(
            navController = navController,
            scrollBehavior = scrollBehavior
        )
    }
    composable("playerBackgroundStyle") {
        PlayerBackgroundStyleScreen(
            navController = navController,
            scrollBehavior = scrollBehavior
        )
    }
    composable("playerSliderStyle") {
        PlayerSliderStyleScreen(
            navController = navController,
            scrollBehavior = scrollBehavior
        )
    }
    composable("lyricsPosition") {
        LyricsPositionScreen(
            navController = navController,
            scrollBehavior = scrollBehavior
        )
    }
    // In your navigation setup
    composable("defaultOpenTab") {
        DefaultOpenTabScreen(
            navController = navController,
            scrollBehavior = scrollBehavior
        )
    }
    // In your navigation setup
    composable("LibraryChip") {
        LibraryChipScreen(
            navController = navController,
            scrollBehavior = scrollBehavior
        )
    }
    // In your navigation setup
    composable("gridItemSize") {
        GridItemSizeScreen(
            navController = navController,
            scrollBehavior = scrollBehavior
        )
    }

    // In your navigation setup
    composable("playerButtonsStyle") {
        PlayerButtonsStyleScreen(
            navController = navController,
            scrollBehavior = scrollBehavior
        )
    }
    composable("update/details") {
        UpdateDetailsScreen(navController)
    }
}
