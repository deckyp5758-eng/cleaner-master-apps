package com.example.models

enum class SocialAppType {
    WHATSAPP,
    TIKTOK,
    INSTAGRAM,
    TELEGRAM
}

/**
 * Model data kategori sampah media sosial (misal: WhatsApp Voice Note, Stiker, TikTok Video Cache).
 */
data class SocialMediaJunk(
    val id: String,
    val appType: SocialAppType,
    val title: String,
    val description: String,
    val sizeBytes: Long,
    val itemCount: Int,
    val iconType: String = "media", // "voicenote", "sticker", "video", "cache"
    val isSelected: Boolean = true
)
