/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.toShape
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.vivi.BuildConfig
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.snackbar.SnackbarManager
import com.music.vivi.ui.component.ExpressiveSettingGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.component.DialogBasic
import com.music.vivi.vivimusic.updater.checkForUpdate
import kotlinx.coroutines.launch
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.safeOpenUri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: (() -> Unit)? = null,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    var showSupportDialog by remember { mutableStateOf(false) }
    val unknownString = stringResource(R.string.unknown)

    val cloverShape = MaterialShapes.Clover4Leaf.toShape()
    val cookieShape = MaterialShapes.Cookie7Sided.toShape()
    
    val installedDate = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val installTime = packageInfo.firstInstallTime
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(installTime))
        } catch (_: Exception) {
            unknownString
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        // Header
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.about),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        AppVersionTile(
            appName = stringResource(R.string.vivi_music_title),
            description = "v${BuildConfig.VERSION_NAME} • ${stringResource(if (BuildConfig.IS_NIGHTLY) R.string.build_nightly else R.string.build_stable)}",
            onGithubClick = { uriHandler.safeOpenUri(context, "https://github.com/vivizzz007/vivi-music") }
        )
        
        Spacer(modifier = Modifier.height(10.dp))

        // Developer Section
        ExpressiveSettingGroup(
            items = listOf(
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.app_developer), color = MaterialTheme.colorScheme.primary) },
                    description = { Text(stringResource(R.string.developer_name)) },
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.dev),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(cookieShape),
                            contentScale = ContentScale.Crop
                        )
                    },
                    onClick = { uriHandler.safeOpenUri(context, "https://github.com/vivizzz007") },
                    isExternalLink = true
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.favorite),
                    title = { Text(stringResource(R.string.support)) },
                    description = { Text(stringResource(R.string.support_desc)) },
                    onClick = { showSupportDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.web_link),
                    title = { Text(stringResource(R.string.website)) },
                    onClick = { uriHandler.safeOpenUri(context, "https://vivimusic.mkmdevilmi.workers.dev/") },
                    isExternalLink = true
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.telegram),
                    title = { Text(stringResource(R.string.telegram_channel)) },
                    onClick = { uriHandler.safeOpenUri(context, "https://t.me/vivimusicapp") },
                    isExternalLink = true
                )
            )
        )
//        Spacer(modifier = Modifier.height(10.dp))
//
//        // Collaborator Section
//        Material3SettingsGroup(
//            title = stringResource(R.string.collaborator_section),
//            items = listOf(
//                Material3SettingsItem(
//                    icon = painterResource(R.drawable.collab),
//                    title = { Text(stringResource(R.string.collaborator_tboyke)) },
//                    description = { Text(stringResource(R.string.collaborator_role)) },
//                    tintIcon = false,
//                    iconShape = cloverShape,
//                    onClick = { uriHandler.safeOpenUri(context, "https://github.com/T-Boyke") }
//                )
//            )
//        )


        Spacer(modifier = Modifier.height(10.dp))

        // App Information Section
        ExpressiveSettingGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.deployed_app_update),
                    title = { Text(stringResource(R.string.installed_date_title)) },
                    trailingContent = { Text(installedDate) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.info),
                    title = { Text(stringResource(R.string.version_code)) },
                    trailingContent = { Text(BuildConfig.VERSION_CODE.toString()) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.license_vivi),
                    title = { Text(stringResource(R.string.license)) },
                    onClick = { uriHandler.safeOpenUri(context, "https://github.com/vivizzz007/vivi-music/blob/main/LICENSE") },
                    isExternalLink = true
                ),
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
    }

    if (showSupportDialog) {
        SupportDialog(
            onDismiss = { showSupportDialog = false },
            onPayPal = { uriHandler.safeOpenUri(context, "https://www.paypal.me/vividhpashokan") },
            onUPI = { uriHandler.safeOpenUri(context, "upi://pay?pa=vividhpashokan@axl&pn=Vividh P Ashokan") },
            onCoffee = { uriHandler.safeOpenUri(context, "https://ko-fi.com/vividhpashokan") }
        )
    }

    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(
                onClick = { onBack?.invoke() ?: navController.navigateUp() },
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}

@Composable
private fun AppVersionTile(
    appName: String,
    description: String,
    onGithubClick: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        ListItem(
            leadingContent = {
                Image(
                    painter = painterResource(R.drawable.icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = containerColor
            ),
            headlineContent = {
                Text(
                    text = appName,
                    color = onContainerColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            supportingContent = {
                Text(
                    text = description,
                    color = onContainerColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            trailingContent = {
                androidx.compose.material3.IconButton(
                    onClick = onGithubClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = containerColor
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.github),
                        contentDescription = "GitHub",
                        tint = onContainerColor
                    )
                }
            }
        )
    }
}

@Composable
private fun SupportDialog(
    onDismiss: () -> Unit,
    onPayPal: () -> Unit,
    onUPI: () -> Unit,
    onCoffee: () -> Unit
) {
    DialogBasic(
        show = true,
        title = "Donate",
        dismissText = stringResource(android.R.string.cancel),
        showOnlyDismissAction = true,
        onDismiss = onDismiss
    ) {
        ExpressiveSettingGroup(
            modifier = Modifier.padding(horizontal = 14.dp),
            items = listOf(
                Material3SettingsItem(
                    leadingContent = { 
                        Icon(painterResource(R.drawable.paypal), contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    },
                    title = { Text("PayPal") },
                    description = { Text("paypal.me/vividhpashokan") },
                    onClick = { onPayPal(); onDismiss() }
                ),
                Material3SettingsItem(
                    leadingContent = { 
                        Icon(painterResource(R.drawable.currency_rupee_upi), contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    },
                    title = { Text("UPI") },
                    description = { Text("vividhpashokan@axl") },
                    onClick = { onUPI(); onDismiss() }
                ),
                Material3SettingsItem(
                    leadingContent = { 
                        Icon(painterResource(R.drawable.buymeacoffee), contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    },
                    title = { Text("Ko-fi") },
                    description = { Text("ko-fi.com/vividhpashokan") },
                    onClick = { onCoffee(); onDismiss() }
                )
            )
        )
    }
}