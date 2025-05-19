package com.music.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val clientId: String,
    val userAgent: String,
    val osVersion: String? = null,
    val referer: String? = null,
    val supportsLogin: Boolean = false,
    val loginSupported: Boolean = false,
    val loginRequired: Boolean = false,
    val useSignatureTimestamp: Boolean = false,
    val isEmbedded: Boolean = false,
) {
    fun toContext(locale: YouTubeLocale, visitorData: String?) = Context(
        client = Context.Client(
            clientName = clientName,
            clientVersion = clientVersion,
            osVersion = osVersion,
            gl = locale.gl,
            hl = locale.hl,
            visitorData = visitorData
        )
    )

    companion object {
        const val ORIGIN_YOUTUBE_MUSIC = "https://music.youtube.com"
        const val REFERER_YOUTUBE_MUSIC = "$ORIGIN_YOUTUBE_MUSIC/"
        const val API_URL_YOUTUBE_MUSIC = "$ORIGIN_YOUTUBE_MUSIC/youtubei/v1/"

        const val USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36"
        private const val USER_AGENT_ANDROID = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Mobile Safari/537.36"

        val ANDROID_MUSIC = YouTubeClient(
            clientName = "ANDROID_MUSIC",
            clientVersion = "5.01",
            clientId = "1",
            userAgent = USER_AGENT_ANDROID,
        )

        val WEB = YouTubeClient(
            clientName = "WEB",
            clientVersion = "2.20250312.04.00",
            clientId = "67",
            userAgent = USER_AGENT_WEB,
        )

        val WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20250310.01.00",
            clientId = "62",
            userAgent = USER_AGENT_WEB,
            referer = REFERER_YOUTUBE_MUSIC,
            supportsLogin = true,
        )

        val TVHTML5_SIMPLY_EMBEDDED_PLAYER = YouTubeClient(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            clientId = "85",
            userAgent = "Mozilla/5.0 (PlayStation 4 5.55) AppleWebKit/601.2 (KHTML, like Gecko)",
            isEmbedded = true
        )

        val IOS = YouTubeClient(
            clientName = "IOS",
            clientVersion = "20.10.4",
            clientId = "85",
            userAgent = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
            osVersion = "18.3.2.22D82",
        )
        val MOBILE = YouTubeClient(
            clientName = "ANDROID",
            clientVersion = "18.13.37",
            clientId = "3",
            userAgent = "com.google.android.youtube/18.13.37 (Linux; U; Android 13; Pixel 6)",
            loginSupported = true,
            useSignatureTimestamp = true
        )
    }
}
