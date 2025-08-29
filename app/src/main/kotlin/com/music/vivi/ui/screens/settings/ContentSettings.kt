package com.music.vivi.ui.screens.settings

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.os.LocaleList
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.toLowerCase
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import com.music.innertube.YouTube
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.*
import com.music.vivi.ui.component.*
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import java.net.Proxy
import java.util.Locale
import androidx.core.net.toUri

import com.music.vivi.constants.SwipeThumbnailKey
import com.music.vivi.constants.SwipeSensitivityKey
import com.music.vivi.constants.SwipeToSongKey
import com.music.vivi.constants.HidePlayerThumbnailKey
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.EnumListPreference
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.ListPreference
import com.music.vivi.ui.component.PlayerSliderTrack
import com.music.vivi.ui.component.PreferenceEntry
import com.music.vivi.ui.component.PreferenceGroupTitle
import com.music.vivi.ui.component.SwitchPreference
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import me.saket.squiggles.SquigglySlider
import kotlin.math.roundToInt


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.music.vivi.update.experiment.SheetDragHandle
import com.music.vivi.update.mordernswitch.ModernSwitch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    val (contentLanguage, onContentLanguageChange) = rememberPreference(key = ContentLanguageKey, defaultValue = "system")
    val (contentCountry, onContentCountryChange) = rememberPreference(key = ContentCountryKey, defaultValue = "system")
    val (hideExplicit, onHideExplicitChange) = rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(key = ProxyEnabledKey, defaultValue = false)
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(key = ProxyTypeKey, defaultValue = Proxy.Type.HTTP)
    val (proxyUrl, onProxyUrlChange) = rememberPreference(key = ProxyUrlKey, defaultValue = "host:port")
    val (enableKugou, onEnableKugouChange) = rememberPreference(key = EnableKugouKey, defaultValue = true)
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(key = EnableLrcLibKey, defaultValue = true)
    val (preferredProvider, onPreferredProviderChange) = rememberEnumPreference(
        key = PreferredLyricsProviderKey,
        defaultValue = PreferredLyricsProvider.LRCLIB,
    )
    val (lyricsRomanizeJapanese, onLyricsRomanizeJapaneseChange) = rememberPreference(LyricsRomanizeJapaneseKey, defaultValue = true)
    val (lyricsRomanizeKorean, onLyricsRomanizeKoreanChange) = rememberPreference(LyricsRomanizeKoreanKey, defaultValue = true)
    val (lengthTop, onLengthTopChange) = rememberPreference(key = TopSize, defaultValue = "50")
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(key = QuickPicksKey, defaultValue = QuickPicks.QUICK_PICKS)

    val scrollState = rememberScrollState()

    // Calculate scroll-based animations
    val titleAlpha by remember {
        derivedStateOf {
            1f - (scrollState.value / 200f).coerceIn(0f, 1f)
        }
    }

    val titleScale by remember {
        derivedStateOf {
            1f - (scrollState.value / 400f).coerceIn(0f, 0.3f)
        }
    }

    // Bottom sheet states
    val languageSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showLanguageSheet by remember { mutableStateOf(false) }

    val countrySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCountrySheet by remember { mutableStateOf(false) }

    val proxyTypeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showProxyTypeSheet by remember { mutableStateOf(false) }

    val proxyUrlSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showProxyUrlSheet by remember { mutableStateOf(false) }

    val lyricsProviderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showLyricsProviderSheet by remember { mutableStateOf(false) }

    val topLengthSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showTopLengthSheet by remember { mutableStateOf(false) }

    val quickPicksSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showQuickPicksSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                // Header Section with scroll animations
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .graphicsLayer {
                            alpha = titleAlpha
                            scaleX = titleScale
                            scaleY = titleScale
                        }
                ) {
                    Text(
                        text = stringResource(R.string.content),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            item {
                // Lottie Animation Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.content))
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                SettingsSection(stringResource(R.string.general)) {
                    SettingsListItem(
                        title = stringResource(R.string.content_language),
                        subtitle = LanguageCodeToName.getOrElse(contentLanguage) { stringResource(R.string.system_default) },
                        onClick = { showLanguageSheet = true },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.language),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.content_country),
                        subtitle = CountryCodeToName.getOrElse(contentCountry) { stringResource(R.string.system_default) },
                        onClick = { showCountrySheet = true },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.location_on),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.hide_explicit),
                        subtitle = if (hideExplicit) "Enabled" else "Disabled",
                        onClick = { onHideExplicitChange(!hideExplicit) },
                        isLast = true,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.explicit),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = hideExplicit,
                                onCheckedChange = onHideExplicitChange
                            )
                        }
                    )
                }
            }

            item {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    SettingsSection(stringResource(R.string.app_language)) {
                        SettingsListItem(
                            title = stringResource(R.string.app_language),
                            subtitle = "System language settings",
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APP_LOCALE_SETTINGS,
                                        "package:${context.packageName}".toUri()
                                    )
                                )
                            },
                            isLast = true,
                            leadingContent = {
                                Icon(
                                    painterResource(R.drawable.language),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.proxy)) {
                    SettingsListItem(
                        title = stringResource(R.string.enable_proxy),
                        subtitle = if (proxyEnabled) "Enabled" else "Disabled",
                        onClick = { onProxyEnabledChange(!proxyEnabled) },
                        isLast = !proxyEnabled,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.wifi_proxy),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = proxyEnabled,
                                onCheckedChange = onProxyEnabledChange
                            )
                        }
                    )

                    if (proxyEnabled) {
                        SettingsListItem(
                            title = stringResource(R.string.proxy_type),
                            subtitle = proxyType.name,
                            onClick = { showProxyTypeSheet = true },
                            isLast = false,
                            leadingContent = {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )

                        SettingsListItem(
                            title = stringResource(R.string.proxy_url),
                            subtitle = proxyUrl,
                            onClick = { showProxyUrlSheet = true },
                            isLast = true,
                            leadingContent = {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.lyrics)) {
                    SettingsListItem(
                        title = stringResource(R.string.enable_lrclib),
                        subtitle = if (enableLrclib) "Enabled" else "Disabled",
                        onClick = { onEnableLrclibChange(!enableLrclib) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.lyrics),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = enableLrclib,
                                onCheckedChange = onEnableLrclibChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.enable_kugou),
                        subtitle = if (enableKugou) "Enabled" else "Disabled",
                        onClick = { onEnableKugouChange(!enableKugou) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.lyrics),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = enableKugou,
                                onCheckedChange = onEnableKugouChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.set_first_lyrics_provider),
                        subtitle = when (preferredProvider) {
                            PreferredLyricsProvider.LRCLIB -> "LrcLib"
                            PreferredLyricsProvider.KUGOU -> "KuGou"
                        },
                        onClick = { showLyricsProviderSheet = true },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.lyrics),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.lyrics_romanize_japanese),
                        subtitle = if (lyricsRomanizeJapanese) "Enabled" else "Disabled",
                        onClick = { onLyricsRomanizeJapaneseChange(!lyricsRomanizeJapanese) },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.lyrics),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = lyricsRomanizeJapanese,
                                onCheckedChange = onLyricsRomanizeJapaneseChange
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.lyrics_romanize_korean),
                        subtitle = if (lyricsRomanizeKorean) "Enabled" else "Disabled",
                        onClick = { onLyricsRomanizeKoreanChange(!lyricsRomanizeKorean) },
                        isLast = true,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.lyrics),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            ModernSwitch(
                                checked = lyricsRomanizeKorean,
                                onCheckedChange = onLyricsRomanizeKoreanChange
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.misc)) {
                    SettingsListItem(
                        title = stringResource(R.string.top_length),
                        subtitle = "$lengthTop items",
                        onClick = { showTopLengthSheet = true },
                        isLast = false,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.trending_up),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsListItem(
                        title = stringResource(R.string.set_quick_picks),
                        subtitle = when (quickPicks) {
                            QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                            QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                        },
                        onClick = { showQuickPicksSheet = true },
                        isLast = true,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.home_outlined),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }

    // Language Selection Bottom Sheet
    if (showLanguageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            sheetState = languageSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.content_language),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.height(400.dp)) {
                    val languages = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList()
                    items(languages) { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val locale = Locale.getDefault()
                                    val languageTag = locale.toLanguageTag().replace("-Hant", "")
                                    YouTube.locale = YouTube.locale.copy(
                                        hl = language.takeIf { it != SYSTEM_DEFAULT }
                                            ?: locale.language.takeIf { it in LanguageCodeToName }
                                            ?: languageTag.takeIf { it in LanguageCodeToName }
                                            ?: "en"
                                    )
                                    onContentLanguageChange(language)
                                    showLanguageSheet = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = contentLanguage == language,
                                onClick = {
                                    val locale = Locale.getDefault()
                                    val languageTag = locale.toLanguageTag().replace("-Hant", "")
                                    YouTube.locale = YouTube.locale.copy(
                                        hl = language.takeIf { it != SYSTEM_DEFAULT }
                                            ?: locale.language.takeIf { it in LanguageCodeToName }
                                            ?: languageTag.takeIf { it in LanguageCodeToName }
                                            ?: "en"
                                    )
                                    onContentLanguageChange(language)
                                    showLanguageSheet = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = LanguageCodeToName.getOrElse(language) { stringResource(R.string.system_default) },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Country Selection Bottom Sheet
    if (showCountrySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCountrySheet = false },
            sheetState = countrySheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.content_country),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.height(400.dp)) {
                    val countries = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList()
                    items(countries) { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val locale = Locale.getDefault()
                                    YouTube.locale = YouTube.locale.copy(
                                        gl = country.takeIf { it != SYSTEM_DEFAULT }
                                            ?: locale.country.takeIf { it in CountryCodeToName }
                                            ?: "US"
                                    )
                                    onContentCountryChange(country)
                                    showCountrySheet = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = contentCountry == country,
                                onClick = {
                                    val locale = Locale.getDefault()
                                    YouTube.locale = YouTube.locale.copy(
                                        gl = country.takeIf { it != SYSTEM_DEFAULT }
                                            ?: locale.country.takeIf { it in CountryCodeToName }
                                            ?: "US"
                                    )
                                    onContentCountryChange(country)
                                    showCountrySheet = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = CountryCodeToName.getOrElse(country) { stringResource(R.string.system_default) },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Proxy Type Bottom Sheet
    if (showProxyTypeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProxyTypeSheet = false },
            sheetState = proxyTypeSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.proxy_type),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Select proxy connection type",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS).forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onProxyTypeChange(type)
                                showProxyTypeSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = proxyType == type,
                            onClick = {
                                onProxyTypeChange(type)
                                showProxyTypeSheet = false
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = type.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Proxy URL Bottom Sheet
    if (showProxyUrlSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProxyUrlSheet = false },
            sheetState = proxyUrlSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            var tempProxyUrl by remember { mutableStateOf(proxyUrl) }

            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.proxy_url),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Enter proxy URL in format host:port",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = tempProxyUrl,
                    onValueChange = { tempProxyUrl = it },
                    label = { Text("Proxy URL") },
                    placeholder = { Text("host:port") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showProxyUrlSheet = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onProxyUrlChange(tempProxyUrl)
                            showProxyUrlSheet = false
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Lyrics Provider Bottom Sheet
    if (showLyricsProviderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLyricsProviderSheet = false },
            sheetState = lyricsProviderSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.set_first_lyrics_provider),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose preferred lyrics provider",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                listOf(PreferredLyricsProvider.LRCLIB, PreferredLyricsProvider.KUGOU).forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPreferredProviderChange(provider)
                                showLyricsProviderSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preferredProvider == provider,
                            onClick = {
                                onPreferredProviderChange(provider)
                                showLyricsProviderSheet = false
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (provider) {
                                PreferredLyricsProvider.LRCLIB -> "LrcLib"
                                PreferredLyricsProvider.KUGOU -> "KuGou"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Top Length Bottom Sheet
    if (showTopLengthSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTopLengthSheet = false },
            sheetState = topLengthSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            var tempLength by remember { mutableStateOf(lengthTop) }
            val isValid = tempLength.toIntOrNull()?.let { it > 0 } == true

            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.top_length),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Number of items to show in top charts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = tempLength,
                    onValueChange = { tempLength = it },
                    label = { Text("Number of items") },
                    placeholder = { Text("50") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isValid && tempLength.isNotEmpty(),
                    supportingText = if (!isValid && tempLength.isNotEmpty()) {
                        { Text("Please enter a valid number greater than 0", color = MaterialTheme.colorScheme.error) }
                    } else null
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showTopLengthSheet = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onLengthTopChange(tempLength)
                            showTopLengthSheet = false
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Quick Picks Bottom Sheet
    if (showQuickPicksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickPicksSheet = false },
            sheetState = quickPicksSheetState,
            dragHandle = { SheetDragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    stringResource(R.string.set_quick_picks),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose what to display on home screen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                listOf(QuickPicks.QUICK_PICKS, QuickPicks.LAST_LISTEN).forEach { pick ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onQuickPicksChange(pick)
                                showQuickPicksSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = quickPicks == pick,
                            onClick = {
                                onQuickPicksChange(pick)
                                showQuickPicksSheet = false
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (pick) {
                                QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                                QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
