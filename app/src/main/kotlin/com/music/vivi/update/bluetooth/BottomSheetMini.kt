
@file:OptIn(ExperimentalMaterial3Api::class)

package com.music.vivi.bluetooth


import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.material3.*


import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import com.music.vivi.constants.AudioQuality
import com.music.vivi.constants.AudioQualityKey
import com.music.vivi.utils.rememberEnumPreference
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface


data class AudioDevice(
    val name: String,
    val type: AudioDeviceType,
    val isConnected: Boolean,
    val isActive: Boolean = false,
    val batteryLevel: Int? = null
)

enum class AudioDeviceType {
    BLUETOOTH,
    WIRED_HEADPHONES,
    PHONE_SPEAKER,
    EXTERNAL_SPEAKER
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDeviceBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var audioDevices by remember { mutableStateOf<List<AudioDevice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // Volume state with animation
    var targetVolume by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    val animatedVolume by animateFloatAsState(
        targetValue = targetVolume,
        animationSpec = tween(durationMillis = 200),
        label = "volumeAnimation"
    )
    var maxVolume by remember { mutableStateOf(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) }

    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        key = AudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadDevices(context, false, onSuccess = { devices ->
                audioDevices = devices
                isLoading = false
            }, onError = { error ->
                errorMessage = error
                isLoading = false
            })
        } else {
            errorMessage = "Bluetooth permission required"
            isLoading = false
        }
    }

    // Volume change receiver
    DisposableEffect(Unit) {
        val volumeChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    if (streamType == AudioManager.STREAM_MUSIC) {
                        targetVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                    }
                }
            }
        }

        val maxVolumeChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val newMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                if (newMax != maxVolume) {
                    maxVolume = newMax
                    if (targetVolume > newMax) {
                        targetVolume = newMax.toFloat()
                    }
                }
            }
        }

        context.registerReceiver(
            volumeChangeReceiver,
            IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        )

        context.registerReceiver(
            maxVolumeChangeReceiver,
            IntentFilter(AudioManager.ACTION_HDMI_AUDIO_PLUG)
        )

        if (checkBluetoothPermission(context)) {
            loadDevices(context, false, onSuccess = { devices ->
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

        val bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                loadDevices(context, false, onSuccess = { devices ->
                    audioDevices = devices
                }, onError = {})
            }
        }

        val wiredHeadsetReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                loadDevices(context, false, onSuccess = { devices ->
                    audioDevices = devices
                }, onError = {})
            }
        }

        with(context) {
            registerReceiver(
                bluetoothReceiver,
                IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                    addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                }
            )
            registerReceiver(
                wiredHeadsetReceiver,
                IntentFilter(AudioManager.ACTION_HEADSET_PLUG)
            )
        }

        onDispose {
            with(context) {
                try {
                    unregisterReceiver(volumeChangeReceiver)
                    unregisterReceiver(maxVolumeChangeReceiver)
                    unregisterReceiver(bluetoothReceiver)
                    unregisterReceiver(wiredHeadsetReceiver)
                } catch (e: IllegalArgumentException) {
                    // Receivers were not registered
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
        ) {
            // Animated header
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audio Devices",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

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
                            imageVector = Icons.Outlined.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                loadDevices(context, false, onSuccess = { devices ->
                                    audioDevices = devices
                                    isLoading = false
                                }, onError = { error ->
                                    errorMessage = error
                                    isLoading = false
                                })
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(text = "Retry")
                        }
                    }
                }

                audioDevices.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Devices,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No audio devices found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Animated device list
                        AnimatedVisibility(
                            visible = audioDevices.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(audioDevices, key = { it.type }) { device ->
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val pressed by interactionSource.collectIsPressedAsState()
                                    val backgroundColor by animateColorAsState(
                                        if (pressed) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        } else if (device.isActive) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        label = "cardColor"
                                    )

                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .animateContentSize(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = backgroundColor
                                            ),
                                            onClick = {
                                                switchToAudioDevice(context, device)
                                                audioDevices = audioDevices.map { currentDevice ->
                                                    if (currentDevice.type == device.type) {
                                                        currentDevice.copy(isActive = true)
                                                    } else {
                                                        currentDevice.copy(isActive = false)
                                                    }
                                                }
                                            },
                                            interactionSource = interactionSource
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = when (device.type) {
                                                        AudioDeviceType.BLUETOOTH -> Icons.Filled.Bluetooth
                                                        AudioDeviceType.WIRED_HEADPHONES -> Icons.Filled.Headphones
                                                        AudioDeviceType.PHONE_SPEAKER -> Icons.Filled.Speaker
                                                        AudioDeviceType.EXTERNAL_SPEAKER -> Icons.Filled.SpeakerGroup
                                                    },
                                                    contentDescription = null,
                                                    tint = if (device.isActive) {
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                )

                                                Spacer(modifier = Modifier.width(16.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = device.name,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = if (device.isActive) {
                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurface
                                                        }
                                                    )

                                                    Text(
                                                        text = when {
                                                            device.isActive -> "Currently in use"
                                                            device.isConnected -> "Connected"
                                                            else -> "Available"
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (device.isActive) {
                                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )

                                                    if (device.type == AudioDeviceType.BLUETOOTH && device.batteryLevel != null) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Filled.BatteryStd,
                                                                contentDescription = "Battery level",
                                                                modifier = Modifier.size(12.dp),
                                                                tint = when {
                                                                    device.batteryLevel > 75 -> Color.Green
                                                                    device.batteryLevel > 25 -> Color.Yellow
                                                                    else -> Color.Red
                                                                }
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "${device.batteryLevel}%",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }

                                                if (device.isActive) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CheckCircle,
                                                        contentDescription = "Active",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                } else if (device.isConnected) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.RadioButtonUnchecked,
                                                        contentDescription = "Connected",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Volume Slider with animation
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text(
                                    text = "Volume",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VolumeUp,
                                        contentDescription = "Volume",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Slider(
                                        value = animatedVolume.coerceIn(0f, maxVolume.toFloat()),
                                        onValueChange = { newVolume ->
                                            targetVolume = newVolume
                                            audioManager.setStreamVolume(
                                                AudioManager.STREAM_MUSIC,
                                                newVolume.toInt(),
                                                AudioManager.FLAG_SHOW_UI
                                            )
                                        },
                                        onValueChangeFinished = {
                                            // Haptic feedback
                                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                                            } else {
                                                vibrator?.vibrate(10)
                                            }
                                        },
                                        valueRange = 0f..maxVolume.toFloat(),
                                        steps = maxVolume - 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Audio Quality Selector with animation
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Text(
                                    text = "Audio Quality",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Surface(
                                    tonalElevation = 1.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AudioQuality.values().forEach { quality ->
                                            val isSelected by animateFloatAsState(
                                                targetValue = if (quality == audioQuality) 1f else 0f,
                                                label = "qualitySelection"
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = Color.Transparent,
                                                border = BorderStroke(
                                                    animateDpAsState(
                                                        if (quality == audioQuality) 1.dp else 0.5.dp,
                                                        label = "borderWidth"
                                                    ).value,
                                                    animateColorAsState(
                                                        if (quality == audioQuality)
                                                            MaterialTheme.colorScheme.primary
                                                        else
                                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                        label = "borderColor"
                                                    ).value
                                                ),
                                                onClick = {
                                                    onAudioQualityChange(quality)
                                                    applyAudioQuality(context, quality)
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 4.dp)
                                                    .background(
                                                        animateColorAsState(
                                                            if (quality == audioQuality)
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f * isSelected)
                                                            else
                                                                Color.Transparent,
                                                            label = "background"
                                                        ).value
                                                    )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = when (quality) {
                                                            AudioQuality.AUTO -> "Auto"
                                                            AudioQuality.HIGH -> "High"
                                                            AudioQuality.LOW -> "Low"
                                                        },
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = animateColorAsState(
                                                            if (quality == audioQuality)
                                                                MaterialTheme.colorScheme.primary
                                                            else
                                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                                            label = "textColor"
                                                        ).value,
                                                        modifier = Modifier.align(Alignment.Center)
                                                    )
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
        }
    }
}
private fun loadDevices(
    context: Context,
    forcePhoneSpeaker: Boolean,
    onSuccess: (List<AudioDevice>) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isBluetoothA2dpOn = audioManager.isBluetoothA2dpOn
        val isWiredHeadsetOn = audioManager.isWiredHeadsetOn
        val isSpeakerphoneOn = audioManager.isSpeakerphoneOn
        val isBluetoothScoOn = audioManager.isBluetoothScoOn

        val devices = mutableListOf<AudioDevice>()

        // Bluetooth devices - check first since they have priority
        if (checkBluetoothPermission(context)) {
            try {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val bluetoothAdapter = bluetoothManager.adapter

                if (bluetoothAdapter?.isEnabled == true) {
                    val connectedDevices = bluetoothAdapter.getBondedDevices()
                        .filter { device ->
                            device.isConnected() &&
                                    (device.bluetoothClass?.majorDeviceClass == 0x0400 || // Audio/Video
                                            device.bluetoothClass?.majorDeviceClass == 0x0404)   // Headphones
                        }

                    connectedDevices.forEach { device ->
                        devices.add(
                            AudioDevice(
                                name = device.name?.takeIf { it.isNotBlank() }
                                    ?: "Bluetooth Headphones",
                                type = AudioDeviceType.BLUETOOTH,
                                isConnected = true,
                                isActive = isBluetoothA2dpOn || isBluetoothScoOn,
                                batteryLevel = getBluetoothBatteryLevel(context, device)
                            )
                        )
                    }
                }
            } catch (e: SecurityException) {
                throw e
            }
        }

        // Wired headphones - only show if connected
        if (isWiredHeadsetOn) {
            devices.add(
                AudioDevice(
                    name = "Wired Headphones",
                    type = AudioDeviceType.WIRED_HEADPHONES,
                    isConnected = true,
                    isActive = !isBluetoothA2dpOn && !isSpeakerphoneOn && !isBluetoothScoOn
                )
            )
        }

        // External speaker - only show if active
        if (isSpeakerphoneOn) {
            devices.add(
                AudioDevice(
                    name = "External Speaker",
                    type = AudioDeviceType.EXTERNAL_SPEAKER,
                    isConnected = true,
                    isActive = true
                )
            )
        }

        // Phone speaker - show only if no other devices are active
        val hasActiveDevice = devices.any { it.isActive }
        if (!hasActiveDevice || devices.isEmpty()) {
            devices.add(
                AudioDevice(
                    name = "Phone Speaker",
                    type = AudioDeviceType.PHONE_SPEAKER,
                    isConnected = true,
                    isActive = !hasActiveDevice
                )
            )
        }

        onSuccess(devices)
    } catch (e: SecurityException) {
        onError("Permission denied: ${e.message}")
    } catch (e: Exception) {
        onError("Failed to load devices: ${e.message}")
    }
}

private fun switchToAudioDevice(context: Context, device: AudioDevice) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    try {
        when (device.type) {
            AudioDeviceType.BLUETOOTH -> {
                // Set mode to normal for media playback
                audioManager.mode = AudioManager.MODE_NORMAL

                // Stop any existing SCO connection
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false

                // For A2DP devices (music playback), we don't need SCO
                // Just ensure A2DP is enabled and speaker is off
                audioManager.isSpeakerphoneOn = false

                // Some devices need this to properly route audio to Bluetooth
                audioManager.setBluetoothA2dpOn(true)

                // Wait a bit for the Bluetooth device to be ready
                Handler(Looper.getMainLooper()).postDelayed({
                    // Try to start bluetooth SCO if needed (for calls)
                    // But for media playback, A2DP should be sufficient
                    if (audioManager.isBluetoothA2dpOn) {
                        // Additional check to ensure audio is routed to Bluetooth
                        audioManager.setMode(AudioManager.MODE_NORMAL)
                    }
                }, 200)
            }

            AudioDeviceType.PHONE_SPEAKER -> {
                audioManager.mode = AudioManager.MODE_NORMAL
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                audioManager.isSpeakerphoneOn = false
                audioManager.setBluetoothA2dpOn(false)
            }

            AudioDeviceType.WIRED_HEADPHONES -> {
                audioManager.mode = AudioManager.MODE_NORMAL
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                audioManager.isSpeakerphoneOn = false
                audioManager.setBluetoothA2dpOn(false)
            }

            AudioDeviceType.EXTERNAL_SPEAKER -> {
                audioManager.mode = AudioManager.MODE_NORMAL
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                audioManager.isSpeakerphoneOn = true
                audioManager.setBluetoothA2dpOn(false)
            }
        }
    } catch (e: SecurityException) {
        // Handle permission denied
        Log.e("AudioDevice", "Permission denied while switching audio device", e)
    }
}
fun checkBluetoothPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true // No need for permission before Android 12
    }
}

private fun BluetoothDevice.isConnected(): Boolean {
    return try {
        // Using reflection to check connection status for older APIs
        val method = this::class.java.getMethod("isConnected")
        method.invoke(this) as? Boolean ?: false
    } catch (e: Exception) {
        false
    }
}

