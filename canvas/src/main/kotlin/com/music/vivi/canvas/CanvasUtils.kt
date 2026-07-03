package com.music.vivi.canvas

import java.text.Normalizer
import java.util.Locale

fun String.normalizeForComparison(): String {
    val decomposed = Normalizer.normalize(this, Normalizer.Form.NFD)
    val withoutDiacritics = Regex("\\p{InCombiningDiacriticalMarks}+").replace(decomposed, "")
    return withoutDiacritics.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
