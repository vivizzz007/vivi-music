package com.music.vivi.update.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.widget.RemoteViews
import androidx.media3.common.MediaItem
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.music.vivi.MainActivity
import com.music.vivi.R
import com.music.vivi.db.MusicDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViviWidgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val imageLoader by lazy {
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }

    suspend fun updateWidgets(
        title: String,
        artist: String,
        artworkUri: String?,
        isPlaying: Boolean,
        isLiked: Boolean,
        queueItems: List<MediaItem>,
        duration: Long = 0,
        currentPosition: Long = 0,
    ) {
        // Mitigation for race condition: MusicService might call this before DB update finishes
        delay(150)

        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Fetch latest like state from DB if possible to be 100% sure
        var actualIsLiked = isLiked
        try {
            // Extract YouTube ID from URL if possible
            val songId = artworkUri?.let { uri ->
                val regex = "vi/([^/]+)/".toRegex()
                regex.find(uri)?.groupValues?.get(1)
            }
            if (songId != null) {
                database.getSongById(songId = songId)?.let { song ->
                    actualIsLiked = song.song.liked
                }
            } else {
                // Fallback: search by title and artist
                database.allSongs().firstOrNull()?.find {
                    it.song.title == title && it.artists.joinToString { it.name } == artist
                }?.let { song ->
                    actualIsLiked = song.song.liked
                }
            }
        } catch (e: Exception) {
            // Fallback to passed value
        }

        // Load and process album art
        val albumArt = artworkUri?.let { loadAlbumArt(it, 300) }

        // Update standard widgets
        val standardComponentName = ComponentName(context, MusicPlayerWidgetReceiver::class.java)
        val standardWidgetIds = appWidgetManager.getAppWidgetIds(standardComponentName)
        if (standardWidgetIds.isNotEmpty()) {
            val standardViews = createRemoteViews(
                title,
                artist,
                albumArt,
                isPlaying,
                actualIsLiked,
                queueItems,
                emptyList(),
                duration,
                currentPosition
            )
            standardWidgetIds.forEach { widgetId ->
                appWidgetManager.updateAppWidget(widgetId, standardViews)
            }
        }

        // Update Waves widgets
        val wavesComponentName = ComponentName(context, MusicWavesWidgetReceiver::class.java)
        val wavesWidgetIds = appWidgetManager.getAppWidgetIds(wavesComponentName)
        if (wavesWidgetIds.isNotEmpty()) {
            val wavesViews = createWavesRemoteViews(
                title,
                artist,
                albumArt,
                actualIsLiked,
                duration,
                currentPosition
            )
            wavesWidgetIds.forEach { widgetId ->
                appWidgetManager.updateAppWidget(widgetId, wavesViews)
            }
        }
    }

    private fun createRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean,
        isLiked: Boolean,
        queueItems: List<MediaItem>,
        queueAlbumArts: List<Bitmap?>,
        duration: Long = 0,
        currentPosition: Long = 0,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_music_player)

        // Set song info
        views.setTextViewText(R.id.widget_song_title, title)
        views.setTextViewText(R.id.widget_artist_name, artist)

        // Set album art with rounded corners
        if (albumArt != null) {
            val roundedAlbumArt = getRoundedCornerBitmap(albumArt, 48f)
            views.setImageViewBitmap(R.id.widget_album_art, roundedAlbumArt)
        } else {
            views.setImageViewResource(R.id.widget_album_art, R.drawable.vivi)
        }

        // Set play/pause icon (Central in Pill)
        val playPauseIcon = if (isPlaying) R.drawable.pause else R.drawable.play
        views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)

        // Dynamic Shape Animation (Capsule -> Squircle)
        // Matches Player.kt behavior: More rounded (Capsule) when Paused, Less rounded (Squircle) when Playing
        val containerShape = if (isPlaying) R.drawable.widget_play_shape_playing else R.drawable.widget_play_shape_paused
        // Note: We need to target the BACKGROUND of the container.
        // RemoteViews setInt("setBackgroundResource") or setImageViewResource if it's an ImageView background
        // The container ID is widget_play_pause_container (FrameLayout).
        // Check layout: inside container is widget_play_pause_bg (ImageView). We should update THAT.
        views.setImageViewResource(R.id.widget_play_pause_bg, containerShape)

        // Set like icon (Left Shape)
        val likeIcon = if (isLiked) R.drawable.favorite_test else R.drawable.favorite_border_test
        views.setImageViewResource(R.id.widget_like_button, likeIcon)

        // Set standard straight progress bar
        views.setImageViewResource(R.id.widget_progress_track, R.drawable.widget_straight_progress_track)
        views.setImageViewResource(R.id.widget_progress_fill, R.drawable.widget_straight_progress_clip)

        // Set Progress Level
        if (duration > 0) {
            val level = ((currentPosition.toDouble() / duration.toDouble()) * 10000).toInt()
            views.setInt(R.id.widget_progress_fill, "setImageLevel", level)
        } else {
            views.setInt(R.id.widget_progress_fill, "setImageLevel", 0)
        }

        // Set click intents
        views.setOnClickPendingIntent(R.id.widget_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_play_pause_container, getPlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_like_button, getLikeIntent())

        return views
    }

    private fun createWavesRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isLiked: Boolean,
        duration: Long,
        currentPosition: Long,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_waves_player)

        // Set song info
        views.setTextViewText(R.id.widget_waves_title, title)
        views.setTextViewText(R.id.widget_waves_artist, artist)

        // Set progress
//        if (duration > 0) {
//            val progress = (currentPosition.toFloat() / duration * 1000).toInt()
//            views.setProgressBar(R.id.widget_waves_progress, 1000, progress, false)
//        } else {
//            views.setProgressBar(R.id.widget_waves_progress, 1000, 0, false)
//        }

        // Set album art background
        if (albumArt != null) {
            val roundedAlbumArt = getRoundedCornerBitmap(albumArt, 64f) // Match background corners
            views.setImageViewBitmap(R.id.widget_waves_artwork, roundedAlbumArt)
        } else {
            views.setImageViewResource(R.id.widget_waves_artwork, R.drawable.vivi)
        }

        // Set like icon using test icons (no theme attributes)
        val likeIcon = if (isLiked) R.drawable.favorite_test else R.drawable.favorite_border_test
        views.setImageViewResource(R.id.widget_waves_favorite, likeIcon)

        // Set click intents
        views.setOnClickPendingIntent(R.id.widget_waves_artwork, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_waves_favorite, getLikeIntent())

        return views
    }

    private suspend fun loadAlbumArt(artworkUri: String, size: Int = 200): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(artworkUri)
                .size(size, size)
                .allowHardware(false)
                .crossfade(300)
                .build()
            val result = imageLoader.execute(request)
            result.image?.toBitmap()
        } catch (e: Exception) {
            null
        }
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadius: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        return output
    }

    private fun getOpenAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getPlayPauseIntent(): PendingIntent {
        val intent = Intent(context, MusicPlayerWidgetReceiver::class.java).apply {
            action = MusicPlayerWidgetReceiver.ACTION_PLAY_PAUSE
        }
        return PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getLikeIntent(): PendingIntent {
        val intent = Intent(context, MusicPlayerWidgetReceiver::class.java).apply {
            action = MusicPlayerWidgetReceiver.ACTION_LIKE
        }
        return PendingIntent.getBroadcast(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
