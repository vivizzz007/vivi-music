package com.music.vivi.ui.screens.playlist

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastSumBy
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.*
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.ui.component.AutoResizeText
import com.music.vivi.ui.component.EmptyPlaceholder
import com.music.vivi.ui.component.FontSizeRange
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.utils.getDirectorytree
import com.music.vivi.ui.component.SongFolderItem
import com.music.vivi.ui.menu.LocalSongSelectionMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.scanLocal
import com.music.vivi.ui.utils.syncDB
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.LibrarySongsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Stack

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPlaylistLocalScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: LibrarySongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val folderStack = remember { viewModel.folderPositionStack }
    val (flatSubfolders) = rememberPreference(FlatSubfoldersKey, defaultValue = true)
    val coroutineScope = rememberCoroutineScope()
    var inLocal by viewModel.inLocal
    var isScannerActive by remember { mutableStateOf(false) }
    var isScanFinished by remember { mutableStateOf(false) }
    var mediaPermission by remember { mutableStateOf(true) }
    val mediaPermissionLevel =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    val (autoSyncLocalSongs) = rememberPreference(
        key = AutoSyncLocalSongsKey,
        defaultValue = true
    )
    val (scannerSensitivity) = rememberEnumPreference(
        key = ScannerSensitivityKey,
        defaultValue = ScannerSensitivity.LEVEL_2
    )
    val (strictExtensions) = rememberPreference(ScannerStrictExtKey, defaultValue = false)

    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()

    flatSubfolders.let {
        viewModel.folderPositionStack = Stack()
    }

    if (folderStack.isEmpty()) {
        val cachedTree = getDirectorytree()
        if (cachedTree == null) {
            viewModel.getLocalSongs(context, viewModel.databaseLink)
        }
        folderStack.push(
            if (flatSubfolders) viewModel.localSongDirectoryTree.value.toFlattenedTree()
            else viewModel.localSongDirectoryTree.value
        )
    }
    var currDir by remember {
        mutableStateOf(folderStack.peek())
    }

    val songs = currDir.files
    val likeLength = remember(songs) {
        songs.fastSumBy { it.song.duration }
    }
    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }


    LaunchedEffect(inSelectMode) {
        backStackEntry?.savedStateHandle?.set("inSelectMode", inSelectMode)
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }

    val searchQueryStr = searchQuery.text.trim()
    val filteredItems = remember(currDir, searchQueryStr) {
        if (searchQueryStr.isEmpty()) currDir else currDir.copy(
            subdirs = currDir.subdirs.filter { dir ->
                dir.currentDir.contains(searchQueryStr, ignoreCase = true)
            },
            files = currDir.files.filter { song ->
                song.title.contains(searchQueryStr, ignoreCase = true) ||
                        song.artists.joinToString("").contains(searchQueryStr, ignoreCase = true)
            }
        )
    }
    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    if (inLocal) {
        BackHandler {
            if (folderStack.size > 1) {
                folderStack.pop()
                currDir = folderStack.peek()
            } else inLocal = false
        }
    }

    if (autoSyncLocalSongs) {
        LaunchedEffect(Unit) {
            if (isScannerActive) {
                return@LaunchedEffect
            }
            if (context.checkSelfPermission(mediaPermissionLevel)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    context as Activity,
                    arrayOf(mediaPermissionLevel), PackageManager.PERMISSION_GRANTED
                )
                mediaPermission = false
                return@LaunchedEffect
            } else if (context.checkSelfPermission(mediaPermissionLevel)
                == PackageManager.PERMISSION_GRANTED
            ) {
                mediaPermission = true
            }
            isScanFinished = false
            isScannerActive = true
            coroutineScope.launch(Dispatchers.IO) {
                val directoryStructure = scanLocal(context).value
                syncDB(database, directoryStructure.toList(), scannerSensitivity, strictExtensions)
                isScannerActive = false
                isScanFinished = true
            }
        }
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            if (filteredItems.toList().isEmpty() && !isSearching) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.music_note,
                        text = stringResource(R.string.playlist_is_empty)
                    )
                }
            }
            if (filteredItems.toList().isEmpty() && isSearching) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search_icon,
                        text = stringResource(R.string.no_results_found)
                    )
                }
            }
            if (filteredItems.toList().isNotEmpty() && !isSearching) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        val libcarditem = 25.dp
                        Box(
                            modifier = Modifier
                                .size(AlbumThumbnailSize)
                                .clip(RoundedCornerShape(libcarditem))
                                .align(alignment = Alignment.CenterHorizontally)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(ThumbnailCornerRadius)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = LocalContentColor.current.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                    .align(Alignment.Center)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AutoResizeText(
                                text = stringResource(R.string.local),
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSizeRange = FontSizeRange(16.sp, 22.sp)
                            )
                            Text(
                                text = makeTimeString(likeLength * 1000L),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Normal
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        if (isScannerActive) {
                                            return@Button
                                        }
                                        if (context.checkSelfPermission(mediaPermissionLevel)
                                            != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            requestPermissions(
                                                context as Activity,
                                                arrayOf(mediaPermissionLevel),
                                                PackageManager.PERMISSION_GRANTED
                                            )
                                            mediaPermission = false
                                            return@Button
                                        } else if (context.checkSelfPermission(mediaPermissionLevel)
                                            == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            mediaPermission = true
                                        }
                                        isScanFinished = false
                                        isScannerActive = true
                                        val text = context.getString(R.string.sync_local_songs_toast)
                                        Toast.makeText(
                                            context,
                                            text,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val directoryStructure =
                                                scanLocal(context).value
                                            syncDB(
                                                database,
                                                directoryStructure.toList(),
                                                scannerSensitivity,
                                                strictExtensions
                                            )
                                            isScannerActive = false
                                            isScanFinished = true
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.sync),
                                        contentDescription = null
                                    )
                                }
                                Button(
                                    onClick = {
                                        playerConnection.addToQueue(
                                            items = currDir.toList().map { it.toMediaItem() },
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.queue_music),
                                        contentDescription = null
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = context.getString(R.string.local),
                                                items = currDir
                                                    .toList()
                                                    .map { it.toMediaItem() })
                                        )
                                    },
                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.play),
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.play))
                                }
                                Button(
                                    onClick = {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = context.getString(R.string.local),
                                                items = currDir.toList().shuffled()
                                                    .map { it.toMediaItem() }
                                            )
                                        )
                                    },
                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.shuffle))
                                }
                            }
                        }
                    }
                }
            }
            itemsIndexed(
                items = filteredItems.subdirs,
                key = { _, item -> item.uid },
                contentType = { _, _ -> CONTENT_TYPE_SONG }
            ) { _, folder ->
                SongFolderItem(
                    folder = folder,
                    subtitle = "${folder.toList().size} Song${if (folder.toList().size > 1) "" else "s"}",
                    modifier = Modifier
                        .combinedClickable {
                            currDir = folderStack.push(folder)
                        }
                        .animateItem(),
                    menuState = menuState,
                    navController = navController
                )
            }
            if (currDir.subdirs.size > 0 && currDir.files.size > 0) {
                item(
                    key = "folder_songs_divider",
                ) {
                    HorizontalDivider(
                        thickness = DividerDefaults.Thickness,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
            itemsIndexed(
                items = filteredItems.files,
                key = { _, item -> item.id },
                contentType = { _, _ -> CONTENT_TYPE_SONG }
            ) { index, song ->
                val onCheckedChange: (Boolean) -> Unit = {
                    if (it) {
                        selection.add(song.id)
                    } else {
                        selection.remove(song.id)
                    }
                }
                if (index == 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.n_song,
                                R.plurals.n_song, filteredItems.toList().size, filteredItems.toList().size
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
                SongListItem(
                    song = song,
                    isActive = song.id == mediaMetadata?.id,
                    showLikedIcon = true,
                    showInLibraryIcon = true,
                    isPlaying = isPlaying,
                    trailingContent = {
                        if (inSelectMode) {
                            Checkbox(
                                checked = selection.contains(song.id),
                                onCheckedChange = { checked ->
                                    if (checked) selection.add(song.id)
                                    else selection.remove(song.id)
                                }
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    painterResource(R.drawable.more_vert),
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (inSelectMode) {
                                    onCheckedChange(song.id !in selection)
                                } else {
                                    if (song.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = context.getString(R.string.local),
                                                items = songs.map { it.toMediaItem() },
                                                startIndex = songs.indexOfFirst { it.song.id == song.id }
                                            )
                                        )
                                    }
                                }
                            },
                            onLongClick = {
                                if (!inSelectMode) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    inSelectMode = true
                                    onCheckedChange(true)
                                }
                            }
                        )
                        .animateItem()
                )
            }
        }
        if (inSelectMode) {
            TopAppBar(
                title = {
                    Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
                },
                navigationIcon = {
                    IconButton(onClick = onExitSelectionMode) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    Checkbox(
                        checked = selection.size == filteredItems.files.size && selection.isNotEmpty(),
                        onCheckedChange = {
                            if (selection.size == filteredItems.files.size) {
                                selection.clear()
                            } else {
                                selection.clear()
                                selection.addAll(filteredItems.files.map { it.id })
                            }
                        }
                    )
                    IconButton(
                        enabled = selection.isNotEmpty(),
                        onClick = {
                            menuState.show {
                                LocalSongSelectionMenu(
                                    selection = selection.mapNotNull { songId ->
                                        songs.find { it.id == songId }
                                    },
                                    onDismiss = menuState::dismiss,
                                    onExitSelectionMode = onExitSelectionMode
                                )
                            }
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                }
            )
        } else {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            trailingIcon = {
                                if (searchQuery.text.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = TextFieldValue("") }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.close),
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        )
                    } else {
                        Text(if (viewModel.folderPositionStack.size > 1) currDir.currentDir else "Internal Storage")
                    }
                },
                navigationIcon = {
                    com.music.vivi.ui.component.IconButton(
                        onClick = {
                            if (isSearching) {
                                isSearching = false
                                searchQuery = TextFieldValue()
                            } else {
                                navController.navigateUp()
                            }
                        },
                        onLongClick = {
                            if (!isSearching) {
                                navController.backToMain()
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                R.drawable.arrow_back
                            ),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    if (!isSearching && !inSelectMode) {
                        IconButton(
                            onClick = { isSearching = true }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.search_icon),
                                contentDescription = null
                            )
                        }
                    }
                },
                scrollBehavior = if (!isSearching) scrollBehavior else null
            )
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}