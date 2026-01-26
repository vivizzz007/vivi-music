@file:Suppress("DEPRECATION")

package com.music.vivi.playback

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.MainActivity
import com.music.vivi.R
import com.music.vivi.constants.AudioNormalizationKey
import com.music.vivi.constants.AudioOffload
import com.music.vivi.constants.AudioQualityKey
import com.music.vivi.constants.AutoDownloadOnLikeKey
import com.music.vivi.constants.AutoLoadMoreKey
import com.music.vivi.constants.AutoSkipNextOnErrorKey
import com.music.vivi.constants.DisableLoadMoreWhenRepeatAllKey
import com.music.vivi.constants.DiscordUseDetailsKey
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.HistoryDuration
import com.music.vivi.constants.MediaSessionConstants.CommandToggleLike
import com.music.vivi.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.music.vivi.constants.MediaSessionConstants.CommandToggleShuffle
import com.music.vivi.constants.MediaSessionConstants.CommandToggleStartRadio
import com.music.vivi.constants.PauseListenHistoryKey
import com.music.vivi.constants.PersistentQueueKey
import com.music.vivi.constants.PowerSaverKey
import com.music.vivi.constants.PowerSaverPauseOnZeroVolumeKey
import com.music.vivi.constants.PlayerVolumeKey
import com.music.vivi.constants.RepeatModeKey
import com.music.vivi.constants.ShowLyricsKey
import com.music.vivi.constants.SimilarContent
import com.music.vivi.constants.SmartShuffleKey
import com.music.vivi.constants.SmartSuggestionsKey
import com.music.vivi.constants.SkipSilenceKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.Event
import com.music.vivi.db.entities.FormatEntity
import com.music.vivi.db.entities.LyricsEntity
import com.music.vivi.db.entities.RelatedSongMap
import com.music.vivi.di.DownloadCache
import com.music.vivi.di.PlayerCache
import com.music.vivi.extensions.SilentHandler
import com.music.vivi.extensions.collect
import com.music.vivi.extensions.collectLatest
import com.music.vivi.extensions.currentMetadata
import com.music.vivi.extensions.findNextMediaItemById
import com.music.vivi.extensions.mediaItems
import com.music.vivi.extensions.metadata
import com.music.vivi.extensions.setOffloadEnabled
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.toPersistQueue
import com.music.vivi.extensions.toQueue
import com.music.vivi.lyrics.LyricsHelper
import com.music.vivi.models.PersistPlayerState
import com.music.vivi.models.PersistQueue
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.Queue
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.playback.queues.filterExplicit
import com.music.vivi.utils.CoilBitmapLoader
import com.music.vivi.utils.DiscordRPC
import com.music.vivi.utils.NetworkConnectivityObserver
import com.music.vivi.utils.ScrobbleManager
import com.music.vivi.utils.SyncUtils
import com.music.vivi.utils.YTPlayerUtils
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.utils.reportException
import com.music.vivi.playback.managers.QueueManager
import com.music.vivi.playback.managers.IntegrationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okhttp3.OkHttpClient
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
// under testing
import android.content.Intent
import android.database.SQLException
import android.os.Build
import com.music.vivi.constants.HideVideoSongsKey
import com.music.vivi.constants.PauseOnHeadphonesDisconnectKey
import com.music.vivi.constants.PauseOnZeroVolumeKey
import com.music.vivi.constants.StopMusicOnTaskClearKey
import com.music.vivi.playback.queues.filterVideoSongs

import com.music.vivi.extensions.toEnum
import com.music.vivi.update.widget.ViviWidgetManager
import com.music.vivi.update.widget.MusicPlayerWidgetReceiver

/**
 * The core Service responsible for handling media playback, notification management,
 * and background audio focus.
 *
 * This service extends [MediaLibraryService] to provide a [MediaSession] to the system,
 * allowing external controls (Bluetooth, Android Auto, Lockscreen) to interact with the app.
 *
 * ## Architecture
 * The service functions as a central hub for playback logic, coordinating between:
 * - **ExoPlayer**: The underlying media player engine.
 * - **MediaSession**: The interface for external controllers.
 * - **QueueManager**: Manages the playlist and track transitions.
 * - **IntegrationManager**: Handles external integrations like Last.fm and Discord RPC.
 *
 * ## Lifecycle & Concurrency
 * - **SupervisorJob**: A [SupervisorJob] is used to manage coroutines. usage of [SupervisorJob] ensures that
 *   child coroutine failures do not crash the entire service scope.
 * - **Foreground Service**: The service runs in the foreground to prevent system killing during playback.
 *   Special handling is implemented for Android 12+ foreground service start restrictions in [safePlay].
 *
 * ## Key Responsibilities
 * - **Playback Management**: Initialization and control of [ExoPlayer].
 * - **Audio Focus**: Handling interruptions via [handleAudioFocusChange] (e.g., pausing when a call comes in).
 * - **State Persistence**: Saving the current queue and player state to disk to survive process death.
 * - **Network Resilience**: Observing connectivity changes to pause/resume or retry playback.
 *
 * @see MediaLibraryService
 * @see ExoPlayer
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@AndroidEntryPoint
class MusicService :
    MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {

    private var isVolumeReceiverRegistered = false
    private var isNoisyReceiverRegistered = false

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var syncUtils: SyncUtils

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    @Inject
    lateinit var widgetManager: ViviWidgetManager

    @Inject
    lateinit var queueManager: QueueManager

    @Inject
    lateinit var integrationManager: IntegrationManager

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var lastAudioFocusState = AudioManager.AUDIOFOCUS_NONE
    private var wasPlayingBeforeAudioFocusLoss = false
    private var wasPausedByZeroVolume = false
    private var hasAudioFocus = false
    private var reentrantFocusGain = false

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val binder = MusicBinder()

    private lateinit var connectivityManager: ConnectivityManager

    @Inject
    lateinit var connectivityObserver: NetworkConnectivityObserver
    val waitingForNetworkConnection = MutableStateFlow(false)
    private val isNetworkConnected = MutableStateFlow(false)

    private val addedSuggestionIds = Collections.synchronizedSet(LinkedHashSet<String>())

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                if (streamType == AudioManager.STREAM_MUSIC) {
                    val volume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
                    scope.launch {
                        val powerSaver = dataStore.get(PowerSaverKey, false)
                        val powerSaverPause = dataStore.get(PowerSaverPauseOnZeroVolumeKey, true)

                        // "Pause on Zero Volume" preference OR (PowerSaver AND PowerSaverPauseOnZeroVolume)
                        if (dataStore.get(PauseOnZeroVolumeKey, false) || (powerSaver && powerSaverPause)) {
                            if (volume == 0) {
                                if (player.isPlaying) {
                                    player.pause()
                                    wasPausedByZeroVolume = true
                                }
                            } else if (volume > 0) {
                                if (wasPausedByZeroVolume) {
                                    safePlay()
                                    wasPausedByZeroVolume = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                scope.launch {
                    if (dataStore.get(PauseOnHeadphonesDisconnectKey, false)) {
                        player.pause()
                    }
                }
            }
        }
    }

    @Inject
    lateinit var okHttpClient: OkHttpClient

    private lateinit var audioQuality: com.music.vivi.constants.AudioQuality

    val currentMediaMetadata = MutableStateFlow<com.music.vivi.models.MediaMetadata?>(null)
    private val currentSong =
        currentMediaMetadata
            .flatMapLatest { mediaMetadata ->
                database.song(mediaMetadata?.id)
            }.stateIn(scope, SharingStarted.Lazily, null)
    private val currentFormat =
        currentMediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    lateinit var playerVolume: MutableStateFlow<Float>

    lateinit var sleepTimer: SleepTimer

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession

    private var isAudioEffectSessionOpened = false
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var discordRpc: DiscordRPC? = null
    private var lastPlaybackSpeed = 1.0f
    private var discordUpdateJob: kotlinx.coroutines.Job? = null

    private var scrobbleManager: ScrobbleManager? = null

    val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())

    private var consecutivePlaybackErr = 0
    private val songUrlCache = HashMap<String, Pair<String, Long>>()
    private val retryCount = HashMap<String, Int>()
    private var retryJob: Job? = null
    private var globalRetryCount = 0

    /**
     * Initializes the service, creating the [ExoPlayer], [MediaSession], and setting up system integrations.
     *
     * This method performs the following critical setup:
     * 1.  **Service Lifecycle**: Marks the service as running.
     * 2.  **Notification Provider**: Configures the media notification appearance.
     * 3.  **ExoPlayer Builder**: Configures the player with [AudioAttributes] (Usage: MEDIA), wake mode, and renderers.
     * 4.  **Integrations**: Starts [IntegrationManager] (Last.fm, Discord).
     * 5.  **Media Session**: Builds the [MediaLibrarySession] with a [CoilBitmapLoader] for artwork.
     * 6.  **Broadcast Receivers**: Registers receivers for volume changes and "becoming noisy" (headphone unplug).
     * 7.  **State Restoration**: Asynchronously restores the last played queue and position from disk.
     */
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setListener(object : MediaSessionService.Listener {
                override fun onForegroundServiceStartNotAllowedException() {
                    Log.e(TAG, "Foreground service start not allowed")
                    // If the service cannot start in foreground, we must ensure we don't try to play
                    // creating an indeterminate state.
                    scope.launch {
                        if (player.isPlaying) {
                            player.pause()
                        }
                    }
                }
            })
        }
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player
            )
                .apply {
                    setSmallIcon(R.drawable.library_music)
                }
        )
        player =
            ExoPlayer
                .Builder(this)
                .setMediaSourceFactory(createMediaSourceFactory())
                .setRenderersFactory(createRenderersFactory())
                .setHandleAudioBecomingNoisy(false)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    false
                ).setSeekBackIncrementMs(5000)
                .setSeekForwardIncrementMs(5000)
                .build()
                .apply {
                    addListener(this@MusicService)
                    sleepTimer = SleepTimer(scope, this)
                    addListener(sleepTimer)
                    addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))
                    setOffloadEnabled(dataStore.get(AudioOffload, false))
                }

        // --- FIX: Setzen des Players im QueueManager ---
        queueManager.setPlayer(player)
        // ----------------------------------------------

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupAudioFocusRequest()

        integrationManager.start(scope, player)

        mediaLibrarySessionCallback.apply {
            toggleLike = ::toggleLike
            toggleStartRadio = ::toggleStartRadio
            toggleLibrary = ::toggleLibrary
        }
        mediaSession =
            MediaLibrarySession
                .Builder(this, player, mediaLibrarySessionCallback)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                ).setBitmapLoader(CoilBitmapLoader(this, scope))
                .build()
        player.repeatMode = dataStore.get(RepeatModeKey, REPEAT_MODE_OFF)

        // Keep a connected controller so that notification works
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())

        connectivityManager = getSystemService()!!
//        connectivityObserver = NetworkConnectivityObserver(this)

        if (!isVolumeReceiverRegistered) {
            registerReceiver(volumeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
            isVolumeReceiverRegistered = true
        }

        if (!isNoisyReceiverRegistered) {
            registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            isNoisyReceiverRegistered = true
        }

        audioQuality = dataStore.get(AudioQualityKey).toEnum(com.music.vivi.constants.AudioQuality.AUTO)
        playerVolume = MutableStateFlow(dataStore.get(PlayerVolumeKey, 1f).coerceIn(0f, 1f))

        queueManager.setScope(scope)

        scope.launch {
            connectivityObserver.networkStatus.collect { isConnected ->
                isNetworkConnected.value = isConnected
                if (isConnected) {
                    // Always clear waiting state if network is restored
                    if (waitingForNetworkConnection.value) {
                        Log.d(TAG, "Network restored, clearing waiting state")
                        waitingForNetworkConnection.value = false
                    }

                    // Trigger retry if player is idle and was previously in a network error state
                    if (player.playbackState == Player.STATE_IDLE &&
                        player.playerError != null &&
                        isNetworkError(player.playerError!!)
                    ) {
                        Log.d(TAG, "Network restored, retrying playback from IDLE")
                        if (player.currentMediaItem != null) {
                            delay(500)
                            player.prepare()
                            if (player.playWhenReady) {
                                safePlay()
                            }
                        }
                    }
                }
            }
        }

        playerVolume.collectLatest(scope) {
            player.volume = it
        }

        playerVolume.debounce(1000).collect(scope) { volume ->
            dataStore.edit { settings ->
                settings[PlayerVolumeKey] = volume
            }
        }

        currentSong.debounce(1000).collect(scope) { song ->
            updateNotification()
            if (song != null && player.playWhenReady && player.playbackState == Player.STATE_READY) {
                discordRpc?.updateSong(
                    song,
                    player.currentPosition,
                    player.playbackParameters.speed,
                    dataStore.get(DiscordUseDetailsKey, false)
                )
            } else {
                discordRpc?.closeRPC()
            }
        }

        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged()
        ) { mediaMetadata, showLyrics ->
            mediaMetadata to showLyrics
        }.collectLatest(scope) { (mediaMetadata, showLyrics) ->
            if (showLyrics &&
                mediaMetadata != null &&
                database.lyrics(mediaMetadata.id)
                    .first() == null
            ) {
                val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadata.id,
                            lyrics = lyrics
                        )
                    )
                }
            }
        }

        dataStore.data
            .map { it[SkipSilenceKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                player.skipSilenceEnabled = it
            }

        dataStore.data
            .map { it[SmartSuggestionsKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                if (enabled) {
                    checkAndLoadMoreItems(player.currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
                } else {
                    clearAutomix()
                }
            }

        combine(
            currentFormat,
            dataStore.data
                .map { it[AudioNormalizationKey] ?: true }
                .distinctUntilChanged()
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(scope) { (format, normalizeAudio) -> setupLoudnessEnhancer() }

        // Load persistent queue and state asynchronously to prevent main thread freeze (Fix #1)
        scope.launch(Dispatchers.IO) {
            if (dataStore.get(PersistentQueueKey, true)) {
                // Restore Queue
                val queueRestored = runCatching {
                    filesDir.resolve(PERSISTENT_QUEUE_FILE).inputStream().use { fis ->
                        ObjectInputStream(fis).use { oos ->
                            oos.readObject() as PersistQueue
                        }
                    }
                }.getOrNull()

                if (queueRestored != null) {
                    val restoredQueue = queueRestored.toQueue()
                    withContext(Dispatchers.Main) {
                        queueManager.playQueue(
                            queue = restoredQueue,
                            playWhenReady = false
                        )
                    }
                }

                // Restore Automix
                val automixRestored = runCatching {
                    filesDir.resolve(PERSISTENT_AUTOMIX_FILE).inputStream().use { fis ->
                        ObjectInputStream(fis).use { oos ->
                            oos.readObject() as PersistQueue
                        }
                    }
                }.getOrNull()

                if (automixRestored != null) {
                    val automixList = automixRestored.items.map { it.toMediaItem() }
                    withContext(Dispatchers.Main) {
                        automixItems.value = automixList
                    }
                }

                // Restore Player State
                val stateRestored = runCatching {
                    filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).inputStream().use { fis ->
                        ObjectInputStream(fis).use { oos ->
                            oos.readObject() as PersistPlayerState
                        }
                    }
                }.getOrNull()

                if (stateRestored != null) {
                    // Restore player settings after queue is loaded
                    withContext(Dispatchers.Main) {
                        // Small delay to ensure items are set? queueManager.playQueue sets items immediately.
                        // However, waiting a brief moments allows the Player to process the queue.
                        delay(200)
                        player.repeatMode = stateRestored.repeatMode
                        player.shuffleModeEnabled = stateRestored.shuffleModeEnabled
                        player.volume = stateRestored.volume

                        if (stateRestored.currentMediaItemIndex < player.mediaItemCount) {
                            player.seekTo(stateRestored.currentMediaItemIndex, stateRestored.currentPosition)
                        }
                    }
                }
            }
        }

        // Save queue periodically to prevent queue loss from crash or force kill
        scope.launch {
            while (isActive) {
                delay(30.seconds)
                if (dataStore.get(PersistentQueueKey, true)) {
                    saveQueueToDisk()
                }
            }
        }

        // Save queue more frequently when playing to ensure state is preserved
        scope.launch {
            while (isActive) {
                delay(10.seconds)
                if (dataStore.get(PersistentQueueKey, true) && player.isPlaying) {
                     saveQueueToDisk()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            MusicPlayerWidgetReceiver.ACTION_PLAY_PAUSE -> player.playPause()
            MusicPlayerWidgetReceiver.ACTION_LIKE -> toggleLike()
            MusicPlayerWidgetReceiver.ACTION_PLAY_SONG -> {
                intent.getStringExtra("song_id")?.let { id ->
                    scope.launch {
                        database.song(id).first()?.let { song ->
                            player.setMediaItem(song.toMediaItem())
                            player.prepare()
                            player.play()
                        }
                    }
                }
            }
            MusicPlayerWidgetReceiver.ACTION_PLAY_QUEUE_ITEM -> {
                val skipCount = intent.getIntExtra("skip_count", 0)
                if (skipCount > 0) {
                    val currentIndex = player.currentMediaItemIndex
                    val timeline = player.currentTimeline
                    if (!timeline.isEmpty) {
                        var targetIndex = currentIndex
                        repeat(skipCount) {
                            val nextIdx = timeline.getNextWindowIndex(
                                targetIndex,
                                player.repeatMode,
                                player.shuffleModeEnabled
                            )
                            if (nextIdx != androidx.media3.common.C.INDEX_UNSET) {
                                targetIndex = nextIdx
                            }
                        }
                        if (targetIndex != currentIndex) {
                            player.seekTo(targetIndex, 0)
                            player.play()
                        }
                    }
                }
            }
            MusicPlayerWidgetReceiver.ACTION_UPDATE_WIDGET -> updateWidget()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun Player.playPause() {
        if (isPlaying) pause() else play()
    }

    private fun setupAudioFocusRequest() {
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                handleAudioFocusChange(focusChange)
            }
            .setAcceptsDelayedFocusGain(true)
            .build()
    }

    /**
     * Handles audio focus changes to ensure correct playback behavior during interruptions.
     *
     * This method reacts to focus changes reported by the system [AudioManager]:
     *
     * - [AudioManager.AUDIOFOCUS_GAIN]: Regained focus. Resumes playback if it was paused transiently
     *   or lowers volume (ducking) was active.
     * - [AudioManager.AUDIOFOCUS_LOSS]: Permanent loss (e.g., another music app started).
     *   Pauses playback and abandons our focus request.
     * - [AudioManager.AUDIOFOCUS_LOSS_TRANSIENT]: Temporary loss (e.g., phone call).
     *   Pauses playback but keeps the focus request active to resume later.
     * - [AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK]: Temporary loss where we can keep playing
     *   at a reduced volume (e.g., notification sound).
     *
     * @param focusChange The integer code representing the new focus state.
     */
    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            -> {
                hasAudioFocus = true

                if (wasPlayingBeforeAudioFocusLoss && !player.isPlaying) {
                    safePlay()
                    wasPlayingBeforeAudioFocusLoss = false
                }

                player.volume = playerVolume.value
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) {
                    player.pause()
                }
                abandonAudioFocus()
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) {
                    player.pause()
                }
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) {
                    player.volume = (playerVolume.value * 0.2f)
                }
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                hasAudioFocus = true
                player.volume = playerVolume.value
                lastAudioFocusState = focusChange
            }
        }
    }

    /**
     * Attempts to start playback safely, handling Android 12+ foreground service restrictions.
     *
     * On Android 12 (API 31) and higher, starting a foreground service from the background is restricted.
     * If the app is in the background and attempts to start playback (which promotes the service to foreground),
     * a [android.app.ForegroundServiceStartNotAllowedException] may be thrown.
     *
     * This method wraps the [player.play] call in a try-catch block to gracefully handle this exception,
     * logging the error instead of crashing.
     *
     * @see android.app.ForegroundServiceStartNotAllowedException
     */
    private fun safePlay() {
        try {
            player.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback (ForegroundServiceStartNotAllowedException?)", e)
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true

        audioFocusRequest?.let { request ->
            val result = audioManager.requestAudioFocus(request)
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            return hasAudioFocus
        }
        return false
    }

    private fun abandonAudioFocus() {
        if (hasAudioFocus) {
            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
                hasAudioFocus = false
            }
        }
    }

    fun hasAudioFocusForPlayback(): Boolean = hasAudioFocus

    private fun waitOnNetworkError() {
        if (waitingForNetworkConnection.value) return

        if (globalRetryCount >= MAX_RETRY_COUNT) {
            Log.w(TAG, "Max retry count ($MAX_RETRY_COUNT) reached, stopping playback")
            stopOnError()
            globalRetryCount = 0
            waitingForNetworkConnection.value = false
            return
        }

        if (!isNetworkConnected.value) {
            waitingForNetworkConnection.value = true
        }

        // Start a retry timer with exponential backoff
        retryJob?.cancel()
        retryJob = scope.launch {
            // Exponential backoff: 3s, 6s, 12s, 24s... max 30s
            val delayMs = minOf(3000L * (1 shl globalRetryCount), 30000L)
            Log.d(TAG, "Waiting ${delayMs}ms before retry attempt ${globalRetryCount + 1}/$MAX_RETRY_COUNT")
            delay(delayMs)

            if (isNetworkConnected.value && waitingForNetworkConnection.value) {
                globalRetryCount++
                triggerRetry()
            }
        }
    }

    private fun triggerRetry() {
        waitingForNetworkConnection.value = false
        retryJob?.cancel()

        if (player.currentMediaItem != null) {
            // After 3+ failed retries, try to refresh the stream URL by seeking to current position
            // This forces ExoPlayer to re-resolve the data source and get a fresh URL
            if (globalRetryCount > 3) {
                Log.d(TAG, "Retry count > 3, attempting to refresh stream URL")
                val currentPosition = player.currentPosition
                player.seekTo(player.currentMediaItemIndex, currentPosition)
            }
            Log.d(TAG, "Triggering network retry")
            player.prepare()
            if (player.playWhenReady) {
                safePlay()
            }
        }
    }

    private fun skipOnError() {
        /**
         * Auto skip to the next media item on error.
         *
         * To prevent a "runaway diesel engine" scenario, force the user to take action after
         * too many errors come up too quickly. Pause to show player "stopped" state
         */
        consecutivePlaybackErr += 2
        val nextWindowIndex = player.nextMediaItemIndex

        if (consecutivePlaybackErr <= MAX_CONSECUTIVE_ERR && nextWindowIndex != C.INDEX_UNSET) {
            player.seekTo(nextWindowIndex, C.TIME_UNSET)
            player.prepare()
            safePlay()
            return
        }

        player.pause()
        consecutivePlaybackErr = 0
    }

    private fun stopOnError() {
        player.pause()
    }

    private fun updateNotification() {
        mediaSession.setCustomLayout(
            listOf(
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            if (currentSong.value?.song?.liked ==
                                true
                            ) {
                                R.string.action_remove_like
                            } else {
                                R.string.action_like
                            }
                        )
                    )
                    .setIconResId(
                        if (currentSong.value?.song?.liked ==
                            true
                        ) {
                            R.drawable.favorite
                        } else {
                            R.drawable.favorite_border
                        }
                    )
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(currentSong.value != null)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            when (player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> throw IllegalStateException()
                            }
                        )
                    ).setIconResId(
                        when (player.repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> throw IllegalStateException()
                        }
                    ).setSessionCommand(CommandToggleRepeatMode)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            if (player.shuffleModeEnabled) R.string.action_shuffle_off else R.string.action_shuffle_on
                        )
                    )
                    .setIconResId(if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(R.string.start_radio))
                    .setIconResId(R.drawable.radio)
                    .setSessionCommand(CommandToggleStartRadio)
                    .setEnabled(currentSong.value != null)
                    .build()
            )
        )
    }

    private suspend fun recoverSong(mediaId: String, playbackData: YTPlayerUtils.PlaybackData? = null) {
        val song = database.song(mediaId).first()
        val mediaMetadata = withContext(Dispatchers.Main) {
            player.findNextMediaItemById(mediaId)?.metadata
        } ?: return
        val duration = song?.song?.duration?.takeIf { it != -1 }
            ?: mediaMetadata.duration.takeIf { it != -1 }
            ?: (
                playbackData?.videoDetails ?: YTPlayerUtils.playerResponseForMetadata(mediaId)
                    .getOrNull()?.videoDetails
                )?.lengthSeconds?.toInt()
            ?: -1
        database.query {
            if (song == null) {
                insert(mediaMetadata.copy(duration = duration))
            } else if (song.song.duration == -1) {
                update(song.song.copy(duration = duration))
            }
        }
        if (!database.hasRelatedSongs(mediaId)) {
            val relatedEndpoint =
                YouTube.next(WatchEndpoint(videoId = mediaId)).getOrNull()?.relatedEndpoint
                    ?: return
            val relatedPage = YouTube.related(relatedEndpoint).getOrNull() ?: return
            database.query {
                relatedPage.songs
                    .map(SongItem::toMediaMetadata)
                    .onEach(::insert)
                    .map {
                        RelatedSongMap(
                            songId = mediaId,
                            relatedSongId = it.id
                        )
                    }
                    .forEach(::insert)
            }
        }
    }

    fun playQueue(queue: Queue, playWhenReady: Boolean = true) {
        queueManager.playQueue(queue, playWhenReady)
    }

    fun startRadioSeamlessly() {
        val currentMediaMetadata = player.currentMetadata ?: return

        // Save current song
        val currentSong = player.currentMediaItem

        // Remove other songs from queue
        if (player.currentMediaItemIndex > 0) {
            player.removeMediaItems(0, player.currentMediaItemIndex)
        }
        if (player.currentMediaItemIndex < player.mediaItemCount - 1) {
            player.removeMediaItems(player.currentMediaItemIndex + 1, player.mediaItemCount)
        }

        scope.launch(SilentHandler) {
            val radioQueue = YouTubeQueue(
                endpoint = WatchEndpoint(videoId = currentMediaMetadata.id)
            )
            val initialStatus = radioQueue.getInitialStatus()

            if (initialStatus.title != null) {
                queueManager.setQueueTitle(initialStatus.title)
            }

            // Add radio songs after current song
            player.addMediaItems(initialStatus.items.drop(1))
            queueManager.setQueue(radioQueue)
        }
    }

    private fun checkAndLoadMoreItems(mediaItem: MediaItem?, reason: Int) {
        val currentMediaId = mediaItem?.mediaId ?: player.currentMediaItem?.mediaId ?: return

        if ((dataStore.get(AutoLoadMoreKey, true) || dataStore.get(SmartSuggestionsKey, false)) &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 5 &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)
        ) {
            if (queueManager.currentQueue.hasNextPage()) {
                scope.launch(SilentHandler) {
                    val mediaItems =
                        queueManager.currentQueue.nextPage()
                            .filterExplicit(dataStore.get(HideExplicitKey, false))
                            .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
                    if (player.playbackState != STATE_IDLE) {
                        val wasEnded = player.playbackState == Player.STATE_ENDED
                        addMediaItemsAndSmartShuffle(mediaItems.drop(1), preserveHistory = true)
                        if (wasEnded) {
                            player.prepare()
                            player.play()
                        }
                    }
                }
            } else if (dataStore.get(SmartSuggestionsKey, false)) {
                // Infinite Queue: Fetch suggestions when fixed queue ends
                scope.launch(SilentHandler) {
                    YouTube.next(WatchEndpoint(videoId = currentMediaId))
                        .onSuccess { nextResult ->
                            val currentMediaIds = (0 until player.mediaItemCount).map {
                                player.getMediaItemAt(it).mediaId
                            }.toSet()

                            val suggestions = nextResult.items
                                .map { it.toMediaItem() }
                                .filterExplicit(dataStore.get(HideExplicitKey, false))
                                .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
                                .filter { it.mediaId !in currentMediaIds && it.mediaId !in addedSuggestionIds }
                                .filterByDiversity(maxPerArtist = 3)

                            if (player.playbackState != STATE_IDLE && suggestions.isNotEmpty()) {
                                addedSuggestionIds.addAll(suggestions.mapNotNull { it.mediaId })
                                val wasEnded = player.playbackState == Player.STATE_ENDED
                                addMediaItemsAndSmartShuffle(suggestions, preserveHistory = true)
                                if (wasEnded) {
                                    player.prepare()
                                    player.play()
                                }
                            }
                        }
                }
            }
        }
    }

    fun getAutomixAlbum(albumId: String) {
        scope.launch(SilentHandler) {
            YouTube
                .album(albumId)
                .onSuccess {
                    getAutomix(it.album.playlistId)
                }
        }
    }

    private fun refreshAutomixItems(mediaId: String?) {
        if (mediaId == null) return
        scope.launch(SilentHandler) {
            YouTube.next(WatchEndpoint(videoId = mediaId))
                .onSuccess { nextResult ->
                    val currentMediaIds =
                        (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
                            .toSet()

                    val suggestions = nextResult.items
                        .map { it.toMediaItem() }
                        .filterExplicit(dataStore.get(HideExplicitKey, false))
                        .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
                        .filter { it.mediaId !in currentMediaIds && it.mediaId !in addedSuggestionIds }

                    automixItems.value = suggestions
                }
        }
    }

    fun getAutomix(playlistId: String) {
        if (dataStore[SimilarContent] == true &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)
        ) {
            scope.launch(SilentHandler) {
                YouTube
                    .next(WatchEndpoint(playlistId = playlistId))
                    .onSuccess {
                        YouTube
                            .next(WatchEndpoint(playlistId = it.endpoint.playlistId))
                            .onSuccess {
                                automixItems.value =
                                    it.items.map { song ->
                                        song.toMediaItem()
                                    }
                            }
                    }
            }
        }
    }

    fun addToQueueAutomix(item: MediaItem, position: Int) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        addToQueue(listOf(item))
        addedSuggestionIds.add(item.mediaId)
    }

    fun playNextAutomix(item: MediaItem, position: Int) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        playNext(listOf(item))
        addedSuggestionIds.add(item.mediaId)
    }

    fun clearAutomix() {
        automixItems.value = emptyList()
    }

    fun playNext(items: List<MediaItem>) {
        // If queue is empty or player is idle, play immediately instead
        if (player.mediaItemCount == 0 || player.playbackState == STATE_IDLE) {
            player.setMediaItems(items)
            player.prepare()
            player.play()
            return
        }

        val insertIndex = player.currentMediaItemIndex + 1
        val shuffleEnabled = player.shuffleModeEnabled

        // Insert items immediately after the current item in the window/index space
        player.addMediaItems(insertIndex, items)
        player.prepare()

        if (shuffleEnabled) {
            // Rebuild shuffle order so that newly inserted items are played next
            val timeline = player.currentTimeline
            if (!timeline.isEmpty) {
                val size = timeline.windowCount
                val currentIndex = player.currentMediaItemIndex

                // Newly inserted indices are a contiguous range [insertIndex, insertIndex + items.size)
                val newIndices = (insertIndex until (insertIndex + items.size)).toSet()

                // Collect existing shuffle traversal order excluding current index
                val orderAfter = mutableListOf<Int>()
                var idx = currentIndex
                while (true) {
                    idx = timeline.getNextWindowIndex(idx, Player.REPEAT_MODE_OFF, /*shuffleModeEnabled=*/true)
                    if (idx == C.INDEX_UNSET) break
                    if (idx != currentIndex) orderAfter.add(idx)
                }

                val prevList = mutableListOf<Int>()
                var pIdx = currentIndex
                while (true) {
                    pIdx = timeline.getPreviousWindowIndex(pIdx, Player.REPEAT_MODE_OFF, /*shuffleModeEnabled=*/true)
                    if (pIdx == C.INDEX_UNSET) break
                    if (pIdx != currentIndex) prevList.add(pIdx)
                }
                prevList.reverse() // preserve original forward order

                val existingOrder = (prevList + orderAfter).filter { it != currentIndex && it !in newIndices }

                // Build new shuffle order: current -> newly inserted (in insertion order) -> rest
                val nextBlock = (insertIndex until (insertIndex + items.size)).toList()
                val finalOrder = IntArray(size)
                var pos = 0
                finalOrder[pos++] = currentIndex
                nextBlock.forEach { if (it in 0 until size) finalOrder[pos++] = it }
                existingOrder.forEach { if (pos < size) finalOrder[pos++] = it }

                // Fill any missing indices (safety) to ensure a full permutation
                if (pos < size) {
                    for (i in 0 until size) {
                        if (!finalOrder.contains(i)) {
                            finalOrder[pos++] = i
                            if (pos == size) break
                        }
                    }
                }

                player.setShuffleOrder(DefaultShuffleOrder(finalOrder, System.currentTimeMillis()))
            }
        }
    }

    fun addToQueue(items: List<MediaItem>) {
        player.addMediaItems(items)
        player.prepare()
    }

    private fun toggleLibrary() {
        database.query {
            currentSong.value?.let {
                update(it.song.toggleLibrary())
            }
        }
    }

    fun toggleLike() {
        scope.launch {
            val songToToggle = currentSong.first()
            songToToggle?.let {
                val song = it.song.toggleLike()
                database.query {
                    update(song)
                    syncUtils.likeSong(song)

                    // Check if auto-download on like is enabled and the song is now liked
                    if (dataStore.get(AutoDownloadOnLikeKey, false) && song.liked) {
                        // Trigger download for the liked song
                        val downloadRequest =
                            androidx.media3.exoplayer.offline.DownloadRequest
                                .Builder(song.id, song.id.toUri())
                                .setCustomCacheKey(song.id)
                                .setData(song.title.toByteArray())
                                .build()
                        androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(
                            this@MusicService,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false
                        )
                    }
                }
                currentMediaMetadata.value = player.currentMetadata
                updateWidget()
            }
        }
    }

    fun toggleStartRadio() {
        startRadioSeamlessly()
    }

    private fun setupLoudnessEnhancer() {
        val audioSessionId = player.audioSessionId

        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) {
            Log.w(TAG, "setupLoudnessEnhancer: invalid audioSessionId ($audioSessionId), cannot create effect yet")
            return
        }

        // Create or recreate enhancer if needed
        if (loudnessEnhancer == null) {
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                Log.d(TAG, "LoudnessEnhancer created for sessionId=$audioSessionId")
            } catch (e: Exception) {
                reportException(e)
                loudnessEnhancer = null
                return
            }
        }

        scope.launch {
            try {
                val currentMediaId = withContext(Dispatchers.Main) {
                    player.currentMediaItem?.mediaId
                }

                val normalizeAudio = withContext(Dispatchers.IO) {
                    dataStore.data.map { it[AudioNormalizationKey] ?: true }.first()
                }

                if (normalizeAudio && currentMediaId != null) {
                    val format = withContext(Dispatchers.IO) {
                        database.format(currentMediaId).first()
                    }

                    val loudnessDb = format?.loudnessDb

                    withContext(Dispatchers.Main) {
                        if (loudnessDb != null) {
                            val targetGain = (-loudnessDb * 100).toInt() + 400
                            val clampedGain = targetGain.coerceIn(MIN_GAIN_MB, MAX_GAIN_MB)
                            try {
                                loudnessEnhancer?.setTargetGain(clampedGain)
                                loudnessEnhancer?.enabled = true
                                Log.d(TAG, "LoudnessEnhancer gain applied: $clampedGain mB")
                            } catch (e: Exception) {
                                reportException(e)
                                releaseLoudnessEnhancer()
                            }
                        } else {
                            loudnessEnhancer?.enabled = false
                            Log.w(TAG, "setupLoudnessEnhancer: loudnessDb is null, enhancer disabled")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loudnessEnhancer?.enabled = false
                        Log.d(TAG, "setupLoudnessEnhancer: normalization disabled or mediaId unavailable")
                    }
                }
            } catch (e: Exception) {
                reportException(e)
                releaseLoudnessEnhancer()
            }
        }
    }

    private fun releaseLoudnessEnhancer() {
        try {
            loudnessEnhancer?.release()
            Log.d(TAG, "LoudnessEnhancer released")
        } catch (e: Exception) {
            reportException(e)
            Log.e(TAG, "Error releasing LoudnessEnhancer: ${e.message}")
        } finally {
            loudnessEnhancer = null
        }
    }

    private fun openAudioEffectSession() {
        if (isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = true
        setupLoudnessEnhancer()
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        )
    }

    private fun closeAudioEffectSession() {
        if (!isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = false
        releaseLoudnessEnhancer()
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
        )
    }

    /**
     * Called when the currently playing media item changes.
     *
     * This method handles the transition logic:
     * - **Persistence**: Saves the queue state to disk immediately.
     * - **Smart Suggestions**: Refreshes the automix/suggestions pool if enabled.
     * - **Auto-Load**: Fetches more songs if near the end of the queue ([checkAndLoadMoreItems]).
     * - **Widget**: Updates the home screen widget with new song details.
     *
     * @param mediaItem The new [MediaItem] being played.
     * @param reason The reason for the transition (e.g., [Player.MEDIA_ITEM_TRANSITION_REASON_AUTO]).
     */
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        lastPlaybackSpeed = -1.0f // force update song

        setupLoudnessEnhancer()

        // Proactively refresh suggestions pool for UI
        if (dataStore.get(SmartSuggestionsKey, false)) {
            refreshAutomixItems(mediaItem?.mediaId)
        }

        // Auto load more songs
        checkAndLoadMoreItems(mediaItem, reason)

        // Save state when media item changes
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
        updateWidget()
        retryCount.remove(mediaItem?.mediaId)
    }

    /**
     * Called when the playback state changes (IDLE, BUFFERING, READY, ENDED).
     *
     * Key actions:
     * - **Persistence**: Saves state on every change.
     * - **Scrobbling**: Triggers periodic scrobble updates or finalizes scrobble on stop.
     * - **Infinite Playback**: Triggers loading more items when [Player.STATE_ENDED] is reached
     *   to simulate infinite playback if enabled.
     *
     * @param playbackState The new state constant from [Player].
     */
    override fun onPlaybackStateChanged(@Player.State playbackState: Int) {
        // Save state when playback state changes
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }

        if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
            scrobbleManager?.onSongStop()
        }

        if (playbackState == Player.STATE_ENDED && dataStore.get(SmartSuggestionsKey, false)) {
            checkAndLoadMoreItems(player.currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        }
        updateWidget()
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
            wasPausedByZeroVolume = false
        }
        if (playWhenReady) {
            setupLoudnessEnhancer()
        }
        updateWidget()
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            val isBufferingOrReady =
                player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            if (isBufferingOrReady && player.playWhenReady) {
                val focusGranted = requestAudioFocus()
                if (focusGranted) {
                    openAudioEffectSession()
                }
            } else {
                closeAudioEffectSession()
            }
        }
        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaMetadata.value = player.currentMetadata
        }

        // Widget updates
        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            if (player.isPlaying) {
                startWidgetUpdates()
            } else {
                stopWidgetUpdates()
                updateWidget() // Ensure one last update to show paused state (straight line)
            }
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateNotification()
        if (shuffleModeEnabled) {
            applySmartShuffle()
        }

        // Save state when shuffle mode changes
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
    }

    private fun applySmartShuffle(preserveHistory: Boolean = false) {
        if (player.mediaItemCount == 0) return

        val shuffledIndices: IntArray
        val offset: Int

        if (preserveHistory && player.shuffleModeEnabled) {
            val currentSequence = getShuffleSequence()
            val currentIndexInSequence = currentSequence.indexOf(player.currentMediaItemIndex)

            if (currentIndexInSequence != -1) {
                val pastAndCurrent = currentSequence.subList(0, currentIndexInSequence + 1)
                val future = currentSequence.subList(currentIndexInSequence + 1, currentSequence.size).toMutableList()

                // Identify indices not in the current sequence (newly added items)
                val allIndices = (0 until player.mediaItemCount).toSet()
                val missingIndices = allIndices - currentSequence.toSet()
                future.addAll(missingIndices)

                future.shuffle()
                shuffledIndices = (pastAndCurrent + future).toIntArray()
                offset = pastAndCurrent.size
            } else {
                shuffledIndices = IntArray(player.mediaItemCount) { it }
                shuffledIndices.shuffle()
                val idx = shuffledIndices.indexOf(player.currentMediaItemIndex)
                if (idx != -1) {
                    val first = shuffledIndices[0]
                    shuffledIndices[0] = player.currentMediaItemIndex
                    shuffledIndices[idx] = first
                }
                offset = 1
            }
        } else {
            shuffledIndices = IntArray(player.mediaItemCount) { it }
            shuffledIndices.shuffle()
            val currentIndex = shuffledIndices.indexOf(player.currentMediaItemIndex)
            if (currentIndex != -1) {
                val firstValue = shuffledIndices[0]
                shuffledIndices[0] = player.currentMediaItemIndex
                shuffledIndices[currentIndex] = firstValue
            }
            offset = 1
        }

        if (dataStore.get(SmartShuffleKey, false)) {
            // Smart Shuffle Algorithm: Avoid back-to-back same artists or albums
            for (i in offset until shuffledIndices.size - 1) {
                val currentMediaItem = player.getMediaItemAt(shuffledIndices[i])
                val previousMediaItem = player.getMediaItemAt(shuffledIndices[i - 1])

                val currentArtist = currentMediaItem.mediaMetadata.artist?.toString()
                val previousArtist = previousMediaItem.mediaMetadata.artist?.toString()
                val currentAlbum = currentMediaItem.mediaMetadata.albumTitle?.toString()
                val previousAlbum = previousMediaItem.mediaMetadata.albumTitle?.toString()

                if ((currentArtist != null && currentArtist == previousArtist) ||
                    (currentAlbum != null && currentAlbum == previousAlbum)
                ) {
                    // Try to find a different artist/album further down the list to swap
                    for (j in i + 1 until shuffledIndices.size) {
                        val nextMediaItem = player.getMediaItemAt(shuffledIndices[j])
                        val nextArtist = nextMediaItem.mediaMetadata.artist?.toString()
                        val nextAlbum = nextMediaItem.mediaMetadata.albumTitle?.toString()

                        if (nextArtist != currentArtist && nextAlbum != currentAlbum) {
                            val temp = shuffledIndices[i]
                            shuffledIndices[i] = shuffledIndices[j]
                            shuffledIndices[j] = temp
                            break
                        }
                    }
                }
            }
        }
        player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
    }

    private fun getShuffleSequence(): List<Int> {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return emptyList()
        val sequence = mutableListOf<Int>()
        var index = timeline.getFirstWindowIndex(true)
        while (index != -1) {
            sequence.add(index)
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
        }
        return sequence
    }

    private fun addMediaItemsAndSmartShuffle(items: List<MediaItem>, preserveHistory: Boolean = true) {
        if (items.isEmpty()) return
        player.addMediaItems(items)
        if (player.shuffleModeEnabled && dataStore.get(SmartShuffleKey, false)) {
            applySmartShuffle(preserveHistory)
        }
    }

    private fun List<MediaItem>.filterByDiversity(maxPerArtist: Int): List<MediaItem> {
        val artistCounts = mutableMapOf<String?, Int>()
        return this.filter { item ->
            val artist = item.mediaMetadata.artist?.toString()
            val count = artistCounts.getOrDefault(artist, 0)
            if (count < maxPerArtist) {
                artistCounts[artist] = count + 1
                true
            } else {
                false
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }

        // Save state when repeat mode changes
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Log.e(TAG, "onPlayerError: ${error.errorCodeName} (${error.errorCode})", error)

        if (!isNetworkConnected.value || isNetworkError(error)) {
            Log.d(TAG, "Network error detected, waiting for connection...")
            waitOnNetworkError()
            return
        }

        // If we reach here, we have network but still got an error
        globalRetryCount = 0 // Reset global retry count since it's not a direct network error now
        retryJob?.cancel()

        // Retry logic for "unlimited streaming" feel
        val mediaId = player.currentMediaItem?.mediaId
        if (mediaId != null) {
            val currentRetries = retryCount.getOrDefault(mediaId, 0)
            if (currentRetries < 3) { // Retry up to 3 times
                retryCount[mediaId] = currentRetries + 1
                songUrlCache.remove(mediaId) // Invalidate cache to force re-fetch
                player.prepare()
                safePlay()
                return
            }
        }

        if (dataStore.get(AutoSkipNextOnErrorKey, false)) {
            skipOnError()
        } else {
            stopOnError()
        }
    }

    private fun createCacheDataSource(): CacheDataSource.Factory = CacheDataSource
        .Factory()
        .setCache(downloadCache)
        .setUpstreamDataSourceFactory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    DefaultDataSource.Factory(
                        this,
                        OkHttpDataSource.Factory(
                            okHttpClient.newBuilder()
                                .proxy(YouTube.proxy)
                                .proxyAuthenticator { _, response ->
                                    YouTube.proxyAuth?.let { auth ->
                                        response.request.newBuilder()
                                            .header("Proxy-Authorization", auth)
                                            .build()
                                    } ?: response.request
                                }
                                .build()
                        )
                    )
                )
        ).setCacheWriteDataSinkFactory(null)
        .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

    private fun createDataSourceFactory(): DataSource.Factory {
        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")

            if (downloadCache.isCached(
                    mediaId,
                    dataSpec.position,
                    if (dataSpec.length >= 0) dataSpec.length else 1
                ) ||
                playerCache.isCached(mediaId, dataSpec.position, CHUNK_LENGTH)
            ) {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec
            }

            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec.withUri(it.first.toUri())
            }

            val playbackData = runBlocking(Dispatchers.IO) {
                // Read audio quality from settings every time to respect user changes
                val currentAudioQuality = dataStore.get(
                    AudioQualityKey
                ).toEnum(com.music.vivi.constants.AudioQuality.AUTO)
                YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = currentAudioQuality,
                    connectivityManager = connectivityManager,
                    httpClient = okHttpClient
                )
            }.getOrElse { throwable ->
                when (throwable) {
                    is PlaybackException -> throw throwable

                    is java.net.ConnectException, is java.net.UnknownHostException -> {
                        throw PlaybackException(
                            getString(R.string.error_no_internet),
                            throwable,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                        )
                    }

                    is java.net.SocketTimeoutException -> {
                        throw PlaybackException(
                            getString(R.string.error_timeout),
                            throwable,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                        )
                    }

                    else -> throw PlaybackException(
                        getString(R.string.error_unknown),
                        throwable,
                        PlaybackException.ERROR_CODE_REMOTE_ERROR
                    )
                }
            }

            val nonNullPlayback = requireNotNull(playbackData) {
                getString(R.string.error_unknown)
            }
            run {
                val format = nonNullPlayback.format

                database.query {
                    upsert(
                        FormatEntity(
                            id = mediaId,
                            itag = format.itag,
                            mimeType = format.mimeType.split(";")[0],
                            codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                            bitrate = format.bitrate,
                            sampleRate = format.audioSampleRate,
                            contentLength = format.contentLength!!,
                            loudnessDb = nonNullPlayback.audioConfig?.loudnessDb,
                            playbackUrl = nonNullPlayback.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                        )
                    )
                }
                scope.launch(Dispatchers.IO) { recoverSong(mediaId, nonNullPlayback) }

                val streamUrl = nonNullPlayback.streamUrl

                songUrlCache[mediaId] =
                    streamUrl to System.currentTimeMillis() + (nonNullPlayback.streamExpiresInSeconds * 1000L)
                return@Factory dataSpec.withUri(streamUrl.toUri()).subrange(dataSpec.uriPositionOffset, CHUNK_LENGTH)
            }
        }
    }

    private fun createMediaSourceFactory() = DefaultMediaSourceFactory(
        createDataSourceFactory(),
        ExtractorsFactory {
            arrayOf(MatroskaExtractor(), FragmentedMp4Extractor())
        }
    )

    private fun createRenderersFactory() = object : DefaultRenderersFactory(this) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean,
        ) = DefaultAudioSink
            .Builder(this@MusicService)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessorChain(
                DefaultAudioSink.DefaultAudioProcessorChain(
                    emptyArray(),
                    SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
                    SonicAudioProcessor()
                )
            ).build()
    }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

    override fun onPlaybackStatsReady(eventTime: AnalyticsListener.EventTime, playbackStats: PlaybackStats) {
        val mediaItem = eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem

        if (playbackStats.totalPlayTimeMs >= (
                dataStore[HistoryDuration]?.times(1000f)
                    ?: 30000f
                ) &&
            !dataStore.get(PauseListenHistoryKey, false)
        ) {
            database.query {
                incrementTotalPlayTime(mediaItem.mediaId, playbackStats.totalPlayTimeMs)
                try {
                    insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = LocalDateTime.now(),
                            playTime = playbackStats.totalPlayTimeMs
                        )
                    )
                } catch (_: SQLException) {
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                val playbackUrl = YTPlayerUtils.playerResponseForMetadata(mediaItem.mediaId, null)
                    .getOrNull()?.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                playbackUrl?.let {
                    YouTube.registerPlayback(null, playbackUrl)
                        .onFailure {
                            reportException(it)
                        }
                }
            }
        }
    }

    private fun saveQueueToDisk(blocking: Boolean = false) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { saveQueueToDisk(blocking) }
            return
        }

        if (player.mediaItemCount == 0) {
            return
        }

        // Capture state on Main Thread
        val currentQueue = queueManager.currentQueue
        val queueTitle = queueManager.queueTitle

        // Convert to serializable format on Main Thread to avoid concurrent modification
        // and ensure thread safety for player and queue access
        val persistQueue = currentQueue.toPersistQueue(
            title = queueTitle,
            items = player.mediaItems.mapNotNull { it.metadata },
            mediaItemIndex = player.currentMediaItemIndex,
            position = player.currentPosition
        )

        val persistAutomix =
            PersistQueue(
                title = getString(R.string.automix),
                items = automixItems.value.mapNotNull { it.metadata },
                mediaItemIndex = 0,
                position = 0
            )

        // Capture player state on Main Thread
        val persistPlayerState = PersistPlayerState(
            playWhenReady = player.playWhenReady,
            repeatMode = player.repeatMode,
            shuffleModeEnabled = player.shuffleModeEnabled,
            volume = player.volume,
            currentPosition = player.currentPosition,
            currentMediaItemIndex = player.currentMediaItemIndex,
            playbackState = player.playbackState
        )

        val performSave = {
            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistQueue)
                    }
                }
            }.onFailure {
                reportException(it)
            }
            runCatching {
                filesDir.resolve(PERSISTENT_AUTOMIX_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistAutomix)
                    }
                }
            }.onFailure {
                reportException(it)
            }
            runCatching {
                filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistPlayerState)
                    }
                }
            }.onFailure {
                reportException(it)
            }
        }

        if (blocking) {
            runBlocking(Dispatchers.IO) { performSave() }
        } else {
            scope.launch(Dispatchers.IO) { performSave() }
        }
    }

    /**
     * Cleans up resources when the service is destroyed.
     *
     * This method ensures a clean shutdown by:
     * 1.  **Persisting State**: Saves the final queue and player state to disk.
     * 2.  **Cancelling Scopes**: Cancels [scope] and [serviceJob] to stop all active coroutines.
     * 3.  **Stopping Integrations**: Disconnects from Last.fm and Discord.
     * 4.  **Releasing Player**: Releases the [ExoPlayer] instance to free hardware decoders.
     * 5.  **Releasing Session**: Releases the [MediaSession] to notify the system the session is dead.
     * 6.  **Unregistering Receivers**: Cleans up all broadcast receivers to prevent leaks.
     */
    override fun onDestroy() {
        isRunning = false
        // Persist queue before killing scope
        if (dataStore.get(PersistentQueueKey, true)) {
            // Use blocking save to ensure data is written before process death
            saveQueueToDisk(blocking = true)
        }

        // Cancel scope FIRST to stop new coroutines/updates
        scope.cancel()
        serviceJob.cancel()

        integrationManager.stop(player)
        connectivityObserver.unregister()
        abandonAudioFocus()
        releaseLoudnessEnhancer()

        if (isVolumeReceiverRegistered) {
            unregisterReceiver(volumeReceiver)
            isVolumeReceiverRegistered = false
        }
        if (isNoisyReceiverRegistered) {
            unregisterReceiver(becomingNoisyReceiver)
            isNoisyReceiverRegistered = false
        }

        if (this::mediaSession.isInitialized) {
            mediaSession.release()
        }

        if (this::player.isInitialized) {
            player.removeListener(this)
            player.removeListener(sleepTimer)
            player.release()
        }
        super.onDestroy()
    }
    // better network handling

    private fun isNetworkError(error: PlaybackException): Boolean =
        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
            error.cause is java.io.IOException ||
            (error.cause as? java.net.ConnectException) != null ||
            (error.cause as? java.net.UnknownHostException) != null ||
            (error.cause as? java.net.SocketTimeoutException) != null ||
            (error.cause as? PlaybackException)?.errorCode ==
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    /**
     * Called when the app is removed from the recent tasks list (swiped away).
     *
     * If the user has enabled [StopMusicOnTaskClearKey], this method will pause playback
     * and stop the service, effectively killing the player. Otherwise, the service continues
     * running in the foreground.
     *
     * @param rootIntent The intent that launched the root activity.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (dataStore.get(StopMusicOnTaskClearKey, false)) {
            player.pause()
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

// added widget updating

    private fun updateWidget() {
        scope.launch {
            widgetManager.updateWidgets(
                title = player.currentMetadata?.title ?: getString(R.string.not_playing),
                artist = player.currentMetadata?.artists?.joinToString { it.name } ?: getString(R.string.tap_to_play),
                artworkUri = player.currentMetadata?.thumbnailUrl,
                isPlaying = player.isPlaying,
                isLiked = currentSong.value?.song?.liked ?: false,
                queueItems = player.mediaItems.drop(player.currentMediaItemIndex + 1).take(5),
                duration = if (player.duration != C.TIME_UNSET) player.duration else 0,
                currentPosition = player.currentPosition
            )
        }
    }

    private var widgetUpdateJob: Job? = null

    private fun startWidgetUpdates() {
        widgetUpdateJob?.cancel()
        widgetUpdateJob = scope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    updateWidget()
                }
                delay(200)
            }
        }
    }

    private fun stopWidgetUpdates() {
        widgetUpdateJob?.cancel()
        widgetUpdateJob = null
    }

    /**
     * Called when the system requests a notification update.
     *
     * **Android 12+ Compliance**:
     * We override this to force `startInForeground = true`. This is a defensive measure to reduce
     * the likelihood of `ForegroundServiceStartNotAllowedException` by trying to keep the service
     * promoted to foreground status as much as possible, even during notification updates.
     */
    override fun onUpdateNotification(session: MediaSession, startInForeground: Boolean) {
        // Try to stay in foreground even when paused to avoid ForegroundServiceStartNotAllowedException
        // when resuming from background on Android 12+
        super.onUpdateNotification(session, true)
    }

    companion object {
        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"
        const val SEARCH = "search"
        const val RECOMMENDED = "Recommended"

        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
        const val ERROR_CODE_NO_STREAM = 1000001
        const val CHUNK_LENGTH = 512 * 1024L
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE = "persistent_automix.data"
        const val PERSISTENT_PLAYER_STATE_FILE = "persistent_player_state.data"
        const val MAX_CONSECUTIVE_ERR = 5
        const val MAX_RETRY_COUNT = 5

        // Constants for audio normalization
        private const val MAX_GAIN_MB = 1000 // Maximum gain in millibels (8 dB)
        private const val MIN_GAIN_MB = -1000 // Minimum gain in millibels (-8 dB)

        private const val TAG = "MusicService"
        var isRunning = false
    }
}
