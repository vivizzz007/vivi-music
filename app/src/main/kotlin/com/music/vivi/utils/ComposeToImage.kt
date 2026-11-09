/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import timber.log.Timber
import coil3.request.allowHardware
import coil3.toBitmap
import com.music.vivi.R
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.LyricsBackgroundStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object ComposeToImage {

    suspend fun createLyricsImage(
        context: Context,
        mediaMetadata: MediaMetadata,
        lyrics: String,
        width: Int = 1080,
        height: Int = 675,
        backgroundColor: Int? = null,
        backgroundStyle: LyricsBackgroundStyle = LyricsBackgroundStyle.BLUR,
        textColor: Int? = null,
        secondaryTextColor: Int? = null,
        lyricsAlignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): Bitmap = createLyricsImage(
        context = context,
        coverArtUrl = mediaMetadata.thumbnailUrl,
        songTitle = mediaMetadata.title,
        artistName = mediaMetadata.artists.joinToString { it.name },
        lyrics = lyrics,
        width = width,
        height = height,
        backgroundColor = backgroundColor,
        backgroundStyle = backgroundStyle,
        textColor = textColor,
        secondaryTextColor = secondaryTextColor,
        lyricsAlignment = lyricsAlignment
    )

    suspend fun createLyricsImage(
        context: Context,
        coverArtUrl: String?,
        songTitle: String,
        artistName: String,
        lyrics: String,
        width: Int,
        height: Int,
        backgroundColor: Int? = null,
        backgroundStyle: LyricsBackgroundStyle = LyricsBackgroundStyle.SOLID,
        textColor: Int? = null,
        secondaryTextColor: Int? = null,
        lyricsAlignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): Bitmap = withContext(Dispatchers.Default) {
        val defaultBackgroundColor = 0xFF121212.toInt()
        val defaultTextColor = 0xFFFFFFFF.toInt()
        val defaultSecondaryTextColor = 0xB3FFFFFF.toInt()

        val bgColor = backgroundColor ?: defaultBackgroundColor
        val mainTextColor = textColor ?: defaultTextColor
        val secondaryTxtColor = secondaryTextColor ?: defaultSecondaryTextColor

        val imageWidth = 1080
        val scale = imageWidth / 360f

        val padding = 20f * scale
        val logoBoxSize = 22f * scale
        val topSpacer = 18f * scale
        val bottomSpacer = 22f * scale
        val coverArtSize = 44f * scale

        // Pre-calculate lyrics layout to dynamically determine image height based on line count
        val lyricsWidth = imageWidth - (padding * 2)
        val lyricsPaint = TextPaint().apply {
            color = mainTextColor
            textSize = 18f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            letterSpacing = 0.005f
        }

        val lyricsLayout = StaticLayout.Builder.obtain(lyrics, 0, lyrics.length, lyricsPaint, lyricsWidth.toInt())
            .setAlignment(lyricsAlignment)
            .setLineSpacing(0f, 1.4f)
            .setIncludePad(false)
            .build()

        val textHeight = lyricsLayout.height
        val calculatedHeight = (padding + logoBoxSize + topSpacer + textHeight + bottomSpacer + coverArtSize + padding).roundToInt()
        val minHeight = (imageWidth * 0.45f).roundToInt()
        val imageHeight = maxOf(calculatedHeight, minHeight)
        
        val bitmap = createBitmap(imageWidth, imageHeight)
        val canvas = Canvas(bitmap)

        // Pre-load cover art using application's SingletonImageLoader (with memory/disk cache)
        var coverArtBitmap: Bitmap? = null
        if (!coverArtUrl.isNullOrBlank()) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(coverArtUrl)
                    .size(1024) 
                    .allowHardware(false)
                    .build()
                val result = SingletonImageLoader.get(context).execute(request)
                if (result is SuccessResult) {
                    coverArtBitmap = result.image.toBitmap()
                } else if (result is ErrorResult) {
                    Timber.e(result.throwable, "ComposeToImage: Failed to load cover art: $coverArtUrl")
                }
            } catch (e: Exception) {
                Timber.e(e, "ComposeToImage: Exception loading cover art: $coverArtUrl")
            }
        }

        // Draw Background
        val backgroundRect = RectF(0f, 0f, imageWidth.toFloat(), imageHeight.toFloat())
        val backgroundPaint = Paint().apply {
            isAntiAlias = true
        }

        when (backgroundStyle) {
            LyricsBackgroundStyle.SOLID -> {
                backgroundPaint.color = bgColor
                canvas.drawRect(backgroundRect, backgroundPaint)
            }
            LyricsBackgroundStyle.BLUR -> {
                // Draw black base
                backgroundPaint.color = 0xFF000000.toInt()
                canvas.drawRect(backgroundRect, backgroundPaint)

                if (coverArtBitmap != null) {
                    try {
                        // Create a scaled down version for blurring (performance)
                        val scaledBitmap = Bitmap.createScaledBitmap(coverArtBitmap, imageWidth / 10, imageHeight / 10, true)
                        val blurredBitmap = fastBlur(scaledBitmap, 1f, 20) // Radius 20 on small image is large blur
                        
                        if (blurredBitmap != null) {
                            val blurRect = RectF(0f, 0f, imageWidth.toFloat(), imageHeight.toFloat())
                            canvas.drawBitmap(blurredBitmap, null, blurRect, null)
                            
                            // High-contrast gradient scrim to prevent white/light cover art from washing out text
                            val overlayPaint = Paint().apply {
                                shader = LinearGradient(
                                    0f, 0f, 0f, imageHeight.toFloat(),
                                    intArrayOf(
                                        0x9E000000.toInt(), // ~62% black at top
                                        0x7D000000.toInt(), // ~49% black at center
                                        0x9E000000.toInt()  // ~62% black at bottom
                                    ),
                                    null,
                                    Shader.TileMode.CLAMP
                                )
                                isAntiAlias = true
                            }
                            canvas.drawRect(blurRect, overlayPaint)
                        }
                    } catch (e: Exception) {
                        // Fallback to solid
                        backgroundPaint.color = bgColor
                        canvas.drawRect(backgroundRect, backgroundPaint)
                    }
                } else {
                    backgroundPaint.color = bgColor
                    canvas.drawRect(backgroundRect, backgroundPaint)
                }
            }
            LyricsBackgroundStyle.GRADIENT -> {
                if (coverArtBitmap != null) {
                    val palette = Palette.from(coverArtBitmap).generate()
                    val vibrant = palette.getVibrantColor(bgColor)
                    val darkVibrant = palette.getDarkVibrantColor(bgColor)
                    
                    val gradient = LinearGradient(
                        0f, 0f, imageWidth.toFloat(), imageHeight.toFloat(),
                        intArrayOf(vibrant, darkVibrant),
                        null,
                        Shader.TileMode.CLAMP
                    )
                    backgroundPaint.shader = gradient
                    canvas.drawRect(backgroundRect, backgroundPaint)
                } else {
                    backgroundPaint.color = bgColor
                    canvas.drawRect(backgroundRect, backgroundPaint)
                }
            }
        }
        
        val cornerRadius = 18f * scale

        // Draw inner border
        val borderPaint = Paint().apply {
            color = mainTextColor
            alpha = (255 * 0.12).toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f * scale
            isAntiAlias = true
        }
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, borderPaint)

        // --- Top Right Section: App Name & Badge ---
        val logoIconSize = 20f * scale
        val headerY = padding

        val appName = context.getString(R.string.app_name)
        val appNamePaint = TextPaint().apply {
            color = 0xFFFFFFFF.toInt() // Solid crisp white text
            textSize = 13f * scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            letterSpacing = 0.05f
        }
        val appNameWidth = appNamePaint.measureText(appName)

        val headerRight = imageWidth - padding
        val logoX = headerRight - logoIconSize
        val appNameX = logoX - (7f * scale) - appNameWidth
        val appNameY = headerY + logoIconSize / 2f - (appNamePaint.descent() + appNamePaint.ascent()) / 2f
        canvas.drawText(appName, appNameX, appNameY, appNamePaint)

        // Draw Official New Vivi Music App Icon (R.drawable.icon) with circular clipping
        val rawLogo = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.icon)?.toBitmap(
            width = logoIconSize.roundToInt(),
            height = logoIconSize.roundToInt()
        )
        rawLogo?.let {
            val logoRect = RectF(logoX, headerY, logoX + logoIconSize, headerY + logoIconSize)
            val logoPath = Path().apply {
                addOval(logoRect, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(logoPath)
            canvas.drawBitmap(it, null, logoRect, null)
            canvas.restore()
        }

        // --- Bottom Left Section: Song Details & Cover Art ---
        val coverCornerRadius = 8f * scale
        val footerY = imageHeight - padding - coverArtSize

        coverArtBitmap?.let {
            val rect = RectF(padding, footerY, padding + coverArtSize, footerY + coverArtSize)
            val path = Path().apply {
                addRoundRect(rect, coverCornerRadius, coverCornerRadius, Path.Direction.CW)
            }

            val coverBorderPaint = Paint().apply {
                color = mainTextColor
                alpha = (255 * 0.16).toInt()
                style = Paint.Style.STROKE
                strokeWidth = 1f * scale
                isAntiAlias = true
            }

            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(it, null, rect, null)
            canvas.restore()
            canvas.drawRoundRect(rect, coverCornerRadius, coverCornerRadius, coverBorderPaint)
        }

        val textStartX = padding + coverArtSize + (12f * scale)
        val textMaxWidth = imageWidth - textStartX - padding

        val titlePaint = TextPaint().apply {
            color = mainTextColor
            textSize = 18f * scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val artistPaint = TextPaint().apply {
            color = secondaryTxtColor
            textSize = 14f * scale
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val titleLayout = StaticLayout.Builder.obtain(songTitle, 0, songTitle.length, titlePaint, textMaxWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(1)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

        val artistLayout = StaticLayout.Builder.obtain(artistName, 0, artistName.length, artistPaint, textMaxWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(1)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

        val footerTextHeight = titleLayout.height + artistLayout.height + (2f * scale)
        val footerCenterY = footerY + coverArtSize / 2f
        val titleY = footerCenterY - footerTextHeight / 2f

        canvas.save()
        canvas.translate(textStartX, titleY)
        titleLayout.draw(canvas)
        canvas.translate(0f, titleLayout.height.toFloat() + (2f * scale))
        artistLayout.draw(canvas)
        canvas.restore()

        // --- Center Lyrics Section ---
        val lyricsTop = headerY + logoBoxSize + topSpacer
        canvas.save()
        canvas.translate(padding, lyricsTop)
        lyricsLayout.draw(canvas)
        canvas.restore()

        return@withContext bitmap
    }

    // Stack Blur v1.0 from http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
    // Java Author: Mario Klingemann <mario at quasimondo.com>
    // http://incubator.quasimondo.com
    //
    // created Feburary 29, 2004
    // Android port : Yahel Bouaziz <yahel at kayenko.com>
    // http://www.kayenko.com
    // ported to Kotlin and adapted
    private fun fastBlur(sentBitmap: Bitmap, scale: Float, radius: Int): Bitmap? {
        val width = (sentBitmap.width * scale).roundToInt()
        val height = (sentBitmap.height * scale).roundToInt()
        
        if (width <= 0 || height <= 0) return null
        
        val bitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))
        var divsum = div + 1 shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }
        yw = 0
        yi = 0
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        var r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        y = 0
        while (y < h) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = -0x1000000 or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    fun saveBitmapAsFile(context: Context, bitmap: Bitmap, fileName: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/vivimusic")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw IllegalStateException("Failed to create new MediaStore record")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            uri
        } else {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val imageFile = File(cachePath, "$fileName.png")
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.FileProvider",
                imageFile
            )
        }
    }
}