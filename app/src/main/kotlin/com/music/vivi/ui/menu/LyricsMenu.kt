package com.music.vivi.ui.menu

import android.app.SearchManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.music.vivi.LocalDatabase
import com.music.vivi.R
import com.music.vivi.constants.LyricsLetterByLetterAnimationKey
import com.music.vivi.constants.LyricsWordForWordKey
import com.music.vivi.db.entities.LyricsEntity
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.lyrics.LyricsResult
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.component.TextFieldDialog
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.LyricsMenuViewModel
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape


/**
 * The menu for lyrics options.
 * Allows editing lyrics, searching for lyrics online, syncing, and toggling romanization.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LyricsMenu(
    lyricsProvider: () -> LyricsEntity?,
    songProvider: () -> SongEntity?,
    mediaMetadataProvider: () -> MediaMetadata,
    onDismiss: () -> Unit,
    viewModel: LyricsMenuViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current

    var showEditDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showEditDialog) {
        TextFieldDialog(
            onDismiss = { showEditDialog = false },
            icon = { Icon(painter = painterResource(R.drawable.edit), contentDescription = null) },
            title = { Text(text = mediaMetadataProvider().title) },
            initialTextFieldValue = TextFieldValue(lyricsProvider()?.lyrics.orEmpty()),
            singleLine = false,
            onDone = {
                viewModel.updateLyrics(mediaMetadataProvider().id, it)
            }
        )
    }

    var showSearchDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showSearchResultDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val searchMediaMetadata =
        remember(showSearchDialog) {
            mediaMetadataProvider()
        }
    val (titleField, onTitleFieldChange) =
        rememberSaveable(showSearchDialog, stateSaver = TextFieldValue.Saver) {
            mutableStateOf(
                TextFieldValue(
                    text = mediaMetadataProvider().title
                )
            )
        }
    val (artistField, onArtistFieldChange) =
        rememberSaveable(showSearchDialog, stateSaver = TextFieldValue.Saver) {
            mutableStateOf(
                TextFieldValue(
                    text = mediaMetadataProvider().artists.joinToString { it.name }
                )
            )
        }

    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState(initial = false)

    if (showSearchDialog) {
        DefaultDialog(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            onDismiss = { showSearchDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.search_lyrics)) },
            buttons = {
                TextButton(
                    onClick = { showSearchDialog = false }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }

                Spacer(Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        showSearchDialog = false
                        onDismiss()
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_WEB_SEARCH).apply {
                                    putExtra(
                                        SearchManager.QUERY,
                                        "${artistField.text} ${titleField.text} lyrics"
                                    )
                                }
                            )
                        } catch (_: Exception) {
                        }
                    }
                ) {
                    Text(stringResource(R.string.search_online))
                }

                Spacer(Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        viewModel.search(
                            searchMediaMetadata.id,
                            titleField.text,
                            artistField.text,
                            searchMediaMetadata.duration
                        )
                        showSearchResultDialog = true

                        if (!isNetworkAvailable) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.error_no_internet),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        ) {
            OutlinedTextField(
                value = titleField,
                onValueChange = onTitleFieldChange,
                singleLine = true,
                label = { Text(stringResource(R.string.song_title)) }
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = artistField,
                onValueChange = onArtistFieldChange,
                singleLine = true,
                label = { Text(stringResource(R.string.song_artists)) }
            )
        }
    }

    if (showSearchResultDialog) {
        val results by viewModel.results.collectAsState(initial = emptyList<LyricsResult>())
        val isLoading by viewModel.isLoading.collectAsState(initial = false)

        var expandedItemIndex by rememberSaveable {
            mutableStateOf(-1)
        }

        ListDialog(
            onDismiss = { showSearchResultDialog = false }
        ) {
            itemsIndexed(results) { index, result ->
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            viewModel.cancelSearch()
                            viewModel.updateLyrics(searchMediaMetadata.id, result.lyrics)
                        }
                        .padding(12.dp)
                        .animateContentSize()
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = result.lyrics,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (index == expandedItemIndex) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = result.providerName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1
                            )
                            if (result.lyrics.startsWith("[")) {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier =
                                    Modifier
                                        .padding(start = 4.dp)
                                        .size(18.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            expandedItemIndex = if (expandedItemIndex == index) -1 else index
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (index ==
                                    expandedItemIndex
                                ) {
                                    R.drawable.expand_less
                                } else {
                                    R.drawable.expand_more
                                }
                            ),
                            contentDescription = null
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (!isLoading && results.isEmpty()) {
                item {
                    Text(
                        text = context.getString(R.string.lyrics_not_found),
                        textAlign = TextAlign.Center,
                        modifier =
                        Modifier
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    var showRomanizationDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showRomanization by rememberSaveable { mutableStateOf(false) }
    var isChecked by remember { mutableStateOf(songProvider()?.romanizeLyrics ?: true) }

    LaunchedEffect(songProvider()) {
        isChecked = songProvider()?.romanizeLyrics ?: true
    }

    val cornerRadius = 24.dp
    val topShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = cornerRadius,
        smoothnessAsPercentBR = 0,
        cornerRadiusBR = 0.dp,
        smoothnessAsPercentTL = 60,
        cornerRadiusTL = cornerRadius,
        smoothnessAsPercentBL = 0,
        cornerRadiusBL = 0.dp,
        smoothnessAsPercentTR = 60
    )
    val middleShape = RectangleShape
    val bottomShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = 0.dp,
        smoothnessAsPercentBR = 60,
        cornerRadiusBR = cornerRadius,
        smoothnessAsPercentTL = 0,
        cornerRadiusTL = 0.dp,
        smoothnessAsPercentBL = 60,
        cornerRadiusBL = cornerRadius,
        smoothnessAsPercentTR = 0
    )
    val singleShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = cornerRadius,
        smoothnessAsPercentBR = 60,
        cornerRadiusBR = cornerRadius,
        smoothnessAsPercentTL = 60,
        cornerRadiusTL = cornerRadius,
        smoothnessAsPercentBL = 60,
        cornerRadiusBL = cornerRadius,
        smoothnessAsPercentTR = 60
    )

    val evenCornerRadiusElems = 26.dp
    val playButtonShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = evenCornerRadiusElems,
        smoothnessAsPercentBR = 60,
        cornerRadiusBR = evenCornerRadiusElems,
        smoothnessAsPercentTL = 60,
        cornerRadiusTL = evenCornerRadiusElems,
        smoothnessAsPercentBL = 60,
        cornerRadiusBL = evenCornerRadiusElems,
        smoothnessAsPercentTR = 60
    )

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(modifier = Modifier.height(1.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Edit Button
            MediumExtendedFloatingActionButton(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight(),
                onClick = {
                    showEditDialog = true
                },
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                shape = playButtonShape,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = stringResource(R.string.edit_content_desc_menu)
                    )
                },
                text = {
                    Text(
                        modifier = Modifier.padding(end = 10.dp),
                        text = stringResource(R.string.edit),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            )

            // Refetch Button
            FilledTonalIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = {
                    onDismiss()
                    viewModel.refetchLyrics(mediaMetadataProvider(), lyricsProvider())
                },
                shape = singleShape
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(R.drawable.cached),
                    contentDescription = stringResource(R.string.refetch_content_desc)
                )
            }

            // Search Button
            FilledTonalIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = {
                    showSearchDialog = true
                },
                shape = singleShape
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(R.drawable.search),
                    contentDescription = stringResource(R.string.search_content_desc)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Settings Section
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Romanize Current Track
            val isRomanized = songProvider()?.romanizeLyrics ?: true
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = topShape,
                onClick = {
                    songProvider()?.let { viewModel.toggleRomanization(it) }
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.language_korean_latin),
                    contentDescription = stringResource(R.string.romanize_icon_content_desc)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.romanize_current_track),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (isRomanized) {
                            stringResource(
                                R.string.romanization_enabled
                            )
                        } else {
                            stringResource(R.string.romanization_disabled)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ModernSwitch(
                    checked = isRomanized,
                    onCheckedChange = { enabled ->
                        songProvider()?.let { viewModel.setRomanization(it, enabled) }
                    }
                )
            }

            Spacer(modifier = Modifier.height(1.dp))

            // Apple Music Lyrics
            val (lyricsWordForWord, onLyricsWordForWordChange) = rememberPreference(LyricsWordForWordKey, true)
            val (_, onLetterByLetterAnimationChange) = rememberPreference(LyricsLetterByLetterAnimationKey, false)

            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = middleShape,
                onClick = {
                    val newValue = !lyricsWordForWord
                    onLyricsWordForWordChange(newValue)
                    if (newValue) {
                        onLetterByLetterAnimationChange(false)
                    }
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.lyrics),
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.apple_lyrics),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(R.string.highlight_words_discretely),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ModernSwitch(
                    checked = lyricsWordForWord,
                    onCheckedChange = { enabled ->
                        onLyricsWordForWordChange(enabled)
                        if (enabled) {
                            onLetterByLetterAnimationChange(false)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(1.dp))

            // Swipe to Change Track
            val swipeGestureEnabled by viewModel.swipeGestureEnabled.collectAsState(initial = true)

            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = bottomShape,
                onClick = viewModel::toggleSwipeGesture,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.swipe),
                    contentDescription = stringResource(R.string.swipe_icon_content_desc)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.swipe_to_change_track),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(R.string.swipe_to_change_track_description),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ModernSwitch(
                    checked = swipeGestureEnabled,
                    onCheckedChange = viewModel::setSwipeGesture
                )
            }
        }
    }
}
