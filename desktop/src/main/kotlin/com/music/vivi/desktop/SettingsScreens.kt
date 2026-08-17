package com.music.vivi.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.innertube.models.YouTubeLocale
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
    selectedFont: AppFont,
    onOpenTheme: () -> Unit,
    onOpenFont: () -> Unit,
    onOpenCanvas: () -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        AppearanceSection(language, selectedFont, onOpenTheme, onOpenFont, onOpenCanvas)
    }
}

@Composable
fun SettingsThemeScreen(
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
        ThemeSection(language, themeMode, accent, onThemeModeChange, onAccentChange, pureBlack, onPureBlackChange)
    }
}

@Composable
fun SettingsFontScreen(
    language: String,
    onBack: () -> Unit,
    selectedFont: AppFont,
    onFontChange: (AppFont) -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        FontSection(language, selectedFont, onFontChange)
    }
}

@Composable
fun SettingsCanvasScreen(
    language: String,
    onBack: () -> Unit,
    canvasEnabled: Boolean,
    onCanvasEnabledChange: (Boolean) -> Unit,
    canvasSource: CanvasSource,
    onCanvasSourceChange: (CanvasSource) -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        CanvasSection(language, canvasEnabled, onCanvasEnabledChange, canvasSource, onCanvasSourceChange)
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
    syncViviVolume: Boolean,
    onToggleSyncViviVolume: (Boolean) -> Unit,
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
            syncViviVolume,
            onToggleSyncViviVolume,
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
fun SettingsDevicesScreen(
    language: String,
    onBack: () -> Unit,
    syncManager: DesktopSyncManager,
    syncViviVolume: Boolean,
    onToggleSyncViviVolume: (Boolean) -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        DeviceSyncSection(language, syncManager, syncViviVolume, onToggleSyncViviVolume)
    }
}

@Composable
fun SettingsUpdatesScreen(
    language: String,
    onBack: () -> Unit,
    updateStatus: UpdateStatus,
    includePreReleases: Boolean,
    updateIntervalHours: Int,
    updateSource: String,
    onIntervalChange: (Int) -> Unit,
    onTogglePreReleases: (Boolean) -> Unit,
    onUpdateSourceChange: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onOpenChangelog: () -> Unit,
) {
    // Check for updates every time the section is opened.
    LaunchedEffect(Unit) { onCheckUpdates() }
    SettingsSubScreen(language, onBack) {
        UpdateSection(
            language,
            updateStatus,
            includePreReleases,
            updateIntervalHours,
            updateSource,
            onIntervalChange,
            onTogglePreReleases,
            onUpdateSourceChange,
            onCheckUpdates,
            onOpenChangelog,
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

/**
 * Backup & restore: exports/imports a full backup (settings, playlists, account
 * and library) via [BackupManager], plus the automatic-backup preferences and
 * the list of stored automatic backups.
 */
@Composable
fun SettingsBackupScreen(language: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }

    var autoBackupEnabled by remember { mutableStateOf(DesktopSettings.load().autoBackupEnabled) }
    var autoBackupWeekly by remember { mutableStateOf(DesktopSettings.load().autoBackupWeekly) }
    var autoBackupBeforeUpdate by remember { mutableStateOf(DesktopSettings.load().autoBackupBeforeUpdate) }

    var backups by remember { mutableStateOf<List<File>>(emptyList()) }
    var restoreTarget by remember { mutableStateOf<File?>(null) }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var pendingRestore by remember { mutableStateOf<File?>(null) }
    var pendingDelete by remember { mutableStateOf<File?>(null) }

    fun reloadBackups() { backups = BackupManager.listAuto() }

    LaunchedEffect(Unit) { reloadBackups() }

    // Defer destructive actions until the dialog is dismissed, so the list
    // reflows after the popup window is torn down (avoids the Compose
    // "layouts are not part of the same hierarchy" crash).
    LaunchedEffect(restoreTarget) {
        val f = pendingRestore
        if (restoreTarget == null && f != null) {
            pendingRestore = null
            withContext(Dispatchers.IO) { BackupManager.import(f) }
            showRestartDialog = true
        }
    }
    LaunchedEffect(deleteTarget) {
        val f = pendingDelete
        if (deleteTarget == null && f != null) {
            pendingDelete = null
            withContext(Dispatchers.IO) { BackupManager.deleteAuto(f) }
            reloadBackups()
        }
    }

    SettingsSubScreen(language, onBack) {
        Text(
            Localization.get(language, "backup_restore"),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            Localization.get(language, "backup_restore_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                if (busy) return@OutlinedButton
                busy = true
                scope.launch {
                    val file = withContext(Dispatchers.IO) { chooseBackupFile(save = true) }
                    val ok = file != null && withContext(Dispatchers.IO) { BackupManager.export(file) }
                    busy = false
                    DesktopSnackbar.show(Localization.get(language, if (ok) "backup_create_success" else "backup_create_failed"))
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(Localization.get(language, "action_backup"))
        }
        Text(
            Localization.get(language, "backup_desc"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (busy) return@Button
                busy = true
                scope.launch {
                    val file = withContext(Dispatchers.IO) { chooseBackupFile(save = false) }
                    val ok = file != null && withContext(Dispatchers.IO) { BackupManager.import(file) }
                    busy = false
                    if (ok) showRestartDialog = true
                    else DesktopSnackbar.show(Localization.get(language, "restore_failed"))
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(Localization.get(language, "action_restore"))
        }
        Text(
            Localization.get(language, "restore_desc"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        HorizontalDivider(Modifier.padding(vertical = 20.dp))

        // Automatic backups
        Text(Localization.get(language, "auto_backup"), style = MaterialTheme.typography.titleMedium)
        Text(
            Localization.get(language, "automatic_backup_desc"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )

        BackupToggleRow(
            language = language,
            titleKey = "enable_automatic_backup",
            checked = autoBackupEnabled,
            onCheckedChange = {
                autoBackupEnabled = it
                DesktopSettings.update { s -> s.copy(autoBackupEnabled = it) }
            },
        )
        BackupToggleRow(
            language = language,
            titleKey = "weekly_backup",
            descKey = "weekly_backup_desc",
            checked = autoBackupWeekly,
            enabled = autoBackupEnabled,
            onCheckedChange = {
                autoBackupWeekly = it
                DesktopSettings.update { s -> s.copy(autoBackupWeekly = it) }
            },
        )
        BackupToggleRow(
            language = language,
            titleKey = "backup_before_update",
            descKey = "backup_before_update_desc",
            checked = autoBackupBeforeUpdate,
            enabled = autoBackupEnabled,
            onCheckedChange = {
                autoBackupBeforeUpdate = it
                DesktopSettings.update { s -> s.copy(autoBackupBeforeUpdate = it) }
            },
        )

        // Stored automatic backups
        Text(
            Localization.get(language, "stored_backups"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp),
        )
        if (backups.isEmpty()) {
            Text(
                Localization.get(language, "backups_empty"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            backups.forEach { file ->
                val (date, type) = parseAutoBackupName(file.name)
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(date, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            Localization.get(language, type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = {
                            pendingRestore = file
                            restoreTarget = file
                        },
                    ) {
                        Text(Localization.get(language, "action_restore"))
                    }
                    TextButton(
                        onClick = {
                            pendingDelete = file
                            deleteTarget = file
                        },
                    ) {
                        Text(Localization.get(language, "delete"), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(Localization.get(language, "restore_success_title")) },
            text = { Text(Localization.get(language, "restore_success")) },
            confirmButton = {
                Button(onClick = { restartApplication() }) {
                    Text(Localization.get(language, "restart_now"))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestartDialog = false }) {
                    Text(Localization.get(language, "later"))
                }
            },
        )
    }

    restoreTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null; pendingRestore = null },
            title = { Text(Localization.get(language, "action_restore")) },
            text = { Text(Localization.get(language, "restore_backup_confirm")) },
            confirmButton = {
                TextButton(onClick = { restoreTarget = null }) {
                    Text(Localization.get(language, "action_restore"))
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreTarget = null; pendingRestore = null }) {
                    Text(Localization.get(language, "later"))
                }
            },
        )
    }

    deleteTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null; pendingDelete = null },
            title = { Text(Localization.get(language, "delete")) },
            text = { Text(Localization.get(language, "delete_backup_confirm")) },
            confirmButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(Localization.get(language, "delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null; pendingDelete = null }) {
                    Text(Localization.get(language, "later"))
                }
            },
        )
    }
}

/** A labelled switch row used by the backup screen (title + optional description). */
@Composable
private fun BackupToggleRow(
    language: String,
    titleKey: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    descKey: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Column(Modifier.weight(1f)) {
            Text(Localization.get(language, titleKey))
            if (descKey != null) {
                Text(
                    Localization.get(language, descKey),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Extracts a display date + a type key (`backup_type_weekly`/`backup_type_before_update`) from a backup filename. */
private fun parseAutoBackupName(name: String): Pair<String, String> {
    val ts = Regex("""(\d{8}_\d{6})\.vivide\.backup$""").find(name)?.groupValues?.getOrNull(1)
    val date = if (ts != null) {
        runCatching {
            LocalDateTime.parse(ts, DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        }.getOrDefault(ts)
    } else {
        name
    }
    val type = if (name.contains("before_update")) "backup_type_before_update" else "backup_type_weekly"
    return date to type
}

/** Native save/open dialog for the backup file (blocks; call on Dispatchers.IO). */
private fun chooseBackupFile(save: Boolean): File? = runCatching {
    val dialog = java.awt.FileDialog(
        null as java.awt.Frame?,
        if (save) "Backup settings" else "Restore settings",
        if (save) java.awt.FileDialog.SAVE else java.awt.FileDialog.LOAD,
    )
    if (save) dialog.file = BackupManager.defaultBackupFileName()
    dialog.isVisible = true
    val dir = dialog.directory
    val name = dialog.file
    dialog.dispose()
    if (dir != null && name != null) File(dir, name) else null
}.getOrNull()

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

    // Master switch, always visible (unlock is also available from About).
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
        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        // ---- Display: where the live stats are shown ----
        DevSectionHeader(language, "dev_tools_mode")
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { DeveloperOptions.setMode(DevToolsMode.OVERLAY) },
                    enabled = mode != DevToolsMode.OVERLAY,
                ) { Text(Localization.get(language, "dev_tools_overlay")) }
                OutlinedButton(
                    onClick = { DeveloperOptions.setMode(DevToolsMode.WINDOW) },
                    enabled = mode != DevToolsMode.WINDOW,
                ) { Text(Localization.get(language, "dev_tools_window")) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { DeveloperOptions.setMode(DevToolsMode.TITLE_BAR) },
                    enabled = mode != DevToolsMode.TITLE_BAR,
                ) { Text(Localization.get(language, "dev_tools_title_bar_only")) }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        // ---- Monitoring: how much detail is shown ----
        DevSectionHeader(language, "dev_tools_profile")
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

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        // ---- Overlay behaviour ----
        DevSectionHeader(language, "dev_tools_movable")
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Switch(checked = movable, onCheckedChange = { DeveloperOptions.setOverlayMovable(it) })
            Column(Modifier.clickable { DeveloperOptions.setOverlayMovable(!movable) }) {
                Text(
                    Localization.get(language, "dev_tools_movable_desc"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        // ---- Title bar ----
        DevSectionHeader(language, "dev_tools_title_bar")
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Switch(checked = titleBar, onCheckedChange = { DeveloperOptions.setShowInTitleBar(it) })
            Column(Modifier.clickable { DeveloperOptions.setShowInTitleBar(!titleBar) }) {
                Text(
                    Localization.get(language, "dev_tools_title_bar_desc"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Section heading inside the (reorganized) developer options screen. */
@Composable
private fun DevSectionHeader(language: String, key: String) {
    Text(
        Localization.get(language, key),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** Notification preferences: where update notifications are shown. */
@Composable
fun SettingsNotificationsScreen(
    language: String,
    onBack: () -> Unit,
    notificationMode: String,
    onNotificationModeChange: (String) -> Unit,
    notificationDurationSeconds: Int,
    onNotificationDurationChange: (Int) -> Unit,
    saveHistory: Boolean,
    onSaveHistoryChange: (Boolean) -> Unit,
    onOpenHistory: () -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        Text(Localization.get(language, "notifications"), style = MaterialTheme.typography.titleLarge)
        Text(
            Localization.get(language, "notification_mode_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(12.dp))

        NotificationModeOption(
            language = language,
            title = Localization.get(language, "notification_main_window"),
            selected = notificationMode != "native",
            onClick = { onNotificationModeChange("in_app") },
        )
        NotificationModeOption(
            language = language,
            title = Localization.get(language, "notification_native"),
            tag = Localization.get(language, "experimental"),
            selected = notificationMode == "native",
            onClick = { onNotificationModeChange("native") },
        )

        Spacer(Modifier.height(16.dp))
        Text(
            Localization.get(language, "notification_duration"),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            Localization.get(language, "notification_duration_desc"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
        )
        NotificationDurationOption(
            seconds = 3,
            selected = notificationDurationSeconds == 3,
            onClick = { onNotificationDurationChange(3) },
        )
        NotificationDurationOption(
            seconds = 5,
            selected = notificationDurationSeconds == 5,
            onClick = { onNotificationDurationChange(5) },
        )
        NotificationDurationOption(
            seconds = 10,
            selected = notificationDurationSeconds == 10,
            onClick = { onNotificationDurationChange(10) },
        )
        NotificationDurationOption(
            seconds = 15,
            selected = notificationDurationSeconds == 15,
            onClick = { onNotificationDurationChange(15) },
        )
        NotificationDurationOption(
            seconds = 30,
            selected = notificationDurationSeconds == 30,
            onClick = { onNotificationDurationChange(30) },
        )

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onSaveHistoryChange(!saveHistory) }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                Localization.get(language, "save_notification_history"),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = saveHistory, onCheckedChange = onSaveHistoryChange)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenHistory)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                Localization.get(language, "notification_history"),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotificationDurationOption(seconds: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${seconds}s", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) {
            Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Scrollable list of recent notifications (in-app and native). */
@Composable
fun NotificationHistoryScreen(
    language: String,
    onBack: () -> Unit,
) {
    var history by remember { mutableStateOf(NotificationHistory.list()) }
    SettingsSubScreen(language, onBack) {
        Text(Localization.get(language, "notification_history"), style = MaterialTheme.typography.titleLarge)
        Text(
            Localization.get(language, "notification_history_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = {
                NotificationHistory.clear()
                history = emptyList()
            }) {
                Text(Localization.get(language, "clear_history"))
            }
        }
        Spacer(Modifier.height(8.dp))
        if (history.isEmpty()) {
            Text(
                Localization.get(language, "history_empty"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            history.forEach { record -> NotificationHistoryItem(language, record) }
        }
    }
}

@Composable
private fun NotificationHistoryItem(language: String, record: NotificationRecord) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                record.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                formatNotificationTime(record.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            record.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(
                if (record.mode == "native") Localization.get(language, "notification_native") else Localization.get(language, "notification_main_window"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

private fun formatNotificationTime(epochMillis: Long): String = runCatching {
    val dt = java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault())
    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(dt)
}.getOrDefault("")

@Composable
private fun NotificationModeOption(
    language: String,
    title: String,
    tag: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (tag != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        if (selected) {
            Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
        }
    }
}
