package com.thazh.share.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.ui.screens.DiscoveryScreen
import com.thazh.share.ui.screens.FileTransferScreen
import com.thazh.share.ui.screens.ReceiveScreen
import com.thazh.share.ui.screens.ReceivedFilesScreen
import com.thazh.share.ui.screens.SettingsScreen
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.LocalBlurBackdrop
import io.github.neoruaa.glasmio.glass.LocalGlassBackdrop
import io.github.neoruaa.glasmio.glass.glassBackdropSource
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberCombinedBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.glass.trackBackdropPosition
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.CompactBottomControlSize
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioNavigationBar
import io.github.neoruaa.glasmio.ui.GlasmioTabItem
import io.github.neoruaa.glasmio.ui.NavigationBarSurfaceHeight

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
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic

    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val peers by viewModel.peers.collectAsStateWithLifecycle()
    val savedDevices by viewModel.savedDevices.collectAsStateWithLifecycle()
    val displayName by viewModel.displayName.collectAsStateWithLifecycle()
    val visibilityMode by viewModel.visibilityMode.collectAsStateWithLifecycle()
    val visibilityRemainingSeconds by viewModel.visibilityRemainingSeconds.collectAsStateWithLifecycle()
    val defaultVisibilityMode by viewModel.defaultVisibilityMode.collectAsStateWithLifecycle()
    val confirmIncomingConnection by viewModel.confirmIncomingConnection.collectAsStateWithLifecycle()
    val backgroundConnectionDialog by viewModel.backgroundConnectionDialog.collectAsStateWithLifecycle()
    val autoSaveToDownloads by viewModel.autoSaveToDownloads.collectAsStateWithLifecycle()
    val soundVibrationEnabled by viewModel.soundVibrationEnabled.collectAsStateWithLifecycle()
    val incomingConnectionRequest by viewModel.incomingConnectionRequest.collectAsStateWithLifecycle()
    val bleFastDiscoveryEnabled by viewModel.bleFastDiscoveryEnabled.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val reduceTransparency by viewModel.reduceTransparency.collectAsStateWithLifecycle()
    val liquidGlassEnabled by viewModel.liquidGlassEnabled.collectAsStateWithLifecycle()
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

    LaunchedEffect(openReceivedFilesRequested) {
        if (openReceivedFilesRequested) {
            viewModel.consumeOpenReceivedFilesRequest()
            selectedTab = ShareTab.RECEIVED_FILES
            if (currentRoute == ShareRoutes.SETTINGS) {
                navController.popBackStack(ShareRoutes.MAIN_TABS, inclusive = false)
            }
        }
    }

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

    val hostBackdrop = LocalGlassBackdrop.current
    val screenBackdrop = rememberLayerBackdrop()
    val screenModalBackdrop = rememberCombinedBackdrop(hostBackdrop, screenBackdrop)

    CompositionLocalProvider(LocalBlurBackdrop provides screenModalBackdrop) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassBackdropSource(screenBackdrop)
                .trackBackdropPosition(screenBackdrop),
        ) {
            NavHost(
                navController = navController,
                startDestination = ShareRoutes.MAIN_TABS,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(ShareRoutes.MAIN_TABS) {
                    BackHandler(enabled = selectedTab != ShareTab.SEND) {
                        selectedTab = ShareTab.SEND
                    }

                    val tabsContentBackdrop = rememberLayerBackdrop()
                    val tabsOverlayBackdrop = rememberCombinedBackdrop(hostBackdrop, tabsContentBackdrop)
                    val bottomBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                        NavigationBarSurfaceHeight + 24.dp
                    val compactProgress = remember { mutableFloatStateOf(0f) }

                    val openSettings = {
                        navController.navigate(ShareRoutes.SETTINGS) { launchSingleTop = true }
                    }

                    val sendLabel = stringResource(R.string.tab_send)
                    val receiveLabel = stringResource(R.string.tab_receive)
                    val receivedFilesLabel = if (unseenReceivedCount > 0) {
                        stringResource(R.string.tab_received_files_count, unseenReceivedCount)
                    } else {
                        stringResource(R.string.tab_received_files)
                    }

                    val tabItems = remember(sendLabel, receiveLabel, receivedFilesLabel) {
                        listOf(
                            GlasmioTabItem(
                                key = ShareTab.SEND,
                                label = sendLabel,
                                symbol = ThazhSymbols.TabSend,
                                contentDescription = sendLabel,
                            ),
                            GlasmioTabItem(
                                key = ShareTab.RECEIVE,
                                label = receiveLabel,
                                symbol = ThazhSymbols.TabReceive,
                                contentDescription = receiveLabel,
                            ),
                            GlasmioTabItem(
                                key = ShareTab.RECEIVED_FILES,
                                label = receivedFilesLabel,
                                symbol = ThazhSymbols.TabReceivedFiles,
                                contentDescription = receivedFilesLabel,
                            ),
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Modal overlays/menus inside MAIN_TABS read tabsOverlayBackdrop;
                        // in-page glass controls use LocalGlassBackdrop (= hostBackdrop) to avoid cyclic sampling.
                        CompositionLocalProvider(LocalBlurBackdrop provides tabsOverlayBackdrop) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .glassBackdropSource(tabsContentBackdrop)
                                    .trackBackdropPosition(tabsContentBackdrop),
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
                                                bottomBarPadding = bottomBarPadding,
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
                                                onBack = null,
                                            )
                                        } else {
                                            DiscoveryScreen(
                                                displayName = displayName,
                                                visibilityMode = visibilityMode,
                                                visibilityRemainingSeconds = visibilityRemainingSeconds,
                                                peers = peers,
                                                savedDevices = savedDevices,
                                                isDiscovering = isDiscovering,
                                                screenState = screenState,
                                                isNfcSupported = isNfcSupported,
                                                isNfcEnabled = isNfcEnabled,
                                                nfcPairingPeerName = nfcPairingPeerName,
                                                pendingWifiDirectFilesCount = pendingWifiDirectFilesCount,
                                                bottomBarPadding = bottomBarPadding,
                                                onCancelPendingFiles = { viewModel.clearPendingWifiDirectFiles() },
                                                onSelectVisibilityMode = { viewModel.setVisibilityMode(it) },
                                                onStartDiscovery = { viewModel.startDiscovery() },
                                                onStopDiscovery = { viewModel.stopDiscovery() },
                                                onConnectPeer = { viewModel.connectToPeer(it) },
                                                onConnectSavedDevice = { viewModel.connectToSavedDevice(it) },
                                                onRenameSavedDevice = { address, name -> viewModel.renameSavedDevice(address, name) },
                                                onRemoveSavedDevice = { viewModel.removeSavedDevice(it) },
                                                onOpenActiveSession = { selectedTab = ShareTab.SEND },
                                                onOpenNfcSettings = { viewModel.openNfcSettings() },
                                                onOpenSettings = openSettings,
                                            )
                                        }
                                    }

                                    ShareTab.RECEIVE -> {
                                        ReceiveScreen(
                                            displayName = displayName,
                                            visibilityMode = visibilityMode,
                                            visibilityRemainingSeconds = visibilityRemainingSeconds,
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
                                            bottomBarPadding = bottomBarPadding,
                                            onSelectVisibilityMode = { viewModel.setVisibilityMode(it) },
                                            onStartDiscovery = { viewModel.startDiscovery() },
                                            onOpenNfcSettings = { viewModel.openNfcSettings() },
                                            onCancelTransfer = { viewModel.cancelTransfer(it) },
                                            onRetryTransfer = { viewModel.retryTransfer(it) },
                                            onDismissTransfer = { viewModel.dismissTransfer(it) },
                                            onClearFinished = { viewModel.clearFinishedTransfers() },
                                            onDisconnect = { viewModel.disconnect() },
                                            onOpenSettings = openSettings,
                                        )
                                    }

                                    ShareTab.RECEIVED_FILES -> {
                                        ReceivedFilesScreen(
                                            files = receivedFiles,
                                            isSocketConnected = isSocketConnected,
                                            peers = peers,
                                            savedDevices = savedDevices,
                                            bottomBarPadding = bottomBarPadding,
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
                                            onSaveToDownloads = { targets, onDone -> viewModel.saveReceivedFilesToDownloads(targets, onDone) },
                                        )
                                    }
                                }
                            }
                        }

                        GlasmioNavigationBar(
                            items = tabItems,
                            selectedKey = selectedTab,
                            onSelected = { selectedTab = it },
                            onExpand = {},
                            compactProgress = compactProgress,
                            compactSize = CompactBottomControlSize,
                            backdrop = tabsOverlayBackdrop,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .widthIn(max = 420.dp)
                                .fillMaxWidth()
                                .testTag("glasmio_bottom_tabs"),
                        )
                    }
                }

                composable(ShareRoutes.SETTINGS) {
                    val totalReceivedBytes = remember(receivedFiles) { receivedFiles.sumOf { it.sizeBytes } }
                    SettingsScreen(
                        currentDisplayName = displayName,
                        visibilityMode = visibilityMode,
                        visibilityRemainingSeconds = visibilityRemainingSeconds,
                        defaultVisibilityMode = defaultVisibilityMode,
                        confirmIncomingConnection = confirmIncomingConnection,
                        backgroundConnectionDialog = backgroundConnectionDialog,
                        autoSaveToDownloads = autoSaveToDownloads,
                        soundVibrationEnabled = soundVibrationEnabled,
                        bleFastDiscoveryEnabled = bleFastDiscoveryEnabled,
                        isBleSupported = viewModel.isBleFastDiscoverySupported(),
                        isNfcSupported = isNfcSupported,
                        isNfcEnabled = isNfcEnabled,
                        themeMode = themeMode,
                        reduceTransparency = reduceTransparency,
                        liquidGlassEnabled = liquidGlassEnabled,
                        receivedFilesCount = receivedFiles.size,
                        receivedFilesTotalBytes = totalReceivedBytes,
                        savedDevicesCount = savedDevices.size,
                        onUpdateDisplayName = { viewModel.updateDisplayName(it) },
                        onSelectVisibilityMode = { viewModel.setVisibilityMode(it) },
                        onSelectDefaultVisibilityMode = { viewModel.setDefaultVisibilityMode(it) },
                        onToggleConfirmIncomingConnection = { viewModel.setConfirmIncomingConnection(it) },
                        onToggleBackgroundConnectionDialog = { viewModel.setBackgroundConnectionDialog(it) },
                        onToggleAutoSaveToDownloads = { viewModel.setAutoSaveToDownloads(it) },
                        onToggleSoundVibration = { viewModel.setSoundVibrationEnabled(it) },
                        onToggleBleFastDiscovery = { viewModel.setBleFastDiscoveryEnabled(it) },
                        onSelectThemeMode = { viewModel.setThemeMode(it) },
                        onToggleReduceTransparency = { viewModel.setReduceTransparency(it) },
                        onToggleLiquidGlass = { viewModel.setLiquidGlassEnabled(it) },
                        onOpenNfcSettings = { viewModel.openNfcSettings() },
                        onOpenReceivedFiles = {
                            selectedTab = ShareTab.RECEIVED_FILES
                            navController.popBackStack(ShareRoutes.MAIN_TABS, inclusive = false)
                        },
                        onClearTempStagingCache = {
                            viewModel.clearTempStagingCache {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.settings_clear_temp_cache_done),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        onClearSavedDevices = {
                            viewModel.clearSavedDevices {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.settings_clear_saved_devices_done),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }

    val pendingRequest = incomingConnectionRequest
    var lastPendingRequest by remember { mutableStateOf(pendingRequest) }
    if (pendingRequest != null) {
        lastPendingRequest = pendingRequest
    }
    val displayRequest = pendingRequest ?: lastPendingRequest
    GlasmioAlertDialog(
        visible = pendingRequest != null && displayRequest != null,
        onDismissRequest = { viewModel.declineIncomingConnection() },
        title = stringResource(R.string.incoming_conn_title),
        message = if (displayRequest != null) {
            if (displayRequest.pendingFileCount > 0) {
                stringResource(R.string.incoming_conn_message_files, displayRequest.peerName, displayRequest.pendingFileCount)
            } else {
                stringResource(R.string.incoming_conn_message, displayRequest.peerName)
            }
        } else null,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.incoming_conn_decline),
                onClick = { viewModel.declineIncomingConnection() },
                role = GlasmioAlertButtonRole.Destructive,
            ),
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.incoming_conn_accept),
                onClick = {
                    viewModel.acceptIncomingConnection()
                    selectedTab = ShareTab.RECEIVE
                },
                role = GlasmioAlertButtonRole.Default,
            ),
        ),
    ) {
        if (displayRequest != null &&
            (displayRequest.peerAddress.isNotBlank() || displayRequest.pendingFileNames.isNotEmpty())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (displayRequest.peerAddress.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ContinuousRoundedRectangle(12.dp))
                            .background(colors.secondaryContent.copy(alpha = if (colors.isDark) 0.14f else 0.08f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.Devices,
                            contentDescription = null,
                            tint = semantic.accentBlue,
                            size = 18.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.incoming_conn_device_address, displayRequest.peerAddress),
                            style = GlasmioTheme.textStyles.footnote,
                            color = colors.secondaryContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (displayRequest.pendingFileNames.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            R.string.incoming_conn_pending_files,
                            displayRequest.pendingFileCount,
                            displayRequest.pendingFileNames.take(3).joinToString(", "),
                        ),
                        style = GlasmioTheme.textStyles.footnote.copy(fontWeight = FontWeight.Medium),
                        color = colors.content,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}
