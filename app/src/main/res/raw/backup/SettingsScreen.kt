package com.music.vivi.ui.screens.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        userImageUri = uri
        saveImageUri(context, uri)
    }

    val greeting = getGreetingBasedOnTime()

    LaunchedEffect(Unit) {
        checkForUpdateFromGitHub { latestRelease ->
            val cleanLatestVersion = latestRelease.removePrefix("v")
            val newVersionAvailable = isNewerVersion(cleanLatestVersion, BuildConfig.VERSION_NAME)
            latestVersion = cleanLatestVersion
            isUpdateAvailable = newVersionAvailable && cleanLatestVersion != BuildConfig.VERSION_NAME
            showUpdateCard = isUpdateAvailable
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        Spacer(Modifier.height(16.dp))
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(120.dp),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            )
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
                        .clickable {
                            imagePickerLauncher.launch("image/*")
                        }
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

                    // Animated Update Text
                    AnimatedVisibility(
                        visible = !showUpdateCard,  // This will ensure the "Welcome" message shows when no update is available
                        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                            animationSpec = tween(500),
                            initialOffsetY = { it / 2 }
                        )
                    ) {
                        Text(
                            text = "👋 Welcome to Settings",  // This message will be shown when no update is available
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 18.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = 4.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = showUpdateCard,  // This will ensure the "Update available" message shows when there is an update
                        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                            animationSpec = tween(500),
                            initialOffsetY = { it / 2 }
                        )
                    ) {
                        Text(
                            text = "🚀 Update available: v$latestVersion",  // This message will be shown when an update is available
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 18.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable(enabled = showUpdateCard) {
                                    if (showUpdateCard) navController.navigate("settings/update")
                                }
                        )
                    }

                }
            }
        }

        Spacer(Modifier.height(20.dp))

        PreferenceEntry(
            title = { Text(stringResource(R.string.update)) },
            icon = {
                if (isUpdateAvailable) {
                    // Show the colored icon for "update available"
                    Icon(
                        painter = painterResource(R.drawable.updateon_icon),
                        contentDescription = null
                    )
                } else {
                    // Show the default icon when no update is available
                    Icon(
                        painter = painterResource(R.drawable.update_icon),
                        contentDescription = null
                    )
                }
            },
            onClick = { navController.navigate("settings/update") }
        )

        Spacer(Modifier.height(10.dp))
        // Now your usual Preference entries
        PreferenceEntry(
            title = { Text(stringResource(R.string.appearance)) },
            icon = { Icon(painterResource(R.drawable.theme_icon), null) },
            onClick = { navController.navigate("settings/appearance") }
        )
        Spacer(Modifier.height(10.dp))
        PreferenceEntry(
            title = { Text(stringResource(R.string.account)) },
            icon = { Icon(painterResource(R.drawable.account_icon), null) },
            onClick = { navController.navigate("settings/account") }
        )
        Spacer(Modifier.height(10.dp))
        PreferenceEntry(
            title = { Text(stringResource(R.string.content)) },
            icon = { Icon(painterResource(R.drawable.content_icon), null) },
            onClick = { navController.navigate("settings/content") }
        )
        Spacer(Modifier.height(10.dp))
        PreferenceEntry(
            title = { Text(stringResource(R.string.player_and_audio)) },
            icon = { Icon(painterResource(R.drawable.play_icon), null) },
            onClick = { navController.navigate("settings/player") }
        )
        Spacer(Modifier.height(10.dp))
        PreferenceEntry(
            title = { Text(stringResource(R.string.storage)) },
            icon = { Icon(painterResource(R.drawable.storage_icon), null) },
            onClick = { navController.navigate("settings/storage") }
        )
        Spacer(Modifier.height(10.dp))
        PreferenceEntry(
            title = { Text(stringResource(R.string.privacy)) },
            icon = { Icon(painterResource(R.drawable.security_icon), null) },
            onClick = { navController.navigate("settings/privacy") }
        )
        Spacer(Modifier.height(10.dp))
        PreferenceEntry(
            title = { Text(stringResource(R.string.backup_restore)) },
            icon = { Icon(painterResource(R.drawable.backups_icon), null) },
            onClick = { navController.navigate("settings/backup_restore") }
        )
        Spacer(Modifier.height(10.dp))
        PreferenceEntry(
            title = { Text(stringResource(R.string.about)) },
            icon = { Icon(painterResource(R.drawable.info_icon), null) },
            onClick = { navController.navigate("settings/about") }
        )
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



// Greeting logic
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

// Version checking
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

// GitHub update check
suspend fun checkForUpdateFromGitHub(onResult: (String) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases/latest")
            val connection = url.openConnection().apply {
                setRequestProperty("User-Agent", "ViviMusicApp")
            }
            connection.connect()

            val json = connection.getInputStream().bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)
            val latestRelease = jsonObject.getString("tag_name").removePrefix("v")
            onResult(latestRelease)
        } catch (e: Exception) {
            e.printStackTrace()
            onResult("")
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
