package com.music.vivi.ui.screens.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NoCell
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.maloy.muzza.LocalPlayerAwareWindowInsets
import com.maloy.muzza.NotificationPermissionPreference
import com.maloy.muzza.R
import com.maloy.muzza.constants.KeepAliveKey
import com.maloy.muzza.playback.KeepAlive
import com.maloy.muzza.ui.component.IconButton
import com.maloy.muzza.ui.component.PreferenceGroupTitle
import com.maloy.muzza.ui.component.SwitchPreference
import com.maloy.muzza.ui.utils.backToMain
import com.maloy.muzza.utils.rememberPreference
import com.maloy.muzza.utils.reportException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val (keepAlive, onKeepAliveChange) = rememberPreference(key = KeepAliveKey, defaultValue = false)

    fun toggleKeepAlive(newValue: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            onKeepAliveChange(false)
            Toast.makeText(
                context,
                "Notification permission is required",
                Toast.LENGTH_SHORT
            ).show()
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf( Manifest.permission.POST_NOTIFICATIONS), PackageManager.PERMISSION_GRANTED
            )
            return
        }
        if (keepAlive != newValue) {
            onKeepAliveChange(newValue)
            if (newValue) {
                try {
                    context.startService(Intent(context, KeepAlive::class.java))
                } catch (e: Exception) {
                    reportException(e)
                }
            } else {
                try {
                    context.stopService(Intent(context, KeepAlive::class.java))
                } catch (e: Exception) {
                    reportException(e)
                }
            }
        }
    }
    LaunchedEffect(keepAlive) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            onKeepAliveChange(false)
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {

        PreferenceGroupTitle(
            title = stringResource(R.string.notifications)
        )

        NotificationPermissionPreference()

        SwitchPreference(
            title = { Text(stringResource(R.string.keep_alive_title)) },
            description = stringResource(R.string.keep_alive_description),
            icon = { Icon(Icons.Rounded.NoCell, null) },
            checked = keepAlive,
            onCheckedChange = { toggleKeepAlive(it) }
        )
    }
    TopAppBar(
        title = { Text(stringResource(R.string.notifications_settings)) },
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