package com.music.vivi.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class NewsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _newsItems = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsItems: StateFlow<List<NewsItem>> = _newsItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchNews()
    }

    fun fetchNews(isRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isRefresh) _isRefreshing.value = true else _isLoading.value = true
            _error.value = null
            try {
                // 1. Fetch file list from the 'news' folder
                // IMPORTANT: Use 'ref=main' to ensure we get the latest from main branch
                val folderUrl = "https://api.github.com/repos/vivizzz007/vivi-music/contents/news?ref=main"
                // Use ETag for the folder listing to prevent rate limits
                val folderJson = fetchUrlContent(folderUrl, useETag = true)
                
                val filesArray = JSONArray(folderJson)
                val items = mutableListOf<NewsItem>()

                // 2. Iterate through files and fetch JSON content
                for (i in 0 until filesArray.length()) {
                    val fileObj = filesArray.getJSONObject(i)
                    val name = fileObj.getString("name")
                    val downloadUrl = fileObj.getString("download_url")

                    if (name.endsWith(".json", ignoreCase = true)) {
                        try {
                            // Raw files can be fetched freshly (using timestamp) or cached.
                            // Since we only fetch them if the folder listing changed (or we refreshed), 
                            // we can keep using force-refresh for individual files to ensure immediate updates if the main listing refreshed.
                            // OR we can use ETag here too for maximum efficiency. Let's use ETag here too.
                            val contentJson = fetchUrlContent(downloadUrl, useETag = true)
                            val item = parseNewsItem(contentJson)
                            items.add(item)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Skip malformed files
                        }
                    }
                }

                // Sort by date descending
                 items.sortByDescending { it.date }

                withContext(Dispatchers.Main) {
                   _newsItems.value = items
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _error.value = "${e.message} (Check internet or try again later)"
                }
            } finally {
                withContext(Dispatchers.Main) {
                   if (isRefresh) _isRefreshing.value = false else _isLoading.value = false
                }
            }
        }
    }

    private fun fetchUrlContent(urlString: String, useETag: Boolean = false): String {
        // Use SharedPreferences for ETag storage
        val prefs = context.getSharedPreferences("news_cache", Context.MODE_PRIVATE)
        val etagKey = "etag_${urlString.hashCode()}"
        val contentKey = "content_${urlString.hashCode()}"
        
        val cachedEtag = if (useETag) prefs.getString(etagKey, null) else null
        
        // Append timestamp only if NOT using ETag (to force fresh)
        // If using ETag, we want the server to check against the ETag, not a unique URL
        val finalUrl = if (!useETag) {
            val separator = if (urlString.contains("?")) "&" else "?"
            "$urlString${separator}t=${System.currentTimeMillis()}"
        } else {
            urlString
        }
        
        val url = URL(finalUrl)
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "ViviMusic-App")
            setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            if (useETag && cachedEtag != null) {
                setRequestProperty("If-None-Match", cachedEtag)
            } else {
                // If not using ETag, force no-cache
                useCaches = false
                defaultUseCaches = false
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Expires", "0")
            }
            
            connect()
            
            if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED && useETag) {
                // Return cached content
                return prefs.getString(contentKey, "") ?: ""
            }
            
            if (responseCode !in 200..299) {
                 throw Exception("HTTP $responseCode: $responseMessage")
            }
            
            val content = inputStream.bufferedReader().use { it.readText() }
            
            // Save ETag and Content only if successful and using ETag strategy
            if (useETag) {
                val newEtag = getHeaderField("ETag")
                if (newEtag != null) {
                    with(prefs.edit()) {
                        putString(etagKey, newEtag)
                        putString(contentKey, content)
                        apply()
                    }
                }
            }
            
            return content
        }
    }

    private fun parseNewsItem(json: String): NewsItem {
        val obj = JSONObject(json)
        
        val reportedBugs = if (obj.has("reported_bugs")) {
            val arr = obj.getJSONArray("reported_bugs")
            List(arr.length()) { arr.getString(it) }
        } else null

        val fixedBugs = if (obj.has("fixed_bugs")) {
            val arr = obj.getJSONArray("fixed_bugs")
            List(arr.length()) { arr.getString(it) }
        } else null
        
        val blocks = if (obj.has("blocks")) {
            val arr = obj.getJSONArray("blocks")
            List(arr.length()) { 
                val blockObj = arr.getJSONObject(it)
                ContentBlock(
                    subtitle = blockObj.optString("subtitle", "").takeIf { s -> s.isNotEmpty() },
                    content = blockObj.optString("content", "").takeIf { c -> c.isNotEmpty() },
                    image = blockObj.optString("image", "").takeIf { img -> img.isNotEmpty() }
                )
            }
        } else emptyList()

        return NewsItem(
            title = obj.optString("title", "No Title"),
            subtitle = obj.optString("subtitle", "").takeIf { it.isNotEmpty() },
            date = obj.optString("date", ""),
            type = obj.optString("type", "news"), // unknown, news, bug_report, fix, feature
            description = obj.optString("description", ""),
            image = obj.optString("image", "").takeIf { it.isNotEmpty() },
            version = obj.optString("version", "").takeIf { it.isNotEmpty() },
            content = obj.optString("content", ""), // Full content
            reportedBugs = reportedBugs,
            fixedBugs = fixedBugs,
            blocks = blocks
        )
    }
}

data class NewsItem(
    val title: String,
    val subtitle: String?,
    val date: String,
    val type: String,
    val description: String,
    val image: String?,
    val version: String?,
    val content: String,
    val reportedBugs: List<String>?,
    val fixedBugs: List<String>?,
    val blocks: List<ContentBlock>
)

data class ContentBlock(
    val subtitle: String?,
    val content: String?,
    val image: String?
)
