package com.thazh.share.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.data.FileCategory
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.icons.SfIcon

fun FileCategory.symbolName(): String = when (this) {
    FileCategory.IMAGE -> ThazhSymbols.FileImage
    FileCategory.VIDEO -> ThazhSymbols.FileVideo
    FileCategory.AUDIO -> ThazhSymbols.FileAudio
    FileCategory.DOCUMENT -> ThazhSymbols.FileDocument
    FileCategory.ARCHIVE -> ThazhSymbols.FileArchive
    FileCategory.APK -> ThazhSymbols.FileApk
    FileCategory.OTHER -> ThazhSymbols.FileOther
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
    val colors = GlasmioTheme.colors
    val tint = category.accentColor()
    val shape = ContinuousRoundedRectangle(12.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        tint.copy(alpha = if (colors.isDark) 0.28f else 0.17f),
                        tint.copy(alpha = if (colors.isDark) 0.16f else 0.09f),
                    ),
                ),
            )
            .border(
                width = 0.8.dp,
                color = tint.copy(alpha = if (colors.isDark) 0.34f else 0.22f),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        SfIcon(
            systemName = category.symbolName(),
            contentDescription = null,
            tint = tint,
            size = size * 0.50f,
        )
    }
}
