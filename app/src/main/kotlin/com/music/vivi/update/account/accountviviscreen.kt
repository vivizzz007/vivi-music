
@file:OptIn(ExperimentalMaterial3Api::class)

package com.music.vivi.update.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.utils.parseCookieString
import com.music.vivi.BuildConfig
import com.music.vivi.R
import com.music.vivi.constants.AccountChannelHandleKey
import com.music.vivi.constants.AccountEmailKey
import com.music.vivi.constants.AccountNameKey
import com.music.vivi.constants.DataSyncIdKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.UseLoginForBrowse
import com.music.vivi.constants.VisitorDataKey
import com.music.vivi.constants.YtmSyncKey
import com.music.vivi.ui.component.InfoLabel
import com.music.vivi.ui.component.TextFieldDialog
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.HomeViewModel
import com.music.vivi.viewmodels.AccountSettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountViewScreen(
    navController: NavController,
    onBack: () -> Unit,
    latestVersionName: String
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // Preferences
    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    var showTokenEditor by remember { mutableStateOf(false) }

    // ViewModels
    val homeViewModel = hiltViewModel<HomeViewModel>()
    val accountSettingsViewModel = hiltViewModel<AccountSettingsViewModel>()

    // State
    val accountName by homeViewModel.accountName.collectAsState()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()

    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
//                    Text(
//                        text = stringResource(R.string.account),
//                        style = MaterialTheme.typography.headlineSmall,
//                        fontWeight = FontWeight.Normal
//                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
        )

        {
            Spacer(modifier = Modifier.height(8.dp))

            // Account Profile Card
            AccountProfileCard(
                isLoggedIn = isLoggedIn,
                accountName = accountName,
                accountEmail = accountEmail,
                accountChannelHandle = accountChannelHandle,
                accountImageUrl = accountImageUrl,
                showTokenEditor = showTokenEditor,
                innerTubeCookie = innerTubeCookie,
                visitorData = visitorData,
                dataSyncId = dataSyncId,
                accountNamePref = accountNamePref,
                onShowTokenEditor = { showTokenEditor = it },
                onInnerTubeCookieChange = onInnerTubeCookieChange,
                onVisitorDataChange = onVisitorDataChange,
                onDataSyncIdChange = onDataSyncIdChange,
                onAccountNameChange = onAccountNameChange,
                onAccountEmailChange = onAccountEmailChange,
                onAccountChannelHandleChange = onAccountChannelHandleChange,
                onLoginClick = {
                    navController.navigate("login")
                },
                onLogoutClick = {
                    accountSettingsViewModel.logoutAndClearSyncedContent(context, onInnerTubeCookieChange)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Account Settings Card (only when logged in)
            if (isLoggedIn) {
                AccountSettingsCard(
                    useLoginForBrowse = useLoginForBrowse,
                    onUseLoginForBrowseChange = onUseLoginForBrowseChange,
                    ytmSync = ytmSync,
                    onYtmSyncChange = onYtmSyncChange
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Integration Card
            IntegrationCard(
                navController = navController,
                isLoggedIn = isLoggedIn,
                showTokenEditor = showTokenEditor,
                innerTubeCookie = innerTubeCookie,
                visitorData = visitorData,
                dataSyncId = dataSyncId,
                accountNamePref = accountNamePref,
                accountEmail = accountEmail,
                accountChannelHandle = accountChannelHandle,
                onShowTokenEditor = { showTokenEditor = it },
                onInnerTubeCookieChange = onInnerTubeCookieChange,
                onVisitorDataChange = onVisitorDataChange,
                onDataSyncIdChange = onDataSyncIdChange,
                onAccountNameChange = onAccountNameChange,
                onAccountEmailChange = onAccountEmailChange,
                onAccountChannelHandleChange = onAccountChannelHandleChange
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AccountProfileCard(
    isLoggedIn: Boolean,
    accountName: String,
    accountEmail: String,
    accountChannelHandle: String,
    accountImageUrl: String?,
    showTokenEditor: Boolean,
    innerTubeCookie: String,
    visitorData: String,
    dataSyncId: String,
    accountNamePref: String,
    onShowTokenEditor: (Boolean) -> Unit,
    onInnerTubeCookieChange: (String) -> Unit,
    onVisitorDataChange: (String) -> Unit,
    onDataSyncIdChange: (String) -> Unit,
    onAccountNameChange: (String) -> Unit,
    onAccountEmailChange: (String) -> Unit,
    onAccountChannelHandleChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(30.dp))

    Text(
        text = "Google Account",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
    )

    Spacer(modifier = Modifier.height(20.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        )


        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Profile Image or Login Icon
                if (isLoggedIn && accountImageUrl != null) {
                    AsyncImage(
                        model = accountImageUrl,
                        contentDescription = stringResource(R.string.profile_image),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isLoggedIn) R.drawable.person else R.drawable.login
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Account Info
                Column(modifier = Modifier.weight(1f)) {
                    if (isLoggedIn) {
                        Text(
                            text = accountName.ifBlank { stringResource(R.string.signed_in) },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (accountEmail.isNotBlank()) {
                            Text(
                                text = accountEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (accountChannelHandle.isNotBlank()) {
                            Text(
                                text = "@$accountChannelHandle",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.not_signed_in),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.sign_in_to_access_features),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Login/Token/Logout Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoggedIn) {
                    OutlinedButton(
                        onClick = onLogoutClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.logout),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_logout))
                    }
                } else {
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.login),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.sign_in))
                    }
                }
            }
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
                        it.startsWith("***INNERTUBE COOKIE*** =") -> onInnerTubeCookieChange(it.substringAfter("="))
                        it.startsWith("***VISITOR DATA*** =") -> onVisitorDataChange(it.substringAfter("="))
                        it.startsWith("***DATASYNC ID*** =") -> onDataSyncIdChange(it.substringAfter("="))
                        it.startsWith("***ACCOUNT NAME*** =") -> onAccountNameChange(it.substringAfter("="))
                        it.startsWith("***ACCOUNT EMAIL*** =") -> onAccountEmailChange(it.substringAfter("="))
                        it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> onAccountChannelHandleChange(it.substringAfter("="))
                    }
                }
            },
            onDismiss = { onShowTokenEditor(false) },
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

@Composable
private fun AccountSettingsCard(
    useLoginForBrowse: Boolean,
    onUseLoginForBrowseChange: (Boolean) -> Unit,
    ytmSync: Boolean,
    onYtmSyncChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // More Content Setting
            SettingItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.add_circle),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                title = stringResource(R.string.more_content),
                trailing = {
                    ModernSwitch(
                        checked = useLoginForBrowse,
                        onCheckedChange = {
                            YouTube.useLoginForBrowse = it
                            onUseLoginForBrowseChange(it)
                        }
                    )
                },
                onClick = {
                    val newValue = !useLoginForBrowse
                    YouTube.useLoginForBrowse = newValue
                    onUseLoginForBrowseChange(newValue)
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // YouTube Music Sync Setting
            SettingItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.cached),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                title = stringResource(R.string.yt_sync),
                trailing = {
                    ModernSwitch(
                        checked = ytmSync,
                        onCheckedChange = onYtmSyncChange
                    )
                },
                onClick = {
                    onYtmSyncChange(!ytmSync)
                }
            )
        }
    }
}

@Composable
private fun IntegrationCard(
    navController: NavController,
    isLoggedIn: Boolean,
    showTokenEditor: Boolean,
    innerTubeCookie: String,
    visitorData: String,
    dataSyncId: String,
    accountNamePref: String,
    accountEmail: String,
    accountChannelHandle: String,
    onShowTokenEditor: (Boolean) -> Unit,
    onInnerTubeCookieChange: (String) -> Unit,
    onVisitorDataChange: (String) -> Unit,
    onDataSyncIdChange: (String) -> Unit,
    onAccountNameChange: (String) -> Unit,
    onAccountEmailChange: (String) -> Unit,
    onAccountChannelHandleChange: (String) -> Unit
) {
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "Discord Account",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
    )

    Spacer(modifier = Modifier.height(10.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {

        Column {
            // Discord Integration
            SettingItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.discord),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                title = stringResource(R.string.discord_integration),
                onClick = {
                    navController.navigate("settings/discord")
                }
            )

            // Advanced Login (only when not logged in)
            if (!isLoggedIn) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                SettingItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.token),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    title = stringResource(R.string.advanced_login),
                    onClick = {
                        onShowTokenEditor(true)
                    }
                )
            }
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
                        it.startsWith("***INNERTUBE COOKIE*** =") -> onInnerTubeCookieChange(it.substringAfter("="))
                        it.startsWith("***VISITOR DATA*** =") -> onVisitorDataChange(it.substringAfter("="))
                        it.startsWith("***DATASYNC ID*** =") -> onDataSyncIdChange(it.substringAfter("="))
                        it.startsWith("***ACCOUNT NAME*** =") -> onAccountNameChange(it.substringAfter("="))
                        it.startsWith("***ACCOUNT EMAIL*** =") -> onAccountEmailChange(it.substringAfter("="))
                        it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> onAccountChannelHandleChange(it.substringAfter("="))
                    }
                }
            },
            onDismiss = { onShowTokenEditor(false) },
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

@Composable
private fun SettingItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
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
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailing != null) {
            trailing()
        }
    }
}