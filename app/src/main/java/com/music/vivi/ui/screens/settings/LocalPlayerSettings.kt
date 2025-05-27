package com.music.vivi.ui.screens.settings


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.AutoSyncLocalSongsKey
import com.music.vivi.constants.FlatSubfoldersKey
import com.music.vivi.constants.ScannerSensitivity
import com.music.vivi.constants.ScannerSensitivityKey
import com.music.vivi.constants.ScannerStrictExtKey
import com.music.vivi.ui.component.EnumListPreference
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.PreferenceGroupTitle
import com.music.vivi.ui.component.SwitchPreference
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalPlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (autoSyncLocalSongs, onAutoSyncLocalSongs) = rememberPreference(
        key = AutoSyncLocalSongsKey,
        defaultValue = true)
    val (scannerSensitivity, onScannerSensitivityChange) = rememberEnumPreference(
        key = ScannerSensitivityKey,
        defaultValue = ScannerSensitivity.LEVEL_2
    )
    val (strictExtensions, onStrictExtensionsChange) = rememberPreference(
        ScannerStrictExtKey,
        defaultValue = false
    )

    val (flatSubfolders, onFlatSubfoldersChange) = rememberPreference(
        FlatSubfoldersKey,
        defaultValue = true
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.internalplayer)) // Replace with your Lottie JSON file
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever, // Loop the animation
            modifier = Modifier
//                    .size(100.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Icon(
                Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )

            Text(
                stringResource(R.string.scanner_warning),
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(horizontal = 15.dp)
            )
        }

        PreferenceGroupTitle(
            title = stringResource(R.string.scanner_settings_title)
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.auto_scanner_title)) },
            description = stringResource(R.string.auto_scanner_description) ,
            icon = { Icon(painterResource(R.drawable.sync), null) },
            checked = autoSyncLocalSongs,
            onCheckedChange = onAutoSyncLocalSongs
        )

        // scanner sensitivity
        EnumListPreference(
            title = { Text(stringResource(R.string.scanner_sensitivity_title)) },
            icon = { Icon(Icons.Rounded.GraphicEq, null) },
            selectedValue = scannerSensitivity,
            onValueSelected = onScannerSensitivityChange,
            valueText = {
                when (it) {
                    ScannerSensitivity.LEVEL_1 -> stringResource(R.string.scanner_sensitivity_L1)
                    ScannerSensitivity.LEVEL_2 -> stringResource(R.string.scanner_sensitivity_L2)
                    ScannerSensitivity.LEVEL_3 -> stringResource(R.string.scanner_sensitivity_L3)
                }
            }
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.scanner_strict_file_name_title)) },
            description = stringResource(R.string.scanner_strict_file_name_description),
            icon = { Icon(Icons.Rounded.TextFields, null) },
            checked = strictExtensions,
            onCheckedChange = onStrictExtensionsChange
        )
        VerticalDivider(
            thickness = DividerDefaults.Thickness,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 10.dp)
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.folders_settings_title)
        )
//        SwitchPreference(
//            title = { Text(stringResource(R.string.flat_subfolders_title)) },
//            description = stringResource(R.string.flat_subfolders_description),
//            icon = { Icon(Icons.Rounded.FolderCopy, null) },
//            checked = flatSubfolders,
//            onCheckedChange = onFlatSubfoldersChange
//        )
        SwitchPreference(
            title = { Text(stringResource(R.string.flat_subfolders_title)) },
            description = stringResource(R.string.flat_subfolders_description),
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.folder_icon), // Replace with your actual drawable name
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
//                    tint = Color.White // Optional: remove or change color as needed
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            checked = flatSubfolders,
            onCheckedChange = onFlatSubfoldersChange
        )
    }
    TopAppBar(
        title = { Text(stringResource(R.string.local_player_settings_title)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}