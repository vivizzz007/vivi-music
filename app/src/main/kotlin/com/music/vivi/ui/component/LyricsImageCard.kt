/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.vivi.R
import com.music.vivi.models.MediaMetadata

import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberAdjustedFontSize(
    text: String,
    maxWidth: Dp,
    maxHeight: Dp,
    density: Density,
    initialFontSize: TextUnit = 20.sp,
    minFontSize: TextUnit = 14.sp,
    style: TextStyle = TextStyle.Default,
    textMeasurer: androidx.compose.ui.text.TextMeasurer? = null
): TextUnit {
    val measurer = textMeasurer ?: rememberTextMeasurer()

    var calculatedFontSize by remember(text, maxWidth, maxHeight, style, density) {
        val initialSize = when {
            text.length < 50 -> initialFontSize
            text.length < 100 -> (initialFontSize.value * 0.8f).sp
            text.length < 200 -> (initialFontSize.value * 0.6f).sp
            else -> (initialFontSize.value * 0.5f).sp
        }
        mutableStateOf(initialSize)
    }

    LaunchedEffect(key1 = text, key2 = maxWidth, key3 = maxHeight) {
        val targetWidthPx = with(density) { maxWidth.toPx() * 0.92f }
        val targetHeightPx = with(density) { maxHeight.toPx() * 0.92f }
        if (text.isBlank()) {
            calculatedFontSize = minFontSize
            return@LaunchedEffect
        }

        if (text.length < 20) {
            val largerSize = (initialFontSize.value * 1.1f).sp
            val result = measurer.measure(
                text = AnnotatedString(text),
                style = style.copy(fontSize = largerSize)
            )
            if (result.size.width <= targetWidthPx && result.size.height <= targetHeightPx) {
                calculatedFontSize = largerSize
                return@LaunchedEffect
            }
        } else if (text.length < 30) {
            val largerSize = (initialFontSize.value * 0.9f).sp
            val result = measurer.measure(
                text = AnnotatedString(text),
                style = style.copy(fontSize = largerSize)
            )
            if (result.size.width <= targetWidthPx && result.size.height <= targetHeightPx) {
                calculatedFontSize = largerSize
                return@LaunchedEffect
            }
        }

        var minSize = minFontSize.value
        var maxSize = initialFontSize.value
        var bestFit = minSize
        var iterations = 0

        while (minSize <= maxSize && iterations < 20) {
            iterations++
            val midSize = (minSize + maxSize) / 2
            val midSizeSp = midSize.sp

            val result = measurer.measure(
                text = AnnotatedString(text),
                style = style.copy(fontSize = midSizeSp)
            )

            if (result.size.width <= targetWidthPx && result.size.height <= targetHeightPx) {
                bestFit = midSize
                minSize = midSize + 0.5f
            } else {
                maxSize = midSize - 0.5f
            }
        }

        calculatedFontSize = if (bestFit < minFontSize.value) minFontSize else bestFit.sp
    }

    return calculatedFontSize
}

enum class LyricsBackgroundStyle {
    SOLID,
    BLUR,
    GRADIENT
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LyricsImageCard(
    lyricText: String,
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    darkBackground: Boolean = true,
    backgroundColor: Color? = null,
    backgroundStyle: LyricsBackgroundStyle = LyricsBackgroundStyle.SOLID,
    textColor: Color? = null,
    secondaryTextColor: Color? = null,
    textAlign: TextAlign = TextAlign.Start
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val cardCornerRadius = 18.dp
    val padding = 18.dp
    val coverArtSize = 44.dp

    val defaultBgColor = if (darkBackground) Color(0xFF121212) else Color(0xFFF5F5F5)
    val backgroundSolidColor = backgroundColor ?: defaultBgColor
    
    val mainTextColor = textColor ?: if (darkBackground) Color.White else Color.Black
    val secondaryColor = secondaryTextColor ?: if (darkBackground) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)

    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(context)
            .data(mediaMetadata.thumbnailUrl)
            .crossfade(false)
            .build()
    )
    
    // Calculate gradient colors if needed
    var gradientBrush by remember { mutableStateOf<Brush?>(null) }
    
    if (backgroundStyle == LyricsBackgroundStyle.GRADIENT) {
        LaunchedEffect(mediaMetadata.thumbnailUrl) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = SingletonImageLoader.get(context)
                    val req = ImageRequest.Builder(context).data(mediaMetadata.thumbnailUrl).allowHardware(false).build()
                    val result = loader.execute(req)
                    val bmp = result.image?.toBitmap()
                    if (bmp != null) {
                        val palette = Palette.from(bmp).generate()
                        val vibrant = palette.getVibrantColor(defaultBgColor.toArgb())
                        val muted = palette.getMutedColor(defaultBgColor.toArgb())
                        val darkVibrant = palette.getDarkVibrantColor(defaultBgColor.toArgb())
                        
                        val color1 = Color(vibrant)
                        val color2 = Color(darkVibrant)
                        
                        gradientBrush = Brush.linearGradient(
                            colors = listOf(color1, color2),
                            tileMode = TileMode.Clamp
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cardCornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        // Background Layer: matches content height
        Box(
            modifier = Modifier.matchParentSize()
        ) {
            when (backgroundStyle) {
                LyricsBackgroundStyle.SOLID -> {
                    Box(modifier = Modifier.fillMaxSize().background(backgroundSolidColor))
                }
                LyricsBackgroundStyle.BLUR -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painter,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(50.dp)
                        )
                        // Protective dark scrim on top of blur: prevents white/bright album art from washing out text
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.58f),
                                            Color.Black.copy(alpha = 0.48f),
                                            Color.Black.copy(alpha = 0.62f)
                                        )
                                    )
                                )
                        )
                    }
                }
                LyricsBackgroundStyle.GRADIENT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(gradientBrush ?: androidx.compose.ui.graphics.Brush.linearGradient(listOf(backgroundSolidColor, backgroundSolidColor)))
                    )
                }
            }
        }

        // Inner Border
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, mainTextColor.copy(alpha = 0.12f), RoundedCornerShape(cardCornerRadius))
        )

        // Content Column: height wraps content dynamically based on lyrics!
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            // Top Section: App name & badge on Top Right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = context.getString(R.string.app_name),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.05.em
                )
                Spacer(modifier = Modifier.width(7.dp))
                Image(
                    painter = painterResource(id = R.drawable.icon),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Middle Section: Lyrics text (natural height, expands as lines increase!)
            Text(
                text = lyricText,
                color = mainTextColor,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Bottom Section: Song details & cover art on Bottom Left
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, mainTextColor.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = mediaMetadata.title,
                        color = mainTextColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = mediaMetadata.artists.joinToString { it.name },
                        color = secondaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

