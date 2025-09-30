package com.music.vivi.ui.screens.settings


import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.vivi.BuildConfig
import com.music.vivi.R
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.isNewerVersion
import com.music.vivi.update.settingstyle.ModernInfoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scrollState = rememberLazyListState()

    var latestRelease by remember { mutableStateOf<String?>(null) }
    val isUpdateAvailable = remember { mutableStateOf(false) }
    var buildVersionClickCount by remember { mutableStateOf(0) }

    // Parallax effect for header
    val headerOffset by remember {
        derivedStateOf {
            (scrollState.firstVisibleItemScrollOffset * 0.5f).coerceAtMost(300f)
        }
    }

    // Calculate scroll-based animations for title
    val titleAlpha by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex == 0) {
                1f - (scrollState.firstVisibleItemScrollOffset / 200f).coerceIn(0f, 1f)
            } else 0f
        }
    }

    // Get app install date
    val installedDate = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val installTime = packageInfo.firstInstallTime
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(installTime))
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // Update check LaunchedEffect
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val apiUrl = "https://api.github.com/repos/vivizzz007/vivi-music/releases"
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    val releases = JSONArray(response)
                    var highestVersion: String? = null
                    var highestTagName: String? = null
                    for (i in 0 until releases.length()) {
                        val tag = releases.getJSONObject(i).getString("tag_name")
                        if (tag.startsWith("v")) {
                            val ver = tag.removePrefix("v")
                            if (highestVersion == null || isNewerVersion(ver, highestVersion)) {
                                highestVersion = ver
                                highestTagName = tag
                            }
                        }
                    }
                    if (highestVersion != null) {
                        latestRelease = highestTagName
                        isUpdateAvailable.value = isNewerVersion(highestVersion, BuildConfig.VERSION_NAME)
                    } else {
                        isUpdateAvailable.value = false
                        latestRelease = null
                    }
                    inputStream.close()
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
//                            text = stringResource(R.string.about),
//                            modifier = Modifier.graphicsLayer { alpha = titleAlpha }
//                        )
                    },
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
                // Hero Header with Parallax
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 32.dp)
                            .graphicsLayer {
                                translationY = -headerOffset
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(30.dp))

                        Text(
                            text = "VIVI MUSIC",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 48.sp,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${BuildConfig.VERSION_NAME} • Stable",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Update Banner (if available)
                if (isUpdateAvailable.value) {
                    item {
                        Text(
                            text = "UPDATE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.NewReleases,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                title = "Update Available",
                                subtitle = "${latestRelease ?: ""} is now available",
                                onClick = { navController.navigate("settings/update") },
                                showArrow = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                titleColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                subtitleColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                arrowColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // App Information Section
                item {
                    Text(
                        text = "APP INFO",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
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
                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.info), null, modifier = Modifier.size(22.dp)) },
                                title = "Installed Date",
                                subtitle = installedDate
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            ModernInfoItem(
                                icon = { Icon(Icons.Filled.History, null, modifier = Modifier.size(22.dp)) },
                                title = "Changelog",
                                subtitle = "View version history",
                                onClick = { navController.navigate("settings/changelog") },
                                showArrow = true
                            )
                        }
                    }
                }

                // Developer Section
                item {
                    Text(
                        text = "DEVELOPER",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
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
                            ModernInfoItem(
                                icon = { Icon(Icons.Filled.Person, null, modifier = Modifier.size(22.dp)) },
                                title = "VIVIDH P ASHOKAN",
                                subtitle = "App Developer",
                                onClick = { uriHandler.openUri("https://github.com/vivizzz007") },
                                showArrow = true
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            ModernInfoItem(
                                icon = { Icon(Icons.Filled.Language, null, modifier = Modifier.size(22.dp)) },
                                title = "Website",
                                subtitle = "vivimusicwebcom.vercel.app",
                                onClick = { uriHandler.openUri("https://vivi-music-web-com.vercel.app/") },
                                showArrow = true
                            )
                        }
                    }
                }

                // Community Section
                item {
                    Text(
                        text = "COMMUNITY",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
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
                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.github_icon_about),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = Color.Unspecified
                                    )
                                },
                                title = "GitHub Repository",
                                subtitle = "View source code",
                                onClick = { uriHandler.openUri("https://github.com/vivizzz007/vivi-music") },
                                showArrow = true
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            ModernInfoItem(
                                icon = { Icon(Icons.Filled.BugReport, null, modifier = Modifier.size(22.dp)) },
                                title = "Report Issue",
                                subtitle = "Bugs & feedback",
                                onClick = { navController.navigate("settings/report_issue") },
                                showArrow = true
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            ModernInfoItem(
                                icon = { Icon(Icons.Filled.Favorite, null, modifier = Modifier.size(22.dp), tint = Color(0xFFE91E63)) },
                                title = "Donate",
                                subtitle = "Support development",
                                onClick = { navController.navigate("settings/support") },
                                showArrow = true
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

