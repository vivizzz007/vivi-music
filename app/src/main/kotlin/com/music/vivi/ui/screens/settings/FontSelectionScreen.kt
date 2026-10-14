/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.music.vivi.ui.component.AnimatedRadioButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.TextButton
import com.music.vivi.constants.CustomFontPathKey
import java.io.File
import java.io.FileOutputStream
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.AppFont
import com.music.vivi.constants.SelectedFontKey
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.ExpressiveSettingGroup
import com.music.vivi.ui.component.Material3SettingsItem

import com.music.vivi.ui.theme.OutfitFontFamily
import com.music.vivi.ui.theme.PlusJakartaSansFontFamily
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSelectionScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (selectedFont, onSelectedFontChange) = rememberPreference(
        SelectedFontKey,
        defaultValue = AppFont.SYSTEM.value
    )
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val (customFontPath, onCustomFontPathChange) = rememberPreference(
        CustomFontPathKey,
        defaultValue = ""
    )

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        var originalName = "custom_font.ttf"
                        context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (displayNameIndex != -1) {
                                    originalName = cursor.getString(displayNameIndex)
                                }
                            }
                        }

                        if (customFontPath.isNotEmpty()) {
                            val oldFile = File(customFontPath)
                            if (oldFile.exists() && oldFile.name != originalName) {
                                oldFile.delete()
                            }
                        }

                        val inputStream = context.contentResolver.openInputStream(it)
                        val file = File(context.filesDir, originalName)
                        val outputStream = FileOutputStream(file)
                        inputStream?.copyTo(outputStream)
                        inputStream?.close()
                        outputStream.close()
                        
                        onCustomFontPathChange(file.absolutePath)
                        onSelectedFontChange(AppFont.CUSTOM.value)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    val activeFontFamily = remember(selectedFont, customFontPath) {
        when (AppFont.fromValue(selectedFont)) {
            AppFont.SYSTEM -> FontFamily.Default

            AppFont.OUTFIT -> OutfitFontFamily
            AppFont.PLUS_JAKARTA_SANS -> PlusJakartaSansFontFamily
            AppFont.CUSTOM -> {
                try {
                    if (customFontPath.isNotEmpty() && File(customFontPath).exists()) {
                        val typeface = android.graphics.Typeface.createFromFile(customFontPath)
                        FontFamily(androidx.compose.ui.text.font.Typeface(typeface))
                    } else {
                        FontFamily.Default
                    }
                } catch (e: Exception) {
                    FontFamily.Default
                }
            }
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {

        // Typography Preview Card
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.typography_preview).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = stringResource(R.string.preview_text_quote),
                    fontFamily = activeFontFamily,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "Expressive typeface is applied to display, headlines, and titles. Body copy and labels remain in the system font for maximum readability.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Options settings group
        ExpressiveSettingGroup(
            title = stringResource(R.string.font_selection),
            items = buildList {
                add(
                    Material3SettingsItem(
                        leadingContent = {
                            AnimatedRadioButton(
                                selected = selectedFont == AppFont.SYSTEM.value,
                                onClick = null
                            )
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.font_system),
                                fontFamily = FontFamily.Default
                            )
                        },
                        description = {
                            Text(
                                text = stringResource(R.string.font_system_desc),
                                fontFamily = FontFamily.Default
                            )
                        },
                        onClick = { onSelectedFontChange(AppFont.SYSTEM.value) }
                    )
                )

                add(
                    Material3SettingsItem(
                        leadingContent = {
                            AnimatedRadioButton(
                                selected = selectedFont == AppFont.OUTFIT.value,
                                onClick = null
                            )
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.font_outfit),
                                fontFamily = OutfitFontFamily
                            )
                        },
                        description = {
                            Text(
                                text = stringResource(R.string.font_outfit_desc),
                                fontFamily = OutfitFontFamily
                            )
                        },
                        onClick = { onSelectedFontChange(AppFont.OUTFIT.value) }
                    )
                )
                
                add(
                    Material3SettingsItem(
                        leadingContent = {
                            AnimatedRadioButton(
                                selected = selectedFont == AppFont.PLUS_JAKARTA_SANS.value,
                                onClick = null
                            )
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.font_plus_jakarta_sans),
                                fontFamily = PlusJakartaSansFontFamily
                            )
                        },
                        description = {
                            Text(
                                text = stringResource(R.string.font_plus_jakarta_sans_desc),
                                fontFamily = PlusJakartaSansFontFamily
                            )
                        },
                        onClick = { onSelectedFontChange(AppFont.PLUS_JAKARTA_SANS.value) }
                    )
                )

                val customFontLoaded = customFontPath.isNotEmpty() && File(customFontPath).exists()
                if (customFontLoaded) {
                    add(
                        Material3SettingsItem(
                            leadingContent = {
                                AnimatedRadioButton(
                                    selected = selectedFont == AppFont.CUSTOM.value,
                                    onClick = null
                                )
                            },
                            title = {
                                Text(
                                    text = stringResource(R.string.font_custom),
                                    fontFamily = activeFontFamily.takeIf { selectedFont == AppFont.CUSTOM.value } ?: FontFamily.Default
                                )
                            },
                            description = {
                                Text(
                                    text = File(customFontPath).name,
                                    fontFamily = FontFamily.Default
                                )
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        File(customFontPath).delete()
                                        onCustomFontPathChange("")
                                        if (selectedFont == AppFont.CUSTOM.value) {
                                            onSelectedFontChange(AppFont.SYSTEM.value)
                                        }
                                    },
                                    onLongClick = {}
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.delete),
                                        contentDescription = "Delete Custom Font"
                                    )
                                }
                            },
                            onClick = { onSelectedFontChange(AppFont.CUSTOM.value) }
                        )
                    )
                }
                
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.add),
                        title = {
                            Text(
                                text = stringResource(R.string.import_custom_font),
                                fontFamily = FontFamily.Default
                            )
                        },
                        description = {
                            Text(
                                text = stringResource(R.string.font_custom_desc),
                                fontFamily = FontFamily.Default
                            )
                        },
                        onClick = { fontPickerLauncher.launch("*/*") }
                    )
                )
            }
        )
        Spacer(modifier = Modifier.height(36.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.app_font)) },
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
        }
    )
}
