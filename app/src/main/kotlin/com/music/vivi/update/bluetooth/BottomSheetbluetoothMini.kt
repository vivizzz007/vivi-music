
@file:OptIn(ExperimentalMaterial3Api::class)

package com.music.vivi.bluetooth

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
import androidx.compose.animation.core.animate
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.music.vivi.R
import com.music.vivi.constants.AudioQuality
import com.music.vivi.constants.AudioQualityKey
import com.music.vivi.utils.rememberEnumPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class AudioDevice(
    val name: String,
    val type: AudioDeviceType,
    val isConnected: Boolean,
    val isActive: Boolean = false,
    val batteryLevel: Int? = null,
    val deviceId: Int? = null, // Added for API 23+
)

enum class AudioDeviceType {
    BLUETOOTH,
    WIRED_HEADPHONES,
    PHONE_SPEAKER,
    EXTERNAL_SPEAKER,
    USB_HEADSET,
    HDMI,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDeviceBottomSheet(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var audioDevices by remember { mutableStateOf<List<AudioDevice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // Volume state - MEDIA only (removed animation for smooth dragging)
    var currentVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }
    var isUserDragging by remember { mutableStateOf(false) }
    var maxVolume by remember { mutableStateOf(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadDevices(context, onSuccess = { devices ->
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
        loadDevices(context, onSuccess = { devices ->
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
            loadDevices(context, onSuccess = { devices ->
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
                refreshDevices()
            }
        }

        context.registerReceiver(
            bluetoothReceiver,
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )

        // Real-time battery polling for Bluetooth devices
        val handler = Handler(Looper.getMainLooper())
        val batteryPollingRunnable = object : Runnable {
            override fun run() {
                // Refresh devices to update battery levels
                refreshDevices()
                // Poll every 30 seconds
                handler.postDelayed(this, 30000)
            }
        }
        // Start polling after 30 seconds
        handler.postDelayed(batteryPollingRunnable, 30000)

        onDispose {
            try {
                context.unregisterReceiver(volumeChangeReceiver)
                context.unregisterReceiver(audioDeviceReceiver)
                context.unregisterReceiver(bluetoothReceiver)
                handler.removeCallbacks(batteryPollingRunnable)
            } catch (e: IllegalArgumentException) {
                // Receivers were not registered
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .animateContentSize()
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
                            imageVector = Icons.Outlined.Error,
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

                    // "Audio will play on" header with Android 16 style
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        ),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 0.dp,
                                    bottomEnd = 0.dp
                                )
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.streaming_to),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = activeDevice?.name ?: stringResource(R.string.this_phone),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = when (activeDevice?.type) {
                                    AudioDeviceType.BLUETOOTH -> Icons.Filled.Bluetooth
                                    AudioDeviceType.WIRED_HEADPHONES -> Icons.Filled.Headphones
                                    AudioDeviceType.USB_HEADSET -> Icons.Filled.Usb
                                    AudioDeviceType.HDMI -> Icons.Filled.Tv
                                    AudioDeviceType.EXTERNAL_SPEAKER -> Icons.Filled.Speaker
                                    else -> Icons.Filled.PhoneAndroid
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
//
//                    // Title
//                    Text(
//                        text = "Volume",
//                        style = MaterialTheme.typography.titleLarge,
//                        color = MaterialTheme.colorScheme.onSurface,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.padding(bottom = 12.dp)
//                    )

                    // Media Volume Control
                    VolumeControlRow(
                        label = stringResource(R.string.volume),
                        icon = Icons.Filled.MusicNote,
                        volume = currentVolume,
                        maxVolume = maxVolume,
                        onVolumeChange = { newVolume ->
                            currentVolume = newVolume
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                newVolume.toInt(),
                                0
                            )
                        },
                        onDragStart = { isUserDragging = true },
                        onDragEnd = { isUserDragging = false }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Audio Quality Selector
                    AudioQualitySelector(context)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bottom section with battery info and Done button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Battery percentage for Bluetooth devices (styled like a button)
                        if (activeDevice?.type == AudioDeviceType.BLUETOOTH && activeDevice.batteryLevel != null) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = when {
                                            activeDevice.batteryLevel >= 80 -> Icons.Filled.BatteryFull
                                            activeDevice.batteryLevel >= 50 -> Icons.Filled.Battery6Bar
                                            activeDevice.batteryLevel >= 30 -> Icons.Filled.Battery4Bar
                                            activeDevice.batteryLevel >= 10 -> Icons.Filled.Battery2Bar
                                            else -> Icons.Filled.Battery1Bar
                                        },
                                        contentDescription = stringResource(R.string.battery_content_desc),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "${activeDevice.batteryLevel}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // Done button
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(stringResource(R.string.done))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VolumeControlRow(
    label: String,
    icon: ImageVector,
    volume: Float,
    maxVolume: Int,
    onVolumeChange: (Float) -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val sliderState = rememberSliderState(
        valueRange = 0f..maxVolume.toFloat(),
        onValueChangeFinished = {
            onDragEnd()
        }
    )

    val snapAnimationSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    var currentValue by rememberSaveable { mutableFloatStateOf(volume) }
    var animateJob: Job? by remember { mutableStateOf(null) }

    // Update currentValue when external volume changes
    LaunchedEffect(volume) {
        if (!sliderState.isDragging) {
            currentValue = volume
            sliderState.value = volume
        }
    }

    sliderState.shouldAutoSnap = false
    sliderState.onValueChange = { newValue ->
        currentValue = newValue
        // only update the sliderState instantly if dragging
        if (sliderState.isDragging) {
            onDragStart()
            animateJob?.cancel()
            sliderState.value = newValue
            onVolumeChange(newValue)
        }
    }

    sliderState.onValueChangeFinished = {
        animateJob = coroutineScope.launch {
            animate(
                initialValue = sliderState.value,
                targetValue = currentValue,
                animationSpec = snapAnimationSpec
            ) { value, _ ->
                sliderState.value = value
            }
        }
        onDragEnd()
    }

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Slider(
            state = sliderState,
            modifier = Modifier
                .fillMaxWidth()
                .progressSemantics(
                    currentValue,
                    sliderState.valueRange.start..sliderState.valueRange.endInclusive,
                    0
                ),
            interactionSource = interactionSource,
            track = {
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(36.dp),
                    trackCornerSize = 12.dp
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AudioQualitySelector(context: Context) {
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        key = AudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.audio_quality_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .fillMaxWidth()
        )

        val options = listOf(
            stringResource(R.string.audio_quality_auto),
            stringResource(R.string.audio_quality_very_high),
            stringResource(R.string.audio_quality_high),
            stringResource(R.string.audio_quality_very_high),
            stringResource(R.string.audio_quality_low)
        )
        val selectedIndex = when (audioQuality) {
            AudioQuality.AUTO -> 0
            AudioQuality.HIGH -> 1
            AudioQuality.VERY_HIGH -> 2
            AudioQuality.LOW -> 3
        }

        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEachIndexed { index, label ->
                ToggleButton(
                    checked = selectedIndex == index,
                    onCheckedChange = {
                        val newQuality = when (index) {
                            0 -> AudioQuality.AUTO
                            1 -> AudioQuality.HIGH
                            2 -> AudioQuality.VERY_HIGH
                            else -> AudioQuality.LOW
                        }
                        onAudioQualityChange(newQuality)
                        applyAudioQuality(context, newQuality)
                    },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun loadDevices(context: Context, onSuccess: (List<AudioDevice>) -> Unit, onError: (String) -> Unit) {
    try {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = mutableListOf<AudioDevice>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Use AudioDeviceInfo API for API 23+
            val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

            var hasActiveDevice = false

            audioDevices.forEach { deviceInfo ->
                val device = when (deviceInfo.type) {
                    // Inside loadDevices() where you create AudioDevice for Bluetooth:
                    // Inside loadDevices() where you create AudioDevice for Bluetooth:
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                        val batteryLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31+
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

                                    // Find matching Bluetooth device by name
                                    val btDevice = pairedDevices?.find {
                                        it.name == deviceInfo.productName.toString()
                                    }

                                    @SuppressLint("MissingPermission")
                                    val battery = btDevice?.let { device ->
                                        try {
                                            // Try to get battery using reflection
                                            val method = android.bluetooth.BluetoothDevice::class.java.getMethod(
                                                "getBatteryLevel"
                                            )
                                            val level = method.invoke(device) as? Int
                                            Log.d("BatteryDebug", "Device: ${device.name}, Battery: $level")
                                            level
                                        } catch (e: NoSuchMethodException) {
                                            Log.e("BatteryDebug", "getBatteryLevel method not found")
                                            null
                                        } catch (e: Exception) {
                                            Log.e("BatteryDebug", "Error: ${e.message}")
                                            null
                                        }
                                    }

                                    if (battery != null && battery >= 0 && battery <= 100) battery else null
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                Log.e("AudioDevice", "Error getting battery level", e)
                                null
                            }
                        } else {
                            null
                        }

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

            // Determine active device
            val activeDevice = determineActiveDevice(audioManager, audioDevices)
            val updatedDevices = devices.map { device ->
                device.copy(isActive = device.deviceId == activeDevice?.id)
            }

            hasActiveDevice = updatedDevices.any { it.isActive }

            // If no active device detected, mark phone speaker as active
            val finalDevices = if (!hasActiveDevice) {
                val phoneSpeaker = updatedDevices.find { it.type == AudioDeviceType.PHONE_SPEAKER }
                if (phoneSpeaker != null) {
                    updatedDevices.map {
                        if (it.type ==
                            AudioDeviceType.PHONE_SPEAKER
                        ) {
                            it.copy(isActive = true)
                        } else {
                            it
                        }
                    }
                } else {
                    updatedDevices
                }
            } else {
                updatedDevices
            }

            onSuccess(finalDevices.sortedByDescending { it.isActive })
        } else {
            // Fallback for older APIs
            loadDevicesLegacy(context, onSuccess, onError)
        }
    } catch (e: Exception) {
        onError("Failed to load devices: ${e.message}")
    }
}

private fun determineActiveDevice(audioManager: AudioManager, audioDevices: Array<AudioDeviceInfo>): AudioDeviceInfo? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // Check various audio manager states
        when {
            // Check if any Bluetooth A2DP device is present in the list (assuming it is connected if present)
            audioDevices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP } ->
                audioDevices.find { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }

            // Check if any Wired Headset/Headphones are present
            audioDevices.any {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
            } ->
                audioDevices.find {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                }

            else ->
                audioDevices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        }
    } else {
        null
    }

@Suppress("DEPRECATION")
private fun loadDevicesLegacy(context: Context, onSuccess: (List<AudioDevice>) -> Unit, onError: (String) -> Unit) {
    // Your existing loadDevices implementation for older APIs
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val devices = mutableListOf<AudioDevice>()

    // Add devices based on AudioManager state
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

    onSuccess(devices)
}

private fun checkBluetoothPermission(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED
} else {
    true
}
