package com.music.vivi.update.settingstyle



import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.DefaultOpenTabKey
import com.music.vivi.ui.screens.settings.NavigationTab
import com.music.vivi.utils.rememberEnumPreference
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.TextButton
import com.music.vivi.constants.ChipSortTypeKey
import com.music.vivi.constants.LibraryFilter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryChipScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(
        key = ChipSortTypeKey,
        defaultValue = LibraryFilter.LIBRARY
    )

    val scrollState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
//                        Text(
//                            "Default Library Section",
//                            style = MaterialTheme.typography.headlineSmall,
//                            fontWeight = FontWeight.SemiBold
//                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = navController::navigateUp) {
                            Icon(
                                painterResource(R.drawable.arrow_back),
                                contentDescription = null
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 32.dp
                )
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Choose Default Library Section",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select which section opens first in your Library tab",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            val libraryOptions = listOf(
                                LibraryFilter.LIBRARY,
                                LibraryFilter.PLAYLISTS,
                                LibraryFilter.SONGS,
                                LibraryFilter.ALBUMS,
                                LibraryFilter.ARTISTS
                            )

                            libraryOptions.forEachIndexed { index, chip ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDefaultChipChange(chip)
                                        }
                                        .padding(vertical = 16.dp, horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = chip == defaultChip,
                                        onClick = {
                                            onDefaultChipChange(chip)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = when (chip) {
                                                LibraryFilter.SONGS -> "Songs"
                                                LibraryFilter.ARTISTS -> "Artists"
                                                LibraryFilter.ALBUMS -> "Albums"
                                                LibraryFilter.PLAYLISTS -> "Playlists"
                                                LibraryFilter.LIBRARY -> "Library Overview"
                                            },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = when (chip) {
                                                LibraryFilter.SONGS -> "Browse all your individual songs"
                                                LibraryFilter.ARTISTS -> "View your collection by artist"
                                                LibraryFilter.ALBUMS -> "Explore your music by albums"
                                                LibraryFilter.PLAYLISTS -> "Access your created and saved playlists"
                                                LibraryFilter.LIBRARY -> "General overview of your music library"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    // Icon for each option
                                    Icon(
                                        painter = when (chip) {
                                            LibraryFilter.SONGS -> painterResource(R.drawable.music_note)
                                            LibraryFilter.ARTISTS -> painterResource(R.drawable.artist)
                                            LibraryFilter.ALBUMS -> painterResource(R.drawable.album)
                                            LibraryFilter.PLAYLISTS -> painterResource(R.drawable.playlist_play)
                                            LibraryFilter.LIBRARY -> painterResource(R.drawable.library_music)
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (index != libraryOptions.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Current Selection",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (defaultChip) {
                                    LibraryFilter.SONGS -> "Songs - Browse all your individual songs"
                                    LibraryFilter.ARTISTS -> "Artists - View your collection by artist"
                                    LibraryFilter.ALBUMS -> "Albums - Explore your music by albums"
                                    LibraryFilter.PLAYLISTS -> "Playlists - Access your created and saved playlists"
                                    LibraryFilter.LIBRARY -> "Library Overview - General overview of your music library"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}