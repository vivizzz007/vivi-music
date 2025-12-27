package com.music.vivi.updatesreen

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.SettingsShapeColorTertiaryKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
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
    val context = LocalContext.current

    // Theme-aware icon colors
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

    // Collect update status and info from ViewModel
    val updateStatus by updateViewModel.updateStatus.collectAsState()
    val updateInfo by updateViewModel.updateInfo.collectAsState()
    val currentVersion = updateViewModel.getCurrentVersion()

    var lastUpdatedDate by remember { mutableStateOf("") }
    var firstInstallDate by remember { mutableStateOf("") }
    var buildVersion by remember { mutableStateOf("") }

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

    val scrollState = rememberLazyListState()

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
                    title = {},
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
                item {
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
                }

                item {
                    Spacer(Modifier.height(24.dp))

                    // Status Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (updateStatus) {
                                is UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (icon, titleText, subtitleText) = when (val status = updateStatus) {
                                is UpdateStatus.UpdateAvailable -> {
                                    Triple(Icons.Filled.NewReleases, "Update available", "Version ${status.latestVersion}")
                                }
                                is UpdateStatus.Loading -> {
                                    Triple(Icons.Filled.NewReleases, "Checking for updates", "Please wait...")
                                }
                                is UpdateStatus.Error -> {
                                    Triple(Icons.Filled.NewReleases, "Check failed", "Unable to check")
                                }
                                else -> {
                                    Triple(painterResource(R.drawable.updated), "App is up to date", "Version $currentVersion")
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                if (icon is androidx.compose.ui.graphics.vector.ImageVector) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (updateStatus is UpdateStatus.UpdateAvailable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        painter = icon as androidx.compose.ui.graphics.painter.Painter,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }

                // Navigation Section
                item {
                    Text(
                        text = "QUICK ACTIONS",
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
                            when (val status = updateStatus) {
                                is UpdateStatus.UpdateAvailable -> {
                                    ModernInfoItem(
                                        icon = {
                                            Icon(
                                                painter = painterResource(R.drawable.rocket_new_update),
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp)
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
                                        iconContentColor = MaterialTheme.colorScheme.error,
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
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        title = "System update",
                                        subtitle = "Automatic checks disabled",
                                        onClick = {
                                            navController.navigate("settings/update")
                                        },
                                        showArrow = true,
                                        showSettingsIcon = true,
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor
                                    )
                                }
                                else -> {
                                    ModernInfoItem(
                                        icon = {
                                            Icon(
                                                painter = painterResource(R.drawable.updated),
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        title = "System update",
                                        subtitle = "App is up to date",
                                        onClick = {
                                            navController.navigate("settings/update")
                                        },
                                        showArrow = true,
                                        showSettingsIcon = true,
                                        iconBackgroundColor = iconBgColor,
                                        iconContentColor = iconStyleColor
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
                                showArrow = true,
                                showSettingsIcon = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor
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
                                showArrow = true,
                                showSettingsIcon = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor
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
                                showArrow = true,
                                showSettingsIcon = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
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
                                showArrow = true,
                                showSettingsIcon = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor
                            )
                        }
                    }
                }

                // App Info Section
                item {
                    Spacer(Modifier.height(24.dp))
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
                                showArrow = false,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor
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
                                title = "Installed date",
                                subtitle = if (firstInstallDate.isNotEmpty()) "Installed on $firstInstallDate" else "Loading...",
                                onClick = { },
                                showArrow = false,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor
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