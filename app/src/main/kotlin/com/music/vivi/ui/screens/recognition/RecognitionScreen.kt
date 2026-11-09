@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)

/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.recognition

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.shazamkit.models.RecognitionResult
import com.music.shazamkit.models.RecognitionStatus
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.db.entities.RecognitionHistory
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.recognition.MusicRecognitionService
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.listItemShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

@Composable
fun RecognitionScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var hasInitialized by rememberSaveable { mutableStateOf(false) }
    var lastSavedTrackId by rememberSaveable { mutableStateOf<String?>(null) }

    fun resetToReady() {
        lastSavedTrackId = null
        MusicRecognitionService.stopRecognizing()
    }

    LaunchedEffect(Unit) {
        if (!hasInitialized) {
            hasInitialized = true
            resetToReady()
        }
    }

    val recognitionStatus by MusicRecognitionService.recognitionStatus.collectAsState()

    BackHandler(enabled = recognitionStatus !is RecognitionStatus.Ready) {
        resetToReady()
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            MusicRecognitionService.startRecognizing(context)
        }
    }

    fun startRecognition() {
        lastSavedTrackId = null
        if (hasPermission) {
            MusicRecognitionService.startRecognizing(context)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun saveToHistory(result: RecognitionResult) {
        if (lastSavedTrackId == result.trackId) return
        lastSavedTrackId = result.trackId
        coroutineScope.launch(Dispatchers.IO) {
            database.query {
                insert(
                    RecognitionHistory(
                        trackId = result.trackId,
                        title = result.title,
                        artist = result.artist,
                        album = result.album,
                        coverArtUrl = result.coverArtUrl,
                        coverArtHqUrl = result.coverArtHqUrl,
                        genre = result.genre,
                        releaseDate = result.releaseDate,
                        label = result.label,
                        shazamUrl = result.shazamUrl,
                        appleMusicUrl = result.appleMusicUrl,
                        spotifyUrl = result.spotifyUrl,
                        isrc = result.isrc,
                        youtubeVideoId = result.youtubeVideoId,
                        recognizedAt = LocalDateTime.now()
                    )
                )
            }
        }
    }

    fun playRecognizedSong(result: RecognitionResult) {
        val videoId = result.youtubeVideoId
        if (videoId != null && playerConnection != null) {
            playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = videoId)))
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            val query = "${result.title} ${result.artist}"
            val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
            val songs = searchResult?.items?.filterIsInstance<SongItem>().orEmpty()
            val bestMatch = songs.firstOrNull { s ->
                s.title.contains(result.title, ignoreCase = true) ||
                s.artists.any { a -> a.name.contains(result.artist, ignoreCase = true) }
            } ?: songs.firstOrNull()

            withContext(Dispatchers.Main) {
                if (bestMatch != null && playerConnection != null) {
                    playerConnection.playQueue(
                        YouTubeQueue(
                            WatchEndpoint(videoId = bestMatch.id),
                            bestMatch.toMediaMetadata()
                        )
                    )
                } else {
                    navController.navigate("search/${java.net.URLEncoder.encode(query, "UTF-8")}")
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = LocalPlayerAwareWindowInsets.current,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = stringResource(R.string.recognize_music),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (recognitionStatus !is RecognitionStatus.Ready) {
                                resetToReady()
                            } else {
                                navController.navigateUp()
                            }
                        },
                        onLongClick = {
                            resetToReady()
                            navController.backToMain()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("recognition_history") },
                        onLongClick = {}
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.history),
                            contentDescription = stringResource(R.string.recognition_history)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = recognitionStatus,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            transitionSpec = { fadeIn(tween(250)).togetherWith(fadeOut(tween(150))) },
            label = "recognition_content"
        ) { status ->
            when (status) {
                is RecognitionStatus.Ready -> ReadyState(
                    onStartRecognition = ::startRecognition,
                    isLandscape = isLandscape
                )

                is RecognitionStatus.Listening -> ListeningState(
                    onCancel = ::resetToReady,
                    isLandscape = isLandscape
                )

                is RecognitionStatus.Processing -> ProcessingState(
                    isLandscape = isLandscape
                )

                is RecognitionStatus.Success -> SuccessState(
                    result = status.result,
                    onPlayOnApp = ::playRecognizedSong,
                    onTryAgain = ::startRecognition,
                    onClose = ::resetToReady,
                    onSaveToHistory = ::saveToHistory,
                    isLandscape = isLandscape
                )

                is RecognitionStatus.NoMatch -> MessageState(
                    shape = MaterialShapes.Sunny.toShape(),
                    icon = R.drawable.close,
                    title = stringResource(R.string.no_match_found),
                    message = status.message,
                    onCancel = ::resetToReady,
                    onTryAgain = ::startRecognition,
                    isLandscape = isLandscape
                )

                is RecognitionStatus.Error -> MessageState(
                    shape = MaterialShapes.Sunny.toShape(),
                    icon = R.drawable.error,
                    title = stringResource(R.string.recognition_error),
                    message = status.message,
                    onCancel = ::resetToReady,
                    onTryAgain = ::startRecognition,
                    isLandscape = isLandscape
                )
            }
        }
    }
}

/** A Material expressive shape (cookie, clover, sunny...) filled with a colour, with optional content on top. */
@Composable
private fun ShapeBadge(
    shape: Shape,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    rotation: () -> Float = { 0f },
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = rotation() }
                .background(color, shape)
        )
        content()
    }
}

/** Large left-aligned heading used at the top of each state. */
@Composable
private fun ScreenHeading(
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    isLandscape: Boolean = false
) {
    Column(
        modifier = Modifier.padding(
            horizontal = if (isLandscape) 0.dp else 24.dp,
            vertical = if (isLandscape) 0.dp else 8.dp
        ),
        horizontalAlignment = if (isLandscape) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Text(
            text = title,
            style = if (isLandscape) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = titleColor,
            textAlign = if (isLandscape) TextAlign.Center else TextAlign.Start
        )
        Text(
            text = subtitle,
            style = if (isLandscape) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (isLandscape) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.padding(top = if (isLandscape) 4.dp else 8.dp)
        )
    }
}

@Composable
private fun ReadyState(
    onStartRecognition: () -> Unit,
    isLandscape: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val cookie9 = MaterialShapes.Cookie9Sided.toShape()
    val cookie12 = MaterialShapes.Cookie12Sided.toShape()

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                ShapeBadge(
                    shape = cookie12,
                    color = colors.primaryContainer,
                    size = 190.dp,
                    rotation = { -8f }
                )
                ShapeBadge(
                    shape = cookie9,
                    color = colors.primary,
                    size = 136.dp,
                    rotation = { 12f },
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onStartRecognition)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.hearing),
                        contentDescription = stringResource(R.string.tap_to_recognize),
                        modifier = Modifier.size(58.dp),
                        tint = colors.onPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                ScreenHeading(
                    title = stringResource(R.string.recognize_music),
                    subtitle = stringResource(R.string.hold_near_music),
                    isLandscape = true
                )

                Box(
                    modifier = Modifier.height(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.tap_to_recognize_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeading(
                title = stringResource(R.string.recognize_music),
                subtitle = stringResource(R.string.hold_near_music)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ShapeBadge(
                        shape = cookie12,
                        color = colors.primaryContainer,
                        size = 290.dp,
                        rotation = { -8f }
                    )
                    ShapeBadge(
                        shape = cookie9,
                        color = colors.primary,
                        size = 206.dp,
                        rotation = { 12f },
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onStartRecognition)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.hearing),
                            contentDescription = stringResource(R.string.tap_to_recognize),
                            modifier = Modifier.size(88.dp),
                            tint = colors.onPrimary
                        )
                    }
                }

                Spacer(Modifier.height(72.dp))

                Box(
                    modifier = Modifier.height(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.tap_to_recognize_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(88.dp))
        }
    }
}

private val EqualizerHeights = listOf(20, 36, 56, 30, 68, 42, 24, 58, 34)

@Composable
private fun ListeningState(
    onCancel: () -> Unit,
    isLandscape: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val cookie9 = MaterialShapes.Cookie9Sided.toShape()
    val cookie12 = MaterialShapes.Cookie12Sided.toShape()

    // Only the sound bars animate while listening.
    val transition = rememberInfiniteTransition(label = "equalizer")

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                ShapeBadge(
                    shape = cookie12,
                    color = colors.primaryContainer,
                    size = 190.dp,
                    rotation = { -8f }
                )
                ShapeBadge(
                    shape = cookie9,
                    color = colors.primary,
                    size = 136.dp,
                    rotation = { 12f }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.hearing),
                        contentDescription = null,
                        modifier = Modifier.size(58.dp),
                        tint = colors.onPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                ScreenHeading(
                    title = stringResource(R.string.listening),
                    subtitle = stringResource(R.string.keep_phone_close),
                    titleColor = colors.primary,
                    isLandscape = true
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(52.dp)
                ) {
                    EqualizerHeights.forEachIndexed { index, barHeight ->
                        val level by transition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(450 + index * 60, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse,
                                initialStartOffset = StartOffset(index * 80)
                            ),
                            label = "bar$index"
                        )
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(((barHeight * 0.7f) * level).dp)
                                .background(
                                    color = if (index % 2 == 0) colors.primary else colors.primary.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Surface(
                    onClick = onCancel,
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceContainerHighest,
                    contentColor = colors.onSurface
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeading(
                title = stringResource(R.string.listening),
                subtitle = stringResource(R.string.keep_phone_close),
                titleColor = colors.primary
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ShapeBadge(
                        shape = cookie12,
                        color = colors.primaryContainer,
                        size = 290.dp,
                        rotation = { -8f }
                    )
                    ShapeBadge(
                        shape = cookie9,
                        color = colors.primary,
                        size = 206.dp,
                        rotation = { 12f }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.hearing),
                            contentDescription = null,
                            modifier = Modifier.size(88.dp),
                            tint = colors.onPrimary
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(72.dp)
                ) {
                    EqualizerHeights.forEachIndexed { index, barHeight ->
                        val level by transition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(450 + index * 60, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse,
                                initialStartOffset = StartOffset(index * 80)
                            ),
                            label = "bar$index"
                        )
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height((barHeight * level).dp)
                                .background(
                                    color = if (index % 2 == 0) colors.primary else colors.primary.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Surface(
                    onClick = onCancel,
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceContainerHighest,
                    contentColor = colors.onSurface
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }

            Spacer(Modifier.height(88.dp))
        }
    }
}

@Composable
private fun ProcessingState(isLandscape: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val cookie9 = MaterialShapes.Cookie9Sided.toShape()
    val cookie12 = MaterialShapes.Cookie12Sided.toShape()

    val infiniteTransition = rememberInfiniteTransition(label = "processing_rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                ShapeBadge(
                    shape = cookie12,
                    color = colors.primaryContainer,
                    size = 190.dp,
                    rotation = { -rotation * 0.25f }
                )
                ShapeBadge(
                    shape = cookie9,
                    color = colors.primary,
                    size = 136.dp,
                    rotation = { rotation * 0.5f }
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .graphicsLayer { rotationZ = rotation }
                            .border(
                                width = 3.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        colors.onPrimary,
                                        colors.onPrimary.copy(alpha = 0.5f),
                                        Color.Transparent,
                                        colors.onPrimary.copy(alpha = 0.5f),
                                        colors.onPrimary
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    Icon(
                        painter = painterResource(R.drawable.hearing),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = colors.onPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                ScreenHeading(
                    title = stringResource(R.string.processing),
                    subtitle = stringResource(R.string.identifying_song),
                    titleColor = colors.primary,
                    isLandscape = true
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeading(
                title = stringResource(R.string.processing),
                subtitle = stringResource(R.string.identifying_song),
                titleColor = colors.primary
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ShapeBadge(
                        shape = cookie12,
                        color = colors.primaryContainer,
                        size = 290.dp,
                        rotation = { -rotation * 0.25f }
                    )
                    ShapeBadge(
                        shape = cookie9,
                        color = colors.primary,
                        size = 206.dp,
                        rotation = { rotation * 0.5f }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .graphicsLayer { rotationZ = rotation }
                            .border(
                                width = 4.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        colors.onPrimary,
                                        colors.onPrimary.copy(alpha = 0.5f),
                                        Color.Transparent,
                                        colors.onPrimary.copy(alpha = 0.5f),
                                        colors.onPrimary
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    Icon(
                        painter = painterResource(R.drawable.hearing),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colors.onPrimary
                    )
                }
            }

            Spacer(Modifier.height(72.dp))

            Spacer(Modifier.height(36.dp))
        }

        Spacer(Modifier.height(88.dp))
    }
    }
}

private data class DetailRow(
    val icon: Int,
    val title: String,
    val label: String,
    val imageUrl: String? = null
)

@Composable
private fun SuccessState(
    result: RecognitionResult,
    onPlayOnApp: (RecognitionResult) -> Unit,
    onTryAgain: () -> Unit,
    onClose: () -> Unit,
    onSaveToHistory: (RecognitionResult) -> Unit,
    isLandscape: Boolean = false
) {
    LaunchedEffect(result) { onSaveToHistory(result) }

    val database = LocalDatabase.current
    val colors = MaterialTheme.colorScheme
    val cookie9 = MaterialShapes.Cookie9Sided.toShape()
    val clover = MaterialShapes.Clover4Leaf.toShape()

    var artistImageUrl by remember(result.artist) { mutableStateOf<String?>(null) }
    var albumImageUrl by remember(result.album, result.coverArtHqUrl, result.coverArtUrl) {
        mutableStateOf(result.coverArtHqUrl ?: result.coverArtUrl)
    }

    LaunchedEffect(result.artist) {
        val localThumb: String? = withContext(Dispatchers.IO) {
            try {
                database.artistByName(result.artist)?.thumbnailUrl
            } catch (_: Exception) {
                null
            }
        }
        if (localThumb != null) {
            artistImageUrl = localThumb
        } else {
            val onlineThumb: String? = withContext(Dispatchers.IO) {
                try {
                    YouTube.search(result.artist, YouTube.SearchFilter.FILTER_ARTIST)
                        .getOrNull()
                        ?.items
                        ?.filterIsInstance<ArtistItem>()
                        ?.firstOrNull()
                        ?.thumbnail
                } catch (_: Exception) {
                    null
                }
            }
            if (onlineThumb != null) {
                artistImageUrl = onlineThumb
            }
        }
    }

    LaunchedEffect(result.album, result.artist) {
        if (albumImageUrl == null && !result.album.isNullOrBlank()) {
            val onlineThumb: String? = withContext(Dispatchers.IO) {
                try {
                    YouTube.search("${result.album} ${result.artist}", YouTube.SearchFilter.FILTER_ALBUM)
                        .getOrNull()
                        ?.items
                        ?.filterIsInstance<AlbumItem>()
                        ?.firstOrNull()
                        ?.thumbnail
                } catch (_: Exception) {
                    null
                }
            }
            if (onlineThumb != null) {
                albumImageUrl = onlineThumb
            }
        }
    }

    // Only rows that have data are shown.
    val rows = buildList {
        add(
            DetailRow(
                icon = R.drawable.person,
                title = result.artist,
                label = stringResource(R.string.artist),
                imageUrl = artistImageUrl
            )
        )
        result.album?.takeIf { it.isNotBlank() }
            ?.let {
                add(
                    DetailRow(
                        icon = R.drawable.album,
                        title = it,
                        label = stringResource(R.string.album),
                        imageUrl = albumImageUrl
                    )
                )
            }
        result.genre?.takeIf { it.isNotBlank() }
            ?.let { add(DetailRow(R.drawable.label, it, stringResource(R.string.genre))) }
        result.releaseDate?.toString()?.takeIf { it.isNotBlank() }
            ?.let { add(DetailRow(R.drawable.calendar_today, it, stringResource(R.string.released))) }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.85f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(160.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .shadow(8.dp, cookie9)
                            .clip(cookie9)
                            .background(
                                Brush.linearGradient(
                                    listOf(colors.primary, colors.primaryContainer, colors.tertiaryContainer)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.music_note),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = colors.onPrimary
                        )
                        AsyncImage(
                            model = result.coverArtHqUrl ?: result.coverArtUrl,
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    ShapeBadge(
                        shape = clover,
                        color = colors.tertiaryContainer,
                        size = 46.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = (-2).dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = colors.onTertiaryContainer
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.match_found_saved),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                rows.forEachIndexed { index, row ->
                    DetailRowItem(row = row, shape = listItemShape(index, rows.size))
                }
                Spacer(Modifier.height(16.dp))

                ActionButtonGroup(
                    onPlay = { onPlayOnApp(result) },
                    onTryAgain = onTryAgain,
                    onClose = onClose
                )

                Spacer(Modifier.height(50.dp))
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Large cookie album art with a clover check badge
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(236.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .shadow(12.dp, cookie9)
                        .clip(cookie9)
                        .background(
                            Brush.linearGradient(
                                listOf(colors.primary, colors.primaryContainer, colors.tertiaryContainer)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        modifier = Modifier.size(92.dp),
                        tint = colors.onPrimary
                    )
                    AsyncImage(
                        model = result.coverArtHqUrl ?: result.coverArtUrl,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                ShapeBadge(
                    shape = clover,
                    color = colors.tertiaryContainer,
                    size = 68.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = colors.onTertiaryContainer
                    )
                }
            }

            // Title block
            Column(modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 18.dp, bottom = 16.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.match_found_saved),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            // Segmented rows: big outer corners, small inner corners
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                rows.forEachIndexed { index, row ->
                    DetailRowItem(row = row, shape = listItemShape(index, rows.size))
                }
            }
            Spacer(Modifier.height(24.dp))

            ActionButtonGroup(
                onPlay = { onPlayOnApp(result) },
                onTryAgain = onTryAgain,
                onClose = onClose
            )

            Spacer(Modifier.height(50.dp))
        }
    }
}

@Composable
private fun DetailRowItem(row: DetailRow, shape: Shape) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(row.icon),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (row.imageUrl != null) {
                    AsyncImage(
                        model = row.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Column {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Connected button group: Play in vivi | listen again | close. */
@Composable
private fun ActionButtonGroup(
    onPlay: () -> Unit,
    onTryAgain: () -> Unit,
    onClose: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val outer = 32.dp
    val inner = 8.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Button(
            onClick = onPlay,
            shape = RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner),
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.play_on_app),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Button(
            onClick = onTryAgain,
            shape = RoundedCornerShape(inner),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer
            ),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .width(68.dp)
                .height(64.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.hearing),
                contentDescription = stringResource(R.string.re_listen),
                modifier = Modifier.size(26.dp)
            )
        }
        Button(
            onClick = onClose,
            shape = RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceContainerHighest,
                contentColor = colors.onSurface
            ),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .width(68.dp)
                .height(64.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = stringResource(R.string.close),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun MessageState(
    shape: Shape,
    icon: Int,
    title: String,
    message: String,
    onCancel: () -> Unit,
    onTryAgain: () -> Unit,
    isLandscape: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val outer = 28.dp
    val inner = 8.dp

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.weight(0.9f),
                contentAlignment = Alignment.Center
            ) {
                ShapeBadge(shape = shape, color = colors.errorContainer, size = 130.dp) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = colors.onErrorContainer
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        shape = RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceContainerHighest,
                            contentColor = colors.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.cancel),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onTryAgain,
                        shape = RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.refresh),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.try_again),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        ) {
            ShapeBadge(shape = shape, color = colors.errorContainer, size = 160.dp) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = colors.onErrorContainer
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Button(
                    onClick = onCancel,
                    shape = RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceContainerHighest,
                        contentColor = colors.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onTryAgain,
                    shape = RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.refresh),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.try_again),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
