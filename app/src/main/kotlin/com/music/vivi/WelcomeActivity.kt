@file:OptIn(ExperimentalTextApi::class)

package com.music.vivi

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.music.vivi.constants.IsFirstRunKey
import com.music.vivi.ui.theme.vivimusicTheme
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit

data class OnboardingPageInfo(
    val content: @Composable (onUpdateScrollState: (Boolean) -> Unit) -> Unit
)

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val isFirstRun = dataStore.get(IsFirstRunKey, true)
        val forceShow = intent.getBooleanExtra("FORCE_SHOW", false)

        if (!isFirstRun && !forceShow) {
            finishOnboarding()
            return
        }

        enableEdgeToEdge()
        setContent {
            vivimusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WelcomePagerScreen(onFinished = {
                        lifecycleScope.launch {
                            dataStore.edit { it[IsFirstRunKey] = false }
                            if (forceShow) {
                                finish()
                            } else {
                                finishOnboarding()
                            }
                        }
                    })
                }
            }
        }
    }

    private fun finishOnboarding() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlex = FontFamily(
    Font(
        resId = com.music.vivi.R.font.google_sans_flex,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.width(100f),
            FontVariation.Setting("ROND", 100f)
        )
    )
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun WelcomePagerScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val commonAnimSpec = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing)

    val topCardShape =
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    val middleCardShape = RoundedCornerShape(4.dp)
    val bottomCardShape =
        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)

    val customWelcomeFontFamily = FontFamily(
        Font(
            resId = com.music.vivi.R.font.sans_flex,
            variationSettings = FontVariation.Settings(
                FontVariation.slant(-9f),
                FontVariation.width(111f),
                FontVariation.weight(333),
                FontVariation.Setting("GRAD", 100f),
                FontVariation.Setting("ROND", 100f)
            )
        )
    )

    val thinHeaderStyle = TextStyle(
        fontFamily = customWelcomeFontFamily,
        fontSize = 48.sp
    )

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var canInstallPackages by remember {
        mutableStateOf(
            if (BuildConfig.FLAVOR.contains("gms") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
            } else true
        )
    }

    var isLastPageScrolledToEnd by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    canInstallPackages = runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
        }
    )

    val installParamsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canInstallPackages = runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        }
    }

    val pages = listOf(
        OnboardingPageInfo(
            content = { _ ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(80.dp))

                    Text(
                        text = stringResource(com.music.vivi.R.string.welcome_to),
                        style = thinHeaderStyle,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(com.music.vivi.R.string.app_name),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 56.sp
                    )
                    
                    val flavorSuffix = if (BuildConfig.FLAVOR.contains("gms", ignoreCase = true)) "Gms Edition" else "Foss Edition"
                    Text(
                        text = flavorSuffix,
                        fontFamily = GoogleSansFlex,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 2.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RotatingShapeContainer(
                            modifier = Modifier.size(280.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = stringResource(com.music.vivi.R.string.welcome_preparing_subtitle),
                            fontFamily = GoogleSansFlex,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.weight(1.2f))
                }
            }
        ),
        OnboardingPageInfo(
            content = { _ ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(80.dp))

                    Text(
                        text = stringResource(com.music.vivi.R.string.perm_required),
                        style = thinHeaderStyle,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(com.music.vivi.R.string.perm_permissions),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 56.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(com.music.vivi.R.string.perm_intro_text),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = GoogleSansFlex
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        PermissionCard(
                            icon = Icons.Rounded.Notifications,
                            iconColor = Color(0xFFffaee4),
                            iconTint = Color(0xFF8d0053),
                            title = stringResource(com.music.vivi.R.string.perm_notif_title),
                            description = stringResource(com.music.vivi.R.string.perm_notif_desc),
                            shape = if (BuildConfig.FLAVOR.contains("gms", ignoreCase = true)) topCardShape else RoundedCornerShape(20.dp),
                            control = {
                                Switch(
                                    checked = hasNotificationPermission,
                                    onCheckedChange = {
                                        if (hasNotificationPermission) {
                                            val intent =
                                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                    putExtra(
                                                        Settings.EXTRA_APP_PACKAGE,
                                                        context.packageName
                                                    )
                                                }
                                            context.startActivity(intent)
                                        } else {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        }
                                    },
                                    thumbContent = {
                                        Icon(
                                            imageVector = if (hasNotificationPermission) Icons.Rounded.Check else Icons.Rounded.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            },
                            onClick = {
                                if (hasNotificationPermission) {
                                    val intent =
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(
                                                Settings.EXTRA_APP_PACKAGE,
                                                context.packageName
                                            )
                                        }
                                    context.startActivity(intent)
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            }
                        )

                        if (BuildConfig.FLAVOR.contains("gms", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(2.dp))

                            PermissionCard(
                                icon = Icons.Rounded.SystemUpdate,
                                iconColor = Color(0xFFffb683),
                                iconTint = Color(0xFF753403),
                                title = stringResource(com.music.vivi.R.string.perm_install_title),
                                description = stringResource(com.music.vivi.R.string.perm_install_desc),
                                shape = bottomCardShape,
                                control = {
                                    Icon(
                                        imageVector = if (canInstallPackages) Icons.Rounded.Check else Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = if (canInstallPackages) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val intent =
                                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                        installParamsLauncher.launch(intent)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        ),
        OnboardingPageInfo(
            content = { onUpdateScroll ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(80.dp))

                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Text(
                            text = stringResource(com.music.vivi.R.string.feat_discover),
                            style = thinHeaderStyle,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(com.music.vivi.R.string.feat_features),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp,
                            color = MaterialTheme.colorScheme.primary,
                            lineHeight = 56.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(com.music.vivi.R.string.feat_intro),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = GoogleSansFlex
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        val scrollState = rememberScrollState()

                        val isAtBottom by remember {
                            derivedStateOf {
                                val layoutInfo = scrollState.maxValue
                                layoutInfo == 0 || scrollState.value >= (layoutInfo - 20)
                            }
                        }

                        LaunchedEffect(isAtBottom) {
                            onUpdateScroll(isAtBottom)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            FeatureCard(
                                icon = Icons.Rounded.Lyrics,
                                iconColor = Color(0xFFffaee4),
                                iconTint = Color(0xFF8d0053),
                                title = stringResource(com.music.vivi.R.string.feat_lyrics_title),
                                description = stringResource(com.music.vivi.R.string.feat_lyrics_desc),
                                shape = topCardShape
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            FeatureCard(
                                icon = Icons.Rounded.CloudDownload,
                                iconColor = Color(0xFF80da88),
                                iconTint = Color(0xFF00522c),
                                shape = middleCardShape,
                                title = stringResource(com.music.vivi.R.string.feat_download_title),
                                description = stringResource(com.music.vivi.R.string.feat_download_desc)
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            FeatureCard(
                                icon = Icons.Rounded.HighQuality,
                                iconColor = Color(0xFFffb683),
                                iconTint = Color(0xFF753403),
                                title = stringResource(com.music.vivi.R.string.feat_quality_title),
                                description = stringResource(com.music.vivi.R.string.feat_quality_desc),
                                shape = middleCardShape
                            )

                            if (BuildConfig.FLAVOR.contains("gms", ignoreCase = true)) {
                                Spacer(modifier = Modifier.height(2.dp))

                                FeatureCard(
                                    icon = Icons.Rounded.SystemUpdate,
                                    iconColor = Color(0xFF67d4ff),
                                    iconTint = Color(0xFF004e5d),
                                    title = stringResource(com.music.vivi.R.string.feat_update_title),
                                    description = stringResource(com.music.vivi.R.string.feat_update_desc),
                                    shape = middleCardShape
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            FeatureCard(
                                icon = Icons.Rounded.Gavel,
                                iconColor = Color(0xFFb6c6ed),
                                iconTint = Color(0xFF001b3f),
                                title = stringResource(com.music.vivi.R.string.feat_license_title),
                                description = stringResource(com.music.vivi.R.string.feat_license_desc),
                                shape = middleCardShape
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            FeatureCard(
                                icon = Icons.Rounded.Terminal,
                                iconColor = Color(0xFFcabeff),
                                iconTint = Color(0xFF1c0062),
                                title = stringResource(com.music.vivi.R.string.feat_github_title),
                                description = stringResource(com.music.vivi.R.string.feat_github_desc),
                                shape = bottomCardShape
                            )

                            Spacer(modifier = Modifier.height(100.dp))
                        }

                        this@Column.AnimatedVisibility(
                            visible = scrollState.canScrollForward,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            LargeFloatingActionButton(
                                onClick = {
                                    scope.launch {
                                        scrollState.animateScrollTo(scrollState.maxValue)
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isFirstPage = pagerState.currentPage == 0
    val isLastPage = pagerState.currentPage == pages.size - 1

    BackHandler(enabled = !isFirstPage) {
        scope.launch {
            pagerState.animateScrollToPage(
                pagerState.currentPage - 1,
                animationSpec = commonAnimSpec
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { index ->
                pages[index].content { scrolledToEnd ->
                    if (index == pages.size - 1) {
                        isLastPageScrolledToEnd = scrolledToEnd
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val backButtonWeight by animateFloatAsState(
                targetValue = if (isFirstPage) 0.0001f else 1f,
                animationSpec = commonAnimSpec,
                label = "backWeight"
            )

            val spacerWeight by animateFloatAsState(
                targetValue = if (isFirstPage) 0.0001f else 0.05f,
                animationSpec = commonAnimSpec,
                label = "spacerWeight"
            )

            val alphaBack by animateFloatAsState(
                targetValue = if (isFirstPage) 0f else 1f,
                animationSpec = commonAnimSpec,
                label = "backAlpha"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(backButtonWeight)
                        .fillMaxHeight()
                        .alpha(alphaBack)
                ) {
                    WelcomeExpressiveButton(
                        text = stringResource(com.music.vivi.R.string.back_button_desc),
                        onClick = {
                            if (!isFirstPage) {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        pagerState.currentPage - 1,
                                        animationSpec = commonAnimSpec
                                    )
                                }
                            }
                        },
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxSize(),
                        isOutlined = true
                    )
                }

                Spacer(modifier = Modifier.weight(spacerWeight))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val isNextEnabled = !isLastPage || isLastPageScrolledToEnd

                    val alphaNext by animateFloatAsState(
                        targetValue = if (isNextEnabled) 1f else 0.5f,
                        label = "nextAlpha"
                    )

                    WelcomeExpressiveButton(
                        text = if (isLastPage) stringResource(com.music.vivi.R.string.get_started) else stringResource(
                            com.music.vivi.R.string.next
                        ),
                        onClick = {
                            if (isLastPage) {
                                if (isLastPageScrolledToEnd) onFinished()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        pagerState.currentPage + 1,
                                        animationSpec = commonAnimSpec
                                    )
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = alphaNext),
                        contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = alphaNext),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun RotatingShapeContainer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shapeRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = com.music.vivi.R.drawable.ic_ten_sided_cookie),
            contentDescription = null,
            tint = primaryColor,
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
        )

        Icon(
            painter = painterResource(com.music.vivi.R.mipmap.ic_launcher_monochrome),
            contentDescription = null,
            modifier = Modifier.size(220.dp),
            tint = backgroundColor
        )
    }
}

@Composable
fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconTint: Color,
    title: String,
    description: String,
    shape: Shape,
    control: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "anim_shape"
    )

    val animatedShape = remember(shape, pressProgress) {
        if (shape is RoundedCornerShape) {
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ): Outline {
                    val targetPx = with(density) { 20.dp.toPx() }
                    fun lerp(start: Float, stop: Float, fraction: Float) =
                        (1 - fraction) * start + fraction * stop

                    val ts = lerp(shape.topStart.toPx(size, density), targetPx, pressProgress)
                    val te = lerp(shape.topEnd.toPx(size, density), targetPx, pressProgress)
                    val bs = lerp(shape.bottomStart.toPx(size, density), targetPx, pressProgress)
                    val be = lerp(shape.bottomEnd.toPx(size, density), targetPx, pressProgress)

                    return Outline.Rounded(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(
                                0f,
                                0f,
                                size.width,
                                size.height
                            ),
                            topLeft = androidx.compose.ui.geometry.CornerRadius(ts),
                            topRight = androidx.compose.ui.geometry.CornerRadius(te),
                            bottomRight = androidx.compose.ui.geometry.CornerRadius(be),
                            bottomLeft = androidx.compose.ui.geometry.CornerRadius(bs)
                        )
                    )
                }
            }
        } else shape
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(animatedShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null, 
                onClick = onClick
            ),
        shape = animatedShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFlex
                )
            },
            supportingContent = {
                Text(
                    text = description,
                    fontFamily = GoogleSansFlex,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconColor,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            trailingContent = control,
            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconTint: Color,
    title: String,
    description: String,
    shape: Shape
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFlex
                )
            },
            supportingContent = {
                Text(
                    text = description,
                    fontFamily = GoogleSansFlex,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconColor,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun WelcomeExpressiveButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    isOutlined: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "buttonScale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(if (isPressed) Modifier.rotate(0f) else Modifier), // placeholder for scale
        color = containerColor,
        contentColor = contentColor,
        border = if (isOutlined) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null,
        shape = RoundedCornerShape(20.dp),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSansFlex,
                fontSize = 18.sp
            )
        }
    }
}
