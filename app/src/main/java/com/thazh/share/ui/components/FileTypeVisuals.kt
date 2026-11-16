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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.data.FileCategory
import com.thazh.share.ui.theme.ThazhTheme
import io.github.neoruaa.glasmio.core.GlasmioTheme

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
    val semantic = ThazhTheme.semantic
    return when (this) {
        FileCategory.IMAGE -> semantic.accentBlue
        FileCategory.VIDEO -> semantic.categoryVideo
        FileCategory.AUDIO -> semantic.categoryAudio
        FileCategory.DOCUMENT -> semantic.categoryDocument
        FileCategory.ARCHIVE -> semantic.warningOrange
        FileCategory.APK -> semantic.accentGreen
        FileCategory.OTHER -> GlasmioTheme.colors.secondaryContent
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
    size: Dp = 44.dp,
) {
    val tint = category.accentColor()
    Box(
        modifier = modifier
            .size(size)
            .clip(ContinuousRoundedRectangle(12.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = category.icon(),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}
