package com.music.vivi.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.music.innertube.models.YouTubeLocale
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** YouTube host-language codes (mirrors the Android app's `LanguageCodeToName`). */
val LanguageCodeToName: Map<String, String> = mapOf(
    "af" to "Afrikaans",
    "az" to "Azərbaycan",
    "id" to "Bahasa Indonesia",
    "ms" to "Bahasa Malaysia",
    "ca" to "Català",
    "cs" to "Čeština",
    "da" to "Dansk",
    "de" to "Deutsch",
    "et" to "Eesti",
    "en-GB" to "English (UK)",
    "en" to "English (US)",
    "es" to "Español (España)",
    "es-419" to "Español (Latinoamérica)",
    "eu" to "Euskara",
    "fil" to "Filipino",
    "fr" to "Français",
    "fr-CA" to "Français (Canada)",
    "gl" to "Galego",
    "hr" to "Hrvatski",
    "zu" to "IsiZulu",
    "is" to "Íslenska",
    "it" to "Italiano",
    "sw" to "Kiswahili",
    "lt" to "Lietuvių",
    "hu" to "Magyar",
    "nl" to "Nederlands",
    "no" to "Norsk",
    "or" to "Odia",
    "uz" to "O‘zbe",
    "pl" to "Polski",
    "pt-PT" to "Português",
    "pt" to "Português (Brasil)",
    "ro" to "Română",
    "sq" to "Shqip",
    "sk" to "Slovenčina",
    "sl" to "Slovenščina",
    "fi" to "Suomi",
    "sv" to "Svenska",
    "bo" to "Tibetan བོད་སྐད།",
    "vi" to "Tiếng Việt",
    "tr" to "Türkçe",
    "bg" to "Български",
    "ky" to "Кыргызча",
    "kk" to "Қазақ Тілі",
    "mk" to "Македонски",
    "mn" to "Монгол",
    "ru" to "Русский",
    "sr" to "Српски",
    "uk" to "Українська",
    "el" to "Ελληνικά",
    "hy" to "Հայերեն",
    "iw" to "עברית",
    "ur" to "اردو",
    "ar" to "العربية",
    "fa" to "فارسی",
    "ne" to "नेपाली",
    "mr" to "मराठी",
    "hi" to "हिन्दी",
    "bn" to "বাংলা",
    "pa" to "ਪੰਜਾਬੀ",
    "gu" to "ગુજરાતી",
    "ta" to "தமிழ்",
    "te" to "తెలుగు",
    "kn" to "ಕನ್ನಡ",
    "ml" to "മലയാളം",
    "si" to "සිංහල",
    "th" to "ภาษาไทย",
    "lo" to "ລາວ",
    "my" to "ဗမာ",
    "ka" to "ქართული",
    "am" to "አማርኛ",
    "km" to "ខ្មែរ",
    "zh-CN" to "中文 (简体)",
    "zh-TW" to "中文 (繁體)",
    "zh-HK" to "中文 (香港)",
    "ja" to "日本語",
    "ko" to "한국어",
)

/** YouTube geolocation codes (mirrors the Android app's `CountryCodeToName`). */
val CountryCodeToName: Map<String, String> = mapOf(
    "DZ" to "Algeria",
    "AR" to "Argentina",
    "AU" to "Australia",
    "AT" to "Austria",
    "AZ" to "Azerbaijan",
    "BH" to "Bahrain",
    "BD" to "Bangladesh",
    "BY" to "Belarus",
    "BE" to "Belgium",
    "BO" to "Bolivia",
    "BA" to "Bosnia and Herzegovina",
    "BR" to "Brazil",
    "BG" to "Bulgaria",
    "KH" to "Cambodia",
    "CA" to "Canada",
    "CL" to "Chile",
    "HK" to "Hong Kong",
    "CO" to "Colombia",
    "CR" to "Costa Rica",
    "HR" to "Croatia",
    "CY" to "Cyprus",
    "CZ" to "Czech Republic",
    "DK" to "Denmark",
    "DO" to "Dominican Republic",
    "EC" to "Ecuador",
    "EG" to "Egypt",
    "SV" to "El Salvador",
    "EE" to "Estonia",
    "FI" to "Finland",
    "FR" to "France",
    "GE" to "Georgia",
    "DE" to "Germany",
    "GH" to "Ghana",
    "GR" to "Greece",
    "GT" to "Guatemala",
    "HN" to "Honduras",
    "HU" to "Hungary",
    "IS" to "Iceland",
    "IN" to "India",
    "ID" to "Indonesia",
    "IQ" to "Iraq",
    "IE" to "Ireland",
    "IL" to "Israel",
    "IT" to "Italy",
    "JM" to "Jamaica",
    "JP" to "Japan",
    "JO" to "Jordan",
    "KZ" to "Kazakhstan",
    "KE" to "Kenya",
    "KR" to "South Korea",
    "KW" to "Kuwait",
    "LA" to "Lao",
    "LV" to "Latvia",
    "LB" to "Lebanon",
    "LY" to "Libya",
    "LI" to "Liechtenstein",
    "LT" to "Lithuania",
    "LU" to "Luxembourg",
    "MK" to "Macedonia",
    "MY" to "Malaysia",
    "MT" to "Malta",
    "MX" to "Mexico",
    "ME" to "Montenegro",
    "MA" to "Morocco",
    "NP" to "Nepal",
    "NL" to "Netherlands",
    "NZ" to "New Zealand",
    "NI" to "Nicaragua",
    "NG" to "Nigeria",
    "NO" to "Norway",
    "OM" to "Oman",
    "PK" to "Pakistan",
    "PA" to "Panama",
    "PG" to "Papua New Guinea",
    "PY" to "Paraguay",
    "PE" to "Peru",
    "PH" to "Philippines",
    "PL" to "Poland",
    "PT" to "Portugal",
    "PR" to "Puerto Rico",
    "QA" to "Qatar",
    "RO" to "Romania",
    "RU" to "Russian Federation",
    "SA" to "Saudi Arabia",
    "SN" to "Senegal",
    "RS" to "Serbia",
    "SG" to "Singapore",
    "SK" to "Slovakia",
    "SI" to "Slovenia",
    "ZA" to "South Africa",
    "ES" to "Spain",
    "LK" to "Sri Lanka",
    "SE" to "Sweden",
    "CH" to "Switzerland",
    "TW" to "Taiwan",
    "TZ" to "Tanzania",
    "TH" to "Thailand",
    "TN" to "Tunisia",
    "TR" to "Turkey",
    "UG" to "Uganda",
    "UA" to "Ukraine",
    "AE" to "United Arab Emirates",
    "GB" to "United Kingdom",
    "US" to "United States",
    "UY" to "Uruguay",
    "VE" to "Venezuela (Bolivarian Republic)",
    "VN" to "Vietnam",
    "YE" to "Yemen",
    "ZW" to "Zimbabwe",
)

/**
 * Resolve the innerTube locale from the saved content language/country. Blank
 * values fall back to the OS default (like the Android app's "system" default).
 */
fun resolveYouTubeLocale(contentLanguage: String, contentCountry: String): YouTubeLocale {
    val system = java.util.Locale.getDefault()
    val hl = contentLanguage.ifBlank { system.language.ifBlank { "en" } }
    val gl = contentCountry.ifBlank { system.country.ifBlank { "US" } }
    return YouTubeLocale(gl = gl, hl = hl)
}

/** Shared scaffold for settings sub-screens: back button + scrollable content. */
@Composable
fun SettingsSubScreen(language: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        BackButton(language, onBack)
        content()
    }
}

@Composable
fun SettingsLanguageScreen(language: String, onBack: () -> Unit, onLanguageChange: (String) -> Unit) {
    SettingsSubScreen(language, onBack) { LanguageSection(language, onLanguageChange) }
}

@Composable
fun SettingsAppearanceScreen(
    language: String,
    onBack: () -> Unit,
    themeMode: ThemeMode,
    accent: androidx.compose.ui.graphics.Color,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentChange: (androidx.compose.ui.graphics.Color) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        AppearanceSection(language, themeMode, accent, onThemeModeChange, onAccentChange, pureBlack, onPureBlackChange)
    }
}

@Composable
fun SettingsPlayerScreen(
    language: String,
    onBack: () -> Unit,
    autoPlayNext: Boolean,
    onToggleAutoPlayNext: (Boolean) -> Unit,
    audioQuality: String,
    onAudioQualityChange: (String) -> Unit,
    rememberShuffleRepeat: Boolean,
    onToggleRememberShuffleRepeat: (Boolean) -> Unit,
    persistentQueue: Boolean,
    onTogglePersistentQueue: (Boolean) -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        PlayerSection(
            language,
            autoPlayNext,
            onToggleAutoPlayNext,
            audioQuality,
            onAudioQualityChange,
            rememberShuffleRepeat,
            onToggleRememberShuffleRepeat,
            persistentQueue,
            onTogglePersistentQueue,
        )
    }
}

@Composable
fun SettingsAccountScreen(
    language: String,
    onBack: () -> Unit,
    isLoggedIn: Boolean,
    accountName: String,
    onOpenLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    SettingsSubScreen(language, onBack) { AccountSection(language, isLoggedIn, accountName, onOpenLogin, onLogout) }
}

@Composable
fun SettingsDevicesScreen(language: String, onBack: () -> Unit, syncManager: DesktopSyncManager) {
    SettingsSubScreen(language, onBack) { DeviceSyncSection(language, syncManager) }
}

@Composable
fun SettingsUpdatesScreen(
    language: String,
    onBack: () -> Unit,
    updateStatus: UpdateStatus,
    includePreReleases: Boolean,
    updateIntervalHours: Int,
    onIntervalChange: (Int) -> Unit,
    onTogglePreReleases: (Boolean) -> Unit,
    onCheckUpdates: () -> Unit,
) {
    // Check for updates every time the section is opened.
    LaunchedEffect(Unit) { onCheckUpdates() }
    SettingsSubScreen(language, onBack) {
        UpdateSection(
            language,
            updateStatus,
            includePreReleases,
            updateIntervalHours,
            onIntervalChange,
            onTogglePreReleases,
            onCheckUpdates,
        )
    }
}

@Composable
fun SettingsAboutScreen(language: String, onBack: () -> Unit, onOpenChangelog: () -> Unit) {
    SettingsSubScreen(language, onBack) { AboutSection(language, onOpenChangelog) }
}

@Composable
fun SettingsDeveloperScreen(language: String, onBack: () -> Unit, syncManager: DesktopSyncManager) {
    SettingsSubScreen(language, onBack) { DeveloperSection(language, syncManager) }
}

@Composable
fun SettingsStorageScreen(language: String, onBack: () -> Unit) {
    SettingsSubScreen(language, onBack) { StorageSection(language) }
}

@Composable
fun SettingsContentScreen(
    language: String,
    onBack: () -> Unit,
    contentLanguage: String,
    contentCountry: String,
    onContentLanguageChange: (String) -> Unit,
    onContentCountryChange: (String) -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        ContentSection(language, contentLanguage, contentCountry, onContentLanguageChange, onContentCountryChange)
    }
}

@Composable
fun SettingsLyricsScreen(
    language: String,
    onBack: () -> Unit,
    syncedLyrics: Boolean,
    onToggleSyncedLyrics: (Boolean) -> Unit,
    lyricsTextSize: Float,
    onLyricsTextSizeChange: (Float) -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        LyricsSection(language, syncedLyrics, onToggleSyncedLyrics, lyricsTextSize, onLyricsTextSizeChange)
    }
}

@Composable
fun SettingsPrivacyScreen(language: String, onBack: () -> Unit, isLoggedIn: Boolean, onLogout: () -> Unit) {
    SettingsSubScreen(language, onBack) { PrivacySection(language, isLoggedIn, onLogout) }
}

/** Content section: innerTube host language + region (hl/gl). */
@Composable
fun ContentSection(
    language: String,
    contentLanguage: String,
    contentCountry: String,
    onContentLanguageChange: (String) -> Unit,
    onContentCountryChange: (String) -> Unit,
) {
    var languageExpanded by remember { mutableStateOf(false) }
    var countryExpanded by remember { mutableStateOf(false) }

    Text(Localization.get(language, "content"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

    Text(Localization.get(language, "content_language"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    Box(Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = { languageExpanded = true }) {
            Text(LanguageCodeToName[contentLanguage] ?: Localization.get(language, "system_default"))
        }
        DropdownMenu(expanded = languageExpanded, onDismissRequest = { languageExpanded = false }) {
            DropdownMenuItem(
                text = { Text(Localization.get(language, "system_default")) },
                onClick = { languageExpanded = false; onContentLanguageChange("") },
            )
            LanguageCodeToName.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { languageExpanded = false; onContentLanguageChange(code) },
                )
            }
        }
    }

    Text(Localization.get(language, "content_country"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    Box(Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = { countryExpanded = true }) {
            Text(CountryCodeToName[contentCountry] ?: Localization.get(language, "system_default"))
        }
        DropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }) {
            DropdownMenuItem(
                text = { Text(Localization.get(language, "system_default")) },
                onClick = { countryExpanded = false; onContentCountryChange("") },
            )
            CountryCodeToName.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { countryExpanded = false; onContentCountryChange(code) },
                )
            }
        }
    }
}

/** Lyrics section: synced (line-by-line) highlighting toggle + text size. */
@Composable
fun LyricsSection(
    language: String,
    syncedLyrics: Boolean,
    onToggleSyncedLyrics: (Boolean) -> Unit,
    lyricsTextSize: Float,
    onLyricsTextSizeChange: (Float) -> Unit,
) {
    Text(Localization.get(language, "lyrics"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(checked = syncedLyrics, onCheckedChange = onToggleSyncedLyrics)
        Column(Modifier.clickable { onToggleSyncedLyrics(!syncedLyrics) }) {
            Text(Localization.get(language, "synced_lyrics"))
            Text(
                Localization.get(language, "synced_lyrics_desc"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Text(
        "${Localization.get(language, "lyrics_text_size")}: ${lyricsTextSize.toInt()} sp",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
    androidx.compose.material3.Slider(
        value = lyricsTextSize,
        onValueChange = onLyricsTextSizeChange,
        valueRange = 12f..32f,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Privacy section: clear the local session, cache and downloaded installers. */
@Composable
fun PrivacySection(language: String, isLoggedIn: Boolean, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    val cacheDir = remember { File(System.getProperty("user.home"), ".vivimusic/cache") }
    var cacheCleared by remember { mutableStateOf(false) }
    var installersCleared by remember { mutableStateOf(false) }

    Text(Localization.get(language, "privacy"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
    Text(
        Localization.get(language, "privacy_desc"),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )

    if (isLoggedIn) {
        Button(onClick = onLogout, modifier = Modifier.padding(top = 12.dp)) {
            Text(Localization.get(language, "clear_session"))
        }
    } else {
        Text(
            Localization.get(language, "not_logged_in"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }

    Button(
        onClick = {
            cacheCleared = false
            scope.launch {
                withContext(Dispatchers.IO) { cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
                cacheCleared = true
            }
        },
        modifier = Modifier.padding(top = 12.dp),
    ) { Text(Localization.get(language, "clear_cache")) }

    Button(
        onClick = {
            UpdateDownloader.deleteAll()
            installersCleared = true
        },
        modifier = Modifier.padding(top = 12.dp),
    ) { Text(Localization.get(language, "delete_installers")) }

    if (cacheCleared) {
        Text(
            Localization.get(language, "cache_cleared"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    if (installersCleared) {
        Text(
            Localization.get(language, "installers_deleted"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Developer options: enable/disable the live stats, pick a profile and placement. */
@Composable
fun DeveloperSection(language: String, syncManager: DesktopSyncManager) {
    val enabled by DeveloperOptions.enabled.collectAsState()
    val mode by DeveloperOptions.mode.collectAsState()
    val profile by DeveloperOptions.profile.collectAsState()
    val movable by DeveloperOptions.overlayMovable.collectAsState()
    val titleBar by DeveloperOptions.showInTitleBar.collectAsState()

    Text(
        Localization.get(language, "developer_options"),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 12.dp),
    )
    Text(
        Localization.get(language, "developer_options_desc"),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )

    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(checked = enabled, onCheckedChange = { DeveloperOptions.setEnabled(it) })
        Column(Modifier.clickable { DeveloperOptions.setEnabled(!enabled) }) {
            Text(Localization.get(language, "developer_options_enabled"))
        }
    }

    if (enabled) {
        Text(
            Localization.get(language, "dev_tools_mode"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { DeveloperOptions.setMode(DevToolsMode.OVERLAY) },
                enabled = mode != DevToolsMode.OVERLAY,
            ) { Text(Localization.get(language, "dev_tools_overlay")) }
            OutlinedButton(
                onClick = { DeveloperOptions.setMode(DevToolsMode.WINDOW) },
                enabled = mode != DevToolsMode.WINDOW,
            ) { Text(Localization.get(language, "dev_tools_window")) }
        }

        Text(
            Localization.get(language, "dev_tools_profile"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { DeveloperOptions.setProfile(DevToolsProfile.FULL) },
                enabled = profile != DevToolsProfile.FULL,
            ) { Text(Localization.get(language, "dev_tools_profile_full")) }
            OutlinedButton(
                onClick = { DeveloperOptions.setProfile(DevToolsProfile.PERFORMANCE) },
                enabled = profile != DevToolsProfile.PERFORMANCE,
            ) { Text(Localization.get(language, "dev_tools_profile_performance")) }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Switch(checked = movable, onCheckedChange = { DeveloperOptions.setOverlayMovable(it) })
            Column(Modifier.clickable { DeveloperOptions.setOverlayMovable(!movable) }) {
                Text(Localization.get(language, "dev_tools_movable"))
                Text(
                    Localization.get(language, "dev_tools_movable_desc"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Switch(checked = titleBar, onCheckedChange = { DeveloperOptions.setShowInTitleBar(it) })
            Column(Modifier.clickable { DeveloperOptions.setShowInTitleBar(!titleBar) }) {
                Text(Localization.get(language, "dev_tools_title_bar"))
                Text(
                    Localization.get(language, "dev_tools_title_bar_desc"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
