package com.example.deepseekwidget.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed interface DownloadProgress {
    data object Idle : DownloadProgress
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadProgress
    data class Completed(val apkFile: File) : DownloadProgress
    data class Failed(val errorMessage: String) : DownloadProgress
}

object UpdateDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        version: String,
        onProgress: (DownloadProgress) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
            }
            val targetFile = File(updatesDir, "deepseek-balance-v$version.apk")
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("User-Agent", "DeepSeek-Balance-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: ${response.message}")
                }
                val body = response.body ?: throw IOException("Empty response body")
                val totalBytes = body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var bytesRead: Int
                        var downloadedBytes = 0L
                        var lastProgressReportTime = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastProgressReportTime > 80L || downloadedBytes == totalBytes) {
                                lastProgressReportTime = now
                                val progress = if (totalBytes > 0) {
                                    (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                onProgress(DownloadProgress.Downloading(progress, downloadedBytes, totalBytes))
                            }
                        }
                        output.flush()
                    }
                }
            }

            onProgress(DownloadProgress.Completed(targetFile))
            targetFile
        }.onFailure { err ->
            onProgress(DownloadProgress.Failed(err.localizedMessage ?: "下载失败"))
        }
    }

    fun installApk(context: Context, apkFile: File): Result<Unit> {
        return runCatching {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, apkFile)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        }
    }
}
