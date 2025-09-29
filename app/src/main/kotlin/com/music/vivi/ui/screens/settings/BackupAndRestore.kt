package com.music.vivi.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.db.entities.Song
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.PreferenceEntry
import com.music.vivi.ui.menu.AddToPlaylistDialogOnline
import com.music.vivi.ui.menu.LoadingScreen
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.experiment.SettingsListItem
import com.music.vivi.update.experiment.SettingsSection
import com.music.vivi.viewmodels.BackupRestoreViewModel
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndRestore(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    var importedTitle by remember { mutableStateOf("") }
    val importedSongs = remember { mutableStateListOf<Song>() }
    var showChoosePlaylistDialogOnline by rememberSaveable {
        mutableStateOf(false)
    }

    var isProgressStarted by rememberSaveable {
        mutableStateOf(false)
    }

    var progressPercentage by rememberSaveable {
        mutableIntStateOf(0)
    }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Calculate scroll-based animations
    val titleAlpha by remember {
        derivedStateOf {
            1f - (scrollState.value / 200f).coerceIn(0f, 1f)
        }
    }

    val titleScale by remember {
        derivedStateOf {
            1f - (scrollState.value / 400f).coerceIn(0f, 0.3f)
        }
    }

    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) {
                viewModel.backup(context, uri)
            }
        }
    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.restore(context, uri)
            }
        }
    val importPlaylistFromCsv =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val result = viewModel.importPlaylistFromCsv(context, uri)
            importedSongs.clear()
            importedSongs.addAll(result)

            if (importedSongs.isNotEmpty()) {
                showChoosePlaylistDialogOnline = true
            }
        }
    val importM3uLauncherOnline = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val result = viewModel.loadM3UOnline(context, uri)
        importedSongs.clear()
        importedSongs.addAll(result)

        if (importedSongs.isNotEmpty()) {
            showChoosePlaylistDialogOnline = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                // Header Section with scroll animations
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .graphicsLayer {
                            alpha = titleAlpha
                            scaleX = titleScale
                            scaleY = titleScale
                        }
                ) {
                    Text(
                        text = stringResource(R.string.backup_restore),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            item {
                // Lottie Animation Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.storage))
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                SettingsSection("Backup & Restore") {
                    SettingsListItem(
                        title = stringResource(R.string.action_backup),
                        subtitle = "Export all your data and settings",
                        onClick = {
                            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                            backupLauncher.launch(
                                "${context.getString(R.string.app_name)}_${
                                    LocalDateTime.now().format(formatter)
                                }.backup"
                            )
                        },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.backup),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.action_restore),
                        subtitle = "Import previously saved data",
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/octet-stream"))
                        },
                        isLast = true,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.restore),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection("Import") {
                    SettingsListItem(
                        title = stringResource(R.string.import_online),
                        subtitle = "Import playlists from M3U files",
                        onClick = {
                            importM3uLauncherOnline.launch(arrayOf("audio/*"))
                        },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.import_csv),
                        subtitle = "Import playlists from CSV files",
                        onClick = {
                            importPlaylistFromCsv.launch(arrayOf("text/csv"))
                        },
                        isLast = true,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }

    AddToPlaylistDialogOnline(
        isVisible = showChoosePlaylistDialogOnline,
        allowSyncing = false,
        initialTextFieldValue = importedTitle,
        songs = importedSongs,
        onDismiss = { showChoosePlaylistDialogOnline = false },
        onProgressStart = { newVal -> isProgressStarted = newVal },
        onPercentageChange = { newPercentage -> progressPercentage = newPercentage }
    )

    LaunchedEffect(progressPercentage, isProgressStarted) {
        if (isProgressStarted && progressPercentage == 99) {
            delay(10000)
            if (progressPercentage == 99) {
                isProgressStarted = false
                progressPercentage = 0
            }
        }
    }

    LoadingScreen(
        isVisible = isProgressStarted,
        value = progressPercentage,
    )
}