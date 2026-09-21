package com.example.models

enum class FileCategory {
    VIDEO,
    ZIP_ARCHIVE,
    DOCUMENT,
    APK_INSTALLER,
    AUDIO_MEDIA,
    OTHER
}

/**
 * Model data untuk file besar dan file duplikat di penyimpanan ponsel.
 */
data class LargeFileInfo(
    val id: String,
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val category: FileCategory = FileCategory.VIDEO,
    val isDuplicate: Boolean = false,
    val duplicateGroupId: String? = null,
    val lastModifiedMillis: Long = System.currentTimeMillis(),
    val isSelected: Boolean = false
)
