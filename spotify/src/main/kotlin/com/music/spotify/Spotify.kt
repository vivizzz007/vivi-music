package com.music.spotify

import com.music.spotify.models.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*

object Spotify {
    @Volatile
    var accessToken: String? = null

    @Volatile
    private var rateLimitedUntilMs: Long = 0L

    private suspend fun checkRateLimitCooldown() {
        val remaining = rateLimitedUntilMs - System.currentTimeMillis()
        if (remaining > 0) {
            delay(remaining)
        }
    }

    private fun handleRateLimit(retryAfterSeconds: Long?) {
        val seconds = (retryAfterSeconds ?: 3L).coerceIn(1L, 60L)
        rateLimitedUntilMs = maxOf(rateLimitedUntilMs, System.currentTimeMillis() + (seconds * 1000L))
    }

    private const val GQL_URL = "https://api-partner.spotify.com/pathfinder/v2/query"

    private fun randomUserAgent(): String {
        val osOptions = arrayOf(
            "Windows NT 10.0; Win64; x64",
            "Macintosh; Intel Mac OS X 10_15_7",
            "X11; Linux x86_64",
        )
        val chromeBase = 140
        val chromeMajor = chromeBase - (0..4).random()
        val chromePatch = (0..499).random()
        val os = osOptions.random()
        return "Mozilla/5.0 ($os) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$chromeMajor.0.$chromePatch.0 Safari/537.36"
    }

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val gqlClient by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                }
            }
            defaultRequest {
                header("User-Agent", randomUserAgent())
                header("app-platform", "WebPlayer")
                header("Origin", "https://open.spotify.com")
                header("Referer", "https://open.spotify.com/")
                header("Accept", "application/json")
            }
            expectSuccess = false
        }
    }

    class SpotifyException(
        val statusCode: Int,
        override val message: String,
        val retryAfterSec: Long = 0,
    ) : Exception(message)

    // JSON parsing helpers
    private fun JsonObject.obj(key: String): JsonObject? =
        try { this[key]?.takeIf { it !is JsonNull }?.jsonObject } catch (_: Exception) { null }

    private fun JsonObject.str(key: String): String? =
        try { this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.contentOrNull } catch (_: Exception) { null }

    private fun JsonObject.int(key: String): Int? =
        try { this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.intOrNull } catch (_: Exception) { null }

    private fun JsonObject.arr(key: String): JsonArray? =
        try { this[key]?.takeIf { it !is JsonNull }?.jsonArray } catch (_: Exception) { null }

    private suspend fun graphqlPost(
        operationName: String,
        variables: JsonObject = buildJsonObject {},
    ): JsonObject {
        val token = accessToken ?: throw SpotifyException(401, "Not authenticated")
        val sha256Hash = SpotifyHashProvider.getHash(operationName)
        val body = buildGqlBody(operationName, sha256Hash, variables)
        return executeGqlWithRetries(operationName, token, body)
    }

    private fun buildGqlBody(
        operationName: String,
        sha256Hash: String,
        variables: JsonObject,
    ): JsonObject =
        buildJsonObject {
            put("variables", variables)
            put("operationName", operationName)
            putJsonObject("extensions") {
                putJsonObject("persistedQuery") {
                    put("version", 1)
                    put("sha256Hash", sha256Hash)
                }
            }
        }

    private suspend fun executeGqlWithRetries(
        operationName: String,
        token: String,
        body: JsonObject,
    ): JsonObject {
        val maxRetries = 4
        for (attempt in 0 until maxRetries) {
            checkRateLimitCooldown()
            val response = gqlClient.post(GQL_URL) {
                header("Authorization", "Bearer $token")
                setBody(
                    TextContent(
                        body.toString(),
                        ContentType.Application.Json.withParameter("charset", "UTF-8"),
                    ),
                )
            }

            if (response.status == HttpStatusCode.Unauthorized) {
                throw SpotifyException(401, "Token expired or invalid")
            }
            if (response.status == HttpStatusCode.TooManyRequests) {
                val retryAfter = response.headers["Retry-After"]?.toLongOrNull() ?: (2L * (attempt + 1))
                handleRateLimit(retryAfter)
                if (attempt < maxRetries - 1) {
                    delay(retryAfter * 1000L)
                    continue
                }
                throw SpotifyException(429, "Rate limited", retryAfterSec = retryAfter)
            }
            if (response.status.value !in 200..299) {
                val bodyText = response.bodyAsText()
                throw SpotifyException(response.status.value, "GraphQL error ${response.status.value}: $bodyText")
            }

            val responseJson = json.parseToJsonElement(response.bodyAsText()).jsonObject

            val errors = responseJson.arr("errors")
            if (errors != null && errors.isNotEmpty()) {
                val errorMsg = errors[0].jsonObject.str("message") ?: "Unknown GraphQL error"
                throw SpotifyException(400, "GraphQL: $errorMsg")
            }

            return responseJson
        }

        throw SpotifyException(429, "Rate limited after $maxRetries retries")
    }

    private fun parseGqlImage(source: JsonObject): SpotifyImage? {
        val url = source.str("url") ?: return null
        return SpotifyImage(url = url, height = source.int("height"), width = source.int("width"))
    }

    private fun parseGqlImages(sources: JsonArray?): List<SpotifyImage> =
        sources?.mapNotNull { parseGqlImage(it.jsonObject) } ?: emptyList()

    private fun parseGqlSimpleArtist(artistObj: JsonObject): SpotifySimpleArtist? {
        val uri = artistObj.str("uri") ?: return null
        return SpotifySimpleArtist(
            id = uri.substringAfterLast(":"),
            name = artistObj.obj("profile")?.str("name") ?: "",
            uri = uri,
        )
    }

    private fun parseGqlTrack(
        trackData: JsonObject,
        albumOverride: SpotifySimpleAlbum? = null,
        uriOverride: String? = null,
    ): SpotifyTrack {
        val uri = uriOverride ?: trackData.str("uri") ?: trackData.str("_uri") ?: ""
        val trackId = uri.substringAfterLast(":")

        val artists = trackData.obj("artists")?.arr("items")?.mapNotNull { elem ->
            parseGqlSimpleArtist(elem.jsonObject)
        } ?: emptyList()

        val album = albumOverride ?: run {
            val albumData = trackData.obj("albumOfTrack")
            val albumUri = albumData?.str("uri") ?: ""
            val albumId = albumUri.substringAfterLast(":")
            SpotifySimpleAlbum(
                id = albumId,
                name = albumData?.str("name") ?: "",
                images = parseGqlImages(albumData?.obj("coverArt")?.arr("sources")),
                uri = albumUri.ifEmpty { null },
            )
        }

        return SpotifyTrack(
            id = trackId,
            name = trackData.str("name") ?: "",
            artists = artists,
            album = album,
            durationMs = parseGqlTrackDurationMs(trackData),
            uri = uri.ifEmpty { null },
        )
    }

    private fun parseGqlTrackDurationMs(trackData: JsonObject): Int {
        trackData.obj("duration")?.int("totalMilliseconds")?.let { if (it > 0) return it }
        trackData.int("durationMs")?.let { if (it > 0) return it }
        trackData.int("duration_ms")?.let { if (it > 0) return it }
        trackData.int("duration")?.let { sec -> if (sec > 0) return sec * 1000 }
        return 0
    }

    private fun parseGqlPlaylistImages(imagesObj: JsonObject?): List<SpotifyImage> =
        imagesObj?.arr("items")?.flatMap { imageGroup ->
            parseGqlImages(imageGroup.jsonObject.arr("sources"))
        } ?: emptyList()

    suspend fun me(): Result<SpotifyUser> = runCatching {
        val response = graphqlPost(operationName = "profileAttributes")
        val profile = response.obj("data")?.obj("me")?.obj("profile")
            ?: throw SpotifyException(500, "Invalid profileAttributes response")

        val uri = profile.str("uri") ?: ""
        SpotifyUser(
            id = uri.substringAfterLast(":"),
            displayName = profile.str("name"),
            images = parseGqlImages(profile.obj("avatar")?.arr("sources")),
        )
    }

    suspend fun myPlaylists(
        limit: Int = 50,
        offset: Int = 0,
    ): Result<SpotifyPaging<SpotifyPlaylist>> = runCatching {
        val token = accessToken ?: throw SpotifyException(401, "Not authenticated")

        // 1. Try official Spotify Web API first (guarantees exact track count and metadata)
        val webApiResult = runCatching {
            checkRateLimitCooldown()
            val response = gqlClient.get("https://api.spotify.com/v1/me/playlists") {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
                parameter("offset", offset)
            }
            if (response.status.value in 200..299) {
                val respJson = json.parseToJsonElement(response.bodyAsText()).jsonObject
                val totalCount = respJson.int("total") ?: 0
                val items = respJson.arr("items")?.mapNotNull { itemElem ->
                    val itemObj = itemElem.jsonObject
                    val id = itemObj.str("id") ?: return@mapNotNull null
                    val name = itemObj.str("name") ?: ""
                    val description = itemObj.str("description")
                    val images = parseGqlImages(itemObj.arr("images"))
                    val ownerObj = itemObj.obj("owner")
                    val tracksObj = itemObj.obj("tracks")
                    val totalTracks = tracksObj?.int("total")
                    SpotifyPlaylist(
                        id = id,
                        name = name,
                        description = description,
                        images = images,
                        owner = SpotifyPlaylistOwner(
                            id = ownerObj?.str("id") ?: "",
                            displayName = ownerObj?.str("display_name"),
                            uri = ownerObj?.str("uri"),
                        ),
                        tracks = SpotifyPlaylistTracksRef(total = totalTracks),
                        uri = itemObj.str("uri") ?: "spotify:playlist:$id",
                    )
                } ?: emptyList()
                SpotifyPaging(
                    items = items,
                    total = totalCount,
                    limit = limit,
                    offset = offset,
                )
            } else null
        }.getOrNull()

        if (webApiResult != null) {
            return@runCatching webApiResult
        }

        // 2. Fallback to GraphQL libraryV3
        val vars = buildJsonObject {
            putJsonArray("filters") { add("Playlists") }
            put("order", null as String?)
            put("textFilter", "")
            putJsonArray("features") {
                add("LIKED_SONGS")
                add("YOUR_EPISODES_V2")
                add("PRERELEASES")
                add("EVENTS")
            }
            put("limit", limit)
            put("offset", offset)
            put("flatten", true)
            putJsonArray("expandedFolders") {}
            put("folderUri", null as String?)
            put("includeFoldersWhenFlattening", false)
        }

        val response = graphqlPost(operationName = "libraryV3", variables = vars)
        val libraryData = response.obj("data")?.obj("me")?.obj("libraryV3")
            ?: throw SpotifyException(500, "Invalid libraryV3 response")

        val totalCount = libraryData.int("totalCount") ?: 0
        val pagingInfo = libraryData.obj("pagingInfo")

        val playlists = libraryData.arr("items")?.mapNotNull { itemElem ->
            val wrapper = itemElem.jsonObject.obj("item") ?: return@mapNotNull null
            if (wrapper.str("__typename") != "PlaylistResponseWrapper") return@mapNotNull null
            parsePlaylistWrapper(wrapper, itemElem.jsonObject)
        } ?: emptyList()

        SpotifyPaging(
            items = playlists,
            total = totalCount,
            limit = pagingInfo?.int("limit") ?: limit,
            offset = pagingInfo?.int("offset") ?: offset,
        )
    }

    private fun parsePlaylistWrapper(wrapper: JsonObject, itemElem: JsonObject? = null): SpotifyPlaylist? {
        val data = wrapper.obj("data") ?: return null
        if (data.str("__typename") != "Playlist") return null
        val playlistUri = wrapper.str("_uri") ?: return null
        val playlistId = playlistUri.substringAfterLast(":")
        val ownerData = data.obj("ownerV2")?.obj("data")
        val ownerId = ownerData?.str("uri")?.substringAfterLast(":") ?: ownerData?.str("id") ?: ""
        return SpotifyPlaylist(
            id = playlistId,
            name = data.str("name") ?: "",
            description = data.str("description"),
            images = parseGqlPlaylistImages(data.obj("images")),
            owner = SpotifyPlaylistOwner(
                id = ownerId,
                displayName = ownerData?.str("name"),
                uri = ownerData?.str("uri"),
            ),
            tracks = SpotifyPlaylistTracksRef(total = parsePlaylistTrackCount(data, wrapper, itemElem)),
            uri = playlistUri,
        )
    }

    private fun parsePlaylistTrackCount(data: JsonObject, wrapper: JsonObject? = null, itemElem: JsonObject? = null): Int? {
        val sources = listOfNotNull(data, wrapper, itemElem)
        for (src in sources) {
            src.obj("content")?.int("totalCount")?.let { return it }
            src.obj("contents")?.int("totalCount")?.let { return it }
            src.obj("tracks")?.int("totalCount")?.let { return it }
            src.obj("tracksV2")?.int("totalCount")?.let { return it }
            src.obj("attributes")?.int("totalCount")?.let { return it }
            src.int("totalCount")?.let { return it }
            src.int("trackCount")?.let { return it }
            src.int("numTracks")?.let { return it }
            src.int("total")?.let { return it }
            src.int("count")?.let { return it }
        }
        return null
    }

    suspend fun playlist(playlistId: String): Result<SpotifyPlaylist> = runCatching {
        val token = accessToken ?: throw SpotifyException(401, "Not authenticated")

        // 1. Try official Spotify Web API first
        val webApiResult = runCatching {
            checkRateLimitCooldown()
            val response = gqlClient.get("https://api.spotify.com/v1/playlists/$playlistId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) {
                val respJson = json.parseToJsonElement(response.bodyAsText()).jsonObject
                val id = respJson.str("id") ?: playlistId
                val name = respJson.str("name") ?: ""
                val description = respJson.str("description")
                val images = parseGqlImages(respJson.arr("images"))
                val ownerObj = respJson.obj("owner")
                val tracksObj = respJson.obj("tracks")
                val totalTracks = tracksObj?.int("total")
                SpotifyPlaylist(
                    id = id,
                    name = name,
                    description = description,
                    images = images,
                    owner = SpotifyPlaylistOwner(
                        id = ownerObj?.str("id") ?: "",
                        displayName = ownerObj?.str("display_name"),
                        uri = ownerObj?.str("uri"),
                    ),
                    tracks = SpotifyPlaylistTracksRef(total = totalTracks),
                    uri = respJson.str("uri") ?: "spotify:playlist:$id",
                )
            } else null
        }.getOrNull()

        if (webApiResult != null) {
            return@runCatching webApiResult
        }

        // 2. Fallback to GraphQL fetchPlaylist metadata
        val vars = buildJsonObject {
            put("uri", "spotify:playlist:$playlistId")
            put("offset", 0)
            put("limit", 1)
            put("enableWatchFeedEntrypoint", false)
        }
        val response = graphqlPost(operationName = "fetchPlaylist", variables = vars)
        val playlistV2 = response.obj("data")?.obj("playlistV2")
            ?: throw SpotifyException(500, "No playlistV2 in fetchPlaylist response")
        val name = playlistV2.str("name") ?: ""
        val images = parseGqlImages(playlistV2.arr("images") ?: playlistV2.obj("images")?.arr("items"))
        val totalCount = playlistV2.obj("content")?.int("totalCount")
        val ownerObj = playlistV2.obj("ownerV2")?.obj("data")
        SpotifyPlaylist(
            id = playlistId,
            name = name,
            description = null,
            images = images,
            owner = SpotifyPlaylistOwner(
                id = ownerObj?.str("id") ?: "",
                displayName = ownerObj?.str("name"),
                uri = ownerObj?.str("uri"),
            ),
            tracks = SpotifyPlaylistTracksRef(total = totalCount),
            uri = "spotify:playlist:$playlistId",
        )
    }

    private fun parseWebApiTrack(trackObj: JsonObject): SpotifyTrack {
        val id = trackObj.str("id") ?: ""
        val name = trackObj.str("name") ?: ""
        val uri = trackObj.str("uri") ?: if (id.isNotEmpty()) "spotify:track:$id" else null
        val durationMs = trackObj.int("duration_ms") ?: 0

        val artists = trackObj.arr("artists")?.mapNotNull { elem ->
            val artObj = elem.jsonObject
            SpotifySimpleArtist(
                id = artObj.str("id"),
                name = artObj.str("name") ?: "",
                uri = artObj.str("uri"),
            )
        } ?: emptyList()

        val albumObj = trackObj.obj("album")
        val album = if (albumObj != null) {
            val albumId = albumObj.str("id") ?: ""
            SpotifySimpleAlbum(
                id = albumId,
                name = albumObj.str("name") ?: "",
                images = parseGqlImages(albumObj.arr("images")),
                uri = albumObj.str("uri"),
            )
        } else null

        return SpotifyTrack(
            id = id,
            name = name,
            artists = artists,
            album = album,
            durationMs = durationMs,
            uri = uri,
        )
    }

    suspend fun playlistTracks(
        playlistId: String,
        limit: Int = 100,
        offset: Int = 0,
    ): Result<SpotifyPaging<SpotifyPlaylistTrack>> = runCatching {
        val token = accessToken ?: throw SpotifyException(401, "Not authenticated")

        // 1. Try official Spotify Web API first (fast, reliable, no GraphQL hash dependency)
        val webApiResult = runCatching {
            checkRateLimitCooldown()
            val response = gqlClient.get("https://api.spotify.com/v1/playlists/$playlistId/tracks") {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
                parameter("offset", offset)
            }
            if (response.status.value in 200..299) {
                val respJson = json.parseToJsonElement(response.bodyAsText()).jsonObject
                val totalCount = respJson.int("total") ?: 0
                val tracks = respJson.arr("items")?.mapNotNull { elem ->
                    val itemObj = elem.jsonObject
                    val trackObj = itemObj.obj("track") ?: return@mapNotNull null
                    val trackId = trackObj.str("id")
                    if (trackId.isNullOrEmpty() && trackObj.str("name").isNullOrEmpty()) return@mapNotNull null
                    SpotifyPlaylistTrack(
                        track = parseWebApiTrack(trackObj),
                        uid = itemObj.str("added_at"),
                    )
                } ?: emptyList()
                SpotifyPaging(
                    items = tracks,
                    total = totalCount,
                    limit = limit,
                    offset = offset,
                )
            } else null
        }.getOrNull()

        if (webApiResult != null) {
            return@runCatching webApiResult
        }

        // 2. Fallback to GraphQL fetchPlaylist
        val vars = buildJsonObject {
            put("uri", "spotify:playlist:$playlistId")
            put("offset", offset)
            put("limit", limit)
            put("enableWatchFeedEntrypoint", false)
        }

        val response = graphqlPost(operationName = "fetchPlaylist", variables = vars)
        val content = response.obj("data")?.obj("playlistV2")?.obj("content")
            ?: throw SpotifyException(500, "No content in fetchPlaylist response")

        val tracks = content.arr("items")?.mapNotNull { elem ->
            val itemWrapper = elem.jsonObject.obj("itemV2") ?: return@mapNotNull null
            val itemData = itemWrapper.obj("data") ?: return@mapNotNull null
            val wrapperUri = itemWrapper.str("_uri") ?: itemWrapper.str("uri")
            SpotifyPlaylistTrack(
                track = parseGqlTrack(itemData, uriOverride = wrapperUri),
                uid = elem.jsonObject.str("uid") ?: itemWrapper.str("uid"),
            )
        } ?: emptyList()

        SpotifyPaging(
            items = tracks,
            total = content.int("totalCount") ?: 0,
            limit = limit,
            offset = offset,
        )
    }

    suspend fun likedSongs(
        limit: Int = 50,
        offset: Int = 0,
    ): Result<SpotifyPaging<SpotifySavedTrack>> = runCatching {
        val token = accessToken ?: throw SpotifyException(401, "Not authenticated")

        // 1. Try official Spotify Web API first (me/tracks)
        val webApiResult = runCatching {
            checkRateLimitCooldown()
            val response = gqlClient.get("https://api.spotify.com/v1/me/tracks") {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
                parameter("offset", offset)
            }
            if (response.status.value in 200..299) {
                val respJson = json.parseToJsonElement(response.bodyAsText()).jsonObject
                val totalCount = respJson.int("total") ?: 0
                val savedTracks = respJson.arr("items")?.mapNotNull { elem ->
                    val itemObj = elem.jsonObject
                    val trackObj = itemObj.obj("track") ?: return@mapNotNull null
                    SpotifySavedTrack(track = parseWebApiTrack(trackObj))
                } ?: emptyList()
                SpotifyPaging(
                    items = savedTracks,
                    total = totalCount,
                    limit = limit,
                    offset = offset,
                )
            } else null
        }.getOrNull()

        if (webApiResult != null) {
            return@runCatching webApiResult
        }

        // 2. Fallback to GraphQL fetchLibraryTracks
        val vars = buildJsonObject {
            put("offset", offset)
            put("limit", limit)
        }

        val response = graphqlPost(operationName = "fetchLibraryTracks", variables = vars)
        val tracksData = response.obj("data")?.obj("me")?.obj("library")?.obj("tracks")
            ?: throw SpotifyException(500, "Invalid fetchLibraryTracks response")

        val savedTracks = tracksData.arr("items")?.mapNotNull { elem ->
            val trackWrapper = elem.jsonObject.obj("track") ?: return@mapNotNull null
            val trackData = trackWrapper.obj("data") ?: return@mapNotNull null
            val wrapperUri = trackWrapper.str("_uri") ?: trackWrapper.str("uri")
            SpotifySavedTrack(track = parseGqlTrack(trackData, uriOverride = wrapperUri))
        } ?: emptyList()

        SpotifyPaging(
            items = savedTracks,
            total = tracksData.int("totalCount") ?: 0,
            limit = limit,
            offset = offset,
        )
    }

    suspend fun createPlaylist(name: String, description: String? = null): Result<SpotifyPlaylist> = runCatching {
        val token = accessToken ?: throw SpotifyException(401, "Not authenticated")
        val user = me().getOrThrow()
        val userId = user.id.ifBlank { throw SpotifyException(400, "Unable to determine Spotify user ID") }
        val payload = buildJsonObject {
            put("name", name)
            if (!description.isNullOrBlank()) {
                put("description", description)
            }
            put("public", false)
        }
        val maxRetries = 3
        for (attempt in 0..maxRetries) {
            checkRateLimitCooldown()
            val response = gqlClient.post("https://api.spotify.com/v1/users/$userId/playlists") {
                header("Authorization", "Bearer $token")
                setBody(
                    TextContent(
                        payload.toString(),
                        ContentType.Application.Json.withParameter("charset", "UTF-8"),
                    )
                )
            }
            if (response.status.value == 429) {
                val retryAfter = response.headers["Retry-After"]?.toLongOrNull() ?: (2L * (attempt + 1))
                handleRateLimit(retryAfter)
                if (attempt < maxRetries) {
                    delay(retryAfter * 1000L)
                    continue
                }
            }
            if (response.status.value !in 200..299) {
                val errorBody = response.bodyAsText()
                val parsedMessage = runCatching {
                    val jsonElem = json.parseToJsonElement(errorBody).jsonObject
                    jsonElem.obj("error")?.str("message") ?: errorBody
                }.getOrDefault(errorBody)
                throw SpotifyException(response.status.value, parsedMessage)
            }
            val responseJson = json.parseToJsonElement(response.bodyAsText()).jsonObject
            val id = responseJson.str("id") ?: throw SpotifyException(500, "Missing playlist id")
            return@runCatching SpotifyPlaylist(
                id = id,
                name = responseJson.str("name") ?: name,
                description = responseJson.str("description"),
                images = emptyList(),
                owner = null,
                tracks = null,
                uri = responseJson.str("uri") ?: "spotify:playlist:$id"
            )
        }
        throw SpotifyException(429, "Spotify rate limit exceeded creating playlist")
    }

    suspend fun searchTrack(query: String): Result<String?> = runCatching {
        val token = accessToken ?: throw SpotifyException(401, "Not authenticated")
        val maxRetries = 3
        for (attempt in 0..maxRetries) {
            checkRateLimitCooldown()
            val response = gqlClient.get("https://api.spotify.com/v1/search") {
                header("Authorization", "Bearer $token")
                parameter("q", query)
                parameter("type", "track")
                parameter("limit", 1)
            }
            if (response.status.value == 429) {
                val retryAfterHeader = response.headers["Retry-After"]?.toLongOrNull() ?: (2L * (attempt + 1))
                handleRateLimit(retryAfterHeader)
                if (attempt < maxRetries) {
                    delay(retryAfterHeader * 1000L)
                    continue
                }
                return@runCatching null
            }
            if (response.status.value !in 200..299) {
                return@runCatching null
            }
            val responseJson = json.parseToJsonElement(response.bodyAsText()).jsonObject
            val items = responseJson.obj("tracks")?.arr("items")
            val firstTrack = items?.firstOrNull()?.jsonObject
            return@runCatching firstTrack?.str("uri") ?: firstTrack?.str("id")?.let { "spotify:track:$it" }
        }
        null
    }

    suspend fun addTracksToPlaylist(playlistId: String, trackUris: List<String>): Result<Unit> = runCatching {
        val token = accessToken ?: throw SpotifyException(401, "Not authenticated")
        trackUris.chunked(100).forEachIndexed { chunkIndex, chunk ->
            if (chunkIndex > 0) {
                delay(600)
            }
            val payload = buildJsonObject {
                putJsonArray("uris") {
                    chunk.forEach { add(it) }
                }
            }
            var succeeded = false
            val maxRetries = 5
            for (attempt in 0..maxRetries) {
                checkRateLimitCooldown()
                val response = gqlClient.post("https://api.spotify.com/v1/playlists/$playlistId/tracks") {
                    header("Authorization", "Bearer $token")
                    setBody(
                        TextContent(
                            payload.toString(),
                            ContentType.Application.Json.withParameter("charset", "UTF-8"),
                        )
                    )
                }
                if (response.status.value == 429) {
                    val retryAfter = response.headers["Retry-After"]?.toLongOrNull() ?: (3L * (attempt + 1))
                    handleRateLimit(retryAfter)
                    if (attempt < maxRetries) {
                        delay(retryAfter * 1000L)
                        continue
                    }
                }
                if (response.status.value !in 200..299) {
                    val errorBody = response.bodyAsText()
                    val parsedMessage = runCatching {
                        val jsonElem = json.parseToJsonElement(errorBody).jsonObject
                        jsonElem.obj("error")?.str("message") ?: errorBody
                    }.getOrDefault(errorBody)
                    throw SpotifyException(response.status.value, parsedMessage)
                }
                succeeded = true
                break
            }
            if (!succeeded) {
                throw SpotifyException(429, "Spotify rate limit exceeded. Please wait a moment and retry.")
            }
        }
    }
}
