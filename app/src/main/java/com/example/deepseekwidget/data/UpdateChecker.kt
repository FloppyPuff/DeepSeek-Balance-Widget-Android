package com.example.deepseekwidget.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val latestVersion: String,
    val tagName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val downloadUrl: String,
    val hasUpdate: Boolean
)

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data class HasUpdate(val updateInfo: UpdateInfo) : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

object UpdateChecker {
    private const val GITHUB_API_URL =
        "https://api.github.com/repos/FloppyPuff/DeepSeek-Balance-Widget-Android/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun checkLatestRelease(currentVersion: String): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "DeepSeek-Balance-App")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}")
                }
                val body = response.body?.string() ?: throw IllegalStateException("Empty response")
                parseReleaseJson(body, currentVersion)
            }
        }
    }

    fun parseReleaseJson(jsonStr: String, currentVersion: String): UpdateInfo {
        val json = JSONObject(jsonStr)
        val rawTag = json.optString("tag_name", "")
        val cleanVersion = rawTag.removePrefix("v").removePrefix("V").trim()
        val title = json.optString("name", "DeepSeek Balance $rawTag")
        val notes = json.optString("body", "").trim()
        val htmlUrl = json.optString("html_url", "https://github.com/FloppyPuff/DeepSeek-Balance-Widget-Android/releases")

        var apkDownloadUrl: String? = null
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    apkDownloadUrl = asset.optString("browser_download_url", "")
                    break
                }
            }
        }

        val downloadUrl = apkDownloadUrl?.takeIf { it.isNotBlank() } ?: htmlUrl
        val hasUpdate = isNewerVersion(currentVersion, cleanVersion)

        return UpdateInfo(
            latestVersion = cleanVersion,
            tagName = rawTag,
            releaseTitle = title,
            releaseNotes = notes,
            htmlUrl = htmlUrl,
            downloadUrl = downloadUrl,
            hasUpdate = hasUpdate
        )
    }

    fun isNewerVersion(currentVersion: String, latestVersion: String): Boolean {
        val currentParts = currentVersion.trim().removePrefix("v").removePrefix("V").split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latestVersion.trim().removePrefix("v").removePrefix("V").split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLen) {
            val curr = currentParts.getOrElse(i) { 0 }
            val latest = latestParts.getOrElse(i) { 0 }
            if (latest > curr) return true
            if (latest < curr) return false
        }
        return false
    }
}
