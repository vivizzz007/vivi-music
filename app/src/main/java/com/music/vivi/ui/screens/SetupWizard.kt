
package com.music.vivi.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close // For the "wrong sign"
import androidx.compose.material.icons.rounded.Settings // For the settings icon
import androidx.compose.material.icons.rounded.MusicVideo
import androidx.compose.material.icons.rounded.NotInterested
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.FirstSetupPassed
import com.music.vivi.ui.screens.settings.shimmerEffect
import com.music.vivi.utils.rememberPreference
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MusicVideo
import androidx.compose.material.icons.rounded.NotInterested
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition




@Composable
fun SetupWizard(
    navController: NavController,
) {
    val shimmerBrush = shimmerEffect() // Assuming this is defined
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current

    // Assuming FirstSetupPassed and rememberPreference are defined and working
    val (firstSetupPassed, onFirstSetupPassedChange) = rememberPreference("FirstSetupPassed", defaultValue = false)

    var position by remember {
        mutableIntStateOf(0)
    }

    val MAX_POS = 2 // Only 0 and 1 now

    if (position > 0) {
        BackHandler {
            position -= 1
        }
    }

    if (firstSetupPassed) {
        navController.navigateUp()
        return // Return to prevent rendering the wizard if already passed
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues) // Apply Scaffold's padding first
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 32.dp)) // Increased spacing

                if (position == 0) {
                    // Welcome Screen
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.party))
                        LottieAnimation(
                            composition = composition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier
                                .size(300.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Your Image composable is commented out here:
                            // Image(
                            //     painter = painterResource(R.drawable.vivimusic),
                            //     contentDescription = null,
                            //     modifier = Modifier
                            //         .size(150.dp)
                            //         .clickable { },
                            // )
                        }
                        // This spacer pushes the text down from the Lottie/Box.

                        // "Welcome to Vivi Music" Text (made bigger as per previous discussion)
                        Text(
                            text = "Welcome to \nVivi Music",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 40.sp // Explicitly set font size
                            ),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp), // Padding around the text itself
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // --- NEW TEXT ADDED BELOW ---
                        Text(
                            text = "Your ultimate music experience, ad-free and open source.",
                            style = MaterialTheme.typography.bodyLarge, // Or bodyMedium, bodySmall
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp), // Adjust padding as needed
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) // Slightly faded color
                        )
                        // --- END NEW TEXT ---

                        Spacer(Modifier.height(20.dp)) // This spacer pushes the button up, making text higher from bottom.
                    }
                }

                if (position == MAX_POS - 1) {
                    // Final Setup Screen
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = stringResource(R.string.finish_setup),
                            modifier = Modifier.size(96.dp), // Increased size
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp)) // Increased spacing
                        Text(
                            text = stringResource(R.string.all_set),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.enjoy_vivi_music),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(20.dp)) // Add spacing before the moved texts

                        // Start Glassmorphism Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)) // Rounded corners for the glass effect
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp).copy(alpha = 0.6f)) // Translucent background
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), // Subtle border
                                    shape = RoundedCornerShape(16.dp)
                                )
                                // MODIFIED: Reduced blur from 8.dp to 3.dp
                                .padding(16.dp) // Internal padding for content
                        ) {
                            ProvideTextStyle(value = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.secondary)) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Row(
                                        verticalAlignment = CenterVertically,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MusicVideo,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.yt_music_fingertips),
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.NotInterested,
                                            tint = Color.Red,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.ad_free_playback),
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Sync,
                                            tint = Color.Red,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.ytm_account_sync),
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.github),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.fos_info),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                        // End Glassmorphism Box
                    }
                }
            }

            // Universal Button at the bottom
            val buttonText: String? = when (position) {
                0 -> stringResource(R.string.get_started)
                MAX_POS - 1 -> stringResource(R.string.finish_setup)
                else -> null // Don't show the button if position is not handled
            }

            if (buttonText != null) {
                SetupWizardButton(
                    text = buttonText,
                    onClick = {
                        when (position) {
                            0 -> position += 1
                            MAX_POS - 1 -> {
                                onFirstSetupPassedChange(true)
                                navController.navigateUp()
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = paddingValues.calculateBottomPadding() + 16.dp)
                )
            }
        }
    }
}

@Composable
fun SetupWizardButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(70.dp), // Standard button height
        shape = RoundedCornerShape(50.dp), // Rounded corners
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

// Dummy implementations for missing functions to make the code runnable in isolation
@Composable
fun shimmerEffect(): Color {
    // Replace with your actual shimmer effect implementation
    return Color.Gray
}

@Composable
fun rememberPreference(key: String, defaultValue: Boolean): Pair<Boolean, (Boolean) -> Unit> {
    // Replace with your actual preference implementation
    val (value, setValue) = remember { mutableStateOf(defaultValue) }
    return value to setValue
}

object R {
    object string {
        val back = 1
        val next = 2
        val get_started = 3
        val welcome_to_vivi = 4
        val allow_unknown_sources = 5
        val allow_unknown_sources_title = 6
        val allow_unknown_sources_explanation = 7
        val grant_permission = 8
        val recheck_permission = 9
        val skip_this_step = 10
        val permission_granted = 11
        val permission_granted_message = 12
        val continue_text = 13 // Not used directly as 'Next' is used
        val finish_setup = 14
        val all_set = 15
        val enjoy_vivi_music = 16
        val yt_music_fingertips = 17
        val ad_free_playback = 18
        val ytm_account_sync = 19
        val fos_info = 20
        val cancel = 21
    }
    object raw {
        val party = 0
    }
    object drawable {
        val vivimusic = 0
        val github = 0
    }
}