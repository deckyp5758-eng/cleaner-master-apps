package com.example.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderDelete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.JunkCategory
import com.example.models.JunkType
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.JunkApk
import com.example.ui.theme.JunkLog
import com.example.ui.theme.JunkThumbnail
import com.example.ui.theme.PrimaryNavy
import com.example.utils.FileUtils

/**
 * Kartu ringkasan untuk setiap kategori file sampah.
 */
@Composable
fun JunkCategoryCard(
    category: JunkCategory,
    isSelected: Boolean,
    onToggle: (JunkType) -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, iconBg) = when (category.type) {
        JunkType.APP_CACHE -> Icons.Rounded.DeleteSweep to PrimaryNavy
        JunkType.TEMPORARY_LOGS -> Icons.Rounded.BugReport to JunkLog
        JunkType.RESIDUAL_FILES -> Icons.Rounded.FolderDelete to PrimaryNavy
        JunkType.THUMBNAIL_MEDIA -> Icons.Rounded.Image to JunkThumbnail
        JunkType.UNUSED_APK -> Icons.Rounded.Android to JunkApk
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("junk_category_${category.type.name}")
            .clickable { onToggle(category.type) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle(category.type) },
                colors = CheckboxDefaults.colors(
                    checkedColor = AccentGreen,
                    checkmarkColor = Color.Black
                ),
                modifier = Modifier.testTag("checkbox_category_${category.type.name}")
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category.title,
                    tint = iconBg,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = category.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FileUtils.formatFileSize(category.totalSizeBytes),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = if (isSelected) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${category.itemCount} item",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
