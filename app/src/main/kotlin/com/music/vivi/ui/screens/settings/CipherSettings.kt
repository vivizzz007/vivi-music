/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.CipherLastUpdatedKey
import com.music.vivi.constants.CipherManualUpdate1Key
import com.music.vivi.constants.CipherManualUpdate2Key
import com.music.vivi.constants.CipherManualUpdate3Key
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.Material3SettingsGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.component.ModernSwitch
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.cipher.PlayerConfigStore
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CipherSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    var isUpdating by remember { mutableStateOf(false) }

    val (lastUpdated, _) = rememberPreference(
        CipherLastUpdatedKey,
        defaultValue = 0L
    )

    // Rate-limiting update timestamps (Limits to 3 clicks per 24 hours / 1 day)
    val (updateTime1, onUpdateTime1Change) = rememberPreference(
        CipherManualUpdate1Key,
        defaultValue = 0L
    )
    val (updateTime2, onUpdateTime2Change) = rememberPreference(
        CipherManualUpdate2Key,
        defaultValue = 0L
    )
    val (updateTime3, onUpdateTime3Change) = rememberPreference(
        CipherManualUpdate3Key,
        defaultValue = 0L
    )

    // Keep track of remaining time in milliseconds for the countdown
    var timeRemaining by remember { mutableStateOf(0L) }
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(lastUpdated) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTimeMillis = now
            val nextFetchTime = lastUpdated + (6 * 60 * 60 * 1000L)
            val remaining = nextFetchTime - now
            if (remaining <= 0L) {
                timeRemaining = 0L
                if (!isUpdating) {
                    isUpdating = true
                    try {
                        PlayerConfigStore.triggerUpdate()
                    } catch (e: Exception) {
                        // Fail silently in background
                    }
                    isUpdating = false
                }
            } else {
                timeRemaining = remaining
            }
            delay(1000L)
        }
    }

    val limitWindow = 24 * 60 * 60 * 1000L // 24 hours (1 day)

    // Check if all 3 recent updates occurred within the last 24 hours
    val isRateLimited = updateTime1 > 0L && updateTime2 > 0L && updateTime3 > 0L &&
        (currentTimeMillis - updateTime1 < limitWindow) &&
        (currentTimeMillis - updateTime2 < limitWindow) &&
        (currentTimeMillis - updateTime3 < limitWindow)

    val rateLimitRemaining = if (isRateLimited) {
        limitWindow - (currentTimeMillis - updateTime1)
    } else {
        0L
    }

    val attemptsUsed = listOf(updateTime1, updateTime2, updateTime3)
        .count { it > 0L && (currentTimeMillis - it < limitWindow) }
    val attemptsLeft = (3 - attemptsUsed).coerceIn(0, 3)

    val hours = (timeRemaining / (1000L * 60 * 60)) % 24
    val minutes = (timeRemaining / (1000L * 60)) % 60
    val seconds = (timeRemaining / 1000L) % 60

    val lastUpdatedText = if (lastUpdated > 0L) {
        val sdf = SimpleDateFormat("yyyy-MM-dd 'at' hh:mm a", Locale.getDefault())
        sdf.format(Date(lastUpdated))
    } else {
        stringResource(R.string.last_updated_never)
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Countdown Snapped Boxes Row (Matches Pomodoro segments from Tomato-1.8.5)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        ) {
            // Hours Box (Rounded outer-left, snap inner-right)
            TimerBox(
                value = hours,
                label = stringResource(R.string.hours_label),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 4.dp, bottomEnd = 4.dp)
            )

            Spacer(modifier = Modifier.width(2.dp))

            // Minutes Box (Flat/snapped both sides)
            TimerBox(
                value = minutes,
                label = stringResource(R.string.minutes_label),
                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 4.dp, bottomEnd = 4.dp)
            )

            Spacer(modifier = Modifier.width(2.dp))

            // Seconds Box (Snap inner-left, rounded outer-right)
            TimerBox(
                value = seconds,
                label = stringResource(R.string.seconds_label),
                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Large capsule banner for main switch (Locked to Active/Always On, but styled fully active)
        val containerColor by animateColorAsState(
            targetValue = MaterialTheme.colorScheme.primaryContainer,
            label = "containerColor"
        )
        val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

        Card(
            onClick = {
                Toast.makeText(context, "Vivi-Extractor is required for playback and cannot be disabled.", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(50),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.youtube_decryption_support),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                ModernSwitch(
                    checked = true,
                    onCheckedChange = {
                        Toast.makeText(context, "Vivi-Extractor is required for playback and cannot be disabled.", Toast.LENGTH_SHORT).show()
                    },
                    enabled = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Options settings group
        Material3SettingsGroup(
            title = stringResource(R.string.youtube_decryption_settings),
            items = listOf(
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.force_update_cipher)) },
                    description = {
                        if (isRateLimited) {
                            val waitHours = rateLimitRemaining / (1000 * 60 * 60)
                            val waitMinutes = (rateLimitRemaining / (1000 * 60)) % 60
                            val waitSeconds = (rateLimitRemaining / 1000) % 60
                            Text(
                                text = "Rate-limited! Cooldown: ${waitHours}h ${waitMinutes}m ${waitSeconds}s left",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(stringResource(R.string.force_update_cipher_desc))
                        }
                    },
                    trailingContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp)
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    strokeWidth = 4.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            val containerColor = if (attemptsLeft == 0) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                            val contentColor = if (attemptsLeft == 0) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(
                                        color = containerColor,
                                        shape = RoundedCornerShape(50)
                                    )
                            ) {
                                Text(
                                    text = attemptsLeft.toString(),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = contentColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    },
                    enabled = !isUpdating && !isRateLimited,
                    onClick = {
                        if (!isUpdating && !isRateLimited) {
                            isUpdating = true
                            Toast.makeText(context, R.string.cipher_updating, Toast.LENGTH_SHORT).show()
                            PlayerConfigStore.forceUpdateNow { success, message ->
                                isUpdating = false
                                if (success) {
                                    // Slide manual update timestamps to record the success
                                    onUpdateTime1Change(updateTime2)
                                    onUpdateTime2Change(updateTime3)
                                    onUpdateTime3Change(System.currentTimeMillis())

                                    val formattedMsg = context.getString(
                                        R.string.cipher_update_success,
                                        PlayerConfigStore.knownHashes().size
                                    )
                                    Toast.makeText(context, formattedMsg, Toast.LENGTH_LONG).show()
                                } else {
                                    val formattedMsg = context.getString(
                                        R.string.cipher_update_failed,
                                        message
                                    )
                                    Toast.makeText(context, formattedMsg, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                ),
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.last_updated)) },
                    description = {
                        Text(
                            text = lastUpdatedText,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                )
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.padding(top = 24.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                painter = painterResource(R.drawable.info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.youtube_decryption_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.youtube_decryption_settings)) },
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

@Composable
private fun TimerBox(value: Long, label: String, shape: Shape) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(112.dp, 100.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = shape
                )
        ) {
            Text(
                text = "%02d".format(value),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 57.sp,
                    letterSpacing = (-2).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
