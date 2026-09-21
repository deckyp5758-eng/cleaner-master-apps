package com.example.models

/**
 * Enums dan data class untuk kategori file sampah.
 */
enum class JunkType {
    APP_CACHE,      // Cache aplikasi (WhatsApp, Instagram, Browser, dll)
    TEMPORARY_LOGS, // File sementara dan log sistem (.log, .tmp)
    RESIDUAL_FILES, // Sisa file aplikasi yang sudah diuninstall
    THUMBNAIL_MEDIA,// File cache thumbnail galeri (.thumbnails, .cache)
    UNUSED_APK      // File APK installer yang tidak terpakai
}

data class JunkCategory(
    val type: JunkType,
    val title: String,
    val description: String,
    val totalSizeBytes: Long,
    val itemCount: Int,
    val isSelected: Boolean = true
)
