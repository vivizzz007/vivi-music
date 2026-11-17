package com.music.vivi.ui.player.lyricsanimation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * High-performance frame-rate lyric clock steered by playback position.
 * Synchronizes with native display refresh (60Hz, 90Hz, 120Hz) via [withFrameNanos].
 */
internal class LyricClock(startMs: Long) {
    /** Where the lyrics are drawn, in song milliseconds. */
    var displayedMs: Double = startMs.toDouble()
        private set

    private var lastNowMs = Double.NaN
    private var lastReportMs = Long.MIN_VALUE
    private var lastSampledAtMs = Double.NaN
    private var heldAtMs = Double.NEGATIVE_INFINITY
    private var lastDiscontinuity: Long? = null
    private var jumpPending = false

    private var anchorAtMs = Double.NaN
    private var anchorMs = 0.0

    private var rate = 1.0
    private val historyAt = DoubleArray(HISTORY)
    private val historyMs = DoubleArray(HISTORY)
    private var historySize = 0
    private var rateSettled = false

    private var suspectAtMs = Double.NaN
    private var suspectMs = 0L
    private var rejections = 0

    private var glideStartMs = Double.NaN
    private var glideOffsetMs = 0.0

    fun restart() {
        lastNowMs = Double.NaN
        jumpPending = false
        glideStartMs = Double.NaN
        clearSuspect()
    }

    fun hold(positionMs: Long, nowMs: Double) {
        restart()
        displayedMs = positionMs.toDouble()
        heldAtMs = nowMs
    }

    fun frame(nowMs: Double, reportedMs: Long, sampledAtMs: Double, discontinuity: Long): Long {
        val firstOfRun = lastNowMs.isNaN()
        if (discontinuity != lastDiscontinuity) {
            if (lastDiscontinuity != null && !firstOfRun) jumpPending = true
            lastDiscontinuity = discontinuity
        }
        val isNew = reportedMs != lastReportMs ||
            (!sampledAtMs.isNaN() && sampledAtMs != lastSampledAtMs)
        lastReportMs = reportedMs
        lastSampledAtMs = sampledAtMs

        if (firstOfRun) {
            lastNowMs = nowMs
            val takenAt = when {
                sampledAtMs.isNaN() || sampledAtMs <= heldAtMs -> nowMs
                else -> minOf(sampledAtMs, nowMs)
            }
            anchor(takenAt, reportedMs)
            displayedMs = targetAt(nowMs)
            return displayedMs.toLong()
        }

        val dt = (nowMs - lastNowMs).coerceAtLeast(0.0)
        lastNowMs = nowMs
        advance(nowMs, dt)

        if (isNew) {
            val takenAt = if (sampledAtMs.isNaN()) nowMs else minOf(sampledAtMs, nowMs)
            if (takenAt >= anchorAtMs || jumpPending) take(nowMs, takenAt, reportedMs)
        }
        return displayedMs.toLong()
    }

    private fun take(nowMs: Double, takenAt: Double, reportedMs: Long) {
        if (jumpPending) {
            jumpPending = false
            clearSuspect()
            anchor(takenAt, reportedMs)
            jumpTo(nowMs)
            return
        }
        val elapsed = takenAt - anchorAtMs
        if (abs(reportedMs - (anchorMs + elapsed * rate)) <= tolerance(elapsed)) {
            clearSuspect()
            anchor(takenAt, reportedMs, continues = true)
            return
        }

        val sinceSuspect = takenAt - suspectAtMs
        val agrees = !suspectAtMs.isNaN() && sinceSuspect >= 0 &&
            abs(reportedMs - (suspectMs + sinceSuspect * rate)) <= tolerance(sinceSuspect)
        rejections++
        if (agrees) {
            val firstAt = suspectAtMs
            val first = suspectMs
            clearSuspect()
            anchor(firstAt, first)
            anchor(takenAt, reportedMs, continues = true)
        } else if (rejections > MAX_REJECTIONS) {
            clearSuspect()
            anchor(takenAt, reportedMs)
        } else {
            suspectAtMs = takenAt
            suspectMs = reportedMs
        }
    }

    private fun tolerance(elapsedMs: Double): Double =
        OUTLIER_MS + abs(elapsedMs) * if (rateSettled) RATE_SLACK else UNSETTLED_RATE_SLACK

    private fun anchor(atMs: Double, positionMs: Long, continues: Boolean = false) {
        anchorAtMs = atMs
        anchorMs = positionMs.toDouble()
        if (!continues) historySize = 0
        if (historySize == HISTORY) {
            historyAt.copyInto(historyAt, 0, 1, HISTORY)
            historyMs.copyInto(historyMs, 0, 1, HISTORY)
            historySize--
        }
        historyAt[historySize] = atMs
        historyMs[historySize] = anchorMs
        historySize++
        learnRate()
    }

    private fun learnRate() {
        val newest = historySize - 1
        if (newest < 1) return
        var oldest = 0
        while (oldest < newest && historyAt[newest] - historyAt[oldest] > RATE_WINDOW_MS) oldest++
        val span = historyAt[newest] - historyAt[oldest]
        if (span >= MIN_RATE_SPAN_MS) {
            rate = ((historyMs[newest] - historyMs[oldest]) / span).coerceIn(MIN_RATE, MAX_RATE)
            rateSettled = true
            return
        }
        val pairSpan = historyAt[newest] - historyAt[newest - 1]
        if (!rateSettled && pairSpan >= MIN_PAIR_SPAN_MS) {
            val measured = ((historyMs[newest] - historyMs[newest - 1]) / pairSpan)
                .coerceIn(MIN_RATE, MAX_RATE)
            rate += (measured - rate) * PAIR_LEARNING
        }
    }

    private fun clearSuspect() {
        suspectAtMs = Double.NaN
        rejections = 0
    }

    private fun advance(nowMs: Double, dt: Double) {
        val target = targetAt(nowMs)
        val step = dt * rate
        val error = target - (displayedMs + step)
        when {
            !glideStartMs.isNaN() -> {
                val progress = (nowMs - glideStartMs) / GLIDE_MS
                if (progress >= 1.0) {
                    displayedMs = target
                    glideStartMs = Double.NaN
                } else {
                    displayedMs = target + glideOffsetMs * (1.0 - smoothstep(progress))
                }
            }
            error <= -LOST_MS || error >= LOST_MS -> displayedMs = target
            error >= GLIDE_FROM_MS -> {
                startGlide(nowMs - dt, displayedMs - (target - step))
                displayedMs = target + glideOffsetMs * (1.0 - smoothstep(dt / GLIDE_MS))
            }
            else -> {
                val closing = error * (dt / (dt + CORRECTION_MS))
                displayedMs += step + closing.coerceIn(-MAX_SLOW * step, MAX_SLEW * step)
            }
        }
    }

    private fun jumpTo(nowMs: Double) {
        val offset = displayedMs - targetAt(nowMs)
        if (abs(offset) > GLIDE_MAX_MS) {
            displayedMs = targetAt(nowMs)
            glideStartMs = Double.NaN
        } else {
            startGlide(nowMs, offset)
        }
    }

    private fun startGlide(startMs: Double, offsetMs: Double) {
        glideStartMs = startMs
        glideOffsetMs = offsetMs
    }

    private fun targetAt(atMs: Double): Double = anchorMs + (atMs - anchorAtMs) * rate
}

private fun smoothstep(fraction: Double): Double {
    val t = fraction.coerceIn(0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)
}

private const val OUTLIER_MS = 250.0
private const val RATE_SLACK = 0.2
private const val UNSETTLED_RATE_SLACK = 0.75
private const val MAX_REJECTIONS = 2
private const val MIN_RATE_SPAN_MS = 900.0
private const val RATE_WINDOW_MS = 4_000.0
private const val HISTORY = 16
private const val MIN_PAIR_SPAN_MS = 300.0
private const val PAIR_LEARNING = 0.5
private const val MIN_RATE = 0.25
private const val MAX_RATE = 4.0
private const val CORRECTION_MS = 600.0
private const val MAX_SLEW = 0.25
private const val MAX_SLOW = 0.5
private const val GLIDE_FROM_MS = 300.0
private const val GLIDE_MS = 260.0
private const val GLIDE_MAX_MS = 2_000.0
private const val LOST_MS = 3_000.0
private const val SAME_CLOCK_NANOS = 50_000_000L

@Composable
internal fun rememberIsForeground(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { owner, _ ->
            foreground = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return foreground
}

/**
 * Returns a smoothly advancing playhead clock tied to display vsync.
 * Updates [MutableLongState] on frame callbacks so only the active line reading it
 * invalidates during the draw phase, without causing whole-screen recomposition.
 */
@Composable
fun rememberLyricClock(
    trackKey: Any?,
    positionProvider: () -> Long,
    isPlaying: Boolean,
): MutableLongState {
    val initialPos = remember(trackKey) { positionProvider() }
    val clock = remember(trackKey) { mutableLongStateOf(initialPos) }
    val engine = remember(trackKey) { LyricClock(initialPos) }
    val foreground = rememberIsForeground()

    var lastReportedPos by remember(trackKey) { mutableLongStateOf(initialPos) }
    var lastSampledAtNanos by remember(trackKey) { mutableLongStateOf(System.nanoTime()) }
    var seekCount by remember(trackKey) { mutableIntStateOf(0) }

    LaunchedEffect(engine, isPlaying, foreground, trackKey) {
        if (!isPlaying || !foreground) return@LaunchedEffect
        while (true) {
            val raw = positionProvider()
            val nowNanos = System.nanoTime()
            if (raw != lastReportedPos) {
                if (lastReportedPos >= 0L && abs(raw - lastReportedPos) > 1500L) {
                    seekCount++
                }
                lastReportedPos = raw
                lastSampledAtNanos = nowNanos
            }
            delay(50)
        }
    }

    if (!isPlaying) {
        val currentPos = positionProvider()
        LaunchedEffect(engine, currentPos) {
            engine.hold(currentPos, System.nanoTime() / 1_000_000.0)
            clock.longValue = currentPos
            lastReportedPos = currentPos
            lastSampledAtNanos = System.nanoTime()
        }
    }

    LaunchedEffect(engine, isPlaying, foreground, trackKey) {
        if (!isPlaying || !foreground) return@LaunchedEffect
        engine.restart()
        while (true) {
            withFrameNanos { frameNanos ->
                val system = System.nanoTime()
                val now = if (abs(system - frameNanos) < SAME_CLOCK_NANOS) frameNanos else system
                val reported = if (lastReportedPos >= 0L) lastReportedPos else positionProvider()
                val sampledAt = lastSampledAtNanos
                clock.longValue = engine.frame(
                    nowMs = now / 1_000_000.0,
                    reportedMs = reported,
                    sampledAtMs = if (sampledAt > 0L) sampledAt / 1_000_000.0 else Double.NaN,
                    discontinuity = seekCount.toLong(),
                )
            }
        }
    }

    return clock
}

