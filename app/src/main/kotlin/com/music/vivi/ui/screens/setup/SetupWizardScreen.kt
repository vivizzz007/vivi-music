/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalTextApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.music.vivi.ui.screens.setup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.music.vivi.BuildConfig
import com.music.vivi.R
import com.music.vivi.constants.AppLanguageKey
import com.music.vivi.constants.LanguageCodeToName
import com.music.vivi.constants.SYSTEM_DEFAULT
import com.music.vivi.ui.component.ExpressiveSettingGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.utils.safeOpenUri
import com.music.vivi.utils.rememberPreference
import com.music.vivi.utils.setAppLocale
import com.music.vivi.vivimusic.updater.getAutoUpdateCheckSetting
import com.music.vivi.vivimusic.updater.saveAutoUpdateCheckSetting
import com.music.vivi.vivimusic.updater.saveUpdateAvailableState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale

val GoogleSansFlex = FontFamily(
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.width(100f),
            FontVariation.Setting("ROND", 100f)
        )
    )
)

private fun buildWavyRingPath(
    cx: Float,
    cy: Float,
    radius: Float,
    amplitude: Float,
    ripples: Int,
    startAngleDeg: Float = 0f,
    sweepAngleDeg: Float = 360f,
    isClosed: Boolean = true
): Path {
    val path = Path()
    val steps = (sweepAngleDeg * 1.5f).toInt().coerceAtLeast(30)
    val degToRad = (Math.PI / 180.0).toFloat()
    val startRad = startAngleDeg * degToRad
    val sweepRad = sweepAngleDeg * degToRad
    for (i in 0..steps) {
        val fraction = i.toFloat() / steps.toFloat()
        val theta = startRad + fraction * sweepRad
        val r = radius + amplitude * kotlin.math.sin(ripples * theta)
        val x = cx + r * kotlin.math.cos(theta)
        val y = cy + r * kotlin.math.sin(theta)
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    if (isClosed) {
        path.close()
    }
    return path
}

@Composable
fun SetupWizardScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var showLanguage by rememberSaveable { mutableStateOf(false) }
    val (language, onLanguageChange) = rememberPreference(key = AppLanguageKey, defaultValue = SYSTEM_DEFAULT)
    var selectedLanguage by rememberSaveable { mutableStateOf(language) }
    val currentLanguageName = remember(language) {
        if (language == SYSTEM_DEFAULT) "System default"
        else LanguageCodeToName[language] ?: language
    }

    val isGms = BuildConfig.FLAVOR.contains("gms", ignoreCase = true)
    val editionLabel = (if (isGms) "Gms Edition" else "Foss Edition") + " v${BuildConfig.VERSION_NAME}"

    var notificationsOn by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var installAllowed by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        )
    }
    var autoUpdateEnabled by rememberSaveable {
        mutableStateOf(getAutoUpdateCheckSetting(context))
    }

    var privacyText by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoadingPrivacy by remember { mutableStateOf(false) }
    var loadPrivacyFailed by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryTrigger) {
        if (privacyText == null) {
            isLoadingPrivacy = true
            loadPrivacyFailed = false
            withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://raw.githubusercontent.com/vivizzz007/vivi-music/main/docs/privacy/PRIVACY.md")
                    val fetched = url.openStream().bufferedReader().use { it.readText() }
                    if (fetched.isNotBlank()) {
                        privacyText = fetched
                        loadPrivacyFailed = false
                    }
                } catch (_: Exception) {
                    loadPrivacyFailed = true
                } finally {
                    isLoadingPrivacy = false
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsOn = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                installAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                    runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsOn = granted
    }

    val installLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        installAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
    }

    BackHandler(enabled = currentStep > 0) {
        currentStep--
    }

    val bounceAnim = remember { Animatable(0f) }
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        AnimatedVisibility(
            visible = currentStep == 0,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            scope.launch {
                                bounceAnim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
                                )
                                bounceAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    }
            ) {
                val baseWidth = 390f
                val scale = if (size.width > size.height) {
                    size.height / baseWidth
                } else {
                    size.width / baseWidth
                }

                val bounceOffset = 45f * scale * bounceAnim.value

                val topRing = buildWavyRingPath(
                    cx = size.width - (65f * scale),
                    cy = (106f * scale) - bounceOffset,
                    radius = 160f * scale,
                    amplitude = (8f + 3f * bounceAnim.value) * scale,
                    ripples = 12
                )

                val bottomRing = buildWavyRingPath(
                    cx = 139f * scale,
                    cy = size.height - (215f * scale) + bounceOffset,
                    radius = 276f * scale,
                    amplitude = (12f + 4f * bounceAnim.value) * scale,
                    ripples = 14,
                    startAngleDeg = 180f,
                    sweepAngleDeg = 180f,
                    isClosed = false
                )

                val stroke = Stroke(
                    width = 1.5.dp.toPx(),
                    join = StrokeJoin.Round,
                    cap = StrokeCap.Round
                )

                val ringColor = primaryColor.copy(alpha = 0.5f)

                drawPath(path = topRing, color = ringColor, style = stroke)
                drawPath(path = bottomRing, color = ringColor, style = stroke)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = {},
                navigationIcon = {
                    if (currentStep > 0) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = stringResource(R.string.back_button_desc)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

        val targetProgress = when (currentStep) {
            1 -> 0.25f
            2 -> 0.50f
            3 -> 0.75f
            4 -> 1.0f
            else -> 0f
        }
        val animatedProgress by animateFloatAsState(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            label = "SetupWizardProgress"
        )
        val progressAlpha by animateFloatAsState(
            targetValue = if (currentStep > 0) 1f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "SetupWizardProgressAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .alpha(progressAlpha)
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width } + fadeIn(tween(250))) togetherWith
                        (slideOutHorizontally { width -> -width } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn(tween(250))) togetherWith
                        (slideOutHorizontally { width -> width } + fadeOut(tween(200)))
                }.using(null)
            },
            label = "SetupWizardStepTransition",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
        ) { step ->
            when (step) {
                0 -> {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val screenHeight = maxHeight
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = screenHeight),
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Spacer(Modifier.height(24.dp))

                                Text(
                                    text = stringResource(R.string.setup_welcome_hi_there),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = GoogleSansFlex,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = buildAnnotatedString {
                                        append(stringResource(R.string.setup_welcome_title))
                                        append("\n")
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                            append(stringResource(R.string.setup_welcome_app_name))
                                        }
                                    },
                                    fontSize = 44.sp,
                                    lineHeight = 50.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = GoogleSansFlex,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = stringResource(R.string.setup_welcome_tagline),
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.height(24.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            selectedLanguage = language
                                            showLanguage = true
                                        },
                                        shapes = ButtonDefaults.shapes(
                                            shape = RoundedCornerShape(
                                                topStart = 26.dp,
                                                bottomStart = 26.dp,
                                                topEnd = 6.dp,
                                                bottomEnd = 6.dp
                                            ),
                                            pressedShape = RoundedCornerShape(
                                                topStart = 18.dp,
                                                bottomStart = 18.dp,
                                                topEnd = 4.dp,
                                                bottomEnd = 4.dp
                                            )
                                        ),
                                        modifier = Modifier.height(52.dp),
                                        contentPadding = PaddingValues(horizontal = 18.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.globe),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            selectedLanguage = language
                                            showLanguage = true
                                        },
                                        shapes = ButtonDefaults.shapes(
                                            shape = RoundedCornerShape(
                                                topStart = 6.dp,
                                                bottomStart = 6.dp,
                                                topEnd = 26.dp,
                                                bottomEnd = 26.dp
                                            ),
                                            pressedShape = RoundedCornerShape(
                                                topStart = 4.dp,
                                                bottomStart = 4.dp,
                                                topEnd = 18.dp,
                                                bottomEnd = 18.dp
                                            )
                                        ),
                                        modifier = Modifier.height(52.dp),
                                        contentPadding = PaddingValues(horizontal = 20.dp)
                                    ) {
                                        Text(
                                            text = currentLanguageName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(Modifier.height(24.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = editionLabel,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "By Vividh P Ashokan",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Spacer(Modifier.height(24.dp))

                                Button(
                                    onClick = { currentStep = 1 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(96.dp),
                                    shapes = ButtonDefaults.shapes(
                                        shape = RoundedCornerShape(48.dp),
                                        pressedShape = RoundedCornerShape(24.dp)
                                    ),
                                    contentPadding = PaddingValues(start = 36.dp, end = 16.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.setup_btn_get_started),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = GoogleSansFlex,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(MaterialShapes.Cookie9Sided.toShape())
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                        )
                                        Icon(
                                            painter = painterResource(R.drawable.arrow_forward),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            1 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(Modifier.height(24.dp))

                        Column {
                            Text(
                                text = stringResource(R.string.setup_permissions_title),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Light,
                                lineHeight = 36.sp,
                                fontFamily = GoogleSansFlex,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.setup_permissions_highlight),
                                fontSize = 44.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = GoogleSansFlex,
                                color = MaterialTheme.colorScheme.primary,
                                lineHeight = 46.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.setup_permissions_desc),
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        ExpressiveSettingGroup(
                                items = buildList {
                                    add(
                                        Material3SettingsItem(
                                            icon = painterResource(R.drawable.notification),
                                            title = {
                                                Text(
                                                    text = stringResource(R.string.setup_permission_notifications_title),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            description = {
                                                Text(text = stringResource(R.string.setup_permission_notifications_desc))
                                            },
                                            trailingContent = {
                                                Switch(
                                                    checked = notificationsOn,
                                                    onCheckedChange = {
                                                        if (notificationsOn) {
                                                            context.startActivity(
                                                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                            )
                                                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                        }
                                                    },
                                                    thumbContent = {
                                                        Icon(
                                                            painter = painterResource(
                                                                if (notificationsOn) R.drawable.check else R.drawable.close
                                                            ),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                                        )
                                                    }
                                                )
                                            },
                                            onClick = {
                                                if (notificationsOn) {
                                                    context.startActivity(
                                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                    )
                                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            }
                                        )
                                    )
                                    if (isGms) {
                                        add(
                                            Material3SettingsItem(
                                                icon = painterResource(R.drawable.apk_install),
                                                title = {
                                                    Text(
                                                        text = stringResource(R.string.setup_permission_install_title),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                },
                                                description = {
                                                    Text(text = stringResource(R.string.setup_permission_install_desc))
                                                },
                                                trailingContent = {
                                                    Switch(
                                                        checked = installAllowed,
                                                        onCheckedChange = {
                                                            if (!installAllowed) {
                                                                installLauncher.launch(
                                                                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                                                        .apply { data = Uri.parse("package:${context.packageName}") }
                                                                )
                                                            }
                                                        },
                                                        thumbContent = {
                                                            Icon(
                                                                painter = painterResource(
                                                                    if (installAllowed) R.drawable.check else R.drawable.close
                                                                ),
                                                                contentDescription = null,
                                                                modifier = Modifier.size(SwitchDefaults.IconSize)
                                                            )
                                                        }
                                                    )
                                                },
                                                onClick = {
                                                    if (!installAllowed) {
                                                        installLauncher.launch(
                                                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                                                .apply { data = Uri.parse("package:${context.packageName}") }
                                                        )
                                                    }
                                                }
                                            )
                                        )
                                    }
                                    add(
                                        Material3SettingsItem(
                                            icon = painterResource(R.drawable.update),
                                            title = {
                                                Text(
                                                    text = stringResource(R.string.auto_update_check),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            description = {
                                                Text(text = stringResource(R.string.setting_auto_update_desc))
                                            },
                                            trailingContent = {
                                                Switch(
                                                    checked = autoUpdateEnabled,
                                                    onCheckedChange = { enabled ->
                                                        autoUpdateEnabled = enabled
                                                        saveAutoUpdateCheckSetting(context, enabled)
                                                        if (!enabled) {
                                                            saveUpdateAvailableState(context, false)
                                                        }
                                                    },
                                                    thumbContent = {
                                                        Icon(
                                                            painter = painterResource(
                                                                if (autoUpdateEnabled) R.drawable.check else R.drawable.close
                                                            ),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                                        )
                                                    }
                                                )
                                            },
                                            onClick = {
                                                val enabled = !autoUpdateEnabled
                                                autoUpdateEnabled = enabled
                                                saveAutoUpdateCheckSetting(context, enabled)
                                                if (!enabled) {
                                                    saveUpdateAvailableState(context, false)
                                                }
                                            }
                                        )
                                    )
                                }
                            )

                            Spacer(Modifier.height(24.dp))
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { currentStep = 2 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp),
                                shapes = ButtonDefaults.shapes(
                                    shape = RoundedCornerShape(48.dp),
                                    pressedShape = RoundedCornerShape(24.dp)
                                ),
                                contentPadding = PaddingValues(start = 36.dp, end = 16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.continue_action),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = GoogleSansFlex,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(MaterialShapes.Cookie9Sided.toShape())
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                    )
                                    Icon(
                                        painter = painterResource(R.drawable.arrow_forward),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
            }
            2 -> {
                val privacyScrollState = rememberScrollState()
                var hasScrolledToBottom by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(privacyScrollState.value, privacyScrollState.maxValue, privacyText) {
                    if (privacyText != null) {
                        if (privacyScrollState.maxValue == 0 ||
                            !privacyScrollState.canScrollForward ||
                            privacyScrollState.value >= (privacyScrollState.maxValue - 40)
                        ) {
                            hasScrolledToBottom = true
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(privacyScrollState),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Spacer(Modifier.height(24.dp))

                            Text(
                                text = stringResource(R.string.setup_privacy_title),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = GoogleSansFlex,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(Modifier.height(12.dp))

                            when {
                                isLoadingPrivacy && privacyText == null -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 64.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LoadingIndicator()
                                    }
                                }
                                loadPrivacyFailed && privacyText == null -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Unable to load privacy policy",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            FilledTonalButton(
                                                onClick = { retryTrigger++ }
                                            ) {
                                                Text("Retry")
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    val lines = remember(privacyText) { privacyText?.lines().orEmpty() }
                                    val boldColor = MaterialTheme.colorScheme.onBackground
                                    val linkColor = MaterialTheme.colorScheme.primary
                                    val uriHandler = LocalUriHandler.current
                                    val linkStyles = remember(linkColor) {
                                        TextLinkStyles(
                                            style = SpanStyle(
                                                color = linkColor,
                                                fontWeight = FontWeight.SemiBold,
                                                textDecoration = TextDecoration.Underline
                                            )
                                        )
                                    }

                                    val parseMarkdownSpans: (String) -> AnnotatedString = { content ->
                                        buildAnnotatedString {
                                            var cursor = 0
                                            val pattern = Regex("""(\[([^\]]+)\]\((https?://[^\s\)]+)\))|(\*\*(.*?)\*\*)|(https?://[^\s\)]+)""")
                                            for (match in pattern.findAll(content)) {
                                                if (match.range.first > cursor) {
                                                    append(content.substring(cursor, match.range.first))
                                                }
                                                when {
                                                    match.groups[1] != null -> {
                                                        val label = match.groupValues[2].replace("**", "")
                                                        val url = match.groupValues[3]
                                                        val link = LinkAnnotation.Url(
                                                            url = url,
                                                            styles = linkStyles,
                                                            linkInteractionListener = {
                                                                try {
                                                                    uriHandler.openUri(url)
                                                                } catch (_: Exception) { }
                                                            }
                                                        )
                                                        withLink(link) {
                                                            append(label)
                                                        }
                                                    }
                                                    match.groups[4] != null -> {
                                                        val boldText = match.groupValues[5]
                                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                                                            append(boldText)
                                                        }
                                                    }
                                                    match.groups[6] != null -> {
                                                        val url = match.groupValues[6]
                                                        val link = LinkAnnotation.Url(
                                                            url = url,
                                                            styles = linkStyles,
                                                            linkInteractionListener = {
                                                                try {
                                                                    uriHandler.openUri(url)
                                                                } catch (_: Exception) { }
                                                            }
                                                        )
                                                        withLink(link) {
                                                            append(url)
                                                        }
                                                    }
                                                }
                                                cursor = match.range.last + 1
                                            }
                                            if (cursor < content.length) {
                                                append(content.substring(cursor))
                                            }
                                        }
                                    }

                                    lines.forEach { rawLine ->
                                        val line = rawLine.trim()
                                        when {
                                            line.isEmpty() -> Spacer(Modifier.height(4.dp))
                                            line.startsWith("# ") -> {} // Omit document h1 title as page already has header
                                            line == "---" -> HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                            line.startsWith("## ") -> Text(
                                                text = line.removePrefix("## ").trim(),
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = GoogleSansFlex,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(top = 10.dp)
                                            )
                                            line.startsWith("### ") -> Text(
                                                text = line.removePrefix("### ").trim(),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                modifier = Modifier.padding(top = 6.dp)
                                            )
                                            line.startsWith("- ") || line.startsWith("* ") -> {
                                                val bulletContent = line.substring(2).trim()
                                                val annotated = remember(bulletContent, boldColor, linkStyles) {
                                                    parseMarkdownSpans(bulletContent)
                                                }
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 4.dp, top = 2.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(top = 8.dp, end = 10.dp)
                                                            .size(5.dp)
                                                            .clip(RoundedCornerShape(2.5.dp))
                                                            .background(MaterialTheme.colorScheme.primary)
                                                    )
                                                    Text(
                                                        text = annotated,
                                                        fontSize = 14.sp,
                                                        lineHeight = 21.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            line.matches(Regex("""^\d+\.\s.*""")) -> {
                                                val dotIndex = line.indexOf('.')
                                                val numberPrefix = line.substring(0, dotIndex + 1)
                                                val numContent = line.substring(dotIndex + 1).trim()
                                                val annotated = remember(numContent, boldColor, linkStyles) {
                                                    parseMarkdownSpans(numContent)
                                                }
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 4.dp, top = 2.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = numberPrefix,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                    Text(
                                                        text = annotated,
                                                        fontSize = 14.sp,
                                                        lineHeight = 21.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            line.startsWith("*") && line.endsWith("*") && !line.startsWith("**") -> {
                                                val italicText = line.removeSurrounding("*").trim()
                                                Text(
                                                    text = italicText,
                                                    fontSize = 13.sp,
                                                    fontStyle = FontStyle.Italic,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                            }
                                            else -> {
                                                val annotated = remember(line, boldColor, linkStyles) {
                                                    parseMarkdownSpans(line)
                                                }
                                                Text(
                                                    text = annotated,
                                                    fontSize = 14.sp,
                                                    lineHeight = 21.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { currentStep = 3 },
                                enabled = hasScrolledToBottom,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp),
                                shapes = ButtonDefaults.shapes(
                                    shape = RoundedCornerShape(48.dp),
                                    pressedShape = RoundedCornerShape(24.dp)
                                ),
                                contentPadding = PaddingValues(start = 36.dp, end = 16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.setup_btn_agree_continue),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = GoogleSansFlex,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(MaterialShapes.Cookie9Sided.toShape())
                                            .background(
                                                if (hasScrolledToBottom) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                                }
                                            )
                                    )
                                    Icon(
                                        painter = painterResource(R.drawable.arrow_forward),
                                        contentDescription = null,
                                        tint = if (hasScrolledToBottom) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
            }
            3 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Spacer(Modifier.height(24.dp))

                            Text(
                                text = stringResource(R.string.setup_community_title),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = GoogleSansFlex,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Text(
                                text = stringResource(R.string.setup_community_desc),
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(8.dp))

                            ExpressiveSettingGroup(
                                items = listOf(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.github),
                                        title = {
                                            Text(
                                                text = stringResource(R.string.github_repository),
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = GoogleSansFlex
                                            )
                                        },
                                        description = {
                                            Text(
                                                text = stringResource(R.string.view_source_code),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = {
                                            uriHandler.safeOpenUri(context, "https://github.com/vivizzz007/vivi-music")
                                        },
                                        isExternalLink = true
                                    ),
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.telegram),
                                        title = {
                                            Text(
                                                text = stringResource(R.string.telegram_channel),
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = GoogleSansFlex
                                            )
                                        },
                                        description = {
                                            Text(
                                                text = stringResource(R.string.setup_community_telegram_desc),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = {
                                            uriHandler.safeOpenUri(context, "https://t.me/vivimusicapp")
                                        },
                                        isExternalLink = true
                                    ),
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.web_link),
                                        title = {
                                            Text(
                                                text = stringResource(R.string.website),
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = GoogleSansFlex
                                            )
                                        },
                                        description = {
                                            Text(
                                                text = stringResource(R.string.setup_community_website_desc),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = {
                                            uriHandler.safeOpenUri(context, "https://vivimusic.mkmdevilmi.workers.dev/")
                                        },
                                        isExternalLink = true
                                    )
                                )
                            )

                            Spacer(Modifier.height(24.dp))
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { currentStep = 4 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp),
                                shapes = ButtonDefaults.shapes(
                                    shape = RoundedCornerShape(48.dp),
                                    pressedShape = RoundedCornerShape(24.dp)
                                ),
                                contentPadding = PaddingValues(start = 36.dp, end = 16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.continue_action),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = GoogleSansFlex,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(MaterialShapes.Cookie9Sided.toShape())
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                    )
                                    Icon(
                                        painter = painterResource(R.drawable.arrow_forward),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            4 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.setup_all_set_title),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GoogleSansFlex,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = stringResource(R.string.setup_all_set_desc),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(8.dp))

                        ExpressiveSettingGroup(
                            items = listOf(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.play),
                                    title = {
                                        Text(
                                            text = stringResource(R.string.setup_feature_seamless_title),
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = GoogleSansFlex
                                        )
                                    },
                                    description = {
                                        Text(
                                            text = stringResource(R.string.setup_feature_seamless_desc),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                ),
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.block),
                                    title = {
                                        Text(
                                            text = stringResource(R.string.setup_feature_adfree_title),
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = GoogleSansFlex
                                        )
                                    },
                                    description = {
                                        Text(
                                            text = stringResource(R.string.setup_feature_adfree_desc),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                ),
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.download),
                                    title = {
                                        Text(
                                            text = stringResource(R.string.setup_feature_offline_title),
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = GoogleSansFlex
                                        )
                                    },
                                    description = {
                                        Text(
                                            text = stringResource(R.string.setup_feature_offline_desc),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            )
                        )

                        Spacer(Modifier.height(24.dp))
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onFinished,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp),
                            shapes = ButtonDefaults.shapes(
                                shape = RoundedCornerShape(48.dp),
                                pressedShape = RoundedCornerShape(24.dp)
                            ),
                            contentPadding = PaddingValues(start = 36.dp, end = 16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.setup_btn_start_listening),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = GoogleSansFlex,
                                modifier = Modifier.weight(1f)
                            )
                            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialShapes.Cookie9Sided.toShape())
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                )
                                Icon(
                                    painter = painterResource(R.drawable.arrow_forward),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
            }
        }
        }
    }

    if (showLanguage) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val options: List<Pair<String, String>> =
            listOf(SYSTEM_DEFAULT to "System default") +
                LanguageCodeToName.map { (tag, name) -> tag to name }

        ModalBottomSheet(
            onDismissRequest = { showLanguage = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.setup_choose_language),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = stringResource(R.string.setup_choose_language_desc),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    options.forEach { (tag, name) ->
                        val isSelected = tag == selectedLanguage
                        val cornerRadius by animateDpAsState(
                            targetValue = if (isSelected) 32.dp else 12.dp,
                            label = "languageCornerRadius"
                        )
                        Surface(
                            onClick = { selectedLanguage = tag },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(cornerRadius),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        onLanguageChange(selectedLanguage)
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            val locale = if (selectedLanguage == SYSTEM_DEFAULT) Locale.getDefault()
                            else Locale.forLanguageTag(selectedLanguage)
                            setAppLocale(context, locale)
                        }
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showLanguage = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shapes = ButtonDefaults.shapes(
                        shape = RoundedCornerShape(36.dp),
                        pressedShape = RoundedCornerShape(24.dp)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.setup_btn_apply),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = GoogleSansFlex
                    )
                }
            }
        }
    }
}
