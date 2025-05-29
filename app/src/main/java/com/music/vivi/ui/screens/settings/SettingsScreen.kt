package com.music.vivi.ui.screens.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.music.vivi.BuildConfig
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.PreferenceEntry
import com.music.vivi.ui.utils.backToMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.*


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
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

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        userImageUri = uri
        saveImageUri(context, uri)
    }

    val greeting = getGreetingBasedOnTime()

    // Fetch all releases and check for update
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
        val keywords: List<String> = emptyList()
    )

    val settingsItems = listOf(
<<<<<<< HEAD
//        SettingItem(
//            title = stringResource(R.string.update),
//            iconRes = if (isUpdateAvailable) R.drawable.updateon_icon else R.drawable.update_icon,
//            route = "settings/update",
//            description = "Check for app updates",
////            and latest versions
//            keywords = listOf("update", "version", "upgrade", "new")
//        )

//        SettingItem(
//            title = stringResource(R.string.update),
//            iconRes = if (isUpdateAvailable) R.drawable.updateon_icon else R.drawable.update_icon,
//            route = "settings/update",
//            // Dynamically set the description based on isUpdateAvailable
//            description = if (isUpdateAvailable) "Update available" else "Check for app updates",
//            keywords = listOf("update", "version", "upgrade", "new","feedback","beta","auto check")
//        ) ,
=======
        SettingItem(
            title = stringResource(R.string.update),
            iconRes = if (isUpdateAvailable) R.drawable.updateon_icon else R.drawable.update_icon,
            route = "settings/update",
            description = if (isUpdateAvailable) "Update available" else "Check for app updates",
            keywords = listOf("update", "version", "upgrade", "new")
        ),
>>>>>>> 426be3ed (updated code to 2.0.5)
        SettingItem(
            title = stringResource(R.string.appearance),
            iconRes = R.drawable.theme_icon,
            route = "settings/appearance",
            description = "Customize theme and colors",
            keywords = listOf("theme", "color", "dark mode", "light mode","home screen","app design","slider style","player","misc")
        ),
        SettingItem(
            title = stringResource(R.string.account),
            iconRes = R.drawable.account_icon,
            route = "settings/account",
            description = "Manage your account settings",
            keywords = listOf("account", "spotify", "discord")
        ),
        SettingItem(
            title = stringResource(R.string.content),
            iconRes = R.drawable.content_icon,
            route = "settings/content",
            description = "Content preferences and settings",
            keywords = listOf("content", "hide explicit","notification setting","app language","enable proxy", "files")
        ),
        SettingItem(
            title = stringResource(R.string.player_and_audio),
            iconRes = R.drawable.play_icon,
            route = "settings/player",
            description = "Player and audio settings",
            keywords = listOf("player", "audio","auto", "sound", "music","lyrics","local player setings","skip silence","enable offload","queue","playback")
        ),
        SettingItem(
            title = stringResource(R.string.storage),
            iconRes = R.drawable.storage_icon,
            route = "settings/storage",
            description = "Manage storage and cache",
            keywords = listOf("storage", "cache", "memory")
        ),
        SettingItem(
            title = stringResource(R.string.privacy),
            iconRes = R.drawable.security_icon,
            route = "settings/privacy",
            description = "Privacy and security settings",
            keywords = listOf("privacy", "security", "permissions","listen history","misc","screenshot","history")
        ),
        SettingItem(
            title = stringResource(R.string.backup_restore),
            iconRes = R.drawable.backups_icon,
            route = "settings/backup_restore",
            description = "Backup and restore your data",
            keywords = listOf("backup", "restore", "data")
        ),
        SettingItem(
            title = stringResource(R.string.about),
            iconRes = R.drawable.info_icon,
            route = "settings/about",
            description = "About this app and version info",
            keywords = listOf("about","developer", "version", "details","changelog","website","github","donate")
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
                                    painterResource(R.drawable.back_icon),
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
                                                "Search in vivi settings...",
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
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Spacer(Modifier.height(160.dp))
                                            val compositionNoResults by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.searchingerror))
                                            val progressNoResults by animateLottieCompositionAsState(
                                                composition = compositionNoResults,
                                                iterations = LottieConstants.IterateForever
                                            )
                                            LottieAnimation(
                                                composition = compositionNoResults,
                                                progress = { progressNoResults },
                                                modifier = Modifier.size(300.dp)
                                            )
                                            Spacer(Modifier.height(30.dp))
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
                                                Icon(
                                                    painterResource(item.iconRes),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
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
                            val compositionSearchHint by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.smarts))
                            val progressSearchHint by animateLottieCompositionAsState(
                                composition = compositionSearchHint,
                                iterations = LottieConstants.IterateForever
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                LottieAnimation(
                                    composition = compositionSearchHint,
                                    progress = { progressSearchHint },
                                    modifier = Modifier.size(500.dp).height(16.dp)
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

    CompositionLocalProvider(LocalPlayerAwareWindowInsets provides WindowInsets(0,0,0,0)) {
        Column(
            Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

            Spacer(Modifier.height(130.dp))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(120.dp),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") }
                    ) {
                        AsyncImage(
                            model = userImageUri ?: "https://github.com/vivizzz007/vivi-music/blob/6c6de54393bb9d8b991f89f6b00e47a1df029867/assets/img.png?raw=true",
                            contentDescription = "User profile image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        AnimatedVisibility(
                            visible = !showUpdateCard,
                            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                                animationSpec = tween(500), initialOffsetY = { it / 2 }
                            )
                        ) {
                            Text(
                                text = "👋 Welcome to Settings",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 18.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

<<<<<<< HEAD
//                        AnimatedVisibility(
//                            visible = showUpdateCard,
//                            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
//                                animationSpec = tween(500), initialOffsetY = { it / 2 }
//                            )
//                        ) {
//                            Text(
//                                text = "🚀 Update available: v$latestVersion",
//                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 18.sp),
//                                color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                modifier = Modifier
//                                    .padding(top = 4.dp)
//                                    .clickable(enabled = showUpdateCard) {
//                                        if (showUpdateCard) navController.navigate("settings/update")
//                                    }
//                            )
//                        }
=======
                        AnimatedVisibility(
                            visible = showUpdateCard,
                            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                                animationSpec = tween(500), initialOffsetY = { it / 2 }
                            )
                        ) {
                            Text(
                                text = "🚀 Update available: v$latestVersion",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 18.sp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable(enabled = showUpdateCard) {
                                        if (showUpdateCard) navController.navigate("settings/update")
                                    }
                            )
                        }
>>>>>>> 426be3ed (updated code to 2.0.5)
                    }
                }
            }

            Spacer(Modifier.height(30.dp))

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

            // Search Bar
            OutlinedTextField(
                value = "",
                onValueChange = { },
                placeholder = {
                    Text(
                        "Search vivi settings…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.search_icon),
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 20.dp)
                    .clickable { showSearchDialog = true },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                enabled = false,
                readOnly = true
            )

            Spacer(Modifier.height(20.dp))

            // Only the "Update available" text is colored if update is available
            settingsItems.forEach { item ->
                val isUpdateItem = item.title == stringResource(R.string.update)
                PreferenceEntry(
                    title = { Text(item.title) },
                    description = {
                        if (item.description.isNotEmpty()) {
                            if (
                                isUpdateItem &&
                                isUpdateAvailable &&
                                item.description.contains("Update available")
                            ) {
                                // Only color the "Update available" text differently
                                Text(
                                    buildAnnotatedString {
                                        val desc = item.description
                                        val keyword = "Update available"
                                        val idx = desc.indexOf(keyword)
                                        if (idx >= 0) {
                                            append(desc.substring(0, idx))
                                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                                append(keyword)
                                            }
                                            append(desc.substring(idx + keyword.length))
                                        } else {
                                            append(desc)
                                        }
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant // fallback color for other text
                                )
                            } else {
                                Text(item.description)
                            }
                        }
                    },
                    icon = { Icon(painterResource(item.iconRes), null) },
                    onClick = { navController.navigate(item.route) }
                )
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(80.dp))
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.back_icon),
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
fun PreferenceEntry(
    title: @Composable () -> Unit,
    description: @Composable (() -> Unit)? = null,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = title,
        supportingContent = description,
        leadingContent = icon,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun getGreetingBasedOnTime(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> "Good Morning 🌅"
        hour in 12..17 -> "Good Afternoon 🌞"
        hour in 18..20 -> "Good Evening 🌆"
        else -> "Good Night 🌙"
    }
}

// Robust version check
fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
    val latestParts = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
    val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
        val latest = latestParts.getOrElse(i) { 0 }
        val current = currentParts.getOrElse(i) { 0 }
        if (latest > current) return true
        if (latest < current) return false
    }
    return false
}

// GitHub update check for all release tags starting with v
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

// SharedPreferences helpers
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