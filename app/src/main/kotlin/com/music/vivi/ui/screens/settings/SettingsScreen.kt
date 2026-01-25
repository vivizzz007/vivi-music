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
import androidx.compose.material.icons.filled.BatteryFull
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
import com.music.vivi.update.settingstyle.Material3ExpressiveSettingsGroup
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
    updateViewModel: UpdateViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    onNavigate: ((String) -> Unit)? = null,
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
                        onClick = { onBack?.invoke() ?: navController.navigateUp() },
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
            SettingsListContent(
                updateStatus = updateStatus,
                currentVersion = currentVersion,
                accountName = if (isLoggedIn && accountName.isNotBlank()) accountName else if (isLoggedIn && accountNamePref.isNotBlank()) accountNamePref else stringResource(R.string.account),
                accountEmail = accountEmail,
                accountImageUrl = accountImageUrl,
                isLoggedIn = isLoggedIn,
                iconBgColor = iconBgColor,
                iconStyleColor = iconStyleColor,
                onNavigate = { route -> onNavigate?.invoke(route) ?: navController.navigate(route) }
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}