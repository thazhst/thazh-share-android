package com.thazh.share.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.thazh.share.R
import com.thazh.share.data.DiscoveredPeer
import com.thazh.share.data.FileCategory
import com.thazh.share.data.ReceivedFile
import com.thazh.share.data.RenameResult
import com.thazh.share.data.SavedDevice
import com.thazh.share.ui.components.FileTypeBadge
import com.thazh.share.ui.components.labelRes
import com.thazh.share.ui.cupertino.CupertinoDivider
import com.thazh.share.ui.cupertino.CupertinoFilterPill
import com.thazh.share.ui.cupertino.CupertinoIcon
import com.thazh.share.ui.cupertino.CupertinoSearchField
import com.thazh.share.ui.cupertino.CupertinoText
import com.thazh.share.ui.cupertino.CupertinoTextField
import com.thazh.share.ui.cupertino.CupertinoTopBar
import com.thazh.share.ui.cupertino.LiquidActionPopover
import com.thazh.share.ui.cupertino.LiquidAlertDialog
import com.thazh.share.ui.cupertino.LiquidButton
import com.thazh.share.ui.cupertino.LiquidCard
import com.thazh.share.ui.cupertino.LiquidIconButton
import com.thazh.share.ui.cupertino.PopoverMenuItem
import com.thazh.share.ui.cupertino.elasticHold
import com.thazh.share.ui.theme.CupertinoTheme
import com.thazh.share.ui.theme.LocalAppBackdrop
import com.thazh.share.util.FileActions
import com.thazh.share.util.FormatUtils
import java.io.File

private enum class ReceivedSortOrder { NEWEST, OLDEST, NAME, SIZE }

@Composable
fun ReceivedFilesScreen(
    files: List<ReceivedFile>,
    isSocketConnected: Boolean = false,
    peers: List<DiscoveredPeer> = emptyList(),
    savedDevices: List<SavedDevice> = emptyList(),
    onBack: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onMarkAllSeen: () -> Unit,
    onShareViaWifiDirect: (List<ReceivedFile>) -> Unit = {},
    onConnectPeerAndShareViaWifiDirect: (DiscoveredPeer, List<ReceivedFile>) -> Unit = { _, _ -> },
    onConnectSavedDeviceAndShareViaWifiDirect: (SavedDevice, List<ReceivedFile>) -> Unit = { _, _ -> },
    onDeleteFiles: (List<ReceivedFile>, onDone: (Int) -> Unit) -> Unit,
    onRenameFile: (ReceivedFile, String, onDone: (RenameResult) -> Unit) -> Unit,
    onSaveToDownloads: (List<ReceivedFile>, onDone: (Int, Int) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CupertinoTheme.colors

    DisposableEffect(Unit) {
        onDispose { onMarkAllSeen() }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<FileCategory?>(null) }
    var sortOrder by remember { mutableStateOf(ReceivedSortOrder.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }

    var isSelectionModeActive by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val inSelectionMode = isSelectionModeActive || selectedIds.isNotEmpty()

    LaunchedEffect(files) {
        val validIds = files.mapTo(HashSet()) { it.id }
        selectedIds = selectedIds.intersect(validIds)
        if (files.isEmpty()) {
            isSelectionModeActive = false
        }
    }

    val exitSelectionMode: () -> Unit = {
        isSelectionModeActive = false
        selectedIds = emptySet()
    }

    BackHandler(enabled = inSelectionMode) {
        exitSelectionMode()
    }

    var filesToDelete by remember { mutableStateOf<List<ReceivedFile>?>(null) }
    var filesPendingPeerPick by remember { mutableStateOf<List<ReceivedFile>?>(null) }
    var fileToRename by remember { mutableStateOf<ReceivedFile?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }
    var fileDetails by remember { mutableStateOf<ReceivedFile?>(null) }

    var pendingDownloadsExport by remember { mutableStateOf<List<ReceivedFile>?>(null) }
    val storagePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingDownloadsExport
        pendingDownloadsExport = null
        if (granted && pending != null) {
            onSaveToDownloads(pending) { saved, failed ->
                val text = when {
                    failed == 0 -> context.getString(R.string.received_files_saved, saved)
                    saved > 0 -> context.getString(R.string.received_files_save_partial, saved, failed)
                    else -> context.getString(R.string.received_files_save_failed)
                }
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }
        } else if (!granted) {
            Toast.makeText(
                context,
                context.getString(R.string.received_files_storage_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val triggerSaveToDownloads: (List<ReceivedFile>) -> Unit = { target ->
        if (target.isNotEmpty()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingDownloadsExport = target
                storagePermLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                onSaveToDownloads(target) { saved, failed ->
                    val text = when {
                        failed == 0 -> context.getString(R.string.received_files_saved, saved)
                        saved > 0 -> context.getString(R.string.received_files_save_partial, saved, failed)
                        else -> context.getString(R.string.received_files_save_failed)
                    }
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val triggerShareViaWifiDirect: (List<ReceivedFile>) -> Unit = { targets ->
        if (targets.isNotEmpty()) {
            if (isSocketConnected || (peers.isEmpty() && savedDevices.isEmpty())) {
                exitSelectionMode()
                onShareViaWifiDirect(targets)
            } else {
                filesPendingPeerPick = targets
            }
        }
    }

    val filteredFiles = remember(files, searchQuery, selectedCategory, sortOrder) {
        val query = searchQuery.trim().lowercase()
        val matched = files.filter { f ->
            val catOk = selectedCategory == null || f.category == selectedCategory
            val queryOk = query.isEmpty() ||
                f.fileName.lowercase().contains(query) ||
                f.peerName.lowercase().contains(query)
            catOk && queryOk
        }
        when (sortOrder) {
            ReceivedSortOrder.NEWEST -> matched.sortedByDescending { it.receivedAtEpochMs }
            ReceivedSortOrder.OLDEST -> matched.sortedBy { it.receivedAtEpochMs }
            ReceivedSortOrder.NAME -> matched.sortedBy { it.fileName.lowercase() }
            ReceivedSortOrder.SIZE -> matched.sortedByDescending { it.sizeBytes }
        }
    }

    val selectedFiles = remember(files, selectedIds) {
        files.filter { it.id in selectedIds }
    }

    val totalBytes = remember(files) { files.sumOf { it.sizeBytes } }
    val subtitle = if (filteredFiles.size == files.size) {
        stringResource(R.string.received_files_summary, files.size, FormatUtils.formatFileSize(totalBytes))
    } else {
        stringResource(R.string.received_files_summary_filtered, filteredFiles.size, files.size)
    }

    val availableCategories = remember(files) {
        FileCategory.entries.filter { cat -> files.any { it.category == cat } }
    }

    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    val topPadding = if (topBarHeightPx > 0) {
        with(density) { topBarHeightPx.toDp() } + 12.dp
    } else {
        108.dp
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
                    bottom = if (selectedFiles.isNotEmpty()) 176.dp else 112.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (files.isNotEmpty()) {
                    item(key = "search_header") {
                        CupertinoSearchField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = stringResource(R.string.received_files_search_hint),
                            modifier = Modifier.testTag("received_files_search_input")
                        )
                    }

                    if (availableCategories.size > 1) {
                        item(key = "category_filters") {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item(key = "all") {
                                    CupertinoFilterPill(
                                        label = stringResource(R.string.received_files_filter_all),
                                        selected = selectedCategory == null,
                                        onClick = { selectedCategory = null }
                                    )
                                }
                                items(availableCategories, key = { it.name }) { category ->
                                    CupertinoFilterPill(
                                        label = stringResource(category.labelRes()),
                                        selected = selectedCategory == category,
                                        onClick = {
                                            selectedCategory = if (selectedCategory == category) null else category
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (files.isEmpty()) {
                    item(key = "empty") {
                        LiquidCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 26.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CupertinoIcon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    tint = colors.tertiaryLabel,
                                    modifier = Modifier.size(38.dp)
                                )
                                CupertinoText(
                                    text = stringResource(R.string.received_files_empty_title),
                                    style = CupertinoTheme.typography.headline,
                                    textAlign = TextAlign.Center
                                )
                                CupertinoText(
                                    text = stringResource(R.string.received_files_empty_body),
                                    style = CupertinoTheme.typography.subheadline,
                                    color = colors.secondaryLabel,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (filteredFiles.isEmpty()) {
                    item(key = "no_match") {
                        LiquidCard(modifier = Modifier.fillMaxWidth()) {
                            CupertinoText(
                                text = stringResource(R.string.received_files_no_match),
                                style = CupertinoTheme.typography.subheadline,
                                color = colors.secondaryLabel,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp)
                            )
                        }
                    }
                } else {
                    item(key = "files_list_card") {
                        LiquidCard(
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredFiles.forEachIndexed { index, file ->
                                val isSelected = file.id in selectedIds
                                ReceivedFileRow(
                                    file = file,
                                    inSelectionMode = inSelectionMode,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (inSelectionMode) {
                                            selectedIds = if (isSelected) selectedIds - file.id else selectedIds + file.id
                                        } else {
                                            val f = File(file.filePath)
                                            if (!f.exists()) {
                                                Toast.makeText(context, context.getString(R.string.received_files_missing), Toast.LENGTH_SHORT).show()
                                            } else if (!FileActions.openFile(context, f, file.mimeType)) {
                                                Toast.makeText(context, context.getString(R.string.received_files_cannot_open), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        isSelectionModeActive = true
                                        selectedIds = selectedIds + file.id
                                    },
                                    onOpen = {
                                        val f = File(file.filePath)
                                        if (!f.exists()) {
                                            Toast.makeText(context, context.getString(R.string.received_files_missing), Toast.LENGTH_SHORT).show()
                                        } else if (!FileActions.openFile(context, f, file.mimeType)) {
                                            Toast.makeText(context, context.getString(R.string.received_files_cannot_open), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onShareWifiDirect = {
                                        triggerShareViaWifiDirect(listOf(file))
                                    },
                                    onShare = {
                                        FileActions.shareFiles(
                                            context,
                                            listOf(File(file.filePath) to file.mimeType),
                                            context.getString(R.string.received_files_share_chooser)
                                        )
                                    },
                                    onSave = { triggerSaveToDownloads(listOf(file)) },
                                    onRename = {
                                        fileToRename = file
                                        renameDraft = file.fileName
                                        renameError = null
                                    },
                                    onDetails = { fileDetails = file },
                                    onDelete = { filesToDelete = listOf(file) }
                                )
                                if (index < filteredFiles.lastIndex) {
                                    CupertinoDivider(startIndent = 74.dp)
                                }
                            }
                        }
                    }
                }
            }
        }

        CompositionLocalProvider(LocalAppBackdrop provides barBackdrop) {
            val topBarModifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { topBarHeightPx = it.height }
            if (inSelectionMode) {
                val allFilteredIds = remember(filteredFiles) { filteredFiles.map { it.id }.toSet() }
                val allSelected = allFilteredIds.isNotEmpty() && selectedIds.containsAll(allFilteredIds)
                CupertinoTopBar(
                    title = if (selectedIds.isEmpty()) {
                        stringResource(R.string.received_files_select_mode)
                    } else {
                        stringResource(R.string.received_files_selected_count, selectedIds.size)
                    },
                    onBack = { exitSelectionMode() },
                    backTestTag = "clear_selection_button",
                    modifier = topBarModifier,
                    actions = {
                        LiquidIconButton(
                            onClick = {
                                selectedIds = if (allSelected) {
                                    selectedIds - allFilteredIds
                                } else {
                                    selectedIds + allFilteredIds
                                }
                            },
                            icon = Icons.Rounded.SelectAll,
                            contentDescription = stringResource(R.string.received_files_select_all),
                            tint = if (allSelected) colors.accentBlue else colors.label,
                            modifier = Modifier.testTag("select_all_received_files_button")
                        )
                        LiquidIconButton(
                            onClick = { triggerShareViaWifiDirect(selectedFiles) },
                            enabled = selectedFiles.isNotEmpty(),
                            icon = Icons.Rounded.WifiTethering,
                            contentDescription = stringResource(R.string.received_files_share_wifi_direct),
                            tint = colors.accentBlue,
                            modifier = Modifier.testTag("top_bar_share_wifi_direct_button")
                        )
                        LiquidIconButton(
                            onClick = {
                                FileActions.shareFiles(
                                    context,
                                    selectedFiles.map { File(it.filePath) to it.mimeType },
                                    context.getString(R.string.received_files_share_chooser)
                                )
                            },
                            enabled = selectedFiles.isNotEmpty(),
                            icon = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.received_files_share)
                        )
                        LiquidIconButton(
                            onClick = { triggerSaveToDownloads(selectedFiles) },
                            enabled = selectedFiles.isNotEmpty(),
                            icon = Icons.Rounded.Download,
                            contentDescription = stringResource(R.string.received_files_save)
                        )
                        LiquidIconButton(
                            onClick = { filesToDelete = selectedFiles },
                            enabled = selectedFiles.isNotEmpty(),
                            icon = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.received_files_delete),
                            tint = colors.destructiveRed
                        )
                    }
                )
            } else {
                CupertinoTopBar(
                    title = stringResource(R.string.received_files_title),
                    subtitle = subtitle,
                    onBack = onBack,
                    backTestTag = "received_files_back_button",
                    modifier = topBarModifier,
                    actions = {
                        if (files.isNotEmpty()) {
                            LiquidIconButton(
                                onClick = { isSelectionModeActive = true },
                                icon = Icons.Rounded.Checklist,
                                contentDescription = stringResource(R.string.received_files_select_mode),
                                modifier = Modifier.testTag("received_files_enter_selection_button")
                            )
                        }
                        Box {
                            LiquidIconButton(
                                onClick = { showSortMenu = true },
                                icon = Icons.Rounded.Sort,
                                contentDescription = stringResource(R.string.received_files_sort),
                                modifier = Modifier.testTag("received_files_sort_button")
                            )
                            LiquidActionPopover(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                items = listOf(
                                    PopoverMenuItem(
                                        label = stringResource(R.string.received_files_sort_newest),
                                        isSelected = sortOrder == ReceivedSortOrder.NEWEST,
                                        onClick = { sortOrder = ReceivedSortOrder.NEWEST }
                                    ),
                                    PopoverMenuItem(
                                        label = stringResource(R.string.received_files_sort_oldest),
                                        isSelected = sortOrder == ReceivedSortOrder.OLDEST,
                                        onClick = { sortOrder = ReceivedSortOrder.OLDEST }
                                    ),
                                    PopoverMenuItem(
                                        label = stringResource(R.string.received_files_sort_name),
                                        isSelected = sortOrder == ReceivedSortOrder.NAME,
                                        onClick = { sortOrder = ReceivedSortOrder.NAME }
                                    ),
                                    PopoverMenuItem(
                                        label = stringResource(R.string.received_files_sort_size),
                                        isSelected = sortOrder == ReceivedSortOrder.SIZE,
                                        onClick = { sortOrder = ReceivedSortOrder.SIZE }
                                    )
                                )
                            )
                        }
                        if (onOpenSettings != null) {
                            LiquidIconButton(
                                onClick = onOpenSettings,
                                icon = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.settings_title),
                                modifier = Modifier.testTag("received_open_settings_button")
                            )
                        }
                    }
                )
            }

            if (selectedFiles.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, bottom = 92.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LiquidButton(
                        onClick = { triggerShareViaWifiDirect(selectedFiles) },
                        modifier = Modifier
                            .widthIn(min = 230.dp, max = 380.dp)
                            .fillMaxWidth(0.86f)
                            .testTag("share_wifi_direct_button"),
                        tint = colors.accentBlue,
                        height = 54.dp
                    ) {
                        CupertinoIcon(
                            imageVector = Icons.Rounded.WifiTethering,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CupertinoText(
                            text = stringResource(
                                R.string.received_files_share_wifi_direct_count,
                                selectedFiles.size
                            ),
                            style = CupertinoTheme.typography.headline,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    filesPendingPeerPick?.let { targets ->
        LiquidAlertDialog(
            onDismissRequest = { filesPendingPeerPick = null },
            title = stringResource(R.string.received_files_pick_peer_title, targets.size),
            confirmText = stringResource(R.string.received_files_open_send_tab),
            onConfirm = {
                filesPendingPeerPick = null
                exitSelectionMode()
                onShareViaWifiDirect(targets)
            },
            dismissText = stringResource(R.string.cancel)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CupertinoText(
                    text = stringResource(R.string.received_files_pick_peer_subtitle),
                    style = CupertinoTheme.typography.footnote,
                    color = colors.secondaryLabel
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    peers.take(4).forEach { peer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .elasticHold(pressedScale = 0.96f)
                                .clip(CupertinoTheme.shapes.input)
                                .background(colors.fillTertiary)
                                .clickable(role = Role.Button) {
                                    filesPendingPeerPick = null
                                    exitSelectionMode()
                                    onConnectPeerAndShareViaWifiDirect(peer, targets)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CupertinoIcon(
                                imageVector = Icons.Rounded.WifiTethering,
                                contentDescription = null,
                                tint = colors.accentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                CupertinoText(
                                    text = peer.deviceName.ifBlank { peer.deviceAddress },
                                    style = CupertinoTheme.typography.headline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CupertinoText(
                                    text = peer.deviceAddress,
                                    style = CupertinoTheme.typography.caption,
                                    color = colors.secondaryLabel,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    val peerAddresses = peers.mapTo(HashSet()) { it.deviceAddress }
                    savedDevices.filterNot { it.deviceAddress in peerAddresses }.take(3).forEach { saved ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .elasticHold(pressedScale = 0.96f)
                                .clip(CupertinoTheme.shapes.input)
                                .background(colors.fillTertiary)
                                .clickable(role = Role.Button) {
                                    filesPendingPeerPick = null
                                    exitSelectionMode()
                                    onConnectSavedDeviceAndShareViaWifiDirect(saved, targets)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CupertinoIcon(
                                imageVector = Icons.Rounded.Devices,
                                contentDescription = null,
                                tint = colors.accentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                CupertinoText(
                                    text = saved.effectiveName,
                                    style = CupertinoTheme.typography.headline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CupertinoText(
                                    text = saved.deviceAddress,
                                    style = CupertinoTheme.typography.caption,
                                    color = colors.secondaryLabel,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    filesToDelete?.let { targets ->
        LiquidAlertDialog(
            onDismissRequest = { filesToDelete = null },
            title = stringResource(R.string.received_files_delete_title),
            confirmText = stringResource(R.string.delete),
            confirmDestructive = true,
            onConfirm = {
                filesToDelete = null
                onDeleteFiles(targets) { removed ->
                    selectedIds = selectedIds - targets.map { it.id }.toSet()
                    if (selectedIds.isEmpty()) isSelectionModeActive = false
                    Toast.makeText(context, context.getString(R.string.received_files_deleted, removed), Toast.LENGTH_SHORT).show()
                }
            },
            dismissText = stringResource(R.string.cancel)
        ) {
            CupertinoText(
                text = stringResource(R.string.received_files_delete_body, targets.size),
                style = CupertinoTheme.typography.subheadline,
                color = colors.secondaryLabel
            )
        }
    }

    fileToRename?.let { target ->
        LiquidAlertDialog(
            onDismissRequest = { fileToRename = null },
            title = stringResource(R.string.received_files_rename_title),
            confirmText = stringResource(R.string.save),
            onConfirm = {
                onRenameFile(target, renameDraft) { result ->
                    when (result) {
                        RenameResult.SUCCESS -> {
                            fileToRename = null
                            Toast.makeText(context, context.getString(R.string.received_files_renamed), Toast.LENGTH_SHORT).show()
                        }
                        RenameResult.NAME_TAKEN -> renameError = context.getString(R.string.received_files_rename_taken)
                        RenameResult.INVALID_NAME -> renameError = context.getString(R.string.received_files_rename_invalid)
                        else -> renameError = context.getString(R.string.received_files_rename_failed)
                    }
                }
            },
            dismissText = stringResource(R.string.cancel)
        ) {
            CupertinoTextField(
                value = renameDraft,
                onValueChange = {
                    renameDraft = it
                    renameError = null
                },
                placeholder = target.fileName,
                errorText = renameError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onRenameFile(target, renameDraft) { result ->
                        when (result) {
                            RenameResult.SUCCESS -> fileToRename = null
                            RenameResult.NAME_TAKEN -> renameError = context.getString(R.string.received_files_rename_taken)
                            RenameResult.INVALID_NAME -> renameError = context.getString(R.string.received_files_rename_invalid)
                            else -> renameError = context.getString(R.string.received_files_rename_failed)
                        }
                    }
                })
            )
        }
    }

    fileDetails?.let { detail ->
        LiquidAlertDialog(
            onDismissRequest = { fileDetails = null },
            title = stringResource(R.string.received_files_details),
            dismissText = stringResource(R.string.close)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(stringResource(R.string.received_files_details_name), detail.fileName)
                DetailRow(stringResource(R.string.received_files_details_type), stringResource(detail.category.labelRes()))
                DetailRow(stringResource(R.string.received_files_details_size), FormatUtils.formatFileSize(detail.sizeBytes))
                DetailRow(
                    stringResource(R.string.received_files_details_from),
                    detail.peerName.ifBlank { stringResource(R.string.transfer_unknown_peer) }
                )
                DetailRow(stringResource(R.string.received_files_details_time), FormatUtils.formatFullDateTime(detail.receivedAtEpochMs))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        CupertinoText(
            text = label,
            style = CupertinoTheme.typography.caption,
            color = CupertinoTheme.colors.secondaryLabel
        )
        CupertinoText(
            text = value,
            style = CupertinoTheme.typography.subheadline
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReceivedFileRow(
    file: ReceivedFile,
    inSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOpen: () -> Unit,
    onShareWifiDirect: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onRename: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = CupertinoTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 68.dp)
            .elasticHold(pressedScale = 0.98f)
            .background(if (isSelected) colors.accentBlue.copy(alpha = 0.12f) else Color.Transparent)
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
            .testTag("received_file_row_${file.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (inSelectionMode) {
            CupertinoIcon(
                imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) colors.accentBlue else colors.tertiaryLabel,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        val showImageThumb = file.category == FileCategory.IMAGE && File(file.filePath).isFile
        if (showImageThumb) {
            AsyncImage(
                model = File(file.filePath),
                contentDescription = file.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CupertinoTheme.shapes.iconBadge)
            )
        } else {
            FileTypeBadge(category = file.category, size = 46.dp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CupertinoText(
                    text = file.fileName,
                    style = CupertinoTheme.typography.headline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!file.seen) {
                    Box(
                        modifier = Modifier
                            .clip(Capsule())
                            .background(colors.accentBlue.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        CupertinoText(
                            text = stringResource(R.string.received_files_new_badge),
                            style = CupertinoTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                            color = colors.accentBlue
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            val sender = file.peerName.ifBlank { stringResource(R.string.transfer_unknown_peer) }
            CupertinoText(
                text = "${FormatUtils.formatFileSize(file.sizeBytes)} • $sender • ${FormatUtils.formatRelativeTime(file.receivedAtEpochMs)}",
                style = CupertinoTheme.typography.footnote,
                color = colors.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!inSelectionMode) {
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .elasticHold(pressedScale = 0.88f)
                        .clip(Capsule())
                        .clickable(role = Role.Button, onClick = { menuExpanded = true }),
                    contentAlignment = Alignment.Center
                ) {
                    CupertinoIcon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = stringResource(R.string.received_files_details),
                        tint = colors.secondaryLabel
                    )
                }

                LiquidActionPopover(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    items = listOf(
                        PopoverMenuItem(stringResource(R.string.received_files_open), Icons.Rounded.OpenInNew, onClick = onOpen),
                        PopoverMenuItem(
                            stringResource(R.string.received_files_share_wifi_direct),
                            Icons.Rounded.WifiTethering,
                            onClick = onShareWifiDirect
                        ),
                        PopoverMenuItem(stringResource(R.string.received_files_share), Icons.Rounded.Share, onClick = onShare),
                        PopoverMenuItem(stringResource(R.string.received_files_save), Icons.Rounded.Download, onClick = onSave),
                        PopoverMenuItem(stringResource(R.string.received_files_rename), Icons.Rounded.Edit, onClick = onRename),
                        PopoverMenuItem(stringResource(R.string.received_files_details), Icons.Rounded.Info, onClick = onDetails),
                        PopoverMenuItem(
                            label = stringResource(R.string.received_files_delete),
                            icon = Icons.Rounded.DeleteOutline,
                            isDestructive = true,
                            onClick = onDelete
                        )
                    )
                )
            }
        }
    }
}
