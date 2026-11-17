package com.thazh.share.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.ui.components.GlasmioText
import com.thazh.share.ui.components.IosSwipeBackContainer
import com.thazh.share.ui.screens.AboutAppScreen
import com.thazh.share.ui.screens.DiscoveryScreen
import com.thazh.share.ui.screens.FileTransferScreen
import com.thazh.share.ui.screens.LinkShareScreen
import com.thazh.share.ui.screens.ReceiveScreen
import com.thazh.share.ui.screens.ReceivedFilesScreen
import com.thazh.share.ui.screens.SettingsScreen
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.swipeBackExcluded
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
    const val ABOUT_APP = "about_app"
}

enum class ShareTab(val index: Int) {
    SEND(0),
    RECEIVE(1),
    LINK_SHARE(2),
    RECEIVED_FILES(3)
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
    val syncDeviceNameWithAccount by viewModel.syncDeviceNameWithAccount.collectAsStateWithLifecycle()
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
    val selectedCatboxExpiry by viewModel.selectedCatboxExpiry.collectAsStateWithLifecycle()
    val isUploadingCatbox by viewModel.isUploadingCatbox.collectAsStateWithLifecycle()
    val catboxUploadProgress by viewModel.catboxUploadProgress.collectAsStateWithLifecycle()
    val catboxUploadError by viewModel.catboxUploadError.collectAsStateWithLifecycle()
    val sessionCatboxLinks by viewModel.sessionCatboxLinks.collectAsStateWithLifecycle()

    val isNfcSupported = remember { viewModel.isNfcSupported }
    val isBleSupported = remember { viewModel.isBleFastDiscoverySupported() }

    var selectedTab by rememberSaveable { mutableStateOf(ShareTab.SEND) }
    val tabHistory = rememberSaveable(
        saver = listSaver(
            save = { list -> list.map { it.name } },
            restore = { saved ->
                saved.mapNotNull { name -> runCatching { ShareTab.valueOf(name) }.getOrNull() }
                    .toMutableStateList()
            },
        ),
    ) {
        mutableStateListOf<ShareTab>()
    }

    fun navigateToTab(targetTab: ShareTab) {
        if (targetTab == selectedTab) return
        // Avoid unbounded duplicate cycles while preserving exact previous-page history
        if (tabHistory.lastOrNull() == targetTab) {
            tabHistory.removeAt(tabHistory.lastIndex)
        } else {
            tabHistory.add(selectedTab)
            if (tabHistory.size > 12) {
                tabHistory.removeAt(0)
            }
        }
        selectedTab = targetTab
    }

    fun popToPreviousTab() {
        val prev = tabHistory.removeLastOrNull()
        if (prev != null) {
            selectedTab = prev
        } else if (selectedTab != ShareTab.SEND) {
            selectedTab = ShareTab.SEND
        }
    }

    val previousTabInStack: ShareTab? = tabHistory.lastOrNull()
        ?: if (selectedTab != ShareTab.SEND) ShareTab.SEND else null

    var showTransferSessionOnSendTab by rememberSaveable { mutableStateOf(true) }
    val isConnectedSession = screenState is ConnectScreenState.Connected

    LaunchedEffect(isConnectedSession) {
        if (isConnectedSession) {
            showTransferSessionOnSendTab = true
        }
    }

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

    // Quick-send launchers for DiscoveryScreen (send immediately if connected, or queue & scan)
    val quickFilesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.sendOrQueueFilesForWifiDirect(uris) { sentImmediately, count ->
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
            }
        }
    }

    val quickMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.sendOrQueueFilesForWifiDirect(uris) { sentImmediately, count ->
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
            }
        }
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
            navigateToTab(ShareTab.RECEIVED_FILES)
            if (currentRoute == ShareRoutes.SETTINGS || currentRoute == ShareRoutes.ABOUT_APP) {
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
    val bottomBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        NavigationBarSurfaceHeight + 24.dp

    val openSettings = {
        navController.navigate(ShareRoutes.SETTINGS) { launchSingleTop = true }
    }

    @Composable
    fun RenderDiscoveryContent() {
        DiscoveryScreen(
            displayName = displayName,
            visibilityMode = visibilityMode,
            visibilityRemainingSeconds = visibilityRemainingSeconds,
            peers = peers,
            savedDevices = savedDevices,
            isDiscovering = isDiscovering,
            screenState = screenState,
            isBleSupported = isBleSupported,
            isBleEnabled = bleFastDiscoveryEnabled,
            isNfcSupported = isNfcSupported,
            isNfcEnabled = isNfcEnabled,
            nfcPairingPeerName = nfcPairingPeerName,
            pendingWifiDirectFilesCount = pendingWifiDirectFilesCount,
            bottomBarPadding = bottomBarPadding,
            onCancelPendingFiles = { viewModel.clearPendingWifiDirectFiles() },
            onQuickPickFiles = { quickFilesPickerLauncher.launch("*/*") },
            onQuickPickMedia = {
                quickMediaPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onOpenLinkShare = { navigateToTab(ShareTab.LINK_SHARE) },
            onToggleBleFastDiscovery = { viewModel.setBleFastDiscoveryEnabled(!bleFastDiscoveryEnabled) },
            onSelectVisibilityMode = { viewModel.setVisibilityMode(it) },
            onStartDiscovery = { viewModel.startDiscovery() },
            onStopDiscovery = { viewModel.stopDiscovery() },
            onConnectPeer = { viewModel.connectToPeer(it) },
            onConnectSavedDevice = { viewModel.connectToSavedDevice(it) },
            onRenameSavedDevice = { address, name -> viewModel.renameSavedDevice(address, name) },
            onRemoveSavedDevice = { viewModel.removeSavedDevice(it) },
            onOpenActiveSession = {
                navigateToTab(ShareTab.SEND)
                showTransferSessionOnSendTab = true
            },
            onOpenNfcSettings = { viewModel.openNfcSettings() },
            onOpenSettings = openSettings,
        )
    }

    @Composable
    fun RenderTabPageSurface(tab: ShareTab, allowInteractiveSwipe: Boolean) {
        when (tab) {
            ShareTab.SEND -> {
                val connectedState = screenState as? ConnectScreenState.Connected
                if (connectedState != null && showTransferSessionOnSendTab) {
                    val peerName = connectedPeerName ?: connectedState.peerName
                    if (allowInteractiveSwipe) {
                        BackHandler {
                            showTransferSessionOnSendTab = false
                        }
                    }
                    IosSwipeBackContainer(
                        enabled = allowInteractiveSwipe,
                        onBack = { showTransferSessionOnSendTab = false },
                        previousContent = { RenderDiscoveryContent() },
                    ) {
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
                            onBack = { showTransferSessionOnSendTab = false },
                        )
                    }
                } else {
                    RenderDiscoveryContent()
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
                    peers = peers,
                    transfers = transfers,
                    isBleSupported = isBleSupported,
                    isBleEnabled = bleFastDiscoveryEnabled,
                    isNfcSupported = isNfcSupported,
                    isNfcEnabled = isNfcEnabled,
                    nfcPairingPeerName = nfcPairingPeerName,
                    bottomBarPadding = bottomBarPadding,
                    onSelectVisibilityMode = { viewModel.setVisibilityMode(it) },
                    onStartDiscovery = { viewModel.startDiscovery() },
                    onConnectPeer = { viewModel.connectToPeer(it) },
                    onToggleBleFastDiscovery = { viewModel.setBleFastDiscoveryEnabled(!bleFastDiscoveryEnabled) },
                    onOpenNfcSettings = { viewModel.openNfcSettings() },
                    onCancelTransfer = { viewModel.cancelTransfer(it) },
                    onRetryTransfer = { viewModel.retryTransfer(it) },
                    onDismissTransfer = { viewModel.dismissTransfer(it) },
                    onClearFinished = { viewModel.clearFinishedTransfers() },
                    onDisconnect = { viewModel.disconnect() },
                    onOpenSettings = openSettings,
                )
            }

            ShareTab.LINK_SHARE -> {
                LinkShareScreen(
                    selectedExpiry = selectedCatboxExpiry,
                    isUploading = isUploadingCatbox,
                    uploadProgress = catboxUploadProgress,
                    uploadError = catboxUploadError,
                    sessionUploadedLinks = sessionCatboxLinks,
                    observeCloudLinks = { uid -> viewModel.observeCloudSharedLinks(uid) },
                    bottomBarPadding = bottomBarPadding,
                    onSelectExpiry = { viewModel.setCatboxExpiry(it) },
                    onUploadUris = { uris, expiry, note ->
                        viewModel.uploadFilesToCatbox(uris, expiry, note)
                    },
                    onUpdateCloudLinkNote = { uid, linkId, note ->
                        viewModel.updateCloudSharedLinkNote(uid, linkId, note)
                    },
                    onDeleteCloudLink = { uid, linkId ->
                        viewModel.deleteCloudSharedLink(uid, linkId)
                    },
                    onClearSessionLinks = { viewModel.clearSessionCatboxLinks() },
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
                                navigateToTab(ShareTab.SEND)
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
                                navigateToTab(ShareTab.SEND)
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
                                navigateToTab(ShareTab.SEND)
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

    @Composable
    fun RenderMainTabsBody(interactiveBackHandler: Boolean) {
        val connectedOnSendSession = selectedTab == ShareTab.SEND &&
            (screenState is ConnectScreenState.Connected) &&
            showTransferSessionOnSendTab
        val canPopTab = previousTabInStack != null && !connectedOnSendSession

        if (interactiveBackHandler) {
            BackHandler(enabled = canPopTab) {
                popToPreviousTab()
            }
        }

        val tabsContentBackdrop = rememberLayerBackdrop()
        val tabsOverlayBackdrop = rememberCombinedBackdrop(hostBackdrop, tabsContentBackdrop)
        val compactProgress = remember { mutableFloatStateOf(0f) }

        val sendLabel = stringResource(R.string.tab_send)
        val receiveLabel = stringResource(R.string.tab_receive)
        val linkShareLabel = stringResource(R.string.tab_link_share)
        val receivedFilesLabel = if (unseenReceivedCount > 0) {
            stringResource(R.string.tab_received_files_count, unseenReceivedCount)
        } else {
            stringResource(R.string.tab_received_files)
        }

        val tabItems = remember(sendLabel, receiveLabel, linkShareLabel, receivedFilesLabel) {
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
                    key = ShareTab.LINK_SHARE,
                    label = linkShareLabel,
                    symbol = ThazhSymbols.TabLinkShare,
                    contentDescription = linkShareLabel,
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
            CompositionLocalProvider(LocalBlurBackdrop provides tabsOverlayBackdrop) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .glassBackdropSource(tabsContentBackdrop)
                        .trackBackdropPosition(tabsContentBackdrop),
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (fadeIn(tween(200)) + scaleIn(initialScale = 0.985f, animationSpec = tween(220))) togetherWith
                                fadeOut(tween(150))
                        },
                        label = "main_tab_transition",
                    ) { tab ->
                        val prevTab = previousTabInStack
                        val tabSwipeEnabled = interactiveBackHandler && prevTab != null &&
                            !(tab == ShareTab.SEND && (screenState is ConnectScreenState.Connected) && showTransferSessionOnSendTab)

                        IosSwipeBackContainer(
                            enabled = tabSwipeEnabled,
                            onBack = { popToPreviousTab() },
                            previousContent = if (prevTab != null) {
                                { RenderTabPageSurface(tab = prevTab, allowInteractiveSwipe = false) }
                            } else {
                                null
                            },
                        ) {
                            RenderTabPageSurface(
                                tab = tab,
                                allowInteractiveSwipe = interactiveBackHandler,
                            )
                        }
                    }
                }
            }

            GlasmioNavigationBar(
                items = tabItems,
                selectedKey = selectedTab,
                onSelected = { navigateToTab(it) },
                onExpand = {},
                compactProgress = compactProgress,
                compactSize = CompactBottomControlSize,
                backdrop = tabsOverlayBackdrop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .swipeBackExcluded()
                    .testTag("glasmio_bottom_tabs"),
            )
        }
    }

    val iosSlideSpec = remember {
        spring<IntOffset>(dampingRatio = 0.88f, stiffness = 420f)
    }

    var isSettingsSubPageActive by rememberSaveable { mutableStateOf(false) }

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
                enterTransition = {
                    slideInHorizontally(iosSlideSpec) { it }
                },
                exitTransition = {
                    slideOutHorizontally(iosSlideSpec) { -(it * 0.30f).toInt() }
                },
                popEnterTransition = {
                    slideInHorizontally(iosSlideSpec) { -(it * 0.30f).toInt() }
                },
                popExitTransition = {
                    slideOutHorizontally(iosSlideSpec) { it }
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(ShareRoutes.MAIN_TABS) {
                    RenderMainTabsBody(interactiveBackHandler = true)
                }

                composable(ShareRoutes.SETTINGS) {
                    val totalReceivedBytes = remember(receivedFiles) { receivedFiles.sumOf { it.sizeBytes } }
                    // Disable outer swipe-back when inside a Settings category sub-page so swiping
                    // returns to the Settings hub first rather than jumping back to MAIN_TABS
                    IosSwipeBackContainer(
                        enabled = !isSettingsSubPageActive,
                        onBack = { navController.popBackStack() },
                        previousContent = { RenderMainTabsBody(interactiveBackHandler = false) },
                    ) {
                        SettingsScreen(
                            currentDisplayName = displayName,
                            syncDeviceNameWithAccount = syncDeviceNameWithAccount,
                            visibilityMode = visibilityMode,
                            visibilityRemainingSeconds = visibilityRemainingSeconds,
                            defaultVisibilityMode = defaultVisibilityMode,
                            confirmIncomingConnection = confirmIncomingConnection,
                            backgroundConnectionDialog = backgroundConnectionDialog,
                            autoSaveToDownloads = autoSaveToDownloads,
                            soundVibrationEnabled = soundVibrationEnabled,
                            bleFastDiscoveryEnabled = bleFastDiscoveryEnabled,
                            isBleSupported = isBleSupported,
                            isNfcSupported = isNfcSupported,
                            isNfcEnabled = isNfcEnabled,
                            themeMode = themeMode,
                            reduceTransparency = reduceTransparency,
                            liquidGlassEnabled = liquidGlassEnabled,
                            receivedFilesCount = receivedFiles.size,
                            receivedFilesTotalBytes = totalReceivedBytes,
                            savedDevicesCount = savedDevices.size,
                            onUpdateDisplayName = { viewModel.updateDisplayName(it) },
                            onToggleSyncDeviceNameWithAccount = { viewModel.setSyncDeviceNameWithAccount(it) },
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
                                navigateToTab(ShareTab.RECEIVED_FILES)
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
                            onOpenAboutApp = {
                                navController.navigate(ShareRoutes.ABOUT_APP) { launchSingleTop = true }
                            },
                            onSubPageActiveChange = { active ->
                                isSettingsSubPageActive = active
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }

                composable(ShareRoutes.ABOUT_APP) {
                    val totalReceivedBytes = remember(receivedFiles) { receivedFiles.sumOf { it.sizeBytes } }
                    IosSwipeBackContainer(
                        enabled = true,
                        onBack = { navController.popBackStack() },
                        previousContent = {
                            SettingsScreen(
                                currentDisplayName = displayName,
                                syncDeviceNameWithAccount = syncDeviceNameWithAccount,
                                visibilityMode = visibilityMode,
                                visibilityRemainingSeconds = visibilityRemainingSeconds,
                                defaultVisibilityMode = defaultVisibilityMode,
                                confirmIncomingConnection = confirmIncomingConnection,
                                backgroundConnectionDialog = backgroundConnectionDialog,
                                autoSaveToDownloads = autoSaveToDownloads,
                                soundVibrationEnabled = soundVibrationEnabled,
                                bleFastDiscoveryEnabled = bleFastDiscoveryEnabled,
                                isBleSupported = isBleSupported,
                                isNfcSupported = isNfcSupported,
                                isNfcEnabled = isNfcEnabled,
                                themeMode = themeMode,
                                reduceTransparency = reduceTransparency,
                                liquidGlassEnabled = liquidGlassEnabled,
                                receivedFilesCount = receivedFiles.size,
                                receivedFilesTotalBytes = totalReceivedBytes,
                                savedDevicesCount = savedDevices.size,
                                onUpdateDisplayName = {},
                                onToggleSyncDeviceNameWithAccount = {},
                                onSelectVisibilityMode = {},
                                onSelectDefaultVisibilityMode = {},
                                onToggleConfirmIncomingConnection = {},
                                onToggleBackgroundConnectionDialog = {},
                                onToggleAutoSaveToDownloads = {},
                                onToggleSoundVibration = {},
                                onToggleBleFastDiscovery = {},
                                onSelectThemeMode = {},
                                onToggleReduceTransparency = {},
                                onToggleLiquidGlass = {},
                                onOpenNfcSettings = {},
                                onOpenReceivedFiles = {},
                                onClearTempStagingCache = {},
                                onClearSavedDevices = {},
                                onOpenAboutApp = {},
                                onBack = {},
                            )
                        },
                    ) {
                        AboutAppScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
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
                    navigateToTab(ShareTab.RECEIVE)
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
                        GlasmioText(
                            text = stringResource(R.string.incoming_conn_device_address, displayRequest.peerAddress),
                            style = GlasmioTheme.textStyles.footnote,
                            color = colors.secondaryContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (displayRequest.pendingFileNames.isNotEmpty()) {
                    GlasmioText(
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
