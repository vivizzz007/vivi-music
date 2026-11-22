package com.music.vivi.ui.player.lyricsanimation

import androidx.compose.ui.unit.dp
import com.music.vivi.lyrics.LyricsEntry
import com.music.vivi.lyrics.LyricsUtils

val WORD_RISE = 2.dp
val WIPE_FEATHER = 30.dp
val GLOW_RADIUS = 6.dp
const val GROW_HEADROOM = 1.6f
const val UNSUNG_ALPHA = 0.45f

const val PHASE_BEFORE = 0
const val PHASE_MOVING = 1
const val PHASE_AFTER = 2

data class LyricSyllable(
    val startMs: Long,
    val endMs: Long,
    val startChar: Int,
    val endChar: Int,
)

data class LyricWord(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val syllables: List<LyricSyllable> = emptyList(),
) {
    fun charsSungAt(positionMs: Long): Float {
        if (positionMs <= startMs) return 0f
        if (positionMs >= endMs) return text.length.toFloat()
        if (syllables.isEmpty()) {
            val span = (endMs - startMs).coerceAtLeast(1L)
            return (positionMs - startMs).toFloat() / span * text.length
        }
        for (syllable in syllables) {
            if (positionMs < syllable.startMs) return syllable.startChar.toFloat()
            if (positionMs < syllable.endMs) {
                val span = (syllable.endMs - syllable.startMs).coerceAtLeast(1L)
                val through = (positionMs - syllable.startMs).toFloat() / span
                return syllable.startChar + through * (syllable.endChar - syllable.startChar)
            }
        }
        return text.length.toFloat()
    }
}

class CharGrowth {
    var scale: Float = 1f
    var shift: Float = 0f
    var rise: Float = 0f
    var bloom: Float = 0f
}

class GrowingWord internal constructor(
    val index: Int,
    word: LyricWord,
) {
    val startMs: Long = word.startMs
    val endMs: Long = word.endMs

    private val chars: Int = word.text.length
    private val scalePeak = FloatArray(chars)
    private val shiftPeak = FloatArray(chars)
    private val risePeak = FloatArray(chars)
    private val bloomPeak = FloatArray(chars)

    val restsAtMs: Long

    init {
        val held = (endMs - startMs).coerceAtLeast(1L).toFloat()
        val earned = ((held - GROW_RAMP_MIN_MS) / (GROW_RAMP_MAX_MS - GROW_RAMP_MIN_MS))
            .coerceIn(0f, 1f)
            .let { it * it * it }
        val decay = decayRate(chars, held)
        val bloomPace = minOf(GROW_BLOOM_PACE_MAX, held / GROW_BLOOM_PACE_MS)
        val bloomSpread = when {
            chars <= 3 -> GROW_BLOOM_SHORT
            chars >= 6 -> GROW_BLOOM_LONG
            else -> 1f
        }
        val base = if (chars <= 3) GROW_BASE_SHORT else GROW_BASE_LONG
        val liftPace = (held / GROW_LIFT_PACE_MS).coerceIn(GROW_LIFT_FLOOR, 1f)

        for (i in 0 until chars) {
            val place = if (chars > 1) i.toFloat() / (chars - 1) else 0f
            val reach = earned * (1f - place * decay)
            val scale = 1f + base + reach * GROW_SCALE_RANGE
            scalePeak[i] = scale * GROW_SCALE_TRIM
            bloomPeak[i] = (GROW_BLOOM_FLOOR + reach * GROW_BLOOM_RANGE) * bloomPace * bloomSpread
            risePeak[i] = ((scale - 1f) / GROW_SCALE_CEILING) * liftPace
            val centre = (i + 0.5f) / chars
            shiftPeak[i] = (centre - 0.5f) * 2f * (scale - 1f) * GROW_SHIFT_EM * GROW_SCALE_TRIM
        }

        val last = (chars - 1).coerceAtLeast(0) * GROW_STAGGER + GROW_SPAN
        restsAtMs = startMs + (held * last).toLong()
    }

    fun sampleInto(charIndex: Int, positionMs: Long, into: CharGrowth) {
        val span = (endMs - startMs).coerceAtLeast(1L).toFloat()
        val elapsed = positionMs - startMs - charIndex * span * GROW_STAGGER
        val phase = (elapsed / (span * GROW_SPAN)).coerceIn(0f, 1f)
        val peakScale = scalePeak[charIndex]
        when {
            phase < GROW_IN -> {
                val t = smooth(phase / GROW_IN)
                into.scale = 1f + (peakScale - 1f) * t
                into.shift = shiftPeak[charIndex] * t
                into.rise = risePeak[charIndex] * t
                into.bloom = bloomPeak[charIndex] * t
            }
            phase < GROW_HOLD -> {
                into.scale = peakScale
                into.shift = shiftPeak[charIndex]
                into.rise = risePeak[charIndex]
                into.bloom = bloomPeak[charIndex]
            }
            phase < GROW_OUT -> {
                val t = smooth((phase - GROW_HOLD) / (GROW_OUT - GROW_HOLD))
                into.scale = peakScale + (1f - peakScale) * t
                into.shift = shiftPeak[charIndex] * (1f - t)
                into.rise = risePeak[charIndex] + (GROW_REST - risePeak[charIndex]) * t
                into.bloom = bloomPeak[charIndex] * (1f - t)
            }
            else -> {
                into.scale = 1f
                into.shift = 0f
                into.rise = GROW_REST
                into.bloom = 0f
            }
        }
    }
}

class ViviLyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val sungUntilMs: Long? = null,
) {
    val isGap: Boolean get() = text.isBlank()
    val isWordSynced: Boolean get() = words.isNotEmpty()

    val wordSpans: List<IntRange> by lazy(LazyThreadSafetyMode.NONE) {
        var offset = 0
        words.map { word ->
            val start = text.indexOf(word.text, offset, ignoreCase = true).takeIf { it >= 0 }
                ?: text.indexOf(word.text.trim(), offset, ignoreCase = true).takeIf { it >= 0 }
                ?: offset.coerceAtMost(text.length)
            val end = (start + word.text.length).coerceAtMost(text.length)
            offset = end
            start until end
        }
    }

    val sweepSpans: List<IntRange> by lazy(LazyThreadSafetyMode.NONE) {
        words.flatMapIndexed { index, word ->
            val span = wordSpans.getOrNull(index) ?: (0 until 0)
            if (word.syllables.isEmpty()) {
                listOf(span)
            } else {
                word.syllables.map { (span.first + it.startChar) until (span.first + it.endChar) }
            }
        }
    }

    val endMs: Long
        get() = words.lastOrNull()?.endMs ?: sungUntilMs ?: timeMs

    val animatesFromMs: Long by lazy(LazyThreadSafetyMode.NONE) {
        minOf(timeMs, words.firstOrNull()?.startMs ?: timeMs)
    }

    val animatesUntilMs: Long by lazy(LazyThreadSafetyMode.NONE) {
        val settled = words.lastOrNull()?.let { it.endMs + RISE_MS.toLong() } ?: endMs
        val grown = growingWords.maxOfOrNull { it.restsAtMs } ?: Long.MIN_VALUE
        maxOf(endMs, settled, grown)
    }

    val growingWords: List<GrowingWord> by lazy(LazyThreadSafetyMode.NONE) {
        words.mapIndexedNotNull { index, word ->
            if (word.canGrow()) GrowingWord(index, word) else null
        }
    }

    fun growingAt(index: Int): GrowingWord? =
        growingWords.firstOrNull { it.index == index }

    fun isGrowing(positionMs: Long): Boolean = growingWords.any {
        positionMs >= it.startMs && positionMs <= it.restsAtMs
    }

    fun isLifted(positionMs: Long): Boolean {
        val first = words.firstOrNull() ?: return false
        if (positionMs <= first.startMs) return false
        return positionMs < (words.last().endMs + RISE_MS) || isGrowing(positionMs)
    }

    fun wordLift(index: Int, positionMs: Long): Float {
        val word = words.getOrNull(index) ?: return 0f
        val riseMs = (word.endMs - word.startMs).toFloat().coerceIn(RISE_MIN_MS, RISE_MS)
        val rising = ((positionMs - word.startMs) / riseMs).coerceIn(0f, 1f)
        val falling = (1f - (positionMs - word.endMs) / RISE_MS).coerceIn(0f, 1f)
        return liftHeight(word) * smooth(minOf(rising, falling))
    }

    fun wordFall(index: Int, positionMs: Long): Float {
        val word = words.getOrNull(index) ?: return 0f
        val falling = (1f - (positionMs - word.endMs) / RISE_MS).coerceIn(0f, 1f)
        return liftHeight(word) * smooth(falling)
    }

    fun revealedChars(positionMs: Long): Float {
        if (words.isEmpty()) return if (positionMs >= timeMs) text.length.toFloat() else 0f
        var offset = 0
        words.forEachIndexed { index, word ->
            val start = text.indexOf(word.text, offset, ignoreCase = true).takeIf { it >= 0 }
                ?: text.indexOf(word.text.trim(), offset, ignoreCase = true).takeIf { it >= 0 }
                ?: offset.coerceAtMost(text.length)
            val end = (start + word.text.length).coerceAtMost(text.length)
            if (positionMs < word.startMs) return start.toFloat()
            if (positionMs < word.endMs) return start + word.charsSungAt(positionMs)
            val next = words.getOrNull(index + 1)
            if (next != null && positionMs < next.startMs) {
                val gapStart = text.indexOf(next.text, end, ignoreCase = true).takeIf { it >= 0 } ?: end
                val pause = (next.startMs - word.endMs).coerceAtLeast(1L)
                val through = (positionMs - word.endMs).toFloat() / pause
                return end + through * (gapStart - end)
            }
            offset = end
        }
        return text.length.toFloat()
    }
}

fun LyricsEntry.toViviLyricLine(nextEntryTime: Long?): ViviLyricLine {
    val wordList = if (words != null && words.isNotEmpty() && !LyricsUtils.isHindi(text)) {
        words.map { wt ->
            val startMs = (wt.startTime * 1000).toLong()
            val endMs = maxOf(startMs + 50L, (wt.endTime * 1000).toLong())
            LyricWord(
                startMs = startMs,
                endMs = endMs,
                text = wt.text,
            )
        }
    } else {
        emptyList()
    }
    return ViviLyricLine(
        timeMs = time,
        text = text,
        words = wordList,
        sungUntilMs = nextEntryTime,
    )
}

private const val RISE_MS = 700f
private const val RISE_MIN_MS = 250f
private const val LIFT_QUICK_MS = 150f
private const val LIFT_HELD_MS = 900f
private const val LIFT_FLOOR = 0.25f

private fun liftHeight(word: LyricWord): Float {
    val held = (word.endMs - word.startMs).toFloat()
    val through = ((held - LIFT_QUICK_MS) / (LIFT_HELD_MS - LIFT_QUICK_MS)).coerceIn(0f, 1f)
    return LIFT_FLOOR + (1f - LIFT_FLOOR) * smooth(through)
}

private fun smooth(fraction: Float): Float = fraction * fraction * (3f - 2f * fraction)

private fun LyricWord.canGrow(): Boolean {
    val length = text.length
    if (length == 0 || length > GROW_MAX_CHARS) return false
    if ('-' in text || text.any { it.isBlockScript() || it.isJoinedScript() }) return false
    val held = endMs - startMs
    return when {
        length == 1 -> held >= GROW_MIN_SOLO_MS
        length <= 3 -> held >= GROW_MIN_SHORT_MS + (length - 2) * GROW_SHORT_STEP_MS
        length == 4 -> held >= GROW_MIN_FOUR_MS
        else -> held >= GROW_MIN_LONG_MS && held >= length * GROW_MS_PER_CHAR
    }
}

private fun decayRate(length: Int, heldMs: Float): Float {
    val long = length > GROW_DECAY_LONG_CHARS
    val quick = heldMs < GROW_DECAY_QUICK_MS
    if (!long && !quick) return 0f
    var strength = 0f
    if (long) {
        strength += minOf((length - GROW_DECAY_LONG_CHARS) / 5f, 1f) * GROW_DECAY_LONG
    }
    if (quick) {
        val short = maxOf(0f, 1f - (heldMs - GROW_DECAY_QUICK_FLOOR_MS) / 400f)
        strength += short * if (length > 3) GROW_DECAY_QUICK else GROW_DECAY_QUICK_TINY
    }
    return minOf(strength, GROW_DECAY_MAX)
}

private fun Char.isBlockScript(): Boolean =
    this in '一'..'鿿' || this in '぀'..'ゟ' ||
        this in '゠'..'ヿ' || this in '가'..'힯'

private fun Char.isJoinedScript(): Boolean =
    this in '֐'..'ࣿ'

private const val GROW_MAX_CHARS = 7
private const val GROW_MIN_SOLO_MS = 1_100L
private const val GROW_MIN_SHORT_MS = 1_360L
private const val GROW_SHORT_STEP_MS = 140L
private const val GROW_MIN_FOUR_MS = 1_050L
private const val GROW_MIN_LONG_MS = 900L
private const val GROW_MS_PER_CHAR = 200L

private const val GROW_DECAY_LONG_CHARS = 5
private const val GROW_DECAY_QUICK_MS = 1_200f
private const val GROW_DECAY_QUICK_FLOOR_MS = 800f
private const val GROW_DECAY_LONG = 0.4f
private const val GROW_DECAY_QUICK = 0.3f
private const val GROW_DECAY_QUICK_TINY = 0.1f
private const val GROW_DECAY_MAX = 0.7f

private const val GROW_STAGGER = 0.09f
private const val GROW_SPAN = 1.5f
private const val GROW_IN = 0.25f
private const val GROW_HOLD = 0.30f
private const val GROW_OUT = 0.75f
private const val GROW_REST = 1f
private const val GROW_RAMP_MIN_MS = 400f
private const val GROW_RAMP_MAX_MS = 3_000f
private const val GROW_BASE_SHORT = 0.05f
private const val GROW_BASE_LONG = 0.04f
private const val GROW_SCALE_RANGE = 0.08f
private const val GROW_SCALE_CEILING = 0.1f
private const val GROW_SCALE_TRIM = 0.98f
private const val GROW_SHIFT_EM = 25f / 34f
private const val GROW_BLOOM_FLOOR = 0.35f
private const val GROW_BLOOM_RANGE = 0.45f
private const val GROW_BLOOM_PACE_MS = 1_500f
private const val GROW_BLOOM_PACE_MAX = 1.1f
private const val GROW_BLOOM_SHORT = 0.85f
private const val GROW_BLOOM_LONG = 1.1f
private const val GROW_LIFT_PACE_MS = 2_000f
private const val GROW_LIFT_FLOOR = 0.3f

const val MIN_GAP_MS = 4_000L

val LyricsEntry.isInstrumental: Boolean
    get() {
        val trimmed = text.trim()
        return trimmed.isBlank() ||
            trimmed == "♪" ||
            trimmed == "♪♪" ||
            trimmed == "♪ Instrumental ♪" ||
            trimmed.equals("instrumental", ignoreCase = true) ||
            trimmed.equals("[instrumental]", ignoreCase = true) ||
            trimmed.equals("(instrumental)", ignoreCase = true)
    }

fun List<LyricsEntry>.withInstrumentalGaps(): List<LyricsEntry> {
    if (isEmpty()) return this
    val out = ArrayList<LyricsEntry>(size + 8)
    if (first().time >= MIN_GAP_MS && !first().isInstrumental) {
        out += LyricsEntry(time = 0L, text = "")
    }
    forEachIndexed { index, line ->
        out += line
        val next = getOrNull(index + 1) ?: return@forEachIndexed
        if (line.isInstrumental || next.isInstrumental) return@forEachIndexed

        val lineEnd = if (!line.words.isNullOrEmpty()) {
            (line.words.last().endTime * 1000).toLong()
        } else {
            val estimatedDurationMs = (line.text.length * 120L).coerceIn(2_000L, 4_500L)
            line.time + estimatedDurationMs
        }

        val silence = next.time - lineEnd
        if (silence >= MIN_GAP_MS && lineEnd > line.time && lineEnd < next.time) {
            out += LyricsEntry(time = lineEnd, text = "")
        }
    }
    return out
}

enum class LyricAlignment { Start, End }

val DUET_LANE = 44.dp

private const val GROUP = "group"
private const val OTHER = "other"
private const val GROUP_AGENT = "v1000"
private const val OTHER_AGENT = "v2000"
private const val MOSTLY_RIGHT = 0.85f

fun computeLineAlignments(lines: List<LyricsEntry>): List<LyricAlignment> {
    var left = true
    var lastVoice: String? = null
    var rightward = 0
    var placed = 0

    val sides = lines.map { entry ->
        val rawSinger = entry.agent
        if (rawSinger.isNullOrBlank() || entry.text.isBlank()) return@map LyricAlignment.Start
        val singer = rawSinger.trim()
        val type = when (singer.lowercase()) {
            GROUP_AGENT, "group" -> GROUP
            OTHER_AGENT, "other" -> OTHER
            else -> "person"
        }
        placed += 1
        if (type == GROUP) return@map LyricAlignment.Start

        when {
            lastVoice == null -> left = type != OTHER
            singer != lastVoice -> left = !left
        }
        lastVoice = singer

        if (!left) rightward += 1
        if (left) LyricAlignment.Start else LyricAlignment.End
    }

    if (placed == 0 || rightward.toFloat() / placed < MOSTLY_RIGHT) return sides
    return sides.map {
        if (it == LyricAlignment.Start) LyricAlignment.End else LyricAlignment.Start
    }
}


