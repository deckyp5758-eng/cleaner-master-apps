package com.example.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build

/**
 * Utilitas untuk membaca versi lokal aplikasi dan membandingkan versi rilis.
 */
object VersionUtils {

    /**
     * Mendapatkan versionName lokal aplikasi saat ini (contoh: "1.0.0").
     */
    fun getLocalVersionName(context: Context): String {
        return try {
            val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * Mendapatkan versionCode lokal aplikasi saat ini.
     */
    fun getLocalVersionCode(context: Context): Long {
        return try {
            val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    /**
     * Memotong awalan 'v' atau 'V' pada string versi (misal: "v1.2.0" menjadi "1.2.0").
     */
    fun sanitizeVersionString(version: String): String {
        return version.trim().removePrefix("v").removePrefix("V")
    }

    /**
     * Membandingkan dua string versi (misal "1.1.0" vs "1.0.0").
     * Mengembalikan true jika remoteVersion lebih baru daripada localVersion.
     */
    fun isRemoteVersionNewer(localVersion: String, remoteVersion: String): Boolean {
        val localClean = sanitizeVersionString(localVersion)
        val remoteClean = sanitizeVersionString(remoteVersion)

        val localParts = localClean.split(".").mapNotNull { it.toIntOrNull() }
        val remoteParts = remoteClean.split(".").mapNotNull { it.toIntOrNull() }

        val maxParts = maxOf(localParts.size, remoteParts.size)

        for (i in 0 until maxParts) {
            val localPart = localParts.getOrElse(i) { 0 }
            val remotePart = remoteParts.getOrElse(i) { 0 }

            if (remotePart > localPart) return true
            if (remotePart < localPart) return false
        }

        return false
    }
}
