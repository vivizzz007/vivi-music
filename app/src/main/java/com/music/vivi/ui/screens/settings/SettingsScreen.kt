package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current


            Column(
                Modifier
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .verticalScroll(rememberScrollState())


            )


            {
                Spacer(
                    Modifier.windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(
                            WindowInsetsSides.Top
                        )
                    )
                )
                Box(
                    modifier = Modifier
                )
                Spacer(Modifier.height(20.dp))
                PreferenceEntry(
                    title = { Text(stringResource(R.string.appearance)) },
                    icon = { Icon(painterResource(R.drawable.theme_icon), null) },
                    onClick = { navController.navigate("settings/appearance") }
                )
                Spacer(Modifier.height(20.dp))
                PreferenceEntry(
                    title = { Text(stringResource(R.string.account)) },
                    icon = { Icon(painterResource(R.drawable.account_icon), null) },
                    onClick = { navController.navigate("settings/account") }
                )
                Spacer(Modifier.height(20.dp))

                PreferenceEntry(
                    title = { Text(stringResource(R.string.content)) },
                    icon = { Icon(painterResource(R.drawable.content_icon), null) },
                    onClick = { navController.navigate("settings/content") }
                )
                Spacer(Modifier.height(20.dp))
                PreferenceEntry(
                    title = { Text(stringResource(R.string.player_and_audio)) },
                    icon = { Icon(painterResource(R.drawable.play_icon), null) },
                    onClick = { navController.navigate("settings/player") }
                )
                Spacer(Modifier.height(20.dp))
                PreferenceEntry(
                    title = { Text(stringResource(R.string.storage)) },
                    icon = { Icon(painterResource(R.drawable.storage_icon), null) },
                    onClick = { navController.navigate("settings/storage") }
                )
                Spacer(Modifier.height(20.dp))
                PreferenceEntry(
                    title = { Text(stringResource(R.string.privacy)) },
                    icon = { Icon(painterResource(R.drawable.security_icon), null) },
                    onClick = { navController.navigate("settings/privacy") }
                )
                Spacer(Modifier.height(20.dp))
                PreferenceEntry(
                    title = { Text(stringResource(R.string.backup_restore)) },
                    icon = { Icon(painterResource(R.drawable.backups_icon), null) },
                    onClick = { navController.navigate("settings/backup_restore") }
                )
                Spacer(Modifier.height(20.dp))
                PreferenceEntry(
                    title = { Text(stringResource(R.string.about)) },
                    icon = { Icon(painterResource(R.drawable.info_icon), null) },
                    onClick = { navController.navigate("settings/about") }
                )

                UpdateCard(uriHandler)
                Spacer(Modifier.height(25.dp))
                VersionCard(uriHandler)
                Spacer(Modifier.height(25.dp))
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
fun VersionCard(uriHandler: UriHandler) {
    Spacer(Modifier.height(20.dp))
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(85.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,

            ),
        shape = RoundedCornerShape(38.dp),
        onClick = { uriHandler.openUri("https://drive.google.com/drive/folders/1iY6PIdVYAu6PnUTM2VBDODt6STEvdJMy?usp=sharing") }
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(38.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(5.dp))

            Text(
                text = "FOR UPDATE - CLICK HERE",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),


                )
        }
    }
}
@Composable
fun UpdateCard(uriHandler: UriHandler) {
    var showUpdateCard by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val newVersion = checkForUpdates()
        if (newVersion != null && isNewerVersion(newVersion, BuildConfig.VERSION_NAME)) {
            showUpdateCard = true
            latestVersion = newVersion
        } else {
            showUpdateCard = false
        }
    }

    if (showUpdateCard) {
        Spacer(Modifier.height(85.dp))
        ElevatedCard(

            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(150.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            shape = RoundedCornerShape(38.dp),
            onClick = {
                uriHandler.openUri("https://drive.google.com/drive/folders/1iY6PIdVYAu6PnUTM2VBDODt6STEvdJMy?usp=sharing")
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "${stringResource(R.string.NewVersion)} $latestVersion",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

suspend fun checkForUpdates(): String? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://drive.google.com/drive/folders/1iY6PIdVYAu6PnUTM2VBDODt6STEvdJMy?usp=sharing")
        val connection = url.openConnection()
        connection.connect()
        val json = connection.getInputStream().bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(json)
        return@withContext jsonObject.getString("tag_name")
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
    val remote = remoteVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    val current = currentVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

    for (i in 0 until maxOf(remote.size, current.size)) {
        val r = remote.getOrNull(i) ?: 0
        val c = current.getOrNull(i) ?: 0
        if (r > c) return true
        if (r < c) return false
    }
    return false
}

