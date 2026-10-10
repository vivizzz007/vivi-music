/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.music.vivi.R
import com.music.vivi.constants.PureBlackKey
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Minimal song description used by [PlaylistSpinWheelDialog] so any playlist type can use it. */
data class SpinWheelItem(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
)

// Angle (degrees) between two neighbouring covers on the wheel.
private const val STEP_DEGREES = 14f

// How many covers are drawn on each side of the centre one (the rest is off-screen anyway).
private const val VISIBLE_HALF = 4

// Cubic ease-out: velocity at t = 0 is exactly 3 * distance / duration, which lets a finger fling hand over to the
// animation without a visible jump in speed.
private val CubicOut = Easing { f -> 1f - (1f - f) * (1f - f) * (1f - f) }

// Fast kick, long and silky deceleration — used for the "Spin" button.
private val SpinEasing = CubicBezierEasing(0.25f, 0.6f, 0.1f, 1f)

private fun floorMod(a: Int, n: Int): Int = ((a % n) + n) % n

/**
 * Full-screen half-circle "wheel of covers". Drag / flick the wheel (or press Spin) and it settles on a random
 * song, which is reported through [onSongLanded] so the caller can start playing it.
 */
@Composable
fun PlaylistSpinWheelDialog(
    playlistName: String,
    items: List<SpinWheelItem>,
    onSongLanded: (index: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val (pureBlack) = rememberPreference(PureBlackKey, defaultValue = false)
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val count = items.size

    // Wheel position in "cards": integer values mean a card sits exactly under the pointer.
    var position by remember { mutableFloatStateOf(0f) }
    var isInteracting by remember { mutableStateOf(false) }
    var isSpinning by remember { mutableStateOf(false) }
    var landedIndex by remember { mutableIntStateOf(-1) }
    var landedToken by remember { mutableIntStateOf(0) }
    var animJob by remember { mutableStateOf<Job?>(null) }
    val intro = remember { Animatable(0f) }

    val centerIndex by remember { derivedStateOf { position.roundToInt() } }

    // Intro: the wheel unrolls into place while the scrim fades in.
    LaunchedEffect(Unit) {
        position = -2.6f
        launch { intro.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
        animate(
            initialValue = -2.6f,
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessVeryLow),
        ) { value, _ -> position = value }
    }

    // Tick haptics every time a new cover passes under the pointer.
    LaunchedEffect(Unit) {
        var last = 0L
        snapshotFlow { position.roundToInt() }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                val now = System.currentTimeMillis()
                if (now - last >= 45L) {
                    last = now
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
    }

    // Once a song has been picked, keep the overlay for a moment so the result is visible, then close.
    LaunchedEffect(landedToken) {
        if (landedToken == 0) return@LaunchedEffect
        delay(1500)
        onDismiss()
    }

    fun land() {
        val idx = floorMod(position.roundToInt(), count)
        position = position.roundToInt().toFloat()
        landedIndex = idx
        landedToken += 1
        isSpinning = false
        view.performHapticFeedback(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.LONG_PRESS
            },
        )
        onSongLanded(idx)
    }

    fun startSettle(targetCards: Float, durationMs: Int, easing: Easing) {
        animJob?.cancel()
        animJob = scope.launch {
            isSpinning = true
            landedIndex = -1
            animate(
                initialValue = position,
                targetValue = targetCards,
                animationSpec = tween(durationMs, easing = easing),
            ) { value, _ -> position = value }
            land()
        }
    }

    fun spinRandom() {
        if (isSpinning) return
        landedToken = 0
        animJob?.cancel()
        animJob = scope.launch {
            isSpinning = true
            landedIndex = -1
            val start = position
            // Tiny anticipation: pull back a little before the kick, like winding up a real wheel.
            animate(
                initialValue = start,
                targetValue = start - 0.45f,
                animationSpec = tween(240, easing = FastOutSlowInEasing),
            ) { value, _ -> position = value }
            val distance = Random.nextInt(24, 47)
            val duration = 4300 + distance * 28
            val from = position
            animate(
                initialValue = from,
                targetValue = from + distance + 0.45f,
                animationSpec = tween(duration, easing = SpinEasing),
            ) { value, _ -> position = value }
            land()
        }
    }

    fun flingTo(velocityCardsPerSec: Float) {
        val v = velocityCardsPerSec
        val naturalTravel = v * 0.75f
        val distance = naturalTravel.roundToInt()
        val target = position.roundToInt() + distance
        if (distance == 0 || abs(v) < 1.2f) {
            // Gentle release: just glide to the closest cover.
            startSettle(position.roundToInt().toFloat(), 420, FastOutSlowInEasing)
        } else {
            val duration = (3f * abs(target - position) / abs(v) * 1000f).toInt().coerceIn(500, 6500)
            startSettle(target.toFloat(), duration, CubicOut)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val scrimColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor.copy(alpha = 0.96f * intro.value))
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = playlistName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .graphicsLayer { alpha = intro.value },
                )
                Spacer(Modifier.height(10.dp))

                // Live title / artist of whatever cover is currently under the pointer.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .padding(horizontal = 28.dp)
                        .graphicsLayer { alpha = intro.value },
                ) {
                    val current = items[floorMod(centerIndex, count)]
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = current.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(12.dp))

                WheelOfCovers(
                    items = items,
                    position = { position },
                    centerIndex = centerIndex,
                    landedIndex = landedIndex,
                    introProgress = { intro.value },
                    onDragStart = {
                        isInteracting = true
                        landedToken = 0
                        animJob?.cancel()
                        isSpinning = false
                    },
                    onDrag = { deltaCards -> position += deltaCards },
                    onDragStop = { velocityCards ->
                        isInteracting = false
                        flingTo(velocityCards)
                    },
                    onCardClick = { k ->
                        val relative = k - position.roundToInt()
                        if (relative == 0) {
                            // Tapping the cover under the pointer plays it right away.
                            if (!isSpinning) {
                                landedToken = 0
                                startSettle(k.toFloat(), 1, FastOutSlowInEasing)
                            }
                        } else {
                            landedToken = 0
                            startSettle(k.toFloat(), (380 + abs(relative) * 90), FastOutSlowInEasing)
                        }
                    },
                )

                Spacer(Modifier.height(20.dp))

                val hint = when {
                    landedIndex >= 0 -> stringResource(R.string.spin_wheel_now_playing)
                    isSpinning || isInteracting -> stringResource(R.string.spin_wheel_spinning)
                    else -> stringResource(R.string.spin_wheel_hint)
                }
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (landedIndex >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.graphicsLayer { alpha = intro.value },
                )

                Spacer(Modifier.height(16.dp))

                SpinButton(
                    enabled = !isSpinning && !isInteracting,
                    onClick = ::spinRandom,
                    modifier = Modifier.graphicsLayer {
                        alpha = intro.value
                        translationY = (1f - intro.value) * 40f
                    },
                )
            }
        }
    }
}

@Composable
private fun SpinButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 28.dp, vertical = 14.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.shuffle),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.spin_wheel_spin),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WheelOfCovers(
    items: List<SpinWheelItem>,
    position: () -> Float,
    centerIndex: Int,
    landedIndex: Int,
    introProgress: () -> Float,
    onDragStart: () -> Unit,
    onDrag: (deltaCards: Float) -> Unit,
    onDragStop: (velocityCards: Float) -> Unit,
    onCardClick: (card: Int) -> Unit,
) {
    val density = LocalDensity.current
    val count = items.size

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f / 0.78f),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val cardSizeDp = maxWidth * 0.34f
        val cardSizePx = with(density) { cardSizeDp.toPx() }
        val radiusPx = widthPx * 1.0f
        val stepRad = Math.toRadians(STEP_DEGREES.toDouble())
        val pxPerCard = (radiusPx * stepRad).toFloat()
        val apexCenterY = cardSizePx * 0.62f

        val dragState = rememberDraggableState { delta -> onDrag(-delta / pxPerCard) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = { onDragStart() },
                    onDragStopped = { velocity -> onDragStop(-velocity / pxPerCard) },
                ),
        ) {
            for (k in (centerIndex - VISIBLE_HALF)..(centerIndex + VISIBLE_HALF)) {
                val item = items[floorMod(k, count)]
                androidx.compose.runtime.key(k) {
                    val isLanded = landedIndex >= 0 && k == centerIndex
                    Box(
                        modifier = Modifier
                            .size(cardSizeDp)
                            .zIndex(1f - (abs(k - centerIndex)).coerceAtMost(VISIBLE_HALF) * 0.1f)
                            .graphicsLayer {
                                val relative = k - position()
                                val angleDeg = relative * STEP_DEGREES
                                val angleRad = Math.toRadians(angleDeg.toDouble())
                                val cx = widthPx / 2f + (radiusPx * sin(angleRad)).toFloat()
                                val cy = apexCenterY + radiusPx * (1f - cos(angleRad)).toFloat()
                                val intro = introProgress()
                                translationX = cx - cardSizePx / 2f
                                translationY = cy - cardSizePx / 2f + (1f - intro) * cardSizePx * 1.4f
                                rotationZ = angleDeg
                                // Cover under the pointer grows slightly and the outermost ones fade out.
                                val focus = (1f - abs(relative)).coerceIn(0f, 1f)
                                val scale = 0.92f + 0.2f * focus
                                scaleX = scale
                                scaleY = scale
                                val edge = ((abs(relative) - (VISIBLE_HALF - 1.4f)) / 1.4f).coerceIn(0f, 1f)
                                alpha = (1f - edge) * intro
                            },
                    ) {
                        WheelCard(
                            item = item,
                            highlighted = isLanded,
                            position = position,
                            card = k,
                            onClick = { onCardClick(k) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelCard(
    item: SpinWheelItem,
    highlighted: Boolean,
    position: () -> Float,
    card: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val ring = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(14.dp, shape, clip = false)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = if (highlighted) 3.dp else 1.dp,
                color = if (highlighted) ring else Color.White.copy(alpha = 0.12f),
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        if (item.thumbnailUrl != null) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
            )
        }
        // Soft darkening on the cards that are away from the pointer gives the centre one depth.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = (abs(card - position()) * 0.35f).coerceIn(0f, 0.55f)
                }
                .background(Color.Black),
        )
    }
}
