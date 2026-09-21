package com.example.models

/**
 * Model data informasi rilis update dari GitHub.
 */
data class UpdateInfo(
    val latestVersionName: String = "1.0.0",
    val latestVersionCode: Int = 1,
    val releaseTitle: String = "",
    val changelog: String = "",
    val apkDownloadUrl: String = "",
    val apkFileName: String = "CleanCachePro.apk",
    val isUpdateAvailable: Boolean = false,
    val localVersionName: String = "1.0.0"
)
