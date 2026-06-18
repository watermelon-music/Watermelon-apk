package com.watermelon.app.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

object AppUpdater {
    private const val RELEASES_API = "https://api.github.com/repos/SatyamPote/Watermelon-apk/releases/latest"

    data class UpdateInfo(
        val version: String,
        val changelog: String,
        val downloadUrl: String
    )

    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(RELEASES_API)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val tagName = json.getString("tag_name")
                val body = json.optString("body", "")

                val assets = json.getJSONArray("assets")
                var downloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.endsWith(".apk")) {
                        downloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }

                if (downloadUrl.isNotEmpty() && isNewerVersion(currentVersion, tagName)) {
                    return@withContext UpdateInfo(tagName, body, downloadUrl)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Check for update failed")
        } finally {
            connection?.disconnect()
        }
        return@withContext null
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val cleanCurrent = current.trim().removePrefix("v").removePrefix("V")
        val cleanLatest = latest.trim().removePrefix("v").removePrefix("V")

        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLength) {
            val currentPart = currentParts.getOrNull(i) ?: 0
            val latestPart = latestParts.getOrNull(i) ?: 0
            if (latestPart > currentPart) return true
            if (currentPart > latestPart) return false
        }
        return false
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            var urlString = downloadUrl
            var redirectCount = 0
            val maxRedirects = 5

            while (redirectCount < maxRedirects) {
                val url = URL(urlString)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308
                ) {
                    urlString = connection.getHeaderField("Location")
                    connection.disconnect()
                    redirectCount++
                    continue
                }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val contentLength = connection.contentLength
                    val cacheDir = context.cacheDir
                    val apkFile = File(cacheDir, "update.apk")
                    if (apkFile.exists()) {
                        apkFile.delete()
                    }

                    val input = BufferedInputStream(connection.inputStream)
                    val output = FileOutputStream(apkFile)
                    val data = ByteArray(8192)
                    var total: Long = 0
                    var count = 0

                    while (coroutineContext.isActive && input.read(data).also { count = it } != -1) {
                        total += count
                        if (contentLength > 0) {
                            val progress = ((total * 100) / contentLength).toInt()
                            onProgress(progress)
                        }
                        output.write(data, 0, count)
                    }

                    output.flush()
                    output.close()
                    input.close()

                    if (!coroutineContext.isActive) {
                        apkFile.delete()
                        return@withContext null
                    }

                    return@withContext apkFile
                } else {
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "APK download failed")
        } finally {
            connection?.disconnect()
        }
        return@withContext null
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Timber.e("Installer APK file does not exist")
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch package installer")
        }
    }
}
