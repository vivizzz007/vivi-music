package com.music.vivi.ui.menu


import android.app.SearchManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.SwipeGestureEnabledKey
import com.music.vivi.db.entities.LyricsEntity
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.component.TextFieldDialog
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.utils.dataStore
import com.music.vivi.viewmodels.LyricsMenuViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape


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
    val playerConnection = LocalPlayerConnection.current

    // State management
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showSearchDialog by rememberSaveable { mutableStateOf(false) }
    var showSearchResultDialog by rememberSaveable { mutableStateOf(false) }

    // Design variables
    val evenCornerRadiusElems = 26.dp
    val buttonShape = CircleShape
    val albumArtShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = evenCornerRadiusElems, smoothnessAsPercentBR = 60,
        cornerRadiusBR = evenCornerRadiusElems, smoothnessAsPercentTL = 60,
        cornerRadiusTL = evenCornerRadiusElems, smoothnessAsPercentBL = 60,
        cornerRadiusBL = evenCornerRadiusElems, smoothnessAsPercentTR = 60
    )

    // Network state
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()

    // Search state
    val searchMediaMetadata = remember(showSearchDialog) { mediaMetadataProvider() }
    val (titleField, onTitleFieldChange) = rememberSaveable(showSearchDialog, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = mediaMetadataProvider().title))
    }
    val (artistField, onArtistFieldChange) = rememberSaveable(showSearchDialog, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = mediaMetadataProvider().artists.joinToString { it.name }))
    }

    // Romanization state
    var isRomanizationChecked by remember { mutableStateOf(songProvider()?.romanizeLyrics ?: true) }

    LaunchedEffect(songProvider()) {
        songProvider()?.let { song ->
            isRomanizationChecked = song.romanizeLyrics ?: true
        }
    }

    // Swipe gesture state
    val dataStore = context.dataStore
    val swipeGestureEnabled by dataStore.data
        .map { it[SwipeGestureEnabledKey] ?: true }
        .collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    // FIXED: Proper favorite state management
    val currentSong = songProvider()
    val isFavorite = currentSong?.liked == true

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(modifier = Modifier.height(1.dp))

        // Header Row - Song Art and Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Song artwork
            AsyncImage(
                model = mediaMetadataProvider().thumbnailUrl,
                contentDescription = "Song Art",
                modifier = Modifier
                    .size(80.dp)
                    .clip(albumArtShape),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = mediaMetadataProvider().title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mediaMetadataProvider().artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // FIXED: Favorite Button with proper click handling
            FilledTonalIconButton(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 6.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = {
                    playerConnection?.toggleLike()
                }
            ) {
                Icon(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    painter = painterResource(
                        if (isFavorite)
                            R.drawable.favorite
                        else
                            R.drawable.favorite_border
                    ),
                    contentDescription = if (isFavorite)
                        "Remove from favorites"
                    else
                        "Add to favorites",
                    tint = if (isFavorite)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Edit Lyrics Button
            MediumExtendedFloatingActionButton(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight(),
                onClick = { showEditDialog = true },
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                shape = AbsoluteSmoothCornerShape(
                    cornerRadiusTR = evenCornerRadiusElems, smoothnessAsPercentBR = 60,
                    cornerRadiusBR = evenCornerRadiusElems, smoothnessAsPercentTL = 60,
                    cornerRadiusTL = evenCornerRadiusElems, smoothnessAsPercentBL = 60,
                    cornerRadiusBL = evenCornerRadiusElems, smoothnessAsPercentTR = 60
                ),
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = "Edit lyrics"
                    )
                },
                text = {
                    Text(
                        modifier = Modifier.padding(end = 10.dp),
                        text = "Edit"
                    )
                }
            )

            // Search Lyrics Button
            FilledIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = { showSearchDialog = true },
                shape = buttonShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(R.drawable.search),
                    contentDescription = "Search lyrics"
                )
            }

            // Refresh Lyrics Button
            FilledTonalIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = {
                    onDismiss()
                    viewModel.refetchLyrics(mediaMetadataProvider(), lyricsProvider())
                },
                shape = buttonShape
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(R.drawable.sync),
                    contentDescription = "Refresh lyrics"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Current Lyrics Display
        lyricsProvider()?.lyrics?.takeIf { it.isNotBlank() }?.let { lyrics ->
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                shape = buttonShape,
                onClick = {
                    showEditDialog = true
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = "Current lyrics"
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Current Lyrics",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        lyrics.take(50) + if (lyrics.length > 50) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Settings Section
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Romanize Lyrics Toggle
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = buttonShape,
                onClick = {
                    val newValue = !isRomanizationChecked
                    isRomanizationChecked = newValue
                    songProvider()?.let { song ->
                        database.query {
                            upsert(song.copy(romanizeLyrics = newValue))
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.language_korean_latin),
                    contentDescription = "Romanize lyrics"
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Romanize Lyrics",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Convert to Latin script",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                ModernSwitch(
                    checked = isRomanizationChecked,
                    onCheckedChange = { checked ->
                        isRomanizationChecked = checked
                        songProvider()?.let { song ->
                            database.query {
                                upsert(song.copy(romanizeLyrics = checked))
                            }
                        }
                    }
                )
            }

            // Swipe Gesture Toggle
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = buttonShape,
                onClick = {
                    scope.launch {
                        dataStore.edit { settings ->
                            settings[SwipeGestureEnabledKey] = !swipeGestureEnabled
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.swipe),
                    contentDescription = "Swipe gesture"
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Swipe to Change Track",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Swipe on player for next/previous",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                ModernSwitch(
                    checked = swipeGestureEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            dataStore.edit { settings ->
                                settings[SwipeGestureEnabledKey] = enabled
                            }
                        }
                    }
                )
            }

            // View Full Lyrics (if available)
            lyricsProvider()?.lyrics?.takeIf { it.isNotBlank() }?.let {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = buttonShape,
                    onClick = { showEditDialog = true }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.fullscreen),
                        contentDescription = "View full lyrics"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "View Full Lyrics",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "See complete lyrics text",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Add Lyrics Button (when no lyrics exist)
            lyricsProvider()?.lyrics?.takeIf { it.isBlank() }?.let {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = buttonShape,
                    onClick = { showEditDialog = true }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = "Add lyrics"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Add Lyrics",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Add lyrics for this song",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showEditDialog) {
        TextFieldDialog(
            onDismiss = { showEditDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            title = {
                Text(
                    text = if (lyricsProvider()?.lyrics.isNullOrBlank()) "Add Lyrics" else "Edit Lyrics",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            initialTextFieldValue = TextFieldValue(lyricsProvider()?.lyrics.orEmpty()),
            singleLine = false,
            onDone = {
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadataProvider().id,
                            lyrics = it,
                        ),
                    )
                }
                showEditDialog = false
            },
        )
    }

    if (showSearchDialog) {
        DefaultDialog(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            onDismiss = { showSearchDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            title = {
                Text(
                    stringResource(R.string.search_lyrics),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            buttons = {
                TextButton(onClick = { showSearchDialog = false }) {
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
                                    putExtra(SearchManager.QUERY, "${artistField.text} ${titleField.text} lyrics")
                                },
                            )
                        } catch (_: Exception) {}
                    },
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
                            Toast.makeText(context, context.getString(R.string.error_no_internet), Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            OutlinedTextField(
                value = titleField,
                onValueChange = onTitleFieldChange,
                singleLine = true,
                label = { Text(stringResource(R.string.song_title)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = artistField,
                onValueChange = onArtistFieldChange,
                singleLine = true,
                label = { Text(stringResource(R.string.song_artists)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showSearchResultDialog) {
        val results by viewModel.results.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        var expandedItemIndex by rememberSaveable { mutableStateOf(-1) }

        ListDialog(onDismiss = { showSearchResultDialog = false }) {
            itemsIndexed(results) { index, result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            viewModel.cancelSearch()
                            database.query {
                                upsert(
                                    LyricsEntity(
                                        id = searchMediaMetadata.id,
                                        lyrics = result.lyrics,
                                    ),
                                )
                            }
                        }
                        .padding(12.dp)
                        .animateContentSize(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.lyrics,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (index == expandedItemIndex) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = result.providerName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                            )
                            if (result.lyrics.startsWith("[")) {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(18.dp),
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { expandedItemIndex = if (expandedItemIndex == index) -1 else index },
                    ) {
                        Icon(
                            painter = painterResource(if (index == expandedItemIndex) R.drawable.expand_less else R.drawable.expand_more),
                            contentDescription = null,
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (!isLoading && results.isEmpty()) {
                item {
                    Text(
                        text = context.getString(R.string.lyrics_not_found),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}