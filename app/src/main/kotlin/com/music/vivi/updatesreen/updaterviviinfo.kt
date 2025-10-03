package com.music.vivi.updatesreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.music.vivi.BuildConfig
import com.music.vivi.LocalPlayerAwareWindowInsets
import java.text.SimpleDateFormat
import java.util.*
import com.music.vivi.R
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.music.vivi.ui.screens.getAutoUpdateCheckSetting
import com.music.vivi.update.settingstyle.ModernInfoItem


@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun SoftwareUpdatesScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehaviorLocal = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageName = context.packageName
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Loading) }
    var lastUpdatedDate by remember { mutableStateOf("") }
    var firstInstallDate by remember { mutableStateOf("") }
    var buildVersion by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val autoUpdateCheckEnabled = remember {
        mutableStateOf(getAutoUpdateCheckSetting(context))
    }

    // Check for updates when screen loads ONLY if automatic update is enabled
    LaunchedEffect(Unit) {
        scope.launch {
            if (autoUpdateCheckEnabled.value) {
                updateStatus = checkForUpdates()
            } else {
                updateStatus = UpdateStatus.UpToDate // Or whatever status you want when disabled
            }
            val packageInfo = getPackageInfo(packageManager, packageName)
            lastUpdatedDate = packageInfo.lastUpdatedDate
            firstInstallDate = packageInfo.firstInstallDate
            buildVersion = packageInfo.buildVersion
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .nestedScroll(scrollBehaviorLocal.nestedScrollConnection)
    ) {
        // Large collapsing toolbar
        TopAppBar(
            title = {
//                Text(
//                    "App updates",
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
            },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
            scrollBehavior = scrollBehaviorLocal,
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
        ) {
            // Header section
            Spacer(modifier = Modifier.height(40.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "App Updates",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Manage app updates and version information",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Spacer(Modifier.height(24.dp))

            // Main status container
            when (val status = updateStatus) {
                is UpdateStatus.UpdateAvailable -> {
                    // Update available container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NewReleases,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(Modifier.width(20.dp))

                            Column {
                                Text(
                                    text = "Update available",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 22.sp
                                )

                                Text(
                                    text = "Version ${status.latestVersion}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
                else -> {
                    // Up to date container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34A853)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(Modifier.width(20.dp))

                            Text(
                                text = "App is up to date",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 22.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Update items container
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
                    // System update item
                    when (val status = updateStatus) {
                        is UpdateStatus.UpdateAvailable -> {
                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.NewReleases,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                title = "System update",
                                subtitle = "Update available - ${status.latestVersion}",
                                onClick = {
                                    navController.navigate("settings/update")
                                },
                                showArrow = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            )
                        }
                        else -> {
                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = Color(0xFF34A853)
                                    )
                                },
                                title = "System update",
                                subtitle = "Updated to $lastUpdatedDate",
                                onClick = {
                                    navController.navigate("settings/update")
                                },
                                showArrow = true,
                                iconBackgroundColor = Color(0xFF34A853).copy(alpha = 0.2f)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.explore_outlined),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        title = "Experimental",
                        subtitle = "Experimental feature",
                        onClick = { navController.navigate("settings/experimental") },
                        showArrow = true
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.notification),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        title = "Notification",
                        subtitle = "Check for updates",
                        onClick = { navController.navigate("settings/updater") },
                        showArrow = true
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.history),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        title = "Changelog",
                        subtitle = "Current app version",
                        onClick = { navController.navigate("settings/changelog") },
                        showArrow = true
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Build version
                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        title = "Build version",
                        subtitle = buildVersion,
                        onClick = { },
                        showArrow = false
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // App build info
                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        title = "App build info",
                        subtitle = "Installed on $firstInstallDate",
                        onClick = { },
                        showArrow = false
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

sealed class UpdateStatus {
    object Loading : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class UpdateAvailable(
        val latestVersion: String,
        val downloadUrl: String
    ) : UpdateStatus()
    object Error : UpdateStatus()
}

suspend fun checkForUpdates(): UpdateStatus = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.github.com/repos/vivizzz007/vivi-music/releases/latest")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.connectTimeout = 5000
        connection.readTimeout = 10000

        if (connection.responseCode != 200) {
            return@withContext UpdateStatus.Error
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        val latestVersion = json.getString("tag_name").removePrefix("v")
        val downloadUrl = json.getString("html_url")

        val currentVersion = BuildConfig.VERSION_NAME

        if (isNewerVersion(latestVersion, currentVersion)) {
            UpdateStatus.UpdateAvailable(latestVersion, downloadUrl)
        } else {
            UpdateStatus.UpToDate
        }
    } catch (e: Exception) {
        e.printStackTrace()
        UpdateStatus.Error
    }
}

private fun isNewerVersion(latest: String, current: String): Boolean {
    try {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(latestParts.size, currentParts.size)

        for (i in 0 until maxLength) {
            val latestPart = latestParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }

            when {
                latestPart > currentPart -> return true
                latestPart < currentPart -> return false
            }
        }
        return false
    } catch (e: Exception) {
        return false
    }
}

data class PackageInfoData(
    val lastUpdatedDate: String,
    val firstInstallDate: String,
    val buildVersion: String
)

@RequiresApi(Build.VERSION_CODES.P)
private fun getPackageInfo(packageManager: PackageManager, packageName: String): PackageInfoData {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }

        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val lastUpdateTime = packageInfo.lastUpdateTime
        val firstInstallTime = packageInfo.firstInstallTime

        val lastUpdatedDate = dateFormat.format(Date(lastUpdateTime))
        val firstInstallDate = dateFormat.format(Date(firstInstallTime))
        val buildVersion = "${packageInfo.versionName} (${packageInfo.longVersionCode})"

        PackageInfoData(lastUpdatedDate, firstInstallDate, buildVersion)
    } catch (e: Exception) {
        PackageInfoData(
            "Unable to determine update date",
            "Unable to determine install date",
            "Unknown build version"
        )
    }
}

