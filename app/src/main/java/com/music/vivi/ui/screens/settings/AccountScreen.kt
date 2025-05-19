package com.music.vivi.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.innertube.YouTube
import com.music.innertube.utils.parseCookieString
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.AccountChannelHandleKey
import com.music.vivi.constants.AccountEmailKey
import com.music.vivi.constants.AccountNameKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.UseLoginForBrowse
import com.music.vivi.constants.YtmSyncKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.InfoLabel
import com.music.vivi.ui.component.PreferenceEntry
import com.music.vivi.ui.component.PreferenceGroupTitle
import com.music.vivi.ui.component.SwitchPreference
import com.music.vivi.ui.component.TextFieldDialog
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val accountName by rememberPreference(AccountNameKey, "")
    val accountEmail by rememberPreference(AccountEmailKey, "")
    val accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, defaultValue = true)
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(key = UseLoginForBrowse, defaultValue = false)

    var showToken: Boolean by remember {
        mutableStateOf(false)
    }
    var showTokenEditor by remember {
        mutableStateOf(false)
    }
    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(10.dp))

        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            visible = true
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {

            Image(
                painter = painterResource(id = R.drawable.account_box),
                contentDescription = stringResource(R.string.playerandaudio),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }


        PreferenceGroupTitle(
            title = stringResource(R.string.account)
        )
        PreferenceEntry(
            title = { Text(if (isLoggedIn) accountName else stringResource(R.string.login)) },
            description = if (isLoggedIn) {
                accountEmail.takeIf { it.isNotEmpty() }
                    ?: accountChannelHandle.takeIf { it.isNotEmpty() }
            } else {
                null
            },
            icon = { Icon(painterResource(R.drawable.profile_icon), null) },
            trailingContent = {
                if (isLoggedIn) {
                    OutlinedButton(onClick = {
                        onInnerTubeCookieChange("")
                    },
                    ) {
                        Text(stringResource(R.string.logout))
                    }
                }
            },
            onClick = { if (!isLoggedIn) navController.navigate("login") }
        )
        if (showTokenEditor) {
            TextFieldDialog(
                modifier = Modifier,
                initialTextFieldValue = TextFieldValue(innerTubeCookie),
                onDone = { onInnerTubeCookieChange(it) },
                onDismiss = { showTokenEditor = false },
                singleLine = false,
                maxLines = 20,
                isInputValid = {
                    it.isNotEmpty() &&
                            try {
                                "SAPISID" in parseCookieString(it)
                                true
                            } catch (e: Exception) {
                                false
                            }
                },
                extraContent = {
                    InfoLabel(text = stringResource(R.string.token_adv_login_description))
                }
            )
        }
        PreferenceEntry(
            title = {
                if (showToken) {
                    Text(stringResource(R.string.token_shown))
                    Text(
                        text = if (isLoggedIn) innerTubeCookie else stringResource(R.string.not_logged_in),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Light,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
                else {
                    Text(stringResource(R.string.token_hidden))
                }
            },
            icon = { Icon(painterResource(R.drawable.token_icon), null) },
            onClick = {
                if (!showToken) {
                    showToken = true
                } else {
                    showTokenEditor = true
                }
            },
        )
        if (isLoggedIn) {
            SwitchPreference(
                title = { Text(stringResource(R.string.ytm_sync)) },
                icon = { Icon(painterResource(R.drawable.sync_icon), null) },
                checked = ytmSync,
                onCheckedChange = onYtmSyncChange,
                isEnabled = true
            )
        }
        Spacer(Modifier.height(10.dp))
        PreferenceGroupTitle(
            title = stringResource(R.string.title_spotify)
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.import_from_spotify)) },
            description = null,
            icon = { Icon(painterResource(R.drawable.spotify_icon), null) },
            onClick = {
                navController.navigate("settings/import_from_spotify/ImportFromSpotify")
            }
        )
        Spacer(Modifier.height(10.dp))
        PreferenceGroupTitle(
            title = stringResource(R.string.title_discord)
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.discord_integration)) },
            icon = { Icon(painterResource(R.drawable.discord_icon), null) },
            onClick = { navController.navigate("settings/discord") }
        )
        PreferenceGroupTitle(
            title = stringResource(R.string.misc)
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.use_login_for_browse)) },
            description = stringResource(R.string.use_login_for_browse_desc),
            icon = { Icon(painterResource(R.drawable.profile_icon), null) },
            checked = useLoginForBrowse,
            onCheckedChange = {
                YouTube.useLoginForBrowse = it
                onUseLoginForBrowseChange(it)
            }
        )
    }
    TopAppBar(
        title = { Text(stringResource(R.string.account)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.back_icon),
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}