package com.music.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Runs(
    val runs: List<Run>?,
)

@Serializable
data class Run(
    val text: String,
    val navigationEndpoint: NavigationEndpoint?,
)

private const val compactCountSuffixPattern =
    "KkMmBbTt\\u4e07\\u842c\\u5104\\u4ebf\\u5146\\u5343\\ucc9c\\ub9cc\\uc5b5"
private val countTextRegex =
    Regex("""\p{Nd}[\p{Nd}\s,.\uFF0C\uFF0E]*[$compactCountSuffixPattern]*""")
private val separatedSuffixRegex = Regex("""\s+(?=[$compactCountSuffixPattern]$)""")

internal fun Runs?.extractCountText(): String? {
    val texts = this?.runs
        ?.map { it.text.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()

    return texts
        .joinToString(separator = "")
        .extractCountValue()
        ?: texts.firstNotNullOfOrNull { it.extractCountValue() }
}

private fun String.extractCountValue(): String? =
    countTextRegex.find(this)
        ?.value
        ?.trim()
        ?.replace(separatedSuffixRegex, "")
        ?.takeIf { value -> value.any { it.isDigit() } }

fun List<Run>.splitBySeparator(): List<List<Run>> {
    val res = mutableListOf<List<Run>>()
    var tmp = mutableListOf<Run>()
    forEach { run ->
        if (run.text == " • ") {
            res.add(tmp)
            tmp = mutableListOf()
        } else {
            tmp.add(run)
        }
    }
    res.add(tmp)
    return res
}

fun List<List<Run>>.clean(): List<List<Run>> =
    if (getOrNull(0)?.getOrNull(0)?.navigationEndpoint != null ||
        (getOrNull(0)?.getOrNull(0)?.text?.contains(regex = Regex("[&,]"))) != false
    ) {
        this
    } else {
        this.drop(1)
    }

fun List<Run>.oddElements() =
    filterIndexed { index, _ ->
        index % 2 == 0
    }

private val VIEW_COUNT_REGEX = Regex("""^[\d,.]+[KMB]?\s*(view|play)s?$""", RegexOption.IGNORE_CASE)

/** Returns the first run text that looks like a view/play count (e.g. "12M views", "1.2B plays"). */
fun List<List<Run>>.findViewCountText(): String? =
    flatten().map { it.text.trim() }.firstOrNull { it.matches(VIEW_COUNT_REGEX) }
