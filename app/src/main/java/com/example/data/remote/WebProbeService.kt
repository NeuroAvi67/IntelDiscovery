package com.example.data.remote

import android.util.Log
import com.example.data.local.LocalIntegrationSecrets
import com.example.data.model.SearchProviderId
import com.example.data.model.VerificationStatus
import com.example.data.model.WebSearchSnippet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class DomainProbeResult(
    val status: VerificationStatus,
    val httpStatusCode: Int,
    val resolvedUrl: String,
    val pageTitle: String,
    val checkedAtIso: String,
    val factSummary: String
)

class WebProbeService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun probeUrl(rawUrl: String): DomainProbeResult = withContext(Dispatchers.IO) {
        val checkedAt = utcIsoNow()
        val normalizedUrl = normalizeUrl(rawUrl)
        try {
            val headRequest = Request.Builder()
                .url(normalizedUrl)
                .head()
                .header("User-Agent", "IntelAtlas-OSINT-Probe/1.0")
                .build()

            val headResponse = runCatching { client.newCall(headRequest).execute() }.getOrNull()
            headResponse?.use { resp ->
                if (resp.code in 200..399) {
                    return@withContext DomainProbeResult(
                        status = VerificationStatus.VERIFIED,
                        httpStatusCode = resp.code,
                        resolvedUrl = resp.request.url.toString(),
                        pageTitle = "",
                        checkedAtIso = checkedAt,
                        factSummary = "HTTP ${resp.code} reachable at ${resp.request.url.host} ($checkedAt)"
                    )
                }
            }

            // Fallback to lightweight GET if HEAD is blocked (403/405) or failed
            val getRequest = Request.Builder()
                .url(normalizedUrl)
                .get()
                .header("User-Agent", "IntelAtlas-OSINT-Probe/1.0")
                .header("Range", "bytes=0-16384")
                .build()

            client.newCall(getRequest).execute().use { resp ->
                val bodySnippet = runCatching { resp.body?.string()?.take(16384).orEmpty() }.getOrDefault("")
                val title = extractHtmlTitle(bodySnippet)
                val isReachable = resp.code in 200..499 && resp.code != 404 && resp.code != 410
                val status = if (isReachable) VerificationStatus.VERIFIED else VerificationStatus.UNREACHABLE
                DomainProbeResult(
                    status = status,
                    httpStatusCode = resp.code,
                    resolvedUrl = resp.request.url.toString(),
                    pageTitle = title,
                    checkedAtIso = checkedAt,
                    factSummary = "HTTP ${resp.code} checked at ${resp.request.url.host}" +
                        if (title.isNotBlank()) " • Title: $title" else ""
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Probe failed for $normalizedUrl: ${e.message}")
            DomainProbeResult(
                status = VerificationStatus.UNREACHABLE,
                httpStatusCode = 0,
                resolvedUrl = normalizedUrl,
                pageTitle = "",
                checkedAtIso = checkedAt,
                factSummary = "Probe failed (${e.javaClass.simpleName}): ${e.message?.take(120).orEmpty()}"
            )
        }
    }

    suspend fun searchConfiguredProviders(
        query: String,
        secrets: LocalIntegrationSecrets
    ): Pair<List<WebSearchSnippet>, List<String>> = withContext(Dispatchers.IO) {
        val snippets = mutableListOf<WebSearchSnippet>()
        val usedProviders = mutableListOf<String>()

        if (secrets.tavilyApiKey.isNotBlank()) {
            val res = queryTavily(query, secrets.tavilyApiKey)
            if (res.isNotEmpty()) {
                snippets.addAll(res)
                usedProviders.add("Tavily")
            }
        }
        if (secrets.braveApiKey.isNotBlank()) {
            val res = queryBrave(query, secrets.braveApiKey)
            if (res.isNotEmpty()) {
                snippets.addAll(res)
                usedProviders.add("Brave")
            }
        }
        if (secrets.serpApiKey.isNotBlank()) {
            val res = querySerpApi(query, secrets.serpApiKey)
            if (res.isNotEmpty()) {
                snippets.addAll(res)
                usedProviders.add("SerpAPI")
            }
        }
        Pair(snippets, usedProviders)
    }

    suspend fun testSearchProviderConnection(
        provider: SearchProviderId,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API key cannot be empty."))
        }
        return@withContext try {
            when (provider) {
                SearchProviderId.TAVILY -> {
                    val bodyJson = JSONObject().apply {
                        put("api_key", trimmed)
                        put("query", "osint framework")
                        put("max_results", 1)
                    }.toString()
                    val req = Request.Builder()
                        .url("https://api.tavily.com/search")
                        .post(bodyJson.toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            Result.success("Tavily API verified (HTTP ${resp.code})")
                        } else {
                            Result.failure(IllegalStateException("Tavily returned HTTP ${resp.code}"))
                        }
                    }
                }
                SearchProviderId.BRAVE -> {
                    val req = Request.Builder()
                        .url("https://api.search.brave.com/res/v1/web/search?q=osint&count=1")
                        .get()
                        .header("X-Subscription-Token", trimmed)
                        .header("Accept", "application/json")
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            Result.success("Brave Search API verified (HTTP ${resp.code})")
                        } else {
                            Result.failure(IllegalStateException("Brave Search returned HTTP ${resp.code}"))
                        }
                    }
                }
                SearchProviderId.SERPAPI -> {
                    val encodedKey = URLEncoder.encode(trimmed, "UTF-8")
                    val req = Request.Builder()
                        .url("https://serpapi.com/search.json?q=osint&num=1&api_key=$encodedKey")
                        .get()
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            Result.success("SerpAPI verified (HTTP ${resp.code})")
                        } else {
                            Result.failure(IllegalStateException("SerpAPI returned HTTP ${resp.code}"))
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryTavily(query: String, apiKey: String): List<WebSearchSnippet> {
        return try {
            val payload = JSONObject().apply {
                put("api_key", apiKey)
                put("query", query)
                put("max_results", 8)
                put("search_depth", "basic")
            }.toString()
            val req = Request.Builder()
                .url("https://api.tavily.com/search")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val json = JSONObject(resp.body?.string().orEmpty())
                val arr = json.optJSONArray("results") ?: return emptyList()
                (0 until arr.length()).mapNotNull { idx ->
                    val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                    WebSearchSnippet(
                        title = obj.optString("title"),
                        url = obj.optString("url"),
                        snippet = obj.optString("content").take(300),
                        provider = "Tavily"
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tavily search error: ${e.message}")
            emptyList()
        }
    }

    private fun queryBrave(query: String, apiKey: String): List<WebSearchSnippet> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val req = Request.Builder()
                .url("https://api.search.brave.com/res/v1/web/search?q=$encoded&count=8")
                .get()
                .header("X-Subscription-Token", apiKey)
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val json = JSONObject(resp.body?.string().orEmpty())
                val arr = json.optJSONObject("web")?.optJSONArray("results") ?: return emptyList()
                (0 until arr.length()).mapNotNull { idx ->
                    val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                    WebSearchSnippet(
                        title = obj.optString("title"),
                        url = obj.optString("url"),
                        snippet = obj.optString("description").take(300),
                        provider = "Brave"
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Brave search error: ${e.message}")
            emptyList()
        }
    }

    private fun querySerpApi(query: String, apiKey: String): List<WebSearchSnippet> {
        return try {
            val encodedQ = URLEncoder.encode(query, "UTF-8")
            val encodedKey = URLEncoder.encode(apiKey, "UTF-8")
            val req = Request.Builder()
                .url("https://serpapi.com/search.json?q=$encodedQ&num=8&api_key=$encodedKey")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val json = JSONObject(resp.body?.string().orEmpty())
                val arr = json.optJSONArray("organic_results") ?: return emptyList()
                (0 until arr.length()).mapNotNull { idx ->
                    val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                    WebSearchSnippet(
                        title = obj.optString("title"),
                        url = obj.optString("link"),
                        snippet = obj.optString("snippet").take(300),
                        provider = "SerpAPI"
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "SerpAPI search error: ${e.message}")
            emptyList()
        }
    }

    private fun extractHtmlTitle(html: String): String {
        val match = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)
        return match?.groupValues?.getOrNull(1)
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.take(120)
            .orEmpty()
    }

    fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    fun utcIsoNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }

    private companion object {
        const val TAG = "WebProbeService"
    }
}
