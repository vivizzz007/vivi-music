@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.music.vivi.vivimusic

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.key
import com.music.vivi.vivimusic.shapes.RoundedStarShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
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
import androidx.compose.material3.CircularWavyProgressIndicator
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
import com.music.vivi.R
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.constants.AudioQuality
import com.music.vivi.constants.AudioQualityKey
import com.music.vivi.utils.rememberEnumPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class BluetoothDeviceKind {
    HEADSET,  // earbuds, headphones, hearing aids
    SPEAKER,  // BT speakers
    UNKNOWN   // fallback
}

data class AudioDevice(
    val name: String,
    val type: AudioDeviceType,
    val isConnected: Boolean,
    val isActive: Boolean = false,
    val batteryLevel: Int? = null,
    val deviceId: Int? = null,
    val address: String? = null,
    val bluetoothKind: BluetoothDeviceKind? = null,
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

    val isDualOutput by (service?.isDualOutputEnabled ?: remember { MutableStateFlow(false) }).collectAsState()
    val primaryVol by (service?.primaryDeviceVolume ?: remember { MutableStateFlow(1f) }).collectAsState()
    val secondaryVol by (service?.secondaryDeviceVolume ?: remember { MutableStateFlow(1f) }).collectAsState()

    fun refreshDevices() {
        loadDevices(context, service?.preferredDeviceId, service?.secondaryPreferredDeviceId, onSuccess = { devices ->
            audioDevices = devices
        }, onError = {})
    }

    fun toggleDevice(dev: AudioDevice) {
        try {
            val currentActives = audioDevices.filter { it.isActive }
            val btDevices = audioDevices.filter { it.type == AudioDeviceType.BLUETOOTH }

            // Dual output is only allowed when all connected BT devices are the same kind
            // (e.g., all HEADSET or all SPEAKER). Different kinds = single-select only.
            val btKinds = btDevices.mapNotNull { it.bluetoothKind }
                .filter { it != BluetoothDeviceKind.UNKNOWN }
                .distinct()
            val allSameKind = btKinds.size <= 1

            val newPrimaryId: Int?
            val newSecondaryId: Int?

            if (dev.isActive) {
                // Deselecting a device
                val remaining = currentActives.filter { it.deviceId != dev.deviceId }
                newPrimaryId = remaining.firstOrNull()?.deviceId
                newSecondaryId = null
            } else if (allSameKind && dev.type == AudioDeviceType.BLUETOOTH &&
                       currentActives.all { it.type == AudioDeviceType.BLUETOOTH } &&
                       currentActives.isNotEmpty()) {
                // Same-kind BT: allow multi-select (up to 2)
                if (currentActives.size < 2) {
                    newPrimaryId = currentActives.first().deviceId
                    newSecondaryId = dev.deviceId
                } else {
                    // Replace secondary with newly selected
                    newPrimaryId = currentActives[0].deviceId
                    newSecondaryId = dev.deviceId
                }
            } else {
                // Different-kind BT or mixed types: single-select (radio button)
                // Deselect everything else, select only this device
                newPrimaryId = dev.deviceId
                newSecondaryId = null
            }

            service?.switchAudioDevice(newPrimaryId, newSecondaryId)
            refreshDevices()
        } catch (e: Exception) {
            Log.e("AudioDeviceBottomSheet", "Error toggling audio device", e)
        }
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadDevices(context, service?.preferredDeviceId, service?.secondaryPreferredDeviceId, onSuccess = { devices ->
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
            loadDevices(context, service?.preferredDeviceId, service?.secondaryPreferredDeviceId, onSuccess = { devices ->
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
                .verticalScroll(rememberScrollState())
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
                    val activeDevices = audioDevices.filter { it.isActive }
                    val isDualActive = isDualOutput && activeDevices.size >= 2
                    val activeDevice = activeDevices.firstOrNull()
                    val hasBluetooth = audioDevices.any { it.type == AudioDeviceType.BLUETOOTH }
                    // Dual output only allowed when all BT devices are the same kind
                    val btKinds = audioDevices
                        .filter { it.type == AudioDeviceType.BLUETOOTH }
                        .mapNotNull { it.bluetoothKind }
                        .filter { it != BluetoothDeviceKind.UNKNOWN }
                        .distinct()
                    val allSameKindBt = btKinds.size <= 1

                    LaunchedEffect(audioDevices) {
                        if (audioDevices.size > 1 && !showDevicePopup) {
                            showDevicePopup = true
                        }
                    }

                    val displayDevice = if (isDualActive && activeDevices.size >= 2) {
                        val name1 = if (audioDevices.count { it.name == activeDevices[0].name } > 1) "${activeDevices[0].name} (1)" else activeDevices[0].name
                        val name2 = if (audioDevices.count { it.name == activeDevices[1].name } > 1) "${activeDevices[1].name} (2)" else activeDevices[1].name
                        activeDevice?.copy(name = "$name1 + $name2") ?: activeDevice
                    } else {
                        activeDevice
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        displayDevice?.let { device ->
                            // Tappable device row — shows chevron hint when Bluetooth is available
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = androidx.compose.ui.graphics.Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(
                                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AudioDeviceRow(
                                        device = device,
                                        currentVolume = currentVolume,
                                        maxVolume = maxVolume,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = hasBluetooth,
                                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(),
                                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally()
                                    ) {
                                        val chevronRotation by animateFloatAsState(
                                            targetValue = if (showDevicePopup) 180f else 0f,
                                            animationSpec = tween(durationMillis = 300),
                                            label = "chevron"
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 12.dp)
                                        ) {
                                            val isActive = device.isActive
                                            Surface(
                                                onClick = { showDevicePopup = !showDevicePopup },
                                                shape = CircleShape,
                                                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                tonalElevation = 2.dp,
                                                modifier = Modifier.size(72.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Filled.ExpandMore,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .graphicsLayer { rotationZ = chevronRotation },
                                                        tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Animated in-place device switcher panel
                        androidx.compose.animation.AnimatedVisibility(
                            visible = hasBluetooth && showDevicePopup,
                            enter = androidx.compose.animation.expandVertically(
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + androidx.compose.animation.fadeIn(tween(200)),
                            exit = androidx.compose.animation.shrinkVertically(
                                animationSpec = tween(250, easing = FastOutSlowInEasing)
                            ) + androidx.compose.animation.fadeOut(tween(150))
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.audio_devices),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                    )

                                    audioDevices.forEachIndexed { index, dev ->
                                        key(dev.deviceId) {
                                            val isSelected = dev.isActive
                                            val deviceIcon = when (dev.type) {
                                                AudioDeviceType.BLUETOOTH -> Icons.Filled.Bluetooth
                                                AudioDeviceType.WIRED_HEADPHONES -> Icons.Filled.Headphones
                                                AudioDeviceType.USB_HEADSET -> Icons.Filled.Usb
                                                AudioDeviceType.HDMI -> Icons.Filled.Tv
                                                AudioDeviceType.EXTERNAL_SPEAKER -> Icons.Filled.Speaker
                                                AudioDeviceType.PHONE_SPEAKER -> Icons.Filled.PhoneAndroid
                                            }
                                            
                                            val itemShape = remember(index, audioDevices.size) {
                                                when {
                                                    audioDevices.size == 1 -> RoundedCornerShape(24.dp)
                                                    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                                    index == audioDevices.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                                                    else -> RoundedCornerShape(4.dp)
                                                }
                                            }

                                            val devName = if (audioDevices.count { it.name == dev.name } > 1) {
                                                val idx = audioDevices.filter { it.name == dev.name }.indexOf(dev) + 1
                                                "${dev.name} ($idx)"
                                            } else dev.name

                                            Surface(
                                                onClick = {
                                                    toggleDevice(dev)
                                                },
                                                shape = itemShape,
                                                color = if (isSelected)
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                else
                                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(vertical = 1.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                                        .fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = deviceIcon,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp),
                                                        tint = if (isSelected)
                                                            MaterialTheme.colorScheme.onSecondaryContainer
                                                        else
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = if (dev.type == AudioDeviceType.PHONE_SPEAKER)
                                                            stringResource(R.string.this_phone) else devName,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = if (isSelected)
                                                            MaterialTheme.colorScheme.onSecondaryContainer
                                                        else
                                                            MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (dev.batteryLevel != null) {
                                                        Text(
                                                            text = "${dev.batteryLevel}%",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    // Show Checkbox for same-kind BT (multi-select), RadioButton otherwise
                                                    val canDualSelect = allSameKindBt &&
                                                        dev.type == AudioDeviceType.BLUETOOTH &&
                                                        audioDevices.count { it.type == AudioDeviceType.BLUETOOTH } > 1
                                                    if (canDualSelect) {
                                                        Checkbox(
                                                            checked = isSelected,
                                                            onCheckedChange = { toggleDevice(dev) }
                                                        )
                                                    } else {
                                                        RadioButton(
                                                            selected = isSelected,
                                                            onClick = { toggleDevice(dev) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { openSystemMediaOutput(context) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Speaker,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "System Multi-Output Settings",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (activeDevices.size >= 2) {
                        activeDevices.take(2).forEachIndexed { idx, dev ->
                            val isPhone = dev.type == AudioDeviceType.PHONE_SPEAKER
                            // Map volume by device ID, not positional index
                            val isPrimary = dev.deviceId == service?.preferredDeviceId
                            val devVol = if (isPhone) {
                                (currentVolume / maxVolume.toFloat()).coerceIn(0f, 1f)
                            } else if (isPrimary) primaryVol else secondaryVol

                            val devName = if (isPhone) {
                                stringResource(R.string.this_phone)
                            } else if (audioDevices.count { it.name == dev.name } > 1) {
                                val i = audioDevices.filter { it.name == dev.name }.indexOf(dev) + 1
                                "${dev.name} ($i)"
                            } else dev.name

                            VolumeControlRow(
                                label = devName,
                                icon = if (isPhone) Icons.Filled.PhoneAndroid else Icons.Filled.VolumeUp,
                                volume = devVol * maxVolume,
                                maxVolume = maxVolume,
                                onVolumeChange = { newVolume ->
                                    val frac = (newVolume / maxVolume.toFloat()).coerceIn(0f, 1f)
                                    // Use device ID to route volume to the correct device
                                    if (isPrimary) {
                                        service?.setPrimaryDeviceVolume(frac)
                                    } else {
                                        service?.setSecondaryDeviceVolume(frac)
                                    }
                                    if (isPhone) {
                                        currentVolume = newVolume
                                        val intVol = newVolume.toInt()
                                        if (intVol != audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) {
                                            audioManager.setStreamVolume(
                                                AudioManager.STREAM_MUSIC,
                                                intVol,
                                                0
                                            )
                                        }
                                    }
                                },
                                onDragStart = { isUserDragging = true },
                                onDragEnd = { isUserDragging = false }
                            )
                            if (idx == 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    } else {
                        val singleDev = activeDevices.firstOrNull()
                        val isPhone = singleDev?.type == AudioDeviceType.PHONE_SPEAKER
                        val singleLabel = if (isPhone) {
                            stringResource(R.string.this_phone)
                        } else {
                            singleDev?.name ?: stringResource(R.string.volume)
                        }
                        VolumeControlRow(
                            label = singleLabel,
                            icon = if (isPhone) Icons.Filled.PhoneAndroid else Icons.Filled.MusicNote,
                            volume = currentVolume,
                            maxVolume = maxVolume,
                            onVolumeChange = { newVolume ->
                                currentVolume = newVolume
                                val intVol = newVolume.toInt()
                                if (intVol != audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) {
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        intVol,
                                        0
                                    )
                                }
                            },
                            onDragStart = { isUserDragging = true },
                            onDragEnd = { isUserDragging = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    AudioQualitySelector(context)

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeDevice?.type == AudioDeviceType.BLUETOOTH && activeDevice.batteryLevel != null) {
                            val density = LocalDensity.current
                            val strokeWidthPx = with(density) { 4.dp.toPx() }
                            val wavyStroke = remember(strokeWidthPx) {
                                Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                            }

                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularWavyProgressIndicator(
                                    progress = { activeDevice.batteryLevel.toFloat() / 100f },
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                                    stroke = wavyStroke,
                                    trackStroke = wavyStroke,
                                    gapSize = 3.dp
                                )
                                Text(
                                    text = "${activeDevice.batteryLevel}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

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
    var currentValue by remember(volume) { mutableFloatStateOf(volume) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(volume) {
        if (!isDragging) {
            currentValue = volume
        }
    }

    // Android 15 Style Volume Pill
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.CenterStart) {
            // Smoothly animate the fill width when not dragging
            val animatedVolumeFraction by animateFloatAsState(
                targetValue = (currentValue / maxVolume.toFloat()).coerceIn(0f, 1f),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "VolumeFillAnimation"
            )
            val displayFraction = if (isDragging) (currentValue / maxVolume.toFloat()).coerceIn(0f, 1f) else animatedVolumeFraction

            // Custom Pill Slider for perfect 0-100% fill
            val widthState = remember { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { widthState.floatValue = it.width.toFloat() }
                    .pointerInput(maxVolume) {
                        detectTapGestures { offset ->
                            if (widthState.floatValue > 0f) {
                                val percent = (offset.x / widthState.floatValue).coerceIn(0f, 1f)
                                val newValue = percent * maxVolume
                                currentValue = newValue
                                onVolumeChange(newValue)
                            }
                        }
                    }
                    .pointerInput(maxVolume) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isDragging = true
                                onDragStart()
                            },
                            onDragEnd = {
                                isDragging = false
                                onDragEnd()
                            },
                            onDragCancel = {
                                isDragging = false
                                onDragEnd()
                            }
                        ) { change, _ ->
                            change.consume()
                            if (widthState.floatValue > 0f) {
                                val percent = (change.position.x / widthState.floatValue).coerceIn(0f, 1f)
                                val newValue = percent * maxVolume
                                currentValue = newValue
                                onVolumeChange(newValue)
                            }
                        }
                    }
            ) {
                // Active Track (Fill) - Instant while dragging, animated when idle
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(displayFraction)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            }

            // Content overlay (Icon, Label and Value)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = if (currentValue > 0) icon else Icons.Filled.VolumeOff,
                        contentDescription = null,
                        tint = if (currentValue / maxVolume.toFloat() > 0.2f) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (currentValue / maxVolume.toFloat() > 0.4f) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${((currentValue / maxVolume.toFloat()).coerceIn(0f, 1f) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (currentValue / maxVolume.toFloat() > 0.85f) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
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
            stringResource(R.string.audio_quality_high),
            stringResource(R.string.audio_quality_low)
        )
        val selectedIndex = when (audioQuality) {
            AudioQuality.AUTO -> 0
            AudioQuality.HIGH -> 1
            AudioQuality.LOW -> 2
            else -> 0
        }

        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { index, label ->
                ToggleButton(
                    checked = selectedIndex == index,
                    onCheckedChange = {
                        val newQuality = when (index) {
                            0 -> AudioQuality.AUTO
                            1 -> AudioQuality.HIGH
                            else -> AudioQuality.LOW
                        }
                        onAudioQualityChange(newQuality)
                        applyAudioQuality(context, newQuality)
                    },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes(
                            shape = ButtonGroupDefaults.connectedLeadingButtonShape,
                            checkedShape = ButtonGroupDefaults.connectedLeadingButtonShape
                        )
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes(
                            shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                            checkedShape = ButtonGroupDefaults.connectedTrailingButtonShape
                        )
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes(
                            shape = RoundedCornerShape(4.dp),
                            checkedShape = RoundedCornerShape(4.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
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

fun applyAudioQuality(context: Context, quality: AudioQuality) {
    // Ported from alpha - logic can be added here if needed
}

private fun isBluetoothDeviceType(type: Int): Boolean = when (type) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_HEARING_AID -> true
    else -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER)) {
            true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && type == AudioDeviceInfo.TYPE_BLE_BROADCAST) {
            true
        } else if (type == 26 || type == 27 || type == 30) {
            true
        } else {
            false
        }
    }
}

@SuppressLint("MissingPermission")
private fun classifyBluetoothDevice(
    audioDeviceType: Int?,
    btDevice: BluetoothDevice?,
    deviceName: String
): BluetoothDeviceKind {
    // 1. Use AudioDeviceInfo.type if available (most reliable)
    if (audioDeviceType != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (audioDeviceType == AudioDeviceInfo.TYPE_BLE_HEADSET) return BluetoothDeviceKind.HEADSET
            if (audioDeviceType == AudioDeviceInfo.TYPE_BLE_SPEAKER) return BluetoothDeviceKind.SPEAKER
        }
        if (audioDeviceType == AudioDeviceInfo.TYPE_HEARING_AID) return BluetoothDeviceKind.HEADSET
    }

    // 2. Use BluetoothClass.Device major/minor if available
    if (btDevice != null) {
        try {
            val btClass = btDevice.bluetoothClass
            if (btClass != null) {
                val deviceClass = btClass.deviceClass
                when (deviceClass) {
                    0x0404, // AUDIO_VIDEO_WEARABLE_HEADSET
                    0x0418  // AUDIO_VIDEO_HEADPHONES
                    -> return BluetoothDeviceKind.HEADSET
                    0x0414, // AUDIO_VIDEO_LOUDSPEAKER
                    0x0420  // AUDIO_VIDEO_PORTABLE_AUDIO
                    -> return BluetoothDeviceKind.SPEAKER
                }
            }
        } catch (_: Exception) { }
    }

    // 3. Name-based heuristics as fallback
    val lowerName = deviceName.lowercase()
    val speakerKeywords = listOf("speaker", " go ", "go 4", "go 3", "go 2", "flip", "charge", "boom",
        "xtreme", "pulse", "clip", "soundlink", "soundcore", "megaboom", "wonderboom",
        "marshall", "harman", "bose s", "jbl ", "sonos", "alexa", "echo", "homepod")
    val headsetKeywords = listOf("buds", "pods", "headphone", "earbud", "earphone", "headset",
        "airpods", "wf-", "wh-", "galaxy buds", "freebuds", "enco", "bullets",
        "ear (", "ear(", "hear", "in-ear", "neckband")

    for (keyword in headsetKeywords) {
        if (lowerName.contains(keyword)) return BluetoothDeviceKind.HEADSET
    }
    for (keyword in speakerKeywords) {
        if (lowerName.contains(keyword)) return BluetoothDeviceKind.SPEAKER
    }

    return BluetoothDeviceKind.UNKNOWN
}

private fun loadDevices(
    context: Context,
    preferredDeviceId: Int?,
    secondaryPreferredDeviceId: Int? = null,
    onSuccess: (List<AudioDevice>) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = mutableListOf<AudioDevice>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val allAudioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()

            allAudioDevices.forEach { deviceInfo ->
                val isPhoneModel = deviceInfo.productName?.toString()?.let { name ->
                    name.equals(Build.MODEL, ignoreCase = true) ||
                    name.equals(Build.DEVICE, ignoreCase = true) ||
                    name.equals(Build.PRODUCT, ignoreCase = true)
                } ?: false

                val device = when {
                    isBluetoothDeviceType(deviceInfo.type) -> {
                        if (isPhoneModel) null
                        else {
                            // Find matching bonded BluetoothDevice for battery + classification
                            val btDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                            ) {
                                try {
                                    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                                    val bluetoothAdapter = bluetoothManager.adapter
                                    bluetoothAdapter?.bondedDevices?.find {
                                        it.name == deviceInfo.productName.toString() ||
                                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && it.address == deviceInfo.address) ||
                                        (deviceInfo.productName != null && it.name != null &&
                                            (it.name.contains(deviceInfo.productName.toString(), ignoreCase = true) ||
                                             deviceInfo.productName.toString().contains(it.name, ignoreCase = true)))
                                    }
                                } catch (_: Exception) { null }
                            } else null

                            val batteryLevel = try {
                                @SuppressLint("MissingPermission")
                                val battery = btDevice?.let { dev ->
                                    try {
                                        val method = android.bluetooth.BluetoothDevice::class.java.getMethod("getBatteryLevel")
                                        val level = method.invoke(dev) as? Int
                                        level
                                    } catch (_: Exception) { null }
                                }
                                if (battery != null && battery >= 0 && battery <= 100) battery else null
                            } catch (_: Exception) { null }

                            val deviceName = deviceInfo.productName?.toString() ?: "Bluetooth Device"
                            val kind = classifyBluetoothDevice(deviceInfo.type, btDevice, deviceName)

                            AudioDevice(
                                name = deviceName,
                                type = AudioDeviceType.BLUETOOTH,
                                isConnected = true,
                                isActive = false,
                                batteryLevel = batteryLevel,
                                deviceId = deviceInfo.id,
                                address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) deviceInfo.address else null,
                                bluetoothKind = kind
                            )
                        }
                    }

                    deviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || deviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADSET -> {
                        AudioDevice(
                            name = context.getString(R.string.wired_headphones),
                            type = AudioDeviceType.WIRED_HEADPHONES,
                            isConnected = true,
                            isActive = false,
                            deviceId = deviceInfo.id
                        )
                    }
                    deviceInfo.type == AudioDeviceInfo.TYPE_USB_HEADSET || deviceInfo.type == AudioDeviceInfo.TYPE_USB_DEVICE -> {
                        AudioDevice(
                            name = deviceInfo.productName?.toString() ?: "USB Audio",
                            type = AudioDeviceType.USB_HEADSET,
                            isConnected = true,
                            isActive = false,
                            deviceId = deviceInfo.id
                        )
                    }
                    deviceInfo.type == AudioDeviceInfo.TYPE_HDMI -> {
                        AudioDevice(
                            name = context.getString(R.string.hdmi),
                            type = AudioDeviceType.HDMI,
                            isConnected = true,
                            isActive = false,
                            deviceId = deviceInfo.id
                        )
                    }
                    deviceInfo.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
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

            // Also search bonded Bluetooth devices to surface any connected second headset (including LE Audio)
            if (checkBluetoothPermission(context)) {
                try {
                    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    val bluetoothAdapter = bluetoothManager?.adapter
                    val pairedDevices = bluetoothAdapter?.bondedDevices
                    val profilesToCheck = intArrayOf(
                        BluetoothProfile.A2DP,
                        BluetoothProfile.HEADSET,
                        22, // BluetoothProfile.LE_AUDIO
                        21, // BluetoothProfile.HEARING_AID
                        25, // CSIP_SET_MEMBER
                        28, // HAP_CLIENT
                    )
                    pairedDevices?.forEach { btDevice ->
                        val isConnected = try {
                            profilesToCheck.any { profile ->
                                try {
                                    bluetoothManager?.getConnectionState(btDevice, profile) == BluetoothProfile.STATE_CONNECTED
                                } catch (_: Throwable) { false }
                            } || (try {
                                profilesToCheck.any { profile ->
                                    try {
                                        bluetoothManager?.getConnectedDevices(profile)?.any { it.address == btDevice.address } == true
                                    } catch (_: Throwable) { false }
                                }
                            } catch (_: Throwable) { false }) || (try {
                                BluetoothDevice::class.java.getMethod("isConnected").invoke(btDevice) as? Boolean ?: false
                            } catch (_: Throwable) { false })
                        } catch (_: Throwable) { false }

                        if (isConnected) {
                            val alreadyAdded = devices.any {
                                it.name.equals(btDevice.name, ignoreCase = true) ||
                                (it.address != null && it.address == btDevice.address) ||
                                (btDevice.name != null && (it.name.contains(btDevice.name!!, ignoreCase = true) || btDevice.name!!.contains(it.name, ignoreCase = true)))
                            }
                            if (!alreadyAdded) {
                                val matchedDevice = allAudioDevices.find { devInfo ->
                                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && devInfo.address == btDevice.address) ||
                                    devInfo.productName?.toString().equals(btDevice.name, ignoreCase = true) ||
                                    (devInfo.productName != null && btDevice.name != null &&
                                        (devInfo.productName.toString().contains(btDevice.name!!, ignoreCase = true) ||
                                         btDevice.name!!.contains(devInfo.productName.toString(), ignoreCase = true)))
                                }
                                val battery = try {
                                    val method = BluetoothDevice::class.java.getMethod("getBatteryLevel")
                                    method.invoke(btDevice) as? Int
                                } catch (_: Exception) { null }

                                val devName = btDevice.name ?: "Bluetooth Device"
                                val kind = classifyBluetoothDevice(matchedDevice?.type, btDevice, devName)

                                devices.add(
                                    AudioDevice(
                                        name = devName,
                                        type = AudioDeviceType.BLUETOOTH,
                                        isConnected = true,
                                        isActive = false,
                                        batteryLevel = if (battery != null && battery >= 0 && battery <= 100) battery else null,
                                        deviceId = matchedDevice?.id ?: btDevice.hashCode(),
                                        address = btDevice.address,
                                        bluetoothKind = kind
                                    )
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            val activeDevice = determineActiveDevice(audioManager, allAudioDevices, preferredDeviceId)
            val updatedDevices = devices.map { device ->
                val isActive = (device.deviceId != null && (device.deviceId == preferredDeviceId || device.deviceId == secondaryPreferredDeviceId)) ||
                        (preferredDeviceId == null && secondaryPreferredDeviceId == null && device.deviceId == activeDevice?.id)
                device.copy(isActive = isActive)
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

            onSuccess(sortedDevices.distinctBy { it.deviceId?.toString() ?: "${it.name}_${it.address ?: it.hashCode()}" })
        } else {
            loadDevicesLegacy(context, onSuccess, onError)
        }
    } catch (e: Exception) {
        onError("Failed to load devices: ${e.message}")
    }
}

private fun determineActiveDevice(
    audioManager: AudioManager,
    audioDevices: List<AudioDeviceInfo>,
    preferredDeviceId: Int?
): AudioDeviceInfo? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val preferred = if (preferredDeviceId != null) {
            audioDevices.find { it.id == preferredDeviceId }
        } else null

        preferred ?: when {
            audioDevices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP } ->
                audioDevices.find { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            audioDevices.any { isBluetoothDeviceType(it.type) } ->
                audioDevices.find { isBluetoothDeviceType(it.type) }
            audioDevices.any {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
            } ->
                audioDevices.find {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                }
            audioDevices.any { it.type == AudioDeviceInfo.TYPE_USB_HEADSET || it.type == AudioDeviceInfo.TYPE_USB_DEVICE } ->
                audioDevices.find { it.type == AudioDeviceInfo.TYPE_USB_HEADSET || it.type == AudioDeviceInfo.TYPE_USB_DEVICE }
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

@Composable
private fun AudioDeviceRow(
    device: AudioDevice,
    currentVolume: Float,
    maxVolume: Int,
    modifier: Modifier = Modifier
) {
    val isActiveDevice = device.isActive
    
    val containerColor = if (isActiveDevice) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val onContainer = if (isActiveDevice) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    val scallopShape = RoundedStarShape(sides = 8, curve = 0.10, rotation = 0f)

    val backgroundScale by animateFloatAsState(
        targetValue = if (isActiveDevice) 1.10f else 1f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "activeDeviceScale"
    )

    val deviceIcon = when (device.type) {
        AudioDeviceType.BLUETOOTH -> Icons.Filled.Bluetooth
        AudioDeviceType.WIRED_HEADPHONES -> Icons.Filled.Headphones
        AudioDeviceType.USB_HEADSET -> Icons.Filled.Usb
        AudioDeviceType.HDMI -> Icons.Filled.Tv
        AudioDeviceType.EXTERNAL_SPEAKER -> Icons.Filled.Speaker
        else -> Icons.Filled.Speaker
    }

    Surface(
        modifier = modifier
            .clip(CircleShape),
        color = containerColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .padding(start = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer(
                            scaleX = backgroundScale,
                            scaleY = backgroundScale
                        )
                        .background(
                            color = onContainer.copy(alpha = 0.12f),
                            shape = if (isActiveDevice) scallopShape else CircleShape
                        )
                )

                Icon(
                    imageVector = deviceIcon,
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val statusText = if (isActiveDevice) "Connected" else "Available"
                
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(onContainer.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = statusText,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium,
                        overflow = TextOverflow.Ellipsis,
                        color = onContainer
                    )
                }
            }

            if (isActiveDevice) {
                val value = ((currentVolume / maxVolume) * 100).toInt()
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = "Volume level",
                        tint = onContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$value%",
                        style = MaterialTheme.typography.labelSmall,
                        color = onContainer
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceSelector(
    devices: List<AudioDevice>,
    onDeviceSelect: (AudioDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        devices.forEachIndexed { index, device ->
            val isSelected = device.isActive
            val deviceIcon = when (device.type) {
                AudioDeviceType.BLUETOOTH -> Icons.Filled.Bluetooth
                AudioDeviceType.WIRED_HEADPHONES -> Icons.Filled.Headphones
                AudioDeviceType.USB_HEADSET -> Icons.Filled.Usb
                AudioDeviceType.HDMI -> Icons.Filled.Tv
                AudioDeviceType.EXTERNAL_SPEAKER -> Icons.Filled.Speaker
                AudioDeviceType.PHONE_SPEAKER -> Icons.Filled.PhoneAndroid
            }

            ToggleButton(
                checked = isSelected,
                onCheckedChange = { if (!isSelected) onDeviceSelect(device) },
                shapes = when {
                    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes(
                        shape = ButtonGroupDefaults.connectedLeadingButtonShape,
                        checkedShape = ButtonGroupDefaults.connectedLeadingButtonShape
                    )
                    index == devices.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes(
                        shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                        checkedShape = ButtonGroupDefaults.connectedTrailingButtonShape
                    )
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes(
                        shape = RoundedCornerShape(4.dp),
                        checkedShape = RoundedCornerShape(4.dp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = deviceIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (device.type == AudioDeviceType.PHONE_SPEAKER) stringResource(R.string.this_phone) else device.name,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

fun openSystemMediaOutput(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Intent("com.android.settings.panel.action.MEDIA_OUTPUT").apply {
            putExtra("com.android.settings.panel.extra.PACKAGE_NAME", context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } else {
        Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }
}
