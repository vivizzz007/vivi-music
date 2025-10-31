
@file:OptIn(ExperimentalMaterial3Api::class)

package com.music.vivi.update.account


import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.Context
import com.music.innertube.utils.parseCookieString
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.AccountChannelHandleKey
import com.music.vivi.constants.AccountEmailKey
import com.music.vivi.constants.AccountNameKey
import com.music.vivi.constants.DataSyncIdKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.UseLoginForBrowse
import com.music.vivi.constants.VisitorDataKey
import com.music.vivi.constants.YtmSyncKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.InfoLabel
import com.music.vivi.ui.component.TextFieldDialog
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.AccountSettingsViewModel
import com.music.vivi.viewmodels.AccountViewModel
import com.music.vivi.viewmodels.HomeViewModel
import kotlin.text.contains

@OptIn(ExperimentalMaterial3Api::class)
@Composable
//fun AccountView(
//    navController: NavController,
//    scrollBehavior: TopAppBarScrollBehavior,
//) {
//    val context = LocalContext.current
//
//    // Get account information from preferences
//    val (accountNamePref, setAccountNamePref) = rememberPreference(AccountNameKey, "")
//    val (accountEmail, setAccountEmail) = rememberPreference(AccountEmailKey, "")
//    val (innerTubeCookie, setInnerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
//
//    // Get account data from ViewModel
//    val homeViewModel = hiltViewModel<HomeViewModel>()
//    val accountName by homeViewModel.accountName.collectAsState()
//    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()
//
//    // Check if user is logged in
//    val isLoggedIn = remember(innerTubeCookie) {
//        "SAPISID" in parseCookieString(innerTubeCookie)
//    }
//
//    // Determine what to display
//    val displayName = when {
//        isLoggedIn && accountName.isNotBlank() -> accountName
//        isLoggedIn && accountNamePref.isNotBlank() -> accountNamePref
//        else -> "Guest User"
//    }
//
//    val displayEmail = if (isLoggedIn && accountEmail.isNotBlank()) {
//        accountEmail
//    } else {
//        "Not signed in"
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text(stringResource(R.string.account)) },
//                navigationIcon = {
//                    IconButton(
//                        onClick = navController::navigateUp,
//                        onLongClick = navController::backToMain
//                    ) {
//                        Icon(
//                            painter = painterResource(R.drawable.arrow_back),
//                            contentDescription = null
//                        )
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.background
//                ),
//                scrollBehavior = scrollBehavior
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .background(MaterialTheme.colorScheme.background)
//                .verticalScroll(rememberScrollState())
//                .padding(horizontal = 16.dp)
//        ) {
//            Spacer(modifier = Modifier.height(24.dp))
//
//            // Profile Section Card
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(20.dp),
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.surfaceContainer
//                ),
//                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//            ) {
//                Column(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(24.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    // Profile Image
//                    if (isLoggedIn && accountImageUrl != null) {
//                        AsyncImage(
//                            model = accountImageUrl,
//                            contentDescription = stringResource(R.string.profile_image),
//                            contentScale = ContentScale.Crop,
//                            modifier = Modifier
//                                .size(80.dp)
//                                .clip(CircleShape)
//                        )
//                    } else {
//                        Box(
//                            modifier = Modifier
//                                .size(80.dp)
//                                .clip(CircleShape)
//                                .background(MaterialTheme.colorScheme.primaryContainer),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Icon(
//                                painter = painterResource(R.drawable.person),
//                                contentDescription = null,
//                                modifier = Modifier.size(40.dp),
//                                tint = MaterialTheme.colorScheme.onPrimaryContainer
//                            )
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    // Account Name
//                    Text(
//                        text = displayName,
//                        style = MaterialTheme.typography.titleLarge,
//                        fontWeight = FontWeight.Bold,
//                        color = MaterialTheme.colorScheme.onSurface
//                    )
//
//                    Spacer(modifier = Modifier.height(4.dp))
//
//                    // Account Email
//                    Text(
//                        text = displayEmail,
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            // Account Actions Card
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(20.dp),
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.surfaceContainer
//                ),
//                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//            ) {
//                Column(modifier = Modifier.padding(vertical = 8.dp)) {
//
//                    if (isLoggedIn) {
//                        // My Library
//                        ModernInfoItem(
//                            icon = {
//                                Icon(
//                                    painter = painterResource(R.drawable.library_music),
//                                    contentDescription = null,
//                                    modifier = Modifier.size(22.dp),
//                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//                            },
//                            title = "My Library",
//                            subtitle = "View your playlists, albums and artists",
//                            onClick = { navController.navigate("account") },
//                            showArrow = true,
//                            iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
//                        )
//
//                        HorizontalDivider(
//                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
//                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
//                        )
//
//                        // Sign Out
//                        ModernInfoItem(
//                            icon = {
//                                Icon(
//                                    painter = painterResource(R.drawable.logout),
//                                    contentDescription = null,
//                                    modifier = Modifier.size(22.dp),
//                                    tint = MaterialTheme.colorScheme.error
//                                )
//                            },
//                            title = "Sign Out",
//                            subtitle = "Sign out from your account",
//                            titleColor = MaterialTheme.colorScheme.error,
//                            onClick = {
//                                // Clear account preferences using the setter functions
//                                setAccountNamePref("")
//                                setAccountEmail("")
//                                setInnerTubeCookie("")
//
//                                // Navigate to login screen and clear back stack
//                                navController.navigate("login") {
//                                    popUpTo(0) { inclusive = true }
//                                }
//                            },
//                            showArrow = false,
//                            iconBackgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
//                        )
//                    } else {
//                        // Sign In
//                        ModernInfoItem(
//                            icon = {
//                                Icon(
//                                    painter = painterResource(R.drawable.login),
//                                    contentDescription = null,
//                                    modifier = Modifier.size(22.dp),
//                                    tint = MaterialTheme.colorScheme.primary
//                                )
//                            },
//                            title = "Sign In",
//                            subtitle = "Sign in to access your account",
//                            titleColor = MaterialTheme.colorScheme.primary,
//                            onClick = {
//                                // Handle sign in
//                                navController.navigate("login")
//                            },
//                            showArrow = true,
//                            iconBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
//                            arrowColor = MaterialTheme.colorScheme.primary
//                        )
//                    }
//                }
//            }
//
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(20.dp),
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.surfaceContainer
//                ),
//                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//            ) {
//                Column(modifier = Modifier.padding(vertical = 8.dp)) {
//                    ModernInfoItem(
//                        icon = {
//                            Icon(
//                                painter = painterResource(R.drawable.integration),
//                                contentDescription = null,
//                                modifier = Modifier.size(22.dp),
//                                tint = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        },
//                        title = "Integrations",
//                        subtitle = "Connect with other services",
//                        onClick = {
//                            navController.navigate("settings/integrations")
//                        },
//                        showArrow = true,
//                        iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(100.dp))
//        }
//    }
//}
fun AccountView(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehaviorLocal = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val context = LocalContext.current

    // Preferences
    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")

    val isLoggedIn = remember(innerTubeCookie) {
        innerTubeCookie.isNotEmpty() && "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
    val accountName by homeViewModel.accountName.collectAsState()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()

    var showToken by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(40.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Manage your account and preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Spacer(Modifier.height(24.dp))

            // Account Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(
                        color = if (isLoggedIn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable {
                        if (isLoggedIn) {
                            navController.navigate("account")
                        } else {
                            navController.navigate("login")
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLoggedIn) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoggedIn && accountImageUrl != null) {
                            AsyncImage(
                                model = accountImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    if (isLoggedIn) R.drawable.account
                                    else R.drawable.login
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = if (isLoggedIn) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLoggedIn) "Signed in" else "Not signed in",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isLoggedIn) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp
                        )

                        Text(
                            text = if (isLoggedIn) accountName else "Tap to sign in",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isLoggedIn) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (isLoggedIn) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Account Settings Card
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
                                painter = painterResource(R.drawable.token),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.surfaceTint
                            )
                        },
                        title = when {
                            !isLoggedIn -> "Advanced login"
                            showToken -> "Token shown"
                            else -> "Token hidden"
                        },
                        subtitle = if (isLoggedIn) "Tap to manage token" else "Sign in with cookies",
                        onClick = {
                            if (!isLoggedIn) showTokenEditor = true
                            else if (!showToken) showToken = true
                            else showTokenEditor = true
                        },
                        showArrow = true,
                        showSettingsIcon = true
                    )

                    if (isLoggedIn) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        ModernInfoItem(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.add_circle),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.surfaceTint
                                )
                            },
                            title = "More content",
                            subtitle = if (useLoginForBrowse) "Enabled" else "Disabled",
                            onClick = {
                                YouTube.useLoginForBrowse = !useLoginForBrowse
                                onUseLoginForBrowseChange(!useLoginForBrowse)
                            },
                            trailingContent = {
                                ModernSwitch(
                                    checked = useLoginForBrowse,
                                    onCheckedChange = {
                                        YouTube.useLoginForBrowse = it
                                        onUseLoginForBrowseChange(it)
                                    }
                                )
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        ModernInfoItem(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.cached),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.surfaceTint
                                )
                            },
                            title = "YouTube sync",
                            subtitle = if (ytmSync) "Enabled" else "Disabled",
                            onClick = {
                                onYtmSyncChange(!ytmSync)
                            },
                            trailingContent = {
                                ModernSwitch(
                                    checked = ytmSync,
                                    onCheckedChange = onYtmSyncChange
                                )
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        ModernInfoItem(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.logout),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            title = "Sign out",
                            subtitle = "Log out from your account",
                            onClick = {
                                accountSettingsViewModel.logoutAndClearSyncedContent(
                                    context,
                                    onInnerTubeCookieChange
                                )
                            },
                            showArrow = true,
                            showSettingsIcon = true,
                            iconBackgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Integrations Card
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
                                painter = painterResource(R.drawable.integration),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.surfaceTint
                            )
                        },
                        title = "Integrations",
                        subtitle = "Connect with other services",
                        onClick = {
                            navController.navigate("settings/integrations")
                        },
                        showArrow = true,
                        showSettingsIcon = true
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
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