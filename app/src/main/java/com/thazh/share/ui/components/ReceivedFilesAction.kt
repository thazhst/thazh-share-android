package com.thazh.share.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thazh.share.R
import com.thazh.share.ui.theme.ThazhSymbols
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioIconButton

@Composable
fun ReceivedFilesAction(
    unseenCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        GlasmioIconButton(
            onClick = onClick,
            modifier = Modifier.testTag("received_files_action_button"),
        ) {
            SfIcon(
                systemName = ThazhSymbols.FolderOpen,
                contentDescription = stringResource(R.string.received_files_title),
            )
        }
        if (unseenCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(GlasmioTheme.colors.destructive),
                contentAlignment = Alignment.Center,
            ) {
                GlasmioText(
                    text = if (unseenCount > 99) "99+" else unseenCount.toString(),
                    style = GlasmioTheme.textStyles.caption2.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}
