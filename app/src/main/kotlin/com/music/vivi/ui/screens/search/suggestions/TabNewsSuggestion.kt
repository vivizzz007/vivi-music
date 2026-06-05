/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.search.suggestions

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.music.vivi.R
import com.music.vivi.constants.SuggestionRegionKey
import com.music.vivi.constants.SuggestionRegionSlugToName
import com.music.vivi.utils.rememberPreference
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import kotlin.math.abs
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SuggestionsTabContent(
    navController: NavController,
    viewModel: SuggestionsViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val suggestionTracks by viewModel.suggestionTracks.collectAsState()
    val suggestionArtists by viewModel.suggestionArtists.collectAsState()
    val suggestionAlbums by viewModel.suggestionAlbums.collectAsState()
    val suggestionVideos by viewModel.suggestionVideos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isManualLoading by viewModel.isManualLoading.collectAsState()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val playerConnection = com.music.vivi.LocalPlayerConnection.current
    val context = LocalContext.current
    val (regionCode, _) = rememberPreference(
        key = SuggestionRegionKey,
        defaultValue = "system"
    )

    androidx.compose.runtime.LaunchedEffect(regionCode) {
        viewModel.refresh(regionCode)
    }

    val pullToRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isManualLoading,
        onRefresh = {
            viewModel.refresh(regionCode, force = true)
        },
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullToRefreshState,
                isRefreshing = isManualLoading,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding
        ) {
        if (isLoading && !isManualLoading && suggestionTracks == null && suggestionArtists == null && suggestionAlbums == null && suggestionVideos == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            }
        }

        suggestionTracks?.let { tracks ->
                item {
                    TrendingAppleMusicSection(
                        tracks = suggestionTracks!!,
                        countryCode = regionCode,
                        onTrackClick = { track ->
                            android.widget.Toast.makeText(context, "Loading ${track.title}...", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.playTrack(track, playerConnection)
                        },
                        onMoreClick = {
                            val code = if (regionCode == "system") java.util.Locale.getDefault().country.lowercase() else regionCode.lowercase()
                            uriHandler.openUri("https://music.apple.com/$code/charts")
                        }
                    )
                }
            }

            suggestionArtists?.let { artists ->
                item {
                    TopArtistsSection(
                        artists = artists,
                        onArtistClick = { artist ->
                            android.widget.Toast.makeText(context, "Loading ${artist.name}...", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.navigateToArtist(artist, navController)
                        }
                    )
                }
            }

            suggestionAlbums?.let { albums ->
                item {
                    TrendingAlbumsSection(
                        albums = albums,
                        onAlbumClick = { album ->
                            android.widget.Toast.makeText(context, "Loading ${album.title}...", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.navigateToAlbum(album, navController)
                        },
                        onMoreClick = {
                            val code = if (regionCode == "system") java.util.Locale.getDefault().country.lowercase() else regionCode.lowercase()
                            uriHandler.openUri("https://music.apple.com/$code/charts/albums")
                        }
                    )
                }
            }

            suggestionVideos?.let { videos ->
                item {
                    TrendingVideosSection(
                        videos = videos,
                        onVideoClick = { video ->
                            android.widget.Toast.makeText(context, "Loading video ${video.title}...", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.playVideo(video, playerConnection)
                        },
                        onMoreClick = {
                            val code = if (regionCode == "system") java.util.Locale.getDefault().country.lowercase() else regionCode.lowercase()
                            uriHandler.openUri("https://music.apple.com/$code/charts/videos")
                        }
                    )
                }
            }

            if (suggestionTracks == null && suggestionArtists == null && suggestionAlbums == null && suggestionVideos == null && !isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No suggestions available at the moment.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = { viewModel.refresh(regionCode, force = true) }) {
                                Text("Refresh")
                            }
                        }
                    }
                }
            }

            // Footer at the very bottom
            if (suggestionTracks != null || suggestionArtists != null || suggestionAlbums != null || suggestionVideos != null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Data from Apple Music",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "vivi-music",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TrendingAppleMusicSection(
    tracks: List<SuggestionTrack>,
    countryCode: String,
    onTrackClick: (SuggestionTrack) -> Unit,
    onMoreClick: () -> Unit
) {
    if (tracks.isEmpty()) return
    val displayTracks = tracks.take(29)
    val totalItems = displayTracks.size + 1
    val pagerState = rememberPagerState(pageCount = { (totalItems + 4) / 5 })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Apple Music Top 100",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 32.dp)
        )
        Text(
            text = SuggestionRegionSlugToName[countryCode] ?: "Global Charts",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth().animateContentSize(tween(300, easing = FastOutSlowInEasing))
        ) { page ->
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                val startIdx = page * 5
                val endIdx = minOf(startIdx + 5, totalItems)
                for (i in startIdx until endIdx) {
                    val isMoreCard = i == 29
                    val isTop = i == startIdx
                    val isBottom = i == endIdx - 1
                    val shape = when {
                        isTop && isBottom -> RoundedCornerShape(24.dp)
                        isTop -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        isBottom -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                        else -> RoundedCornerShape(4.dp)
                    }
                    if (isMoreCard) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable { onMoreClick() }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(painterResource(R.drawable.globe_search), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("View more on Apple Music", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    } else if (i < displayTracks.size) {
                        val track = displayTracks[i]
                        Row(modifier = Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceContainer).clickable { onTrackClick(track) }) {
                            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                Text(track.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                                Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)) {
                                    Text("#${track.rank}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    // Mock play count
                                    val playCount = remember(track.rank) { 
                                        val base = 2_500_000 / (track.rank + 2)
                                        if (base >= 1_000_000) String.format("%.1fM plays", base / 1_000_000f)
                                        else String.format("%dk plays", base / 1_000)
                                    }
                                    Text(playCount, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                }
                            }
                            if (track.thumbnailUrl != null) {
                                SubcomposeAsyncImage(
                                    model = track.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    loading = {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            LoadingIndicator()
                                        }
                                    },
                                    modifier = Modifier.padding(16.dp).clip(MaterialTheme.shapes.large).size(80.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainer)) {
                IconButton(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }, enabled = pagerState.currentPage > 0) {
                    Icon(painterResource(R.drawable.chevron_leftpx), "Previous")
                }
                Text(stringResource(R.string.page_indicator, pagerState.currentPage + 1, pagerState.pageCount), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }, enabled = pagerState.currentPage < pagerState.pageCount - 1) {
                    Icon(painterResource(R.drawable.chevron_right_px), "Next")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopArtistsSection(
    artists: List<SuggestionArtist>,
    onArtistClick: (SuggestionArtist) -> Unit
) {
    if (artists.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Trending Artists",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            items(artists) { artist ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp).clickable { onArtistClick(artist) }) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        SubcomposeAsyncImage(
                            model = artist.thumbnailUrl,
                            contentDescription = artist.name,
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    LoadingIndicator()
                                }
                            },
                            modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Surface(modifier = Modifier.size(28.dp).offset((-4).dp, (-4).dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary, tonalElevation = 4.dp) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(artist.rank.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(artist.name, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                    val playCount = remember(artist.rank) { 
                        val base = 15_000_000 / (artist.rank + 8)
                        if (base >= 1_000_000) String.format("%.1fM plays", base / 1_000_000f)
                        else String.format("%dk plays", base / 1_000)
                    }
                    Text(playCount, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun TrendingAlbumsSection(
    albums: List<SuggestionAlbum>,
    onAlbumClick: (SuggestionAlbum) -> Unit,
    onMoreClick: () -> Unit
) {
    if (albums.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Trending Albums",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)
        ) {
            items(albums) { album ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp).clickable { onAlbumClick(album) }) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        SubcomposeAsyncImage(
                            model = album.thumbnailUrl,
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    LoadingIndicator()
                                }
                            },
                            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Surface(modifier = Modifier.size(28.dp).offset((-4).dp, (-4).dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary, tonalElevation = 4.dp) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(album.rank.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(album.title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                    Text(album.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                }
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(100.dp)
                        .padding(bottom = 20.dp)
                        .clickable { onMoreClick() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.globe_search),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "More",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingVideosSection(
    videos: List<SuggestionTrack>,
    onVideoClick: (SuggestionTrack) -> Unit,
    onMoreClick: () -> Unit
) {
    if (videos.isEmpty()) return
    
    val carouselState = rememberCarouselState(itemCount = { videos.size })
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trending Music Videos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "More",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onMoreClick() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 320.dp,
            itemSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { i ->
            val video = videos[i]
            var isCardFocused by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .onGloballyPositioned { coordinates ->
                        val cardCenter = coordinates.boundsInRoot().center.x
                        val screenWidth = context.resources.displayMetrics.widthPixels
                        val screenCenter = screenWidth / 2f
                        isCardFocused = abs(cardCenter - screenCenter) < 150
                    }
                    .clickable { onVideoClick(video) }
            ) {
                // Background Image (Thumbnail)
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )


                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 300f
                            )
                        )
                )

                // Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = video.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
