package com.music.vivi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.CachePolicy
import coil3.request.allowHardware
import coil3.request.crossfade
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeLocale
import com.music.kugou.KuGou
import com.music.lastfm.LastFM
import com.music.vivi.BuildConfig
import com.music.vivi.R
import com.music.vivi.constants.*
import com.music.vivi.di.ApplicationScope
import com.music.vivi.extensions.toEnum
import com.music.vivi.extensions.toInetSocketAddress
import com.music.vivi.update.experiment.CrashLogHandler
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.utils.reportException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import timber.log.Timber
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.Proxy
import java.util.Locale
import javax.inject.Inject

/**
 * The main Application class for Vivi Music.
 *
 * This class serves as the dependency injection root (@HiltAndroidApp) and handles global initialization for:
 * - **Image Loading**: Configures Coil with caching policies.
 * - **Settings**: Loads critical preferences (Locale, Proxy, Integrations) at startup.
 * - **Notification Channels**: Creates the required channels for Android O+.
 * - **Error Handling**: Initializes global crash handlers.
 *
 * ## Usage Example
 * Accessing the application scope for global coroutines:
 * ```kotlin
 * @Inject lateinit var app: App
 * app.applicationScope.launch { ... }
 * ```
 */
@HiltAndroidApp
class App :
    Application(),
    SingletonImageLoader.Factory {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    /**
     * Initializes the application.
     *
     * - Sets up [Timber] for logging.
     * - Launches a coroutine to initialize settings ([initializeSettings]) and observe changes ([observeSettingsChanges]).
     * - This ensures that critical config (like Proxy settings) is applied before any network requests are made.
     */
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        applicationScope.launch {
            initializeSettings()
            observeSettingsChanges()
        }
    }

    private suspend fun initializeSettings() {
        val settings = dataStore.data.first()
        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "")

        YouTube.locale = YouTubeLocale(
            gl = settings[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.country.takeIf { it in CountryCodeToName }
                ?: "US",
            hl = settings[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.language.takeIf { it in LanguageCodeToName }
                ?: languageTag.takeIf { it in LanguageCodeToName }
                ?: "en"
        )

        if (languageTag == "zh-TW") {
            KuGou.useTraditionalChinese = true
        }

        // Initialize LastFM with API keys
        LastFM.initialize(BuildConfig.LASTFM_API_KEY, BuildConfig.LASTFM_SECRET)

        // Initialize crash log handler app crash
        CrashLogHandler.initialize(this)

        if (settings[ProxyEnabledKey] == true) {
            val username = settings[ProxyUsernameKey].orEmpty()
            val password = settings[ProxyPasswordKey].orEmpty()
            val type = settings[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP)

            if (username.isNotEmpty() || password.isNotEmpty()) {
                if (type == Proxy.Type.HTTP) {
                    YouTube.proxyAuth = Credentials.basic(username, password)
                } else {
                    Authenticator.setDefault(object : Authenticator() {
                        override fun getPasswordAuthentication(): PasswordAuthentication =
                            PasswordAuthentication(username, password.toCharArray())
                    })
                }
            }
            try {
                settings[ProxyUrlKey]?.let {
                    YouTube.proxy = Proxy(type, it.toInetSocketAddress())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@App, getString(R.string.failed_to_parse_proxy_url), Toast.LENGTH_SHORT).show()
                }
                reportException(e)
            }
        }

        YouTube.useLoginForBrowse = settings[UseLoginForBrowse] ?: true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            // Music playback channel (required for MusicService)
            val musicChannel = NotificationChannel(
                "music_channel_01",
                getString(R.string.music_player),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.controls_for_music_playback)
                setShowBadge(false)
            }
            nm.createNotificationChannel(musicChannel)
        }
    }

    private fun observeSettingsChanges() {
        // Consolidated observer to reduce overhead of 4 separate coroutines
        applicationScope.launch(Dispatchers.IO) {
            dataStore.data.distinctUntilChanged().collect { settings ->
                // Visitor Data
                val visitorData = settings[VisitorDataKey]
                if (visitorData != "null") {
                    YouTube.visitorData = visitorData
                        ?: YouTube.visitorData().getOrNull()?.also { newVisitorData ->
                            dataStore.edit { it[VisitorDataKey] = newVisitorData }
                        }
                }

                // Data Sync ID
                val dataSyncId = settings[DataSyncIdKey]
                YouTube.dataSyncId = dataSyncId?.let {
                    it.takeIf { !it.contains("||") }
                        ?: it.takeIf { it.endsWith("||") }?.substringBefore("||")
                        ?: it.substringAfter("||")
                }

                // InnerTube Cookie
                val cookie = settings[InnerTubeCookieKey]
                try {
                    YouTube.cookie = cookie
                } catch (e: Exception) {
                    Timber.e(e, "Could not parse cookie. Clearing existing cookie.")
                    forgetAccount(this@App)
                }

                // LastFM Session
                val session = settings[LastFMSessionKey]
                try {
                    LastFM.sessionKey = session
                } catch (e: Exception) {
                    Timber.e("Error while loading last.fm session key. %s", e.message)
                }
            }
        }
    }

    /**
     * Configures the global Coil [ImageLoader].
     *
     * The loader is configured with:
     * - **Crossfade**: Enabled for smooth transitions.
     * - **Hardware Bitmaps**: Allowed on Android P+ for performance.
     * - **Memory Cache**: Limited to 25% of available memory.
     * - **Disk Cache**: Configurable via [MaxImageCacheSizeKey], located in `cacheDir/coil`.
     *
     * @param context The platform context.
     * @return A configured [ImageLoader] instance.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val cacheSize = dataStore.get(MaxImageCacheSizeKey, 512)

        return ImageLoader.Builder(this).apply {
            crossfade(true)
            allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            memoryCache {
                coil3.memory.MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            if (cacheSize == 0) {
                diskCachePolicy(CachePolicy.DISABLED)
            } else {
                diskCache(
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("coil"))
                        .maxSizeBytes(cacheSize * 1024 * 1024L)
                        .build()
                )
            }
        }.build()
    }

    companion object {
        suspend fun forgetAccount(context: Context) {
            context.dataStore.edit { settings ->
                settings.remove(InnerTubeCookieKey)
                settings.remove(VisitorDataKey)
                settings.remove(DataSyncIdKey)
                settings.remove(AccountNameKey)
                settings.remove(AccountEmailKey)
                settings.remove(AccountChannelHandleKey)
            }
        }
    }
}
