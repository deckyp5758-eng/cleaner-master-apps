package com.example.models

/**
 * Model data hasil pemindaian cache dan junk files.
 */
data class ScanResult(
    val totalJunkSizeBytes: Long = 0L,
    val appCacheList: List<AppCacheInfo> = emptyList(),
    val categories: List<JunkCategory> = emptyList(),
    val scanTimestampMillis: Long = System.currentTimeMillis()
)
