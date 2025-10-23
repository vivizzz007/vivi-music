package com.music.vivi.updatesreen

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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.music.vivi.ui.screens.checkForUpdate
import com.music.vivi.ui.screens.getAutoUpdateCheckSetting
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.experiment.getBetaUpdaterSetting
import com.music.vivi.update.settingstyle.ModernInfoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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
    val currentVersion = BuildConfig.VERSION_NAME

    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Loading) }
    var lastUpdatedDate by remember { mutableStateOf("") }
    var firstInstallDate by remember { mutableStateOf("") }
    var buildVersion by remember { mutableStateOf("") }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    val autoUpdateCheckEnabled = remember {
        mutableStateOf(getAutoUpdateCheckSetting(context))
    }
    val betaUpdaterEnabled = remember {
        mutableStateOf(getBetaUpdaterSetting(context))
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val packageInfo = getPackageInfo(packageManager, packageName)
                withContext(Dispatchers.Main) {
                    lastUpdatedDate = packageInfo.lastUpdatedDate
                    firstInstallDate = packageInfo.firstInstallDate
                    buildVersion = packageInfo.buildVersion
                }

                if (autoUpdateCheckEnabled.value) {
                    checkForUpdate(
                        isBetaEnabled = betaUpdaterEnabled.value,
                        onSuccess = { version, changelog, apkSize, releaseDate ->
                            if (isNewerVersion(version, currentVersion)) {
                                updateStatus = UpdateStatus.UpdateAvailable(version, "")
                                updateInfo = UpdateInfo(version, changelog, apkSize, releaseDate)
                            } else {
                                updateStatus = UpdateStatus.UpToDate
                            }
                        },
                        onError = {
                            updateStatus = UpdateStatus.UpToDate
                        }
                    )
                } else {
                    updateStatus = UpdateStatus.UpToDate
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateStatus = UpdateStatus.Error
                }
            }
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
                    text = "Manage app updates and version information",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Spacer(Modifier.height(24.dp))

            when (val status = updateStatus) {
                is UpdateStatus.UpdateAvailable -> {
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

                                updateInfo?.let { info ->
                                    if (info.apkSize.isNotEmpty()) {
                                        Text(
                                            text = "${info.apkSize} MB • ${info.releaseDate}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is UpdateStatus.Loading -> {
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )

                            Spacer(Modifier.width(20.dp))

                            Text(
                                text = "Checking for updates...",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 22.sp
                            )
                        }
                    }
                }
                else -> {
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
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.updated),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
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
                                iconBackgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
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
                                subtitle = if (lastUpdatedDate.isNotEmpty()) "Updated to $lastUpdatedDate" else "Checking...",
                                onClick = {
                                    navController.navigate("settings/update")
                                },
                                showArrow = true
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
                        title = "Update Settings",
                        subtitle = "Turn on and off updates",
                        onClick = { navController.navigate("settings/updateinfo") },
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
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.rocket),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        title = "About",
                        subtitle = "app & dev information",
                        onClick = { navController.navigate("settings/about") },
                        showArrow = true
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
                                modifier = Modifier.size(22.dp)
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
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        title = "App build info",
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

// Data class to hold update info
data class UpdateInfo(
    val version: String,
    val changelog: String,
    val apkSize: String,
    val releaseDate: String
)

sealed class UpdateStatus {
    object Loading : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class UpdateAvailable(
        val latestVersion: String,
        val downloadUrl: String
    ) : UpdateStatus()
    object Error : UpdateStatus()
}

// Keep existing helper functions
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
        val buildVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } else {
            @Suppress("DEPRECATION")
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        }
        PackageInfoData(lastUpdatedDate, firstInstallDate, buildVersion)
    } catch (e: Exception) {
        PackageInfoData(
            "Unable to determine update date",
            "Unable to determine install date",
            "Unknown build version"
        )
    }
}
