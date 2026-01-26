package com.music.vivi.updatesreen

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.SettingsShapeColorTertiaryKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.settingstyle.Material3ExpressiveSettingsGroup
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.update.viewmodelupdate.UpdateViewModel
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViviUpdatesScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    updateViewModel: UpdateViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val errorLoadingVersion = stringResource(R.string.error_loading_version)
    val errorLoadingDate = stringResource(R.string.error_loading_date)

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
            buildVersion = errorLoadingVersion
            firstInstallDate = errorLoadingDate
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
                            onClick = { onBack?.invoke() ?: navController.navigateUp() },
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
                            text = stringResource(R.string.app_updates_title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.app_updates_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Navigation Section
                item {
                    Text(
                        text = stringResource(R.string.quick_actions).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    val quickActions = remember(updateStatus, iconBgColor, iconStyleColor) {
                        buildList<@Composable () -> Unit> {
                            add {
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
                                            title = stringResource(R.string.system_update),
                                            subtitle = stringResource(
                                                R.string.update_available_subtitle,
                                                status.latestVersion
                                            ),
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
                                            title = stringResource(R.string.system_update),
                                            subtitle = stringResource(R.string.update_disabled_subtitle),
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
                                            title = stringResource(R.string.system_update),
                                            subtitle = stringResource(R.string.up_to_date_subtitle),
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
                            }
                            add {
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.explore_outlined),
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    title = stringResource(R.string.experimental_title),
                                    subtitle = stringResource(R.string.experimental_subtitle),
                                    onClick = { navController.navigate("settings/experimental") },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                            add {
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.notification),
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    title = stringResource(R.string.update_settings_subtitle).replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                    },
                                    subtitle = stringResource(R.string.update_settings_subtitle),
                                    onClick = { navController.navigate("settings/updateinfo") },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                            add {
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.history),
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    title = stringResource(R.string.changelog),
                                    subtitle = stringResource(R.string.changelog_subtitle),
                                    onClick = { navController.navigate("settings/changelog") },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                            add {
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.rocket),
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    title = stringResource(R.string.about),
                                    subtitle = stringResource(R.string.about_subtitle),
                                    onClick = { navController.navigate("settings/about") },
                                    showArrow = true,
                                    showSettingsIcon = true,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                        }
                    }
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = quickActions
                    )
                }

                // App Info Section
                item {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.app_info_header),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    val appInfoItems = remember(buildVersion, firstInstallDate, iconBgColor, iconStyleColor) {
                        buildList<@Composable () -> Unit> {
                            add {
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.info),
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    title = stringResource(R.string.build_version_title),
                                    subtitle = buildVersion.ifEmpty {
                                        stringResource(R.string.loading_charts).removeSuffix("…")
                                    },
                                    onClick = { },
                                    showArrow = false,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                            add {
                                ModernInfoItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.info),
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    title = stringResource(R.string.installed_date_title),
                                    subtitle = if (firstInstallDate.isNotEmpty()) {
                                        stringResource(
                                            R.string.installed_on_format,
                                            firstInstallDate
                                        )
                                    } else {
                                        stringResource(R.string.loading_charts).removeSuffix("…")
                                    },
                                    onClick = { },
                                    showArrow = false,
                                    iconBackgroundColor = iconBgColor,
                                    iconContentColor = iconStyleColor
                                )
                            }
                        }
                    }
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = appInfoItems
                    )
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
    val imageUrl: String? = null,
)

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
