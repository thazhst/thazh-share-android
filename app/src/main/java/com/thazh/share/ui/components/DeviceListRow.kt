package com.thazh.share.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.thazh.share.R
import com.thazh.share.data.DiscoveredPeer
import com.thazh.share.data.PeerStatus
import com.thazh.share.data.SavedDevice
import com.thazh.share.ui.cupertino.CupertinoActivityIndicator
import com.thazh.share.ui.cupertino.CupertinoIcon
import com.thazh.share.ui.cupertino.CupertinoText
import com.thazh.share.ui.cupertino.LiquidActionPopover
import com.thazh.share.ui.cupertino.LiquidButton
import com.thazh.share.ui.cupertino.PopoverMenuItem
import com.thazh.share.ui.cupertino.elasticHold
import com.thazh.share.ui.theme.CupertinoTheme
import com.thazh.share.util.FormatUtils

@Composable
fun DiscoveredPeerRow(
    peer: DiscoveredPeer,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CupertinoTheme.colors
    val badgeColor = when (peer.status) {
        PeerStatus.CONNECTED -> colors.accentGreen
        PeerStatus.INVITED -> colors.warningOrange
        else -> colors.accentBlue
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .elasticHold(enabled = !isConnecting && peer.status != PeerStatus.INVITED, pressedScale = 0.97f)
            .clickable(
                enabled = !isConnecting && peer.status != PeerStatus.INVITED,
                role = Role.Button,
                onClick = onConnect
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CupertinoTheme.shapes.iconBadge)
                .background(badgeColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            CupertinoIcon(
                imageVector = Icons.Rounded.WifiTethering,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            CupertinoText(
                text = peer.deviceName.ifBlank { peer.deviceAddress },
                style = CupertinoTheme.typography.headline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val statusSubtitle = when (peer.status) {
                PeerStatus.CONNECTED -> stringResource(R.string.notif_connected, peer.deviceName)
                PeerStatus.INVITED -> stringResource(R.string.discover_connecting)
                PeerStatus.AVAILABLE -> peer.deviceAddress
                PeerStatus.FAILED, PeerStatus.UNAVAILABLE -> peer.deviceAddress
            }
            CupertinoText(
                text = statusSubtitle,
                style = CupertinoTheme.typography.footnote,
                color = colors.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        when {
            isConnecting || peer.status == PeerStatus.INVITED -> {
                CupertinoActivityIndicator(size = 20.dp, color = colors.accentBlue)
            }
            peer.status == PeerStatus.CONNECTED -> {
                CupertinoText(
                    text = stringResource(R.string.discover_connect),
                    style = CupertinoTheme.typography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accentGreen
                )
            }
            else -> {
                LiquidButton(
                    onClick = onConnect,
                    modifier = Modifier.testTag("connect_peer_button_${peer.deviceAddress}"),
                    height = 48.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    CupertinoText(
                        text = stringResource(R.string.discover_connect),
                        style = CupertinoTheme.typography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SavedDeviceRow(
    device: SavedDevice,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CupertinoTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .elasticHold(enabled = !isConnecting, pressedScale = 0.97f)
            .clickable(
                enabled = !isConnecting,
                role = Role.Button,
                onClick = onConnect
            )
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CupertinoTheme.shapes.iconBadge)
                .background(colors.accentCyan.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            CupertinoIcon(
                imageVector = Icons.Rounded.Devices,
                contentDescription = null,
                tint = colors.accentBlue,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            CupertinoText(
                text = device.effectiveName,
                style = CupertinoTheme.typography.headline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CupertinoIcon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = colors.tertiaryLabel,
                    modifier = Modifier.size(13.dp)
                )
                CupertinoText(
                    text = FormatUtils.formatRelativeTime(device.lastConnectedAtEpochMs),
                    style = CupertinoTheme.typography.footnote,
                    color = colors.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .elasticHold(pressedScale = 0.88f)
                    .clip(Capsule())
                    .clickable(
                        role = Role.Button,
                        onClick = { menuExpanded = true }
                    )
                    .testTag("saved_device_menu_${device.deviceAddress}"),
                contentAlignment = Alignment.Center
            ) {
                CupertinoIcon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = stringResource(R.string.saved_device_rename),
                    tint = colors.secondaryLabel
                )
            }

            LiquidActionPopover(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                items = listOf(
                    PopoverMenuItem(
                        label = stringResource(R.string.saved_device_rename),
                        icon = Icons.Rounded.Edit,
                        onClick = onRename
                    ),
                    PopoverMenuItem(
                        label = stringResource(R.string.saved_device_remove),
                        icon = Icons.Rounded.DeleteOutline,
                        isDestructive = true,
                        onClick = onRemove
                    )
                )
            )
        }
    }
}
