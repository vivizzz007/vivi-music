package com.music.vivi.ui.player

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import androidx.media3.ui.PlayerView
import com.music.vivi.playback.VideoCacheManager
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.constants.VideoQuality
import com.music.vivi.constants.VideoQualityKey
import com.music.vivi.constants.VideoQualityDefaultValue
import com.music.vivi.utils.rememberEnumPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Composable that displays a video player using ExoPlayer.
 * This replaces the album art when video mode is enabled.
 *
 * @param videoUrl The URL of the video stream to play
 * @param modifier Modifier for the player view
 * @param onError Callback when video playback fails
 */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoPlayerView(
    videoUrl: String?,
    modifier: Modifier = Modifier,
    isPlayerExpanded: Boolean = true,
    onError: ((Exception) -> Unit)? = null
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Video quality preference
    val (videoQuality, onVideoQualityChange) = rememberEnumPreference(
        key = VideoQualityKey,
        defaultValue = VideoQuality.valueOf(VideoQualityDefaultValue)
    )

    // Create a separate ExoPlayer instance for video (VIDEO ONLY, no audio)
    val videoPlayer = remember {
        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true) // Disable audio track
                .build()
        }

        // Aggressive buffering for faster initial playback and smooth seeking
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2500,  // minBufferMs - Must be >= bufferForPlaybackAfterRebufferMs
                60000, // maxBufferMs - Buffer up to 60s ahead for smooth seeking (like YouTube Music)
                1000,  // bufferForPlaybackMs - Start playback after 1s
                2000   // bufferForPlaybackAfterRebufferMs - Rebuffer threshold
            )
            .setPrioritizeTimeOverSizeThresholds(true) // Prioritize time-based buffering
            .setTargetBufferBytes(50 * 1024 * 1024) // 50MB buffer target for smooth seeks
            .build()

        // Hardware acceleration for video decoding
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        }

        // Use cached data source for faster loading
        val mediaSourceFactory = try {
            DefaultMediaSourceFactory(VideoCacheManager.getCacheDataSourceFactory())
        } catch (e: Exception) {
            // Fallback to default if cache not initialized
            timber.log.Timber.w(e, "Video cache not available, using default data source")
            DefaultMediaSourceFactory(context)
        }

        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                // Set video scaling mode
                videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                repeatMode = Player.REPEAT_MODE_OFF
                volume = 0f // Mute video player (use main player audio)

                // Add listener for loading state
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                isLoading = false
                                hasError = false
                            }
                            Player.STATE_BUFFERING -> {
                                isLoading = true
                            }
                            Player.STATE_ENDED -> {
                                isLoading = false
                            }
                            Player.STATE_IDLE -> {
                                isLoading = false
                            }
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        hasError = true
                        isLoading = false
                        onError?.invoke(Exception(error.message ?: "Video playback error"))
                    }
                })
            }
    }

    // Sync video playback with audio player
    // Prepare video immediately when URL is available (even if not visible yet)
    LaunchedEffect(videoUrl) {
        if (!videoUrl.isNullOrEmpty()) {
            try {
                timber.log.Timber.d("Preparing video player with URL")
                val mediaItem = MediaItem.fromUri(videoUrl)
                videoPlayer.setMediaItem(mediaItem)

                // Prepare immediately for instant playback when user switches to video
                videoPlayer.prepare()

                // Sync position with main audio player
                playerConnection?.player?.let { audioPlayer ->
                    val currentPosition = audioPlayer.currentPosition
                    videoPlayer.seekTo(currentPosition)
                }

                // Start playback if main player is playing
                if (playerConnection?.player?.isPlaying == true) {
                    videoPlayer.play()
                }

                timber.log.Timber.d("Video player ready")
            } catch (e: Exception) {
                hasError = true
                onError?.invoke(e)
                timber.log.Timber.e(e, "Failed to prepare video player")
            }
        }
    }

    // Sync playback state with main audio player (play/pause)
    // Also pause video when player is minimized to save resources
    DisposableEffect(playerConnection, isPlayerExpanded) {
        val playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Only play video if player is expanded
                if (isPlaying && isPlayerExpanded) {
                    videoPlayer.play()
                } else {
                    videoPlayer.pause()
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // Only play video if player is expanded
                if (playWhenReady && isPlayerExpanded) {
                    videoPlayer.play()
                } else {
                    videoPlayer.pause()
                }
            }
        }

        playerConnection?.player?.addListener(playerListener)

        onDispose {
            playerConnection?.player?.removeListener(playerListener)
        }
    }

    // Pause video when player is minimized
    LaunchedEffect(isPlayerExpanded) {
        if (!isPlayerExpanded) {
            videoPlayer.pause()
        } else if (playerConnection?.player?.isPlaying == true) {
            videoPlayer.play()
        }
    }

    // Fallback LaunchedEffect for cases where connection changes
    LaunchedEffect(playerConnection?.player?.isPlaying) {
        if (playerConnection?.player?.isPlaying == true) {
            videoPlayer.play()
        } else {
            videoPlayer.pause()
        }
    }

    // Sync video position with audio player continuously (optimized)
    LaunchedEffect(Unit) {
        while (isActive) {
            // Only sync if video is not loading to avoid interference
            if (!isLoading && !hasError) {
                playerConnection?.player?.let { audioPlayer ->
                    val audioPosition = audioPlayer.currentPosition
                    val videoPosition = videoPlayer.currentPosition
                    // Sync if positions differ by more than 1000ms (reduced sensitivity)
                    if (kotlin.math.abs(audioPosition - videoPosition) > 1000) {
                        videoPlayer.seekTo(audioPosition)
                    }
                }
            }
            delay(500) // Check every 500ms (reduced from 100ms for better performance)
        }
    }

    // Cleanup when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            videoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!videoUrl.isNullOrEmpty() && !hasError) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = videoPlayer
                        useController = false
                        resizeMode = RESIZE_MODE_FIT
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Show loading indicator (Material 3 Expressive)
        if (isLoading && !hasError) {
            Column(
                horizontalAlignment =Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                ContainedLoadingIndicator()
            }
        }
    }
}
