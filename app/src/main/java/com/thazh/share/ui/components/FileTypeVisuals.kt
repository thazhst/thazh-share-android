package com.thazh.share.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thazh.share.R
import com.thazh.share.data.FileCategory
import com.thazh.share.ui.cupertino.CupertinoIcon
import com.thazh.share.ui.theme.CupertinoTheme

fun FileCategory.icon(): ImageVector = when (this) {
    FileCategory.IMAGE -> Icons.Rounded.Image
    FileCategory.VIDEO -> Icons.Rounded.VideoFile
    FileCategory.AUDIO -> Icons.Rounded.AudioFile
    FileCategory.DOCUMENT -> Icons.Rounded.Description
    FileCategory.ARCHIVE -> Icons.Rounded.FolderZip
    FileCategory.APK -> Icons.Rounded.Android
    FileCategory.OTHER -> Icons.Rounded.InsertDriveFile
}

@Composable
fun FileCategory.accentColor(): Color {
    val colors = CupertinoTheme.colors
    return when (this) {
        FileCategory.IMAGE -> colors.accentBlue
        FileCategory.VIDEO -> Color(0xFFAF52DE)
        FileCategory.AUDIO -> Color(0xFFFF2D55)
        FileCategory.DOCUMENT -> Color(0xFF5856D6)
        FileCategory.ARCHIVE -> colors.warningOrange
        FileCategory.APK -> colors.accentGreen
        FileCategory.OTHER -> colors.secondaryLabel
    }
}

fun FileCategory.labelRes(): Int = when (this) {
    FileCategory.IMAGE -> R.string.received_files_category_image
    FileCategory.VIDEO -> R.string.received_files_category_video
    FileCategory.AUDIO -> R.string.received_files_category_audio
    FileCategory.DOCUMENT -> R.string.received_files_category_document
    FileCategory.ARCHIVE -> R.string.received_files_category_archive
    FileCategory.APK -> R.string.received_files_category_apk
    FileCategory.OTHER -> R.string.received_files_category_other
}

@Composable
fun FileTypeBadge(
    category: FileCategory,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val tint = category.accentColor()
    Box(
        modifier = modifier
            .size(size)
            .clip(CupertinoTheme.shapes.iconBadge)
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        CupertinoIcon(
            imageVector = category.icon(),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}
