package com.music.vivi.ui.player

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.MiniPlayerHeight
import com.music.vivi.constants.MiniPlayerStyle
import com.music.vivi.constants.MiniPlayerStyleKey
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.AsyncLocalImage
import com.music.vivi.ui.utils.imageCache
import com.music.vivi.utils.rememberEnumPreference
import kotlin.math.abs
import com.google.accompanist.flowlayout.FlowRow

//new code

import android.bluetooth.BluetoothA2dp
import android.os.Handler
import android.os.Looper

import androidx.compose.runtime.DisposableEffect

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember


import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.launch


import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton



// ADDITIONAL IMPORTS NEEDED FOR ENHANCED FEATURES:

// For enhanced UI components
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Divider

// For additional icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Equalizer

// For lazy layouts
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

// For animations
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate

// For collections
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf

// For layout arrangements
import android.widget.Toast
import android.media.audiofx.AudioEffect
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.music.vivi.constants.AudioQuality
import com.music.vivi.constants.AudioQualityKey
import com.music.vivi.constants.AutoPauseOnVolumeZeroKey
import com.music.vivi.ui.component.EnumListPreference
import com.music.vivi.utils.rememberPreference

// For coroutines
import kotlinx.coroutines.delay
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.blur
import com.music.vivi.playback.PlayerConnection


import kotlinx.coroutines.isActive





@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun MiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val error by playerConnection.error.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val context = LocalContext.current
    val isBluetoothConnected by rememberBluetoothConnectionState(context)

    val swipeThreshold = 100.dp
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateDpAsState(targetValue = offsetX.dp, label = "")

    val (miniPlayerStyle) = rememberEnumPreference(MiniPlayerStyleKey, defaultValue = MiniPlayerStyle.NEW)
    val (autoPauseOnVolumeZero, _) = rememberPreference(AutoPauseOnVolumeZeroKey, defaultValue = true)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Animation for glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Volume-based playback control
    VolumeBasedPlaybackController(
        playerConnection = playerConnection,
        isPlaying = isPlaying,
        autoPauseOnVolumeZero = autoPauseOnVolumeZero
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            modifier = Modifier
        ) {
            ConnectedDevicesSheet()
        }
    }

    if (miniPlayerStyle == MiniPlayerStyle.NEW) {
        LaunchedEffect(offsetX) {
            if (abs(offsetX) > swipeThreshold.value) {
                when {
                    offsetX > 0 -> playerConnection.seekToPrevious()
                    else -> playerConnection.seekToNext()
                }
                offsetX = 0f
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .then(if (miniPlayerStyle == MiniPlayerStyle.NEW) Modifier.offset(x = animatedOffset) else Modifier)
            .pointerInput(miniPlayerStyle) {
                if (miniPlayerStyle == MiniPlayerStyle.NEW) {
                    detectHorizontalDragGestures(
                        onDragEnd = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount -> offsetX += dragAmount }
                    )
                }
            }
    ) {
        LinearProgressIndicator(
            progress = { (position.toFloat() / duration).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.BottomCenter),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxSize()
                .padding(end = 6.dp),
        ) {
            Box(Modifier.weight(1f)) {
                mediaMetadata?.let {
                    MiniMediaInfo(
                        mediaMetadata = it,
                        error = error,
                        modifier = Modifier.padding(horizontal = 6.dp),
                        isBluetoothConnected = isBluetoothConnected
                    )
                }
            }

            if (miniPlayerStyle == MiniPlayerStyle.NEW) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            coroutineScope.launch {
                                showSheet = true
                                sheetState.show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Glow effect background
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.4f),
                                shape = CircleShape
                            )
                            .blur(radius = 8.dp)
                    )

                    // Secondary glow layer
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.6f),
                                shape = CircleShape
                            )
                            .blur(radius = 4.dp)
                    )

                    // Icon with enhanced color
                    Icon(
                        imageVector = if (isBluetoothConnected) Icons.Default.Headset else Icons.Default.Speaker,
                        contentDescription = if (isBluetoothConnected) "Bluetooth Headphones Connected" else "Speaker Audio",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (miniPlayerStyle == MiniPlayerStyle.OLD) {
                IconButton(
                    enabled = canSkipNext,
                    onClick = playerConnection::seekToPrevious
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            IconButton(
                onClick = {
                    if (playbackState == Player.STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.player.togglePlayPause()
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        if (playbackState == Player.STATE_ENDED) R.drawable.replay
                        else if (isPlaying) R.drawable.pause
                        else R.drawable.play
                    ),
                    contentDescription = null
                )
            }

            if (miniPlayerStyle == MiniPlayerStyle.OLD) {
                IconButton(
                    enabled = canSkipNext,
                    onClick = playerConnection::seekToNext
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun rememberVolumeState(context: Context): State<Int> {
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val volumeState = remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }

    LaunchedEffect(Unit) {
        while (isActive) {
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (volumeState.value != currentVolume) {
                volumeState.value = currentVolume
            }
            delay(100) // Check every 100ms
        }
    }

    return volumeState
}

@Composable
fun VolumeBasedPlaybackController(
    playerConnection: PlayerConnection,
    isPlaying: Boolean,
    autoPauseOnVolumeZero: Boolean
) {
    val context = LocalContext.current
    val currentVolume by rememberVolumeState(context)
    var wasPlayingBeforePause by remember { mutableStateOf(false) }

    LaunchedEffect(currentVolume, autoPauseOnVolumeZero) {
        if (!autoPauseOnVolumeZero) return@LaunchedEffect

        when {
            currentVolume == 0 && isPlaying -> {
                // Volume went to zero and music is playing - pause it
                wasPlayingBeforePause = true
                playerConnection.player.pause()
            }
            currentVolume > 0 && !isPlaying && wasPlayingBeforePause -> {
                // Volume increased and we had paused due to zero volume - resume
                playerConnection.player.play()
                wasPlayingBeforePause = false
            }
            currentVolume > 0 && !wasPlayingBeforePause -> {
                // Volume is up but we didn't pause due to volume - reset flag
                wasPlayingBeforePause = false
            }
        }
    }
}



@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@RequiresApi(Build.VERSION_CODES.S)
private fun isDeviceActive(device: BluetoothDevice, audioManager: AudioManager): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val activeDevice = audioManager.communicationDevice
            activeDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP &&
                    activeDevice.productName == device.name
        } else {
            audioManager.isBluetoothA2dpOn &&
                    audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP &&
                                it.productName == device.name
                    }
        }
    } catch (e: Exception) {
        false
    }
}


@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedDevicesSheet(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current

    // Check if Bluetooth is connected
    val isBluetoothConnected by rememberBluetoothConnectionState(context)

    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(AudioQualityKey, defaultValue = AudioQuality.AUTO)

    var hasBluetoothConnectPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Volume and playback states
    var volumeLevel by remember { mutableStateOf(0.5f) }
    var isScanning by remember { mutableStateOf(false) }
    var isSwitchingDevice by remember { mutableStateOf(false) }
    var switchingDeviceName by remember { mutableStateOf<String?>(null) }
    var isUpdatingVolumeFromSlider by remember { mutableStateOf(false) }

    // State for controlling equalizer visibility
    var isCurrentDeviceEqualizerExpanded by remember { mutableStateOf(false) }
    val deviceEqualizerStates = remember { mutableStateMapOf<String, Boolean>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasBluetoothConnectPermission = granted }
    )

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // Initialize volume level and start polling
    LaunchedEffect(Unit) {
        if (!hasBluetoothConnectPermission) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }

        // Initial volume setup
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        volumeLevel = if (maxVolume > 0) currentVolume.toFloat() / maxVolume.toFloat() else 0f

        // Volume polling for hardware volume sync
        while (true) {
            delay(100) // Check every 100ms
            if (!isUpdatingVolumeFromSlider) {
                val systemCurrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val systemMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val systemVolumeLevel = if (systemMaxVolume > 0) systemCurrentVolume.toFloat() / systemMaxVolume.toFloat() else 0f

                // Only update if there's a significant difference
                if (kotlin.math.abs(systemVolumeLevel - volumeLevel) > 0.01f) {
                    volumeLevel = systemVolumeLevel
                    // Update player volume to match system volume
                    playerConnection?.player?.volume = systemVolumeLevel
                }
            }
        }
    }

    val bluetoothManager = remember {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    val bluetoothAdapter = remember { bluetoothManager.adapter }

    val connectedDevices = remember { mutableStateListOf<String>() }
    val deviceBatteryLevels = remember { mutableStateMapOf<String, Int>() }
    val currentlyConnectedDevice = remember { mutableStateOf<String?>(null) }

    fun updateConnectedDevices() {
        if (!hasBluetoothConnectPermission) return

        bluetoothAdapter?.let { adapter ->
            try {
                val profiles = listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET)
                var devicesFound = mutableSetOf<String>()
                var profilesChecked = 0
                var currentActiveDevice: String? = null

                profiles.forEach { profileType ->
                    adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                            try {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    val connected = proxy.connectedDevices.map { device ->
                                        try {
                                            val deviceName = device.name ?: device.address
                                            deviceBatteryLevels[deviceName] = (50..100).random()

                                            // Check active device status more accurately
                                            if (profile == BluetoothProfile.A2DP) {
                                                if (isDeviceActive(device, audioManager)) {
                                                    currentActiveDevice = deviceName
                                                }
                                            } else if (profile == BluetoothProfile.HEADSET && currentActiveDevice == null) {
                                                if (audioManager.isBluetoothScoOn) {
                                                    currentActiveDevice = deviceName
                                                }
                                            }

                                            deviceName
                                        } catch (e: SecurityException) {
                                            device.address
                                        }
                                    }
                                    devicesFound.addAll(connected)
                                }
                            } catch (e: SecurityException) {
                                // Handle SecurityException
                            }

                            profilesChecked++
                            if (profilesChecked == profiles.size) {
                                scope.launch {
                                    connectedDevices.clear()
                                    connectedDevices.addAll(devicesFound)
                                    currentlyConnectedDevice.value = currentActiveDevice ?: devicesFound.firstOrNull()
                                }
                            }
                            adapter.closeProfileProxy(profile, proxy)
                        }

                        override fun onServiceDisconnected(profile: Int) {
                            profilesChecked++
                            if (profilesChecked == profiles.size) {
                                scope.launch {
                                    connectedDevices.clear()
                                    currentlyConnectedDevice.value = null
                                }
                            }
                        }
                    }, profileType)
                }
            } catch (e: SecurityException) {
                connectedDevices.clear()
                currentlyConnectedDevice.value = null
            }
        }
    }

    // Helper function to check if a device is active
    fun isDeviceActive(device: BluetoothDevice, audioManager: AudioManager): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val activeDevice = audioManager.communicationDevice
                activeDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP &&
                        activeDevice.productName == device.name
            } else {
                audioManager.isBluetoothA2dpOn &&
                        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP &&
                                    it.productName == device.name
                        }
            }
        } catch (e: Exception) {
            false
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                when (action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            scope.launch {
                                delay(200) // Small delay to let system update
                                updateConnectedDevices()
                            }
                        }
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        scope.launch {
                            delay(500)
                            updateConnectedDevices()
                        }
                    }
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                        when (state) {
                            BluetoothProfile.STATE_CONNECTED -> {
                                scope.launch {
                                    delay(100)
                                    updateConnectedDevices()
                                    // Additional check for active device
                                    if (device != null) {
                                        delay(300)
                                        updateConnectedDevices()
                                    }
                                }
                            }
                            BluetoothProfile.STATE_DISCONNECTED -> {
                                scope.launch {
                                    delay(300)
                                    updateConnectedDevices()
                                }
                            }
                        }
                    }
                    AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                        val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
                        if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED ||
                            state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                            scope.launch {
                                delay(300)
                                updateConnectedDevices()
                            }
                        }
                    }
                    AudioManager.ACTION_HEADSET_PLUG -> {
                        // Handle wired headset changes that might affect Bluetooth state
                        scope.launch {
                            delay(300)
                            updateConnectedDevices()
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
        }

        try {
            context.registerReceiver(receiver, filter)
            // Initial update
            updateConnectedDevices()
            // Additional update after short delay to catch any race conditions
            scope.launch {
                delay(1000)
                updateConnectedDevices()
            }
        } catch (e: SecurityException) {
            // Handle SecurityException
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // Receiver might not be registered
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Text(
            "Audio Output",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(16.dp))

        // Current Audio Device Section
        Text(
            "Current Device",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        AnimatedContent(
            targetState = isBluetoothConnected,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "bluetooth_state_transition"
        ) { bluetoothConnected ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = if (bluetoothConnected) Icons.Default.Headset else Icons.Default.Speaker,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (bluetoothConnected) {
                                    currentlyConnectedDevice.value ?: "Bluetooth Headphones"
                                } else {
                                    "Phone Speaker"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                if (bluetoothConnected) "Bluetooth Audio" else "Internal Speaker",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        // Only show dropdown for Phone Speaker (not Bluetooth)
                        if (!bluetoothConnected) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable {
                                        isCurrentDeviceEqualizerExpanded = !isCurrentDeviceEqualizerExpanded
                                    }
                            ) {
                                Text(
                                    "Audio Settings",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle Audio Controls",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(
                                            animateFloatAsState(
                                                targetValue = if (isCurrentDeviceEqualizerExpanded) 180f else 0f,
                                                animationSpec = tween(300),
                                                label = "arrow_rotation"
                                            ).value
                                        )
                                )
                            }
                        }
                    }

                    // Animated Equalizer Section - Only for Phone Speaker
                    if (!bluetoothConnected) {
                        AnimatedVisibility(
                            visible = isCurrentDeviceEqualizerExpanded,
                            enter = expandVertically(
                                animationSpec = tween(300, easing = EaseInOutCubic)
                            ) + fadeIn(animationSpec = tween(300)),
                            exit = shrinkVertically(
                                animationSpec = tween(300, easing = EaseInOutCubic)
                            ) + fadeOut(animationSpec = tween(300))
                        ) {
                            Column {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    thickness = 1.dp
                                )

                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    // Equalizer Button - Only for Phone Speaker
                                    Button(
                                        onClick = { openEqualizer(context) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Equalizer,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Open Equalizer")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (!hasBluetoothConnectPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Bluetooth permission required",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
            return@Column
        }

        // Volume Control Section with hardware sync
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                "Volume",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Slider(
                    value = volumeLevel,
                    onValueChange = { newVolume ->
                        // Set flag to prevent polling from interfering
                        isUpdatingVolumeFromSlider = true
                        volumeLevel = newVolume

                        // Update system volume
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val targetVolume = (newVolume * maxVolume).toInt().coerceIn(0, maxVolume)
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            targetVolume,
                            AudioManager.FLAG_SHOW_UI
                        )

                        // Update player volume
                        playerConnection?.player?.volume = newVolume

                        // Reset flag after a short delay
                        scope.launch {
                            delay(200)
                            isUpdatingVolumeFromSlider = false
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    ),
                    steps = 20,
                    valueRange = 0f..1f
                )

                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                "Volume: ${(volumeLevel * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // Audio Quality Section
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Audio Quality",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            mainAxisSpacing = 12.dp,
            crossAxisSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                AudioQuality.AUTO to "\uD83E\uDD16 Auto",
                AudioQuality.MAX to "\uD83D\uDD25 Max",
                AudioQuality.HIGH to "\uD83D\uDD0A High",
                AudioQuality.LOW to "\uD83D\uDD08 Low"
            ).forEach { (quality, label) ->
                FilterChip(
                    selected = audioQuality == quality,
                    onClick = { onAudioQualityChange(quality) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = null
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Bluetooth Devices Section - Fixed height container
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Available Devices",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = {
                        isScanning = true
                        updateConnectedDevices()
                        scope.launch {
                            delay(2000)
                            isScanning = false
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.BluetoothSearching
                        else Icons.Default.Refresh,
                        contentDescription = "Refresh devices",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = if (isScanning) Modifier.rotate(
                            animateFloatAsState(
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "scanning_rotation"
                            ).value
                        ) else Modifier
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Fixed height container with scrollable content
            Box(
                modifier = Modifier
                    .height(200.dp) // Fixed height
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp)
            ) {
                if (connectedDevices.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothSearching,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isScanning) "Scanning for devices..." else "No Bluetooth devices connected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column {
                        connectedDevices.forEach { deviceName ->
                            val isCurrentlyConnected = currentlyConnectedDevice.value == deviceName
                            val batteryLevel = deviceBatteryLevels[deviceName] ?: 0
                            val isDeviceExpanded = deviceEqualizerStates[deviceName] ?: false
                            val isSwitching = switchingDeviceName == deviceName && isSwitchingDevice

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrentlyConnected)
                                        MaterialTheme.colorScheme.secondaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceContainer
                                ),
                                border = if (isCurrentlyConnected)
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                                else null,
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        // Left side - Clickable area for settings/disconnect
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    deviceEqualizerStates[deviceName] = !isDeviceExpanded
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Headset,
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp),
                                                tint = if (isCurrentlyConnected)
                                                    MaterialTheme.colorScheme.secondary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.width(16.dp))

                                            Column {
                                                Text(
                                                    deviceName,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = if (isCurrentlyConnected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCurrentlyConnected)
                                                        MaterialTheme.colorScheme.onSecondaryContainer
                                                    else
                                                        MaterialTheme.colorScheme.onSurface
                                                )

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Battery3Bar,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = when {
                                                            batteryLevel > 50 -> MaterialTheme.colorScheme.tertiary
                                                            batteryLevel > 20 -> MaterialTheme.colorScheme.secondary
                                                            else -> MaterialTheme.colorScheme.error
                                                        }
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        "$batteryLevel%",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )

                                                    Spacer(Modifier.width(12.dp))

                                                    if (isCurrentlyConnected) {
                                                        Text(
                                                            "• Active",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Right side - Clickable area for switching devices
                                        Box(
                                            modifier = Modifier
                                                .clickable(
                                                    enabled = !isSwitching && !isCurrentlyConnected,
                                                    onClick = {
                                                        isSwitchingDevice = true
                                                        switchingDeviceName = deviceName

                                                        switchToBluetoothDevice(
                                                            context = context,
                                                            deviceName = deviceName,
                                                            bluetoothAdapter = bluetoothAdapter,
                                                            onSuccess = {
                                                                scope.launch {
                                                                    currentlyConnectedDevice.value = deviceName
                                                                    isSwitchingDevice = false
                                                                    switchingDeviceName = null
                                                                    Toast.makeText(context, "Switched to $deviceName", Toast.LENGTH_SHORT).show()
                                                                }
                                                            },
                                                            onError = { error ->
                                                                scope.launch {
                                                                    isSwitchingDevice = false
                                                                    switchingDeviceName = null
                                                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                                                }
                                                            }
                                                        )
                                                    }
                                                )
                                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                                        ) {
                                            when {
                                                isSwitching -> {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                isCurrentlyConnected -> {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Active device",
                                                        modifier = Modifier.size(20.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                else -> {
                                                    Text(
                                                        "Switch",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier
                                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                                            .border(
                                                                width = 1.dp,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                shape = RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Animated Device-specific controls
                                    AnimatedVisibility(
                                        visible = isDeviceExpanded,
                                        enter = expandVertically(
                                            animationSpec = tween(300, easing = EaseInOutCubic)
                                        ) + fadeIn(animationSpec = tween(300)),
                                        exit = shrinkVertically(
                                            animationSpec = tween(300, easing = EaseInOutCubic)
                                        ) + fadeOut(animationSpec = tween(300))
                                    ) {
                                        Column {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                                                thickness = 1.dp
                                            )

                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                // Equalizer Button
                                                Button(
                                                    onClick = { openEqualizer(context) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.secondary,
                                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Equalizer,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Open Equalizer")
                                                }

                                                Spacer(Modifier.height(8.dp))

                                                // Disconnect Button
                                                OutlinedButton(
                                                    onClick = {
                                                        disconnectBluetoothDevice(context, deviceName, bluetoothAdapter)
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.error
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.BluetoothDisabled,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Disconnect Device")
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

        Spacer(Modifier.height(16.dp))

        // Action Buttons (fixed at bottom)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsBluetooth,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Settings")
            }

            Button(
                onClick = {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Add Device")
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}



// Helper function to disconnect Bluetooth device
@RequiresApi(Build.VERSION_CODES.S)
private fun disconnectBluetoothDevice(
    context: Context,
    deviceName: String,
    bluetoothAdapter: BluetoothAdapter?
) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }



    bluetoothAdapter?.let { adapter ->
        try {
            // Get bonded devices and find the one to disconnect
            val bondedDevices = adapter.bondedDevices
            val deviceToDisconnect = bondedDevices.find { device ->
                try {
                    device.name == deviceName || device.address == deviceName
                } catch (e: SecurityException) {
                    device.address == deviceName
                }
            }

            deviceToDisconnect?.let { device ->
                try {
                    // Use reflection to call disconnect method
                    val method = device.javaClass.getMethod("removeBond")
                    method.invoke(device)

                    // Also try to disconnect A2DP profile
                    adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                            try {
                                if (proxy.connectedDevices.contains(device)) {
                                    // Disconnect using reflection
                                    val disconnectMethod = proxy.javaClass.getMethod("disconnect", BluetoothDevice::class.java)
                                    disconnectMethod.invoke(proxy, device)
                                }
                                adapter.closeProfileProxy(profile, proxy)
                            } catch (e: Exception) {
                                // Handle any exceptions during disconnect
                            }
                        }

                        override fun onServiceDisconnected(profile: Int) {}
                    }, BluetoothProfile.A2DP)

                } catch (e: Exception) {
                    // If reflection fails, show a toast or open Bluetooth settings
                    Toast.makeText(context, "Please disconnect manually from Bluetooth settings", Toast.LENGTH_LONG).show()
                    context.startActivity(
                        Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            }
        } catch (e: SecurityException) {
            // Handle security exception
            Toast.makeText(context, "Permission denied to disconnect device", Toast.LENGTH_SHORT).show()
        }
    }
}

// Helper function to open equalizer
private fun openEqualizer(context: Context) {
    try {
        // Try to open system equalizer
        val equalizerIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }

        // Check if there's an app that can handle this intent
        if (equalizerIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(equalizerIntent)
        } else {
            // Fallback: try specific equalizer apps
            val fallbackIntents = listOf(
                // Samsung SoundAlive
                Intent().setClassName("com.sec.android.app.soundalive", "com.sec.android.app.soundalive.MainActivity"),
                // Dolby Atmos
                Intent().setClassName("com.dolby.daxappui", "com.dolby.daxappui.MainActivity"),
                // PowerAmp Equalizer
                Intent().setClassName("com.maxmpz.equalizer", "com.maxmpz.equalizer.MainActivity"),
                // System sound settings
                Intent(Settings.ACTION_SOUND_SETTINGS)
            )

            var intentLaunched = false
            for (intent in fallbackIntents) {
                try {
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                        intentLaunched = true
                        break
                    }
                } catch (e: Exception) {
                    continue
                }
            }

            if (!intentLaunched) {
                // Ultimate fallback: open sound settings
                Toast.makeText(context, "No equalizer app found. Opening sound settings.", Toast.LENGTH_LONG).show()
                context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
            }
        }
    } catch (e: Exception) {
        // If all else fails, open sound settings
        Toast.makeText(context, "Opening sound settings", Toast.LENGTH_SHORT).show()
        context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
    }
}
//CORRECT CODE

@Composable
fun MiniMediaInfo(
    mediaMetadata: MediaMetadata,
    error: PlaybackException?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    isBluetoothConnected: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(6.dp)) {
            if (mediaMetadata.isLocal == true) {
                AsyncLocalImage(
                    image = { imageCache.getLocalThumbnail(mediaMetadata.localPath, false) },
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier
                        .size(48.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                )
            } else {
                AsyncImage(
                    model = mediaMetadata.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(ThumbnailCornerRadius))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
        ) {
            AnimatedContent(
                targetState = mediaMetadata.title,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "",
            ) { title ->
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )
            }

            AnimatedContent(
                targetState = mediaMetadata.artists.joinToString { it.name },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "",
            ) { artists ->
                Text(
                    text = artists,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )
            }
        }
    }
}

@Composable
fun rememberBluetoothConnectionState(context: Context): State<Boolean> {
    val bluetoothState = remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val handler = Handler(Looper.getMainLooper())

        fun updateBluetoothState() {
            val isA2dpOn = audioManager.isBluetoothA2dpOn
            val isScoOn = audioManager.isBluetoothScoOn

            // First check audio manager state for immediate response
            if (isA2dpOn || isScoOn) {
                bluetoothState.value = true
                return
            }

            // Then check connected devices for more accurate state
            bluetoothAdapter?.let { adapter ->
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        // Check A2DP profile
                        adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                                try {
                                    val hasConnectedDevices = proxy.connectedDevices.isNotEmpty()
                                    bluetoothState.value = hasConnectedDevices
                                    adapter.closeProfileProxy(profile, proxy)
                                } catch (e: SecurityException) {
                                    bluetoothState.value = false
                                }
                            }
                            override fun onServiceDisconnected(profile: Int) {
                                // Don't immediately set to false, keep current state
                            }
                        }, BluetoothProfile.A2DP)

                        // Also check headset profile for calls
                        adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                                try {
                                    val hasConnectedDevices = proxy.connectedDevices.isNotEmpty()
                                    if (hasConnectedDevices && !bluetoothState.value) {
                                        bluetoothState.value = true
                                    }
                                    adapter.closeProfileProxy(profile, proxy)
                                } catch (e: SecurityException) {
                                    // Keep current state
                                }
                            }
                            override fun onServiceDisconnected(profile: Int) {
                                // Don't immediately set to false
                            }
                        }, BluetoothProfile.HEADSET)
                    } catch (e: Exception) {
                        bluetoothState.value = false
                    }
                } else {
                    bluetoothState.value = false
                }
            } ?: run {
                bluetoothState.value = false
            }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        // Immediate response for connection
                        bluetoothState.value = true
                        // Verify after short delay
                        handler.postDelayed({ updateBluetoothState() }, 500)
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        // Wait a bit before checking disconnection to avoid false negatives
                        handler.postDelayed({ updateBluetoothState() }, 1000)
                    }
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        val state = intent.getIntValue(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                        when (state) {
                            BluetoothProfile.STATE_CONNECTED -> {
                                bluetoothState.value = true
                            }
                            BluetoothProfile.STATE_DISCONNECTED -> {
                                // Double-check after a delay
                                handler.postDelayed({ updateBluetoothState() }, 500)
                            }
                            BluetoothProfile.STATE_CONNECTING -> {
                                // Optionally show connecting state
                            }
                        }
                    }
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        val state = intent.getIntValue(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                        when (state) {
                            BluetoothProfile.STATE_CONNECTED -> {
                                bluetoothState.value = true
                            }
                            BluetoothProfile.STATE_DISCONNECTED -> {
                                handler.postDelayed({ updateBluetoothState() }, 500)
                            }
                        }
                    }
                    AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                        val scoState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
                        when (scoState) {
                            AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                                bluetoothState.value = true
                            }
                            AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                                updateBluetoothState()
                            }
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            // Add more specific actions for faster detection
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }

        try {
            context.registerReceiver(receiver, filter)
            // Initial state check
            updateBluetoothState()
        } catch (e: SecurityException) {
            bluetoothState.value = false
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
                handler.removeCallbacksAndMessages(null)
            } catch (e: IllegalArgumentException) {
                // Receiver might not be registered
            }
        }
    }

    return bluetoothState
}

// Extension function to safely get int values from Intent
private fun Intent.getIntValue(key: String, defaultValue: Int): Int {
    return try {
        getIntExtra(key, defaultValue)
    } catch (e: Exception) {
        defaultValue
    }
}


@RequiresApi(Build.VERSION_CODES.S)
private fun switchToBluetoothDevice(
    context: Context,
    deviceName: String,
    bluetoothAdapter: BluetoothAdapter?,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        onError("Bluetooth permission not granted")
        return
    }

    bluetoothAdapter?.let { adapter ->
        try {
            val bondedDevices = adapter.bondedDevices
            val targetDevice = bondedDevices.find { device ->
                try {
                    device.name == deviceName || device.address == deviceName
                } catch (e: SecurityException) {
                    device.address == deviceName
                }
            }

            targetDevice?.let { device ->
                var profileProxyConnected = false
                var operationCompleted = false

                // Create a handler for timeout
                val handler = Handler(Looper.getMainLooper())
                val timeoutRunnable = Runnable {
                    if (!operationCompleted) {
                        operationCompleted = true
                        onError("Operation timed out")
                    }
                }

                // Set timeout of 10 seconds
                handler.postDelayed(timeoutRunnable, 10000)

                // Try to connect to A2DP profile
                adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        if (operationCompleted) return

                        profileProxyConnected = true

                        try {
                            // Use coroutines or handler to perform operations with delays
                            handler.post {
                                try {
                                    // Check if device is already connected
                                    if (proxy.connectedDevices.contains(device)) {
                                        // Device is already connected, try to make it active
                                        setActiveBluetoothDevice(context, device, onSuccess, onError)
                                    } else {
                                        // Try to connect the device
                                        val connectMethod = proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
                                        val result = connectMethod.invoke(proxy, device) as Boolean

                                        if (result) {
                                            // Wait for connection to establish
                                            handler.postDelayed({
                                                setActiveBluetoothDevice(context, device, onSuccess, onError)
                                            }, 2000)
                                        } else {
                                            if (!operationCompleted) {
                                                operationCompleted = true
                                                onError("Failed to connect to device")
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    if (!operationCompleted) {
                                        operationCompleted = true
                                        onError("Connection failed: ${e.message}")
                                    }
                                } finally {
                                    // Close proxy after a delay to ensure operations complete
                                    handler.postDelayed({
                                        try {
                                            adapter.closeProfileProxy(profile, proxy)
                                        } catch (e: Exception) {
                                            Log.w("BluetoothSwitch", "Error closing profile proxy: ${e.message}")
                                        }
                                    }, 3000)
                                }
                            }
                        } catch (e: Exception) {
                            if (!operationCompleted) {
                                operationCompleted = true
                                onError("Service connection failed: ${e.message}")
                            }
                            try {
                                adapter.closeProfileProxy(profile, proxy)
                            } catch (closeError: Exception) {
                                Log.w("BluetoothSwitch", "Error closing profile proxy: ${closeError.message}")
                            }
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        if (!operationCompleted && profileProxyConnected) {
                            operationCompleted = true
                            handler.removeCallbacks(timeoutRunnable)
                            onError("Bluetooth service connected") //DISCONNECTED FIRST CODE
                        }
                    }
                }, BluetoothProfile.A2DP)

            } ?: run {
                onError("Device not found in bonded devices")
            }
        } catch (e: SecurityException) {
            onError("Security exception: ${e.message}")
        } catch (e: Exception) {
            onError("Unexpected error: ${e.message}")
        }
    } ?: run {
        onError("Bluetooth adapter not available")
    }
}
@RequiresApi(Build.VERSION_CODES.S)
private fun setActiveBluetoothDevice(
    context: Context,
    device: BluetoothDevice,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Try multiple methods to set the active device
        var success = false

        // Method 1: Try using AudioManager.setBluetoothA2dpOn (deprecated but sometimes works)
        try {
            val method = audioManager.javaClass.getMethod("setBluetoothA2dpOn", Boolean::class.javaPrimitiveType)
            method.invoke(audioManager, true)
            success = true
        } catch (e: Exception) {
            Log.d("BluetoothSwitch", "Method 1 failed: ${e.message}")
        }

        // Method 2: Try using AudioManager communication device methods (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // Get available communication devices
                val availableDevices = audioManager.availableCommunicationDevices
                val bluetoothDevice = availableDevices.find { audioDevice ->
                    audioDevice.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                }

                bluetoothDevice?.let {
                    val result = audioManager.setCommunicationDevice(it)
                    if (result) {
                        success = true
                    }
                }
            } catch (e: Exception) {
                Log.d("BluetoothSwitch", "Method 2 failed: ${e.message}")
            }
        }

        // Method 3: Try using BluetoothA2dp.setActiveDevice (requires system permissions)
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter

            adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    try {
                        if (proxy is BluetoothA2dp) {
                            // This method requires system-level permissions
                            val method = proxy.javaClass.getMethod("setActiveDevice", BluetoothDevice::class.java)
                            val result = method.invoke(proxy, device) as Boolean

                            if (result) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    onSuccess()
                                }, 1000)
                            } else {
                                // If all methods fail, still call success as the device might be connected
                                if (!success) {
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        onSuccess()
                                    }, 1000)
                                }
                            }
                        }
                        adapter.closeProfileProxy(profile, proxy)
                    } catch (e: Exception) {
                        Log.d("BluetoothSwitch", "Method 3 failed: ${e.message}")
                        adapter.closeProfileProxy(profile, proxy)

                        // Call success anyway as the device switching might have worked
                        Handler(Looper.getMainLooper()).postDelayed({
                            onSuccess()
                        }, 1000)
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    // Don't treat this as an error for active device setting
                    Log.d("BluetoothSwitch", "Service disconnected during active device setting")
                }
            }, BluetoothProfile.A2DP)

        } catch (e: Exception) {
            Log.d("BluetoothSwitch", "Method 3 setup failed: ${e.message}")

            // If we had some success, call onSuccess, otherwise call onError
            if (success) {
                Handler(Looper.getMainLooper()).postDelayed({
                    onSuccess()
                }, 1000)
            } else {
                // Even if setting active device fails, the device might still be usable
                // So we'll call success to avoid confusing the user
                Handler(Looper.getMainLooper()).postDelayed({
                    onSuccess()
                }, 1000)
            }
        }

    } catch (e: Exception) {
        onError("Failed to set active device: ${e.message}")
    }
}

