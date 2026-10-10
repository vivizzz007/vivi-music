/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.music.innertube.models.YouTubeClient
import com.music.vivi.R
import com.music.vivi.utils.MusicVideoRepository
import com.music.vivi.utils.MusicVideoStream
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import timber.log.Timber

private sealed interface VideoStreamState {
    data object Loading : VideoStreamState
    data class Ready(val stream: MusicVideoStream) : VideoStreamState
    data object Failed : VideoStreamState
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun buildVideoPlayer(
    context: Context,
    stream: MusicVideoStream,
    startPositionMs: Long,
    playWhenReady: Boolean,
): ExoPlayer {
    val client = OkHttpClient.Builder().build()
    val dataSourceFactory = OkHttpDataSource.Factory(client)
        .setUserAgent(stream.userAgent)
        .setDefaultRequestProperties(
            if (stream.userAgent == YouTubeClient.USER_AGENT_WEB) {
                mapOf("Referer" to "https://www.youtube.com/", "Origin" to "https://www.youtube.com")
            } else {
                emptyMap()
            },
        )
    val sourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)

    val mediaSource: MediaSource = if (stream.audioUrl == null) {
        sourceFactory.createMediaSource(MediaItem.fromUri(stream.videoUrl))
    } else {
        MergingMediaSource(
            sourceFactory.createMediaSource(MediaItem.fromUri(stream.videoUrl)),
            sourceFactory.createMediaSource(MediaItem.fromUri(stream.audioUrl)),
        )
    }

    return ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply {
            setMediaSource(mediaSource)
            if (startPositionMs > 0L) seekTo(startPositionMs)
            this.playWhenReady = playWhenReady
            prepare()
        }
}

/**
 * Full-screen music video player.
 *
 * The caller is expected to pause its own audio playback while this dialog is open. [onClose] reports where the
 * video stopped so that the audio can continue from there.
 */
@Composable
fun MusicVideoPlayerDialog(
    videoId: String,
    title: String,
    artist: String,
    startPositionMs: Long,
    onClose: (positionMs: Long, wasPlaying: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // The activity is recreated when the phone is rotated, so what we need to carry over lives in saveable state.
    var resumePositionMs by rememberSaveable(videoId) { mutableLongStateOf(startPositionMs) }
    var resumePlaying by rememberSaveable(videoId) { mutableStateOf(true) }
    var landscape by rememberSaveable { mutableStateOf(false) }
    var playbackFailed by remember(videoId) { mutableStateOf(false) }

    val streamState by produceState<VideoStreamState>(VideoStreamState.Loading, videoId) {
        val stream = MusicVideoRepository.resolveStream(videoId)
        value = if (stream != null) VideoStreamState.Ready(stream) else VideoStreamState.Failed
    }

    val player = remember(streamState) {
        (streamState as? VideoStreamState.Ready)?.let {
            buildVideoPlayer(context, it.stream, resumePositionMs, resumePlaying)
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Timber.tag("MusicVideoPlayer").w(error, "Video playback failed for $videoId")
                playbackFailed = true
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                resumePlaying = playWhenReady
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
            player?.release()
        }
    }

    // Remember the position every so often so a rotation (activity recreation) can continue where it was.
    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        while (true) {
            resumePositionMs = p.currentPosition
            delay(400)
        }
    }

    fun close() {
        val position = player?.currentPosition ?: resumePositionMs
        val playing = player?.playWhenReady ?: false
        onClose(position, playing)
    }

    // Orientation: rotate the activity for the "fullscreen" button and give it back afterwards.
    DisposableEffect(Unit) {
        val original = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose { activity?.requestedOrientation = original }
    }
    LaunchedEffect(landscape) {
        activity?.requestedOrientation = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Dialog(
        onDismissRequest = { close() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        // Immersive: hide the system bars while the video is on screen.
        val dialogView = LocalView.current
        DisposableEffect(dialogView) {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            var controller: WindowInsetsControllerCompat? = null
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                controller = WindowCompat.getInsetsController(window, dialogView).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
        }

        BackHandler { close() }

        var controlsVisible by remember { mutableStateOf(true) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            when {
                streamState is VideoStreamState.Loading -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                streamState is VideoStreamState.Failed || playbackFailed -> {
                    VideoUnavailable(
                        videoId = videoId,
                        onClose = { close() },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                player != null -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = true
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                controllerShowTimeoutMs = 3000
                                setControllerVisibilityListener(
                                    PlayerView.ControllerVisibilityListener { visibility ->
                                        controlsVisible = visibility == View.VISIBLE
                                    },
                                )
                            }
                        },
                        update = { view -> view.player = player },
                    )
                }
            }

            // Top bar: close, title / artist, rotate.
            AnimatedVisibility(
                visible = controlsVisible || streamState !is VideoStreamState.Ready || playbackFailed,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                            ),
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    IconButton(onClick = { close() }) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.close),
                            tint = Color.White,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { openInYouTube(context, videoId) }) {
                        Icon(
                            painter = painterResource(R.drawable.open_in_new_icon),
                            contentDescription = stringResource(R.string.music_video_open_in_youtube),
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = { landscape = !landscape }) {
                        Icon(
                            painter = painterResource(R.drawable.fullscreen),
                            contentDescription = stringResource(R.string.music_video_rotate),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoUnavailable(
    videoId: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(32.dp),
    ) {
        Text(
            text = stringResource(R.string.music_video_unavailable),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = { openInYouTube(context, videoId) }) {
            Text(stringResource(R.string.music_video_open_in_youtube))
        }
        TextButton(onClick = onClose) {
            Text(text = stringResource(R.string.close), color = Color.White)
        }
    }
}

private fun openInYouTube(context: Context, videoId: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/watch?v=$videoId")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
