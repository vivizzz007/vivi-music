package com.music.vivi.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.music.vivi.BuildConfig
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.Material3SettingsGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.component.ReleaseNotesCard
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.isNewerVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.URL
import java.util.Calendar
import kotlin.io.copyTo
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.music.innertube.utils.parseCookieString
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.HomeViewModel
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    var latestVersion by remember { mutableStateOf("") }
    var isUpdateAvailable by remember { mutableStateOf(false) }
    var showUpdateCard by remember { mutableStateOf(false) }
    var userImageUri by remember { mutableStateOf(loadImageUri(context)) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchDialog by remember { mutableStateOf(false) }

    // Account sync state - check if user is logged in
    val (innerTubeCookie, _) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    // Get account info from HomeViewModel
    val viewModel: HomeViewModel = hiltViewModel()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val accountName by viewModel.accountName.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // Take persistent URI permission to prevent image from disappearing
            try {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                userImageUri = selectedUri
                saveImageUri(context, selectedUri)
            } catch (e: SecurityException) {
                // If persistent permission fails, copy the image to internal storage
                copyImageToInternalStorage(context, selectedUri) { copiedUri ->
                    userImageUri = copiedUri
                    saveImageUri(context, copiedUri)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        checkForUpdateFromGitHubAll { highestVersion ->
            val cleanLatestVersion = highestVersion.removePrefix("v")
            val newVersionAvailable = isNewerVersion(cleanLatestVersion, BuildConfig.VERSION_NAME)
            latestVersion = cleanLatestVersion
            isUpdateAvailable = newVersionAvailable && cleanLatestVersion != BuildConfig.VERSION_NAME
            showUpdateCard = isUpdateAvailable
        }
    }

    data class SettingItem(
        val title: String,
        val iconRes: Int,
        val route: String,
        val description: String = "",
        val keywords: List<String> = emptyList(),
        val iconColor: Color = Color.Red,
        val backgroundColor: Color = Color.Blue.copy(alpha = 0.1f)
    )

// Android 16 style settings
    val settingsItems = listOf(
        SettingItem(
            title = stringResource(R.string.update),
            iconRes = if (isUpdateAvailable) R.drawable.updateon_icon else R.drawable.update_icon,
            route = "settings/update",
            description = if (isUpdateAvailable) "Update available" else "Check for app updates",
            keywords = listOf("update", "version", "upgrade", "new"),
            iconColor = if (isUpdateAvailable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            backgroundColor = if (isUpdateAvailable) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
        ),
        SettingItem(
            title = stringResource(R.string.appearance),
            iconRes = R.drawable.palette,
            route = "settings/appearance",
            description = "Customize theme and colors",
            keywords = listOf("theme", "color", "dark mode", "light mode","home screen","app design","slider style","player","misc"),
            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        ),
        SettingItem(
            title = stringResource(R.string.content),
            iconRes = R.drawable.language,
            route = "settings/content",
            description = "Content preferences and settings",
            keywords = listOf("content", "hide explicit","notification setting","app language","enable proxy", "files"),
            iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        ),
        SettingItem(
            title = stringResource(R.string.play),
            iconRes = R.drawable.play,
            route = "settings/player",
            description = "Player and audio settings",
            keywords = listOf("player", "audio","auto", "sound", "music","lyrics","local player setings","skip silence","enable offload","queue","playback"),
            iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ),
        SettingItem(
            title = stringResource(R.string.storage),
            iconRes = R.drawable.storage,
            route = "settings/storage",
            description = "Manage storage and cache",
            keywords = listOf("storage", "cache", "memory"),
            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        ),
        SettingItem(
            title = stringResource(R.string.account),
            iconRes = R.drawable.google_icon,
            route = "account_settings",
            description = "Add your account",
            keywords = listOf("account","sign in","sign out"),
            iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        ),
        SettingItem(
            title = stringResource(R.string.privacy),
            iconRes = R.drawable.security,
            route = "settings/privacy",
            description = "Privacy and security settings",
            keywords = listOf("privacy", "security", "permissions","misc","screenshot"),
            iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        ),
        SettingItem(
            title = stringResource(R.string.backup_restore),
            iconRes = R.drawable.restore,
            route = "settings/backup_restore",
            description = "Backup and restore your data",
            keywords = listOf("backup", "restore", "data"),
            iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ),
        SettingItem(
            title = stringResource(R.string.about),
            iconRes = R.drawable.info,
            route = "settings/about",
            description = "About this app and version info",
            keywords = listOf("about","developer", "version", "details","changelog","website","github","donate"),
            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        )
    )

    fun matchesQuery(item: SettingItem, query: String): Boolean {
        if (query.isBlank()) return true
        val tokens = query.trim().lowercase().split("\\s+".toRegex())
        return tokens.all { token ->
            item.title.lowercase().contains(token) ||
                    item.description.lowercase().contains(token) ||
                    item.keywords.any { it.lowercase().contains(token) }
        }
    }

    val filteredItems = settingsItems.filter { item ->
        matchesQuery(item, searchQuery)
    }

    @Composable
    fun SearchDialog(
        showDialog: Boolean,
        onDismiss: () -> Unit,
        searchQuery: String,
        onSearchQueryChange: (String) -> Unit,
        filteredItems: List<SettingItem>,
        onItemClick: (String) -> Unit
    ) {
        if (showDialog) {
            val focusRequester = remember { FocusRequester() }

            // Lottie compositions
            val searchAnimationComposition by rememberLottieComposition(
                spec = LottieCompositionSpec.RawRes(R.raw.smarts)
            )
            val noResultsComposition by rememberLottieComposition(
                spec = LottieCompositionSpec.RawRes(R.raw.searchingerror)
            )

            // Animation progress states
            val searchAnimationProgress by animateLottieCompositionAsState(
                composition = searchAnimationComposition,
                iterations = LottieConstants.IterateForever
            )
            val noResultsProgress by animateLottieCompositionAsState(
                composition = noResultsComposition,
                iterations = LottieConstants.IterateForever
            )

            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                BackHandler(enabled = true) {
                    onDismiss()
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    painterResource(R.drawable.arrow_back),
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .focusRequester(focusRequester),
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                "Search settings",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            AnimatedVisibility(
                                visible = searchQuery.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        painterResource(R.drawable.search_off),
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (searchQuery.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                if (filteredItems.isEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Spacer(Modifier.height(80.dp))
                                            LottieAnimation(
                                                composition = noResultsComposition,
                                                progress = { noResultsProgress },
                                                modifier = Modifier.size(200.dp)
                                            )
                                            Spacer(Modifier.height(24.dp))
                                            Text(
                                                "No results found",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "Try a different keyword",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(filteredItems) { item ->
                                        ListItem(
                                            headlineContent = { Text(item.title) },
                                            supportingContent = item.description.takeIf { it.isNotEmpty() }?.let {
                                                { Text(it) }
                                            },
                                            leadingContent = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .background(item.backgroundColor, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painterResource(item.iconRes),
                                                        contentDescription = null,
                                                        tint = item.iconColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            },
                                            modifier = Modifier.clickable {
                                                onItemClick(item.route)
                                                onDismiss()
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                LottieAnimation(
                                    composition = searchAnimationComposition,
                                    progress = { searchAnimationProgress },
                                    modifier = Modifier.size(250.dp)
                                )
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    "Type to search settings",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "e.g., \"theme\" or \"player\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
    ) {
        // Top App Bar
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
            },
            scrollBehavior = scrollBehavior
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp), // TopAppBar padding
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Large Settings title (like in original code)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.settings),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Search bar at top (Android 16 style)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSearchDialog = true },
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(R.drawable.search_icon),
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Search Settings",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // User profile section (Android 16 card style)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("account_settings") },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile image
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        ) {
                            if (isLoggedIn && accountImageUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(accountImageUrl)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .crossfade(true)
                                        .error(R.drawable.account)
                                        .build(),
                                    contentDescription = "User account image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.account),
                                        contentDescription = "Account",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // User info
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isLoggedIn) {
                                    accountName.ifEmpty { "Your Account" }
                                } else {
                                    "Guest User"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isLoggedIn) "Google services and preferences" else "Sign in to sync your data",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Settings items grouped in cards (Android 16 boxed style)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
//                    color = MaterialTheme.colorScheme.surfaceContainer,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 1.dp
                ) {
                    Column {
                        settingsItems.forEachIndexed { index, item ->
                            val isUpdateItem = item.title == stringResource(R.string.update)

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate(item.route) },
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Colored circular icon background (Android 16 style)
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                item.backgroundColor,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painterResource(item.iconRes),
                                            contentDescription = item.title,
                                            modifier = Modifier.size(24.dp),
                                            tint = item.iconColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Text content
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Normal
                                        )
                                        if (item.description.isNotEmpty()) {
                                            Text(
                                                text = if (isUpdateItem && isUpdateAvailable)
                                                    "Update available"
                                                else
                                                    item.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isUpdateItem && isUpdateAvailable)
                                                    item.iconColor
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Add subtle divider between items (except last)
                            if (index < settingsItems.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 80.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Search Dialog
        SearchDialog(
            showDialog = showSearchDialog,
            onDismiss = {
                showSearchDialog = false
                searchQuery = ""
            },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            filteredItems = filteredItems,
            onItemClick = { route ->
                navController.navigate(route)
            }
        )
    }
}

suspend fun checkForUpdateFromGitHubAll(onResult: (String) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases")
            val connection = url.openConnection().apply {
                setRequestProperty("User-Agent", "ViviMusicApp")
            }
            connection.connect()
            val json = connection.getInputStream().bufferedReader().use { it.readText() }
            val releases = JSONArray(json)
            var highestVersion = BuildConfig.VERSION_NAME
            for (i in 0 until releases.length()) {
                val tag = releases.getJSONObject(i).getString("tag_name")
                if (tag.startsWith("v")) {
                    val clean = tag.removePrefix("v")
                    if (isNewerVersion(clean, highestVersion)) {
                        highestVersion = clean
                    }
                }
            }
            onResult("v$highestVersion")
        } catch (e: Exception) {
            e.printStackTrace()
            onResult("v${BuildConfig.VERSION_NAME}")
        }
    }
}

fun saveImageUri(context: Context, uri: Uri?) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString("user_image_uri", uri?.toString())
    editor.apply()
}

fun loadImageUri(context: Context): Uri? {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val uriString = sharedPreferences.getString("user_image_uri", null)
    return uriString?.let { Uri.parse(it) }
}

private fun copyImageToInternalStorage(
    context: Context,
    sourceUri: Uri,
    onComplete: (Uri?) -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(sourceUri)
        if (inputStream != null) {
            val fileName = "profile_image_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)

            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider", // Make sure this matches your manifest
                file
            )
            onComplete(fileUri)
        } else {
            onComplete(null)
        }
        inputStream?.close()
    } catch (e: Exception) {
        e.printStackTrace()
        onComplete(null)
    }
}