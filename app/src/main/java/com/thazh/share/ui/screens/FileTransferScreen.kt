package com.thazh.share.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.thazh.share.R
import com.thazh.share.data.ConnectionQuality
import com.thazh.share.data.TransferDirection
import com.thazh.share.data.TransferItem
import com.thazh.share.data.TransferState
import com.thazh.share.ui.components.ConnectionQualityIndicator
import com.thazh.share.ui.components.FileTypeBadge
import com.thazh.share.ui.cupertino.CupertinoIcon
import com.thazh.share.ui.cupertino.CupertinoProgressBar
import com.thazh.share.ui.cupertino.CupertinoText
import com.thazh.share.ui.cupertino.CupertinoTopBar
import com.thazh.share.ui.cupertino.LiquidAlertDialog
import com.thazh.share.ui.cupertino.LiquidButton
import com.thazh.share.ui.cupertino.LiquidCard
import com.thazh.share.ui.cupertino.LiquidIconButton
import com.thazh.share.ui.cupertino.elasticHold
import com.thazh.share.ui.theme.CupertinoTheme
import com.thazh.share.ui.theme.LocalAppBackdrop
import com.thazh.share.util.FileActions
import com.thazh.share.util.FormatUtils
import java.io.File

@Composable
fun FileTransferScreen(
    peerName: String,
    isSocketConnected: Boolean,
    connectionQuality: ConnectionQuality,
    estimatedDistanceMeters: Int?,
    transfers: List<TransferItem>,
    onPickFiles: () -> Unit,
    onPickMedia: () -> Unit,
    onCancelTransfer: (String) -> Unit,
    onRetryTransfer: (String) -> Unit,
    onDismissTransfer: (String) -> Unit,
    onClearFinished: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CupertinoTheme.colors
    var showDisconnectDialog by remember { mutableStateOf(false) }

    val subtitleParts = buildList {
        add(if (isSocketConnected) stringResource(R.string.transfer_send_subtitle, peerName) else stringResource(R.string.transfer_connecting))
        if (estimatedDistanceMeters != null) {
            add(FormatUtils.formatDistance(estimatedDistanceMeters))
        }
    }

    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    val topPadding = if (topBarHeightPx > 0) {
        with(density) { topBarHeightPx.toDp() } + 16.dp
    } else {
        112.dp
    }
    val contentBackdrop = rememberLayerBackdrop()
    val barBackdrop = rememberCombinedBackdrop(LocalAppBackdrop.current, contentBackdrop)

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(contentBackdrop),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = topPadding,
                    bottom = 112.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "send_panel") {
                    LiquidCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CupertinoTheme.shapes.iconBadge)
                                    .background(colors.accentBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CupertinoIcon(
                                    imageVector = Icons.Rounded.SwapVert,
                                    contentDescription = null,
                                    tint = colors.accentBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                CupertinoText(
                                    text = stringResource(R.string.transfer_send_title),
                                    style = CupertinoTheme.typography.title2
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                CupertinoText(
                                    text = if (isSocketConnected) {
                                        stringResource(R.string.transfer_send_subtitle, peerName)
                                    } else {
                                        stringResource(R.string.transfer_waiting_socket)
                                    },
                                    style = CupertinoTheme.typography.footnote,
                                    color = if (isSocketConnected) colors.secondaryLabel else colors.warningOrange
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LiquidButton(
                                onClick = onPickFiles,
                                enabled = isSocketConnected,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pick_files_button")
                            ) {
                                CupertinoIcon(
                                    imageVector = Icons.Rounded.AttachFile,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                CupertinoText(
                                    text = stringResource(R.string.transfer_pick_files),
                                    style = CupertinoTheme.typography.headline,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }

                            LiquidButton(
                                onClick = onPickMedia,
                                enabled = isSocketConnected,
                                tint = Color.Unspecified,
                                surfaceColor = colors.accentBlue.copy(alpha = 0.14f),
                                contentColor = colors.accentBlue,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pick_media_button")
                            ) {
                                CupertinoIcon(
                                    imageVector = Icons.Rounded.PhotoLibrary,
                                    contentDescription = null,
                                    tint = colors.accentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                CupertinoText(
                                    text = stringResource(R.string.transfer_pick_media),
                                    style = CupertinoTheme.typography.headline,
                                    color = colors.accentBlue,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                item(key = "activity_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CupertinoText(
                            text = stringResource(R.string.transfer_activity_title).uppercase(),
                            style = CupertinoTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.secondaryLabel
                        )
                        if (transfers.any { it.isFinished }) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 40.dp)
                                    .elasticHold(pressedScale = 0.92f)
                                    .clip(Capsule())
                                    .clickable(role = Role.Button, onClick = onClearFinished)
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("clear_finished_transfers_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                CupertinoText(
                                    text = stringResource(R.string.transfer_clear_finished),
                                    style = CupertinoTheme.typography.footnote.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.accentBlue
                                )
                            }
                        }
                    }
                }

                if (transfers.isEmpty()) {
                    item(key = "transfers_empty") {
                        LiquidCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CupertinoText(
                                    text = stringResource(R.string.transfer_empty_title),
                                    style = CupertinoTheme.typography.headline,
                                    textAlign = TextAlign.Center
                                )
                                CupertinoText(
                                    text = stringResource(R.string.transfer_empty_body),
                                    style = CupertinoTheme.typography.subheadline,
                                    color = colors.secondaryLabel,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(transfers, key = { it.id }) { item ->
                        TransferActivityCard(
                            item = item,
                            onOpenFile = {
                                val path = item.localPath ?: return@TransferActivityCard
                                if (!FileActions.openFile(context, File(path), item.mimeType)) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.transfer_cannot_open_file),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onCancel = { onCancelTransfer(item.id) },
                            onRetry = { onRetryTransfer(item.id) },
                            onDismiss = { onDismissTransfer(item.id) }
                        )
                    }
                }
            }
        }

        CompositionLocalProvider(LocalAppBackdrop provides barBackdrop) {
            CupertinoTopBar(
                title = peerName,
                subtitle = subtitleParts.joinToString(" • "),
                onBack = onBack,
                backTestTag = "transfer_back_button",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged { topBarHeightPx = it.height },
                actions = {
                    ConnectionQualityIndicator(
                        quality = connectionQuality,
                        showLabel = false
                    )
                    LiquidIconButton(
                        onClick = { showDisconnectDialog = true },
                        icon = Icons.Rounded.LinkOff,
                        contentDescription = stringResource(R.string.transfer_disconnect),
                        tint = colors.destructiveRed,
                        modifier = Modifier.testTag("disconnect_button")
                    )
                    LiquidIconButton(
                        onClick = onOpenSettings,
                        icon = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        modifier = Modifier.testTag("transfer_open_settings_button")
                    )
                }
            )
        }
    }

    if (showDisconnectDialog) {
        LiquidAlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = stringResource(R.string.transfer_disconnect),
            confirmText = stringResource(R.string.confirm),
            confirmDestructive = true,
            onConfirm = {
                showDisconnectDialog = false
                onDisconnect()
            },
            dismissText = stringResource(R.string.cancel)
        ) {
            CupertinoText(
                text = stringResource(R.string.transfer_send_subtitle, peerName),
                style = CupertinoTheme.typography.subheadline,
                color = colors.secondaryLabel
            )
        }
    }
}

@Composable
internal fun TransferActivityCard(
    item: TransferItem,
    onOpenFile: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = CupertinoTheme.colors
    val canOpen = item.direction == TransferDirection.RECEIVED &&
        item.state == TransferState.COMPLETED &&
        !item.localPath.isNullOrBlank()

    LiquidCard(
        onClick = if (canOpen) onOpenFile else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileTypeBadge(category = item.category)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    CupertinoIcon(
                        imageVector = if (item.direction == TransferDirection.SENT) {
                            Icons.Rounded.ArrowUpward
                        } else {
                            Icons.Rounded.ArrowDownward
                        },
                        contentDescription = null,
                        tint = if (item.direction == TransferDirection.SENT) colors.accentBlue else colors.accentGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    CupertinoText(
                        text = item.fileName,
                        style = CupertinoTheme.typography.headline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                val statusText = when (item.state) {
                    TransferState.PENDING -> stringResource(
                        R.string.transfer_status_pending,
                        FormatUtils.formatFileSize(item.fileSizeBytes)
                    )
                    TransferState.IN_PROGRESS -> {
                        val base = if (item.direction == TransferDirection.SENT) {
                            stringResource(R.string.transfer_status_sending, FormatUtils.formatProgressPercent(item.progress))
                        } else {
                            stringResource(R.string.transfer_status_receiving, FormatUtils.formatProgressPercent(item.progress))
                        }
                        val speedPart = if (item.speedBytesPerSec > 0L) " • ${FormatUtils.formatSpeed(item.speedBytesPerSec)}" else ""
                        val etaPart = item.etaSeconds?.let { " • ${stringResource(R.string.transfer_status_eta, FormatUtils.formatEta(it))}" } ?: ""
                        "$base$speedPart$etaPart"
                    }
                    TransferState.COMPLETED -> {
                        val peer = item.peerName.ifBlank { stringResource(R.string.transfer_unknown_peer) }
                        if (item.direction == TransferDirection.SENT) {
                            stringResource(R.string.transfer_status_sent, FormatUtils.formatFileSize(item.fileSizeBytes), peer)
                        } else {
                            stringResource(R.string.transfer_status_received, FormatUtils.formatFileSize(item.fileSizeBytes), peer)
                        }
                    }
                    TransferState.FAILED -> item.errorMessage?.let {
                        stringResource(R.string.transfer_status_failed, it)
                    } ?: stringResource(R.string.transfer_status_failed_generic)
                    TransferState.CANCELLED -> stringResource(R.string.transfer_status_cancelled)
                }

                CupertinoText(
                    text = statusText,
                    style = CupertinoTheme.typography.footnote,
                    color = when (item.state) {
                        TransferState.COMPLETED -> colors.accentGreen
                        TransferState.FAILED -> colors.destructiveRed
                        TransferState.CANCELLED -> colors.warningOrange
                        else -> colors.secondaryLabel
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.isActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CupertinoProgressBar(
                        progress = item.progress,
                        color = if (item.direction == TransferDirection.SENT) colors.accentBlue else colors.accentGreen
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            when (item.state) {
                TransferState.PENDING, TransferState.IN_PROGRESS -> {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .elasticHold(pressedScale = 0.88f)
                            .clip(Capsule())
                            .clickable(role = Role.Button, onClick = onCancel),
                        contentAlignment = Alignment.Center
                    ) {
                        CupertinoIcon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.transfer_cancel),
                            tint = colors.destructiveRed
                        )
                    }
                }
                TransferState.FAILED, TransferState.CANCELLED -> {
                    Row {
                        if (item.direction == TransferDirection.SENT && item.sourceUri != null) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .elasticHold(pressedScale = 0.88f)
                                    .clip(Capsule())
                                    .clickable(role = Role.Button, onClick = onRetry),
                                contentAlignment = Alignment.Center
                            ) {
                                CupertinoIcon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.transfer_retry),
                                    tint = colors.accentBlue
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .elasticHold(pressedScale = 0.88f)
                                .clip(Capsule())
                                .clickable(role = Role.Button, onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            CupertinoIcon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.transfer_dismiss),
                                tint = colors.secondaryLabel
                            )
                        }
                    }
                }
                TransferState.COMPLETED -> {
                    CupertinoIcon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = colors.accentGreen,
                        modifier = Modifier.padding(end = 6.dp).size(20.dp)
                    )
                }
            }
        }
    }
}
