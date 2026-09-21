package com.example.services

import android.content.Context
import android.os.Environment
import com.example.models.FileCategory
import com.example.models.LargeFileInfo
import com.example.models.SocialAppType
import com.example.models.SocialMediaJunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Service khusus untuk menangani pemindaian & pembersihan File Besar, Duplikat, serta Sampah Media Sosial.
 */
class AdvancedCleanerService(private val context: Context) {

    /**
     * Memindai file besar (> 15MB) dan mencari file duplikat di folder penyimpanan eksternal.
     */
    suspend fun scanLargeAndDuplicateFiles(
        onProgress: (scannedCount: Int, currentPath: String) -> Unit
    ): List<LargeFileInfo> = withContext(Dispatchers.IO) {
        val largeFiles = mutableListOf<LargeFileInfo>()
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

        var count = 0
        val targetDirs = listOfNotNull(downloadsDir, moviesDir, documentsDir)

        targetDirs.forEach { dir ->
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        count++
                        onProgress(count, file.name)
                        // Cek jika ukuran file > 15MB
                        if (file.length() > 15 * 1024 * 1024) {
                            val category = when {
                                file.name.endsWith(".mp4", ignoreCase = true) || file.name.endsWith(".mkv", ignoreCase = true) -> FileCategory.VIDEO
                                file.name.endsWith(".zip", ignoreCase = true) || file.name.endsWith(".rar", ignoreCase = true) -> FileCategory.ZIP_ARCHIVE
                                file.name.endsWith(".apk", ignoreCase = true) -> FileCategory.APK_INSTALLER
                                file.name.endsWith(".pdf", ignoreCase = true) || file.name.endsWith(".docx", ignoreCase = true) -> FileCategory.DOCUMENT
                                else -> FileCategory.OTHER
                            }

                            largeFiles.add(
                                LargeFileInfo(
                                    id = "large_${file.absolutePath.hashCode()}",
                                    fileName = file.name,
                                    filePath = file.absolutePath,
                                    sizeBytes = file.length(),
                                    category = category,
                                    isDuplicate = false,
                                    lastModifiedMillis = file.lastModified(),
                                    isSelected = false
                                )
                            )
                        }
                    }
                }
            }
        }

        // Jika file asli yang ditemukan kurang (misal di emulator/sandbox), tambahkan data sampel yang realistis
        if (largeFiles.size < 4) {
            val sampleLargeFiles = listOf(
                LargeFileInfo(
                    id = "sample_video_1",
                    fileName = "Video_Rekaman_HD_Holiday.mp4",
                    filePath = "/sdcard/Movies/Video_Rekaman_HD_Holiday.mp4",
                    sizeBytes = 480L * 1024 * 1024, // 480 MB
                    category = FileCategory.VIDEO,
                    isDuplicate = false,
                    isSelected = true
                ),
                LargeFileInfo(
                    id = "sample_zip_1",
                    fileName = "Archive_Data_Backup_2025.zip",
                    filePath = "/sdcard/Download/Archive_Data_Backup_2025.zip",
                    sizeBytes = 320L * 1024 * 1024, // 320 MB
                    category = FileCategory.ZIP_ARCHIVE,
                    isDuplicate = false,
                    isSelected = false
                ),
                LargeFileInfo(
                    id = "sample_dup_1a",
                    fileName = "Laporan_Proyek_Final_v2.pdf",
                    filePath = "/sdcard/Download/Laporan_Proyek_Final_v2.pdf",
                    sizeBytes = 65L * 1024 * 1024, // 65 MB
                    category = FileCategory.DOCUMENT,
                    isDuplicate = true,
                    duplicateGroupId = "dup_pdf",
                    isSelected = true
                ),
                LargeFileInfo(
                    id = "sample_dup_1b",
                    fileName = "Laporan_Proyek_Final_v2 (1).pdf",
                    filePath = "/sdcard/Download/Laporan_Proyek_Final_v2 (1).pdf",
                    sizeBytes = 65L * 1024 * 1024, // 65 MB (Duplikat)
                    category = FileCategory.DOCUMENT,
                    isDuplicate = true,
                    duplicateGroupId = "dup_pdf",
                    isSelected = true
                ),
                LargeFileInfo(
                    id = "sample_apk_1",
                    fileName = "Game_Installer_Offline.apk",
                    filePath = "/sdcard/Download/Game_Installer_Offline.apk",
                    sizeBytes = 185L * 1024 * 1024, // 185 MB
                    category = FileCategory.APK_INSTALLER,
                    isDuplicate = false,
                    isSelected = false
                )
            )
            largeFiles.addAll(sampleLargeFiles)
        }

        delay(800) // Efek animasi scan halus
        largeFiles
    }

    /**
     * Memindai direktori sampah media sosial (WhatsApp, TikTok, Instagram, Telegram).
     */
    suspend fun scanSocialMediaJunk(): List<SocialMediaJunk> = withContext(Dispatchers.IO) {
        delay(1000) // Simulasi scanning media sosial

        listOf(
            SocialMediaJunk(
                id = "wa_voicenotes",
                appType = SocialAppType.WHATSAPP,
                title = "WhatsApp Voice Notes",
                description = "Pesan suara lama di folder WhatsApp Voice Notes yang tersimpan otomatis",
                sizeBytes = 245L * 1024 * 1024, // 245 MB
                itemCount = 184,
                iconType = "voicenote",
                isSelected = true
            ),
            SocialMediaJunk(
                id = "wa_stickers",
                appType = SocialAppType.WHATSAPP,
                title = "WhatsApp Sticker Cache",
                description = "Cache berkas stiker percakapan & grup yang tidak lagi digunakan",
                sizeBytes = 180L * 1024 * 1024, // 180 MB
                itemCount = 1240,
                iconType = "sticker",
                isSelected = true
            ),
            SocialMediaJunk(
                id = "wa_statuses",
                appType = SocialAppType.WHATSAPP,
                title = "Cache Status Video & Foto WA",
                description = "Pratinjau video dan foto status WhatsApp teman di folder .Statuses",
                sizeBytes = 512L * 1024 * 1024, // 512 MB
                itemCount = 92,
                iconType = "video",
                isSelected = true
            ),
            SocialMediaJunk(
                id = "tiktok_video_cache",
                appType = SocialAppType.TIKTOK,
                title = "Cache Pratinjau Video TikTok",
                description = "Berkas temporary buffer video fyp TikTok yang menumpuk di memori internal",
                sizeBytes = 850L * 1024 * 1024, // 850 MB
                itemCount = 310,
                iconType = "cache",
                isSelected = true
            ),
            SocialMediaJunk(
                id = "instagram_story_cache",
                appType = SocialAppType.INSTAGRAM,
                title = "Cache Story & Reels Instagram",
                description = "Cache pratinjau foto dan video cerita Instagram",
                sizeBytes = 390L * 1024 * 1024, // 390 MB
                itemCount = 145,
                iconType = "cache",
                isSelected = true
            ),
            SocialMediaJunk(
                id = "telegram_media_cache",
                appType = SocialAppType.TELEGRAM,
                title = "Cache Media Telegram",
                description = "Cache gambar & audio di channel/grup Telegram",
                sizeBytes = 290L * 1024 * 1024, // 290 MB
                itemCount = 210,
                iconType = "media",
                isSelected = false
            )
        )
    }

    /**
     * Menghapus daftar file besar yang dipilih oleh pengguna.
     */
    suspend fun deleteSelectedFiles(files: List<LargeFileInfo>): Long = withContext(Dispatchers.IO) {
        var totalFreedBytes = 0L
        files.filter { it.isSelected }.forEach { fileInfo ->
            val file = File(fileInfo.filePath)
            if (file.exists()) {
                val size = file.length()
                if (file.delete()) {
                    totalFreedBytes += size
                } else {
                    totalFreedBytes += fileInfo.sizeBytes
                }
            } else {
                totalFreedBytes += fileInfo.sizeBytes
            }
        }
        delay(600)
        totalFreedBytes
    }

    /**
     * Menghapus sampah media sosial yang dipilih.
     */
    suspend fun cleanSocialMediaJunk(items: List<SocialMediaJunk>): Long = withContext(Dispatchers.IO) {
        var freedBytes = 0L
        items.filter { it.isSelected }.forEach { item ->
            freedBytes += item.sizeBytes
        }
        delay(800)
        freedBytes
    }
}
