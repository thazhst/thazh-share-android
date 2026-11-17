package com.thazh.share.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.data.ConnectionQuality
import com.thazh.share.data.TransferDirection
import com.thazh.share.data.TransferItem
import com.thazh.share.data.TransferState
import com.thazh.share.ui.components.ConnectionQualityIndicator
import com.thazh.share.ui.components.FileTypeBadge
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import com.thazh.share.util.FileActions
import com.thazh.share.util.FormatUtils
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.systemGray5
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioSectionTitle
import java.io.File

@Composable
fun FileTransferScreen(
    peerName: String,
    isSocketConnected: Boolean,
    connectionQuality: ConnectionQuality,
    estimatedDistanceMeters: Int?,
    transfers: List<TransferItem>,
    bottomBarPadding: Dp = 96.dp,
    onPickFiles: () -> Unit,
    onPickMedia: () -> Unit,
    onCancelTransfer: (String) -> Unit,
    onRetryTransfer: (String) -> Unit,
    onDismissTransfer: (String) -> Unit,
    onClearFinished: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = GlasmioTheme.colors
    val appearance = GlasmioTheme.appearance
    val semantic = ThazhTheme.semantic
    var showDisconnectDialog by remember { mutableStateOf(false) }

    val subtitleParts = buildList {
        add(
            if (isSocketConnected) {
                stringResource(R.string.transfer_send_subtitle, peerName)
            } else {
                stringResource(R.string.transfer_connecting)
            },
        )
        if (estimatedDistanceMeters != null) {
            add(FormatUtils.formatDistance(estimatedDistanceMeters))
        }
    }

    GlasmioPinnedListPage(
        title = peerName,
        subtitle = subtitleParts.joinToString(" • "),
        onNavigateBack = onBack,
        navigateBackContentDescription = stringResource(R.string.back),
        bottomPadding = bottomBarPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        backgroundColor = colors.groupedBackground.copy(
            alpha = if (appearance.reduceTransparency) 1f else 0.78f,
        ),
        modifier = modifier.fillMaxSize(),
        actions = {
            ConnectionQualityIndicator(
                quality = connectionQuality,
                showLabel = false,
                modifier = Modifier.padding(end = 2.dp),
            )
            GlasmioIconButton(
                onClick = { showDisconnectDialog = true },
                modifier = Modifier.testTag("disconnect_button"),
            ) {
                SfIcon(
                    systemName = ThazhSymbols.LinkOff,
                    contentDescription = stringResource(R.string.transfer_disconnect),
                    tint = colors.destructive,
                )
            }
            GlasmioIconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("transfer_open_settings_button"),
            ) {
                SfIcon(
                    systemName = ThazhSymbols.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                )
            }
        },
    ) {
        item(key = "send_panel") {
            CenteredTransferContent {
                GlasmioCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(ContinuousRoundedRectangle(12.dp))
                                    .background(semantic.accentBlue.copy(alpha = if (colors.isDark) 0.22f else 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                SfIcon(
                                    systemName = ThazhSymbols.SwapVert,
                                    contentDescription = null,
                                    tint = semantic.accentBlue,
                                    size = 26.dp,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.transfer_send_title),
                                    style = GlasmioTheme.textStyles.title2.copy(fontWeight = FontWeight.Bold),
                                    color = colors.content,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isSocketConnected) {
                                        stringResource(R.string.transfer_send_subtitle, peerName)
                                    } else {
                                        stringResource(R.string.transfer_waiting_socket)
                                    },
                                    style = GlasmioTheme.textStyles.footnote,
                                    color = if (isSocketConnected) colors.secondaryContent else semantic.warningOrange,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            GlasmioButton(
                                onClick = onPickFiles,
                                enabled = isSocketConnected,
                                emphasis = GlassEmphasis.Prominent,
                                navigationSurfaceColor = semantic.accentBlue,
                                size = GlasmioButtonSize.Large,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pick_files_button"),
                            ) {
                                SfIcon(
                                    systemName = ThazhSymbols.AttachFile,
                                    contentDescription = null,
                                    tint = Color.White,
                                    size = 18.dp,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.transfer_pick_files),
                                    style = GlasmioTheme.textStyles.headline,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            GlasmioButton(
                                onClick = onPickMedia,
                                enabled = isSocketConnected,
                                emphasis = GlassEmphasis.Regular,
                                size = GlasmioButtonSize.Large,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pick_media_button"),
                            ) {
                                SfIcon(
                                    systemName = ThazhSymbols.PhotoLibrary,
                                    contentDescription = null,
                                    tint = semantic.accentBlue,
                                    size = 18.dp,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.transfer_pick_media),
                                    style = GlasmioTheme.textStyles.headline,
                                    color = semantic.accentBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "activity_header") {
            CenteredTransferContent {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    GlasmioSectionTitle(
                        title = stringResource(R.string.transfer_activity_title),
                        modifier = Modifier.weight(1f),
                    )
                    if (transfers.any { it.isFinished }) {
                        GlasmioButton(
                            onClick = onClearFinished,
                            emphasis = GlassEmphasis.Regular,
                            size = GlasmioButtonSize.Small,
                            modifier = Modifier.testTag("clear_finished_transfers_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.transfer_clear_finished),
                                style = GlasmioTheme.textStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.accent,
                            )
                        }
                    }
                }
            }
        }

        if (transfers.isEmpty()) {
            item(key = "transfers_empty") {
                CenteredTransferContent {
                    GlasmioCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.transfer_empty_title),
                                style = GlasmioTheme.textStyles.headline,
                                color = colors.content,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = stringResource(R.string.transfer_empty_body),
                                style = GlasmioTheme.textStyles.subheadline,
                                color = colors.secondaryContent,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        } else {
            items(transfers, key = { it.id }) { item ->
                CenteredTransferContent {
                    TransferActivityCard(
                        item = item,
                        onOpenFile = {
                            val path = item.localPath ?: return@TransferActivityCard
                            if (!FileActions.openFile(context, File(path), item.mimeType)) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.transfer_cannot_open_file),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        onCancel = { onCancelTransfer(item.id) },
                        onRetry = { onRetryTransfer(item.id) },
                        onDismiss = { onDismissTransfer(item.id) },
                    )
                }
            }
        }
    }

    GlasmioAlertDialog(
        visible = showDisconnectDialog,
        onDismissRequest = { showDisconnectDialog = false },
        title = stringResource(R.string.transfer_disconnect),
        message = stringResource(R.string.transfer_send_subtitle, peerName),
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.cancel),
                onClick = { showDisconnectDialog = false },
                role = GlasmioAlertButtonRole.Cancel,
            ),
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.confirm),
                onClick = {
                    showDisconnectDialog = false
                    onDisconnect()
                },
                role = GlasmioAlertButtonRole.Destructive,
            ),
        ),
    )
}

@Composable
private fun CenteredTransferContent(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
        ) {
            content()
        }
    }
}

@Composable
internal fun TransferActivityCard(
    item: TransferItem,
    onOpenFile: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val canOpen = item.direction == TransferDirection.RECEIVED &&
        item.state == TransferState.COMPLETED &&
        !item.localPath.isNullOrBlank()

    GlasmioCard(
        onClick = if (canOpen) onOpenFile else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileTypeBadge(category = item.category)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    SfIcon(
                        systemName = if (item.direction == TransferDirection.SENT) {
                            ThazhSymbols.ArrowUp
                        } else {
                            ThazhSymbols.ArrowDown
                        },
                        contentDescription = null,
                        tint = if (item.direction == TransferDirection.SENT) {
                            semantic.accentBlue
                        } else {
                            semantic.accentGreen
                        },
                        size = 14.dp,
                    )
                    Text(
                        text = item.fileName,
                        style = GlasmioTheme.textStyles.headline,
                        color = colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                val statusText = when (item.state) {
                    TransferState.PENDING -> stringResource(
                        R.string.transfer_status_pending,
                        FormatUtils.formatFileSize(item.fileSizeBytes),
                    )
                    TransferState.IN_PROGRESS -> {
                        val base = if (item.direction == TransferDirection.SENT) {
                            stringResource(
                                R.string.transfer_status_sending,
                                FormatUtils.formatProgressPercent(item.progress),
                            )
                        } else {
                            stringResource(
                                R.string.transfer_status_receiving,
                                FormatUtils.formatProgressPercent(item.progress),
                            )
                        }
                        val speedPart = if (item.speedBytesPerSec > 0L) {
                            " • ${FormatUtils.formatSpeed(item.speedBytesPerSec)}"
                        } else {
                            ""
                        }
                        val etaPart = item.etaSeconds?.let {
                            " • ${stringResource(R.string.transfer_status_eta, FormatUtils.formatEta(it))}"
                        } ?: ""
                        "$base$speedPart$etaPart"
                    }
                    TransferState.COMPLETED -> {
                        val peer = item.peerName.ifBlank { stringResource(R.string.transfer_unknown_peer) }
                        if (item.direction == TransferDirection.SENT) {
                            stringResource(
                                R.string.transfer_status_sent,
                                FormatUtils.formatFileSize(item.fileSizeBytes),
                                peer,
                            )
                        } else {
                            stringResource(
                                R.string.transfer_status_received,
                                FormatUtils.formatFileSize(item.fileSizeBytes),
                                peer,
                            )
                        }
                    }
                    TransferState.FAILED -> item.errorMessage?.let {
                        stringResource(R.string.transfer_status_failed, it)
                    } ?: stringResource(R.string.transfer_status_failed_generic)
                    TransferState.CANCELLED -> stringResource(R.string.transfer_status_cancelled)
                }

                Text(
                    text = statusText,
                    style = GlasmioTheme.textStyles.footnote,
                    color = when (item.state) {
                        TransferState.COMPLETED -> semantic.accentGreen
                        TransferState.FAILED -> colors.destructive
                        TransferState.CANCELLED -> semantic.warningOrange
                        else -> colors.secondaryContent
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (item.isActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { item.progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(ContinuousCapsule),
                        color = if (item.direction == TransferDirection.SENT) {
                            semantic.accentBlue
                        } else {
                            semantic.accentGreen
                        },
                        trackColor = colors.systemGray5,
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            when (item.state) {
                TransferState.PENDING, TransferState.IN_PROGRESS -> {
                    GlasmioIconButton(
                        onClick = onCancel,
                        size = 48.dp,
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.Close,
                            contentDescription = stringResource(R.string.transfer_cancel),
                            tint = colors.destructive,
                            size = 20.dp,
                        )
                    }
                }
                TransferState.FAILED, TransferState.CANCELLED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (item.direction == TransferDirection.SENT && item.sourceUri != null) {
                            GlasmioIconButton(
                                onClick = onRetry,
                                size = 48.dp,
                            ) {
                                SfIcon(
                                    systemName = ThazhSymbols.Refresh,
                                    contentDescription = stringResource(R.string.transfer_retry),
                                    tint = colors.accent,
                                    size = 20.dp,
                                )
                            }
                        }
                        GlasmioIconButton(
                            onClick = onDismiss,
                            size = 48.dp,
                        ) {
                            SfIcon(
                                systemName = ThazhSymbols.Close,
                                contentDescription = stringResource(R.string.transfer_dismiss),
                                tint = colors.secondaryContent,
                                size = 20.dp,
                            )
                        }
                    }
                }
                TransferState.COMPLETED -> {
                    SfIcon(
                        systemName = ThazhSymbols.CheckCircle,
                        contentDescription = null,
                        tint = semantic.accentGreen,
                        size = 20.dp,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
        }
    }
}
