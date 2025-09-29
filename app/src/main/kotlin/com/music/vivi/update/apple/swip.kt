package com.music.vivi.update.apple

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import android.app.Activity
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_READY
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.constants.PlayerBackgroundStyleKey
import com.music.vivi.constants.SliderStyle
import com.music.vivi.constants.SliderStyleKey
import com.music.vivi.db.entities.LyricsEntity
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.extensions.toggleRepeatMode
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.Lyrics
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.PlayerSliderTrack
import com.music.vivi.ui.component.BigSeekBar
import androidx.navigation.NavController
import androidx.room.util.copy
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.playback.PlayerConnection
import me.saket.squiggles.SquigglySlider
import com.music.vivi.ui.menu.LyricsMenu
import com.music.vivi.ui.theme.PlayerColorExtractor
import com.music.vivi.ui.theme.PlayerSliderColors
import com.music.vivi.utils.rememberEnumPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.runCatching
import com.music.vivi.utils.makeTimeString
import kotlinx.coroutines.flow.first

import androidx.compose.foundation.gestures.detectHorizontalDragGestures

import androidx.compose.ui.input.pointer.pointerInput

import kotlin.math.abs


fun Modifier.SwipeGesture(
    enabled: Boolean = true,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    swipeThreshold: Float = 100f
): Modifier = if (enabled) {
    this.pointerInput(Unit) {
        var totalDrag = 0f

        detectHorizontalDragGestures(
            onDragStart = {
                totalDrag = 0f
            },
            onDragEnd = {
                if (abs(totalDrag) > swipeThreshold) {
                    if (totalDrag > 0) {
                        onSwipeLeft()
                    } else {
                        onSwipeRight()
                    }
                }
                totalDrag = 0f
            },
            onDragCancel = {
                totalDrag = 0f
            },
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                totalDrag += dragAmount
            }
        )
    }
} else {
    this
}