package com.music.vivi.ui.crash

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.view.WindowCompat
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.Toast
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.music.vivi.MainActivity
import com.music.vivi.R
import com.music.vivi.ui.theme.DefaultThemeColor
import com.music.vivi.ui.theme.MusicTheme
import com.music.vivi.ui.utils.appBarScrollBehavior
import com.music.vivi.update.settingstyle.ModernInfoItem
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashPage(
    errorText: String
) {
    val context = LocalContext.current
    val window = (context as? Activity)?.window

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(isSystemInDarkTheme) {
        isSystemInDarkTheme
    }

    val themeColor = remember { DefaultThemeColor }

    LaunchedEffect(Unit) {
        (context as? Activity)?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars())
                    controller.hide(WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        )
            }
        }
    }

    val scrollBehavior = appBarScrollBehavior(canScroll = { true })

    MusicTheme(
        darkTheme = useDarkTheme,
        pureBlack = false,
        themeColor = themeColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text("Uh Oh!")
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        scrollBehavior = scrollBehavior
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Uh oh! ViVi hit a wall and came to an abrupt stop. See the log below for more information. More options are provided on the bottom of the page."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = errorText,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.CopyAll,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                title = "Copy Log Content",
                                subtitle = "Copies the crash log to clipboard.",
                                titleColor = MaterialTheme.colorScheme.onSurface,
                                subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = {
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Short ViVi Crash Info", errorText)
                                    clipboard.setPrimaryClip(clip)
                                },
                                showArrow = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                title = "Send via Gmail",
                                subtitle = "Copies the log and opens Gmail.",
                                titleColor = MaterialTheme.colorScheme.onSurface,
                                subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = {
                                    // Copy to clipboard
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("ViVi Crash Log", errorText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Log copied to clipboard", Toast.LENGTH_SHORT).show()

                                    // Send Email
                                    val deviceInfo = """
                                        App Version: ${com.music.vivi.BuildConfig.VERSION_NAME}
                                        Android Version: ${Build.VERSION.RELEASE}
                                        Device: ${Build.MANUFACTURER} ${Build.MODEL}
                                        SDK: ${Build.VERSION.SDK_INT}
                                    """.trimIndent()

                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf("mkmdevilmi@gmail.com"))
                                        putExtra(Intent.EXTRA_SUBJECT, "ViVi Music Crash Report")
                                        putExtra(Intent.EXTRA_TEXT, "Crash Log (also copied to clipboard):\n\n${errorText}\n\nDevice Info:\n${deviceInfo}")
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(emailIntent, "Send email via..."))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                showArrow = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.github),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                title = "Report via GitHub",
                                subtitle = "Opens GitHub to report the issue.",
                                titleColor = MaterialTheme.colorScheme.onSurface,
                                subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/vivizzz007/vivi-music/issues/new?template=bug_report.yml"))
                                    )
                                },
                                showArrow = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            ModernInfoItem(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                title = "Restart ViVi",
                                subtitle = "Restarts ViVi.",
                                titleColor = MaterialTheme.colorScheme.onSurface,
                                subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = {
                                    val intent = Intent(context, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    }
                                    context.startActivity(intent)
                                    Thread.sleep(1000)
                                    exitProcess(0)
                                },
                                showArrow = true,
                                iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
