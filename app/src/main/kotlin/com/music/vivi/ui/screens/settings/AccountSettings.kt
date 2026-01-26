package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.utils.parseCookieString
import com.music.vivi.R
import com.music.vivi.constants.AccountChannelHandleKey
import com.music.vivi.constants.AccountEmailKey
import com.music.vivi.constants.AccountNameKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.DataSyncIdKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.SettingsShapeColorTertiaryKey
import com.music.vivi.constants.UseLoginForBrowse
import com.music.vivi.constants.VisitorDataKey
import com.music.vivi.constants.YtmSyncKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.InfoLabel
import com.music.vivi.ui.component.TextFieldDialog
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.Material3ExpressiveSettingsGroup
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.AccountSettingsViewModel
import com.music.vivi.viewmodels.HomeViewModel

/**
 * Screen for managing user account settings, including login/logout.
 * Also handles sync settings and advanced token management.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccountSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    onBack: () -> Unit = { navController.popBackStack() },
) {
    val context = LocalContext.current

// **CHANGE: Initialize ViewModels first so they can be used below**
    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()

    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")

    // Now homeViewModel is known and can be used here
    val isLoggedIn by homeViewModel.isLoggedIn.collectAsState()
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val accountName by homeViewModel.accountName.collectAsState()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()

    var showToken by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }

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
            val scrollState = rememberLazyListState()

            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 32.dp
                )
            ) {
                // Header
                item {
                    Spacer(modifier = Modifier.height(25.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.account_title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.manage_account_preferences),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Account Section
                item {
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = listOf {
                            ModernInfoItem(
                                icon = {
                                    if (isLoggedIn && accountImageUrl != null) {
                                        AsyncImage(
                                            model = accountImageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(
                                                if (isLoggedIn) R.drawable.person else R.drawable.login
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                title = if (isLoggedIn) accountName else stringResource(R.string.login),
                                subtitle = if (isLoggedIn) accountEmail else stringResource(R.string.tap_to_sign_in),
                                onClick = {
                                    if (isLoggedIn) {
                                        navController.navigate("account")
                                    } else {
                                        navController.navigate("login")
                                    }
                                },
                                trailingContent = {
                                    if (isLoggedIn) {
                                        androidx.compose.material3.Surface(
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                            contentColor = MaterialTheme.colorScheme.error,
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                                .clickable(
                                                    indication = null,
                                                    interactionSource = remember {
                                                        androidx.compose.foundation.interaction.MutableInteractionSource()
                                                    }
                                                ) {
                                                    accountSettingsViewModel.logoutAndClearSyncedContent(
                                                        context,
                                                        onInnerTubeCookieChange
                                                    )
                                                }
                                        ) {
                                            Text(
                                                text = stringResource(R.string.action_logout).uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 1.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                },
                                showArrow = !isLoggedIn,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor
                            )
                        }
                    )
                }

                // Synchronization Section
                if (isLoggedIn) {
                    item {
                        Text(
                            text = stringResource(R.string.account_status).uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                        )
                    }

                    item {
                        Material3ExpressiveSettingsGroup(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            items = listOf(
                                {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = {
                                                    Icon(
                                                        painterResource(R.drawable.add_circle),
                                                        null,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                },
                                                title = stringResource(R.string.more_content),
                                                subtitle = stringResource(R.string.ytm_sync),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(
                                            checked = useLoginForBrowse,
                                            onCheckedChange = {
                                                YouTube.useLoginForBrowse = it
                                                onUseLoginForBrowseChange(it)
                                            },
                                            modifier = Modifier.padding(end = 20.dp)
                                        )
                                    }
                                },
                                {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernInfoItem(
                                                icon = {
                                                    Icon(
                                                        painterResource(R.drawable.cached),
                                                        null,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                },
                                                title = stringResource(R.string.yt_sync),
                                                subtitle = stringResource(R.string.ytm_sync),
                                                iconBackgroundColor = iconBgColor,
                                                iconContentColor = iconStyleColor
                                            )
                                        }
                                        ModernSwitch(
                                            checked = ytmSync,
                                            onCheckedChange = onYtmSyncChange,
                                            modifier = Modifier.padding(end = 20.dp)
                                        )
                                    }
                                }
                            )
                        )
                    }
                }

                // Advanced Section
                item {
                    Text(
                        text = stringResource(R.string.general).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = listOf {
                            ModernInfoItem(
                                icon = {
                                    Icon(painterResource(R.drawable.token), null, modifier = Modifier.size(22.dp))
                                },
                                title = when {
                                    !isLoggedIn -> stringResource(R.string.login_using_token)
                                    showToken -> stringResource(R.string.token_shown)
                                    else -> stringResource(R.string.token_hidden)
                                },
                                subtitle = stringResource(R.string.token_adv_login_description),
                                onClick = {
                                    if (!isLoggedIn) {
                                        showTokenEditor = true
                                    } else if (!showToken) {
                                        showToken = true
                                    } else {
                                        showTokenEditor = true
                                    }
                                },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor
                            )
                        }
                    )
                }

                // Integrations Section
                item {
                    Text(
                        text = stringResource(R.string.integrations).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    Material3ExpressiveSettingsGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        items = listOf {
                            ModernInfoItem(
                                icon = {
                                    Icon(painterResource(R.drawable.integration), null, modifier = Modifier.size(22.dp))
                                },
                                title = stringResource(R.string.integrations),
                                subtitle = stringResource(R.string.integrations_subtitle),
                                onClick = {
                                    navController.navigate("settings/integrations")
                                },
                                showArrow = true,
                                iconBackgroundColor = iconBgColor,
                                iconContentColor = iconStyleColor
                            )
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(if (isLoggedIn) 100.dp else 40.dp))
                }
            }

            // Token Editor Dialog
            if (showTokenEditor) {
                val text = """
                    ***INNERTUBE COOKIE*** =$innerTubeCookie
                    ***VISITOR DATA*** =$visitorData
                    ***DATASYNC ID*** =$dataSyncId
                    ***ACCOUNT NAME*** =$accountNamePref
                    ***ACCOUNT EMAIL*** =$accountEmail
                    ***ACCOUNT CHANNEL HANDLE*** =$accountChannelHandle
                """.trimIndent()

                TextFieldDialog(
                    initialTextFieldValue = TextFieldValue(text),
                    onDone = { data ->
                        data.split("\n").forEach {
                            when {
                                it.startsWith(
                                    "***INNERTUBE COOKIE*** ="
                                ) -> onInnerTubeCookieChange(it.substringAfter("="))
                                it.startsWith("***VISITOR DATA*** =") -> onVisitorDataChange(it.substringAfter("="))
                                it.startsWith("***DATASYNC ID*** =") -> onDataSyncIdChange(it.substringAfter("="))
                                it.startsWith("***ACCOUNT NAME*** =") -> onAccountNameChange(it.substringAfter("="))
                                it.startsWith("***ACCOUNT EMAIL*** =") -> onAccountEmailChange(it.substringAfter("="))
                                it.startsWith(
                                    "***ACCOUNT CHANNEL HANDLE*** ="
                                ) -> onAccountChannelHandleChange(it.substringAfter("="))
                            }
                        }
                    },
                    onDismiss = { showTokenEditor = false },
                    singleLine = false,
                    maxLines = 20,
                    isInputValid = {
                        it.isNotEmpty() && "SAPISID" in parseCookieString(it)
                    },
                    extraContent = {
                        InfoLabel(text = stringResource(R.string.token_adv_login_description))
                    }
                )
            }
        }
    }
}
