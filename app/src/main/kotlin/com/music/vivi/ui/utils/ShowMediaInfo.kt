package com.music.vivi.ui.utils

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import com.music.innertube.YouTube
import com.music.innertube.models.MediaInfo
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.PureBlackKey
import com.music.vivi.db.entities.FormatEntity
import com.music.vivi.db.entities.Song
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.MenuState
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.component.shimmer.TextPlaceholder
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.*


@Composable
fun ShowMediaInfo(videoId: String) {
    if (videoId.isBlank() || videoId.isEmpty()) return

    val windowInsets = WindowInsets.systemBars

    var info by remember {
        mutableStateOf<MediaInfo?>(null)
    }

    val database = LocalDatabase.current
    var song by remember { mutableStateOf<Song?>(null) }

    var currentFormat by remember { mutableStateOf<FormatEntity?>(null) }

    val playerConnection = LocalPlayerConnection.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current

    // Animation states
    val animatedVisibilityState = remember { MutableTransitionState(false) }
    LaunchedEffect(info) {
        animatedVisibilityState.targetState = info != null
    }

    LaunchedEffect(Unit, videoId) {
        info = YouTube.getMediaInfo(videoId).getOrNull()
    }
    LaunchedEffect(Unit, videoId) {
        database.song(videoId).collect {
            song = it
        }
    }
    LaunchedEffect(Unit, videoId) {
        database.format(videoId).collect {
            currentFormat = it
        }
    }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier
            .padding(windowInsets.asPaddingValues())
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    )
                )
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Hero Header
        item(contentType = "HeroHeader") {
            AnimatedVisibility(
                visibleState = animatedVisibilityState,
                enter = slideInVertically(
                    animationSpec = tween(800, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(800))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        Color.Transparent
                                    ),
                                    radius = 300f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.info),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.details),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Media Information",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (song != null) {
            // Track Details Card
            item(contentType = "TrackDetails") {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 200))
                ) {
                    ModernDetailsCard(
                        title = "Track Details",
                        icon = Icons.Rounded.MusicNote
                    ) {
                        val baseList = listOf(
                            stringResource(R.string.song_title) to song?.title,
                            stringResource(R.string.song_artists) to song?.artists?.joinToString { it.name },
                            stringResource(R.string.media_id) to song?.id
                        )
                        val extendedList = baseList + if (currentFormat != null) {
                            listOf(
                                "Itag" to currentFormat?.itag?.toString(),
                                stringResource(R.string.mime_type) to currentFormat?.mimeType,
                                stringResource(R.string.codecs) to currentFormat?.codecs,
                                stringResource(R.string.bitrate) to currentFormat?.bitrate?.let { "${it / 1000} Kbps" },
                                stringResource(R.string.sample_rate) to currentFormat?.sampleRate?.let { "$it Hz" },
                                stringResource(R.string.loudness) to currentFormat?.loudnessDb?.let { "$it dB" },
                                stringResource(R.string.volume) to if (playerConnection != null)
                                    "${(playerConnection.player.volume * 100).toInt()}%" else null,
                                stringResource(R.string.file_size) to
                                        currentFormat?.contentLength?.let {
                                            Formatter.formatShortFileSize(context, it)
                                        }
                            )
                        } else {
                            emptyList()
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            extendedList.forEachIndexed { index, item ->
                                val label = item.first
                                val text = item.second
                                val displayText = text ?: stringResource(R.string.unknown)

                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInHorizontally(
                                        initialOffsetX = { -it },
                                        animationSpec = tween(400, delayMillis = 100 * index, easing = FastOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(400, delayMillis = 100 * index))
                                ) {
                                    DetailItem(
                                        label = label,
                                        value = displayText,
                                        context = context
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Media Info Section
        if (info != null) {
            if (song == null) {
                item(contentType = "MediaTitle") {
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            animationSpec = tween(600, delayMillis = 300, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 300))
                    ) {
                        ModernDetailsCard(
                            title = "Media Title",
                            icon = Icons.Rounded.PlayArrow
                        ) {
                            Text(
                                text = info?.title ?: "",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            item(contentType = "MediaAuthor") {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        animationSpec = tween(600, delayMillis = 400, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 400))
                ) {
                    ModernDetailsCard(
                        title = stringResource(R.string.artists),
                        icon = Icons.Rounded.Person
                    ) {
                        Text(
                            text = info?.author ?: "",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            item(contentType = "MediaDescription") {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        animationSpec = tween(600, delayMillis = 500, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 500))
                ) {
                    ModernDetailsCard(
                        title = stringResource(R.string.description),
                        icon = Icons.Rounded.Description
                    ) {
                        Text(
                            text = info?.description ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            item(contentType = "MediaStats") {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        animationSpec = tween(600, delayMillis = 600, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 600))
                ) {
                    ModernDetailsCard(
                        title = stringResource(R.string.numbers),
                        icon = Icons.Rounded.Analytics
                    ) {
                        StatsGrid(info = info)
                    }
                }
            }
        } else {
            item(contentType = "MediaInfoLoader") {
                ModernLoadingCard()
            }
        }
    }
}

@Composable
private fun ModernDetailsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    context: Context
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("text", value))
                        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                    }
                )
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatsGrid(info: MediaInfo?) {
    val stats = listOf(
        Triple("Subscribers", info?.subscribers ?: "", Icons.Rounded.Group),
        Triple("Views", info?.viewCount?.toInt()?.let { numberFormatter(it) } ?: "", Icons.Rounded.Visibility),
        Triple("Likes", info?.like?.toInt()?.let { numberFormatter(it) } ?: "", Icons.Rounded.ThumbUp),
        Triple("Dislikes", info?.dislike?.toInt()?.let { numberFormatter(it) } ?: "", Icons.Rounded.ThumbDown)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(200.dp)
    ) {
        itemsIndexed(stats) { index, (label, value, icon) ->
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(
                    animationSpec = tween(400, delayMillis = 100 * index, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400, delayMillis = 100 * index))
            ) {
                StatCard(label = label, value = value, icon = icon)
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ModernLoadingCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading media info...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}