package com.music.vivi.applecanvas

import com.music.vivi.canvas.CanvasArtwork
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException

private object AppleCanvasLogger {
    fun d(msg: String) = println("AppleMusicCanvas: D: $msg")
    fun w(msg: String) = println("AppleMusicCanvas: W: $msg")
    fun e(t: Throwable, msg: String) {
        println("AppleMusicCanvas: E: $msg")
        t.printStackTrace()
    }
}

/**
 * Fetches Apple Music album motion artwork (HLS canvas) for the player.
 *
 * Search strategy (strict):
 * - Always queries using: album + artist + song
 * - No album = no canvas (returns null immediately)
 * - Edition-aware: Deluxe/Explicit/Clean editions each have their own canvas;
 *   a mismatch between the requested edition and the result's edition is a hard reject.
 * - Only shows a canvas when the result belongs to the exact album the song is from.
 *
 * Results are cached for 24 hours.
 */
object AppleMusicCanvasProvider {

    // Public read-only JWT used by the Apple Music web player for unauthenticated catalog reads.
    private const val APPLE_MUSIC_TOKEN =
        "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IldlYlBsYXlLaWQifQ" +
        ".eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzc0NDU2MzgyLCJleHAiOjE3ODE3" +
        "MTM5ODIsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ" +
        ".4n8qYF4qa18sL1E0G9A3qX35cD8wQ-IJcS9Bh8ZT8JV_yLBtVq46B-9-2ZS3EvWHuw3yK9BYFYAhAdTaDm38vQ"

    private const val AMP_BASE_URL = "https://amp-api.music.apple.com"

    /**
     * Edition keywords that create distinct, separately-canvased entries on Apple Music.
     * A Deluxe canvas must not be shown for the standard edition and vice versa.
     */
    private val EDITION_KEYWORDS = listOf(
        "deluxe", "explicit", "clean", "censored",
        "expanded", "anniversary", "remastered", "bonus",
        "3am", "platinum", "super", "special"
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
                // iTunes returns text/javascript for JSON responses
                register(ContentType.Text.JavaScript, KotlinxSerializationConverter(json))
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 25_000
                socketTimeoutMillis = 25_000
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            install(HttpCache)
            expectSuccess = false
        }
    }

    private data class CacheEntry(
        val value: CanvasArtwork?,
        val expiresAtMs: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 hours

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Primary entry point. Searches Apple Music using all three identifiers:
     *   album + artist + song
     *
     * Returns null immediately if [album] is blank — we never guess without
     * a known album, because that would risk showing the wrong canvas.
     *
     * Edition-aware: if the song belongs to the Deluxe edition, only a Deluxe
     * canvas is returned. If the song is from the standard album, only the
     * standard canvas is returned.
     */
    suspend fun getBySongAlbumArtist(
        song: String,
        album: String,
        artist: String,
        storefront: String = "us",
    ): CanvasArtwork? {
        if (album.isBlank()) {
            AppleCanvasLogger.d("getBySongAlbumArtist: album is blank — skipping")
            return null
        }
        AppleCanvasLogger.d("getBySongAlbumArtist: song='$song', album='$album', artist='$artist'")
        val key = cacheKey("strict", song, album, artist, storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val result = strictSearch(song = song, album = album, artist = artist, storefront = storefront)
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    /**
     * Convenience wrapper for album-screen lookups (no individual song title known).
     * Uses the album title as the song term — Apple Music's album editorialVideo
     * is still found via the album+artist query.
     */
    suspend fun getByAlbumArtist(
        album: String,
        artist: String,
        storefront: String = "us",
    ): CanvasArtwork? {
        if (album.isBlank()) return null
        AppleCanvasLogger.d("getByAlbumArtist: album='$album', artist='$artist'")
        // Use the album name as both song and album; this finds album-level editorialVideo.
        return getBySongAlbumArtist(song = album, album = album, artist = artist, storefront = storefront)
    }

    /**
     * Convenience wrapper for song-based lookups.
     * [album] is required — returns null immediately when blank.
     */
    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
        storefront: String = "us",
    ): CanvasArtwork? {
        if (album.isNullOrBlank()) {
            AppleCanvasLogger.d("getBySongArtist: album is blank — skipping Apple Music lookup")
            return null
        }
        return getBySongAlbumArtist(song = song, album = album, artist = artist, storefront = storefront)
    }

    /**
     * Direct lookup by Apple Music album ID (used by the album detail screen).
     */
    suspend fun getByAlbumId(
        albumId: String,
        storefront: String = "us",
    ): CanvasArtwork? {
        val key = cacheKey("id", albumId, storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val result = fetchMotionArtwork(albumId, storefront, null)
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core search — strict album + artist + song, no fallback
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Performs a single AMP API search with query = "$album $artist $song".
     * No retry / fallback queries are attempted.
     *
     * Results are scored and filtered strictly:
     * - Artist must match (fuzzy).
     * - Album must match (fuzzy allowed only when editions also match).
     * - Edition mismatch (Deluxe vs standard, Explicit vs Clean, etc.) is a
     *   hard reject — the score drops by 50, which is always below the accept
     *   threshold of 12.
     */
    private suspend fun strictSearch(
        song: String,
        album: String,
        artist: String,
        storefront: String,
    ): CanvasArtwork? {
        return runCatching {
            // Build the query: album + artist + song (most specific context for AMP)
            val query = buildQuery(album = album, artist = artist, song = song)
            AppleCanvasLogger.d("strictSearch: query='$query' in storefront='$storefront'")

            val requestedEditions = editionProfile(album)

            val url = "$AMP_BASE_URL/v1/catalog/$storefront/search"
            val response = client.get(url) {
                header("Authorization", "Bearer $APPLE_MUSIC_TOKEN")
                header("Origin", "https://music.apple.com")
                header("Referer", "https://music.apple.com/")
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                parameter("term", query)
                parameter("types", "songs")
                parameter("limit", "10")
                parameter("extend", "editorialVideo")
                parameter("include", "albums")
            }

            if (response.status != HttpStatusCode.OK) {
                AppleCanvasLogger.w("AMP search failed with status ${response.status}")
                return@runCatching null
            }

            val root = response.body<JsonObject>()
            val results = root["results"]?.jsonObject
                ?.get("songs")?.jsonObject
                ?.get("data")?.jsonArray
                ?: return@runCatching null

            // Score every result; keep only those that pass strict filters
            val scoredResults = results.mapNotNull { item ->
                scoreResult(
                    item = item.jsonObject,
                    requestedSong = song,
                    requestedAlbum = album,
                    requestedArtist = artist,
                    requestedEditions = requestedEditions,
                )
            }.sortedByDescending { it.first }

            AppleCanvasLogger.d("strictSearch: ${scoredResults.size} results passed scoring for query='$query'")

            // Walk results best-first, resolve album, and look for editorialVideo
            for ((score, item) in scoredResults) {
                if (score < 12) {
                    AppleCanvasLogger.d("  skipping low-score result (score=$score)")
                    break // list is sorted; no point continuing
                }

                val obj = item.jsonObject
                val attributes = obj["attributes"]?.jsonObject ?: continue
                val resultName = attributes["name"]?.jsonPrimitive?.contentOrNull ?: ""
                val resultArtist = attributes["artistName"]?.jsonPrimitive?.contentOrNull ?: ""
                val resultAlbum = attributes["collectionName"]?.jsonPrimitive?.contentOrNull ?: ""

                // Resolve the album ID from the song result
                val targetAlbumId = resolveAlbumId(obj, attributes) ?: run {
                    AppleCanvasLogger.d("  skipping '$resultName' — could not resolve albumId")
                    continue
                }

                if (targetAlbumId.startsWith("pl.")) {
                    AppleCanvasLogger.d("  skipping playlist id $targetAlbumId for '$resultName'")
                    continue
                }

                AppleCanvasLogger.d("  trying albumId=$targetAlbumId for '$resultName' by '$resultArtist' from '$resultAlbum' (score=$score)")

                // 1. Check for inline editorialVideo first (no extra network call)
                val ev = attributes["editorialVideo"]?.jsonObject
                if (ev != null) {
                    val hlsUrl = extractEditorialVideoUrl(ev)
                    if (!hlsUrl.isNullOrBlank()) {
                        AppleCanvasLogger.d("  found inline editorialVideo for '$resultName'")
                        return@runCatching CanvasArtwork(
                            name = resultName,
                            artist = resultArtist,
                            albumId = targetAlbumId,
                            albumName = resultAlbum,
                            animated = hlsUrl,
                        )
                    }
                }

                // 2. Fetch full album metadata for editorialVideo
                val fetched = fetchMotionArtwork(
                    albumId = targetAlbumId,
                    storefront = storefront,
                    fallbackArtist = resultArtist,
                    titleOverride = resultName,
                    artistOverride = resultArtist,
                )
                if (fetched != null) return@runCatching fetched
            }

            AppleCanvasLogger.d("strictSearch: no canvas found for song='$song', album='$album'")
            null
        }.onFailure {
            if (it is CancellationException) throw it
            AppleCanvasLogger.e(it, "error in strictSearch for song='$song', album='$album'")
        }.getOrNull()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scoring
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scores a single AMP search result against the requested song/album/artist.
     *
     * Returns null (item excluded) if:
     * - Artist doesn't match at all (not even fuzzy).
     * - Album doesn't match at all (not even fuzzy).
     * - Edition mismatch (Deluxe vs non-Deluxe, Explicit vs Clean, etc.).
     * - The result is a blacklisted playlist / DJ mix / editorial station.
     *
     * A score >= 12 is accepted; below that is rejected in the caller.
     */
    private fun scoreResult(
        item: JsonObject,
        requestedSong: String,
        requestedAlbum: String,
        requestedArtist: String,
        requestedEditions: Set<String>,
    ): Pair<Int, JsonObject>? {
        val attributes = item["attributes"]?.jsonObject ?: return null

        val resultName   = attributes["name"]?.jsonPrimitive?.contentOrNull ?: ""
        val resultArtist = attributes["artistName"]?.jsonPrimitive?.contentOrNull ?: ""
        val resultAlbum  = attributes["collectionName"]?.jsonPrimitive?.contentOrNull ?: ""

        // ── Blacklist filter ─────────────────────────────────────────────────
        val nameLower   = resultName.lowercase(Locale.ROOT)
        val albumLower  = resultAlbum.lowercase(Locale.ROOT)
        val isBlacklisted = nameLower.contains("playlist") || nameLower.contains("set list") ||
                albumLower.contains("playlist") || albumLower.contains("set list") ||
                nameLower.contains("essentials") || albumLower.contains("essentials") ||
                albumLower.contains("dj mix") || albumLower.contains("mixed") ||
                albumLower.contains("apple music") || albumLower.contains("today's hits") ||
                nameLower.contains("session") || albumLower.contains("session")
        if (isBlacklisted) {
            AppleCanvasLogger.d("  excluded blacklisted: '$resultName' (album: '$resultAlbum')")
            return null
        }

        var score = 0

        // ── Artist match ─────────────────────────────────────────────────────
        val artistExact = resultArtist.equals(requestedArtist, ignoreCase = true)
        val artistFuzzy = resultArtist.contains(requestedArtist, ignoreCase = true) ||
                requestedArtist.contains(resultArtist, ignoreCase = true)
        if (!artistFuzzy) {
            AppleCanvasLogger.d("  excluded (artist mismatch): '$resultName' by '$resultArtist'")
            return null
        }
        score += if (artistExact) 10 else 5

        // ── Song title match ─────────────────────────────────────────────────
        val songExact = resultName.equals(requestedSong, ignoreCase = true)
        val songFuzzy = resultName.contains(requestedSong, ignoreCase = true) ||
                requestedSong.contains(resultName, ignoreCase = true)
        when {
            songExact  -> score += 15
            songFuzzy  -> score += 7
            else       -> score -= 10  // wrong song by the same artist
        }

        // ── Album match + edition-aware check ────────────────────────────────
        val resultEditions = editionProfile(resultAlbum)

        // Hard reject when editions don't match:
        // e.g. requested = "Midnights" (no edition) but result = "Midnights (3am Edition)"
        //      requested = "Midnights (Deluxe)" but result = "Midnights"
        if (requestedEditions != resultEditions) {
            AppleCanvasLogger.d("  excluded (edition mismatch): requested editions=$requestedEditions, result editions=$resultEditions for '$resultAlbum'")
            score -= 50 // guaranteed below accept threshold
        }

        val albumExact = resultAlbum.equals(requestedAlbum, ignoreCase = true)
        val albumFuzzy = resultAlbum.contains(requestedAlbum, ignoreCase = true) ||
                requestedAlbum.contains(resultAlbum, ignoreCase = true)

        when {
            albumExact  -> score += 20
            albumFuzzy && requestedEditions == resultEditions -> score += 8
            !albumFuzzy -> {
                AppleCanvasLogger.d("  excluded (album mismatch): '$resultAlbum' vs requested '$requestedAlbum'")
                score -= 50 // hard reject — wrong album
            }
        }

        AppleCanvasLogger.d("  scored=$score: '$resultName' by '$resultArtist' on '$resultAlbum'")
        return score to item
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Album motion artwork fetch
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun fetchMotionArtwork(
        albumId: String,
        storefront: String,
        fallbackArtist: String?,
        titleOverride: String? = null,
        artistOverride: String? = null,
    ): CanvasArtwork? {
        if (albumId.startsWith("pl.")) {
            AppleCanvasLogger.d("fetchMotionArtwork: ignoring playlist id $albumId")
            return null
        }
        return runCatching {
            AppleCanvasLogger.d("fetchMotionArtwork: fetching albumId=$albumId")
            val url = "$AMP_BASE_URL/v1/catalog/$storefront/albums/$albumId"
            val response = client.get(url) {
                header("Authorization", "Bearer $APPLE_MUSIC_TOKEN")
                header("Origin", "https://music.apple.com")
                header("Referer", "https://music.apple.com/")
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                parameter("extend", "editorialVideo")
                parameter("include", "tracks")
            }
            if (response.status != HttpStatusCode.OK) {
                AppleCanvasLogger.w("album fetch failed for $albumId: ${response.status}")
                return@runCatching null
            }

            val root = response.body<JsonObject>()
            val data = root["data"]?.jsonArray
            if (data.isNullOrEmpty()) return@runCatching null

            val albumObj   = data.firstOrNull()?.jsonObject ?: return@runCatching null
            val attributes = albumObj["attributes"]?.jsonObject
            val albumName  = attributes?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
            val artistName = attributes?.get("artistName")?.jsonPrimitive?.contentOrNull ?: fallbackArtist

            // Blacklist check at album level
            val nameLower = albumName.lowercase(Locale.ROOT)
            val isBlacklisted = nameLower.contains("playlist") || nameLower.contains("set list") ||
                    nameLower.contains("essentials") || nameLower.contains("dj mix") ||
                    nameLower.contains("mixed") || nameLower.contains("apple music") ||
                    nameLower.contains("today's hits") || nameLower.contains("session")
            if (isBlacklisted) {
                AppleCanvasLogger.d("fetchMotionArtwork: ignoring blacklisted album '$albumName'")
                return@runCatching null
            }

            val finalTitle  = titleOverride ?: albumName
            val finalArtist = artistOverride ?: artistName

            // Strategy: editorialVideo on the album
            val ev = attributes?.get("editorialVideo")?.jsonObject
            if (ev != null) {
                val videoUrl = extractEditorialVideoUrl(ev)
                if (!videoUrl.isNullOrBlank()) {
                    AppleCanvasLogger.d("fetchMotionArtwork: found editorialVideo for '$finalTitle' (album: $albumName, id: $albumId)")
                    return@runCatching CanvasArtwork(
                        name = finalTitle,
                        artist = finalArtist,
                        albumId = albumId,
                        albumName = albumName,
                        animated = videoUrl,
                    )
                }
            }

            AppleCanvasLogger.d("fetchMotionArtwork: no editorialVideo for $albumId (keys: ${attributes?.keys})")
            null
        }.onFailure {
            if (it is CancellationException) throw it
            AppleCanvasLogger.e(it, "error in fetchMotionArtwork for $albumId")
        }.getOrNull()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the AMP search query as "album artist song".
     * Special characters (&, ,) are normalised to spaces.
     */
    private fun buildQuery(album: String, artist: String, song: String): String {
        return "$album $artist $song"
            .replace("&", " ")
            .replace(",", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Returns the set of edition keywords present in [text] (lower-cased).
     * Used for edition-aware comparison: two album names must share the same
     * edition profile to be considered compatible.
     *
     * Examples:
     *   "Midnights"             → {}
     *   "Midnights (Deluxe)"    → {"deluxe"}
     *   "Midnights (3am Edition)" → {"3am"}
     *   "Thriller (Explicit)"   → {"explicit"}
     */
    private fun editionProfile(text: String): Set<String> {
        val lower = text.lowercase(Locale.ROOT)
        return EDITION_KEYWORDS.filter { lower.contains(it) }.toSet()
    }

    /**
     * Resolves the Apple Music album ID from a song result object.
     * Tries (in order):
     *  1. relationships.albums.data[0].id
     *  2. attributes.collectionId
     *  3. Parse from attributes.url
     */
    private fun resolveAlbumId(obj: JsonObject, attributes: JsonObject): String? {
        // 1. relationships
        val fromRelationship = obj["relationships"]?.jsonObject
            ?.get("albums")?.jsonObject
            ?.get("data")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("id")?.jsonPrimitive?.contentOrNull
        if (!fromRelationship.isNullOrBlank()) return fromRelationship

        // 2. collectionId attribute (iTunes-style)
        val collectionId = attributes["collectionId"]?.jsonPrimitive?.contentOrNull
        if (!collectionId.isNullOrBlank()) return collectionId

        // 3. Parse from URL: https://music.apple.com/region/album/name/ID?i=songId
        val url = attributes["url"]?.jsonPrimitive?.contentOrNull ?: return null
        val albumPart = url.substringAfter("/album/", "").substringBefore("?")
        val id = albumPart.substringAfterLast("/", "")
        return if (id.isNotBlank() && id.all { it.isDigit() }) id else null
    }

    /**
     * Extracts the best available HLS/video URL from an editorialVideo object.
     * Priority: motionDetailRaw > motionDetailSquare > motionDetailTall > motionDetailStatic
     */
    private fun extractEditorialVideoUrl(ev: JsonObject): String? {
        val assets = listOf(
            ev["motionDetailRaw"]?.jsonObject,
            ev["motionDetailSquare"]?.jsonObject,
            ev["motionDetailTall"]?.jsonObject,
            ev["motionDetailStatic"]?.jsonObject,
        ).filterNotNull()

        for (asset in assets) {
            val video = asset["video"]?.jsonPrimitive?.contentOrNull
                ?: asset["videoUrl"]?.jsonPrimitive?.contentOrNull
                ?: asset["hlsUrl"]?.jsonPrimitive?.contentOrNull
                ?: asset["url"]?.jsonPrimitive?.contentOrNull

            if (!video.isNullOrBlank()) return video
        }

        AppleCanvasLogger.d("editorialVideo present but no video link found (keys: ${ev.keys})")
        return null
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        return "$prefix|" + parts.joinToString("|") { it.trim().lowercase(Locale.ROOT) }
    }
}
