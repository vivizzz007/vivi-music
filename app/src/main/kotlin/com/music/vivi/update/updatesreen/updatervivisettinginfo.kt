package com.music.vivi.updatesreen

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.vivi.BuildConfig
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.screens.KEY_AUTO_UPDATE_CHECK
import com.music.vivi.ui.screens.PREFS_NAME
import com.music.vivi.ui.screens.checkForUpdate
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.isNewerVersion
import com.music.vivi.update.settingstyle.ModernInfoItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViviUpdatesScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehaviorLocal = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val context = LocalContext.current
    val currentVersion = BuildConfig.VERSION_NAME

    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Loading) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    var lastUpdatedDate by remember { mutableStateOf("") }
    var firstInstallDate by remember { mutableStateOf("") }
    var buildVersion by remember { mutableStateOf("") }

    // Get the auto-update check preference
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val checkForUpdates = sharedPrefs.getBoolean(KEY_AUTO_UPDATE_CHECK, true)

    // Modified update checking logic - only run if auto-update is enabled
    LaunchedEffect(checkForUpdates) {
        if (checkForUpdates) {
            // Set loading state first
            updateStatus = UpdateStatus.Loading

            checkForUpdate(
                onSuccess = { latestVersion, changelog, apkSize, releaseDate ->
                    if (isNewerVersion(latestVersion, currentVersion)) {
                        updateStatus = UpdateStatus.UpdateAvailable(latestVersion)
                        updateInfo = UpdateInfo(latestVersion, changelog, apkSize, releaseDate)
                    } else {
                        updateStatus = UpdateStatus.UpToDate
                    }
                },
                onError = {
                    updateStatus = UpdateStatus.Error
                }
            )
        } else {
            // When auto-update is disabled, immediately set status to disabled
            // Clear any previous update info
            updateStatus = UpdateStatus.Disabled
            updateInfo = null
        }
    }

    // Get app info
    LaunchedEffect(Unit) {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            buildVersion = "$versionName (Build $versionCode)"

            val firstInstall = packageInfo.firstInstallTime
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            firstInstallDate = dateFormat.format(Date(firstInstall))

            val lastUpdate = packageInfo.lastUpdateTime
            lastUpdatedDate = dateFormat.format(Date(lastUpdate))

        } catch (e: Exception) {
            buildVersion = "Error loading version"
            firstInstallDate = "Error loading date"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .nestedScroll(scrollBehaviorLocal.nestedScrollConnection)
    ) {
        TopAppBar(
            title = { },
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
                    text = "App updates and version information",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Spacer(Modifier.height(24.dp))

            // Large Box - Shows different states based on update status
            val (boxColor, iconTint, textColor, subtextColor) = when (updateStatus) {
                is UpdateStatus.UpdateAvailable -> {
                    Quadruple(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.onErrorContainer,
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                }
                is UpdateStatus.Disabled -> {
                    Quadruple(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.onSurface,
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                else -> {
                    Quadruple(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.onSurface,
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(
                        color = boxColor,
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
                        when (updateStatus) {
                            is UpdateStatus.UpdateAvailable -> {
                                Icon(
                                    imageVector = Icons.Filled.NewReleases,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = iconTint
                                )
                            }
                            is UpdateStatus.Disabled -> {
                                Icon(
                                    painter = painterResource(R.drawable.updated),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = iconTint
                                )
                            }
                            is UpdateStatus.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = iconTint
                                )
                            }
                            else -> {
                                Icon(
                                    painter = painterResource(R.drawable.updated),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = iconTint
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(20.dp))

                    Column {
                        val (titleText, subtitleText) = when (val status = updateStatus) {
                            is UpdateStatus.UpdateAvailable -> {
                                Pair("Update available", "Version ${status.latestVersion}")
                            }
                            is UpdateStatus.Disabled -> {
                                Pair("Updates disabled", "Enable automatic checks \nin update settings")
                            }
                            is UpdateStatus.Loading -> {
                                Pair("Checking for updates", "Please wait...")
                            }
                            is UpdateStatus.Error -> {
                                Pair("Check failed", "Unable to check for updates")
                            }
                            else -> {
                                Pair("App is up to date", "Current version $currentVersion")
                            }
                        }

                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            fontSize = 22.sp
                        )

                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = subtextColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // Only show size when update is available
                        if (updateStatus is UpdateStatus.UpdateAvailable) {
                            updateInfo?.let { info ->
                                if (info.apkSize.isNotEmpty()) {
                                    Text(
                                        text = "${info.apkSize} MB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = subtextColor.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Small Box - Shows different states
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
                    when (val status = updateStatus) {
                        is UpdateStatus.UpdateAvailable -> {
                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.rocket_new_update),
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
                                showSettingsIcon = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                arrowColor = MaterialTheme.colorScheme.error,
                                settingsIconColor = MaterialTheme.colorScheme.error
                            )
                        }
                        is UpdateStatus.Disabled -> {
                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.updated),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
//                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        tint = MaterialTheme.colorScheme.surfaceTint
                                    )
                                },
                                title = "System update",
                                subtitle = "Automatic checks disabled",
                                onClick = {
                                    navController.navigate("settings/updateinfo")
                                },
                                showArrow = true,
                                showSettingsIcon = true
                            )
                        }
                        is UpdateStatus.Loading -> {
                            ModernInfoItem(
                                icon = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.surfaceTint
                                    )
                                },
                                title = "System update",
                                subtitle = "Checking for updates...",
                                onClick = { },
                                showArrow = false,
                                showSettingsIcon = false
                            )
                        }
                        else -> {
                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.updated),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.surfaceTint
                                    )
                                },
                                title = "System update",
                                subtitle = "App is up to date",
                                onClick = {
                                    navController.navigate("settings/update")
                                },
                                showArrow = true,
                                showSettingsIcon = true
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
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.surfaceTint
                            )
                        },
                        title = "Experimental",
                        subtitle = "Experimental feature",
                        onClick = { navController.navigate("settings/experimental") },
                        showArrow = true,
                        showSettingsIcon = true
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
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.surfaceTint
                            )
                        },
                        title = "Update Settings",
                        subtitle = "Turn on and off updates",
                        onClick = { navController.navigate("settings/updateinfo") },
                        showArrow = true,
                        showSettingsIcon = true
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
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.surfaceTint
                            )
                        },
                        title = "Changelog",
                        subtitle = "Current app version",
                        onClick = { navController.navigate("settings/changelog") },
                        showArrow = true,
                        showSettingsIcon = true
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.rocket),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.surfaceTint
                            )
                        },
                        title = "About",
                        subtitle = "app & dev information",
                        onClick = { navController.navigate("settings/about") },
                        showArrow = true,
                        showSettingsIcon = true
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.surfaceTint
                            )
                        },
                        title = "Build version",
                        subtitle = buildVersion.ifEmpty { "Loading..." },
                        onClick = { },
                        showArrow = false
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.surfaceTint
                            )
                        },
                        title = "installed date",
                        subtitle = if (firstInstallDate.isNotEmpty()) "Installed on $firstInstallDate" else "Loading...",
                        onClick = { },
                        showArrow = false
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// Required data classes
sealed class UpdateStatus {
    object Loading : UpdateStatus()
    object UpToDate : UpdateStatus()
    object Error : UpdateStatus()
    object Disabled : UpdateStatus()  // State when auto-check is disabled
    data class UpdateAvailable(val latestVersion: String) : UpdateStatus()
}

data class UpdateInfo(
    val version: String,
    val changelog: String,
    val apkSize: String,
    val releaseDate: String
)

// Helper data class for colors
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)