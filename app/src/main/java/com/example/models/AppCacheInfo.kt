package com.example.models

/**
 * Model data yang me-representasikan informasi cache untuk sebuah aplikasi.
 */
data class AppCacheInfo(
    val id: String,
    val appName: String,
    val packageName: String,
    val cacheSizeBytes: Long,
    val categoryName: String = "Cache Aplikasi",
    val isSystemApp: Boolean = false,
    val isSelected: Boolean = true,
    val iconResId: Int? = null
)
