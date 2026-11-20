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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.thazh.share.ui.components.GlasmioText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.data.DiscoveredPeer
import com.thazh.share.data.FileCategory
import com.thazh.share.data.ReceivedFile
import com.thazh.share.data.RenameResult
import com.thazh.share.data.SavedDevice
import com.thazh.share.ui.components.FileTypeBadge
import com.thazh.share.ui.components.labelRes
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import com.thazh.share.util.FileActions
import com.thazh.share.util.FormatUtils
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.swipeBackExcluded
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioAlertFieldGroup
import io.github.neoruaa.glasmio.ui.GlasmioAlertTextField
import io.github.neoruaa.glasmio.ui.GlasmioBottomSheet
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.ui.GlasmioMenu
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem
import io.github.neoruaa.glasmio.ui.GlasmioScrollableTabRow
import io.github.neoruaa.glasmio.ui.GlasmioSearchBar
import io.github.neoruaa.glasmio.ui.GlasmioSheetToolbarButton
import io.github.neoruaa.glasmio.ui.GlasmioSheetTopToolbar
import io.github.neoruaa.glasmio.ui.LocalGlasmioSheetContentPadding
import io.github.neoruaa.glasmio.ui.glasmioGroupedListSeparator
import java.io.File

private enum class ReceivedSortOrder { NEWEST, OLDEST, NAME, SIZE }

@Composable
fun ReceivedFilesScreen(
    files: List<ReceivedFile>,
    isSocketConnected: Boolean = false,
    peers: List<DiscoveredPeer> = emptyList(),
    savedDevices: List<SavedDevice> = emptyList(),
    bottomBarPadding: Dp = 96.dp,
    onBack: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onMarkAllSeen: () -> Unit,
    onShareViaWifiDirect: (List<ReceivedFile>) -> Unit = {},
    onConnectPeerAndShareViaWifiDirect: (DiscoveredPeer, List<ReceivedFile>) -> Unit = { _, _ -> },
    onConnectSavedDeviceAndShareViaWifiDirect: (SavedDevice, List<ReceivedFile>) -> Unit = { _, _ -> },
    onDeleteFiles: (List<ReceivedFile>, onDone: (Int) -> Unit) -> Unit,
    onRenameFile: (ReceivedFile, String, onDone: (RenameResult) -> Unit) -> Unit,
    onSaveToDownloads: (List<ReceivedFile>, onDone: (Int, Int) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = GlasmioTheme.colors
    val appearance = GlasmioTheme.appearance
    val semantic = ThazhTheme.semantic

    DisposableEffect(Unit) {
        onDispose { onMarkAllSeen() }
    }

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var searchActive by remember { mutableStateOf(false) }
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
    var lastDeleteTargets by remember { mutableStateOf<List<ReceivedFile>>(emptyList()) }
    if (filesToDelete != null) {
        lastDeleteTargets = filesToDelete.orEmpty()
    }

    var filesPendingPeerPick by remember { mutableStateOf<List<ReceivedFile>?>(null) }
    var lastPeerPickTargets by remember { mutableStateOf<List<ReceivedFile>>(emptyList()) }
    if (filesPendingPeerPick != null) {
        lastPeerPickTargets = filesPendingPeerPick.orEmpty()
    }

    var fileToRename by remember { mutableStateOf<ReceivedFile?>(null) }
    var lastRenameFile by remember { mutableStateOf<ReceivedFile?>(null) }
    if (fileToRename != null) {
        lastRenameFile = fileToRename
    }
    var renameDraft by remember { mutableStateOf(TextFieldValue("")) }
    var renameError by remember { mutableStateOf<String?>(null) }

    var fileDetails by remember { mutableStateOf<ReceivedFile?>(null) }
    var lastFileDetails by remember { mutableStateOf<ReceivedFile?>(null) }
    if (fileDetails != null) {
        lastFileDetails = fileDetails
    }

    var pendingDownloadsExport by remember { mutableStateOf<List<ReceivedFile>?>(null) }
    val storagePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
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
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val triggerSaveToDownloads: (List<ReceivedFile>) -> Unit = { target ->
        if (target.isNotEmpty()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) != PackageManager.PERMISSION_GRANTED
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

    val filteredFiles = remember(files, searchQuery.text, selectedCategory, sortOrder) {
        val query = searchQuery.text.trim().lowercase()
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

    val allLabel = stringResource(R.string.received_files_filter_all)
    val categoryTabs = remember(availableCategories, allLabel) {
        buildList<Pair<FileCategory?, String>> {
            add(null to allLabel)
            availableCategories.forEach { cat ->
                add(cat to context.getString(cat.labelRes()))
            }
        }
    }

    val topBarTitle = if (inSelectionMode) {
        if (selectedIds.isEmpty()) {
            stringResource(R.string.received_files_select_mode)
        } else {
            stringResource(R.string.received_files_selected_count, selectedIds.size)
        }
    } else {
        stringResource(R.string.received_files_title)
    }

    val allFilteredIds = remember(filteredFiles) { filteredFiles.map { it.id }.toSet() }
    val allSelected = allFilteredIds.isNotEmpty() && selectedIds.containsAll(allFilteredIds)

    Box(modifier = modifier.fillMaxSize()) {
        GlasmioPinnedListPage(
            title = topBarTitle,
            subtitle = if (inSelectionMode) null else subtitle,
            onNavigateBack = if (inSelectionMode) exitSelectionMode else onBack,
            navigateBackContentDescription = stringResource(
                if (inSelectionMode) R.string.cancel else R.string.back,
            ),
            bottomPadding = bottomBarPadding + if (selectedFiles.isNotEmpty()) 72.dp else 0.dp,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            backgroundColor = colors.groupedBackground.copy(
                alpha = if (appearance.reduceTransparency) 1f else 0.78f,
            ),
            modifier = Modifier.fillMaxSize(),
            actions = {
                if (inSelectionMode) {
                    GlasmioIconButton(
                        onClick = {
                            selectedIds = if (allSelected) {
                                selectedIds - allFilteredIds
                            } else {
                                selectedIds + allFilteredIds
                            }
                        },
                        modifier = Modifier.testTag("select_all_received_files_button"),
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.SelectAll,
                            contentDescription = stringResource(R.string.received_files_select_all),
                            tint = if (allSelected) colors.accent else colors.content,
                        )
                    }
                    val hasSelection = selectedFiles.isNotEmpty()
                    GlasmioIconButton(
                        onClick = { triggerShareViaWifiDirect(selectedFiles) },
                        enabled = hasSelection,
                        modifier = Modifier.testTag("top_bar_share_wifi_direct_button"),
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.WifiTethering,
                            contentDescription = stringResource(R.string.received_files_share_wifi_direct),
                            tint = colors.accent.copy(alpha = if (hasSelection) 1f else 0.35f),
                        )
                    }
                    GlasmioIconButton(
                        onClick = {
                            FileActions.shareFiles(
                                context,
                                selectedFiles.map { File(it.filePath) to it.mimeType },
                                context.getString(R.string.received_files_share_chooser),
                            )
                        },
                        enabled = hasSelection,
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.Share,
                            contentDescription = stringResource(R.string.received_files_share),
                            tint = colors.content.copy(alpha = if (hasSelection) 1f else 0.35f),
                        )
                    }
                    GlasmioIconButton(
                        onClick = { triggerSaveToDownloads(selectedFiles) },
                        enabled = hasSelection,
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.Download,
                            contentDescription = stringResource(R.string.received_files_save),
                            tint = colors.content.copy(alpha = if (hasSelection) 1f else 0.35f),
                        )
                    }
                    GlasmioIconButton(
                        onClick = { filesToDelete = selectedFiles },
                        enabled = hasSelection,
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.Delete,
                            contentDescription = stringResource(R.string.received_files_delete),
                            tint = colors.destructive.copy(alpha = if (hasSelection) 1f else 0.35f),
                        )
                    }
                } else {
                    if (files.isNotEmpty()) {
                        GlasmioIconButton(
                            onClick = { isSelectionModeActive = true },
                            modifier = Modifier.testTag("received_files_enter_selection_button"),
                        ) {
                            SfIcon(
                                systemName = ThazhSymbols.Checklist,
                                contentDescription = stringResource(R.string.received_files_select_mode),
                            )
                        }
                    }
                    GlasmioMenu(
                        expanded = showSortMenu,
                        onExpandedChange = { showSortMenu = it },
                        itemCount = 4,
                        anchor = { onClick ->
                            GlasmioIconButton(
                                onClick = onClick,
                                modifier = Modifier.testTag("received_files_sort_button"),
                            ) {
                                SfIcon(
                                    systemName = ThazhSymbols.Sort,
                                    contentDescription = stringResource(R.string.received_files_sort),
                                )
                            }
                        },
                    ) { _, close ->
                        GlasmioMenuItem(
                            title = stringResource(R.string.received_files_sort_newest),
                            systemName = if (sortOrder == ReceivedSortOrder.NEWEST) "checkmark" else null,
                            onClick = {
                                sortOrder = ReceivedSortOrder.NEWEST
                                close()
                            },
                        )
                        GlasmioMenuItem(
                            title = stringResource(R.string.received_files_sort_oldest),
                            systemName = if (sortOrder == ReceivedSortOrder.OLDEST) "checkmark" else null,
                            onClick = {
                                sortOrder = ReceivedSortOrder.OLDEST
                                close()
                            },
                        )
                        GlasmioMenuItem(
                            title = stringResource(R.string.received_files_sort_name),
                            systemName = if (sortOrder == ReceivedSortOrder.NAME) "checkmark" else null,
                            onClick = {
                                sortOrder = ReceivedSortOrder.NAME
                                close()
                            },
                        )
                        GlasmioMenuItem(
                            title = stringResource(R.string.received_files_sort_size),
                            systemName = if (sortOrder == ReceivedSortOrder.SIZE) "checkmark" else null,
                            onClick = {
                                sortOrder = ReceivedSortOrder.SIZE
                                close()
                            },
                        )
                    }
                    if (onOpenSettings != null) {
                        GlasmioIconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("received_open_settings_button"),
                        ) {
                            SfIcon(
                                systemName = ThazhSymbols.Settings,
                                contentDescription = stringResource(R.string.settings_title),
                            )
                        }
                    }
                }
            },
        ) {
            if (files.isNotEmpty()) {
                item(key = "search_header") {
                    CenteredReceivedContent {
                        GlasmioSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            active = searchActive,
                            onActiveChange = { searchActive = it },
                            onClose = {
                                searchQuery = TextFieldValue("")
                                searchActive = false
                            },
                            placeholder = stringResource(R.string.received_files_search_hint),
                            closeContentDescription = stringResource(R.string.close),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("received_files_search_input"),
                        )
                    }
                }

                if (availableCategories.size > 1) {
                    item(key = "category_filters") {
                        CenteredReceivedContent {
                            GlasmioScrollableTabRow(
                                items = categoryTabs,
                                selected = selectedCategory,
                                onSelected = { selectedCategory = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .swipeBackExcluded(),
                            )
                        }
                    }
                }
            }

            if (files.isEmpty()) {
                item(key = "empty") {
                    CenteredReceivedContent {
                        GlasmioCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SfIcon(
                                    systemName = ThazhSymbols.FolderOpen,
                                    contentDescription = null,
                                    tint = colors.tertiaryContent,
                                    size = 38.dp,
                                )
                                GlasmioText(
                                    text = stringResource(R.string.received_files_empty_title),
                                    style = GlasmioTheme.textStyles.headline,
                                    color = colors.content,
                                    textAlign = TextAlign.Center,
                                )
                                GlasmioText(
                                    text = stringResource(R.string.received_files_empty_body),
                                    style = GlasmioTheme.textStyles.subheadline,
                                    color = colors.secondaryContent,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            } else if (filteredFiles.isEmpty()) {
                item(key = "no_match") {
                    CenteredReceivedContent {
                        GlasmioCard(modifier = Modifier.fillMaxWidth()) {
                            GlasmioText(
                                text = stringResource(R.string.received_files_no_match),
                                style = GlasmioTheme.textStyles.subheadline,
                                color = colors.secondaryContent,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 22.dp, horizontal = 16.dp),
                            )
                        }
                    }
                }
            } else {
                item(key = "files_list_card") {
                    CenteredReceivedContent {
                        GlasmioGroupedList(modifier = Modifier.fillMaxWidth()) {
                            filteredFiles.forEachIndexed { index, file ->
                                val isSelected = file.id in selectedIds
                                ReceivedFileRow(
                                    file = file,
                                    inSelectionMode = inSelectionMode,
                                    isSelected = isSelected,
                                    showTopSeparator = index > 0,
                                    onClick = {
                                        if (inSelectionMode) {
                                            selectedIds = if (isSelected) {
                                                selectedIds - file.id
                                            } else {
                                                selectedIds + file.id
                                            }
                                        } else if (file.isTextTransferFile()) {
                                            // Open the in-app text viewer & Copy to clipboard window directly
                                            fileDetails = file
                                        } else {
                                            val f = File(file.filePath)
                                            if (!f.exists()) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.received_files_missing),
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            } else if (!FileActions.openFile(context, f, file.mimeType)) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.received_files_cannot_open),
                                                    Toast.LENGTH_SHORT,
                                                ).show()
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
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.received_files_missing),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        } else if (!FileActions.openFile(context, f, file.mimeType)) {
                                            if (file.isTextTransferFile()) {
                                                fileDetails = file
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.received_files_cannot_open),
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                        }
                                    },
                                    onCopyText = if (file.isTextTransferFile()) {
                                        { copyReceivedTextFileToClipboard(context, file) }
                                    } else {
                                        null
                                    },
                                    onShareWifiDirect = { triggerShareViaWifiDirect(listOf(file)) },
                                    onShare = {
                                        FileActions.shareFiles(
                                            context,
                                            listOf(File(file.filePath) to file.mimeType),
                                            context.getString(R.string.received_files_share_chooser),
                                        )
                                    },
                                    onSave = { triggerSaveToDownloads(listOf(file)) },
                                    onRename = {
                                        renameDraft = TextFieldValue(
                                            file.fileName,
                                            TextRange(file.fileName.length),
                                        )
                                        renameError = null
                                        fileToRename = file
                                    },
                                    onDetails = { fileDetails = file },
                                    onDelete = { filesToDelete = listOf(file) },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedFiles.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, end = 24.dp, bottom = bottomBarPadding),
                contentAlignment = Alignment.Center,
            ) {
                GlasmioButton(
                    onClick = { triggerShareViaWifiDirect(selectedFiles) },
                    emphasis = GlassEmphasis.Prominent,
                    navigationSurfaceColor = semantic.accentBlue,
                    size = GlasmioButtonSize.Large,
                    modifier = Modifier
                        .widthIn(min = 230.dp, max = 380.dp)
                        .fillMaxWidth(0.86f)
                        .testTag("share_wifi_direct_button"),
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.WifiTethering,
                        contentDescription = null,
                        tint = Color.White,
                        size = 20.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    GlasmioText(
                        text = stringResource(
                            R.string.received_files_share_wifi_direct_count,
                            selectedFiles.size,
                        ),
                        style = GlasmioTheme.textStyles.headline,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    val peerPickTargets = lastPeerPickTargets
    PeerPickerBottomSheet(
        visible = filesPendingPeerPick != null && peerPickTargets.isNotEmpty(),
        fileCount = peerPickTargets.size,
        peers = peers,
        savedDevices = savedDevices,
        onPickPeer = { peer ->
            filesPendingPeerPick = null
            exitSelectionMode()
            onConnectPeerAndShareViaWifiDirect(peer, peerPickTargets)
        },
        onPickSavedDevice = { saved ->
            filesPendingPeerPick = null
            exitSelectionMode()
            onConnectSavedDeviceAndShareViaWifiDirect(saved, peerPickTargets)
        },
        onOpenSendTab = {
            filesPendingPeerPick = null
            exitSelectionMode()
            onShareViaWifiDirect(peerPickTargets)
        },
        onDismiss = { filesPendingPeerPick = null },
    )

    val deleteTargets = lastDeleteTargets
    GlasmioAlertDialog(
        visible = filesToDelete != null && deleteTargets.isNotEmpty(),
        onDismissRequest = { filesToDelete = null },
        title = stringResource(R.string.received_files_delete_title),
        message = stringResource(R.string.received_files_delete_body, deleteTargets.size),
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.cancel),
                onClick = { filesToDelete = null },
                role = GlasmioAlertButtonRole.Cancel,
            ),
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.delete),
                onClick = {
                    filesToDelete = null
                    onDeleteFiles(deleteTargets) { removed ->
                        selectedIds = selectedIds - deleteTargets.map { it.id }.toSet()
                        if (selectedIds.isEmpty()) isSelectionModeActive = false
                        Toast.makeText(
                            context,
                            context.getString(R.string.received_files_deleted, removed),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                role = GlasmioAlertButtonRole.Destructive,
            ),
        ),
    )

    val renameTarget = lastRenameFile
    val submitRename: () -> Unit = {
        val target = renameTarget
        if (target != null) {
            onRenameFile(target, renameDraft.text) { result ->
                when (result) {
                    RenameResult.SUCCESS -> {
                        fileToRename = null
                        Toast.makeText(
                            context,
                            context.getString(R.string.received_files_renamed),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    RenameResult.NAME_TAKEN -> renameError = context.getString(R.string.received_files_rename_taken)
                    RenameResult.INVALID_NAME -> renameError = context.getString(R.string.received_files_rename_invalid)
                    else -> renameError = context.getString(R.string.received_files_rename_failed)
                }
            }
        }
    }
    GlasmioAlertDialog(
        visible = fileToRename != null && renameTarget != null,
        onDismissRequest = { fileToRename = null },
        title = stringResource(R.string.received_files_rename_title),
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.cancel),
                onClick = { fileToRename = null },
                role = GlasmioAlertButtonRole.Cancel,
            ),
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.save),
                onClick = submitRename,
                role = GlasmioAlertButtonRole.Default,
            ),
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            GlasmioAlertFieldGroup {
                GlasmioAlertTextField(
                    value = renameDraft,
                    onValueChange = {
                        renameDraft = it
                        renameError = null
                    },
                    placeholder = renameTarget?.fileName.orEmpty(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitRename() }),
                )
            }
            if (renameError != null) {
                GlasmioText(
                    text = renameError.orEmpty(),
                    style = GlasmioTheme.textStyles.caption1,
                    color = colors.destructive,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
                )
            }
        }
    }

    val detailsTarget = lastFileDetails
    FileDetailsBottomSheet(
        visible = fileDetails != null && detailsTarget != null,
        file = detailsTarget,
        onOpen = { target ->
            fileDetails = null
            val f = File(target.filePath)
            if (!f.exists()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.received_files_missing),
                    Toast.LENGTH_SHORT,
                ).show()
            } else if (!FileActions.openFile(context, f, target.mimeType)) {
                Toast.makeText(
                    context,
                    context.getString(R.string.received_files_cannot_open),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
        onShare = { target ->
            fileDetails = null
            FileActions.shareFiles(
                context,
                listOf(File(target.filePath) to target.mimeType),
                context.getString(R.string.received_files_share_chooser),
            )
        },
        onSave = { target ->
            fileDetails = null
            triggerSaveToDownloads(listOf(target))
        },
        onDismiss = { fileDetails = null },
    )
}

@Composable
private fun PeerPickerBottomSheet(
    visible: Boolean,
    fileCount: Int,
    peers: List<DiscoveredPeer>,
    savedDevices: List<SavedDevice>,
    onPickPeer: (DiscoveredPeer) -> Unit,
    onPickSavedDevice: (SavedDevice) -> Unit,
    onOpenSendTab: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val scrollState = rememberScrollState()
    val blurPx = with(LocalDensity.current) { 24.dp.toPx() }
    val blurProgress = (scrollState.value / blurPx).coerceIn(0f, 1f)
    val sheetTitle = stringResource(R.string.received_files_pick_peer_title, fileCount)

    GlasmioBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        fitContent = true,
        title = sheetTitle,
        topBarBlurProgress = blurProgress,
        topBar = {
            GlasmioSheetTopToolbar(
                title = sheetTitle,
                actions = {
                    GlasmioSheetToolbarButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("peer_picker_close_button"),
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = colors.content,
                        )
                    }
                },
            )
        },
    ) {
        val sheetPadding = LocalGlasmioSheetContentPadding.current
        val peerAddresses = remember(peers) { peers.mapTo(HashSet()) { it.deviceAddress } }
        val remainingSaved = remember(savedDevices, peerAddresses) {
            savedDevices.filterNot { it.deviceAddress in peerAddresses }.take(4)
        }
        val visiblePeers = remember(peers) { peers.take(6) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(
                    top = sheetPadding.calculateTopPadding() + 8.dp,
                    bottom = sheetPadding.calculateBottomPadding() + 18.dp,
                )
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GlasmioText(
                text = stringResource(R.string.received_files_pick_peer_subtitle),
                style = GlasmioTheme.textStyles.subheadline,
                color = colors.secondaryContent,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            if (visiblePeers.isNotEmpty() || remainingSaved.isNotEmpty()) {
                GlasmioGroupedList(modifier = Modifier.fillMaxWidth()) {
                    visiblePeers.forEachIndexed { index, peer ->
                        GlasmioListRow(
                            title = peer.deviceName.ifBlank { peer.deviceAddress },
                            subtitle = peer.deviceAddress,
                            systemName = ThazhSymbols.WifiTethering,
                            onClick = { onPickPeer(peer) },
                            showTopSeparator = index > 0,
                            modifier = Modifier.testTag("sheet_pick_peer_${peer.deviceAddress}"),
                        )
                    }
                    remainingSaved.forEachIndexed { index, saved ->
                        val hasPrevious = visiblePeers.isNotEmpty() || index > 0
                        GlasmioListRow(
                            title = saved.effectiveName,
                            subtitle = saved.deviceAddress,
                            systemName = ThazhSymbols.Devices,
                            onClick = { onPickSavedDevice(saved) },
                            showTopSeparator = hasPrevious,
                            modifier = Modifier.testTag("sheet_pick_saved_${saved.deviceAddress}"),
                        )
                    }
                }
            }

            GlasmioButton(
                onClick = onOpenSendTab,
                emphasis = GlassEmphasis.Prominent,
                navigationSurfaceColor = semantic.accentBlue,
                size = GlasmioButtonSize.Large,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("peer_picker_open_send_button"),
            ) {
                SfIcon(
                    systemName = ThazhSymbols.WifiTethering,
                    contentDescription = null,
                    tint = Color.White,
                    size = 18.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                GlasmioText(
                    text = stringResource(R.string.received_files_open_send_tab),
                    style = GlasmioTheme.textStyles.headline,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FileDetailsBottomSheet(
    visible: Boolean,
    file: ReceivedFile?,
    onOpen: (ReceivedFile) -> Unit,
    onShare: (ReceivedFile) -> Unit,
    onSave: (ReceivedFile) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val scrollState = rememberScrollState()
    val blurPx = with(LocalDensity.current) { 24.dp.toPx() }
    val blurProgress = (scrollState.value / blurPx).coerceIn(0f, 1f)
    val sheetTitle = stringResource(R.string.received_files_details)

    GlasmioBottomSheet(
        visible = visible && file != null,
        onDismissRequest = onDismiss,
        fitContent = true,
        title = sheetTitle,
        topBarBlurProgress = blurProgress,
        topBar = {
            GlasmioSheetTopToolbar(
                title = sheetTitle,
                actions = {
                    GlasmioSheetToolbarButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("file_details_close_button"),
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = colors.content,
                        )
                    }
                },
            )
        },
    ) {
        if (file == null) return@GlasmioBottomSheet
        val sheetPadding = LocalGlasmioSheetContentPadding.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(
                    top = sheetPadding.calculateTopPadding() + 8.dp,
                    bottom = sheetPadding.calculateBottomPadding() + 18.dp,
                )
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val showImageThumb = file.category == FileCategory.IMAGE && File(file.filePath).isFile
                if (showImageThumb) {
                    AsyncImage(
                        model = File(file.filePath),
                        contentDescription = file.fileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(ContinuousRoundedRectangle(14.dp)),
                    )
                } else {
                    FileTypeBadge(category = file.category, size = 56.dp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    GlasmioText(
                        text = file.fileName,
                        style = GlasmioTheme.textStyles.headline,
                        color = colors.content,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    GlasmioText(
                        text = "${FormatUtils.formatFileSize(file.sizeBytes)} • ${stringResource(file.category.labelRes())}",
                        style = GlasmioTheme.textStyles.footnote,
                        color = colors.secondaryContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            val isTextFile = remember(file.id, file.fileName, file.mimeType) {
                file.isTextTransferFile()
            }
            var loadedTextPreview by remember(file.id) { mutableStateOf<String?>(null) }
            var fullTextForCopy by remember(file.id) { mutableStateOf<String?>(null) }
            var isLoadingText by remember(file.id) { mutableStateOf(isTextFile) }
            val context = LocalContext.current

            LaunchedEffect(file.id, file.filePath, isTextFile) {
                if (!isTextFile) {
                    isLoadingText = false
                    loadedTextPreview = null
                    fullTextForCopy = null
                    return@LaunchedEffect
                }
                isLoadingText = true
                val pair = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    readTextFileContentSafe(File(file.filePath))
                }
                loadedTextPreview = pair?.first
                fullTextForCopy = pair?.second
                isLoadingText = false
            }

            if (isTextFile) {
                GlasmioCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("received_text_file_preview_card"),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SfIcon(
                                    systemName = ThazhSymbols.FileDocument,
                                    contentDescription = null,
                                    tint = semantic.accentBlue,
                                    size = 17.dp,
                                )
                                GlasmioText(
                                    text = stringResource(R.string.received_files_text_preview_title),
                                    style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.content,
                                )
                            }

                            GlasmioButton(
                                onClick = {
                                    val textToCopy = fullTextForCopy ?: loadedTextPreview
                                    if (!textToCopy.isNullOrEmpty()) {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText(file.fileName, textToCopy))
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.received_files_text_copied_toast),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } else {
                                        copyReceivedTextFileToClipboard(context, file)
                                    }
                                },
                                emphasis = GlassEmphasis.Prominent,
                                navigationSurfaceColor = semantic.accentBlue,
                                size = GlasmioButtonSize.Small,
                                modifier = Modifier.testTag("copy_text_to_clipboard_button"),
                            ) {
                                SfIcon(
                                    systemName = ThazhSymbols.Copy,
                                    contentDescription = null,
                                    tint = Color.White,
                                    size = 14.dp,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                GlasmioText(
                                    text = stringResource(R.string.received_files_copy_to_clipboard),
                                    style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ContinuousRoundedRectangle(12.dp))
                                .background(
                                    colors.secondaryContent.copy(alpha = if (colors.isDark) 0.14f else 0.07f),
                                )
                                .padding(12.dp),
                        ) {
                            when {
                                isLoadingText -> {
                                    GlasmioText(
                                        text = stringResource(R.string.received_files_text_preview_loading),
                                        style = GlasmioTheme.textStyles.footnote,
                                        color = colors.secondaryContent,
                                    )
                                }
                                !loadedTextPreview.isNullOrBlank() -> {
                                    GlasmioText(
                                        text = loadedTextPreview!!,
                                        style = GlasmioTheme.textStyles.footnote,
                                        color = colors.content,
                                        maxLines = 12,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                else -> {
                                    GlasmioText(
                                        text = stringResource(R.string.received_files_text_preview_empty),
                                        style = GlasmioTheme.textStyles.footnote,
                                        color = colors.secondaryContent,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            GlasmioGroupedList(modifier = Modifier.fillMaxWidth()) {
                GlasmioListRow(
                    title = stringResource(R.string.received_files_details_name),
                    subtitle = file.fileName,
                    showTopSeparator = false,
                    showDisclosure = false,
                )
                GlasmioListRow(
                    title = stringResource(R.string.received_files_details_type),
                    detail = stringResource(file.category.labelRes()),
                    showTopSeparator = true,
                    showDisclosure = false,
                )
                GlasmioListRow(
                    title = stringResource(R.string.received_files_details_size),
                    detail = FormatUtils.formatFileSize(file.sizeBytes),
                    showTopSeparator = true,
                    showDisclosure = false,
                )
                GlasmioListRow(
                    title = stringResource(R.string.received_files_details_from),
                    detail = file.peerName.ifBlank { stringResource(R.string.transfer_unknown_peer) },
                    showTopSeparator = true,
                    showDisclosure = false,
                )
                GlasmioListRow(
                    title = stringResource(R.string.received_files_details_time),
                    detail = FormatUtils.formatFullDateTime(file.receivedAtEpochMs),
                    showTopSeparator = true,
                    showDisclosure = false,
                )
            }

            if (isTextFile) {
                GlasmioButton(
                    onClick = {
                        val textToCopy = fullTextForCopy ?: loadedTextPreview
                        if (!textToCopy.isNullOrEmpty()) {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText(file.fileName, textToCopy))
                            Toast.makeText(
                                context,
                                context.getString(R.string.received_files_text_copied_toast),
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            copyReceivedTextFileToClipboard(context, file)
                        }
                    },
                    emphasis = GlassEmphasis.Prominent,
                    navigationSurfaceColor = semantic.accentBlue,
                    size = GlasmioButtonSize.Large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("file_details_copy_text_button"),
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.Copy,
                        contentDescription = null,
                        tint = Color.White,
                        size = 17.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    GlasmioText(
                        text = stringResource(R.string.received_files_copy_to_clipboard),
                        style = GlasmioTheme.textStyles.headline,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlasmioButton(
                    onClick = { onOpen(file) },
                    emphasis = if (isTextFile) GlassEmphasis.Regular else GlassEmphasis.Prominent,
                    navigationSurfaceColor = semantic.accentBlue,
                    size = GlasmioButtonSize.Medium,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("file_details_open_button"),
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.OpenInNew,
                        contentDescription = null,
                        tint = if (isTextFile) colors.content else Color.White,
                        size = 16.dp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    GlasmioText(
                        text = stringResource(R.string.received_files_open),
                        style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isTextFile) colors.content else Color.White,
                        maxLines = 1,
                    )
                }

                GlasmioButton(
                    onClick = { onShare(file) },
                    emphasis = GlassEmphasis.Regular,
                    size = GlasmioButtonSize.Medium,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("file_details_share_button"),
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.Share,
                        contentDescription = null,
                        tint = colors.content,
                        size = 16.dp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    GlasmioText(
                        text = stringResource(R.string.received_files_share),
                        style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.content,
                        maxLines = 1,
                    )
                }

                GlasmioIconButton(
                    onClick = { onSave(file) },
                    size = 48.dp,
                    modifier = Modifier.testTag("file_details_save_button"),
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.Download,
                        contentDescription = stringResource(R.string.received_files_save),
                        tint = semantic.accentBlue,
                    )
                }
            }
        }
    }
}

private val TEXT_FILE_EXTENSIONS = setOf(
    "txt", "md", "markdown", "json", "xml", "csv", "tsv", "log",
    "html", "htm", "css", "js", "ts", "jsx", "tsx", "kt", "kts",
    "java", "py", "rb", "go", "rs", "c", "cpp", "h", "hpp",
    "yaml", "yml", "toml", "ini", "conf", "cfg", "properties",
    "sh", "bash", "zsh", "bat", "sql", "graphql", "gitignore",
)

fun ReceivedFile.isTextTransferFile(): Boolean {
    if (mimeType.startsWith("text/", ignoreCase = true)) return true
    val lowerMime = mimeType.lowercase()
    if (lowerMime == "application/json" ||
        lowerMime == "application/xml" ||
        lowerMime == "application/javascript" ||
        lowerMime == "application/x-yaml" ||
        lowerMime == "application/x-sh"
    ) {
        return true
    }
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return ext in TEXT_FILE_EXTENSIONS
}

private fun readTextFileContentSafe(
    file: File,
    maxPreviewChars: Int = 4_000,
    maxCopyBytes: Int = 512 * 1024,
): Pair<String, String>? {
    if (!file.isFile || !file.canRead()) return null
    return runCatching {
        val bytesToRead = file.length().coerceAtMost(maxCopyBytes.toLong()).toInt()
        if (bytesToRead <= 0) return@runCatching "" to ""
        val buffer = ByteArray(bytesToRead)
        file.inputStream().use { input ->
            var offset = 0
            while (offset < bytesToRead) {
                val read = input.read(buffer, offset, bytesToRead - offset)
                if (read <= 0) break
                offset += read
            }
            val fullText = String(buffer, 0, offset, Charsets.UTF_8)
            val preview = if (fullText.length > maxPreviewChars) {
                fullText.take(maxPreviewChars) + "\n…"
            } else {
                fullText
            }
            preview to fullText
        }
    }.getOrNull()
}

private fun copyReceivedTextFileToClipboard(
    context: android.content.Context,
    file: ReceivedFile,
) {
    val targetFile = File(file.filePath)
    if (!targetFile.isFile) {
        Toast.makeText(
            context,
            context.getString(R.string.received_files_missing),
            Toast.LENGTH_SHORT,
        ).show()
        return
    }
    val extracted = readTextFileContentSafe(targetFile)?.second
    if (extracted.isNullOrEmpty()) {
        Toast.makeText(
            context,
            context.getString(R.string.received_files_text_preview_empty),
            Toast.LENGTH_SHORT,
        ).show()
        return
    }
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText(file.fileName, extracted))
    Toast.makeText(
        context,
        context.getString(R.string.received_files_text_copied_toast),
        Toast.LENGTH_SHORT,
    ).show()
}

@Composable
private fun CenteredReceivedContent(content: @Composable () -> Unit) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReceivedFileRow(
    file: ReceivedFile,
    inSelectionMode: Boolean,
    isSelected: Boolean,
    showTopSeparator: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOpen: () -> Unit,
    onCopyText: (() -> Unit)? = null,
    onShareWifiDirect: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onRename: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 68.dp)
            .glasmioGroupedListSeparator(colors.separator, visible = showTopSeparator, horizontalInset = 16.dp)
            .background(
                if (isSelected) colors.accent.copy(alpha = if (colors.isDark) 0.18f else 0.12f)
                else Color.Transparent,
            )
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
            .testTag("received_file_row_${file.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (inSelectionMode) {
            SfIcon(
                systemName = if (isSelected) ThazhSymbols.CheckCircle else ThazhSymbols.RadioUnchecked,
                contentDescription = null,
                tint = if (isSelected) colors.accent else colors.tertiaryContent,
                size = 24.dp,
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
                    .clip(ContinuousRoundedRectangle(12.dp)),
            )
        } else {
            FileTypeBadge(category = file.category, size = 46.dp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GlasmioText(
                    text = file.fileName,
                    style = GlasmioTheme.textStyles.headline,
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!file.seen) {
                    Box(
                        modifier = Modifier
                            .clip(ContinuousCapsule)
                            .background(colors.accent.copy(alpha = if (colors.isDark) 0.24f else 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        GlasmioText(
                            text = stringResource(R.string.received_files_new_badge),
                            style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.Bold),
                            color = colors.accent,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            val sender = file.peerName.ifBlank { stringResource(R.string.transfer_unknown_peer) }
            GlasmioText(
                text = "${FormatUtils.formatFileSize(file.sizeBytes)} • $sender • ${FormatUtils.formatRelativeTime(file.receivedAtEpochMs)}",
                style = GlasmioTheme.textStyles.footnote,
                color = colors.secondaryContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!inSelectionMode) {
            if (onCopyText != null) {
                GlasmioIconButton(
                    onClick = onCopyText,
                    size = 42.dp,
                    modifier = Modifier.testTag("received_file_quick_copy_text_${file.id}"),
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.Copy,
                        contentDescription = stringResource(R.string.received_files_copy_to_clipboard),
                        tint = colors.accent,
                        size = 18.dp,
                    )
                }
            }
            GlasmioMenu(
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = it },
                itemCount = if (onCopyText != null) 8 else 7,
                anchor = { onAnchorClick ->
                    GlasmioIconButton(
                        onClick = onAnchorClick,
                        size = 48.dp,
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.MoreHoriz,
                            contentDescription = stringResource(R.string.received_files_details),
                            tint = colors.secondaryContent,
                            size = 20.dp,
                        )
                    }
                },
            ) { _, close ->
                if (onCopyText != null) {
                    GlasmioMenuItem(
                        title = stringResource(R.string.received_files_copy_to_clipboard),
                        systemName = ThazhSymbols.Copy,
                        onClick = {
                            close()
                            onCopyText()
                        },
                    )
                }
                GlasmioMenuItem(
                    title = stringResource(R.string.received_files_open),
                    systemName = ThazhSymbols.OpenInNew,
                    onClick = {
                        close()
                        onOpen()
                    },
                )
                GlasmioMenuItem(
                    title = stringResource(R.string.received_files_share_wifi_direct),
                    systemName = ThazhSymbols.WifiTethering,
                    onClick = {
                        close()
                        onShareWifiDirect()
                    },
                )
                GlasmioMenuItem(
                    title = stringResource(R.string.received_files_share),
                    systemName = ThazhSymbols.Share,
                    onClick = {
                        close()
                        onShare()
                    },
                )
                GlasmioMenuItem(
                    title = stringResource(R.string.received_files_save),
                    systemName = ThazhSymbols.Download,
                    onClick = {
                        close()
                        onSave()
                    },
                )
                GlasmioMenuItem(
                    title = stringResource(R.string.received_files_rename),
                    systemName = ThazhSymbols.Edit,
                    onClick = {
                        close()
                        onRename()
                    },
                )
                GlasmioMenuItem(
                    title = stringResource(R.string.received_files_details),
                    systemName = ThazhSymbols.Info,
                    onClick = {
                        close()
                        onDetails()
                    },
                )
                GlasmioMenuItem(
                    title = stringResource(R.string.received_files_delete),
                    systemName = ThazhSymbols.Delete,
                    destructive = true,
                    onClick = {
                        close()
                        onDelete()
                    },
                )
            }
        }
    }
}
