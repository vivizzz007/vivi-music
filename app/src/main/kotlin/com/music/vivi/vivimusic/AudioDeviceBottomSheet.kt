/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.music.vivi.vivimusic

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.key
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ToggleButtonDefaults
import com.music.vivi.R
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.constants.AudioQuality
import com.music.vivi.constants.AudioQualityKey
import com.music.vivi.utils.rememberEnumPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource

data class AudioDevice(
    val name: String,
    val type: AudioDeviceType,
    val isConnected: Boolean,
    val isActive: Boolean = false,
    val batteryLevel: Int? = null,
    val deviceId: Int? = null,
)

enum class AudioDeviceType {
    BLUETOOTH,
    WIRED_HEADPHONES,
    PHONE_SPEAKER,
    EXTERNAL_SPEAKER,
    USB_HEADSET,
    HDMI,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AudioDeviceBottomSheet(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var audioDevices by remember { mutableStateOf<List<AudioDevice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var currentVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }
    var isUserDragging by remember { mutableStateOf(false) }
    var maxVolume by remember { mutableStateOf(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) }

    val playerConnection = LocalPlayerConnection.current
    val service = playerConnection?.service
    var showDevicePopup by remember { mutableStateOf(false) }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadDevices(context, service?.preferredDeviceId, onSuccess = { devices ->
                audioDevices = devices
                isLoading = false
            }, onError = { error ->
                errorMessage = error
                isLoading = false
            })
        } else {
            errorMessage = context.getString(R.string.bluetooth_permission_required)
            isLoading = false
        }
    }

    fun refreshDevices() {
        loadDevices(context, service?.preferredDeviceId, onSuccess = { devices ->
            audioDevices = devices
        }, onError = {})
    }

    DisposableEffect(Unit) {
        val volumeChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    if (streamType == AudioManager.STREAM_MUSIC && !isUserDragging) {
                        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                    }
                }
            }
        }

        val audioDeviceReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                refreshDevices()
            }
        }

        context.registerReceiver(
            volumeChangeReceiver,
            IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.registerReceiver(
                audioDeviceReceiver,
                IntentFilter().apply {
                    addAction(AudioManager.ACTION_HEADSET_PLUG)
                    addAction(AudioManager.ACTION_HDMI_AUDIO_PLUG)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                    }
                }
            )
        }

        if (checkBluetoothPermission(context)) {
            loadDevices(context, service?.preferredDeviceId, onSuccess = { devices ->
                audioDevices = devices
                isLoading = false
            }, onError = { error ->
                errorMessage = error
                isLoading = false
            })
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        val handler = Handler(Looper.getMainLooper())

        val audioDeviceCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            object : android.media.AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    refreshDevices()
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    refreshDevices()
                }
            }
        } else null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioDeviceCallback != null) {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler)
        }

        val bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                refreshDevices()
                handler.postDelayed({ refreshDevices() }, 1000)
                handler.postDelayed({ refreshDevices() }, 2500)
            }
        }

        context.registerReceiver(
            bluetoothReceiver,
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )

        val batteryPollingRunnable = object : Runnable {
            override fun run() {
                refreshDevices()
                handler.postDelayed(this, 30000)
            }
        }
        handler.postDelayed(batteryPollingRunnable, 30000)

        onDispose {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioDeviceCallback != null) {
                    audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
                }
                context.unregisterReceiver(volumeChangeReceiver)
                context.unregisterReceiver(audioDeviceReceiver)
                context.unregisterReceiver(bluetoothReceiver)
                handler.removeCallbacksAndMessages(null)
            } catch (e: IllegalArgumentException) {
            }
        }
    }

    val isDarkTheme = isSystemInDarkTheme()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.error),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                refreshDevices()
                            }
                        ) {
                            Text(text = stringResource(R.string.retry))
                        }
                    }
                }

                else -> {
                    val activeDevice = audioDevices.firstOrNull { it.isActive }
                    val hasBluetooth = audioDevices.any { it.type == AudioDeviceType.BLUETOOTH }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Section Header matching Volume and Audio Quality
                                Text(
                                    text = stringResource(R.string.audio_devices),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                activeDevice?.let { device ->
                                    val rawBattery = device.batteryLevel
                                    val hasValidBattery = device.type == AudioDeviceType.BLUETOOTH && rawBattery != null && rawBattery in 0..100
                                    val batteryProgress by animateFloatAsState(
                                        targetValue = if (hasValidBattery) (rawBattery!! / 100f).coerceIn(0f, 1f) else 0f,
                                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                                        label = "batteryArc"
                                    )
                                    val chevronRotation by animateFloatAsState(
                                        targetValue = if (showDevicePopup) 180f else 0f,
                                        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                                        label = "chevron"
                                    )
                                    @DrawableRes val activeDeviceIconRes = when (device.type) {
                                        AudioDeviceType.BLUETOOTH, AudioDeviceType.WIRED_HEADPHONES -> R.drawable.headphones
                                        AudioDeviceType.USB_HEADSET -> R.drawable.usb
                                        AudioDeviceType.HDMI -> R.drawable.tv
                                        AudioDeviceType.PHONE_SPEAKER -> R.drawable.phone_android
                                        AudioDeviceType.EXTERNAL_SPEAKER -> R.drawable.speaker_apple
                                    }

                                    Surface(
                                        onClick = { showDevicePopup = !showDevicePopup },
                                        enabled = hasBluetooth,
                                        shape = RoundedCornerShape(32.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (hasValidBattery) {
                                                val progressColor = MaterialTheme.colorScheme.primary
                                                val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                                        val strokeWidth = 8.dp.toPx()
                                                        val inset = strokeWidth / 2f
                                                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                                                        val style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                                        drawArc(
                                                            color = trackColor,
                                                            startAngle = 135f,
                                                            sweepAngle = 270f,
                                                            useCenter = false,
                                                            topLeft = Offset(inset, inset),
                                                            size = arcSize,
                                                            style = style
                                                        )
                                                        drawArc(
                                                            color = progressColor,
                                                            startAngle = 135f,
                                                            sweepAngle = 270f * batteryProgress,
                                                            useCenter = false,
                                                            topLeft = Offset(inset, inset),
                                                            size = arcSize,
                                                            style = style
                                                        )
                                                    }
                                                    Text(
                                                        text = "$rawBattery",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier
                                                            .align(Alignment.Center)
                                                            .graphicsLayer { translationY = -2.dp.toPx() }
                                                    )
                                                    Icon(
                                                        painter = painterResource(R.drawable.headphones),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier
                                                            .align(Alignment.BottomCenter)
                                                            .size(12.dp)
                                                            .graphicsLayer { translationY = -3.dp.toPx() }
                                                    )
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier.size(44.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(activeDeviceIconRes),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp),
                                                        tint = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (device.type == AudioDeviceType.PHONE_SPEAKER) {
                                                        stringResource(R.string.this_phone)
                                                    } else {
                                                        device.name
                                                    },
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (hasValidBattery) "Connected · $rawBattery% battery" else "Connected",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            if (hasBluetooth) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 32.dp, height = 44.dp)
                                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.expand_more),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .graphicsLayer { rotationZ = chevronRotation },
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Animated in-place device switcher panel
                                AnimatedVisibility(
                                    visible = hasBluetooth && showDevicePopup,
                                    enter = expandVertically(
                                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                                    ) + fadeIn(
                                        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                                    ),
                                    exit = shrinkVertically(
                                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                                    ) + fadeOut(
                                        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val availableDevices = audioDevices.filter { !it.isActive }
                                        availableDevices.forEach { dev ->
                                            key(dev.deviceId) {
                                                val devBattery = dev.batteryLevel
                                                val hasDevBattery = dev.type == AudioDeviceType.BLUETOOTH && devBattery != null && devBattery in 0..100
                                                @DrawableRes val deviceIconRes = when (dev.type) {
                                                    AudioDeviceType.BLUETOOTH, AudioDeviceType.WIRED_HEADPHONES -> R.drawable.headphones
                                                    AudioDeviceType.USB_HEADSET -> R.drawable.usb
                                                    AudioDeviceType.HDMI -> R.drawable.tv
                                                    AudioDeviceType.PHONE_SPEAKER -> R.drawable.phone_android
                                                    AudioDeviceType.EXTERNAL_SPEAKER -> R.drawable.speaker_apple
                                                }

                                                Surface(
                                                    onClick = {
                                                        service?.setPreferredAudioDevice(dev.deviceId)
                                                        refreshDevices()
                                                        showDevicePopup = false
                                                    },
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(deviceIconRes),
                                                                contentDescription = null,
                                                                modifier = Modifier.size(22.dp),
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.width(14.dp))

                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = if (dev.type == AudioDeviceType.PHONE_SPEAKER)
                                                                    stringResource(R.string.this_phone) else dev.name,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Medium,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            val subtitle = when (dev.type) {
                                                                AudioDeviceType.PHONE_SPEAKER -> stringResource(R.string.phone_speaker)
                                                                AudioDeviceType.BLUETOOTH -> if (hasDevBattery) "$devBattery% battery" else null
                                                                AudioDeviceType.WIRED_HEADPHONES -> stringResource(R.string.wired_headphones)
                                                                AudioDeviceType.HDMI -> stringResource(R.string.hdmi)
                                                                else -> null
                                                            }
                                                            if (subtitle != null) {
                                                                Text(
                                                                    text = subtitle,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }

                                                        if (hasDevBattery) {
                                                            val progressColor = MaterialTheme.colorScheme.primary
                                                            val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                                            Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                                    val strokeWidth = 3.5.dp.toPx()
                                                                    val inset = strokeWidth / 2f
                                                                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                                                                    val style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                                                    drawArc(trackColor, -90f, 360f, false, Offset(inset, inset), arcSize, style = style)
                                                                    drawArc(progressColor, -90f, 360f * (devBattery!! / 100f).coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = style)
                                                                }
                                                                Text(
                                                                    text = "$devBattery",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    // Volume Control
                    var volumeSliderValue by rememberSaveable { mutableFloatStateOf(currentVolume) }
                    var lastNonZeroVolume by rememberSaveable { mutableFloatStateOf(if (currentVolume > 0f) currentVolume else maxVolume * 0.5f) }
                    LaunchedEffect(currentVolume) {
                        volumeSliderValue = currentVolume
                        if (currentVolume > 0f) {
                            lastNonZeroVolume = currentVolume
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                                Text(
                                    text = stringResource(R.string.volume),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f).padding(start = 12.dp, end = 16.dp)
                                )
                                val percentage = if (maxVolume > 0) ((volumeSliderValue / maxVolume) * 100).toInt() else 0
                                Text(
                                    text = "$percentage%",
                                    modifier = Modifier.padding(end = 14.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            ViviVolumeSlider(
                                modifier = Modifier.fillMaxWidth(),
                                value = volumeSliderValue,
                                onValueChange = { newValue ->
                                    isUserDragging = true
                                    volumeSliderValue = newValue
                                    currentVolume = newValue
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, kotlin.math.round(newValue).toInt(), 0)
                                },
                                onValueChangeFinished = { isUserDragging = false },
                                valueRange = 0f..maxVolume.toFloat()
                            )
                        }
                    }

                    // Audio Quality Selector
                    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(key = AudioQualityKey, defaultValue = AudioQuality.AUTO)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                            Text(
                                text = stringResource(R.string.audio_quality_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp, start = 12.dp).fillMaxWidth()
                            )
                            val options = listOf(stringResource(R.string.audio_quality_auto), stringResource(R.string.audio_quality_high), stringResource(R.string.audio_quality_low))
                            val selectedIndex = when (audioQuality) { AudioQuality.AUTO -> 0; AudioQuality.HIGH -> 1; AudioQuality.LOW -> 2; else -> 0 }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                            ) {
                                options.forEachIndexed { index, label ->
                                    ToggleButton(
                                        checked = selectedIndex == index,
                                        onCheckedChange = { onAudioQualityChange(when (index) { 0 -> AudioQuality.AUTO; 1 -> AudioQuality.HIGH; else -> AudioQuality.LOW }) },
                                        colors = ToggleButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                                        shapes = when (index) { 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes(); options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes(); else -> ButtonGroupDefaults.connectedMiddleButtonShapes() },
                                        modifier = Modifier.weight(1f).height(48.dp).semantics { role = Role.RadioButton }
                                    ) { Text(text = label, style = MaterialTheme.typography.bodyMedium) }
                                }
                            }
                        }
                    }
                    }


                }
            }
        }
    }
}

private fun loadDevices(
    context: Context,
    preferredDeviceId: Int?,
    onSuccess: (List<AudioDevice>) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = mutableListOf<AudioDevice>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            var hasActiveDevice = false

            audioDevices.forEach { deviceInfo ->
                val device = when (deviceInfo.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                        val batteryLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                if (ActivityCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    val bluetoothManager = context.getSystemService(
                                        Context.BLUETOOTH_SERVICE
                                    ) as BluetoothManager
                                    val bluetoothAdapter = bluetoothManager.adapter
                                    val pairedDevices = bluetoothAdapter?.bondedDevices
                                    val btDevice = pairedDevices?.find {
                                        it.name == deviceInfo.productName.toString()
                                    }

                                    @SuppressLint("MissingPermission")
                                    val battery = btDevice?.let { device ->
                                        try {
                                            val method = android.bluetooth.BluetoothDevice::class.java.getMethod(
                                                "getBatteryLevel"
                                            )
                                            val level = method.invoke(device) as? Int
                                            level
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (battery != null && battery >= 0 && battery <= 100) battery else null
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        } else null

                        AudioDevice(
                            name = deviceInfo.productName?.toString() ?: "Bluetooth Device",
                            type = AudioDeviceType.BLUETOOTH,
                            isConnected = true,
                            isActive = false,
                            batteryLevel = batteryLevel,
                            deviceId = deviceInfo.id
                        )
                    }

                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> {
                        AudioDevice(
                            name = context.getString(R.string.wired_headphones),
                            type = AudioDeviceType.WIRED_HEADPHONES,
                            isConnected = true,
                            isActive = false,
                            deviceId = deviceInfo.id
                        )
                    }
                    AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> {
                        AudioDevice(
                            name = deviceInfo.productName?.toString() ?: "USB Audio",
                            type = AudioDeviceType.USB_HEADSET,
                            isConnected = true,
                            isActive = false,
                            deviceId = deviceInfo.id
                        )
                    }
                    AudioDeviceInfo.TYPE_HDMI -> {
                        AudioDevice(
                            name = context.getString(R.string.hdmi),
                            type = AudioDeviceType.HDMI,
                            isConnected = true,
                            isActive = false,
                            deviceId = deviceInfo.id
                        )
                    }
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                        AudioDevice(
                            name = context.getString(R.string.phone_speaker),
                            type = AudioDeviceType.PHONE_SPEAKER,
                            isConnected = true,
                            isActive = false,
                            deviceId = deviceInfo.id
                        )
                    }
                    else -> null
                }
                device?.let { devices.add(it) }
            }

            val activeDevice = determineActiveDevice(audioManager, audioDevices, preferredDeviceId)
            val updatedDevices = devices.map { device ->
                device.copy(isActive = device.deviceId == activeDevice?.id)
            }

            // Maintain a stable order: Phone Speaker -> Wired -> Bluetooth -> Others
            val sortedDevices = updatedDevices.sortedWith(compareBy<AudioDevice> {
                when (it.type) {
                    AudioDeviceType.PHONE_SPEAKER -> 0
                    AudioDeviceType.WIRED_HEADPHONES -> 1
                    AudioDeviceType.USB_HEADSET -> 2
                    AudioDeviceType.BLUETOOTH -> 3
                    else -> 4
                }
            }.thenBy { it.name })

            onSuccess(sortedDevices.distinctBy { it.name })
        } else {
            loadDevicesLegacy(context, onSuccess, onError)
        }
    } catch (e: Exception) {
        onError("Failed to load devices: ${e.message}")
    }
}

private fun determineActiveDevice(
    audioManager: AudioManager,
    audioDevices: Array<AudioDeviceInfo>,
    preferredDeviceId: Int?
): AudioDeviceInfo? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val preferred = if (preferredDeviceId != null) {
            audioDevices.find { it.id == preferredDeviceId }
        } else null

        preferred ?: when {
            audioDevices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP } ->
                audioDevices.find { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            audioDevices.any {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
            } ->
                audioDevices.find {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                }
            else -> audioDevices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        }
    } else null

@Suppress("DEPRECATION")
private fun loadDevicesLegacy(context: Context, onSuccess: (List<AudioDevice>) -> Unit, onError: (String) -> Unit) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val devices = mutableListOf<AudioDevice>()

    if (audioManager.isBluetoothA2dpOn) {
        devices.add(AudioDevice("Bluetooth Device", AudioDeviceType.BLUETOOTH, true, true))
    }
    if (audioManager.isWiredHeadsetOn) {
        devices.add(AudioDevice("Wired Headphones", AudioDeviceType.WIRED_HEADPHONES, true, true))
    }
    if (audioManager.isSpeakerphoneOn) {
        devices.add(AudioDevice("External Speaker", AudioDeviceType.EXTERNAL_SPEAKER, true, true))
    }
    if (devices.isEmpty() || !devices.any { it.isActive }) {
        devices.add(AudioDevice("Phone Speaker", AudioDeviceType.PHONE_SPEAKER, true, true))
    }
    onSuccess(devices.filter { it.isActive }.take(1))
}

private fun checkBluetoothPermission(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED
} else true




