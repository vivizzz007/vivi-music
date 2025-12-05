package com.music.vivi.update.widget

import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import android.widget.RemoteViews
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.google.common.util.concurrent.MoreExecutors
import com.music.vivi.MainActivity
import com.music.vivi.R
import com.music.vivi.db.MusicDatabase
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.playback.MusicService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MusicPlayerWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        android.util.Log.d("MusicWidget", "onUpdate called for ${appWidgetIds.size} widgets")
        val intent = Intent(context, WidgetUpdateService::class.java)
        context.startService(intent)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        android.util.Log.d("MusicWidget", "Widget enabled")
        val intent = Intent(context, WidgetUpdateService::class.java)
        context.startService(intent)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        android.util.Log.d("MusicWidget", "Widget disabled")
        val intent = Intent(context, WidgetUpdateService::class.java)
        context.stopService(intent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                android.util.Log.d("MusicWidget", "Play/Pause clicked")
                val updateIntent = Intent(context, WidgetUpdateService::class.java).apply {
                    action = ACTION_PLAY_PAUSE
                }
                context.startService(updateIntent)
            }
            ACTION_LIKE -> {
                android.util.Log.d("MusicWidget", "Like clicked")
                val updateIntent = Intent(context, WidgetUpdateService::class.java).apply {
                    action = ACTION_LIKE
                }
                context.startService(updateIntent)
            }
            ACTION_PLAY_SONG -> {
                android.util.Log.d("MusicWidget", "Play song from widget")
                val songId = intent.getStringExtra("song_id")
                val updateIntent = Intent(context, WidgetUpdateService::class.java).apply {
                    action = ACTION_PLAY_SONG
                    putExtra("song_id", songId)
                }
                context.startService(updateIntent)
            }
            "com.music.vivi.widget.PLAY_QUEUE_ITEM" -> {
                android.util.Log.d("MusicWidget", "Queue item clicked")
                val skipCount = intent.getIntExtra("skip_count", 0)
                val updateIntent = Intent(context, WidgetUpdateService::class.java).apply {
                    action = "com.music.vivi.widget.PLAY_QUEUE_ITEM"
                    putExtra("skip_count", skipCount)
                }
                context.startService(updateIntent)
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.music.vivi.widget.PLAY_PAUSE"
        const val ACTION_LIKE = "com.music.vivi.widget.LIKE"
        const val ACTION_PLAY_SONG = "com.music.vivi.widget.PLAY_SONG"
    }
}

@AndroidEntryPoint
class WidgetUpdateService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var updateJob: Job? = null
    private var animationJob: Job? = null
    private var selectedQueueIndex: Int = -1

    @Inject
    lateinit var database: MusicDatabase

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("MusicWidget", "Service started with action: ${intent?.action}")

        when (intent?.action) {
            MusicPlayerWidgetReceiver.ACTION_PLAY_PAUSE -> {
                togglePlayPause()
            }
            MusicPlayerWidgetReceiver.ACTION_LIKE -> {
                toggleLike()
            }
            MusicPlayerWidgetReceiver.ACTION_PLAY_SONG -> {
                val songId = intent.getStringExtra("song_id")
                if (songId != null) {
                    playSong(songId)
                }
            }
            "com.music.vivi.widget.PLAY_QUEUE_ITEM" -> {
                val skipCount = intent.getIntExtra("skip_count", 0)
                playQueueItem(skipCount)
            }
            else -> {
                startPeriodicUpdates()
            }
        }

        return START_STICKY
    }

    private fun playSong(songId: String) {
        scope.launch {
            try {
                val song = withContext(Dispatchers.IO) {
                    database.song(songId).first()
                }

                if (song == null) {
                    android.util.Log.e("MusicWidget", "Song not found: $songId")
                    return@launch
                }

                val sessionToken = SessionToken(
                    this@WidgetUpdateService,
                    ComponentName(this@WidgetUpdateService, MusicService::class.java)
                )
                val controllerFuture = MediaController.Builder(
                    this@WidgetUpdateService,
                    sessionToken
                ).buildAsync()

                controllerFuture.addListener({
                    try {
                        val controller = controllerFuture.get(2, TimeUnit.SECONDS)
                        controller.clearMediaItems()
                        val mediaItem = song.toMediaItem()
                        controller.setMediaItem(mediaItem)
                        controller.prepare()
                        controller.play()

                        android.util.Log.d("MusicWidget", "Playing song: ${song.song.title}")

                        scope.launch {
                            delay(500)
                            updateAllWidgets()
                        }

                        controller.release()
                    } catch (e: Exception) {
                        android.util.Log.e("MusicWidget", "Error playing song", e)
                    }
                }, MoreExecutors.directExecutor())
            } catch (e: Exception) {
                android.util.Log.e("MusicWidget", "Error connecting for song playback", e)
            }
        }
    }

    private fun startPeriodicUpdates() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                updateAllWidgets()
                delay(2000)
            }
        }
    }

    private fun updateAllWidgets() {
        scope.launch {
            try {
                val sessionToken = SessionToken(
                    this@WidgetUpdateService,
                    ComponentName(this@WidgetUpdateService, MusicService::class.java)
                )
                val controllerFuture = MediaController.Builder(
                    this@WidgetUpdateService,
                    sessionToken
                ).buildAsync()

                controllerFuture.addListener({
                    try {
                        val controller = controllerFuture.get(2, TimeUnit.SECONDS)

                        val title = controller.mediaMetadata.title?.toString() ?: "Not Playing"
                        val artist = controller.mediaMetadata.artist?.toString() ?: "Tap to play"
                        val artworkUri = controller.mediaMetadata.artworkUri
                        val isPlaying = controller.isPlaying
                        val mediaId = controller.currentMediaItem?.mediaId

                        val nextSongs = mutableListOf<androidx.media3.common.MediaItem>()
                        val currentIndex = controller.currentMediaItemIndex
                        val timeline = controller.currentTimeline

                        if (!timeline.isEmpty) {
                            var nextIndex = currentIndex
                            var count = 0
                            while (count < 5) {
                                nextIndex = timeline.getNextWindowIndex(
                                    nextIndex,
                                    controller.repeatMode,
                                    controller.shuffleModeEnabled
                                )
                                if (nextIndex == androidx.media3.common.C.INDEX_UNSET) break
                                try {
                                    val nextItem = controller.getMediaItemAt(nextIndex)
                                    nextSongs.add(nextItem)
                                    count++
                                } catch (e: Exception) {
                                    break
                                }
                            }
                        }

                        scope.launch(Dispatchers.IO) {
                            val albumBitmap = if (artworkUri != null) {
                                loadAlbumArt(artworkUri.toString())
                            } else null

                            val isLiked = mediaId?.let { id ->
                                try {
                                    database.song(id).first()?.song?.liked ?: false
                                } catch (e: Exception) {
                                    false
                                }
                            } ?: false

                            val queueAlbumArts = nextSongs.mapNotNull { item ->
                                item.mediaMetadata.artworkUri?.toString()?.let { uri ->
                                    loadAlbumArt(uri, 150)
                                }
                            }

                            withContext(Dispatchers.Main) {
                                updateWidgetViews(title, artist, albumBitmap, isPlaying, isLiked, nextSongs, queueAlbumArts)
                            }
                        }

                        controller.release()
                    } catch (e: Exception) {
                        android.util.Log.e("MusicWidget", "Error in listener", e)
                        updateWidgetViews("Not Playing", "Tap to play", null, false, false, emptyList(), emptyList())
                    }
                }, MoreExecutors.directExecutor())
            } catch (e: Exception) {
                android.util.Log.e("MusicWidget", "Error creating MediaController", e)
                updateWidgetViews("Connection Error", "Tap to retry", null, false, false, emptyList(), emptyList())
            }
        }
    }

    private fun togglePlayPause() {
        scope.launch {
            try {
                val sessionToken = SessionToken(
                    this@WidgetUpdateService,
                    ComponentName(this@WidgetUpdateService, MusicService::class.java)
                )
                val controllerFuture = MediaController.Builder(
                    this@WidgetUpdateService,
                    sessionToken
                ).buildAsync()

                controllerFuture.addListener({
                    try {
                        val controller = controllerFuture.get(2, TimeUnit.SECONDS)

                        if (controller.isPlaying) {
                            controller.pause()
                        } else {
                            controller.play()
                        }

                        scope.launch {
                            delay(300)
                            updateAllWidgets()
                        }

                        controller.release()
                    } catch (e: Exception) {
                        android.util.Log.e("MusicWidget", "Error toggling playback", e)
                    }
                }, MoreExecutors.directExecutor())
            } catch (e: Exception) {
                android.util.Log.e("MusicWidget", "Error connecting for playback", e)
            }
        }
    }

    private fun toggleLike() {
        scope.launch {
            try {
                val sessionToken = SessionToken(
                    this@WidgetUpdateService,
                    ComponentName(this@WidgetUpdateService, MusicService::class.java)
                )
                val controllerFuture = MediaController.Builder(
                    this@WidgetUpdateService,
                    sessionToken
                ).buildAsync()

                controllerFuture.addListener({
                    try {
                        val controller = controllerFuture.get(2, TimeUnit.SECONDS)
                        val mediaId = controller.currentMediaItem?.mediaId

                        if (mediaId != null) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val song = database.song(mediaId).first()
                                    if (song != null) {
                                        val updatedSong = song.song.copy(liked = !song.song.liked)
                                        database.query {
                                            update(updatedSong)
                                        }
                                        android.util.Log.d("MusicWidget", "Toggled like for: ${song.song.title}, now liked: ${updatedSong.liked}")
                                        
                                        // Update widget immediately
                                        withContext(Dispatchers.Main) {
                                            delay(100)
                                            updateAllWidgets()
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MusicWidget", "Error toggling like in database", e)
                                }
                            }
                        }

                        controller.release()
                    } catch (e: Exception) {
                        android.util.Log.e("MusicWidget", "Error getting current media item", e)
                    }
                }, MoreExecutors.directExecutor())
            } catch (e: Exception) {
                android.util.Log.e("MusicWidget", "Error connecting for like toggle", e)
            }
        }
    }

    private fun playQueueItem(skipCount: Int) {
        // No animation - instant playback
        try {
            val sessionToken = SessionToken(
                this@WidgetUpdateService,
                ComponentName(this@WidgetUpdateService, MusicService::class.java)
            )
            val controllerFuture = MediaController.Builder(
                this@WidgetUpdateService,
                sessionToken
            ).buildAsync()

            controllerFuture.addListener({
                try {
                    val controller = controllerFuture.get(2, TimeUnit.SECONDS)

                    val currentIndex = controller.currentMediaItemIndex
                    val timeline = controller.currentTimeline

                    if (!timeline.isEmpty) {
                        var targetIndex = currentIndex
                        repeat(skipCount) {
                            val nextIdx = timeline.getNextWindowIndex(
                                targetIndex,
                                controller.repeatMode,
                                controller.shuffleModeEnabled
                            )
                            if (nextIdx != androidx.media3.common.C.INDEX_UNSET) {
                                targetIndex = nextIdx
                            }
                        }

                        if (targetIndex != currentIndex) {
                            controller.seekTo(targetIndex, 0)
                            controller.play()
                        }
                    }

                    // Immediate widget update
                    scope.launch {
                        updateAllWidgets()
                    }

                    controller.release()
                } catch (e: Exception) {
                    android.util.Log.e("MusicWidget", "Error playing queue item", e)
                    // Update widget even on error
                    scope.launch {
                        updateAllWidgets()
                    }
                }
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            android.util.Log.e("MusicWidget", "Error connecting for queue playback", e)
            // Update widget even on error
            scope.launch {
                updateAllWidgets()
            }
        }
    }

    private suspend fun animateQueueItemSelection(index: Int) {
        // Multi-step smooth animation using setImageAlpha (supported on RemoteViews)
        updateQueueItemAlpha(index, 150) // Fade down
        delay(80)
        updateQueueItemAlpha(index, 255) // Fade back up
        delay(80)
        updateQueueItemAlpha(index, 180) // Pulse effect
        delay(80)
        updateQueueItemAlpha(index, 255) // Return to normal
    }

    private fun updateQueueItemAlpha(index: Int, alpha: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(this, MusicPlayerWidgetReceiver::class.java)
        )

        widgetIds.forEach { widgetId ->
            val views = RemoteViews(packageName, R.layout.widget_music_player)

            val queueAlbumViewIds = listOf(
                R.id.queue_album_1,
                R.id.queue_album_2,
                R.id.queue_album_3,
                R.id.queue_album_4,
                R.id.queue_album_5
            )

            if (index in queueAlbumViewIds.indices) {
                // Apply smooth fade animation using setImageAlpha (works on ImageView in RemoteViews)
                views.setInt(queueAlbumViewIds[index], "setImageAlpha", alpha)
            }

            appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
        }
    }

    private suspend fun loadAlbumArt(artworkUri: String, size: Int = 200): Bitmap? {
        return try {
            val imageLoader = ImageLoader.Builder(this)
                .crossfade(true)
                .build()

            val request = ImageRequest.Builder(this)
                .data(artworkUri)
                .size(size, size)
                .allowHardware(false)
                .crossfade(300) // 300ms crossfade duration for smooth transitions
                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                .build()

            val result = imageLoader.execute(request)
            result.image?.toBitmap()
        } catch (e: Exception) {
            android.util.Log.e("MusicWidget", "Error loading album art", e)
            null
        }
    }

    private fun updateWidgetViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean,
        isLiked: Boolean,
        queueItems: List<androidx.media3.common.MediaItem>,
        queueAlbumArts: List<Bitmap?>
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(this, MusicPlayerWidgetReceiver::class.java)
        )

        widgetIds.forEach { widgetId ->
            val views = createRemoteViews(title, artist, albumArt, isPlaying, isLiked, queueItems, queueAlbumArts)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun createRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean,
        isLiked: Boolean,
        queueItems: List<androidx.media3.common.MediaItem>,
        queueAlbumArts: List<Bitmap?>
    ): RemoteViews {
        val views = RemoteViews(packageName, R.layout.widget_music_player)

        views.setTextViewText(R.id.widget_song_title, title)
        views.setTextViewText(R.id.widget_artist_name, artist)

        if (albumArt != null) {
            val circularBitmap = getCircularBitmap(albumArt)
            views.setImageViewBitmap(R.id.widget_album_art, circularBitmap)
        } else {
            views.setImageViewResource(R.id.widget_album_art, R.drawable.library_music)
        }

        views.setImageViewResource(
            R.id.widget_play_pause,
            if (isPlaying) R.drawable.pause else R.drawable.play
        )

        if (isLiked) {
            views.setImageViewResource(R.id.widget_like_button, R.drawable.favorite)
            views.setInt(R.id.widget_like_button, "setColorFilter", android.graphics.Color.RED)
        } else {
            views.setImageViewResource(R.id.widget_like_button, R.drawable.favorite_border)
            views.setInt(R.id.widget_like_button, "setColorFilter", android.graphics.Color.parseColor("#BB86FC"))
        }

        val queueAlbumViewIds = listOf(
            R.id.queue_album_1,
            R.id.queue_album_2,
            R.id.queue_album_3,
            R.id.queue_album_4,
            R.id.queue_album_5
        )

        queueAlbumViewIds.forEachIndexed { index, albumViewId ->
            if (index < queueItems.size) {
                // Set album art
                if (index < queueAlbumArts.size && queueAlbumArts[index] != null) {
                    val roundedQueueArt = getRoundedCornerBitmap(queueAlbumArts[index]!!, 16f)
                    views.setImageViewBitmap(albumViewId, roundedQueueArt)
                } else {
                    views.setImageViewResource(albumViewId, R.drawable.library_music)
                }

                views.setViewVisibility(albumViewId, android.view.View.VISIBLE)

                // Apply selection highlight if this is the selected item
                if (index == selectedQueueIndex) {
                    views.setInt(albumViewId, "setImageAlpha", 180) // Slightly dimmed
                } else {
                    views.setInt(albumViewId, "setImageAlpha", 255) // Full opacity
                }

                // Set click listener
                val playQueueIntent = Intent(this, MusicPlayerWidgetReceiver::class.java).apply {
                    action = "com.music.vivi.widget.PLAY_QUEUE_ITEM"
                    putExtra("skip_count", index + 1)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    100 + index,
                    playQueueIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(albumViewId, pendingIntent)
            } else {
                views.setViewVisibility(albumViewId, android.view.View.GONE)
            }
        }

        views.setOnClickPendingIntent(R.id.widget_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_play_pause, getPlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_like_button, getLikeIntent())

        return views
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            shader = android.graphics.BitmapShader(
                bitmap,
                android.graphics.Shader.TileMode.CLAMP,
                android.graphics.Shader.TileMode.CLAMP
            )
        }

        canvas.drawCircle(
            size / 2f,
            size / 2f,
            size / 2f,
            paint
        )

        return output
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadius: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true //true
            isDither = true
            shader = android.graphics.BitmapShader(
                bitmap,
                android.graphics.Shader.TileMode.CLAMP,
                android.graphics.Shader.TileMode.CLAMP
            )
        }

        val rect = android.graphics.RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        return output
    }

    private fun getOpenAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getPlayPauseIntent(): PendingIntent {
        val intent = Intent(this, MusicPlayerWidgetReceiver::class.java).apply {
            action = MusicPlayerWidgetReceiver.ACTION_PLAY_PAUSE
        }
        return PendingIntent.getBroadcast(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getLikeIntent(): PendingIntent {
        val intent = Intent(this, MusicPlayerWidgetReceiver::class.java).apply {
            action = MusicPlayerWidgetReceiver.ACTION_LIKE
        }
        return PendingIntent.getBroadcast(
            this,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        animationJob?.cancel()
        (scope.coroutineContext[Job] as? Job)?.cancel()
    }
}