package com.music.innertube

import com.music.innertube.models.response.PlayerResponse
import com.music.innertube.models.IpVersion
import com.music.innertube.models.YouTubeClient
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.CancellableCall
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

class NewPipeDownloaderImpl : Downloader() {

    private fun playbackNetworkClient(): OkHttpClient = OkHttpClient.Builder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = Dns.SYSTEM.lookup(hostname)
                return when (YouTube.ipVersion) {
                    IpVersion.IPV4 -> addresses.filterIsInstance<Inet4Address>().ifEmpty { addresses }
                    IpVersion.IPV6 -> addresses.filterIsInstance<Inet6Address>().ifEmpty { addresses }
                    IpVersion.AUTO -> addresses
                }
            }
        })
        .proxy(YouTube.proxy)
        .proxyAuthenticator { _, response ->
            YouTube.proxyAuth?.let {
                response.request.newBuilder().header("Proxy-Authorization", it).build()
            } ?: response.request
        }
        .build()

    private fun buildOkHttpRequest(request: Request): okhttp3.Request {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder =
            okhttp3.Request
                .Builder()
                .method(httpMethod, dataToSend?.toRequestBody())
                .url(url)
                .addHeader("User-Agent", YouTubeClient.USER_AGENT_WEB)

        headers.forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        return requestBuilder.build()
    }

    private fun toNewPipeResponse(response: okhttp3.Response): Response {
        val responseBodyToReturn = response.body?.string() ?: ""
        val latestUrl = response.request.url.toString()
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseBodyToReturn,
            responseBodyToReturn.toByteArray(),
            latestUrl
        )
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val url = request.url()
        val response = playbackNetworkClient().newCall(buildOkHttpRequest(request)).execute()

        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        return toNewPipeResponse(response)
    }

    override fun executeAsync(
        request: Request,
        callback: Downloader.AsyncCallback
    ): CancellableCall {
        val url = request.url()

        val call = playbackNetworkClient().newCall(buildOkHttpRequest(request))
        call.enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                try {
                    if (response.code == 429) {
                        response.close()
                        callback.onError(ReCaptchaException("reCaptcha Challenge requested", url))
                        return
                    }
                    callback.onSuccess(toNewPipeResponse(response))
                } catch (e: Exception) {
                    callback.onError(e)
                }
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback.onError(e)
            }
        })
        return CancellableCall(call)
    }
}

object NewPipeExtractor {
    private var newPipeDownloader: NewPipeDownloaderImpl? = null
    private var isInitialized = false

    fun init() {
        if (!isInitialized) {
            newPipeDownloader = NewPipeDownloaderImpl()
            NewPipe.init(newPipeDownloader)
            isInitialized = true
        }
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> {
        init()
        return runCatching {
            YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
        }
    }

    fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        init()
        val signatureCipher = format.signatureCipher ?: format.cipher

        if (signatureCipher.isNullOrEmpty()) {
            val directUrl = format.url ?: return null
            return applyNTransformOrNull(videoId, directUrl)
        }

        val params = YouTubeExtractor.parseQueryParams(signatureCipher)
        val obfuscatedSig = params["s"] ?: return null
        val sigParam = params["sp"] ?: "signature"
        val baseUrl = params["url"] ?: return null

        val decryptedSig = runCatching {
            YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, obfuscatedSig)
        }.getOrNull()

        val sigResult = if (decryptedSig != null) {
            val separator = if ("?" in baseUrl) "&" else "?"
            "$baseUrl$separator$sigParam=${java.net.URLEncoder.encode(decryptedSig, "UTF-8")}"
        } else {

            YouTubeExtractor.decryptUrl(signatureCipher).takeIf { it.isNotEmpty() }
        }

        val resultSigValue = sigResult?.let { com.music.innertube.YouTubeExtractor.parseQueryParams(it)[sigParam] }
        if (sigResult == null || resultSigValue == null || resultSigValue == obfuscatedSig) {

            return null
        }

        return applyNTransformOrNull(videoId, sigResult)
    }

    private fun applyNTransformOrNull(videoId: String, url: String): String? {
        val result = runCatching {
            YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
        }.getOrElse {

            runCatching { YouTubeExtractor.deobfuscateUrlNParam(url) }.getOrDefault(url)
        }
        if (nParamUnchanged(url, result)) {

            return null
        }
        return result
    }

    private fun extractNParam(url: String): String? =
        Regex("[?&]n=([^&]+)").find(url)?.groupValues?.get(1)

    private fun nParamUnchanged(before: String, after: String): Boolean {
        val n1 = extractNParam(before) ?: return false
        val n2 = extractNParam(after) ?: return false
        return n1 == n2
    }

    fun newPipePlayer(videoId: String): List<Pair<Int, String>> {
        init()
        return try {
            val streamInfo = StreamInfo.getInfo(
                NewPipe.getService(0),
                "https://www.youtube.com/watch?v=$videoId"
            )
            val streamsList = streamInfo.audioStreams + streamInfo.videoStreams + streamInfo.videoOnlyStreams
            streamsList.mapNotNull {
                (it.itagItem?.id ?: return@mapNotNull null) to it.content
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun enrichWithNewPipe(videoId: String, response: PlayerResponse): PlayerResponse? {
        if (response.playabilityStatus?.status != "OK") return null
        val streams = newPipePlayer(videoId)
        if (streams.isEmpty()) return null

        return response.copy(
            streamingData = response.streamingData?.copy(
                formats = response.streamingData.formats?.map { format ->
                    format.copy(url = streams.find { it.first == format.itag }?.second ?: format.url)
                },
                adaptiveFormats = response.streamingData.adaptiveFormats.map { format ->
                    format.copy(url = streams.find { it.first == format.itag }?.second ?: format.url)
                },
            ),
        )
    }
}
