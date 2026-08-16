package com.music.vivi.desktop

/** A selectable language: locale tag + native display name. */
data class AppLanguage(val code: String, val name: String)

/**
 * Languages supported by the desktop edition, mirroring the Android app's
 * `LanguageCodeToName` map. English is the primary (source) language.
 */
object Languages {
    val all: List<AppLanguage> = listOf(
        AppLanguage("en", "English"),
        AppLanguage("az", "Azərbaycan dili"),
        AppLanguage("bs", "Bosanski"),
        AppLanguage("ca", "Català"),
        AppLanguage("cs", "Čeština"),
        AppLanguage("de", "Deutsch"),
        AppLanguage("et", "Eesti"),
        AppLanguage("es", "Español"),
        AppLanguage("eu", "Euskara"),
        AppLanguage("fil", "Filipino"),
        AppLanguage("fr", "Français"),
        AppLanguage("hr", "Hrvatski"),
        AppLanguage("id", "Bahasa Indonesia"),
        AppLanguage("it", "Italiano"),
        AppLanguage("lt", "Lietuvių"),
        AppLanguage("hu", "Magyar"),
        AppLanguage("ms", "Bahasa Melayu"),
        AppLanguage("nl", "Nederlands"),
        AppLanguage("nb", "Norsk bokmål"),
        AppLanguage("pl", "Polski"),
        AppLanguage("pt", "Português"),
        AppLanguage("ro", "Română"),
        AppLanguage("sk", "Slovenčina"),
        AppLanguage("sl", "Slovenščina"),
        AppLanguage("sr", "Српски"),
        AppLanguage("fi", "Suomi"),
        AppLanguage("sv", "Svenska"),
        AppLanguage("vi", "Tiếng Việt"),
        AppLanguage("tr", "Türkçe"),
        AppLanguage("el", "Ελληνικά"),
        AppLanguage("be", "Беларуская"),
        AppLanguage("bg", "Български"),
        AppLanguage("ru", "Русский"),
        AppLanguage("uk", "Українська"),
        AppLanguage("ar", "العربية"),
        AppLanguage("hi", "हिन्दी"),
        AppLanguage("as", "অসমীয়া"),
        AppLanguage("bn", "বাংলা"),
        AppLanguage("pa", "ਪੰਜਾਬੀ"),
        AppLanguage("ta", "தமிழ்"),
        AppLanguage("te", "తెలుగు"),
        AppLanguage("ml", "മലയാളം"),
        AppLanguage("th", "ไทย"),
        AppLanguage("km", "ខ្មែរ"),
        AppLanguage("ko", "한국어"),
        AppLanguage("zh-rCN", "简体中文"),
        AppLanguage("zh-rTW", "繁體中文"),
        AppLanguage("ja", "日本語"),
    )

    fun name(code: String): String = all.firstOrNull { it.code == code }?.name ?: code

    /**
     * Maps an Android locale code (as stored by the mobile app's DataStore,
     * e.g. "no", "pt-PT", "zh-CN") to the equivalent desktop code ("nb", "pt",
     * "zh-rCN"). The two apps use slightly different locale tags for the same
     * languages, which otherwise breaks settings sync (a language changed on
     * one device would be silently ignored on the other).
     */
    fun fromMobileCode(code: String): String = when (code) {
        "no" -> "nb"
        "pt-PT", "pt-BR" -> "pt"
        "zh-CN", "zh-Hans" -> "zh-rCN"
        "zh-TW", "zh-HK", "zh-Hant" -> "zh-rTW"
        "en-GB", "en-US" -> "en"
        "es-419" -> "es"
        "fr-CA" -> "fr"
        else -> code
    }

    /** Inverse of [fromMobileCode]: desktop code -> Android locale code. */
    fun toMobileCode(code: String): String = when (code) {
        "nb" -> "no"
        "zh-rCN" -> "zh-CN"
        "zh-rTW" -> "zh-TW"
        else -> code
    }
}
