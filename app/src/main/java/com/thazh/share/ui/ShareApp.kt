package com.thazh.share.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.thazh.share.R
import com.thazh.share.ui.cupertino.CupertinoIcon
import com.thazh.share.ui.cupertino.CupertinoText
import com.thazh.share.ui.cupertino.LiquidBottomTab
import com.thazh.share.ui.cupertino.LiquidBottomTabs
import com.thazh.share.ui.cupertino.LocalCupertinoContentColor
import com.thazh.share.ui.screens.DiscoveryScreen
import com.thazh.share.ui.screens.FileTransferScreen
import com.thazh.share.ui.screens.ReceiveScreen
import com.thazh.share.ui.screens.ReceivedFilesScreen
import com.thazh.share.ui.screens.SettingsScreen
import com.thazh.share.ui.theme.CupertinoTheme
import com.thazh.share.ui.theme.LocalAppBackdrop
import com.thazh.share.ui.theme.LocalOverlayBackdrop

object ShareRoutes {
    const val MAIN_TABS = "main_tabs"
    const val SETTINGS = "settings"
}

enum class ShareTab(val index: Int) {
    SEND(0),
    RECEIVE(1),
    RECEIVED_FILES(2)
}

@Composable
fun ShareApp(viewModel: ShareViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val isServiceReady by viewModel.isServiceReady.collectAsStateWithLifecycle()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val peers by viewModel.peers.collectAsStateWithLifecycle()
    val savedDevices by viewModel.savedDevices.collectAsStateWithLifecycle()
    val displayName by viewModel.displayName.collectAsStateWithLifecycle()
    val bleFastDiscoveryEnabled by viewModel.bleFastDiscoveryEnabled.collectAsStateWithLifecycle()
    val isDiscovering by viewModel.isDiscovering.collectAsStateWithLifecycle()
    val connectedPeerName by viewModel.connectedPeerName.collectAsStateWithLifecycle()
    val isSocketConnected by viewModel.isSocketConnected.collectAsStateWithLifecycle()
    val connectionQuality by viewModel.connectionQuality.collectAsStateWithLifecycle()
    val estimatedDistanceMeters by viewModel.estimatedDistanceMeters.collectAsStateWithLifecycle()
    val transfers by viewModel.transfers.collectAsStateWithLifecycle()
    val receivedFiles by viewModel.receivedFiles.collectAsStateWithLifecycle()
    val unseenReceivedCount by viewModel.unseenReceivedCount.collectAsStateWithLifecycle()
    val openReceivedFilesRequested by viewModel.openReceivedFilesRequested.collectAsStateWithLifecycle()
    val isNfcEnabled by viewModel.isNfcEnabled.collectAsStateWithLifecycle()
    val nfcPairingPeerName by viewModel.nfcPairingPeerName.collectAsStateWithLifecycle()
    val pendingWifiDirectFilesCount by viewModel.pendingWifiDirectFilesCount.collectAsStateWithLifecycle()
    val isNfcSupported = remember { viewModel.isNfcSupported }

    var selectedTab by rememberSaveable { mutableStateOf(ShareTab.SEND) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val filesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.sendFilesForTransfer(uris)
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.sendFilesForTransfer(uris)
    }

    // Auto-start P2P discovery once the foreground service finishes binding
    var didAutoScan by remember { mutableStateOf(false) }
    LaunchedEffect(isServiceReady) {
        if (isServiceReady && !didAutoScan) {
            didAutoScan = true
            viewModel.startDiscovery()
        }
    }

    // Subtle toast when an NFC tap exchanges credentials and kicks Wi-Fi Direct pairing
    LaunchedEffect(nfcPairingPeerName) {
        val peer = nfcPairingPeerName
        if (!peer.isNullOrBlank()) {
            Toast.makeText(
                context,
                context.getString(R.string.nfc_toast_paired, peer),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Deep-link from "Đã nhận tệp mới" notification
    LaunchedEffect(openReceivedFilesRequested) {
        if (openReceivedFilesRequested) {
            viewModel.consumeOpenReceivedFilesRequest()
            selectedTab = ShareTab.RECEIVED_FILES
            if (currentRoute == ShareRoutes.SETTINGS) {
                navController.popBackStack(ShareRoutes.MAIN_TABS, inclusive = false)
            }
        }
    }

    // Toast when a file finishes arriving while user is browsing other tabs
    LaunchedEffect(Unit) {
        viewModel.receivedFileEvents.collect { fileName ->
            val onMainTabs = navController.currentBackStackEntry?.destination?.route == ShareRoutes.MAIN_TABS
            val onReceiveOrFiles = onMainTabs && (selectedTab == ShareTab.RECEIVE || selectedTab == ShareTab.RECEIVED_FILES)
            if (!onReceiveOrFiles) {
                Toast.makeText(
                    context,
                    context.getString(R.string.transfer_toast_file_received, fileName),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val appBackdrop = LocalAppBackdrop.current
    val screenBackdrop = rememberLayerBackdrop()
    val overlayBackdrop = rememberCombinedBackdrop(appBackdrop, screenBackdrop)

    CompositionLocalProvider(LocalOverlayBackdrop provides overlayBackdrop) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(screenBackdrop)
        ) {
            NavHost(
                navController = navController,
                startDestination = ShareRoutes.MAIN_TABS,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(ShareRoutes.MAIN_TABS) {
                    BackHandler(enabled = selectedTab != ShareTab.SEND) {
                        selectedTab = ShareTab.SEND
                    }

                    val tabsContentBackdrop = rememberLayerBackdrop()
                    val bottomTabsBackdrop = rememberCombinedBackdrop(appBackdrop, tabsContentBackdrop)
                    val openSettings = {
                        navController.navigate(ShareRoutes.SETTINGS) { launchSingleTop = true }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .layerBackdrop(tabsContentBackdrop)
                        ) {
                            when (selectedTab) {
                                ShareTab.SEND -> {
                                    val connectedState = screenState as? ConnectScreenState.Connected
                                    if (connectedState != null) {
                                        val peerName = connectedPeerName ?: connectedState.peerName
                                        FileTransferScreen(
                                            peerName = peerName,
                                            isSocketConnected = isSocketConnected,
                                            connectionQuality = connectionQuality,
                                            estimatedDistanceMeters = estimatedDistanceMeters,
                                            transfers = transfers,
                                            onPickFiles = { filesPickerLauncher.launch("*/*") },
                                            onPickMedia = {
                                                mediaPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                                )
                                            },
                                            onCancelTransfer = { viewModel.cancelTransfer(it) },
                                            onRetryTransfer = { viewModel.retryTransfer(it) },
                                            onDismissTransfer = { viewModel.dismissTransfer(it) },
                                            onClearFinished = { viewModel.clearFinishedTransfers() },
                                            onDisconnect = { viewModel.disconnect() },
                                            onOpenSettings = openSettings,
                                            onBack = null
                                        )
                                    } else {
                                        DiscoveryScreen(
                                            displayName = displayName,
                                            peers = peers,
                                            savedDevices = savedDevices,
                                            isDiscovering = isDiscovering,
                                            screenState = screenState,
                                            isNfcSupported = isNfcSupported,
                                            isNfcEnabled = isNfcEnabled,
                                            nfcPairingPeerName = nfcPairingPeerName,
                                            pendingWifiDirectFilesCount = pendingWifiDirectFilesCount,
                                            onCancelPendingFiles = { viewModel.clearPendingWifiDirectFiles() },
                                            onStartDiscovery = { viewModel.startDiscovery() },
                                            onStopDiscovery = { viewModel.stopDiscovery() },
                                            onConnectPeer = { viewModel.connectToPeer(it) },
                                            onConnectSavedDevice = { viewModel.connectToSavedDevice(it) },
                                            onRenameSavedDevice = { address, name -> viewModel.renameSavedDevice(address, name) },
                                            onRemoveSavedDevice = { viewModel.removeSavedDevice(it) },
                                            onOpenActiveSession = { selectedTab = ShareTab.SEND },
                                            onOpenNfcSettings = { viewModel.openNfcSettings() },
                                            onOpenSettings = openSettings
                                        )
                                    }
                                }

                                ShareTab.RECEIVE -> {
                                    ReceiveScreen(
                                        displayName = displayName,
                                        isDiscovering = isDiscovering,
                                        screenState = screenState,
                                        connectedPeerName = connectedPeerName,
                                        isSocketConnected = isSocketConnected,
                                        connectionQuality = connectionQuality,
                                        estimatedDistanceMeters = estimatedDistanceMeters,
                                        transfers = transfers,
                                        isNfcSupported = isNfcSupported,
                                        isNfcEnabled = isNfcEnabled,
                                        nfcPairingPeerName = nfcPairingPeerName,
                                        onStartDiscovery = { viewModel.startDiscovery() },
                                        onOpenNfcSettings = { viewModel.openNfcSettings() },
                                        onCancelTransfer = { viewModel.cancelTransfer(it) },
                                        onRetryTransfer = { viewModel.retryTransfer(it) },
                                        onDismissTransfer = { viewModel.dismissTransfer(it) },
                                        onClearFinished = { viewModel.clearFinishedTransfers() },
                                        onDisconnect = { viewModel.disconnect() },
                                        onOpenSettings = openSettings
                                    )
                                }

                                ShareTab.RECEIVED_FILES -> {
                                    ReceivedFilesScreen(
                                        files = receivedFiles,
                                        isSocketConnected = isSocketConnected,
                                        peers = peers,
                                        savedDevices = savedDevices,
                                        onBack = null,
                                        onOpenSettings = openSettings,
                                        onMarkAllSeen = { viewModel.markReceivedFilesSeen() },
                                        onShareViaWifiDirect = { targets ->
                                            viewModel.shareReceivedFilesViaWifiDirect(targets) { sentImmediately, count ->
                                                if (count > 0) {
                                                    val msgRes = if (sentImmediately) {
                                                        R.string.received_files_share_wifi_direct_sending
                                                    } else {
                                                        R.string.received_files_share_wifi_direct_queued
                                                    }
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(msgRes, count),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    selectedTab = ShareTab.SEND
                                                }
                                            }
                                        },
                                        onConnectPeerAndShareViaWifiDirect = { peer, targets ->
                                            viewModel.shareReceivedFilesViaWifiDirect(targets) { _, count ->
                                                if (count > 0) {
                                                    viewModel.connectToPeer(peer)
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.received_files_share_wifi_direct_sending, count),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    selectedTab = ShareTab.SEND
                                                }
                                            }
                                        },
                                        onConnectSavedDeviceAndShareViaWifiDirect = { saved, targets ->
                                            viewModel.shareReceivedFilesViaWifiDirect(targets) { _, count ->
                                                if (count > 0) {
                                                    viewModel.connectToSavedDevice(saved)
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.received_files_share_wifi_direct_sending, count),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    selectedTab = ShareTab.SEND
                                                }
                                            }
                                        },
                                        onDeleteFiles = { targets, onDone -> viewModel.deleteReceivedFiles(targets, onDone) },
                                        onRenameFile = { file, name, onDone -> viewModel.renameReceivedFile(file, name, onDone) },
                                        onSaveToDownloads = { targets, onDone -> viewModel.saveReceivedFilesToDownloads(targets, onDone) }
                                    )
                                }
                            }
                        }

                        LiquidBottomTabs(
                            selectedTabIndex = { selectedTab.index },
                            onTabSelected = { index ->
                                selectedTab = ShareTab.entries.getOrElse(index) { ShareTab.SEND }
                            },
                            backdrop = bottomTabsBackdrop,
                            tabsCount = 3,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .widthIn(max = 420.dp)
                                .fillMaxWidth()
                                .testTag("liquid_bottom_tabs")
                        ) {
                            ShareBottomTabItem(
                                label = stringResource(R.string.tab_send),
                                icon = Icons.Rounded.ArrowUpward,
                                selected = selectedTab == ShareTab.SEND,
                                onClick = { selectedTab = ShareTab.SEND },
                                modifier = Modifier.testTag("tab_send")
                            )
                            ShareBottomTabItem(
                                label = stringResource(R.string.tab_receive),
                                icon = Icons.Rounded.ArrowDownward,
                                selected = selectedTab == ShareTab.RECEIVE,
                                onClick = { selectedTab = ShareTab.RECEIVE },
                                modifier = Modifier.testTag("tab_receive")
                            )
                            ShareBottomTabItem(
                                label = stringResource(R.string.tab_received_files),
                                icon = Icons.Rounded.FolderOpen,
                                selected = selectedTab == ShareTab.RECEIVED_FILES,
                                badgeCount = unseenReceivedCount,
                                onClick = { selectedTab = ShareTab.RECEIVED_FILES },
                                modifier = Modifier.testTag("tab_received_files")
                            )
                        }
                    }
                }

                composable(ShareRoutes.SETTINGS) {
                    SettingsScreen(
                        currentDisplayName = displayName,
                        bleFastDiscoveryEnabled = bleFastDiscoveryEnabled,
                        isBleSupported = viewModel.isBleFastDiscoverySupported(),
                        isNfcSupported = isNfcSupported,
                        isNfcEnabled = isNfcEnabled,
                        onUpdateDisplayName = { viewModel.updateDisplayName(it) },
                        onToggleBleFastDiscovery = { viewModel.setBleFastDiscoveryEnabled(it) },
                        onOpenNfcSettings = { viewModel.openNfcSettings() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ShareBottomTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    val colors = CupertinoTheme.colors
    val refractedTint = LocalCupertinoContentColor.current
    val tintColor = when {
        refractedTint != Color.Unspecified -> refractedTint
        selected -> colors.accentBlue
        else -> colors.label
    }

    LiquidBottomTab(
        onClick = onClick,
        badgeCount = badgeCount,
        modifier = modifier
    ) {
        CupertinoIcon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(22.dp)
        )
        CupertinoText(
            text = label,
            style = CupertinoTheme.typography.caption.copy(
                fontWeight = if (selected || refractedTint != Color.Unspecified) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = tintColor,
            maxLines = 1
        )
    }
}
