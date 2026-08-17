package com.music.vivi.desktop

/**
 * Build metadata for the desktop edition, read from `version.txt` which is
 * bundled as a classpath resource (copied by `processResources`).
 *
 * version.txt layout (comment lines prefixed with '#' are ignored):
 *   line 1 = mobile version, line 2 = mobile version code,
 *   line 3 = mobile release channel, line 4 = DE version,
 *   line 5 = desktop version code (monotonic counter, bumped each release),
 *   line 6 = desktop release channel.
 */
object AppInfo {
    val MOBILE_VERSION: String
    val MOBILE_VERSION_CODE: Int
    val MOBILE_CHANNEL: String
    val DE_VERSION: String
    val FULL_VERSION: String
    val CHANNEL: String
    val VERSION_CODE: Int

    init {
        val lines = runCatching {
            AppInfo::class.java.getResourceAsStream("/version.txt")
                ?.bufferedReader()
                ?.use { it.readLines() }
        }.getOrNull().orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        val mobile = lines.getOrNull(0).orEmpty()
        val de = lines.getOrNull(3).orEmpty()
        val channel = lines.getOrNull(5).orEmpty()

        MOBILE_VERSION = mobile.ifEmpty { "0.0.0" }
        MOBILE_VERSION_CODE = lines.getOrNull(1)?.toIntOrNull() ?: 0
        MOBILE_CHANNEL = lines.getOrNull(2).orEmpty().ifEmpty { "stable" }
        DE_VERSION = de.ifEmpty { "0.0.0" }
        FULL_VERSION = "${MOBILE_VERSION}_DE-${DE_VERSION}"
        CHANNEL = channel.ifEmpty { "stable" }
        // Line 5 is the explicit desktop version code (a small, monotonic
        // counter that matches the number of DE releases — not derived from the
        // SemVer). Fall back to the old formula only if line 5 is missing.
        VERSION_CODE = lines.getOrNull(4)?.toIntOrNull()
            ?: runCatching {
                val parts = DE_VERSION.split(".").map { it.toIntOrNull() ?: 0 }
                (parts.getOrNull(0) ?: 0) * 10000 + (parts.getOrNull(1) ?: 0) * 100 + (parts.getOrNull(2) ?: 0)
            }.getOrDefault(0)
    }
}
