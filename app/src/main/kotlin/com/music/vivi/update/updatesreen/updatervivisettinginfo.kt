package com.music.vivi.updatesreen

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.BuildConfig
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.settingstyle.ModernInfoItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.music.vivi.update.viewmodelupdate.UpdateViewModel
import org.json.JSONObject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViviUpdatesScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehaviorLocal = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val context = LocalContext.current

    // Collect update status and info from ViewModel
    val updateStatus by updateViewModel.updateStatus.collectAsState()
    val updateInfo by updateViewModel.updateInfo.collectAsState()
    val currentVersion = updateViewModel.getCurrentVersion()

    var lastUpdatedDate by remember { mutableStateOf("") }
    var firstInstallDate by remember { mutableStateOf("") }
    var buildVersion by remember { mutableStateOf("") }
    var cachedImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingImage by remember { mutableStateOf(true) }

    // Load cached image for current or latest version
    LaunchedEffect(updateStatus) {
        val versionToLoad = when (val status = updateStatus) {
            is UpdateStatus.UpdateAvailable -> "v${status.latestVersion}"
            else -> "v$currentVersion"
        }

        val imageUrl = loadImageFromCache(context, versionToLoad)
        cachedImageUrl = imageUrl
        isLoadingImage = false
        Log.d("ViviUpdatesScreen", "Loaded image for $versionToLoad: $imageUrl")
    }

    // Refresh update status when screen opens
    LaunchedEffect(Unit) {
        updateViewModel.refreshUpdateStatus()
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
            Spacer(modifier = Modifier.height(20.dp))
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

            // Image Box with overlay - Shows cached changelog image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                when {
                    isLoadingImage -> {
                        // Loading state
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                    cachedImageUrl != null -> {
                        // Display cached image
                        AsyncImage(
                            model = cachedImageUrl,
                            contentDescription = "Update preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )

                        // Overlay with gradient and text
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            androidx.compose.ui.graphics.Color.Transparent,
                                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )

                        // Status info overlay
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                        ) {
                            val (titleText, subtitleText) = when (val status = updateStatus) {
                                is UpdateStatus.UpdateAvailable -> {
                                    Pair("Update available", "Version ${status.latestVersion}")
                                }
                                is UpdateStatus.Disabled -> {
                                    Pair("Updates disabled", "Enable in settings")
                                }
                                is UpdateStatus.Error -> {
                                    Pair("Check failed", "Unable to check")
                                }
                                else -> {
                                    Pair("Up to date", "Version $currentVersion")
                                }
                            }

                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White,
                                fontSize = 22.sp
                            )

                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            // Show size when update is available
                            if (updateStatus is UpdateStatus.UpdateAvailable) {
                                updateInfo?.let { info ->
                                    if (info.apkSize.isNotEmpty()) {
                                        Text(
                                            text = "${info.apkSize} MB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // Fallback when no image available
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
                                .fillMaxSize()
                                .background(boxColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .align(Alignment.CenterStart),
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
                                            Pair("Updates disabled", "Enable automatic checks")
                                        }
                                        is UpdateStatus.Loading -> {
                                            Pair("Checking for updates", "Please wait...")
                                        }
                                        is UpdateStatus.Error -> {
                                            Pair("Check failed", "Unable to check")
                                        }
                                        else -> {
                                            Pair("App is up to date", "Version $currentVersion")
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
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Small Box - Navigation items
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
                                        tint = MaterialTheme.colorScheme.surfaceTint
                                    )
                                },
                                title = "System update",
                                subtitle = "Automatic checks disabled",
                                onClick = {
                                    navController.navigate("settings/update")
                                },
                                showArrow = true,
                                showSettingsIcon = true
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

// Load image URL from cached changelog data
private fun loadImageFromCache(context: Context, versionTag: String): String? {
    return try {
        val cacheFile = File(context.filesDir, "changelog_cache_$versionTag.json")
        if (!cacheFile.exists()) {
            Log.d("UpdatesScreen", "No cache found for $versionTag")
            return null
        }

        val cacheContent = context.openFileInput("changelog_cache_$versionTag.json").use {
            it.bufferedReader().readText()
        }

        val cacheData = JSONObject(cacheContent)
        val imageUrl = cacheData.optString("image", null)

        if (imageUrl.isNullOrBlank()) null else imageUrl
    } catch (e: Exception) {
        Log.e("UpdatesScreen", "Error loading image from cache", e)
        null
    }
}

// Data classes remain the same
sealed class UpdateStatus {
    object Loading : UpdateStatus()
    object UpToDate : UpdateStatus()
    object Error : UpdateStatus()
    object Disabled : UpdateStatus()
    data class UpdateAvailable(val latestVersion: String) : UpdateStatus()
}

data class UpdateInfo(
    val version: String,
    val changelog: String,
    val apkSize: String,
    val releaseDate: String,
    val description: String? = null,
    val imageUrl: String? = null
)

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)