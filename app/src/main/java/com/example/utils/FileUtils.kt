package com.example.utils

import android.os.Environment
import android.os.StatFs
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

/**
 * Utilitas untuk manipulasi file, pemformatan ukuran storage, dan pembacaan direktori.
 */
object FileUtils {

    /**
     * Memformat ukuran byte menjadi string yang mudah dibaca (misal: "245.5 MB" atau "1.24 GB").
     */
    fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(sizeInBytes.toDouble()) / log10(1024.0)).toInt().coerceAtMost(units.size - 1)
        val value = sizeInBytes / 1024.0.pow(digitGroups.toDouble())
        val df = DecimalFormat("#,##0.00")
        return "${df.format(value)} ${units[digitGroups]}"
    }

    /**
     * Mendapatkan total ukuran penyimpanan internal device dalam Byte.
     */
    fun getTotalInternalStorageSize(): Long {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            totalBlocks * blockSize
        } catch (e: Exception) {
            64L * 1024 * 1024 * 1024 // Fallback 64 GB
        }
    }

    /**
     * Mendapatkan sisa ruang penyimpanan internal yang tersedia dalam Byte.
     */
    fun getAvailableInternalStorageSize(): Long {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            availableBlocks * blockSize
        } catch (e: Exception) {
            12L * 1024 * 1024 * 1024 // Fallback 12 GB
        }
    }

    /**
     * Menghitung total ukuran file di dalam suatu folder secara rekursif.
     */
    fun getFolderSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var result: Long = 0
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            result += if (file.isDirectory) {
                getFolderSize(file)
            } else {
                file.length()
            }
        }
        return result
    }

    /**
     * Memhapus semua isi file dalam sebuah folder secara aman tanpa menghapus direktori utamanya.
     */
    fun deleteFolderContents(dir: File?): Long {
        if (dir == null || !dir.exists() || !dir.isDirectory) return 0L
        var freedBytes: Long = 0
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            freedBytes += if (file.isDirectory) {
                val size = getFolderSize(file)
                if (file.deleteRecursively()) size else 0L
            } else {
                val size = file.length()
                if (file.delete()) size else 0L
            }
        }
        return freedBytes
    }
}
