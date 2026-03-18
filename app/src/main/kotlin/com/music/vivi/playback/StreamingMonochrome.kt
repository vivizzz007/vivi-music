/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Monochrome streaming integration.
 *
 * Fetches up to 320 kbps (HIGH quality) audio stream URLs from the Monochrome
 * community API, which is backed by the Tidal catalog.
 *
 * Usage:
 *   val url = StreamingMonochrome.getStreamForSong(title, artist)
 *   // Returns a direct CDN URL on success, or null if the song is not found
 *   // or if every API instance fails. The caller should fall back to YouTube Music.
 */
object StreamingMonochrome {

    private const val TAG = "StreamingMonochrome"

    // Quality to request from Monochrome – HIGH yields 320 kbps AAC.
    // Valid values: LOW | HIGH | LOSSLESS | HI_RES
    private const val STREAM_QUALITY = "HIGH"

    // Uptime-tracker Workers that return the live instance list.
    // We try both (in shuffled order) and fall back to the hardcoded list.
    private val UPTIME_URLS = listOf(
        "https://tidal-uptime.jiffy-puffs-1j.workers.dev/",
        "https://tidal-uptime.props-76styles.workers.dev/"
    )

    // Hardcoded fallback instance list (mirrors monochrome-main/functions/track/[id].js)
    private val FALLBACK_INSTANCES = listOf(
        "https://eu-central.monochrome.tf",
        "https://us-west.monochrome.tf",
        "https://arran.monochrome.tf",
        "https://triton.squid.wtf",
        "https://api.monochrome.tf",
        "https://monochrome-api.samidy.com",
        "https://maus.qqdl.site",
        "https://vogel.qqdl.site",
        "https://katze.qqdl.site",
        "https://hund.qqdl.site",
        "https://tidal.kinoplus.online",
        "https://wolf.qqdl.site"
    )

    /** Cached live instance lists. */
    private data class InstanceCache(
        val api: List<String>,
        val streaming: List<String>
    )

    @Volatile
    private var instanceCache: InstanceCache? = null

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Attempts to resolve a 320 kbps stream URL for [title] by [artist].
     *
     * All network calls run on [Dispatchers.IO].  Any exception is caught
     * internally; the function returns `null` rather than throwing, so the
     * caller can safely fall back to YouTube Music.
     *
     * @param title  Song title (as shown in YouTube Music metadata).
     * @param artist Primary artist name.
     * @return Direct CDN audio URL, or `null` if unresolvable.
     */
    suspend fun getStreamForSong(title: String, artist: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val (apiInstances, streamingInstances) = getInstances()
                if (apiInstances.isEmpty() && streamingInstances.isEmpty()) {
                    Timber.tag(TAG).w("No Monochrome instances available")
                    return@withContext null
                }

                // Search uses API instances
                val searchList = apiInstances.takeIf { it.isNotEmpty() } ?: streamingInstances
                val trackId = searchTrackId(searchList, title, artist)
                    ?: run {
                        Timber.tag(TAG).d("No Tidal match found for: $title – $artist")
                        return@withContext null
                    }

                // Resolution uses Streaming instances
                val resolutionList = streamingInstances.takeIf { it.isNotEmpty() } ?: apiInstances
                val streamUrl = fetchStreamUrl(resolutionList, trackId)
                if (streamUrl != null) {
                    Timber.tag(TAG).i("Monochrome stream resolved [$STREAM_QUALITY]: $title – $artist")
                }
                streamUrl
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Unexpected error in getStreamForSong")
                null
            }
        }

    // -------------------------------------------------------------------------
    // Instance discovery
    // -------------------------------------------------------------------------

    /**
     * Returns the live Monochrome API and Streaming instance lists.
     * Falls back to [FALLBACK_INSTANCES] for both if discovery fails.
     */
    private fun getInstances(): Pair<List<String>, List<String>> {
        instanceCache?.let { return it.api to it.streaming }

        val shuffledUptimeUrls = UPTIME_URLS.shuffled()
        for (uptimeUrl in shuffledUptimeUrls) {
            try {
                val request = Request.Builder().url(uptimeUrl).get().build()
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) continue

                val body = response.body?.string() ?: continue
                val json = JSONObject(body)
                
                val apiList = parseInstanceArray(json.optJSONArray("api"))
                val streamingList = parseInstanceArray(json.optJSONArray("streaming"))

                if (apiList.isNotEmpty() || streamingList.isNotEmpty()) {
                    // Fallback streaming to api if empty (as seen in storage.js)
                    val finalStreaming = if (streamingList.isEmpty()) apiList else streamingList
                    instanceCache = InstanceCache(apiList, finalStreaming)
                    Timber.tag(TAG).d("Fetched ${apiList.size} API and ${finalStreaming.size} Streaming instances from $uptimeUrl")
                    return apiList to finalStreaming
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w("Failed to fetch instances from $uptimeUrl: ${e.message}")
            }
        }

        Timber.tag(TAG).w("Discovery failed. Using fallback instances.")
        val fallback = FALLBACK_INSTANCES.map { it.trimEnd('/') }
        instanceCache = InstanceCache(fallback, fallback)
        return fallback to fallback
    }

    private fun parseInstanceArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val item = array.opt(i)
            val url = when (item) {
                is JSONObject -> item.optString("url")
                is String    -> item
                else         -> continue
            }
            if (url.isNotBlank() && !url.contains("spotisaver.net")) {
                list += url.trimEnd('/')
            }
        }
        return list
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    /**
     * Searches all instances in order and returns the first Tidal track ID
     * that matches [title] and [artist], or `null` if nothing was found.
     */
    private fun searchTrackId(instances: List<String>, title: String, artist: String): String? {
        val query = "$title $artist".trim()
        Timber.tag(TAG).d("Searching for track ID with query: '$query'")

        for (baseUrl in instances) {
            try {
                // Monochrome uses ?s= for track searches, and often prefers trailing slash /search/
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                    .replace("+", "%20") // Some instances prefer %20 for spaces
                
                val url = "$baseUrl/search/?s=$encodedQuery&type=tracks&limit=5"
                Timber.tag(TAG).v("Search URL: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Monochrome/2.0.0 (https://github.com/monochrome-music/monochrome)")
                    .get()
                    .build()
                
                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    Timber.tag(TAG).d("Search failed on $baseUrl (code ${response.code})")
                    continue
                }
                val body = response.body?.string() ?: continue
                Timber.tag(TAG).v("Search response body length: ${body.length}")

                val trackId = parseTrackIdFromSearch(body, artist)
                if (trackId != null) {
                    Timber.tag(TAG).d("Found track ID $trackId on $baseUrl")
                    return trackId
                }
            } catch (e: Exception) {
                Timber.tag(TAG).d("Exception during search on $baseUrl: ${e.message}")
            }
        }
        return null
    }

    /**
     * Parses the Tidal /search response and returns the ID of the best-matching
     * track. Robustly searches for 'items' array at various nesting levels.
     */
    private fun parseTrackIdFromSearch(body: String, artist: String): String? {
        return try {
            val root = JSONObject(body)
            
            // Look for items array recursively (simplified version of findSearchSection in api.js)
            val items = findItemsArray(root) ?: run {
                Timber.tag(TAG).d("No items array found in search response")
                return null
            }

            Timber.tag(TAG).d("Parsing ${items.length()} search results...")
            for (i in 0 until items.length()) {
                val track = items.optJSONObject(i) ?: continue
                // ID can be 'id' or 'trackId' depending on API version
                val id = (track.optString("id").takeIf { it.isNotBlank() }
                    ?: track.optString("trackId").takeIf { it.isNotBlank() }) ?: continue

                // Check artists
                val artistsArray = track.optJSONArray("artists")
                if (artistsArray != null && artistsArray.length() > 0) {
                    for (j in 0 until artistsArray.length()) {
                        val artistObj = artistsArray.optJSONObject(j) ?: continue
                        val artistName = artistObj.optString("name", "")
                        if (matchesArtist(artistName, artist)) {
                            Timber.tag(TAG).d("Match found: '$artistName' matches '$artist' (ID: $id)")
                            return id
                        }
                    }
                } else {
                    // Fallback to top-level artist object or name
                    val topArtist = track.optJSONObject("artist")?.optString("name")
                        ?: track.optString("artist")
                    if (topArtist.isNotBlank() && matchesArtist(topArtist, artist)) {
                        Timber.tag(TAG).d("Match found: '$topArtist' matches '$artist' (ID: $id)")
                        return id
                    }
                }
            }
            Timber.tag(TAG).d("No artist match found in results for artist '$artist'")
            null
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to parse search response")
            null
        }
    }

    private fun findItemsArray(json: JSONObject): JSONArray? {
        // Direct
        json.optJSONArray("items")?.let { return it }
        // Nested under 'tracks' (standard)
        json.optJSONObject("tracks")?.optJSONArray("items")?.let { return it }
        // Nested under 'data' (some v2 instances)
        json.optJSONObject("data")?.let { data ->
            data.optJSONArray("items")?.let { return it }
            data.optJSONArray("tracks")?.let { return it }
        }
        // Maybe it's an array directly? (handled by caller if body is JSONArray, but we assume root is JSONObject here)
        return null
    }

    private fun matchesArtist(candidate: String, target: String): Boolean {
        if (candidate.isBlank() || target.isBlank()) return false
        val cNorm = candidate.lowercase().trim()
        val tNorm = target.lowercase().trim()
        return cNorm.contains(tNorm) || tNorm.contains(cNorm)
    }

    // -------------------------------------------------------------------------
    // Stream URL resolution
    // -------------------------------------------------------------------------

    /**
     * Tries every instance until one returns a non-empty stream URL.
     */
    private fun fetchStreamUrl(instances: List<String>, trackId: String): String? {
        Timber.tag(TAG).d("Fetching stream URL for trackId: $trackId, quality: $STREAM_QUALITY")
        val userAgent = "Monochrome/2.0.0 (https://github.com/monochrome-music/monochrome)"
        
        for (baseUrl in instances) {
            try {
                // Try /stream endpoint first (simplest direct URL)
                val url = "$baseUrl/stream?id=$trackId&quality=$STREAM_QUALITY"
                Timber.tag(TAG).v("Stream fetch URL: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .get()
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val code = response.code
                val body = response.body?.string() ?: ""
                
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val streamUrl = json.optString("url").takeIf { it.isNotBlank() }
                        ?: json.optString("streamUrl").takeIf { it.isNotBlank() }

                    if (!streamUrl.isNullOrBlank()) {
                        Timber.tag(TAG).d("Stream URL obtained from /stream on $baseUrl")
                        return streamUrl
                    } else {
                        Timber.tag(TAG).d("No direct URL in /stream response from $baseUrl: $body")
                    }
                } else {
                    Timber.tag(TAG).d("/stream failed on $baseUrl (code $code): $body")
                }

                // Fallback to /track/?id=... (returns full info including manifest)
                // Note: api.js uses /track/ with a trailing slash and ?id=
                val trackUrl = "$baseUrl/track/?id=$trackId&quality=$STREAM_QUALITY"
                Timber.tag(TAG).v("Track info fetch URL: $trackUrl")
                
                val trackRequest = Request.Builder()
                    .url(trackUrl)
                    .header("User-Agent", userAgent)
                    .get()
                    .build()
                
                val trackResponse = httpClient.newCall(trackRequest).execute()
                val tCode = trackResponse.code
                val tBody = trackResponse.body?.string() ?: ""
                
                if (trackResponse.isSuccessful) {
                    val json = JSONObject(tBody)
                    val data = json.optJSONObject("data") ?: json
                    
                    // Check for direct URLs first
                    val directUrl = data.optString("url").takeIf { it.isNotBlank() }
                        ?: data.optString("streamUrl").takeIf { it.isNotBlank() }
                    
                    if (!directUrl.isNullOrBlank()) {
                        Timber.tag(TAG).d("Stream URL obtained from /track on $baseUrl")
                        return directUrl
                    }

                    // Check for manifest
                    val manifest = data.optString("manifest")
                    if (manifest.isNotBlank()) {
                         Timber.tag(TAG).v("Decoding manifest on $baseUrl...")
                         val resolved = extractStreamUrlFromManifest(manifest)
                         if (!resolved.isNullOrBlank()) {
                             Timber.tag(TAG).d("Stream URL extracted from manifest on $baseUrl")
                             return resolved
                         }
                    } else {
                         Timber.tag(TAG).d("No direct URL or manifest in /track response from $baseUrl: $tBody")
                    }
                } else {
                    Timber.tag(TAG).d("/track failed on $baseUrl (code $tCode): $tBody")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).d("Exception during stream fetch on $baseUrl: ${e.message}")
            }
        }
        return null
    }

    private fun extractStreamUrlFromManifest(manifest: String): String? {
        return try {
            val decoded = try {
                val bytes = android.util.Base64.decode(manifest, android.util.Base64.DEFAULT)
                String(bytes)
            } catch (e: Exception) {
                manifest // Might not be base64
            }

            // DASH manifests (XML) require a DASH media source in ExoPlayer.
            // For now, we try to find direct progressive URLs within the manifest if possible.
            if (decoded.contains("<MPD")) {
                Timber.tag(TAG).w("DASH manifest detected. Direct extraction may fail.")
            }

            // Extract all likely stream URLs using regex
            // Targets https://... URLs that don't contain common JSON/XML delimiters
            val urlRegex = """https?://[^\s"<>|]+""".toRegex()
            val matches = urlRegex.findAll(decoded).map { it.value.trimEnd(',', ';', '"', '\'') }.toList()
            
            if (matches.isEmpty()) return null

            // Quality sorting logic (mirrors strategy in api.js)
            // We prioritize Hi-Res/Lossless/High keywords to ensure 320kbps+ is chosen if available.
            val bestUrl = matches.sortedByDescending { url ->
                val u = url.lowercase()
                when {
                    u.contains("hi-res") || u.contains("hires") || u.contains("96k") && u.contains("flac") -> 100
                    u.contains("lossless") || u.contains("flac") -> 90
                    u.contains("high") || u.contains("320") -> 80
                    u.contains("medium") || u.contains("128") -> 50
                    u.contains("low") || u.contains("96") || u.contains("64") -> 20
                    else -> 10
                }
            }.firstOrNull()

            if (bestUrl != null) {
                Timber.tag(TAG).v("Extracted best URL from manifest: $bestUrl")
            }
            bestUrl
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Manifest extraction failed")
            null
        }
    }
}
