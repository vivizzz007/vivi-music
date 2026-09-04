package com.music.vivi.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.music.innertube.utils.parseCookieString
import com.music.vivi.BuildConfig
import com.music.vivi.R
import com.music.vivi.constants.AccountEmailKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.HasStarredRepoKey
import com.music.vivi.github.GitHubViewModel
import com.music.vivi.utils.rememberPreference
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.music.vivi.utils.listItemShape
import com.music.vivi.viewmodels.HomeViewModel
import com.music.vivi.vivimusic.updater.getUpdateAvailableState
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onNavigate: (String) -> Unit,
    homeViewModel: HomeViewModel,
    gitHubViewModel: GitHubViewModel = hiltViewModel(),
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    
    var isUpdateAvailable by remember { mutableStateOf(getUpdateAvailableState(context)) }
    val (innerTubeCookie, _) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        innerTubeCookie.isNotEmpty() && "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val isStarred by gitHubViewModel.isStarred.collectAsState()
    LaunchedEffect(expanded) {
        if (expanded) {
            isUpdateAvailable = getUpdateAvailableState(context)
            gitHubViewModel.checkStarStatus(context)
        }
    }

    val (accountEmail, _) = rememberPreference(AccountEmailKey, "")
    val accountName by homeViewModel.accountName.collectAsState()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()
    
    val itemContainerColor = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(x = 0.dp, y = (-48).dp),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .width(280.dp) // Ensures consistent wide size inspired by Chrome menus
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // --- 0. Chrome-like Top Icons Row with Circular Backgrounds ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconButtonModifier = Modifier.size(40.dp)
            val iconButtonColors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )

            IconButton(
                onClick = { 
                    gitHubViewModel.toggleStar(context) {
                        val clientId = com.music.vivi.BuildConfig.GITHUB_CLIENT_ID
                        uriHandler.openUri("https://github.com/login/oauth/authorize?client_id=${clientId}&scope=public_repo")
                    }
                }, 
                modifier = iconButtonModifier,
                colors = iconButtonColors,
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    imageVector = if (isStarred) Icons.Rounded.Star else Icons.Rounded.StarBorder, 
                    contentDescription = "Star Repo",
                    tint = if (isStarred) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { 
                    onDismissRequest()
                    onNavigate("settings/update")
                }, 
                modifier = iconButtonModifier,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isUpdateAvailable) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    imageVector = if (isUpdateAvailable) Icons.Rounded.NewReleases else Icons.Rounded.SystemUpdate, 
                    contentDescription = "Update",
                    tint = if (isUpdateAvailable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { 
                    onDismissRequest()
                    onNavigate("history")
                }, 
                modifier = iconButtonModifier,
                colors = iconButtonColors,
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    imageVector = Icons.Rounded.History, 
                    contentDescription = "History",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { 
                    onDismissRequest()
                    val targetApkName = if (BuildConfig.FLAVOR.contains("foss", ignoreCase = true)) "izzydroid-universal-foss-release.apk" else "vivi.apk"
                    val shareUrl = "https://github.com/vivizzz007/vivi-music/releases/latest/download/$targetApkName"
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }, 
                modifier = iconButtonModifier,
                colors = iconButtonColors,
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.share_newicons), 
                    contentDescription = "Share App",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))

        // --- 1. Real Logic Menu Sections ---
        Surface(
            shape = listItemShape(0, 5, 16.dp),
            color = itemContainerColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 1.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = if (isLoggedIn) {
                            accountName.ifEmpty { accountEmail.ifEmpty { "Account" } }
                        } else {
                            stringResource(R.string.account)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    if (isLoggedIn && !accountImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = accountImageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.account),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                onClick = {
                    onDismissRequest()
                    if (isLoggedIn) {
                        onNavigate("settings/account")
                    } else {
                        onNavigate("login")
                    }
                },
                modifier = Modifier.height(48.dp)
            )
        }
        DropdownMenuIconItem(
            title = stringResource(R.string.integrations),
            icon = R.drawable.extension,
            shape = listItemShape(1, 5, 16.dp),
            containerColor = itemContainerColor,
            onClick = {
                onDismissRequest()
                onNavigate("settings/integrations")
            }
        )
        DropdownMenuIconItem(
            title = stringResource(R.string.history),
            icon = R.drawable.music_history,
            shape = listItemShape(2, 5, 16.dp),
            containerColor = itemContainerColor,
            onClick = {
                onDismissRequest()
                onNavigate("history")
            }
        )
        DropdownMenuIconItem(
            title = stringResource(R.string.listen_together),
            icon = R.drawable.group_outlined,
            shape = listItemShape(3, 5, 16.dp),
            containerColor = itemContainerColor,
            onClick = {
                onDismissRequest()
                onNavigate("listen_together_from_topbar")
            }
        )
        DropdownMenuIconItem(
            title = stringResource(R.string.stats),
            icon = R.drawable.stats,
            shape = listItemShape(4, 5, 16.dp),
            containerColor = itemContainerColor,
            onClick = {
                onDismissRequest()
                onNavigate("stats")
            }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))

        DropdownMenuIconItem(
            title = stringResource(R.string.settings),
            icon = R.drawable.settings,
            shape = listItemShape(0, 2, 16.dp),
            containerColor = itemContainerColor,
            onClick = {
                onDismissRequest()
                onNavigate("settings")
            }
        )
        Surface(
            shape = listItemShape(1, 2, 16.dp),
            color = itemContainerColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 1.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.about),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                leadingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                },
                onClick = {
                    onDismissRequest()
                    onNavigate("settings/about")
                },
                modifier = Modifier.height(48.dp)
            )
        }
    }
}

@Composable
fun DropdownMenuIconItem(
    title: String,
    icon: Int,
    trailingText: String? = null,
    shape: Shape = RectangleShape,
    containerColor: Color = Color.Transparent,
    onClick: () -> Unit = {}
) {
    Surface(
        shape = shape,
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 1.dp)
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingIcon = trailingText?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            onClick = onClick,
            modifier = Modifier.height(48.dp)
        )
    }
}
