/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.jiosaavn.crypto

import android.util.Base64
import android.util.Log
import com.music.jiosaavn.models.SaavnDownloadUrl
import com.music.jiosaavn.models.SaavnImage
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object SaavnCrypto {

    private const val TAG = "SaavnCrypto"
    private const val DES_KEY = "38346591" // DES 8-byte key

    /**
     * Decrypt the encrypted media URL string returned by JioSaavn using DES-ECB.
     */
    fun decryptUrl(encryptedUrl: String): String {
        if (encryptedUrl.isBlank()) return ""
        Log.d(TAG, "decryptUrl: encryptedUrl length = ${encryptedUrl.length}")
        return try {
            val secretKey = SecretKeySpec(DES_KEY.toByteArray(Charsets.UTF_8), "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decodedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val decryptedUrl = String(decryptedBytes, Charsets.UTF_8).trim()
            Log.d(TAG, "decryptUrl: successfully decrypted: $decryptedUrl")
            decryptedUrl
        } catch (e: Exception) {
            Log.e(TAG, "JioSaavn URL decryption failed", e)
            ""
        }
    }

    /**
     * Decode and build direct CDN download URLs for different audio bitrates.
     */
    fun createDownloadLinks(encryptedUrl: String): List<SaavnDownloadUrl> {
        Log.d(TAG, "createDownloadLinks: starting decryption for URL...")
        val decryptedUrl = decryptUrl(encryptedUrl)
        if (decryptedUrl.isBlank()) {
            Log.w(TAG, "createDownloadLinks: decryptedUrl is blank!")
            return emptyList()
        }

        val qualities = listOf(
            Pair("_96", "96kbps"),
            Pair("_160", "160kbps"),
            Pair("_320", "320kbps")
        )

        val suffixRegex = Regex("_(48|96|160|320)\\.(mp4|aac|mp3)$")
        val generatedUrls = qualities.map { (suffix, bitrate) ->
            val url = if (decryptedUrl.contains(suffixRegex)) {
                decryptedUrl.replace(suffixRegex) { match ->
                    "${suffix}.${match.groupValues[2]}"
                }
            } else {
                decryptedUrl.replace("_96", suffix)
            }
            SaavnDownloadUrl(quality = bitrate, url = url)
        }
        Log.d(TAG, "createDownloadLinks: generated URLs: ${generatedUrls.map { "${it.quality} -> ${it.url}" }}")
        return generatedUrls
    }

    /**
     * Clean and generate higher quality image URLs matching typical grid sizes.
     */
    fun createImageLinks(link: String): List<SaavnImage> {
        if (link.isBlank()) return emptyList()
        val qualities = listOf("50x50", "150x150", "500x500")
        val qualityRegex = Regex("150x150|50x50")
        val protocolRegex = Regex("^http://")

        return qualities.map { quality ->
            val url = link.replace(qualityRegex, quality).replace(protocolRegex, "https://")
            SaavnImage(quality = quality, url = url)
        }
    }
}

