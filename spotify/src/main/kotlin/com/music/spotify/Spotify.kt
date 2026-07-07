package com.music.spotify

import com.music.spotify.models.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
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
        val maxRetries = 3
        for (attempt in 0 until maxRetries) {
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
                if (attempt < maxRetries - 1) {
                    delay(retryAfter * 1000)
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
            parsePlaylistWrapper(wrapper)
        } ?: emptyList()

        SpotifyPaging(
            items = playlists,
            total = totalCount,
            limit = pagingInfo?.int("limit") ?: limit,
            offset = pagingInfo?.int("offset") ?: offset,
        )
    }

    private fun parsePlaylistWrapper(wrapper: JsonObject): SpotifyPlaylist? {
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
            tracks = SpotifyPlaylistTracksRef(total = parsePlaylistTrackCount(data)),
            uri = playlistUri,
        )
    }

    private fun parsePlaylistTrackCount(data: JsonObject): Int? =
        data.obj("content")?.int("totalCount")
            ?: data.obj("contents")?.int("totalCount")
            ?: data.obj("tracks")?.int("totalCount")
            ?: data.obj("tracksV2")?.int("totalCount")
            ?: data.int("totalCount")
            ?: data.int("trackCount")
            ?: data.int("numTracks")

    suspend fun playlistTracks(
        playlistId: String,
        limit: Int = 100,
        offset: Int = 0,
    ): Result<SpotifyPaging<SpotifyPlaylistTrack>> = runCatching {
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
}
