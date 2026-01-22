package com.music.vivi.playback.managers

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.exoplayer.ExoPlayer
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.HideVideoSongsKey
import com.music.vivi.extensions.SilentHandler
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.playback.queues.EmptyQueue
import com.music.vivi.playback.queues.Queue
import com.music.vivi.playback.queues.filterExplicit
import com.music.vivi.playback.queues.filterVideoSongs
import com.music.vivi.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject

class QueueManager @Inject constructor(
    private val context: Context,
    private val player: ExoPlayer,
    private val dataStore: DataStore<Preferences>,
) {
    var scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    
    var currentQueue: Queue = EmptyQueue
        private set
        
    var queueTitle: String? = null
        private set

    val addedSuggestionIds = Collections.synchronizedSet(LinkedHashSet<String>())

    fun setScope(newScope: CoroutineScope) {
        scope = newScope
    }

    fun playQueue(
        queue: Queue,
        playWhenReady: Boolean = true,
    ) {
        addedSuggestionIds.clear()
        // KORREKTUR: Job() muss zum Context addiert werden, nicht zum Scope-Objekt
        if (!scope.isActive) scope = CoroutineScope(Dispatchers.Main + Job())
        
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
                    queue.getInitialStatus()
                        .filterExplicit(dataStore.get(HideExplicitKey, false))
                        .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
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
                    if (initialStatus.mediaItemIndex > 0) {
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
    
    fun clearQueue() {
        currentQueue = EmptyQueue
        queueTitle = null
        addedSuggestionIds.clear()
    }

    fun setQueue(queue: Queue) {
        currentQueue = queue
    }

    fun setQueueTitle(title: String?) {
        queueTitle = title
    }
}
