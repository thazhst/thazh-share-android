package com.thazh.share.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.data.DiscoveredPeer
import com.thazh.share.data.PeerStatus
import com.thazh.share.data.SavedDevice
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import com.thazh.share.util.FormatUtils
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioMenu
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem
import io.github.neoruaa.glasmio.ui.glasmioGroupedListSeparator

@Composable
fun DiscoveredPeerRow(
    peer: DiscoveredPeer,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    showTopSeparator: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val badgeColor = when (peer.status) {
        PeerStatus.CONNECTED -> semantic.accentGreen
        PeerStatus.INVITED -> semantic.warningOrange
        else -> semantic.accentBlue
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 68.dp)
            .glasmioGroupedListSeparator(colors.separator, visible = showTopSeparator, horizontalInset = 16.dp)
            .clickable(
                enabled = !isConnecting && peer.status != PeerStatus.INVITED,
                role = Role.Button,
                onClick = onConnect,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(ContinuousRoundedRectangle(12.dp))
                .background(badgeColor.copy(alpha = if (colors.isDark) 0.22f else 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            SfIcon(
                systemName = ThazhSymbols.WifiTethering,
                contentDescription = null,
                tint = badgeColor,
                size = 22.dp,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = peer.deviceName.ifBlank { peer.deviceAddress },
                style = GlasmioTheme.textStyles.headline,
                color = colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val statusSubtitle = when (peer.status) {
                PeerStatus.CONNECTED -> stringResource(R.string.notif_connected, peer.deviceName)
                PeerStatus.INVITED -> stringResource(R.string.discover_connecting)
                PeerStatus.AVAILABLE -> peer.deviceAddress
                PeerStatus.FAILED, PeerStatus.UNAVAILABLE -> peer.deviceAddress
            }
            Text(
                text = statusSubtitle,
                style = GlasmioTheme.textStyles.footnote,
                color = colors.secondaryContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        when {
            isConnecting || peer.status == PeerStatus.INVITED -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = colors.accent,
                    strokeWidth = 2.2.dp,
                )
            }
            peer.status == PeerStatus.CONNECTED -> {
                Text(
                    text = stringResource(R.string.discover_connect),
                    style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                    color = semantic.accentGreen,
                )
            }
            else -> {
                GlasmioButton(
                    onClick = onConnect,
                    emphasis = GlassEmphasis.Prominent,
                    size = GlasmioButtonSize.Medium,
                    modifier = Modifier.testTag("connect_peer_button_${peer.deviceAddress}"),
                ) {
                    Text(
                        text = stringResource(R.string.discover_connect),
                        style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
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
    showTopSeparator: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 68.dp)
            .glasmioGroupedListSeparator(colors.separator, visible = showTopSeparator, horizontalInset = 16.dp)
            .clickable(
                enabled = !isConnecting,
                role = Role.Button,
                onClick = onConnect,
            )
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(ContinuousRoundedRectangle(12.dp))
                .background(semantic.accentCyan.copy(alpha = if (colors.isDark) 0.22f else 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            SfIcon(
                systemName = ThazhSymbols.Devices,
                contentDescription = null,
                tint = colors.accent,
                size = 22.dp,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.effectiveName,
                style = GlasmioTheme.textStyles.headline,
                color = colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SfIcon(
                    systemName = ThazhSymbols.History,
                    contentDescription = null,
                    tint = colors.tertiaryContent,
                    size = 13.dp,
                )
                Text(
                    text = FormatUtils.formatRelativeTime(device.lastConnectedAtEpochMs),
                    style = GlasmioTheme.textStyles.footnote,
                    color = colors.secondaryContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        GlasmioMenu(
            expanded = menuExpanded,
            onExpandedChange = { menuExpanded = it },
            itemCount = 2,
            anchor = { onClick ->
                GlasmioIconButton(
                    onClick = onClick,
                    size = 48.dp,
                    modifier = Modifier.testTag("saved_device_menu_${device.deviceAddress}"),
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.MoreHoriz,
                        contentDescription = stringResource(R.string.saved_device_rename),
                        tint = colors.secondaryContent,
                        size = 20.dp,
                    )
                }
            },
        ) { _, close ->
            GlasmioMenuItem(
                title = stringResource(R.string.saved_device_rename),
                systemName = ThazhSymbols.Edit,
                onClick = {
                    close()
                    onRename()
                },
            )
            GlasmioMenuItem(
                title = stringResource(R.string.saved_device_remove),
                systemName = ThazhSymbols.Delete,
                destructive = true,
                onClick = {
                    close()
                    onRemove()
                },
            )
        }
    }
}
