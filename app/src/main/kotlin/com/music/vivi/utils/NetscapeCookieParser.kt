/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

/**
 * Parses a Netscape-format cookies.txt file (as exported by browser extensions like
 * "Get cookies.txt LOCALLY") into an HTTP `Cookie:` header string, e.g. "name=value; name2=value2".
 *
 * Format: one cookie per line, 7 tab-separated fields:
 * domain \t includeSubdomains \t path \t secure \t expiry \t name \t value
 * Comment lines start with '#', except "#HttpOnly_" which prefixes a valid cookie line.
 */
object NetscapeCookieParser {
    private val relevantDomains = listOf("youtube.com", "google.com")

    fun parse(content: String): String {
        return content.lineSequence()
            .mapNotNull { rawLine -> parseLine(rawLine) }
            .filter { (domain, _, _) -> relevantDomains.any { domain.endsWith(it) } }
            .joinToString("; ") { (_, name, value) -> "$name=$value" }
    }

    private fun parseLine(rawLine: String): Triple<String, String, String>? {
        val line = rawLine.trim()
        if (line.isEmpty()) return null

        val effectiveLine = when {
            line.startsWith("#HttpOnly_") -> line.removePrefix("#HttpOnly_")
            line.startsWith("#") -> return null
            else -> line
        }

        val fields = effectiveLine.split("\t")
        if (fields.size < 7) return null

        val domain = fields[0].removePrefix(".")
        val name = fields[5]
        val value = fields[6]
        if (name.isBlank()) return null

        return Triple(domain, name, value)
    }
}
