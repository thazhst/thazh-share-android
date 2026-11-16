package com.thazh.share.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.thazh.share.R
import com.thazh.share.ui.cupertino.LiquidIconButton

@Composable
fun ReceivedFilesAction(
    unseenCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidIconButton(
        onClick = onClick,
        icon = Icons.Rounded.FolderOpen,
        contentDescription = stringResource(R.string.received_files_title),
        badgeCount = unseenCount,
        modifier = modifier.testTag("received_files_action_button")
    )
}
