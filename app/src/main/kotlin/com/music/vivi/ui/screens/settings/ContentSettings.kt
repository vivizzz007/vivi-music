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
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition



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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            // Lottie Animation in a Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.content))
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // General Settings Section
            PreferenceGroupTitle(
                title = stringResource(R.string.general),
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column {
                    ListPreference(
                        title = { Text(stringResource(R.string.content_language)) },
                        icon = { Icon(painterResource(R.drawable.language), null) },
                        selectedValue = contentLanguage,
                        values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
                        valueText = {
                            LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
                        },
                        onValueSelected = { newValue ->
                            val locale = Locale.getDefault()
                            val languageTag = locale.toLanguageTag().replace("-Hant", "")
                            YouTube.locale = YouTube.locale.copy(
                                hl = newValue.takeIf { it != SYSTEM_DEFAULT }
                                    ?: locale.language.takeIf { it in LanguageCodeToName }
                                    ?: languageTag.takeIf { it in LanguageCodeToName }
                                    ?: "en"
                            )
                            onContentLanguageChange(newValue)
                        }
                    )

                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )

                    ListPreference(
                        title = { Text(stringResource(R.string.content_country)) },
                        icon = { Icon(painterResource(R.drawable.location_on), null) },
                        selectedValue = contentCountry,
                        values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
                        valueText = {
                            CountryCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
                        },
                        onValueSelected = { newValue ->
                            val locale = Locale.getDefault()
                            YouTube.locale = YouTube.locale.copy(
                                gl = newValue.takeIf { it != SYSTEM_DEFAULT }
                                    ?: locale.country.takeIf { it in CountryCodeToName }
                                    ?: "US"
                            )
                            onContentCountryChange(newValue)
                        }
                    )

                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )

                    SwitchPreference(
                        title = { Text(stringResource(R.string.hide_explicit)) },
                        icon = { Icon(painterResource(R.drawable.explicit), null) },
                        checked = hideExplicit,
                        onCheckedChange = onHideExplicitChange,
                    )
                }
            }

            // App Language Section
            PreferenceGroupTitle(
                title = stringResource(R.string.app_language),
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.app_language)) },
                        icon = { Icon(painterResource(R.drawable.language), null) },
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APP_LOCALE_SETTINGS,
                                    "package:${context.packageName}".toUri()
                                )
                            )
                        }
                    )
                }
            }

            // Proxy Settings Section
            PreferenceGroupTitle(
                title = stringResource(R.string.proxy),
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_proxy)) },
                        icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
                        checked = proxyEnabled,
                        onCheckedChange = onProxyEnabledChange,
                    )

                    if (proxyEnabled) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            thickness = 1.dp
                        )

                        ListPreference(
                            title = { Text(stringResource(R.string.proxy_type)) },
                            selectedValue = proxyType,
                            values = listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS),
                            valueText = { it.name },
                            onValueSelected = onProxyTypeChange,
                        )

                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            thickness = 1.dp
                        )

                        EditTextPreference(
                            title = { Text(stringResource(R.string.proxy_url)) },
                            value = proxyUrl,
                            onValueChange = onProxyUrlChange,
                        )
                    }
                }
            }

            // Lyrics Settings Section
            PreferenceGroupTitle(
                title = stringResource(R.string.lyrics),
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_lrclib)) },
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = enableLrclib,
                        onCheckedChange = onEnableLrclibChange,
                    )

                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )

                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_kugou)) },
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = enableKugou,
                        onCheckedChange = onEnableKugouChange,
                    )

                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )

                    ListPreference(
                        title = { Text(stringResource(R.string.set_first_lyrics_provider)) },
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        selectedValue = preferredProvider,
                        values = listOf(PreferredLyricsProvider.LRCLIB, PreferredLyricsProvider.KUGOU),
                        valueText = {
                            when (it) {
                                PreferredLyricsProvider.LRCLIB -> "LrcLib"
                                PreferredLyricsProvider.KUGOU -> "KuGou"
                            }
                        },
                        onValueSelected = onPreferredProviderChange,
                    )

                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )

                    SwitchPreference(
                        title = { Text(stringResource(R.string.lyrics_romanize_japanese)) },
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = lyricsRomanizeJapanese,
                        onCheckedChange = onLyricsRomanizeJapaneseChange,
                    )

                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )

                    SwitchPreference(
                        title = { Text(stringResource(R.string.lyrics_romanize_korean)) },
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = lyricsRomanizeKorean,
                        onCheckedChange = onLyricsRomanizeKoreanChange,
                    )
                }
            }

            // Misc Section
            PreferenceGroupTitle(
                title = stringResource(R.string.misc),
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column {
                    EditTextPreference(
                        title = { Text(stringResource(R.string.top_length)) },
                        icon = { Icon(painterResource(R.drawable.trending_up), null) },
                        value = lengthTop,
                        isInputValid = { it.toIntOrNull()?.let { num -> num > 0 } == true },
                        onValueChange = onLengthTopChange,
                    )

                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )

                    ListPreference(
                        title = { Text(stringResource(R.string.set_quick_picks)) },
                        icon = { Icon(painterResource(R.drawable.home_outlined), null) },
                        selectedValue = quickPicks,
                        values = listOf(QuickPicks.QUICK_PICKS, QuickPicks.LAST_LISTEN),
                        valueText = {
                            when (it) {
                                QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                                QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                            }
                        },
                        onValueSelected = onQuickPicksChange,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Top App Bar
        TopAppBar(
            title = { Text(stringResource(R.string.content)) },
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
            scrollBehavior = scrollBehavior,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}