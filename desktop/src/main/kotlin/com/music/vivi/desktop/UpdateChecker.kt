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
    val body: String = "",
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
data class GitHubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    val size: Long = 0,
)

/** A downloadable installer asset selected for the host platform. */
data class UpdateAsset(
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
)

/** Result of an update check, rendered by the Settings → Updates section. */
sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object UpToDate : UpdateStatus
    data class Available(val version: String, val url: String, val asset: UpdateAsset? = null) : UpdateStatus
    data class Failed(val message: String) : UpdateStatus
}

/**
 * Selectable update source: the user's fork (default) or the original repo.
 * Resolves to the GitHub owner/name and default branch used by the update
 * checker and the live-changelog fetch.
 */
object UpdateSource {
    const val FORK = "fork"
    const val ORIGINAL = "original"

    fun current(): String = DesktopSettings.load().updateSource

    /** GitHub owner/name for a given source key (fork vs original). */
    fun repoFor(source: String): String =
        if (source == ORIGINAL) "vivizzz007/vivi-music" else "PiBOH/vivi-music"

    fun repo(): String = repoFor(current())

    fun branch(): String =
        if (current() == ORIGINAL) "main" else "vivi-music-de"
}

/**
 * Compares the locally installed DE version against the newest desktop release
 * on GitHub. Desktop releases are identified by a tag containing `_DE-`
 * (e.g. `6.1.0_DE-1.2.1` or `6.1.0_DE-1.2.1-nightly`); the mobile releases
 * (tags like `6.1.0`) are ignored.
 */
object UpdateChecker {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Picks the installer asset for the host OS/arch. Windows: EXE then MSI
     * (the Inno Setup .exe is lighter and more user-friendly); Linux: AppImage
     * then DEB; macOS: DMG then PKG. Prefers an arch-specific asset, then
     * falls back to any matching extension.
     */
    fun selectAsset(assets: List<GitHubAsset>): UpdateAsset? {
        if (assets.isEmpty()) return null
        val preferred = when (Platform.os) {
            DesktopOs.WINDOWS -> listOf(".exe", ".msi")
            DesktopOs.MACOS -> listOf(".dmg", ".pkg")
            DesktopOs.LINUX -> listOf(".AppImage", ".deb")
        }
        val archTokens = if (Platform.arch == DesktopArch.ARM64) {
            listOf("arm64", "aarch64")
        } else {
            listOf("x64", "amd64", "x86_64")
        }

        fun matchesArch(name: String): Boolean =
            archTokens.any { name.contains(it, ignoreCase = true) } ||
                (Platform.arch == DesktopArch.X64 && !name.contains("arm", ignoreCase = true))

        for (ext in preferred) {
            assets.firstOrNull { it.name.endsWith(ext, ignoreCase = true) && matchesArch(it.name) }
                ?.let { return UpdateAsset(it.name, it.browserDownloadUrl, it.size) }
        }
        for (ext in preferred) {
            assets.firstOrNull { it.name.endsWith(ext, ignoreCase = true) }
                ?.let { return UpdateAsset(it.name, it.browserDownloadUrl, it.size) }
        }
        return null
    }

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
     * Returns the desktop release with the highest DE version, instead of the
     * first / "latest" entry in GitHub's list (which is ordered by publish
     * date, not by version — so an older tag can show up first). Returns null
     * when there are no matching desktop releases.
     */
    private fun highestDesktopRelease(
        releases: List<GitHubRelease>,
        includePreReleases: Boolean,
    ): Pair<String, GitHubRelease>? =
        desktopReleases(releases, includePreReleases).firstOrNull()

    /**
     * All desktop releases (version -> release), newest DE version first.
     * Pre-releases are included only when [includePreReleases] is true.
     */
    private fun desktopReleases(
        releases: List<GitHubRelease>,
        includePreReleases: Boolean,
    ): List<Pair<String, GitHubRelease>> =
        releases.asSequence()
            .filter { !it.prerelease || includePreReleases }
            .mapNotNull { r -> deVersionFromTag(r.tagName)?.let { it to r } }
            .sortedWith { a, b -> compareVersions(b.first, a.first) }
            .toList()

    /**
     * Fetches the live CHANGELOG.md straight from the repository (default
     * branch), so the About → Changelog screen always shows the current
     * changelog without waiting for a new app build. Returns null on any
     * failure.
     */
    fun fetchChangelogFromRepo(): String? = try {
        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/${UpdateSource.repo()}/${UpdateSource.branch()}/CHANGELOG.md")
            .header("User-Agent", "VIVIMusic-Desktop-Updater")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body.string().takeIf { it.isNotBlank() }
        }
    } catch (_: Exception) {
        null
    }

    /**
     * Fetches the release notes (body) of the latest GitHub release, used by
     * the About → Changelog screen. Returns null on any failure.
     */
    fun latestReleaseNotes(): String? = try {
        val request = Request.Builder()
            .url("https://api.github.com/repos/${UpdateSource.repo()}/releases?per_page=100")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "VIVIMusic-Desktop-Updater")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val releases = json.decodeFromString<List<GitHubRelease>>(response.body.string())
            highestDesktopRelease(releases, includePreReleases = true)
                ?.second
                ?.body
                ?.takeIf { it.isNotBlank() }
        }
    } catch (_: Exception) {
        null
    }

    /**
     * Queries GitHub and decides whether a newer desktop release exists.
     * Pre-releases (nightly/beta/rc/alpha) are considered only when
     * [includePreReleases] is true.
     */
    fun check(includePreReleases: Boolean): UpdateStatus = try {
        val request = Request.Builder()
            .url("https://api.github.com/repos/${UpdateSource.repo()}/releases?per_page=100")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "VIVIMusic-Desktop-Updater")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return UpdateStatus.Failed("HTTP ${response.code}")
            val body = response.body.string()
            val releases = json.decodeFromString<List<GitHubRelease>>(body)

            val current = AppInfo.DE_VERSION
            val sorted = desktopReleases(releases, includePreReleases)
            if (sorted.isEmpty()) return UpdateStatus.Failed("No desktop release found")

            val newest = sorted.first()
            if (compareVersions(newest.first, current) <= 0) return UpdateStatus.UpToDate

            // Prefer the newest release that actually ships an installer for
            // this OS. If a release's build for this platform failed (so its
            // MSI/EXE/DMG/… asset is missing), falling through to the next
            // newest release with an asset avoids silently opening the browser.
            val withAsset = sorted.asSequence()
                .filter { (v, _) -> compareVersions(v, current) > 0 }
                .mapNotNull { (v, r) -> selectAsset(r.assets)?.let { Triple(v, r, it) } }
                .firstOrNull()

            if (withAsset != null) {
                return UpdateStatus.Available(withAsset.first, withAsset.second.htmlUrl, withAsset.third)
            }
            // A newer release exists but none of them has an installer asset for
            // this OS — surface the version anyway so the UI can offer the
            // release page instead of a broken in-app download.
            UpdateStatus.Available(newest.first, newest.second.htmlUrl, null)
        }
    } catch (e: Exception) {
        UpdateStatus.Failed(e.message ?: e::class.simpleName ?: "error")
    }
}
