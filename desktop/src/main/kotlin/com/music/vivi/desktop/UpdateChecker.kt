package com.music.vivi.desktop

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Minimal subset of the GitHub Releases API response. */
@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val prerelease: Boolean = false,
    @SerialName("html_url") val htmlUrl: String = "",
)

/** Result of an update check, rendered by the Settings → Updates section. */
sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object UpToDate : UpdateStatus
    data class Available(val version: String, val url: String) : UpdateStatus
    data class Failed(val message: String) : UpdateStatus
}

/**
 * Compares the locally installed DE version against the newest desktop release
 * on GitHub. Desktop releases are identified by a tag containing `_DE-`
 * (e.g. `6.1.0_DE-1.2.1` or `6.1.0_DE-1.2.1-nightly`); the mobile releases
 * (tags like `6.1.0`) are ignored.
 */
object UpdateChecker {
    private const val REPO = "PiBOH/vivi-music"
    private const val API_URL = "https://api.github.com/repos/$REPO/releases"

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** Extracts the DE version (`1.2.1`) from a desktop release tag. */
    fun deVersionFromTag(tag: String): String? {
        val idx = tag.indexOf("_DE-")
        if (idx < 0) return null
        val rest = tag.substring(idx + "_DE-".length)
        return rest.substringBefore('-').takeIf { it.isNotBlank() }
    }

    /** Compares two `MAJOR.MINOR.PATCH` versions; returns > 0 when [a] is newer. */
    fun compareVersions(a: String, b: String): Int {
        val pa = parts(a)
        val pb = parts(b)
        for (i in 0..2) {
            val d = pa[i] - pb[i]
            if (d != 0) return d
        }
        return 0
    }

    private fun parts(v: String): IntArray {
        val p = v.split('.')
        return intArrayOf(
            p.getOrNull(0)?.toIntOrNull() ?: 0,
            p.getOrNull(1)?.toIntOrNull() ?: 0,
            p.getOrNull(2)?.toIntOrNull() ?: 0,
        )
    }

    /**
     * Queries GitHub and decides whether a newer desktop release exists.
     * Pre-releases (nightly/beta/rc/alpha) are considered only when
     * [includePreReleases] is true.
     */
    fun check(includePreReleases: Boolean): UpdateStatus = try {
        val request = Request.Builder()
            .url("$API_URL?per_page=20")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "VIVIMusic-Desktop-Updater")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return UpdateStatus.Failed("HTTP ${response.code}")
            val body = response.body.string()
            val releases = json.decodeFromString<List<GitHubRelease>>(body)

            val current = AppInfo.DE_VERSION
            val candidate = releases.asSequence()
                .filter { !it.prerelease || includePreReleases }
                .mapNotNull { r -> deVersionFromTag(r.tagName)?.let { it to r } }
                .firstOrNull()
                ?: return UpdateStatus.Failed("No desktop release found")

            val (version, release) = candidate
            if (compareVersions(version, current) > 0) {
                UpdateStatus.Available(version, release.htmlUrl)
            } else {
                UpdateStatus.UpToDate
            }
        }
    } catch (e: Exception) {
        UpdateStatus.Failed(e.message ?: e::class.simpleName ?: "error")
    }
}
