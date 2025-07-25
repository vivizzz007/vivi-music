@file:Suppress("DEPRECATION")

package com.music.vivi.playback

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.SQLException
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.media.AudioAttributes as LegacyAudioAttributes
import android.media.audiofx.AudioEffect
import android.net.ConnectivityManager
import android.os.Binder
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
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.MainActivity
import com.music.vivi.R
import com.music.vivi.constants.AudioNormalizationKey
import com.music.vivi.constants.AudioQualityKey
import com.music.vivi.constants.AutoLoadMoreKey
import com.music.vivi.constants.AutoDownloadOnLikeKey
import com.music.vivi.constants.AutoSkipNextOnErrorKey
import com.music.vivi.constants.CrossfadeDurationKey
import com.music.vivi.constants.DiscordTokenKey
import com.music.vivi.constants.EnableDiscordRPCKey
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.HistoryDuration
import com.music.vivi.constants.MediaSessionConstants.CommandToggleLike
import com.music.vivi.constants.MediaSessionConstants.CommandToggleStartRadio
import com.music.vivi.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.music.vivi.constants.MediaSessionConstants.CommandToggleShuffle
import com.music.vivi.constants.PauseListenHistoryKey
import com.music.vivi.constants.PersistentQueueKey
import com.music.vivi.constants.PlayerVolumeKey
import com.music.vivi.constants.RepeatModeKey
import com.music.vivi.constants.ShowLyricsKey
import com.music.vivi.constants.SimilarContent
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
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.lyrics.LyricsHelper
import com.music.vivi.models.PersistQueue
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.EmptyQueue
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.Queue
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.playback.queues.filterExplicit
import com.music.vivi.utils.CoilBitmapLoader
import com.music.vivi.utils.DiscordRPC
import com.music.vivi.utils.SyncUtils
import com.music.vivi.utils.YTPlayerUtils
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.enumPreference
import com.music.vivi.utils.get
import com.music.vivi.utils.isInternetAvailable
import com.music.vivi.utils.reportException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
//import kotlinx.coroutines.delay
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
import okhttp3.OkHttpClient
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.max
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

//import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@AndroidEntryPoint
class MusicService : MediaLibraryService(), Player.Listener, PlaybackStatsListener.Callback {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var syncUtils: SyncUtils

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    var scope = CoroutineScope(Dispatchers.Main) + Job()
    private val binder = MusicBinder()

    private lateinit var connectivityManager: ConnectivityManager

    private val audioQuality by enumPreference(
        this,
        AudioQualityKey,
        com.music.vivi.constants.AudioQuality.AUTO
    )
    ///crossfade
    var crossfadeJob: Job? = null
    var isPerformingCrossfade = false
    private var nextTrackStartTime = 0L
    private var nextTrackCrossfadeJob: Job? = null
    // Keep your existing crossfadeDuration property
    private val crossfadeDuration by lazy {
        dataStore.get(CrossfadeDurationKey, 3000) // 3 seconds default
    }

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null

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

    private val normalizeFactor = MutableStateFlow(1f)
    val playerVolume = MutableStateFlow(dataStore.get(PlayerVolumeKey, 1f).coerceIn(0f, 1f))

    private var wasPlayingBeforeFocusLoss = false
    private var pausedByUser = false

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

    private var discordRpc: DiscordRPC? = null

    val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player
            )
                .apply {
                    setSmallIcon(R.drawable.vivi)
                },
        )
        player =
            ExoPlayer
                .Builder(this)
                .setMediaSourceFactory(createMediaSourceFactory())
                .setRenderersFactory(createRenderersFactory())
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setAudioAttributes(
                    androidx.media3.common.AudioAttributes  // ← Media3 version for ExoPlayer
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,  // Enable automatic audio focus handling
                ).setSeekBackIncrementMs(5000)
                .setSeekForwardIncrementMs(5000)
                .build()
                .apply {
                    addListener(this@MusicService)
                    sleepTimer = SleepTimer(scope, this)
                    addListener(sleepTimer)
                    addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))
                }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        requestAudioFocus()

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
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).setBitmapLoader(CoilBitmapLoader(this, scope))
                .build()
        player.repeatMode = dataStore.get(RepeatModeKey, REPEAT_MODE_OFF)

        // Keep a connected controller so that notification works
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())

        connectivityManager = getSystemService()!!

        combine(playerVolume, normalizeFactor) { playerVolume, normalizeFactor ->
            playerVolume * normalizeFactor
        }.collectLatest(scope) {
            player.volume = it
        }

        playerVolume.debounce(1000).collect(scope) { volume ->
            dataStore.data
                .map { it[CrossfadeDurationKey] ?: 3000 }
                .distinctUntilChanged()
                .collectLatest(scope) { duration ->
                    // Crossfade duration updated
                }
        }

        currentSong.debounce(1000).collect(scope) { song ->
            updateNotification()
            if (song != null) {
                discordRpc?.updateSong(song)
            } else {
                discordRpc?.closeRPC()
            }
        }

        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged(),
        ) { mediaMetadata, showLyrics ->
            mediaMetadata to showLyrics
        }.collectLatest(scope) { (mediaMetadata, showLyrics) ->
            if (showLyrics && mediaMetadata != null && database.lyrics(mediaMetadata.id)
                    .first() == null
            ) {
                val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadata.id,
                            lyrics = lyrics,
                        ),
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

        combine(
            currentFormat,
            dataStore.data
                .map { it[AudioNormalizationKey] ?: true }
                .distinctUntilChanged(),
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(scope) { (format, normalizeAudio) ->
            normalizeFactor.value =
                if (normalizeAudio && format?.loudnessDb != null) {
                    min(10f.pow(-format.loudnessDb.toFloat() / 20), 1f)
                } else {
                    1f
                }
        }

        dataStore.data
            .map { it[DiscordTokenKey] to (it[EnableDiscordRPCKey] ?: true) }
            .debounce(300)
            .distinctUntilChanged()
            .collect(scope) { (key, enabled) ->
                if (discordRpc?.isRpcRunning() == true) {
                    discordRpc?.closeRPC()
                }
                discordRpc = null
                if (key != null && enabled) {
                    discordRpc = DiscordRPC(this, key)
                    currentSong.value?.let {
                        discordRpc?.updateSong(it)
                    }
                }
            }

        if (dataStore.get(PersistentQueueKey, true)) {
            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                playQueue(
                    queue =
                        ListQueue(
                            title = queue.title,
                            items = queue.items.map { it.toMediaItem() },
                            startIndex = queue.mediaItemIndex,
                            position = queue.position,
                        ),
                    playWhenReady = false,
                )
            }
            runCatching {
                filesDir.resolve(PERSISTENT_AUTOMIX_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                automixItems.value = queue.items.map { it.toMediaItem() }
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
                            },
                        ),
                    )
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border)
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
                            },
                        ),
                    ).setIconResId(
                        when (player.repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> throw IllegalStateException()
                        },
                    ).setSessionCommand(CommandToggleRepeatMode)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(getString(if (player.shuffleModeEnabled) R.string.action_shuffle_off else R.string.action_shuffle_on))
                    .setIconResId(if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(R.string.start_radio))
                    .setIconResId(R.drawable.radio)
                    .setSessionCommand(CommandToggleStartRadio)
                    .setEnabled(currentSong.value != null)
                    .build(),
            ),
        )
    }



    private suspend fun recoverSong(
        mediaId: String,
        playbackData: YTPlayerUtils.PlaybackData? = null
    ) {
        val song = database.song(mediaId).first()
        val mediaMetadata = withContext(Dispatchers.Main) {
            player.findNextMediaItemById(mediaId)?.metadata
        } ?: return
        val duration = song?.song?.duration?.takeIf { it != -1 }
            ?: mediaMetadata.duration.takeIf { it != -1 }
            ?: (playbackData?.videoDetails ?: YTPlayerUtils.playerResponseForMetadata(mediaId)
                .getOrNull()?.videoDetails)?.lengthSeconds?.toInt()
            ?: -1
        database.query {
            if (song == null) insert(mediaMetadata.copy(duration = duration))
            else if (song.song.duration == -1) update(song.song.copy(duration = duration))
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

    fun playQueue(
        queue: Queue,
        playWhenReady: Boolean = true,
    ) {
        if (!scope.isActive) scope = CoroutineScope(Dispatchers.Main) + Job()
        currentQueue = queue
        queueTitle = null
        player.shuffleModeEnabled = false
        if (queue.preloadItem != null) {
            player.setMediaItem(queue.preloadItem!!.toMediaItem())
            player.prepare()
            player.playWhenReady = playWhenReady
        }
        scope.launch(SilentHandler) {
            val initialStatus =
                withContext(Dispatchers.IO) {
                    queue.getInitialStatus().filterExplicit(dataStore.get(HideExplicitKey, false))
                }
            if (queue.preloadItem != null && player.playbackState == STATE_IDLE) return@launch
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            if (initialStatus.items.isEmpty()) return@launch
            if (queue.preloadItem != null) {
                player.addMediaItems(
                    0,
                    initialStatus.items.subList(0, initialStatus.mediaItemIndex)
                )
                player.addMediaItems(
                    initialStatus.items.subList(
                        initialStatus.mediaItemIndex + 1,
                        initialStatus.items.size
                    )
                )
            } else {
                player.setMediaItems(
                    initialStatus.items,
                    if (initialStatus.mediaItemIndex >
                        0
                    ) {
                        initialStatus.mediaItemIndex
                    } else {
                        0
                    },
                    initialStatus.position,
                )
                player.prepare()
                player.playWhenReady = playWhenReady
            }
        }
    }

    fun startRadioSeamlessly() {
        val currentMediaMetadata = player.currentMetadata ?: return
        if (player.currentMediaItemIndex > 0) player.removeMediaItems(
            0,
            player.currentMediaItemIndex
        )
        if (player.currentMediaItemIndex <
            player.mediaItemCount - 1
        ) {
            player.removeMediaItems(player.currentMediaItemIndex + 1, player.mediaItemCount)
        }
        scope.launch(SilentHandler) {
            val radioQueue =
                YouTubeQueue(endpoint = WatchEndpoint(videoId = currentMediaMetadata.id))
            val initialStatus = radioQueue.getInitialStatus()
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            player.addMediaItems(initialStatus.items.drop(1))
            currentQueue = radioQueue
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

    fun getAutomix(playlistId: String) {
        if (dataStore[SimilarContent] == true) {
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

    fun addToQueueAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        addToQueue(listOf(item))
    }

    fun playNextAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        playNext(listOf(item))
    }

    fun clearAutomix() {
        filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
        automixItems.value = emptyList()
    }

    fun playNext(items: List<MediaItem>) {
        player.addMediaItems(
            if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1,
            items
        )
        player.prepare()
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
        database.query {
            currentSong.value?.let {
                val song = it.song.toggleLike()
                update(song)
                syncUtils.likeSong(song)

                // Check if auto-download on like is enabled and the song is now liked
                if (dataStore.get(AutoDownloadOnLikeKey, false) && song.liked) {
                    // Trigger download for the liked song
                    val downloadRequest = androidx.media3.exoplayer.offline.DownloadRequest
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
        }
    }

    fun toggleStartRadio() {
        startRadioSeamlessly()
    }

    private fun openAudioEffectSession() {
        if (isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = true
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            },
        )
    }

    private fun closeAudioEffectSession() {
        if (!isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = false
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            },
        )
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // ... existing code for auto-loading more items ...

        if (dataStore.get(AutoLoadMoreKey, true) &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 5 &&
            currentQueue.hasNextPage()
        ) {
            scope.launch(SilentHandler) {
                val mediaItems = currentQueue.nextPage().filterExplicit(dataStore.get(HideExplicitKey, false))
                if (player.playbackState != STATE_IDLE) {
                    player.addMediaItems(mediaItems.drop(1))
                }
            }
        }

        // Reset crossfade state on transitions
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
            isPerformingCrossfade = false
            // Don't immediately reset volume here - let crossfade handle it
        }

        when (reason) {
            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {
                if (crossfadeDuration > 0) {
                    // Start monitoring for next crossfade
                    startCrossfadeMonitoring()
                }
            }
            Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> {
                if (crossfadeDuration > 0) {
                    fadeInNewTrack()
                }
            }
        }
    }



    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        if (playbackState == STATE_IDLE) {
            currentQueue = EmptyQueue
            player.shuffleModeEnabled = false
            queueTitle = null
        }
    }

    override fun onEvents(
        player: Player,
        events: Player.Events,
    ) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            val isBufferingOrReady =
                player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            if (isBufferingOrReady && player.playWhenReady) {
                openAudioEffectSession()
            } else {
                closeAudioEffectSession()
            }
        }
        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaMetadata.value = player.currentMetadata
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)

        if (!isPlaying && player.playbackState != Player.STATE_IDLE) {
            // Cancel crossfade when paused
            crossfadeJob?.cancel()
            isPerformingCrossfade = false

            if (!wasPlayingBeforeFocusLoss) {
                pausedByUser = true
            }
        } else if (isPlaying) {
            pausedByUser = false
            wasPlayingBeforeFocusLoss = false

            // Restart crossfade monitoring if we have a next track
            if (player.hasNextMediaItem() && crossfadeDuration > 0) {
                startCrossfadeMonitoring() // Use new seamless monitoring
            }

            // Enhanced playback start
            if (crossfadeDuration > 0) {
                handlePlaybackStart() // Use enhanced version
            }
        }
    }
    private fun handlePlaybackStart() {
        if (isPerformingCrossfade) return

        scope.launch {
            try {
                val targetVolume = playerVolume.value * normalizeFactor.value
                val fadeSteps = 15
                val stepDuration = 40L // 600ms total fade-in

                player.volume = 0.2f // Start at audible but low volume

                for (i in 0..fadeSteps) {
                    if (!coroutineContext.isActive || !player.isPlaying) break

                    val progress = i.toFloat() / fadeSteps
                    val volume = 0.2f + (targetVolume - 0.2f) * progress.pow(0.8f)
                    player.volume = volume.coerceAtMost(targetVolume)

                    delay(stepDuration)
                }

                player.volume = targetVolume

            } catch (e: Exception) {
                player.volume = playerVolume.value * normalizeFactor.value
            }
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateNotification()
        if (shuffleModeEnabled) {
            // Always put current playing item at first
            val shuffledIndices = IntArray(player.mediaItemCount) { it }
            shuffledIndices.shuffle()
            shuffledIndices[shuffledIndices.indexOf(player.currentMediaItemIndex)] =
                shuffledIndices[0]
            shuffledIndices[0] = player.currentMediaItemIndex
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        if (dataStore.get(AutoSkipNextOnErrorKey, false) &&
            isInternetAvailable(this) &&
            player.hasNextMediaItem()
        ) {
            player.seekToNext()
            player.prepare()
            player.playWhenReady = true
        }
    }

    private fun createCacheDataSource(): CacheDataSource.Factory =
        CacheDataSource
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
                                OkHttpClient
                                    .Builder()
                                    .proxy(YouTube.proxy)
                                    .build(),
                            ),
                        ),
                    ),
            ).setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

    private fun createDataSourceFactory(): DataSource.Factory {
        val songUrlCache = HashMap<String, Pair<String, Long>>()
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
                YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                )
            }.getOrNull()

            if (playbackData == null) {
                throw PlaybackException(
                    getString(R.string.error_unknown),
                    null,
                    PlaybackException.ERROR_CODE_REMOTE_ERROR
                )
            } else {
                val format = playbackData.format

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
                            loudnessDb = playbackData.audioConfig?.loudnessDb,
                            playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                        )
                    )
                }
                scope.launch(Dispatchers.IO) { recoverSong(mediaId, playbackData) }

                val streamUrl = playbackData.streamUrl

                songUrlCache[mediaId] =
                    streamUrl to System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)
                return@Factory dataSpec.withUri(streamUrl.toUri()).subrange(dataSpec.uriPositionOffset, CHUNK_LENGTH)
            }
        }
    }


    //crossfade

    fun startCrossfadeMonitoring() {
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            while (coroutineContext.isActive && player.hasNextMediaItem() && player.isPlaying) {
                val currentDuration = player.duration
                val currentPosition = player.currentPosition

                if (currentDuration > 0 && currentPosition > 0) {
                    // Skip crossfade for very short tracks
                    if (currentDuration < crossfadeDuration + 2000) {
                        delay(100)
                        continue
                    }

                    val timeRemaining = currentDuration - currentPosition

                    // Start crossfade when we reach the crossfade duration before end
                    if (timeRemaining <= crossfadeDuration) {
                        performSmoothCrossfade()
                        break
                    }

                    // Dynamic monitoring frequency based on proximity to crossfade point
                    val checkInterval = when {
                        timeRemaining < crossfadeDuration + 1000 -> 50L  // Close to crossfade
                        timeRemaining < crossfadeDuration + 3000 -> 100L // Approaching
                        else -> 500L // Far away
                    }

                    delay(checkInterval)
                } else {
                    delay(100)
                }
            }
        }
    }

    private suspend fun performSmoothCrossfade() {
        if (crossfadeDuration <= 0 || isPerformingCrossfade || !player.hasNextMediaItem()) return

        isPerformingCrossfade = true
        val originalVolume = playerVolume.value * normalizeFactor.value

        try {
            // Calculate fade parameters for smooth transition
            val fadeSteps = (crossfadeDuration / 50).coerceIn(10, 50) // 50ms per step, 10-50 steps
            val stepDuration = crossfadeDuration.toLong() / fadeSteps
            val halfSteps = fadeSteps / 2

            // Phase 1: Fade out current track (first half of crossfade duration)
            for (step in 0 until halfSteps) {
                if (!coroutineContext.isActive || !player.isPlaying) return

                val progress = step.toFloat() / halfSteps
                // Use exponential curve for natural fade out
                val volume = originalVolume * (1f - progress).pow(2f)
                player.volume = volume.coerceAtLeast(0.05f) // Avoid complete silence

                delay(stepDuration)
            }

            // Phase 2: Quick transition at minimum audible volume
            player.volume = 0.05f

            // Store current position before transition
            val transitionPosition = player.currentPosition

            // Perform the track transition
            player.seekToNext()

            // Brief pause to allow buffering
            var bufferWaitTime = 0L
            val maxBufferWait = 200L

            while (player.playbackState == Player.STATE_BUFFERING &&
                bufferWaitTime < maxBufferWait &&
                coroutineContext.isActive) {
                delay(25)
                bufferWaitTime += 25
            }

            // Ensure playback continues
            if (!player.isPlaying) {
                player.play()
            }

            // Small stabilization delay
            delay(50)

            // Phase 3: Fade in new track (second half of crossfade duration)
            for (step in 0 until halfSteps) {
                if (!coroutineContext.isActive) return

                val progress = step.toFloat() / halfSteps
                // Use exponential curve for natural fade in
                val volume = 0.05f + (originalVolume - 0.05f) * progress.pow(0.5f)
                player.volume = volume.coerceAtMost(originalVolume)

                delay(stepDuration)
            }

            // Ensure final volume is correct
            player.volume = originalVolume

        } catch (e: Exception) {
            // Restore normal volume on any error
            player.volume = originalVolume
            reportException(e)
        } finally {
            isPerformingCrossfade = false
        }
    }

    private fun fadeInNewTrack() {
        scope.launch {
            try {
                val targetVolume = playerVolume.value * normalizeFactor.value
                val fadeSteps = 20
                val stepDuration = 50L // 1 second total fade-in

                player.volume = 0.1f // Start at low but audible volume

                for (i in 0..fadeSteps) {
                    if (!coroutineContext.isActive) break

                    val progress = i.toFloat() / fadeSteps
                    val volume = 0.1f + (targetVolume - 0.1f) * progress.pow(0.7f)
                    player.volume = volume.coerceAtMost(targetVolume)

                    delay(stepDuration)
                }

                player.volume = targetVolume

            } catch (e: Exception) {
                player.volume = playerVolume.value * normalizeFactor.value
                reportException(e)
            }
        }
    }



    fun setCrossfadeDuration(durationMs: Int) {
        scope.launch {
            dataStore.edit { settings ->
                settings[CrossfadeDurationKey] = durationMs
            }
        }
    }

    private fun createMediaSourceFactory() =
        DefaultMediaSourceFactory(
            createDataSourceFactory(),
            ExtractorsFactory {
                arrayOf(MatroskaExtractor(), FragmentedMp4Extractor())
            },
        )

    private fun createRenderersFactory() =
        object : DefaultRenderersFactory(this) {
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
                        SonicAudioProcessor(),
                    ),
                ).build()
        }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats,
    ) {
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
                            playTime = playbackStats.totalPlayTimeMs,
                        ),
                    )
                } catch (_: SQLException) {
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                val playbackUrl = database.format(mediaItem.mediaId).first()?.playbackUrl
                    ?: YTPlayerUtils.playerResponseForMetadata(mediaItem.mediaId, null)
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

    private fun saveQueueToDisk() {
        if (player.playbackState == STATE_IDLE) {
            filesDir.resolve(PERSISTENT_AUTOMIX_FILE).delete()
            filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            return
        }
        val persistQueue =
            PersistQueue(
                title = queueTitle,
                items = player.mediaItems.mapNotNull { it.metadata },
                mediaItemIndex = player.currentMediaItemIndex,
                position = player.currentPosition,
            )
        val persistAutomix =
            PersistQueue(
                title = "automix",
                items = automixItems.value.mapNotNull { it.metadata },
                mediaItemIndex = 0,
                position = 0,
            )
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
    }

    private fun requestAudioFocus(): Boolean {
        val audioAttributes = android.media.AudioAttributes.Builder()  // ← Use android.media version
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        wasPlayingBeforeFocusLoss = player.isPlaying
                        player.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        wasPlayingBeforeFocusLoss = player.isPlaying
                        player.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        player.volume = 0.2f
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        player.volume = playerVolume.value
                        if (wasPlayingBeforeFocusLoss && !pausedByUser) {
                            player.play()
                        }
                        wasPlayingBeforeFocusLoss = false
                    }
                }
            }.build()

        audioFocusRequest = focusRequest
        val result = audioManager.requestAudioFocus(focusRequest)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
        }
    }


    override fun onDestroy() {
        crossfadeJob?.cancel()

        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }

        if (discordRpc?.isRpcRunning() == true) {
            discordRpc?.closeRPC()
        }

        discordRpc = null
        abandonAudioFocus()
        mediaSession.release()
        player.removeListener(this)
        player.removeListener(sleepTimer)
        player.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    companion object {
        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"

        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
        const val ERROR_CODE_NO_STREAM = 1000001
        const val CHUNK_LENGTH = 512 * 1024L
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE = "persistent_automix.data"
    }
}
