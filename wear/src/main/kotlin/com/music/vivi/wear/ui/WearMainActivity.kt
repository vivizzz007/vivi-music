/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.ui

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import java.net.URLDecoder
import java.net.URLEncoder
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.music.vivi.wear.WearApp
import com.music.vivi.wear.playback.WearPlaybackService
import com.music.vivi.wear.ui.theme.ViviWearTheme
import timber.log.Timber

/**
 * Main Activity for the standalone Wear OS Vivi Music client.
 *
 * Hosts a [SwipeDismissableNavHost] with:
 * - login (QR Code pairing, shown when no credentials stored)
 * - now_playing
 * - library (offline downloads + Browse Online button)
 * - account_library (YouTube Music playlists)
 * - playlist/{playlistId}/{playlistTitle} (tracks inside a playlist)
 */
class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ViviWearTheme {
                WearNavHost()
            }
        }
    }
}

/**
 * Holds reactive state derived from the [MediaController] connection.
 */
class MediaControllerState {
    var controller: MediaController? by mutableStateOf(null)
    var isPlaying: Boolean by mutableStateOf(false)
    var currentMediaItem: MediaItem? by mutableStateOf(null)
    var duration: Long by mutableLongStateOf(0L)
    var position: Long by mutableLongStateOf(0L)
}

/**
 * Connects to the [WearPlaybackService] and provides a reactive [MediaControllerState].
 */
@Composable
fun rememberMediaControllerState(): MediaControllerState {
    val context = LocalContext.current
    val state = remember { MediaControllerState() }

    DisposableEffect(Unit) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, WearPlaybackService::class.java),
        )
        val controllerFuture: ListenableFuture<MediaController> =
            MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener(
            {
                try {
                    val ctrl = controllerFuture.get()
                    state.controller = ctrl
                    state.isPlaying = ctrl.isPlaying
                    state.currentMediaItem = ctrl.currentMediaItem
                    state.duration = ctrl.duration.coerceAtLeast(0)
                    state.position = ctrl.currentPosition.coerceAtLeast(0)

                    ctrl.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            state.isPlaying = isPlaying
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            state.currentMediaItem = mediaItem
                            state.duration = ctrl.duration.coerceAtLeast(0)
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            state.duration = ctrl.duration.coerceAtLeast(0)
                        }
                    })
                } catch (e: Exception) {
                    Timber.e(e, "Failed to connect MediaController")
                }
            },
            MoreExecutors.directExecutor(),
        )

        onDispose {
            MediaController.releaseFuture(controllerFuture)
            state.controller = null
        }
    }

    // Periodically update position
    LaunchedEffect(state.isPlaying) {
        if (state.isPlaying) {
            while (true) {
                state.position = state.controller?.currentPosition?.coerceAtLeast(0) ?: 0L
                kotlinx.coroutines.delay(500)
            }
        }
    }

    return state
}

@Composable
fun WearNavHost() {
    val navController = rememberSwipeDismissableNavController()
    val mediaState = rememberMediaControllerState()
    val authManager = remember { WearApp.instance.authManager }

    // Observe login state — start on login if no credentials, otherwise now_playing
    val isLoggedIn by authManager.isLoggedIn.collectAsState(initial = null)

    // Wait until we know the login state before setting the start destination
    val startDestination = when (isLoggedIn) {
        null -> return // Still loading DataStore — render nothing yet
        false -> "login"
        true -> "now_playing"
    }

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("now_playing") {
                        popUpTo("login") { inclusive = true }
                    }
                },
            )
        }

        composable("now_playing") {
            NowPlayingScreen(
                mediaState = mediaState,
                onNavigateToLibrary = { navController.navigate("library") },
            )
        }

        composable("library") {
            LibraryScreen(
                mediaState = mediaState,
                onNavigateToPlaylist = { playlistId, title ->
                    val encodedTitle = URLEncoder.encode(title.ifBlank { "Playlist" }, "UTF-8")
                    navController.navigate("playlist_detail/$playlistId/$encodedTitle")
                },
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "playlist_detail/{playlistId}/{title}",
            arguments = listOf(
                navArgument("playlistId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
            val rawTitle = backStackEntry.arguments?.getString("title") ?: ""
            val title = runCatching { URLDecoder.decode(rawTitle, "UTF-8") }.getOrDefault(rawTitle)
            PlaylistDetailScreen(
                playlistId = playlistId,
                title = title,
                mediaState = mediaState,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNowPlaying = {
                    navController.navigate("now_playing") {
                        popUpTo("now_playing") { inclusive = false }
                    }
                },
            )
        }

        // Backward compatibility route
        composable(
            route = "playlist/{playlistId}/{playlistTitle}",
            arguments = listOf(
                navArgument("playlistId") { type = NavType.StringType },
                navArgument("playlistTitle") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
            val rawTitle = backStackEntry.arguments?.getString("playlistTitle") ?: ""
            val title = runCatching { URLDecoder.decode(rawTitle, "UTF-8") }.getOrDefault(rawTitle)
            PlaylistDetailScreen(
                playlistId = playlistId,
                title = title,
                mediaState = mediaState,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNowPlaying = {
                    navController.navigate("now_playing") {
                        popUpTo("now_playing") { inclusive = false }
                    }
                },
            )
        }
    }
}
