package com.music.vivi.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.background
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
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.screens.checkForUpdate
import com.music.vivi.ui.screens.getAutoUpdateCheckSetting
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.isNewerVersion
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.update.viewmodelupdate.UpdateViewModel
import com.music.vivi.updatesreen.UpdateInfo
import com.music.vivi.updatesreen.UpdateStatus
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
        "Manage your account and preferences"
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
            Spacer(modifier = Modifier.height(40.dp))

            // Settings Title
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Unified settings card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {

                    // App Updates item with dynamic state from ViewModel
                    ModernInfoItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.NewReleases,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = when (updateStatus) {
                                    is UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.error
                                    is UpdateStatus.Loading -> MaterialTheme.colorScheme.onSurfaceVariant
                                    is UpdateStatus.Disabled -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        },
                        title = when (updateStatus) {
                            is UpdateStatus.UpdateAvailable -> "Update Available"
                            is UpdateStatus.Loading -> stringResource(R.string.appupdate)
                            is UpdateStatus.Disabled -> stringResource(R.string.appupdate)
                            else -> stringResource(R.string.appupdate)
                        },
                        subtitle = when (updateStatus) {
                            is UpdateStatus.Disabled -> {
                                "Automatic check disabled • V${currentVersion}"
                            }
                            is UpdateStatus.UpdateAvailable -> {
                                "${(updateStatus as UpdateStatus.UpdateAvailable).latestVersion} is now available"
                            }
                            is UpdateStatus.Loading -> {
                                "Checking for updates..."
                            }
                            else -> {
                                "Current version: ${currentVersion}"
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
                        iconBackgroundColor = when (updateStatus) {
                            is UpdateStatus.UpdateAvailable -> {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            }
                            is UpdateStatus.Loading -> {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            }
                            is UpdateStatus.Disabled -> {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                            }
                            else -> {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            }
                        },
                        arrowColor = when (updateStatus) {
                            is UpdateStatus.UpdateAvailable -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

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
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        title = accountTitle,
                        subtitle = accountSubtitle,
                        onClick = { navController.navigate("settings/account_view") },
                        showArrow = true,
                        iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Appearance
                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.palette),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = stringResource(R.string.appearance),
                        subtitle = "Customize theme and display settings",
                        onClick = { navController.navigate("settings/appearance") },
                        showArrow = true,
                        iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Player & Audio
                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.play),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = stringResource(R.string.player_and_audio),
                        subtitle = "Audio quality and playback settings",
                        onClick = { navController.navigate("settings/player") },
                        showArrow = true,
                        iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Content
                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.language),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = stringResource(R.string.content),
                        subtitle = "Language and content preferences",
                        onClick = { navController.navigate("settings/content") },
                        showArrow = true,
                        iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Privacy
                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.security),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = stringResource(R.string.privacy),
                        subtitle = "Privacy and security settings",
                        onClick = { navController.navigate("settings/privacy") },
                        showArrow = true,
                        iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Storage
                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.storage),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = stringResource(R.string.storage),
                        subtitle = "Manage storage and downloads",
                        onClick = { navController.navigate("settings/storage") },
                        showArrow = true,
                        iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Backup & Restore
                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.restore),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = stringResource(R.string.backup_restore),
                        subtitle = "Backup and restore your data",
                        onClick = { navController.navigate("settings/backup_restore") },
                        showArrow = true,
                        iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // About
                    ModernInfoItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.rocket),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = stringResource(R.string.about),
                        subtitle = "App information and legal",
                        onClick = { navController.navigate("settings/about") },
                        showArrow = true,
                        iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}