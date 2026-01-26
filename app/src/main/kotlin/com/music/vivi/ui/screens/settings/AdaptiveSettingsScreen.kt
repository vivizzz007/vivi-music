package com.music.vivi.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.music.vivi.ui.screens.settings.RomanizationSettings
import com.music.vivi.ui.screens.settings.integrations.DiscordSettings
import com.music.vivi.ui.screens.settings.integrations.IntegrationScreen
import com.music.vivi.ui.screens.settings.integrations.LastFMSettings
import com.music.vivi.updatesreen.ViviUpdatesScreen

/**
 * A master-detail settings screen layout that adapts to screen size (e.g., tablets vs phones).
 * Uses [ListDetailPaneScaffold] to show the list of settings on one side and the detail on the other
 * on large screens, or full screen on smaller devices.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSettingsScreen(navController: NavController) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()

    BackHandler(enabled = navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                SettingsScreen(
                    navController = navController,
                    scrollBehavior = scrollBehavior,
                    onNavigate = { route ->
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, route)
                    }
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val route = navigator.currentDestination?.content
                if (route != null) {
                    when (route) {
                        "settings/account_settings" -> AccountSettings(
                            navController = navController,
                            onBack = { navigator.navigateBack() }
                        )
                        "settings/appearance" -> AppearanceSettings(navController, scrollBehavior, onBack = {
                            navigator.navigateBack()
                        })
                        "settings/player" -> PlayerSettings(navController, scrollBehavior, onBack = {
                            navigator.navigateBack()
                        })
                        "settings/backup_restore" -> BackupAndRestore(navController, scrollBehavior, onBack = {
                            navigator.navigateBack()
                        })
                        "settings/content" -> ContentSettings(
                            navController,
                            scrollBehavior,
                            onBack = { navigator.navigateBack() }
                        )
                        "settings/content/romanization" -> RomanizationSettings(navController, scrollBehavior, onBack = {
                            navigator.navigateBack()
                        })
                        "settings/storage" -> StorageSettings(navController, scrollBehavior, onBack = {
                            navigator.navigateBack()
                        })
                        "settings/privacy" -> PrivacySettings(navController, scrollBehavior, onBack = {
                            navigator.navigateBack()
                        })
                        "settings/power_saver" -> PowerSaverSettings(navController, scrollBehavior, onBack = {
                            navigator.navigateBack()
                        })
                        "settings/about" -> AboutScreen(navController, scrollBehavior, onBack = {
                            navigator.navigateBack()
                        })
                        "settings/software_updates" -> ViviUpdatesScreen(
                            navController = navController,
                            scrollBehavior = scrollBehavior,
                            onBack = { navigator.navigateBack() }
                        )
                        "settings/integrations" -> IntegrationScreen(navController, scrollBehavior, onBack = {
                            navigator.navigateBack()
                        })
                        "settings/integrations/discord" -> DiscordSettings(navController, scrollBehavior, onBack = {
                            navigator.navigateBack()
                        })
                        "settings/integrations/lastfm" -> LastFMSettings(navController, scrollBehavior, onBack = {
                            navigator.navigateBack()
                        })
                        else -> {
                            // Fallback for routes not handled by adaptive layout yet.
                            // We shouldn't really reach here if SettingsListContent only links to known routes.
                            // But if it does, we can either show empty or try to handle.
                        }
                    }
                }
            }
        }
    )
}
