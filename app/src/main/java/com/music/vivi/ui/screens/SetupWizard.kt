package com.music.vivi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.MusicVideo
import androidx.compose.material.icons.rounded.NotInterested
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.FirstSetupPassed
import com.music.vivi.ui.screens.settings.shimmerEffect
import com.music.vivi.utils.rememberPreference

@Composable
fun SetupWizard(
    navController: NavController,
) {
    val shimmerBrush = shimmerEffect()
    val layoutDirection = LocalLayoutDirection.current

    val (firstSetupPassed, onFirstSetupPassedChange) = rememberPreference(FirstSetupPassed, defaultValue = false)

    var position by remember {
        mutableIntStateOf(0)
    }

    val MAX_POS = 4

    if (position > 0) {
        BackHandler {
            position -= 1
        }
    }

    if (firstSetupPassed) {
        navController.navigateUp()
    }

    val navBar = @Composable {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    if (position > 0) {
                        position -= 1
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.back),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                    contentDescription = null
                )
            }

            LinearProgressIndicator(
                progress = { position.toFloat() / MAX_POS },
                strokeCap = StrokeCap.Butt,
                drawStopIndicator = {},
                modifier = Modifier
                    .weight(1f, false)
                    .height(8.dp)
                    .padding(2.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    if (position < MAX_POS) {
                        position += 1
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.next),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                    contentDescription = null
                )
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (position > 0 && position < MAX_POS) {
                Box(
                    Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                        .fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        navBar()
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(
                    PaddingValues(
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        top = 0.dp,
                        end = paddingValues.calculateEndPadding(layoutDirection),
                        bottom = paddingValues.calculateBottomPadding()
                    )
                )
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 16.dp))

                when (position) {
                    0 -> {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(NavigationBarDefaults.Elevation))
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_launcher_monochrome),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground, BlendMode.SrcIn),
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(shimmerBrush)
                            )
                        }
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = stringResource(R.string.welcome_to_vivi),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicVideo,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = stringResource(R.string.yt_music_fingertips),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotInterested,
                                    tint = Color.Red,
                                    contentDescription = null
                                )
                                Text(
                                    text = stringResource(R.string.ad_free_playback),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    tint = Color.Red,
                                    contentDescription = null
                                )
                                Text(
                                    text = stringResource(R.string.ytm_account_sync),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.github),
                                    contentDescription = null
                                )
                                Text(
                                    text = stringResource(R.string.fos_info),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    navController.navigate("settings/backup_restore")
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.i_have_a_backup),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }

                            TextButton(
                                onClick = {
                                    onFirstSetupPassedChange(true)
                                    navController.navigateUp()
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.skip),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }

            if (position == 0 || position == MAX_POS) {
                FloatingActionButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomEnd),
                    onClick = {
                        navController.navigate("home")
                        onFirstSetupPassedChange(true)
                        navController.navigateUp()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }
    }
}