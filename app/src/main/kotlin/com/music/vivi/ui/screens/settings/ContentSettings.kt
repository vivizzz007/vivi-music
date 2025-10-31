package com.music.vivi.ui.screens.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.music.innertube.YouTube
import com.music.vivi.R
import com.music.vivi.constants.AppLanguageKey
import com.music.vivi.constants.ContentCountryKey
import com.music.vivi.constants.ContentLanguageKey
import com.music.vivi.constants.CountryCodeToName
import com.music.vivi.constants.EnableKugouKey
import com.music.vivi.constants.EnableLrcLibKey
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.LanguageCodeToName
import com.music.vivi.constants.PreferredLyricsProvider
import com.music.vivi.constants.PreferredLyricsProviderKey
import com.music.vivi.constants.ProxyEnabledKey
import com.music.vivi.constants.ProxyPasswordKey
import com.music.vivi.constants.ProxyTypeKey
import com.music.vivi.constants.ProxyUrlKey
import com.music.vivi.constants.ProxyUsernameKey
import com.music.vivi.constants.QuickPicks
import com.music.vivi.constants.QuickPicksKey
import com.music.vivi.constants.SYSTEM_DEFAULT
import com.music.vivi.constants.TopSize
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.update.mordernswitch.ModernSwitch
import com.music.vivi.update.settingstyle.ModernInfoItem
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.utils.reportException
import com.music.vivi.utils.setAppLocale
import java.net.Proxy
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Used only before Android 13
    val (appLanguage, onAppLanguageChange) = rememberPreference(key = AppLanguageKey, defaultValue = SYSTEM_DEFAULT)

    val (contentLanguage, onContentLanguageChange) = rememberPreference(key = ContentLanguageKey, defaultValue = "system")
    val (contentCountry, onContentCountryChange) = rememberPreference(key = ContentCountryKey, defaultValue = "system")
    val (hideExplicit, onHideExplicitChange) = rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(key = ProxyEnabledKey, defaultValue = false)
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(key = ProxyTypeKey, defaultValue = Proxy.Type.HTTP)
    val (proxyUrl, onProxyUrlChange) = rememberPreference(key = ProxyUrlKey, defaultValue = "host:port")
    val (proxyUsername, onProxyUsernameChange) = rememberPreference(key = ProxyUsernameKey, defaultValue = "username")
    val (proxyPassword, onProxyPasswordChange) = rememberPreference(key = ProxyPasswordKey, defaultValue = "password")
    val (enableKugou, onEnableKugouChange) = rememberPreference(key = EnableKugouKey, defaultValue = true)
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(key = EnableLrcLibKey, defaultValue = true)
    val (preferredProvider, onPreferredProviderChange) =
        rememberEnumPreference(
            key = PreferredLyricsProviderKey,
            defaultValue = PreferredLyricsProvider.LRCLIB,
        )
    val (lengthTop, onLengthTopChange) = rememberPreference(key = TopSize, defaultValue = "50")
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(key = QuickPicksKey, defaultValue = QuickPicks.QUICK_PICKS)

    var showProxyConfigurationDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showContentLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showContentCountryDialog by rememberSaveable { mutableStateOf(false) }
    var showAppLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showPreferredProviderDialog by rememberSaveable { mutableStateOf(false) }
    var showQuickPicksDialog by rememberSaveable { mutableStateOf(false) }
    var showTopLengthDialog by rememberSaveable { mutableStateOf(false) }

    // Content Language Dialog
    if (showContentLanguageDialog) {
        DefaultDialog(
            onDismiss = { showContentLanguageDialog = false },
            content = {
                LazyColumn {
                    items(listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList()) { value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val locale = Locale.getDefault()
                                    val languageTag = locale.toLanguageTag().replace("-Hant", "")

                                    YouTube.locale = YouTube.locale.copy(
                                        hl = value.takeIf { it != SYSTEM_DEFAULT }
                                            ?: locale.language.takeIf { it in LanguageCodeToName }
                                            ?: languageTag.takeIf { it in LanguageCodeToName }
                                            ?: "en"
                                    )

                                    onContentLanguageChange(value)
                                    showContentLanguageDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = value == contentLanguage,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(LanguageCodeToName.getOrElse(value) { stringResource(R.string.system_default) })
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showContentLanguageDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Content Country Dialog
    if (showContentCountryDialog) {
        DefaultDialog(
            onDismiss = { showContentCountryDialog = false },
            content = {
                LazyColumn {
                    items(listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList()) { value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val locale = Locale.getDefault()

                                    YouTube.locale = YouTube.locale.copy(
                                        gl = value.takeIf { it != SYSTEM_DEFAULT }
                                            ?: locale.country.takeIf { it in CountryCodeToName }
                                            ?: "US"
                                    )

                                    onContentCountryChange(value)
                                    showContentCountryDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = value == contentCountry,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(CountryCodeToName.getOrElse(value) { stringResource(R.string.system_default) })
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showContentCountryDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showAppLanguageDialog) {
        DefaultDialog(
            onDismiss = { showAppLanguageDialog = false },
            content = {
                LazyColumn {
                    items(listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList()) { value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val newLocale = value
                                            .takeUnless { it == SYSTEM_DEFAULT }
                                            ?.let { tag ->
                                                if (tag.isNotEmpty()) Locale.forLanguageTag(tag) else null
                                            }
                                            ?: Locale.getDefault()

                                        if (newLocale != null && newLocale.language.isNotEmpty()) {
                                            onAppLanguageChange(value)
                                            setAppLocale(context, newLocale)
                                        }
                                    } catch (e: Exception) {
                                        reportException(e)
                                    }
                                    showAppLanguageDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = value == appLanguage,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(LanguageCodeToName.getOrElse(value) { stringResource(R.string.system_default) })
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showAppLanguageDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Preferred Provider Dialog
    if (showPreferredProviderDialog) {
        DefaultDialog(
            onDismiss = { showPreferredProviderDialog = false },
            content = {
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    listOf(PreferredLyricsProvider.LRCLIB, PreferredLyricsProvider.KUGOU).forEach { value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPreferredProviderChange(value)
                                    showPreferredProviderDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = value == preferredProvider,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                when (value) {
                                    PreferredLyricsProvider.LRCLIB -> "LrcLib"
                                    PreferredLyricsProvider.KUGOU -> "KuGou"
                                }
                            )
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showPreferredProviderDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Quick Picks Dialog
    if (showQuickPicksDialog) {
        DefaultDialog(
            onDismiss = { showQuickPicksDialog = false },
            content = {
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    listOf(QuickPicks.QUICK_PICKS, QuickPicks.LAST_LISTEN).forEach { value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onQuickPicksChange(value)
                                    showQuickPicksDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = value == quickPicks,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                when (value) {
                                    QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                                    QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                                }
                            )
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showQuickPicksDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Top Length Dialog
    if (showTopLengthDialog) {
        var textValue by remember { mutableStateOf(lengthTop) }

        DefaultDialog(
            onDismiss = { showTopLengthDialog = false },
            content = {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(stringResource(R.string.top_length)) },
                    placeholder = { Text("50") },
                    isError = textValue.toIntOrNull()?.let { it <= 0 } ?: true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(onClick = { showTopLengthDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        if (textValue.toIntOrNull()?.let { it > 0 } == true) {
                            onLengthTopChange(textValue)
                            showTopLengthDialog = false
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    if (showProxyConfigurationDialog) {
        var expandedDropdown by remember { mutableStateOf(false) }

        var tempProxyUrl by rememberSaveable { mutableStateOf(proxyUrl) }
        var tempProxyUsername by rememberSaveable { mutableStateOf(proxyUsername) }
        var tempProxyPassword by rememberSaveable { mutableStateOf(proxyPassword) }
        var authEnabled by rememberSaveable { mutableStateOf(proxyUsername.isNotBlank() || proxyPassword.isNotBlank()) }

        AlertDialog(
            onDismissRequest = { showProxyConfigurationDialog = false },
            title = {
                Text(stringResource(R.string.config_proxy))
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = proxyType.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.proxy_type)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS).forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        onProxyTypeChange(type)
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tempProxyUrl,
                        onValueChange = { tempProxyUrl = it },
                        label = { Text(stringResource(R.string.proxy_url)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.enable_authentication))
                        Switch(
                            checked = authEnabled,
                            onCheckedChange = {
                                authEnabled = it
                                if (!it) {
                                    tempProxyUsername = ""
                                    tempProxyPassword = ""
                                }
                            }
                        )
                    }

                    AnimatedVisibility(visible = authEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = tempProxyUsername,
                                onValueChange = { tempProxyUsername = it },
                                label = { Text(stringResource(R.string.proxy_username)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = tempProxyPassword,
                                onValueChange = { tempProxyPassword = it },
                                label = { Text(stringResource(R.string.proxy_password)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onProxyUrlChange(tempProxyUrl)
                        onProxyUsernameChange(if (authEnabled) tempProxyUsername else "")
                        onProxyPasswordChange(if (authEnabled) tempProxyPassword else "")
                        showProxyConfigurationDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showProxyConfigurationDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    val scrollState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(
                            onClick = navController::navigateUp,
                            onLongClick = navController::backToMain
                        ) {
                            Icon(
                                painterResource(R.drawable.arrow_back),
                                contentDescription = null
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 32.dp
                )
            ) {
                item {
                    Spacer(modifier = Modifier.height(25.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.content),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Configure content preferences",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // General Section
                item {
                    Text(
                        text = stringResource(R.string.general).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.language), null, modifier = Modifier.size(22.dp)) },
                                title = stringResource(R.string.content_language),
                                subtitle = LanguageCodeToName.getOrElse(contentLanguage) { stringResource(R.string.system_default) },
                                onClick = { showContentLanguageDialog = true },
                                showArrow = true,
                                showSettingsIcon = true
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.location_on), null, modifier = Modifier.size(22.dp)) },
                                title = stringResource(R.string.content_country),
                                subtitle = CountryCodeToName.getOrElse(contentCountry) { stringResource(R.string.system_default) },
                                onClick = { showContentCountryDialog = true },
                                showArrow = true,
                                showSettingsIcon = true
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.explicit), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.hide_explicit),
                                        subtitle = "Filter explicit content"
                                    )
                                }
                                ModernSwitch(
                                    checked = hideExplicit,
                                    onCheckedChange = onHideExplicitChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }
                        }
                    }
                }

                // App Language Section
                item {
                    Text(
                        text = stringResource(R.string.app_language).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                ModernInfoItem(
                                    icon = { Icon(painterResource(R.drawable.language), null, modifier = Modifier.size(22.dp)) },
                                    title = stringResource(R.string.app_language),
                                    subtitle = "Configure in system settings",
                                    onClick = {
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_APP_LOCALE_SETTINGS,
                                                "package:${context.packageName}".toUri()
                                            )
                                        )
                                    },
                                    showArrow = true,
                                    showSettingsIcon = true
                                )
                            } else {
                                ModernInfoItem(
                                    icon = { Icon(painterResource(R.drawable.language), null, modifier = Modifier.size(22.dp)) },
                                    title = stringResource(R.string.app_language),
                                    subtitle = LanguageCodeToName.getOrElse(appLanguage) { stringResource(R.string.system_default) },
                                    onClick = { showAppLanguageDialog = true },
                                    showArrow = true,
                                    showSettingsIcon = true
                                )
                            }
                        }
                    }
                }

                // Proxy Section
                item {
                    Text(
                        text = stringResource(R.string.proxy).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.wifi_proxy), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.enable_proxy),
                                        subtitle = "Route traffic through proxy"
                                    )
                                }
                                ModernSwitch(
                                    checked = proxyEnabled,
                                    onCheckedChange = onProxyEnabledChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            if (proxyEnabled) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )

                                ModernInfoItem(
                                    icon = { Icon(painterResource(R.drawable.settings), null, modifier = Modifier.size(22.dp)) },
                                    title = stringResource(R.string.config_proxy),
                                    subtitle = "Configure proxy settings",
                                    onClick = { showProxyConfigurationDialog = true },
                                    showArrow = true,
                                    showSettingsIcon = true
                                )
                            }
                        }
                    }
                }

                // Lyrics Section
                item {
                    Text(
                        text = stringResource(R.string.lyrics).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.lyrics), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.enable_lrclib),
                                        subtitle = "LrcLib lyrics provider"
                                    )
                                }
                                ModernSwitch(
                                    checked = enableLrclib,
                                    onCheckedChange = onEnableLrclibChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernInfoItem(
                                        icon = { Icon(painterResource(R.drawable.lyrics), null, modifier = Modifier.size(22.dp)) },
                                        title = stringResource(R.string.enable_kugou),
                                        subtitle = "KuGou lyrics provider"
                                    )
                                }
                                ModernSwitch(
                                    checked = enableKugou,
                                    onCheckedChange = onEnableKugouChange,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.lyrics), null, modifier = Modifier.size(22.dp)) },
                                title = stringResource(R.string.set_first_lyrics_provider),
                                subtitle = when (preferredProvider) {
                                    PreferredLyricsProvider.LRCLIB -> "LrcLib"
                                    PreferredLyricsProvider.KUGOU -> "KuGou"
                                },
                                onClick = { showPreferredProviderDialog = true },
                                showArrow = true,
                                showSettingsIcon = true
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.language_korean_latin), null, modifier = Modifier.size(22.dp)) },
                                title = stringResource(R.string.lyrics_romanization),
                                subtitle = "Romanization settings",
                                onClick = { navController.navigate("settings/content/romanization") },
                                showArrow = true,
                                showSettingsIcon = true
                            )
                        }
                    }
                }

                // Misc Section
                item {
                    Text(
                        text = stringResource(R.string.misc).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.trending_up), null, modifier = Modifier.size(22.dp)) },
                                title = stringResource(R.string.top_length),
                                subtitle = "$lengthTop items",
                                onClick = { showTopLengthDialog = true },
                                showArrow = true,
                                showSettingsIcon = true
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            ModernInfoItem(
                                icon = { Icon(painterResource(R.drawable.home_outlined), null, modifier = Modifier.size(22.dp)) },
                                title = stringResource(R.string.set_quick_picks),
                                subtitle = when (quickPicks) {
                                    QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                                    QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                                },
                                onClick = { showQuickPicksDialog = true },
                                showArrow = true,
                                showSettingsIcon = true
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}