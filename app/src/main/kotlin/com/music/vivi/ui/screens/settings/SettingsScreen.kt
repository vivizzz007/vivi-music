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
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.screens.checkForUpdate
import com.music.vivi.ui.screens.getAutoUpdateCheckSetting
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.isNewerVersion
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.updatesreen.UpdateStatus
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    LocalUriHandler.current
    val context = LocalContext.current
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Add these preferences to get account information
    val (accountNamePref, _) = rememberPreference(AccountNameKey, "")
    val (accountEmail, _) = rememberPreference(AccountEmailKey, "")
    val (innerTubeCookie, _) = rememberPreference(InnerTubeCookieKey, "")

    // Add ViewModels to get account data
    val homeViewModel = hiltViewModel<HomeViewModel>()
    val accountName by homeViewModel.accountName.collectAsState()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()

    // Check if user is logged in
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    // Determine what to display for account section
    val accountTitle = when {
        isLoggedIn && accountName.isNotBlank() -> accountName
        isLoggedIn && accountNamePref.isNotBlank() -> accountNamePref
        isLoggedIn -> stringResource(R.string.account) // Fallback if logged in but no name
        else -> stringResource(R.string.account) // Not logged in
    }

    val accountSubtitle = if (isLoggedIn && accountEmail.isNotBlank()) {
        accountEmail
    } else {
        "Manage your account and preferences"
    }

    // Read the automatic update check setting
    val autoUpdateCheckEnabled = remember {
        mutableStateOf(getAutoUpdateCheckSetting(context))
    }

    // Add this state variable at the top of your SettingsScreen composable
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.UpToDate) }

    val isUpdateAvailable = updateStatus is UpdateStatus.UpdateAvailable

    // Launch effect to check for updates ONLY if automatic update is enabled
    LaunchedEffect(Unit) {
        if (autoUpdateCheckEnabled.value) {
            withContext(Dispatchers.IO) {
                try {
                    withTimeout(8000L) {
                        checkForUpdate(
                            isBetaEnabled = false,
                            onSuccess = { version, _, _, _ ->
                                // ✅ ADD PROPER VERSION COMPARISON HERE
                                if (isNewerVersion(version, BuildConfig.VERSION_NAME)) {
                                    updateStatus = UpdateStatus.UpdateAvailable(version, "")
                                } else {
                                    updateStatus = UpdateStatus.UpToDate
                                }
                            },
                            onError = {
                                updateStatus = UpdateStatus.UpToDate
                            }
                        )
                    }
                } catch (e: Exception) {
                    updateStatus = UpdateStatus.UpToDate
                }
            }
        } else {
            updateStatus = UpdateStatus.UpToDate
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                },
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

            // Settings Title (outside the card)
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Single unified settings card with proper curves
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {

                    ModernInfoItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.NewReleases,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (isUpdateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        title = if (isUpdateAvailable) "Update Available" else stringResource(R.string.appupdate),
                        subtitle = if (!autoUpdateCheckEnabled.value) {
                            "V : ${BuildConfig.VERSION_NAME}"
                        } else if (isUpdateAvailable) {
                            "${(updateStatus as UpdateStatus.UpdateAvailable).latestVersion} is now available"
                        } else {
                            "Current version: ${BuildConfig.VERSION_NAME}"
                        },
                        titleColor = if (isUpdateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        subtitleColor = if (isUpdateAvailable) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { navController.navigate("settings/software_updates") },
                        showArrow = true,
                        iconBackgroundColor = if (isUpdateAvailable) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        },
                        arrowColor = if (isUpdateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Account - Modified to show user name, email and profile image when logged in
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        title = accountTitle,
                        subtitle = accountSubtitle,
                        onClick = { navController.navigate("settings/account_view") },
                        showArrow = true,
                        iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    // ... rest of your existing settings items remain exactly the same
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
