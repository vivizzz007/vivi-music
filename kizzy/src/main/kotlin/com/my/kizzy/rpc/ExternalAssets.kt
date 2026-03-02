package com.my.kizzy.rpc

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ExternalAssetResponse(
    val url: String? = null,
    @SerialName("external_asset_path")
    val externalAssetPath: String? = null,
)

suspend fun fetchExternalAsset(
    client: HttpClient,
    applicationId: String,
    token: String,
    imageUrl: String,
    userAgent: String? = null,
    superPropertiesBase64: String? = null,
): String? {
    if (imageUrl.startsWith("mp:")) return imageUrl
    val api = "https://discord.com/api/v9/applications/$applicationId/external-assets"
    return runCatching {
        val response = client.post(api) {
            header("Authorization", token)
            header("User-Agent", userAgent ?: "Discord-Android/314013;RNA")
            if (superPropertiesBase64 != null) header("X-Super-Properties", superPropertiesBase64)
            header("Content-Type", "application/json")
            setBody("{\"urls\":[\"$imageUrl\"]}")
        }
        val text = response.body<String>()
        val json = Json { ignoreUnknownKeys = true }
        val list = json.decodeFromString<List<ExternalAssetResponse>>(text)
        list.firstOrNull()?.externalAssetPath?.let { "mp:$it" }
    }.getOrNull()
}
