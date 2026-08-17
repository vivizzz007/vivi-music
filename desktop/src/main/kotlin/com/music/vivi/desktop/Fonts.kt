package com.music.vivi.desktop

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

/**
 * Selectable app font (mirrors the Android app's `AppFont` enum). The same
 * `.ttf` variable fonts are bundled in `desktop/src/main/resources/fonts/`.
 */
enum class AppFont(val value: String) {
    SYSTEM("system"),
    GOOGLE_SANS("google_sans"),
    SANS_FLEX("sans_flex"),
    OUTFIT("outfit"),
    PLUS_JAKARTA_SANS("plus_jakarta_sans");

    companion object {
        fun fromValue(value: String): AppFont = entries.find { it.value == value } ?: SYSTEM
    }
}

/** Loads a bundled `.ttf` (classpath resource) as a 3-weight [FontFamily]. */
private fun fontFamily(resource: String): FontFamily = FontFamily(
    Font(resource, FontWeight.Normal),
    Font(resource, FontWeight.Medium),
    Font(resource, FontWeight.Bold),
)

/** The bundled app fonts (lazy so the app starts fast and only loads when used). */
object AppFonts {
    val googleSans: FontFamily by lazy { fontFamily("fonts/google_sans_flex.ttf") }
    val sansFlex: FontFamily by lazy { fontFamily("fonts/sans_flex.ttf") }
    val outfit: FontFamily by lazy { fontFamily("fonts/outfit.ttf") }
    val plusJakartaSans: FontFamily by lazy { fontFamily("fonts/plus_jakarta_sans.ttf") }

    fun familyFor(font: AppFont): FontFamily = when (font) {
        AppFont.SYSTEM -> FontFamily.Default
        AppFont.GOOGLE_SANS -> googleSans
        AppFont.SANS_FLEX -> sansFlex
        AppFont.OUTFIT -> outfit
        AppFont.PLUS_JAKARTA_SANS -> plusJakartaSans
    }
}
