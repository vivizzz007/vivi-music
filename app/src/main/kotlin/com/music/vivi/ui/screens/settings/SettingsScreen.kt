/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import com.music.vivi.R
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.Material3SettingsGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.screens.Screens
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.vivimusic.updater.getUpdateAvailableState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val isUpdateAvailable = getUpdateAvailableState(context) && com.music.vivi.vivimusic.updater.getAutoUpdateCheckSetting(context)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    // Debounce: wait 300ms after typing stops before filtering
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            debouncedQuery = ""
        } else {
            delay(300L)
            debouncedQuery = searchQuery
        }
    }

    // Search entries for deep search
    val searchEntries = remember { getSettingsSearchEntries() }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 16.dp)
        )

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_settings),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = { searchQuery = "" }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.clear),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
            singleLine = true,
        )

        if (debouncedQuery.isBlank()) {
            // ── Default view: show all top-level categories ──
            val categoryItems = buildCategoryItems(
                isUpdateAvailable = isUpdateAvailable,
                navController = navController
            )
            Material3SettingsGroup(items = categoryItems)
        } else {
            // ── Filtered view: search across ALL individual settings ──
            val query = debouncedQuery.trim().lowercase()
            val matchedEntries = remember(query, searchEntries) {
                searchEntries.filter { entry ->
                    val titleMatch = context.getString(entry.titleResId).lowercase().contains(query)
                    val keywordMatch = entry.extraKeywords.any { it.lowercase().contains(query) }
                    val categoryMatch = context.getString(entry.categoryResId).lowercase().contains(query)
                    titleMatch || keywordMatch || categoryMatch
                }.distinctBy { it.settingKey }
            }

            if (matchedEntries.isNotEmpty()) {
                // Group by route
                val groupedByRoute = matchedEntries.groupBy { it.route }

                groupedByRoute.entries.forEachIndexed { index, (route, entries) ->
                    val categoryName = stringResource(entries.first().categoryResId)
                    Material3SettingsGroup(
                        title = categoryName,
                        items = entries.map { entry ->
                            Material3SettingsItem(
                                icon = painterResource(entry.iconResId),
                                title = { Text(stringResource(entry.titleResId)) },
                                onClick = {
                                    navController.navigate(
                                        "${entry.route}?highlight=${entry.settingKey}"
                                    )
                                }
                            )
                        }
                    )
                    if (index < groupedByRoute.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            } else {
                // No results message
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.search_off),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(bottom = 12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = stringResource(R.string.no_settings_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(50.dp))
    }

    TopAppBar(
        title = {
//            Text(stringResource(R.string.settings))
                },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )
}

/**
 * Builds the top-level category items (default non-searching view).
 */
@Composable
private fun buildCategoryItems(
    isUpdateAvailable: Boolean,
    navController: NavController
): List<Material3SettingsItem> = listOf(
    Material3SettingsItem(
        icon = painterResource(if (isUpdateAvailable) R.drawable.vivimusicnotification else R.drawable.network_update),
        title = { Text(stringResource(R.string.system_update)) },
        description = if (isUpdateAvailable) {
            {
                Text(
                    text = stringResource(R.string.update_available),
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else null,
        onClick = { navController.navigate("settings/update") }
    ),
    Material3SettingsItem(
        icon = painterResource(R.drawable.palette),
        title = { Text(stringResource(R.string.appearance)) },
        onClick = { navController.navigate("settings/appearance") }
    ),
    Material3SettingsItem(
        icon = painterResource(R.drawable.account),
        title = { Text(stringResource(R.string.account)) },
        onClick = { navController.navigate("settings/account") }
    ),
    Material3SettingsItem(
        icon = painterResource(R.drawable.group),
        title = { Text(stringResource(R.string.listen_together)) },
        onClick = { navController.navigate(Screens.ListenTogether.route) }
    ),
    Material3SettingsItem(
        icon = painterResource(R.drawable.play),
        title = { Text(stringResource(R.string.player_and_audio)) },
        onClick = { navController.navigate("settings/player") }
    ),
    Material3SettingsItem(
        icon = painterResource(R.drawable.language),
        title = { Text(stringResource(R.string.content)) },
        onClick = { navController.navigate("settings/content") }
    ),
    Material3SettingsItem(
        icon = painterResource(R.drawable.translate),
        title = { Text(stringResource(R.string.ai_lyrics_translation)) },
        onClick = { navController.navigate("settings/ai") }
    ),
    Material3SettingsItem(
        icon = painterResource(R.drawable.security),
        title = { Text(stringResource(R.string.privacy)) },
        onClick = { navController.navigate("settings/privacy") }
    ),
    Material3SettingsItem(
        icon = painterResource(R.drawable.storage),
        title = { Text(stringResource(R.string.storage)) },
        onClick = { navController.navigate("settings/storage") }
    ),
    Material3SettingsItem(
        icon = painterResource(R.drawable.restore),
        title = { Text(stringResource(R.string.backup_restore)) },
        onClick = { navController.navigate("settings/backup_restore") }
    ),
    Material3SettingsItem(
        icon = painterResource(R.drawable.info),
        title = { Text(stringResource(R.string.about)) },
        onClick = { navController.navigate("settings/about") }
    )
)
