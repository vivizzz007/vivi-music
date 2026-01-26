package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.toShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.vivi.BuildConfig
import com.music.vivi.R
import com.music.vivi.constants.CheckForUpdatesKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.SettingsShapeColorTertiaryKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.isNewerVersion
import com.music.vivi.update.settingstyle.Material3ExpressiveSettingsGroup
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen displaying app information, "About" details, developer info, and changelog.
 * Also handles fetching and displaying updates from GitHub.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(navController: NavController, scrollBehavior: TopAppBarScrollBehavior, onBack: (() -> Unit)? = null) {
    val (settingsShapeTertiary, _) = rememberPreference(SettingsShapeColorTertiaryKey, false)
    val (darkMode, _) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkMode, isSystemInDarkTheme) {
        if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
    }

    val (iconBgColor, iconStyleColor) = if (settingsShapeTertiary) {
        if (useDarkTheme) {
            Pair(
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.onTertiary
            )
        } else {
            Pair(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    } else {
        Pair(
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.primary
        )
    }

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val unknownString = stringResource(R.string.unknown)
    val scrollState = rememberLazyListState()

    var latestRelease by remember { mutableStateOf<String?>(null) }
    val isUpdateAvailable = remember { mutableStateOf(false) }

    // Get auto-update preference
    val (checkForUpdates, _) = rememberPreference(CheckForUpdatesKey, true)

    // Parallax effect for header
    val headerOffset by remember {
        derivedStateOf {
            (scrollState.firstVisibleItemScrollOffset * 0.5f).coerceAtMost(300f)
        }
    }

    // Get app install date
    val installedDate = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val installTime = packageInfo.firstInstallTime
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(installTime))
        } catch (_: Exception) {
            unknownString
        }
    }

    // Update check LaunchedEffect - only run if auto-update is enabled
    LaunchedEffect(checkForUpdates) {
        if (checkForUpdates) {
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
        } else {
            // If auto-update is disabled, reset the update state
            isUpdateAvailable.value = false
            latestRelease = null
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
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { onBack?.invoke() ?: navController.navigateUp() },
                            onLongClick = navController::backToMain
                        ) {
                            Icon(
                                painterResource(R.drawable.arrow_back),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp) // Increased back icon size
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
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.vivi_music_title),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 48.sp,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME} • Van Halen • Stable",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Update Banner (only show if update is available AND auto-update is enabled)
                if (checkForUpdates && isUpdateAvailable.value) {
                    item {
                        Text(
                            text = stringResource(R.string.update_section),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp) // Changed from 16.dp to 8.dp
                        )
                    }

                    item {
                        Material3ExpressiveSettingsGroup(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            items = listOf {
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.network_intelligence_update_vivi),
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    title = stringResource(R.string.update_available),
                                    subtitle = stringResource(R.string.version_now_available, latestRelease ?: ""),
                                    onClick = { navController.navigate("settings/update") },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    titleColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    subtitleColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    arrowColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                )
                            }
                        )
                    }
                }

                // Developer Section
                item {
                    Text(
                        text = stringResource(R.string.developer_section),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = listOf(
                            {
                                ModernInfoItem(
                                    icon = {
                                        Image(
                                            painter = painterResource(R.drawable.dev),
                                            contentDescription = stringResource(R.string.developer),
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    },
                                    title = stringResource(R.string.developer_name),
                                    subtitle = stringResource(R.string.app_developer),
                                    onClick = { uriHandler.openUri("https://github.com/vivizzz007") },
                                    showArrow = true,
                                    iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                    iconShape = MaterialShapes.Circle.toShape(),
                                    iconSize = 48.dp
                                )
                            },
                            {
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            painterResource(R.drawable.web_vivi),
                                            null,
                                            modifier = Modifier.size(28.dp) // Increased from 22dp to 28dp
                                        )
                                    },
                                    title = stringResource(R.string.website),
                                    subtitle = "vivimusic.vercel.app",
                                    onClick = { uriHandler.openUri("https://vivimusic.vercel.app/") },
                                    showArrow = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                        )
                    )
                }

                // Collaborator Section
                item {
                    Text(
                        text = stringResource(R.string.collaborator_section),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = listOf {
                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.person),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                },
                                title = stringResource(R.string.collaborator_tboyke),
                                subtitle = stringResource(R.string.collaborator_role),
                                onClick = { uriHandler.openUri("https://github.com/T-Boyke") },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor
                            )
                        }
                    )
                }

                // App Information Section
                item {
                    Text(
                        text = stringResource(R.string.app_info_section),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = listOf(
                            {
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            painterResource(R.drawable.info),
                                            null,
                                            modifier = Modifier.size(28.dp) // Increased from 22dp to 28dp
                                        )
                                    },
                                    title = stringResource(R.string.installed_date_title),
                                    subtitle = installedDate,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            },
                            {
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            Icons.Filled.History,
                                            null,
                                            modifier = Modifier.size(28.dp) // Increased from 22dp to 28dp
                                        )
                                    },
                                    title = stringResource(R.string.changelog),
                                    subtitle = stringResource(R.string.view_version_history),
                                    onClick = { navController.navigate("settings/changelog") },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                        )
                    )
                }

                // Community Section
                item {
                    Text(
                        text = stringResource(R.string.community_section),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                // Replace the Community Section in your AboutScreen.kt with this:

// Community Section
                item {
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = listOf(
                            {
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.github),
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    },
                                    title = stringResource(R.string.github_repository),
                                    subtitle = stringResource(R.string.view_source_code),
                                    onClick = { uriHandler.openUri("https://github.com/vivizzz007/vivi-music") },
                                    showArrow = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            },
                            {
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.person),
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    },
                                    title = stringResource(R.string.contributors),
                                    subtitle = stringResource(R.string.see_community_heroes),
                                    onClick = { navController.navigate("settings/contribution") },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            },
                            {
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            Icons.Filled.BugReport,
                                            null,
                                            modifier = Modifier.size(28.dp)
                                            // No tint specified - uses default onSurface
                                        )
                                    },
                                    title = stringResource(R.string.report_issue_title_about),
                                    subtitle = stringResource(R.string.bugs_feedback),
                                    onClick = { navController.navigate("settings/report_issue") },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            },
                            {
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            Icons.Filled.Favorite,
                                            null,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    },
                                    title = stringResource(R.string.donate),
                                    subtitle = stringResource(R.string.support_development),
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor,
                                    onClick = { navController.navigate("settings/support") },
                                    showArrow = true,
                                    showSettingsIcon = true
                                )
                            }
                        )
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
