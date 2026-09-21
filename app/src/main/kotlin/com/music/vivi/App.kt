/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import android.net.Uri
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.key.Keyer
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.Options
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.asImage
import coil3.decode.DataSource
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Environment
import com.music.innertube.YouTube
import com.music.innertube.models.IpVersion
import com.music.innertube.models.YouTubeLocale
import com.music.kugou.KuGou
import com.music.lastfm.LastFM
import com.music.vivi.constants.*
import com.music.vivi.vivimusic.release.NewReleaseCheckWorker
import com.music.vivi.di.ApplicationScope
import com.music.vivi.extensions.toEnum
import com.music.vivi.extensions.toInetSocketAddress
import com.music.vivi.utils.CrashHandler
import com.music.vivi.utils.InnerTubeXPlayer
import com.music.vivi.utils.ViviPrefCache
import com.music.vivi.utils.cipher.CipherDeobfuscator
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.normalizeDataSyncId
import com.music.vivi.utils.reportException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Interceptor as OkHttpInterceptor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import timber.log.Timber
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.Proxy
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), SingletonImageLoader.Factory {


    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        context = this

        // Start preferences cache immediately
        ViviPrefCache.start(this)

        // Install crash handler first
        CrashHandler.install(this)

        // Initialize InnerTubeX stream extractor
        InnerTubeXPlayer.initialize(this)

        // Initialize cipher deobfuscator for WEB_REMIX streaming
        CipherDeobfuscator.initialize(this)

        Timber.plant(Timber.DebugTree())

        // تهيئة إعدادات التطبيق عند الإقلاع
        applicationScope.launch {
            initializeSettings()
            observeSettingsChanges()
        }
    }

    private suspend fun initializeSettings() {
        val settings = dataStore.data.first()
        val locale = Locale.getDefault()
        val languageTag = locale.language

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

        // Initialize LastFM with API keys from BuildConfig (GitHub Secrets)
        LastFM.initialize(
            apiKey = BuildConfig.LASTFM_API_KEY.takeIf { it.isNotEmpty() } ?: "",
            secret = BuildConfig.LASTFM_SECRET.takeIf { it.isNotEmpty() } ?: ""
        )

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
                    Toast.makeText(this@App, getString(R.string.failed_to_parse_proxy), Toast.LENGTH_SHORT).show()
                }
                reportException(e)
            }
        }

        YouTube.useLoginForBrowse = settings[UseLoginForBrowse] ?: true
        YouTube.ipVersion = settings[IpVersionKey]?.toEnum(defaultValue = IpVersion.IPV4) ?: IpVersion.IPV4

        val channel = NotificationChannel(
            "updates",
            getString(R.string.update_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.update_channel_desc)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun observeSettingsChanges() {
        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { visitorData ->
                    YouTube.visitorData = visitorData?.takeIf { it != "null" }
                        ?: YouTube.visitorData().getOrNull()?.also { newVisitorData ->
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newVisitorData
                            }
                        }
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[DataSyncIdKey] }
                .distinctUntilChanged()
                .collect { dataSyncId ->
                    YouTube.dataSyncId = normalizeDataSyncId(dataSyncId)
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    try {
                        YouTube.cookie = cookie
                    } catch (e: Exception) {
                        Timber.e(e, "Could not parse cookie. Clearing existing cookie.")
                        forgetAccount(this@App)
                    }
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[LastFMSessionKey] }
                .distinctUntilChanged()
                .collect { session ->
                    try {
                        LastFM.sessionKey = session
                    } catch (e: Exception) {
                        Timber.e("Error while loading last.fm session key. %s", e.message)
                    }
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { Triple(it[ContentCountryKey], it[ContentLanguageKey], it[AppLanguageKey]) }
                .distinctUntilChanged()
                .collect { (contentCountry, contentLanguage, appLanguage) ->
                    val systemLocale = Locale.getDefault()
                    val effectiveAppLocale = appLanguage
                        ?.takeUnless { it == SYSTEM_DEFAULT }
                        ?.let { Locale.forLanguageTag(it) }
                        ?: systemLocale

                    YouTube.locale = YouTubeLocale(
                        gl = contentCountry?.takeIf { it != SYSTEM_DEFAULT }
                            ?: effectiveAppLocale.country.takeIf { it in CountryCodeToName }
                            ?: systemLocale.country.takeIf { it in CountryCodeToName }
                            ?: "US",
                        hl = contentLanguage?.takeIf { it != SYSTEM_DEFAULT }
                            ?: effectiveAppLocale.toLanguageTag().takeIf { it in LanguageCodeToName }
                            ?: effectiveAppLocale.language.takeIf { it in LanguageCodeToName }
                            ?: "en"
                    )
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[IpVersionKey] }
                .distinctUntilChanged()
                .collect { ipVersion ->
                    YouTube.ipVersion = ipVersion?.toEnum(defaultValue = IpVersion.IPV4) ?: IpVersion.IPV4
                }
        }

        // One-time migration: clear stale "seen releases" baseline from the buggy first run
        // so the worker re-snapshots all artists correctly on next launch.
        val migrationPrefs = getSharedPreferences("app_migrations", Context.MODE_PRIVATE)
        val NEW_RELEASE_MIGRATION_V1 = "new_release_seen_reset_v1"
        if (!migrationPrefs.getBoolean(NEW_RELEASE_MIGRATION_V1, false)) {
            NewReleaseCheckWorker.clearSeenReleases(this)
            migrationPrefs.edit().putBoolean(NEW_RELEASE_MIGRATION_V1, true).apply()
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map {
                    val bookmarkedEnabled = it[NewReleaseNotificationsKey] ?: true
                    val tasteBasedEnabled = it[TasteBasedReleaseNotificationsKey] ?: false
                    bookmarkedEnabled || tasteBasedEnabled
                }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) {
                        NewReleaseCheckWorker.schedule(this@App)
                    } else {
                        NewReleaseCheckWorker.cancel(this@App)
                    }
                }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val cacheSize = runBlocking {
            dataStore.data.map { it[MaxImageCacheSizeKey] ?: 512 }.first()
        }
        return ImageLoader.Builder(this).apply {
            crossfade(true)
            allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            components {
                add(ThumbnailDiskCacheInterceptor())
                add(StringThumbnailKeyer())
                add(UriThumbnailKeyer())
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            val okHttpCacheDir = cacheDir.resolve("okhttp_image_cache")
                            val okHttpCache = Cache(
                                directory = okHttpCacheDir,
                                maxSize = (cacheSize * 1024 * 1024L).coerceAtLeast(64 * 1024 * 1024L)
                            )
                            OkHttpClient.Builder()
                                .cache(okHttpCache)
                                .addInterceptor(OkHttpInterceptor { chain ->
                                    var request = chain.request()
                                    if (!isInternetConnected()) {
                                        request = request.newBuilder()
                                            .header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 365 * 10)
                                            .build()
                                    }
                                    try {
                                        chain.proceed(request)
                                    } catch (e: Exception) {
                                        if (!request.cacheControl.onlyIfCached) {
                                            val fallbackRequest = request.newBuilder()
                                                .header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 365 * 10)
                                                .build()
                                            try {
                                                chain.proceed(fallbackRequest)
                                            } catch (_: Exception) {
                                                throw e
                                            }
                                        } else {
                                            throw e
                                        }
                                    }
                                })
                                .addNetworkInterceptor(OkHttpInterceptor { chain ->
                                    val response = chain.proceed(chain.request())
                                    response.newBuilder()
                                        .removeHeader("Pragma")
                                        .removeHeader("Cache-Control")
                                        .header("Cache-Control", "public, max-age=" + 60 * 60 * 24 * 365 * 10)
                                        .build()
                                })
                                .build()
                        }
                    )
                )
            }
            // Memory cache for fast image loading (prevents network requests on recomposition)
            memoryCache {
                MemoryCache.Builder()
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

    private fun isInternetConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        lateinit var context: Context
            private set

        suspend fun forgetAccount(context: Context) {
            Timber.d("forgetAccount: Starting logout process")

            // Clear DataStore preferences
            Timber.d("forgetAccount: Clearing DataStore preferences")
            context.dataStore.edit { settings ->
                settings.remove(InnerTubeCookieKey)
                settings.remove(VisitorDataKey)
                settings.remove(DataSyncIdKey)
                settings.remove(AccountNameKey)
                settings.remove(AccountEmailKey)
                settings.remove(AccountChannelHandleKey)
            }
            Timber.d("forgetAccount: DataStore preferences cleared")

            // Immediately clear YouTube object's auth state
            Timber.d("forgetAccount: Clearing YouTube object auth state")
            Timber.d("forgetAccount: Before - cookie=${YouTube.cookie?.take(50)}, visitorData=${YouTube.visitorData?.take(20)}, dataSyncId=${YouTube.dataSyncId?.take(20)}")
            YouTube.cookie = null
            YouTube.visitorData = null
            YouTube.dataSyncId = null
            Timber.d("forgetAccount: After - cookie=${YouTube.cookie}, visitorData=${YouTube.visitorData}, dataSyncId=${YouTube.dataSyncId}")

            // Clear WebView cookies to prevent auto-relogin
            Timber.d("forgetAccount: Clearing WebView CookieManager")
            withContext(Dispatchers.Main) {
                android.webkit.CookieManager.getInstance().apply {
                    removeAllCookies { removed ->
                        Timber.d("forgetAccount: CookieManager.removeAllCookies callback: removed=$removed")
                    }
                    flush()
                }
            }
            Timber.d("forgetAccount: Logout process complete")
        }
    }
}

class StringThumbnailKeyer : Keyer<String> {
    override fun key(data: String, options: Options): String? {
        val isGoogleCdn = data.contains("googleusercontent.com") || data.contains("ggpht.com")
        if (isGoogleCdn) {
            return data.split(Regex("=[wshd]"), limit = 2)[0]
        }
        val ytMatch = Regex("/vi(?:_webp)?/([^/]+)/").find(data)
        if (ytMatch != null) {
            val videoId = ytMatch.groupValues[1]
            return "yt_thumb:$videoId"
        }
        return null
    }
}

class UriThumbnailKeyer : Keyer<Uri> {
    override fun key(data: Uri, options: Options): String? {
        val str = data.toString()
        val isGoogleCdn = str.contains("googleusercontent.com") || str.contains("ggpht.com")
        if (isGoogleCdn) {
            return str.split(Regex("=[wshd]"), limit = 2)[0]
        }
        val ytMatch = Regex("/vi(?:_webp)?/([^/]+)/").find(str)
        if (ytMatch != null) {
            val videoId = ytMatch.groupValues[1]
            return "yt_thumb:$videoId"
        }
        return null
    }
}

fun getCanonicalThumbnailKey(data: Any?): String? {
    val str = when (data) {
        is String -> data
        is Uri -> data.toString()
        is android.net.Uri -> data.toString()
        else -> data?.toString() ?: return null
    }
    val isGoogleCdn = str.contains("googleusercontent.com") || str.contains("ggpht.com")
    if (isGoogleCdn) {
        return str.split(Regex("=[wshd]"), limit = 2)[0]
    }
    val ytMatch = Regex("/vi(?:_webp)?/([^/]+)/").find(str)
    if (ytMatch != null) {
        val videoId = ytMatch.groupValues[1]
        return "yt_thumb:$videoId"
    }
    return null
}

class ThumbnailDiskCacheInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val canonicalKey = getCanonicalThumbnailKey(request.data)
        val modifiedRequest = if (canonicalKey != null) {
            val builder = request.newBuilder()
            if (request.diskCacheKey == null) {
                builder.diskCacheKey(canonicalKey)
            }
            if (request.memoryCacheKey == null) {
                builder.memoryCacheKey(canonicalKey)
            }
            builder.build()
        } else {
            request
        }
        val result = chain.withRequest(modifiedRequest).proceed()
        if (result is coil3.request.ErrorResult) {
            val fallbackBitmap = tryFindDownloadedArtwork(request.data)
            if (fallbackBitmap != null) {
                return coil3.request.SuccessResult(
                    image = fallbackBitmap.asImage(),
                    request = request,
                    dataSource = DataSource.DISK
                )
            }
        }
        return result
    }
}

private fun tryFindDownloadedArtwork(data: Any?): Bitmap? {
    val str = when (data) {
        is String -> data
        is Uri -> data.toString()
        is android.net.Uri -> data.toString()
        else -> data?.toString() ?: return null
    }

    val videoId = Regex("/vi(?:_webp)?/([^/?]+)").find(str)?.groupValues?.get(1)
        ?: if (str.startsWith("yt_thumb:")) str.removePrefix("yt_thumb:")
        else null

    try {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)?.resolve("ViviMusic")
        if (musicDir != null && musicDir.exists()) {
            val files = musicDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile && (file.extension.equals("m4a", true) || file.extension.equals("opus", true) || file.extension.equals("mp3", true))) {
                        val mmr = MediaMetadataRetriever()
                        try {
                            mmr.setDataSource(file.absolutePath)
                            val picture = mmr.embeddedPicture
                            if (picture != null) {
                                val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                                if (videoId == null || file.name.contains(videoId, ignoreCase = true) || (title != null && str.contains(title, ignoreCase = true))) {
                                    val bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                                    if (bitmap != null) return bitmap
                                }
                            }
                        } catch (_: Exception) {
                        } finally {
                            try { mmr.release() } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    } catch (_: Exception) {}

    return null
}


