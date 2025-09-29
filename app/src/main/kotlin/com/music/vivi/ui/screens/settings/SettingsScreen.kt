package com.music.vivi.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.ui.component.Material3SettingsGroup
import com.music.vivi.ui.component.Material3SettingsItem

import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.Updater
import com.music.vivi.R
import com.music.vivi.constants.AccountEmailKey
import com.music.vivi.constants.AccountNameKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.screens.getAutoUpdateCheckSetting
import com.music.vivi.updatesreen.UpdateStatus
import com.music.vivi.updatesreen.checkForUpdates
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

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

    // Animation states for update item
    val isUpdateAvailable = updateStatus is UpdateStatus.UpdateAvailable
    val backgroundColor by animateColorAsState(
        targetValue = if (isUpdateAvailable) {
            Color.Red.copy(alpha = 0.1f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "updateBackgroundColor"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isUpdateAvailable) {
            Color.Red
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "updateIconTint"
    )

    val subtitleColor by animateColorAsState(
        targetValue = if (isUpdateAvailable) {
            Color.Red
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "updateSubtitleColor"
    )

    // Scale animation for the update item
    val scale by animateFloatAsState(
        targetValue = if (isUpdateAvailable) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "updateItemScale"
    )

    // Launch effect to check for updates ONLY if automatic update is enabled
    LaunchedEffect(Unit) {
        if (autoUpdateCheckEnabled.value) {
            updateStatus = checkForUpdates()
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Single unified settings card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {

                    // App Update with animated properties
                    SettingItem(
                        icon = {
                            Icon(
                                Icons.Default.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = iconTint
                            )
                        },
                        title = stringResource(R.string.appupdate),
                        subtitle = if (!autoUpdateCheckEnabled.value) {
                            "V : ${BuildConfig.VERSION_NAME}"
                        } else if (updateStatus is UpdateStatus.UpdateAvailable) {
                            "Update ${(updateStatus as UpdateStatus.UpdateAvailable).latestVersion} available"
                        } else {
                            "Current version: ${BuildConfig.VERSION_NAME}"
                        },
                        subtitleColor = subtitleColor,
                        onClick = { navController.navigate("settings/software_updates") },
                        modifier = Modifier
                            .background(backgroundColor, RoundedCornerShape(8.dp))
                            .scale(scale)
                    )

                    // Animated divider for update section
                    AnimatedVisibility(
                        visible = isUpdateAvailable,
                        enter = slideInVertically() + expandVertically() + fadeIn(),
                        exit = slideOutVertically() + shrinkVertically() + fadeOut()
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = Color.Red.copy(alpha = 0.3f)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Account - Modified to show user name, email and profile image when logged in
                    SettingItem(
                        icon = {
                            if (isLoggedIn && accountImageUrl != null) {
                                AsyncImage(
                                    model = accountImageUrl,
                                    contentDescription = stringResource(R.string.profile_image),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(35.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(
                                        if (isLoggedIn) R.drawable.person else R.drawable.account
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        title = accountTitle,
                        subtitle = accountSubtitle,
                        onClick = { navController.navigate("settings/account_view") }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Appearance
                    SettingItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.palette),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        title = stringResource(R.string.appearance),
                        subtitle = "Customize theme and display settings",
                        onClick = { navController.navigate("settings/appearance") }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Player & Audio
                    SettingItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.play),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        title = stringResource(R.string.player_and_audio),
                        subtitle = "Audio quality and playback settings",
                        onClick = { navController.navigate("settings/player") }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Content
                    SettingItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.language),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        title = stringResource(R.string.content),
                        subtitle = "Language and content preferences",
                        onClick = { navController.navigate("settings/content") }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Privacy
                    SettingItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.security),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        title = stringResource(R.string.privacy),
                        subtitle = "Privacy and security settings",
                        onClick = { navController.navigate("settings/privacy") }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Storage
                    SettingItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.storage),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        title = stringResource(R.string.storage),
                        subtitle = "Manage storage and downloads",
                        onClick = { navController.navigate("settings/storage") }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Backup & Restore
                    SettingItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.restore),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        title = stringResource(R.string.backup_restore),
                        subtitle = "Backup and restore your data",
                        onClick = { navController.navigate("settings/backup_restore") }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // About
                    SettingItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        title = stringResource(R.string.about),
                        subtitle = "App information and legal",
                        onClick = { navController.navigate("settings/about") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SettingItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor
                )
            }
        }

        trailing?.invoke()
    }
}