package com.music.vivi.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.StatPeriod
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.ChoiceChipsRow
import com.music.vivi.ui.component.HideOnScrollFAB
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalAlbumsGrid
import com.music.vivi.ui.component.LocalArtistsGrid
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.LocalSongsGrid
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.menu.AlbumMenu
import com.music.vivi.ui.menu.ArtistMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.joinByBullet
import com.music.vivi.utils.makeTimeString
import com.music.vivi.viewmodels.StatsViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val context = LocalContext.current

    val indexChips by viewModel.indexChips.collectAsState()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val mostPlayedSongsStats by viewModel.mostPlayedSongsStats.collectAsState()
    val mostPlayedArtists by viewModel.mostPlayedArtists.collectAsState()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsState()
    val firstEvent by viewModel.firstEvent.collectAsState()
    val currentDate = LocalDateTime.now()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val selectedOption by viewModel.selectedOption.collectAsState()

    val weeklyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(currentDate) { it.minusWeeks(1) }
                .takeWhile { it.isAfter(firstEvent?.event?.timestamp?.minusWeeks(1)) }
                .mapIndexed { index, date ->
                    val endDate = date.plusWeeks(1).minusDays(1).coerceAtMost(currentDate)
                    val formatter = DateTimeFormatter.ofPattern("dd MMM")

                    val startDateFormatted = formatter.format(date)
                    val endDateFormatted = formatter.format(endDate)

                    val startMonth = date.month
                    val endMonth = endDate.month
                    val startYear = date.year
                    val endYear = endDate.year

                    val text =
                        when {
                            startYear != currentDate.year -> "$startDateFormatted, $startYear - $endDateFormatted, $endYear"
                            startMonth != endMonth -> "$startDateFormatted - $endDateFormatted"
                            else -> "${date.dayOfMonth} - $endDateFormatted"
                        }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }

    val monthlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(
                currentDate.plusMonths(1).withDayOfMonth(1).minusDays(1)
            ) { it.minusMonths(1) }
                .takeWhile {
                    it.isAfter(
                        firstEvent
                            ?.event
                            ?.timestamp
                            ?.withDayOfMonth(1),
                    )
                }.mapIndexed { index, date ->
                    val formatter = DateTimeFormatter.ofPattern("MMM")
                    val formattedDate = formatter.format(date)
                    val text =
                        if (date.year != currentDate.year) {
                            "$formattedDate ${date.year}"
                        } else {
                            formattedDate
                        }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }

    val yearlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(
                currentDate
                    .plusYears(1)
                    .withDayOfYear(1)
                    .minusDays(1),
            ) { it.minusYears(1) }
                .takeWhile {
                    it.isAfter(
                        firstEvent
                            ?.event
                            ?.timestamp,
                    )
                }.mapIndexed { index, date ->
                    Pair(index, "${date.year}")
                }.toList()
        } else {
            emptyList()
        }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues(),
            modifier = Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        ) {
            item {
                ChoiceChipsRow(
                    chips =
                    when (selectedOption) {
                        OptionStats.WEEKS -> weeklyDates
                        OptionStats.MONTHS -> monthlyDates
                        OptionStats.YEARS -> yearlyDates
                        OptionStats.CONTINUOUS -> {
                            listOf(
                                StatPeriod.WEEK_1.ordinal to pluralStringResource(
                                    R.plurals.n_week,
                                    1,
                                    1
                                ),
                                StatPeriod.MONTH_1.ordinal to pluralStringResource(
                                    R.plurals.n_month,
                                    1,
                                    1
                                ),
                                StatPeriod.MONTH_3.ordinal to pluralStringResource(
                                    R.plurals.n_month,
                                    3,
                                    3
                                ),
                                StatPeriod.MONTH_6.ordinal to pluralStringResource(
                                    R.plurals.n_month,
                                    6,
                                    6
                                ),
                                StatPeriod.YEAR_1.ordinal to pluralStringResource(
                                    R.plurals.n_year,
                                    1,
                                    1
                                ),
                                StatPeriod.ALL.ordinal to stringResource(R.string.filter_all),
                            )
                        }
                    },
                    options =
                    listOf(
                        OptionStats.CONTINUOUS to stringResource(id = R.string.continuous),
                        OptionStats.WEEKS to stringResource(R.string.weeks),
                        OptionStats.MONTHS to stringResource(R.string.months),
                        OptionStats.YEARS to stringResource(R.string.years),
                    ),
                    selectedOption = selectedOption,
                    onSelectionChange = {
                        viewModel.selectedOption.value = it
                        viewModel.indexChips.value = 0
                    },
                    currentValue = indexChips,
                    onValueUpdate = { viewModel.indexChips.value = it },
                )
            }

            item(key = "mostPlayedSongs") {
                NavigationTitle(
                    title = "${mostPlayedSongsStats.size} ${stringResource(id = R.string.songs)}",
                    modifier = Modifier.animateItem(),
                )

                LazyRow(
                    modifier = Modifier.animateItem(),
                ) {
                    itemsIndexed(
                        items = mostPlayedSongsStats,
                        key = { _, song -> song.id },
                    ) { index, song ->
                        LocalSongsGrid(
                            title = "${index + 1}. ${song.title}",
                            subtitle =
                            joinByBullet(
                                pluralStringResource(
                                    R.plurals.n_time,
                                    song.songCountListened,
                                    song.songCountListened,
                                ),
                                makeTimeString(song.timeListened),
                            ),
                            thumbnailUrl = song.thumbnailUrl,
                            isActive = song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    endpoint = WatchEndpoint(song.id),
                                                    preloadItem = mostPlayedSongs[index].toMediaMetadata(),
                                                ),
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            SongMenu(
                                                originalSong = mostPlayedSongs[index],
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                )
                                .animateItem(),
                        )
                    }
                }
            }

            item(key = "mostPlayedArtists") {
                NavigationTitle(
                    title = "${mostPlayedArtists.size} ${stringResource(id = R.string.artists)}",
                    modifier = Modifier.animateItem(),
                )

                LazyRow(
                    modifier = Modifier.animateItem(),
                ) {
                    itemsIndexed(
                        items = mostPlayedArtists,
                        key = { _, artist -> artist.id },
                    ) { index, artist ->
                        LocalArtistsGrid(
                            title = "${index + 1}. ${artist.artist.name}",
                            subtitle =
                            joinByBullet(
                                pluralStringResource(
                                    R.plurals.n_time,
                                    artist.songCount,
                                    artist.songCount
                                ),
                                makeTimeString(artist.timeListened?.toLong()),
                            ),
                            thumbnailUrl = artist.artist.thumbnailUrl,
                            modifier =
                            Modifier
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("artist/${artist.id}")
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            ArtistMenu(
                                                originalArtist = artist,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                )
                                .animateItem(),
                        )
                    }
                }
            }

            item(key = "mostPlayedAlbums") {
                NavigationTitle(
                    title = "${mostPlayedAlbums.size} ${stringResource(id = R.string.albums)}",
                    modifier = Modifier.animateItem(),
                )

                if (mostPlayedAlbums.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.animateItem(),
                    ) {
                        itemsIndexed(
                            items = mostPlayedAlbums,
                            key = { _, album -> album.id },
                        ) { index, album ->
                            LocalAlbumsGrid(
                                title = "${index + 1}. ${album.album.title}",
                                subtitle =
                                joinByBullet(
                                    pluralStringResource(
                                        R.plurals.n_time,
                                        album.songCountListened!!,
                                        album.songCountListened
                                    ),
                                    makeTimeString(album.timeListened?.toLong()),
                                ),
                                thumbnailUrl = album.album.thumbnailUrl,
                                isActive = album.id == mediaMetadata?.album?.id,
                                isPlaying = isPlaying,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("album/${album.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                AlbumMenu(
                                                    originalAlbum = album,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }
                }
            }
        }

        // FAB to shuffle most played songs
        if (mostPlayedSongs.isNotEmpty()) {
            HideOnScrollFAB(
                visible = true,
                lazyListState = lazyListState,
                icon = R.drawable.shuffle,
                onClick = {
                    playerConnection.playQueue(
                        ListQueue(
                            title = context.getString(R.string.most_played_songs),
                            items = mostPlayedSongs.map { it.toMediaMetadata().toMediaItem() }.shuffled()
                        )
                    )
                }
            )
        }

        TopAppBar(
            title = { Text(stringResource(R.string.stats)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
        )
    }
}

enum class OptionStats { WEEKS, MONTHS, YEARS, CONTINUOUS }
