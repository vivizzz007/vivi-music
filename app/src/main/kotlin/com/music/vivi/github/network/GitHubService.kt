package com.music.vivi.github.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

import javax.inject.Inject

class GitHubService @Inject constructor() {

    private val client = HttpClient(OkHttp) {
        engine {
            preconfigured = OkHttpClient()
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun getAccessToken(clientId: String, clientSecret: String, code: String): String? {
        val result = client.post("https://github.com/login/oauth/access_token") {
            header("Accept", "application/json")
            contentType(ContentType.Application.Json)
            setBody(
                GitHubTokenRequest(
                    client_id = clientId,
                    client_secret = clientSecret,
                    code = code
                )
            )
        }
        
        return if (result.status == HttpStatusCode.OK) {
            val response = result.body<GitHubTokenResponse>() 
            response.access_token
        } else {
            null
        }
    }

    suspend fun isRepoStarred(token: String, owner: String = "vivizzz007", repo: String = "vivi-music"): Boolean {
        val response: HttpResponse = client.get("https://api.github.com/user/starred/$owner/$repo") {
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
        }
        return response.status == HttpStatusCode.NoContent
    }

    suspend fun starRepo(token: String, owner: String = "vivizzz007", repo: String = "vivi-music"): Boolean {
        val response = client.put("https://api.github.com/user/starred/$owner/$repo") {
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
        }
        return response.status == HttpStatusCode.NoContent
    }
    
    suspend fun unstarRepo(token: String, owner: String = "vivizzz007", repo: String = "vivi-music"): Boolean {
        val response = client.delete("https://api.github.com/user/starred/$owner/$repo") {
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
        }
        return response.status == HttpStatusCode.NoContent
    }
}

@Serializable
data class GitHubTokenRequest(
    val client_id: String,
    val client_secret: String,
    val code: String
)

@Serializable
data class GitHubTokenResponse(
    val access_token: String,
    val scope: String,
    val token_type: String
)
