/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.jiosaavn.matcher

import com.music.jiosaavn.models.SaavnSong
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/**
 * High-accuracy song matcher and title normalizer for JioSaavn.
 * Separates track identity from packaging (film labels, official video tags, noise words)
 * and accurately scores candidates to prevent wrong song matching while unlocking 320kbps streams.
 */
object SaavnMatcher {

    data class Target(
        val title: String,
        val artist: String = "",
        val durationSec: Int? = null,
        val album: String? = null,
        val isExplicit: Boolean? = null,
        val isVideo: Boolean = false,
    )

    // ── Asking (Query Generation) ───────────────────────────────────────────

    /**
     * Generate search queries with packaging stripped off, best query first.
     */
    fun queries(target: Target): List<String> {
        val title = searchableTitle(target.title, target.artist)
        if (title.isBlank()) return emptyList()
        val artist = primaryArtist(target.artist)
        val out = if (artist.isBlank()) mutableListOf(title) else mutableListOf("$title $artist", title)
        return out.distinct()
    }

    /** The title with packaging taken off, version markers kept. */
    fun searchableTitle(title: String, artist: String = ""): String =
        parseTitle(title, artist).let { (it.words + it.versions).joinToString(" ") }

    /** First credited artist name. */
    fun primaryArtist(artist: String): String {
        val primary = normalize(artist).split(ARTIST_SEPARATORS).firstOrNull()?.trim().orEmpty()
        return primary.takeUnless { it in YOUTUBE_TYPE_LABELS }.orEmpty()
    }

    // ── Judging & Scoring ───────────────────────────────────────────────────

    /**
     * Pick the single highest scoring candidate that represents the target recording, or null.
     */
    fun best(candidates: List<SaavnSong>, target: Target): SaavnSong? =
        ranked(candidates, target).firstOrNull()

    /**
     * Every candidate that matches target, ordered by confidence score descending.
     */
    fun ranked(candidates: List<SaavnSong>, target: Target): List<SaavnSong> =
        candidates
            .mapNotNull { candidate -> score(candidate, target)?.let { candidate to it } }
            .sortedByDescending { it.second }
            .map { it.first }

    /**
     * Calculate match confidence score, or return null if candidate is rejected.
     */
    fun score(candidate: SaavnSong, target: Target): Int? {
        val candidateArtist = candidate.artists.primary.joinToString(", ") { it.name }
            .ifBlank { candidate.artists.all.joinToString(", ") { it.name } }

        val wanted = parseTitle(target.title, target.artist)
        val got = parseTitle(candidate.name, candidateArtist)
        if (wanted.core.isBlank() || got.core.isBlank()) return null

        val isTitleMatch = when {
            wanted.core == got.core -> true
            wanted.core.length <= 3 || got.core.length <= 3 -> false
            else -> wanted.core.contains(got.core) || got.core.contains(wanted.core)
        }
        if (!isTitleMatch) return null

        // Version check: Remix, Live, Acoustic must match exactly in both directions
        val albumMarks = albumVersionMarkers(candidate.album?.name)
        val effectiveVersions = got.versions + (albumMarks intersect NOVOCAL_TAKES) +
                (albumMarks intersect wanted.versions)
        if (wanted.versions != effectiveVersions) return null

        val creditedArtist = artistScore(target.artist, candidateArtist)
        val duration = durationScore(
            target.durationSec,
            candidate.duration,
            allowVideoDrift = creditedArtist != null,
        ) ?: return null

        val artist = creditedArtist
            ?: CREDITS_DISAGREE.takeIf {
                !target.isVideo && withinSeconds(candidate.duration, target.durationSec, CREDIT_OVERRIDE_SEC)
            }
            ?: return null

        val explicit = explicitScore(target.isExplicit, candidate.explicitContent) ?: return null
        return BASE + artist + duration + albumScore(target.album, candidate.album?.name) + explicit +
                contextScore(wanted, got)
    }

    private fun withinSeconds(gotSec: Int?, wantedSec: Int?, seconds: Int): Boolean {
        if (gotSec == null || wantedSec == null) return false
        return abs(wantedSec - gotSec) <= seconds
    }

    // ── Title Parser ────────────────────────────────────────────────────────

    internal data class TitleParts(
        val words: List<String>,
        val core: String,
        val versions: Set<String>,
        val context: Set<String>,
    )

    internal fun parseTitle(raw: String, artist: String = ""): TitleParts {
        val versions = sortedSetOf<String>()
        val context = mutableSetOf<String>()
        var text = normalize(raw)

        text = text.replace(PT_BR_FEATURING, " ")

        // Bracketed passes
        repeat(BRACKET_PASSES) {
            if (!BRACKETED.containsMatchIn(text)) return@repeat
            text = BRACKETED.replace(text) { match ->
                classify(match.groupValues[1], versions, context)
                " "
            }
        }
        text.indexOfFirst { it == '(' || it == '[' }.takeIf { it >= 0 }?.let { open ->
            classify(text.substring(open), versions, context)
            text = text.substring(0, open)
        }

        // Dash & pipe passes
        repeat(DASH_PASSES) {
            val dash = DASH.find(text) ?: return@repeat
            val head = text.substring(0, dash.range.first)
            val tail = text.substring(dash.range.last + 1)
            text = if (isArtistName(head, artist)) {
                classify(head, versions, context)
                tail
            } else {
                classify(tail, versions, context)
                head
            }
        }

        text = text.replace(FEATURING, " ")

        var words = text.split(WORD_SPLIT)
            .map { it.replace(NON_ALNUM, "") }
            .filter { it.isNotEmpty() && it !in JOINING_WORDS }

        while (words.size > 1) {
            val last = words.last()
            if (last in TRAILING_NOISE) words = words.dropLast(1)
            else if (last in TRAILING_TAKES) {
                versions += last
                words = words.dropLast(1)
            } else break
        }

        return TitleParts(
            words = words,
            core = words.joinToString(""),
            versions = versions,
            context = context,
        )
    }

    private fun classify(
        segment: String,
        versions: MutableSet<String>,
        context: MutableSet<String>,
    ) {
        val words = segment.split(WORD_SPLIT)
            .map { it.replace(NON_ALNUM, "") }
            .filter { it.isNotEmpty() }
        if (words.isEmpty()) return
        if (words.joinToString("") in NEUTRAL_SEGMENTS) return
        val marks = words.filter { it in VERSION_WORDS }
        if (marks.isNotEmpty()) {
            versions += marks
            return
        }
        context += words.filter { it.length > 2 && it !in NOISE_WORDS }
    }

    private fun isArtistName(text: String, artist: String): Boolean {
        if (artist.isBlank()) return false
        val words = text.split(WORD_SPLIT).map { it.replace(NON_ALNUM, "") }.filter { it.isNotEmpty() }
        if (words.isEmpty()) return false
        val credited = normalize(artist).split(WORD_SPLIT)
            .map { it.replace(NON_ALNUM, "") }
            .filter { it.isNotEmpty() }
            .toSet()
        return words.all { it in credited }
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace("&", " and ")

    // ── Artist Scoring ──────────────────────────────────────────────────────

    private fun artistScore(wanted: String, got: String): Int? {
        val want = artistNames(wanted)
        val have = artistNames(got)
        if (want.isEmpty() || have.isEmpty()) return 0
        val shared = sharesArtist(wanted, got)
        if (!shared) return null
        return if (want == have) ARTIST_EXACT else ARTIST_SHARED
    }

    fun sharesArtist(wanted: String, got: String): Boolean {
        val want = artistNames(wanted)
        val have = artistNames(got)
        return want.isNotEmpty() && have.isNotEmpty() &&
                want.any { w -> have.any { h -> sameArtist(w, h) } }
    }

    private fun artistNames(value: String): Set<List<String>> = normalize(value)
        .split(ARTIST_SEPARATORS)
        .map { name ->
            name.split(WORD_SPLIT)
                .map { it.replace(NON_ALNUM, "") }
                .filter { it.length > 1 }
        }
        .filter { it.isNotEmpty() }
        .toSet()

    private fun sameArtist(a: List<String>, b: List<String>) = runOf(a, b) || runOf(b, a)

    private fun runOf(outer: List<String>, inner: List<String>): Boolean {
        if (inner.isEmpty() || inner.size > outer.size) return false
        return (0..outer.size - inner.size).any { at ->
            outer.subList(at, at + inner.size) == inner
        }
    }

    // ── Duration Scoring ────────────────────────────────────────────────────

    private fun durationScore(wanted: Int?, got: Int?, allowVideoDrift: Boolean = false): Int? {
        if (wanted == null || got == null) return 0
        val drift = abs(wanted - got)
        return when {
            drift > DURATION_LIMIT_SEC && allowVideoDrift && drift <= VIDEO_DURATION_LIMIT_SEC -> 0
            drift > DURATION_LIMIT_SEC -> null
            drift <= DURATION_TIGHT_SEC -> DURATION_TIGHT
            else -> DURATION_LOOSE
        }
    }

    // ── Album & Explicit Scoring ────────────────────────────────────────────

    private fun albumScore(wanted: String?, got: String?): Int {
        val want = albumKey(wanted) ?: return 0
        val have = albumKey(got) ?: return 0
        return if (want == have) ALBUM_EXACT else 0
    }

    private fun explicitScore(wanted: Boolean?, got: Boolean?): Int? = when {
        wanted == null || got == null -> 0
        wanted != got -> null
        else -> EXPLICIT_EXACT
    }

    private fun albumVersionMarkers(album: String?): Set<String> =
        if (album.isNullOrBlank()) emptySet() else parseTitle(album).versions

    private fun albumKey(value: String?): String? {
        var text = normalize(value.orEmpty()).trim()
        if (text.isEmpty()) return null
        repeat(BRACKET_PASSES) { text = BRACKETED.replace(text, " ") }
        val words = text.split(WORD_SPLIT)
            .map { it.replace(NON_ALNUM, "") }
            .filter { it.isNotEmpty() && it !in ALBUM_NOISE_WORDS }
        return words.joinToString("").takeIf { it.isNotEmpty() }
    }

    private fun contextScore(wanted: TitleParts, got: TitleParts): Int =
        if (wanted.context.any { it in got.context }) CONTEXT_SHARED else 0

    // ── Weights ─────────────────────────────────────────────────────────────

    private const val BASE = 100
    private const val ARTIST_EXACT = 25
    private const val ARTIST_SHARED = 10
    private const val CREDITS_DISAGREE = -30
    private const val CREDIT_OVERRIDE_SEC = 2
    private const val DURATION_TIGHT = 40
    private const val DURATION_LOOSE = 15
    private const val ALBUM_EXACT = 35
    private const val EXPLICIT_EXACT = 20
    private const val CONTEXT_SHARED = 20

    private const val DURATION_TIGHT_SEC = 3
    const val DURATION_LIMIT_SEC = 16
    private const val VIDEO_DURATION_LIMIT_SEC = 90

    private const val BRACKET_PASSES = 3
    private const val DASH_PASSES = 3

    private val BRACKETED = Regex("""[(\[]([^()\[\]]*)[)\]]""")
    private val DASH = Regex("""\s+[-–—|]+\s+""")
    private val FEATURING = Regex("""\b(feat|ft|featuring|with)\b.*""")
    private val PT_BR_FEATURING = Regex(
        """\bpart\.\s+(?!(?:\d|[ivx]+\b))[^)\]]*[)\]]?|\bpart\s+(?:esp\.?|especial)\b[^)\]]*[)\]]?""",
        RegexOption.IGNORE_CASE,
    )
    private val WORD_SPLIT = Regex("""[\s.·~〜]+""")
    private val NON_ALNUM = Regex("""[^\p{L}\p{N}]""")

    private val ARTIST_SEPARATORS =
        Regex("""\s*(?:[,&/;·|、，×]|\band\b|\bx\b|\bvs\.?\b|\bfeat\.?\b|\bft\.?\b|\bfeaturing\b|\bwith\b)\s*""")

    private val VERSION_WORDS = setOf(
        "remix", "remixes", "rmx", "refix", "flip", "bootleg", "mashup", "medley",
        "live", "concert", "unplugged", "acoustic", "instrumental", "karaoke",
        "vocals", "vocal", "acapella", "acappella", "backing", "stems", "stem",
        "cover", "demo", "reprise", "remake", "rework", "extended", "edit",
        "version", "mix", "dub", "vip", "session", "sessions",
        "sped", "slowed", "reverb", "nightcore", "lofi", "orchestral", "symphonic",
        "part", "pt", "chapter",
    )

    private val TRAILING_TAKES = setOf(
        "instrumental", "karaoke", "acapella", "acappella", "remix", "rmx", "nightcore",
    )

    private val NOVOCAL_TAKES = setOf(
        "instrumental", "karaoke", "acapella", "acappella", "backing", "stems", "stem",
    )

    private val NEUTRAL_SEGMENTS = setOf(
        "albumversion", "originalversion", "originalmix", "singleversion",
        "radioversion", "radioedit", "stereoversion", "monoversion",
        "studioversion", "fullversion", "standardversion", "explicitversion",
        "deluxeversion", "originaltrack",
    )

    private val YOUTUBE_TYPE_LABELS = setOf(
        "música", "musica", "vídeo", "video", "song", "album", "álbum", "artista", "artist",
    )

    private val NOISE_WORDS = setOf(
        "official", "video", "audio", "lyrics", "lyric", "lyrical", "visualizer",
        "song", "songs", "full", "music", "the", "and", "from", "feat", "ft",
        "featuring", "with", "new", "latest", "free", "download", "remaster",
        "remastered", "explicit", "clean", "bonus", "track", "deluxe", "original",
        "album", "single", "hd", "hq", "4k", "mp3",
    )

    private val ALBUM_NOISE_WORDS = setOf(
        "album", "deluxe", "edition", "expanded", "remaster", "remastered",
        "version", "explicit", "clean", "bonus", "anniversary",
    )

    private val TRAILING_NOISE = setOf(
        "song", "songs", "video", "audio", "lyrics", "lyric", "lyrical",
        "official", "full", "hd", "hq", "4k", "mp3", "ost", "soundtrack",
    )

    private val JOINING_WORDS = setOf("and")
}
