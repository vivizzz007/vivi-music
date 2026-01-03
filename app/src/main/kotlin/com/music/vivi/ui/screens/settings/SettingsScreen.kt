package com.music.vivi.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.utils.parseCookieString
import com.music.vivi.BuildConfig
import com.music.vivi.R
import com.music.vivi.constants.AccountEmailKey
import com.music.vivi.constants.AccountNameKey
import com.music.vivi.constants.CheckForUpdatesKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.SettingsShapeColorTertiaryKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.screens.checkForUpdate
import com.music.vivi.ui.screens.getAutoUpdateCheckSetting
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.isNewerVersion
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.update.viewmodelupdate.UpdateViewModel
import com.music.vivi.updatesreen.UpdateInfo
import com.music.vivi.updatesreen.UpdateStatus
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.HomeViewModel

//new view model for update
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    updateViewModel: UpdateViewModel = hiltViewModel() // Use ViewModel instead of parameter
) {
    val context = LocalContext.current

    // Collect update status from ViewModel
    val updateStatus by updateViewModel.updateStatus.collectAsState()
    val currentVersion = updateViewModel.getCurrentVersion()

    // Monitor preference changes and refresh ViewModel immediately
    val (checkForUpdatesPreference, _) = rememberPreference(CheckForUpdatesKey, true)

    LaunchedEffect(checkForUpdatesPreference) {
        // Immediately update ViewModel when preference changes
        updateViewModel.refreshUpdateStatus()
    }

    // Get account preferences
    val (accountNamePref, _) = rememberPreference(AccountNameKey, "")
    val (accountEmail, _) = rememberPreference(AccountEmailKey, "")
    val (innerTubeCookie, _) = rememberPreference(InnerTubeCookieKey, "")

    // Get ViewModels to get account data
    val homeViewModel = hiltViewModel<HomeViewModel>()
    val accountName by homeViewModel.accountName.collectAsState(initial = "")
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState(initial = null)

    // Check if user is logged in
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

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

    // Determine what to display for account section
    val accountTitle = when {
        isLoggedIn && accountName.isNotBlank() -> accountName
        isLoggedIn && accountNamePref.isNotBlank() -> accountNamePref
        isLoggedIn -> stringResource(R.string.account)
        else -> stringResource(R.string.account)
    }

    val accountSubtitle = if (isLoggedIn && accountEmail.isNotBlank()) {
        accountEmail
    } else {
        stringResource(R.string.manage_account_preferences)
    }

    val isUpdateAvailable = updateStatus is UpdateStatus.UpdateAvailable

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Settings Title
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Unified settings card replaced with Material3 Expressive Group
            com.music.vivi.update.settingstyle.Material3ExpressiveSettingsGroup(
                modifier = Modifier.fillMaxWidth(),
                items = listOf(
                    {
                        // App Updates
                        ModernInfoItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.NewReleases,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = when (updateStatus) {
                                        is UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.error
                                        is UpdateStatus.Loading -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        is UpdateStatus.Disabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        else -> LocalContentColor.current
                                    }
                                )
                            },
                            title = when (updateStatus) {
                                is UpdateStatus.UpdateAvailable -> stringResource(R.string.update_available_title)
                                is UpdateStatus.Loading -> stringResource(R.string.appupdate)
                                is UpdateStatus.Disabled -> stringResource(R.string.appupdate)
                                else -> stringResource(R.string.appupdate)
                            },
                            subtitle = when (updateStatus) {
                                is UpdateStatus.Disabled -> {
                                    stringResource(R.string.automatic_check_disabled, currentVersion)
                                }
                                is UpdateStatus.UpdateAvailable -> {
                                    stringResource(R.string.version_now_available, (updateStatus as UpdateStatus.UpdateAvailable).latestVersion)
                                }
                                is UpdateStatus.Loading -> {
                                    stringResource(R.string.checking_for_updates)
                                }
                                else -> {
                                    stringResource(R.string.current_version, currentVersion)
                                }
                            },
                            titleColor = when (updateStatus) {
                                is UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            subtitleColor = when (updateStatus) {
                                is UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                is UpdateStatus.Loading -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                is UpdateStatus.Disabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            onClick = { navController.navigate("settings/software_updates") },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor,
                            arrowColor = when (updateStatus) {
                                is UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    },
                    {
                        // Account
                        ModernInfoItem(
                            icon = {
                                if (isLoggedIn && accountImageUrl != null) {
                                    AsyncImage(
                                        model = accountImageUrl,
                                        contentDescription = stringResource(R.string.profile_image),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(
                                            if (isLoggedIn) R.drawable.person else R.drawable.account
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            title = accountTitle,
                            subtitle = accountSubtitle,
                            onClick = { navController.navigate("settings/account_view") },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor
                        )
                    },
                    {
                        // Appearance
                        ModernInfoItem(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.palette),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            title = stringResource(R.string.appearance),
                            subtitle = stringResource(R.string.customize_theme_display_settings),
                            onClick = { navController.navigate("settings/appearance") },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor
                        )
                    },
                    {
                        // Player & Audio
                        ModernInfoItem(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.play),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            title = stringResource(R.string.player_and_audio),
                            subtitle = stringResource(R.string.audio_quality_playback_settings),
                            onClick = { navController.navigate("settings/player") },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor
                        )
                    },
                    {
                        // Content
                        ModernInfoItem(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.language),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            title = stringResource(R.string.content),
                            subtitle = stringResource(R.string.language_content_preferences),
                            onClick = { navController.navigate("settings/content") },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor
                        )
                    },
                    {
                        // Privacy
                        ModernInfoItem(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.security),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            title = stringResource(R.string.privacy),
                            subtitle = stringResource(R.string.privacy_security_settings),
                            onClick = { navController.navigate("settings/privacy") },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor
                        )
                    },
                    {
                        // Storage
                        ModernInfoItem(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.storage),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            title = stringResource(R.string.storage),
                            subtitle = stringResource(R.string.manage_storage_downloads),
                            onClick = { navController.navigate("settings/storage") },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor
                        )
                    },
                    {
                        // Backup & Restore
                        ModernInfoItem(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.restore),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            title = stringResource(R.string.backup_restore),
                            subtitle = stringResource(R.string.backup_restore_data),
                            onClick = { navController.navigate("settings/backup_restore") },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor
                        )
                    },
                    {
                        // About
                        ModernInfoItem(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.rocket),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            title = stringResource(R.string.about),
                            subtitle = stringResource(R.string.app_information_legal),
                            onClick = { navController.navigate("settings/about") },
                            showArrow = true,
                            iconBackgroundColor = iconBgColor,
                            iconContentColor = iconStyleColor
                        )
                    }
                )
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}