/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.ui.component.LocalAlbumsGrid
import com.music.vivi.ui.component.LocalArtistsGrid
import com.music.vivi.ui.component.LocalSongsGrid
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.IconButton
import com.music.vivi.utils.joinByBullet
import com.music.vivi.utils.makeTimeString
import com.music.vivi.viewmodels.DetailedListeningHistoryViewModel
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedListeningHistoryScreen(
    navController: NavController,
    viewModel: DetailedListeningHistoryViewModel = hiltViewModel()
) {
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val mostPlayedArtists by viewModel.mostPlayedArtists.collectAsState()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsState()
    val totalPlayTime by viewModel.totalPlayTime.collectAsState()
    
    val uniqueSongsCount by viewModel.uniqueSongsCount.collectAsState()
    val uniqueArtistsCount by viewModel.uniqueArtistsCount.collectAsState()
    val uniqueAlbumsCount by viewModel.uniqueAlbumsCount.collectAsState()

    val dateStr = Instant.ofEpochMilli(viewModel.startTimestamp)
        .atZone(ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = dateStr) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize()
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Format precise time mapping
                        val preciseSec = (totalPlayTime / 1000) % 60
                        val preciseMin = (totalPlayTime / (1000 * 60)) % 60
                        val preciseHr = (totalPlayTime / (1000 * 60 * 60))
                        val detailedTimeStr = buildString {
                            if (preciseHr > 0) append("${preciseHr}h ")
                            if (preciseMin > 0 || preciseHr > 0) append("${preciseMin}m ")
                            append("${preciseSec}s")
                        }.trim()

                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total listening time",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = detailedTimeStr,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                Text(text = "$uniqueSongsCount", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text(text = "Songs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                Text(text = "$uniqueArtistsCount", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text(text = "Artists", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                Text(text = "$uniqueAlbumsCount", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text(text = "Albums", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Top Artist Highlight
                        val topArtist = mostPlayedArtists.firstOrNull()
                        if (topArtist != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                   .background(
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Top Artist:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = topArtist.artist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (mostPlayedSongs.isNotEmpty()) {
                item {
                    NavigationTitle(title = "${mostPlayedSongs.size} Songs")
                    LazyRow {
                        itemsIndexed(mostPlayedSongs, key = { _, song -> song.id }) { index, song ->
                            LocalSongsGrid(
                                title = "${index + 1}. ${song.title}",
                                subtitle = joinByBullet(
                                    "${song.songCountListened} plays",
                                    makeTimeString(song.timeListened ?: 0L)
                                ),
                                thumbnailUrl = song.thumbnailUrl,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            if (mostPlayedArtists.isNotEmpty()) {
                item {
                    NavigationTitle(title = "${mostPlayedArtists.size} Artists")
                    LazyRow {
                        itemsIndexed(mostPlayedArtists, key = { _, artist -> artist.id }) { index, artist ->
                            LocalArtistsGrid(
                                title = "${index + 1}. ${artist.artist.name}",
                                subtitle = joinByBullet(
                                    "${artist.songCount} plays",
                                    makeTimeString(artist.timeListened?.toLong() ?: 0L)
                                ),
                                thumbnailUrl = artist.artist.thumbnailUrl
                            )
                        }
                    }
                }
            }

            if (mostPlayedAlbums.isNotEmpty()) {
                item {
                    NavigationTitle(title = "${mostPlayedAlbums.size} Albums")
                    LazyRow {
                        itemsIndexed(mostPlayedAlbums, key = { _, album -> album.id }) { index, album ->
                            LocalAlbumsGrid(
                                title = "${index + 1}. ${album.album.title}",
                                subtitle = joinByBullet(
                                    "${album.songCountListened} plays",
                                    makeTimeString(album.timeListened ?: 0L)
                                ),
                                thumbnailUrl = album.album.thumbnailUrl,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
