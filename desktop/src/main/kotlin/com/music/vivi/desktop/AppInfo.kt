package com.music.vivi.desktop

/**
 * Build metadata for the desktop edition, read from `version.txt` which is
 * bundled as a classpath resource (copied by `processResources`).
 *
 * version.txt layout:
 *   line 1 = mobile version, line 2 = DE version, line 3 = release channel.
 */
object AppInfo {
    val MOBILE_VERSION: String
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

        val mobile = lines.getOrNull(0)?.trim().orEmpty()
        val de = lines.getOrNull(1)?.trim().orEmpty()
        val channel = lines.getOrNull(2)?.trim().orEmpty()

        MOBILE_VERSION = mobile.ifEmpty { "0.0.0" }
        DE_VERSION = de.ifEmpty { "0.0.0" }
        FULL_VERSION = "${MOBILE_VERSION}_DE-${DE_VERSION}"
        CHANNEL = channel.ifEmpty { "stable" }
        VERSION_CODE = runCatching {
            val parts = DE_VERSION.split(".").map { it.toIntOrNull() ?: 0 }
            (parts.getOrNull(0) ?: 0) * 10000 + (parts.getOrNull(1) ?: 0) * 100 + (parts.getOrNull(2) ?: 0)
        }.getOrDefault(0)
    }
}
