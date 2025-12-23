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
import android.view.View
import android.widget.RemoteViews
import androidx.media3.common.MediaItem
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.music.vivi.MainActivity
import com.music.vivi.R
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.music.vivi.db.MusicDatabase
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViviWidgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase
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
        queueItems: List<MediaItem>
    ) {
        // Mitigation for race condition: MusicService might call this before DB update finishes
        delay(150)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, MusicPlayerWidgetReceiver::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (widgetIds.isEmpty()) return

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

        coroutineScope {
            val albumArtDeferred = async { artworkUri?.let { loadAlbumArt(it) } }
            val queueAlbumArtsDeferred = queueItems.take(5).map { item ->
                async {
                    item.mediaMetadata.artworkUri?.toString()?.let { uri ->
                        loadAlbumArt(uri, 150)
                    }
                }
            }

            val albumArt = albumArtDeferred.await()
            val processedAlbumArt = albumArt?.let { getCircularBitmap(it) }

            val queueAlbumArts = queueAlbumArtsDeferred.awaitAll()
            val processedQueueArts = queueAlbumArts.map { bitmap ->
                bitmap?.let { getRoundedCornerBitmap(it, 16f) }
            }

            val views = createRemoteViews(
                title,
                artist,
                processedAlbumArt,
                isPlaying,
                actualIsLiked,
                queueItems,
                processedQueueArts
            )

            widgetIds.forEach { widgetId ->
                appWidgetManager.updateAppWidget(widgetId, views)
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
        queueAlbumArts: List<Bitmap?>
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_music_player)

        views.setTextViewText(R.id.widget_song_title, title)
        views.setTextViewText(R.id.widget_artist_name, artist)

        if (albumArt != null) {
            views.setImageViewBitmap(R.id.widget_album_art, albumArt)
        } else {
            views.setImageViewResource(R.id.widget_album_art, R.drawable.library_music)
        }

        views.setImageViewResource(
            R.id.widget_play_pause,
            if (isPlaying) R.drawable.pause else R.drawable.play
        )

        // Read Theme
        val darkModePref = context.dataStore[DarkModeKey] ?: DarkMode.AUTO.name
        val systemIsDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val isDark = when (darkModePref) {
            DarkMode.ON.name -> true
            DarkMode.OFF.name -> false
            else -> systemIsDark
        }

        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            if (isDark) R.drawable.widget_background_dark else R.drawable.widget_background_light
        )

        val primaryTextColor = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        val secondaryTextColor = if (isDark) android.graphics.Color.parseColor("#B3FFFFFF") else android.graphics.Color.parseColor("#757575")

        views.setTextColor(R.id.widget_song_title, primaryTextColor)
        views.setTextColor(R.id.widget_artist_name, secondaryTextColor)

        if (isLiked) {
            views.setImageViewBitmap(
                R.id.widget_like_button,
                getTintedBitmap(R.drawable.star_shine_fav_filled, android.graphics.Color.parseColor("#FFC107"))
            )
        } else {
            val color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            views.setImageViewBitmap(R.id.widget_like_button, getTintedBitmap(R.drawable.star_fav, color))
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
                if (index < queueAlbumArts.size && queueAlbumArts[index] != null) {
                    views.setImageViewBitmap(albumViewId, queueAlbumArts[index])
                } else {
                    views.setImageViewResource(albumViewId, R.drawable.library_music)
                }
                views.setViewVisibility(albumViewId, View.VISIBLE)

                // Intent for queue item
                val playQueueIntent = Intent(context, MusicPlayerWidgetReceiver::class.java).apply {
                    action = MusicPlayerWidgetReceiver.ACTION_PLAY_QUEUE_ITEM
                    putExtra("skip_count", index + 1)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    100 + index,
                    playQueueIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(albumViewId, pendingIntent)
            } else {
                views.setViewVisibility(albumViewId, View.GONE)
            }
        }

        views.setOnClickPendingIntent(R.id.widget_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_play_pause, getPlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_like_button, getLikeIntent())

        return views
    }

    private suspend fun loadAlbumArt(artworkUri: String, size: Int = 200): Bitmap? {
        return withContext(Dispatchers.IO) {
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
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return output
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

    private fun getTintedBitmap(resourceId: Int, color: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, resourceId)!!
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.colorFilter = android.graphics.PorterDuffColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
        drawable.draw(canvas)
        return bitmap
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
