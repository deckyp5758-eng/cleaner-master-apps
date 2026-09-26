package com.example.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.models.UpdateInfo
import com.example.utils.VersionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service untuk menangani pengecekan rilis GitHub, pengunduhan APK, dan instalasi aplikasi.
 */
class UpdateService(private val context: Context) {

    // Pemilik dan Nama Repositori GitHub default untuk CleanCache Pro
    var githubOwner: String = "cleancachepro"
    var githubRepo: String = "cleancache-pro-android"

    /**
     * Memeriksa versi terbaru dari GitHub API secara asinkron.
     */
    suspend fun checkForUpdates(
        owner: String = githubOwner,
        repo: String = githubRepo
    ): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "CleanCachePro-AndroidApp")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode != 200) {
                return@withContext Result.failure(
                    Exception("Gagal terhubung ke GitHub API (HTTP ${connection.responseCode})")
                )
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)

            val tagName = json.optString("tag_name", "1.0.0")
            val releaseTitle = json.optString("name", "CleanCache Pro Rilis Baru")
            val changelog = json.optString("body", "Peningkatan performa dan pembersihan cache yang lebih cepat.")

            // Cari asset file APK di dalam array assets
            var apkDownloadUrl = ""
            var apkFileName = "CleanCachePro-v${VersionUtils.sanitizeVersionString(tagName)}.apk"

            if (json.has("assets")) {
                val assets = json.getJSONArray("assets")
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    val downloadUrl = asset.optString("browser_download_url", "")
                    if (name.endsWith(".apk", ignoreCase = true) || downloadUrl.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = downloadUrl
                        apkFileName = name
                        break
                    }
                }
            }

            // Jika asset APK tidak ditemukan di list asset, gunakan fallback URL rilis
            if (apkDownloadUrl.isEmpty()) {
                apkDownloadUrl = "https://github.com/$owner/$repo/releases/download/$tagName/app-release.apk"
            }

            val localVersion = VersionUtils.getLocalVersionName(context)
            val isNewer = VersionUtils.isRemoteVersionNewer(localVersion, tagName)

            val updateInfo = UpdateInfo(
                latestVersionName = VersionUtils.sanitizeVersionString(tagName),
                latestVersionCode = 2,
                releaseTitle = releaseTitle,
                changelog = changelog,
                apkDownloadUrl = apkDownloadUrl,
                apkFileName = apkFileName,
                isUpdateAvailable = isNewer,
                localVersionName = localVersion
            )

            Result.success(updateInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Memunculkan rilis demo simulasi jika repositori belum memiliki rilis publik aktif di GitHub.
     */
    fun getDemoUpdateInfo(): UpdateInfo {
        val localVersion = VersionUtils.getLocalVersionName(context)
        return UpdateInfo(
            latestVersionName = "1.2.0",
            latestVersionCode = 3,
            releaseTitle = "CleanCache Pro v1.2.0 - Performa Pembersih Lebih Cepat",
            changelog = "• Algoritma pemindaian cache 3x lebih cepat\n" +
                    "• Dukungan pembersihan file thumbnail galeri yang presisi\n" +
                    "• Perbaikan bug dan optimalisasi penggunaan memori RAM\n" +
                    "• Pembaruan antarmuka Material 3 terbaru",
            apkDownloadUrl = "https://github.com/$githubOwner/$githubRepo/releases/download/v1.2.0/CleanCachePro-v1.2.0.apk",
            apkFileName = "CleanCachePro-v1.2.0.apk",
            isUpdateAvailable = true,
            localVersionName = localVersion
        )
    }

    /**
     * Mengunduh file APK dari URL dengan progress pembaruan.
     */
    suspend fun downloadApk(
        downloadUrl: String,
        targetFile: File,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Jika URL adalah demo URL, lakukan simulasi pengunduhan yang halus
            if (downloadUrl.contains("example.com") || downloadUrl.contains("releases/download/v1.1.0") || downloadUrl.contains("releases/download/v1.2.0")) {
                val totalSimulatedBytes = 18L * 1024 * 1024 // 18 MB
                var currentBytes = 0L

                // Tulis dummy APK file
                targetFile.parentFile?.mkdirs()
                FileOutputStream(targetFile).use { out ->
                    val buffer = ByteArray(8192)
                    while (currentBytes < totalSimulatedBytes) {
                        val chunkSize = (256 * 1024).coerceAtMost((totalSimulatedBytes - currentBytes).toInt())
                        out.write(buffer, 0, chunkSize.coerceAtMost(buffer.size))
                        currentBytes += chunkSize
                        val percent = ((currentBytes.toDouble() / totalSimulatedBytes.toDouble()) * 100).toInt()
                        onProgress(percent, currentBytes, totalSimulatedBytes)
                        kotlinx.coroutines.delay(80)
                    }
                }
                return@withContext Result.success(targetFile)
            }

            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    Exception("Gagal mengunduh APK (HTTP ${connection.responseCode})")
                )
            }

            val totalBytes = connection.contentLength.toLong()
            targetFile.parentFile?.mkdirs()

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalDownloaded = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        val percent = if (totalBytes > 0) {
                            ((totalDownloaded.toDouble() / totalBytes.toDouble()) * 100).toInt()
                        } else 50
                        onProgress(percent, totalDownloaded, totalBytes)
                    }
                }
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Membuka installer Android untuk memasang file APK yang diunduh secara langsung.
     */
    fun installApk(apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() <= 0) return false

        return try {
            // Android 8.0+ (Oreo): Cek izin instalasi dari sumber tidak dikenal
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return false
                }
            }

            // Dapatkan URI file aman menggunakan FileProvider
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
