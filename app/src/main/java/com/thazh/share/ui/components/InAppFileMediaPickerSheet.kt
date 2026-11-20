package com.thazh.share.ui.components

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.data.FileCategory
import com.thazh.share.data.PickedFile
import com.thazh.share.data.ReceivedFile
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import com.thazh.share.util.FileActions
import com.thazh.share.util.FileNameUtils
import com.thazh.share.util.FormatUtils
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioBottomSheet
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioScrollableTabRow
import io.github.neoruaa.glasmio.ui.GlasmioSearchBar
import io.github.neoruaa.glasmio.ui.GlasmioSegmentedControl
import io.github.neoruaa.glasmio.ui.GlasmioSheetToolbarButton
import io.github.neoruaa.glasmio.ui.GlasmioSheetTopToolbar
import io.github.neoruaa.glasmio.ui.LocalGlasmioSheetContentPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class InAppPickerTab {
    FILES,
    MEDIA,
    FOLDERS,
}

private data class SelectablePickerEntry(
    val key: String,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val category: FileCategory,
    val modifiedAtEpochSec: Long,
    val parentFolderName: String = "",
)

private data class DirectoryNodeEntry(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val childCount: Int,
    val mimeType: String,
    val category: FileCategory,
    val modifiedAtEpochSec: Long,
)

/**
 * Checks whether the application holds full **"Truy cập vào mọi tệp" (All Files Access)** on the device:
 * - On Android 11+ (API 30+): [Environment.isExternalStorageManager] (`MANAGE_EXTERNAL_STORAGE`)
 * - On Android 10 and below: `READ_EXTERNAL_STORAGE`
 */
private fun hasAllFilesAccessPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun hasAnyMediaCatalogPermission(context: Context): Boolean {
    if (hasAllFilesAccessPermission(context)) return true
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val img = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val vid = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        val aud = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        img || vid || aud
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

private fun requiredMediaCatalogPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun buildManageAllFilesAccessIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
    }
}

@Suppress("DEPRECATION")
private fun resolveStorageRootDir(context: Context): File {
    val extRoot = runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
    if (extRoot != null && extRoot.exists()) return extRoot
    return File(context.filesDir, "share_files").apply { mkdirs() }
}

/**
 * Loads files inside a specific directory for the in-app folder explorer (`InAppPickerTab.FOLDERS`).
 */
private fun listDirectoryNodes(directory: File): List<DirectoryNodeEntry> {
    val children = runCatching { directory.listFiles() }.getOrNull() ?: return emptyList()
    return children
        .filter { !it.name.startsWith(".") && (it.isDirectory || it.length() > 0L) }
        .map { child ->
            if (child.isDirectory) {
                val subCount = runCatching {
                    child.list()?.count { !it.startsWith(".") } ?: 0
                }.getOrDefault(0)
                DirectoryNodeEntry(
                    file = child,
                    name = child.name,
                    isDirectory = true,
                    sizeBytes = 0L,
                    childCount = subCount,
                    mimeType = "resource/folder",
                    category = FileCategory.OTHER,
                    modifiedAtEpochSec = child.lastModified() / 1000L,
                )
            } else {
                val mime = FileNameUtils.guessMimeType(child.name)
                DirectoryNodeEntry(
                    file = child,
                    name = child.name,
                    isDirectory = false,
                    sizeBytes = child.length(),
                    childCount = 0,
                    mimeType = mime,
                    category = FileCategory.from(mime, child.name),
                    modifiedAtEpochSec = child.lastModified() / 1000L,
                )
            }
        }
        .sortedWith(
            compareByDescending<DirectoryNodeEntry> { it.isDirectory }
                .thenBy { it.name.lowercase() },
        )
}

/**
 * Recursively walks primary user folders on external storage when "All files access"
 * (`MANAGE_EXTERNAL_STORAGE`) is granted, combined with `MediaStore` and in-app received files,
 * so every document, APK, ZIP, PDF, TXT, audio, photo, and video on the device is displayed in-app.
 */
@Suppress("DEPRECATION")
private fun loadOnDeviceCatalogEntries(
    context: Context,
    inAppReceivedFiles: List<ReceivedFile>,
): List<SelectablePickerEntry> {
    val entriesMap = LinkedHashMap<String, SelectablePickerEntry>(256)
    val canonicalSeenPaths = HashSet<String>(256)

    fun putFileEntry(file: File, overrideCategory: FileCategory? = null, overrideMime: String? = null) {
        if (!file.isFile || file.length() <= 0L || file.name.startsWith(".")) return
        val absPath = file.absolutePath
        if (!canonicalSeenPaths.add(absPath)) return
        val uri = Uri.fromFile(file)
        val key = uri.toString()
        val mime = overrideMime?.takeIf { it.isNotBlank() } ?: FileNameUtils.guessMimeType(file.name)
        val category = overrideCategory ?: FileCategory.from(mime, file.name)
        entriesMap[key] = SelectablePickerEntry(
            key = key,
            uri = uri,
            name = file.name,
            sizeBytes = file.length(),
            mimeType = mime,
            category = category,
            modifiedAtEpochSec = file.lastModified() / 1000L,
            parentFolderName = file.parentFile?.name.orEmpty(),
        )
    }

    // 1. In-app received files on disk
    inAppReceivedFiles.forEach { rf ->
        val f = File(rf.filePath)
        if (f.isFile && f.length() > 0L) {
            putFileEntry(f, overrideCategory = rf.category, overrideMime = rf.mimeType)
        }
    }

    // 2. App-owned share_files directory
    val appShareDir = File(context.filesDir, "share_files")
    runCatching {
        if (appShareDir.isDirectory && appShareDir.canRead()) {
            appShareDir.listFiles()?.take(80)?.forEach { f -> putFileEntry(f) }
        }
    }

    // 3. When All Files Access (MANAGE_EXTERNAL_STORAGE) or storage permission is granted,
    // traverse real device storage directories directly (Download, Documents, DCIM, Pictures, Music, Movies, Root)
    if (hasAllFilesAccessPermission(context)) {
        val extRoot = runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
        val targetDirs = buildList {
            if (extRoot != null && extRoot.isDirectory) {
                add(extRoot to 1) // Top-level files in /storage/emulated/0
            }
            runCatching {
                add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) to 3)
                add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) to 3)
                add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) to 2)
                add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) to 2)
                add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) to 2)
                add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) to 2)
            }
        }

        var scannedFilesCount = 0
        val maxScannedFiles = 360

        fun scanDirRecursive(dir: File, depthRemaining: Int) {
            if (scannedFilesCount >= maxScannedFiles || depthRemaining < 0) return
            val children = runCatching { dir.listFiles() }.getOrNull() ?: return
            for (child in children) {
                if (scannedFilesCount >= maxScannedFiles) break
                val name = child.name
                if (name.startsWith(".") || name.equals("Android", ignoreCase = true)) continue
                if (child.isFile) {
                    if (child.length() > 0L) {
                        putFileEntry(child)
                        scannedFilesCount++
                    }
                } else if (child.isDirectory && depthRemaining > 0) {
                    scanDirRecursive(child, depthRemaining - 1)
                }
            }
        }

        for ((dir, maxDepth) in targetDirs) {
            if (dir.isDirectory && dir.canRead()) {
                scanDirRecursive(dir, maxDepth)
            }
        }
    }

    // 4. MediaStore queries (Photos, Videos, Audio, Downloads, and External Files table)
    val resolver = context.contentResolver
    val projection = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.DATE_MODIFIED,
        MediaStore.MediaColumns.DATA,
    )
    val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

    fun queryCollection(collectionUri: Uri, defaultMimePrefix: String?, limit: Int) {
        runCatching {
            resolver.query(
                collectionUri,
                projection,
                null,
                null,
                sortOrder,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val rawName = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    if (size <= 0L || rawName.isNullOrBlank() || rawName.startsWith(".")) continue

                    val diskPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    if (!diskPath.isNullOrBlank() && canonicalSeenPaths.contains(diskPath)) {
                        continue
                    }

                    val rawMime = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                    val mime = rawMime?.takeIf { it.isNotBlank() }
                        ?: defaultMimePrefix
                        ?: FileNameUtils.guessMimeType(rawName)
                    val modifiedSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val parentFolder = diskPath?.let { File(it).parentFile?.name }.orEmpty()

                    val contentUri = ContentUris.withAppendedId(collectionUri, id)
                    val key = contentUri.toString()
                    if (!entriesMap.containsKey(key)) {
                        if (!diskPath.isNullOrBlank()) {
                            canonicalSeenPaths.add(diskPath)
                        }
                        entriesMap[key] = SelectablePickerEntry(
                            key = key,
                            uri = contentUri,
                            name = rawName,
                            sizeBytes = size,
                            mimeType = mime,
                            category = FileCategory.from(mime, rawName),
                            modifiedAtEpochSec = modifiedSec,
                            parentFolderName = parentFolder,
                        )
                        count++
                    }
                }
            }
        }
    }

    if (hasAnyMediaCatalogPermission(context)) {
        queryCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*", limit = 100)
        queryCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*", limit = 60)
        queryCollection(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio/*", limit = 60)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        queryCollection(MediaStore.Downloads.EXTERNAL_CONTENT_URI, null, limit = 80)
    }
    if (hasAllFilesAccessPermission(context)) {
        queryCollection(MediaStore.Files.getContentUri("external"), null, limit = 140)
    }

    return entriesMap.values.sortedByDescending { it.modifiedAtEpochSec }
}

@Composable
fun InAppFileMediaPickerSheet(
    visible: Boolean = true,
    initialTab: InAppPickerTab,
    inAppReceivedFiles: List<ReceivedFile>,
    onConfirmSelection: (List<Uri>) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val scope = rememberCoroutineScope()

    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<FileCategory?>(null) }

    var catalogRefreshTick by remember { mutableIntStateOf(0) }
    var hasAllFilesAccess by remember {
        mutableStateOf(hasAllFilesAccessPermission(context))
    }
    var hasMediaPermission by remember {
        mutableStateOf(hasAnyMediaCatalogPermission(context))
    }

    var isLoadingCatalog by remember { mutableStateOf(true) }
    var deviceCatalogEntries by remember { mutableStateOf<List<SelectablePickerEntry>>(emptyList()) }

    // Direct in-app Folder Browser state (for InAppPickerTab.FOLDERS)
    val storageRootDir = remember(context, hasAllFilesAccess) {
        resolveStorageRootDir(context)
    }
    var currentFolder by remember(storageRootDir) { mutableStateOf(storageRootDir) }
    var currentFolderNodes by remember { mutableStateOf<List<DirectoryNodeEntry>>(emptyList()) }
    var isLoadingFolder by remember { mutableStateOf(false) }

    val stagedSystemEntries = remember { mutableStateListOf<SelectablePickerEntry>() }
    val selectedKeys = remember { mutableStateListOf<String>() }

    // Re-check All Files Access when user returns from Settings (ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
    val allFilesSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        hasAllFilesAccess = hasAllFilesAccessPermission(context)
        hasMediaPermission = hasAnyMediaCatalogPermission(context)
        catalogRefreshTick++
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        hasAllFilesAccess = hasAllFilesAccessPermission(context)
        hasMediaPermission = hasAnyMediaCatalogPermission(context)
        catalogRefreshTick++
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val latestAllFiles = hasAllFilesAccessPermission(context)
                val latestMedia = hasAnyMediaCatalogPermission(context)
                if (latestAllFiles != hasAllFilesAccess || latestMedia != hasMediaPermission) {
                    hasAllFilesAccess = latestAllFiles
                    hasMediaPermission = latestMedia
                    catalogRefreshTick++
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val requestAllFilesAccess: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                allFilesSettingsLauncher.launch(buildManageAllFilesAccessIntent(context))
            }.onFailure {
                runCatching {
                    allFilesSettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        } else {
            mediaPermissionLauncher.launch(requiredMediaCatalogPermissions())
        }
    }

    LaunchedEffect(inAppReceivedFiles, hasAllFilesAccess, hasMediaPermission, catalogRefreshTick) {
        isLoadingCatalog = true
        val loaded = withContext(Dispatchers.IO) {
            loadOnDeviceCatalogEntries(context, inAppReceivedFiles)
        }
        deviceCatalogEntries = loaded
        isLoadingCatalog = false
    }

    LaunchedEffect(currentFolder, hasAllFilesAccess, catalogRefreshTick) {
        isLoadingFolder = true
        val nodes = withContext(Dispatchers.IO) {
            listDirectoryNodes(currentFolder)
        }
        currentFolderNodes = nodes
        isLoadingFolder = false
    }

    // Back handler when navigating inside subdirectories in FOLDERS tab
    val canNavigateUpInFolder = activeTab == InAppPickerTab.FOLDERS &&
        currentFolder.absolutePath != storageRootDir.absolutePath &&
        currentFolder.parentFile != null
    BackHandler(enabled = canNavigateUpInFolder) {
        currentFolder.parentFile?.let { parent -> currentFolder = parent }
    }

    val systemFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val resolved = withContext(Dispatchers.IO) {
                    uris.map { uri ->
                        val picked: PickedFile = FileActions.queryPickedFile(context, uri)
                        SelectablePickerEntry(
                            key = uri.toString(),
                            uri = uri,
                            name = picked.name,
                            sizeBytes = picked.sizeBytes,
                            mimeType = picked.mimeType,
                            category = FileCategory.from(picked.mimeType, picked.name),
                            modifiedAtEpochSec = System.currentTimeMillis() / 1000L,
                        )
                    }
                }
                resolved.forEach { entry ->
                    if (stagedSystemEntries.none { it.key == entry.key }) {
                        stagedSystemEntries.add(0, entry)
                    }
                    if (entry.key !in selectedKeys) {
                        selectedKeys.add(entry.key)
                    }
                }
            }
        }
    }

    val systemMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val resolved = withContext(Dispatchers.IO) {
                    uris.map { uri ->
                        val picked: PickedFile = FileActions.queryPickedFile(context, uri)
                        SelectablePickerEntry(
                            key = uri.toString(),
                            uri = uri,
                            name = picked.name,
                            sizeBytes = picked.sizeBytes,
                            mimeType = picked.mimeType,
                            category = FileCategory.from(picked.mimeType, picked.name),
                            modifiedAtEpochSec = System.currentTimeMillis() / 1000L,
                        )
                    }
                }
                resolved.forEach { entry ->
                    if (stagedSystemEntries.none { it.key == entry.key }) {
                        stagedSystemEntries.add(0, entry)
                    }
                    if (entry.key !in selectedKeys) {
                        selectedKeys.add(entry.key)
                    }
                }
            }
        }
    }

    val allEntries = remember(deviceCatalogEntries, stagedSystemEntries.toList(), currentFolderNodes) {
        val map = LinkedHashMap<String, SelectablePickerEntry>()
        stagedSystemEntries.forEach { map[it.key] = it }
        deviceCatalogEntries.forEach { if (!map.containsKey(it.key)) map[it.key] = it }
        currentFolderNodes.filter { !it.isDirectory }.forEach { node ->
            val uri = Uri.fromFile(node.file)
            val key = uri.toString()
            if (!map.containsKey(key)) {
                map[key] = SelectablePickerEntry(
                    key = key,
                    uri = uri,
                    name = node.name,
                    sizeBytes = node.sizeBytes,
                    mimeType = node.mimeType,
                    category = node.category,
                    modifiedAtEpochSec = node.modifiedAtEpochSec,
                    parentFolderName = node.file.parentFile?.name.orEmpty(),
                )
            }
        }
        map.values.toList()
    }

    val visibleEntries = remember(allEntries, activeTab, searchQuery.text, selectedCategory) {
        val q = searchQuery.text.trim().lowercase()
        allEntries.filter { entry ->
            val matchesTab = when (activeTab) {
                InAppPickerTab.MEDIA -> entry.category == FileCategory.IMAGE || entry.category == FileCategory.VIDEO
                InAppPickerTab.FILES -> selectedCategory == null || entry.category == selectedCategory
                InAppPickerTab.FOLDERS -> true
            }
            val matchesQuery = q.isEmpty() ||
                entry.name.lowercase().contains(q) ||
                entry.parentFolderName.lowercase().contains(q)
            matchesTab && matchesQuery
        }
    }

    val visibleFolderNodes = remember(currentFolderNodes, searchQuery.text) {
        val q = searchQuery.text.trim().lowercase()
        if (q.isEmpty()) currentFolderNodes
        else currentFolderNodes.filter { it.name.lowercase().contains(q) }
    }

    val selectedEntries = remember(allEntries, selectedKeys.toList()) {
        allEntries.filter { it.key in selectedKeys }
    }
    val totalSelectedBytes = remember(selectedEntries) {
        selectedEntries.sumOf { it.sizeBytes.coerceAtLeast(0L) }
    }

    val sheetTitle = when (activeTab) {
        InAppPickerTab.MEDIA -> stringResource(R.string.picker_media_title)
        InAppPickerTab.FOLDERS -> stringResource(R.string.picker_tab_folders)
        InAppPickerTab.FILES -> stringResource(R.string.picker_files_title)
    }

    val filterTabs = listOf<Pair<FileCategory?, String>>(
        null to stringResource(R.string.received_files_filter_all),
    ) + FileCategory.entries.map { cat ->
        cat to stringResource(cat.labelRes())
    }

    GlasmioBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        fitContent = true,
        title = sheetTitle,
        topBar = {
            GlasmioSheetTopToolbar(
                title = sheetTitle,
                navigation = {
                    GlasmioSheetToolbarButton(
                        onClick = { catalogRefreshTick++ },
                        modifier = Modifier.testTag("in_app_picker_refresh_button"),
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.Refresh,
                            contentDescription = stringResource(R.string.browser_action_reload),
                            tint = colors.content,
                        )
                    }
                },
                actions = {
                    if (!hasAllFilesAccess) {
                        GlasmioSheetToolbarButton(
                            onClick = requestAllFilesAccess,
                            modifier = Modifier.testTag("in_app_picker_top_grant_all_files_button"),
                        ) {
                            SfIcon(
                                systemName = ThazhSymbols.ShieldCheck,
                                contentDescription = stringResource(R.string.picker_all_files_permission_title),
                                tint = semantic.warningOrange,
                            )
                        }
                    }
                    GlasmioSheetToolbarButton(
                        onClick = {
                            if (activeTab == InAppPickerTab.MEDIA) {
                                systemMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                                )
                            } else {
                                systemFileLauncher.launch("*/*")
                            }
                        },
                        modifier = Modifier.testTag("in_app_picker_browse_system_button"),
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.FolderOpen,
                            contentDescription = stringResource(R.string.picker_browse_system_files),
                            tint = colors.accent,
                        )
                    }
                    GlasmioSheetToolbarButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("in_app_picker_close_button"),
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 470.dp, max = 630.dp)
                .padding(
                    top = sheetPadding.calculateTopPadding() + 8.dp,
                    bottom = sheetPadding.calculateBottomPadding() + 16.dp,
                )
                .padding(horizontal = 16.dp)
                .testTag("in_app_file_media_picker_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 1. GlasmioSegmentedControl: All Files vs Folders vs Photos & Videos
            GlasmioSegmentedControl(
                items = listOf(
                    InAppPickerTab.FILES to stringResource(R.string.picker_tab_files),
                    InAppPickerTab.FOLDERS to stringResource(R.string.picker_tab_folders),
                    InAppPickerTab.MEDIA to stringResource(R.string.picker_tab_media),
                ),
                selected = activeTab,
                onSelected = { activeTab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("in_app_picker_mode_segment"),
            ) { tab, isSelected ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    SfIcon(
                        systemName = when (tab) {
                            InAppPickerTab.MEDIA -> ThazhSymbols.PhotoLibrary
                            InAppPickerTab.FOLDERS -> ThazhSymbols.FolderOpen
                            InAppPickerTab.FILES -> ThazhSymbols.AttachFile
                        },
                        contentDescription = null,
                        tint = if (isSelected) colors.accent else colors.secondaryContent,
                        size = 14.dp,
                    )
                    GlasmioText(
                        text = when (tab) {
                            InAppPickerTab.MEDIA -> stringResource(R.string.picker_tab_media)
                            InAppPickerTab.FOLDERS -> stringResource(R.string.picker_tab_folders)
                            InAppPickerTab.FILES -> stringResource(R.string.picker_tab_files)
                        },
                        style = GlasmioTheme.textStyles.caption1.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (isSelected) colors.content else colors.secondaryContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // 2. Authentic GlasmioSearchBar for searching in-app catalog
            GlasmioSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
                onClose = {
                    searchQuery = TextFieldValue("")
                    isSearchActive = false
                },
                placeholder = stringResource(R.string.picker_search_placeholder),
                closeContentDescription = stringResource(R.string.close),
                controlHeight = 42.dp,
                searchIconSize = 18.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("in_app_picker_search_input"),
            )

            // 3. Prominent "Truy cập vào mọi tệp" (All Files Access) Permission Banner when not yet granted
            if (!hasAllFilesAccess) {
                GlasmioCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                semantic.accentBlue.copy(alpha = if (colors.isDark) 0.18f else 0.10f),
                            )
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ThazhSquircleIconBox(
                            symbol = ThazhSymbols.FolderOpen,
                            accentColor = semantic.accentBlue,
                            boxSize = 38.dp,
                            iconSize = 19.dp,
                            cornerRadius = 11.dp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            GlasmioText(
                                text = stringResource(R.string.picker_all_files_permission_title),
                                style = GlasmioTheme.textStyles.footnote.copy(fontWeight = FontWeight.Bold),
                                color = colors.content,
                            )
                            GlasmioText(
                                text = stringResource(R.string.picker_all_files_permission_body),
                                style = GlasmioTheme.textStyles.caption2,
                                color = colors.secondaryContent,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        GlasmioButton(
                            onClick = requestAllFilesAccess,
                            emphasis = GlassEmphasis.Prominent,
                            navigationSurfaceColor = semantic.accentBlue,
                            size = GlasmioButtonSize.Small,
                            modifier = Modifier.testTag("in_app_picker_grant_permission_button"),
                        ) {
                            GlasmioText(
                                text = stringResource(R.string.permission_grant),
                                style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            // 4. Category filter bar (FILES tab) OR Breadcrumb bar (FOLDERS tab)
            if (activeTab == InAppPickerTab.FILES) {
                GlasmioScrollableTabRow(
                    items = filterTabs,
                    selected = selectedCategory,
                    onSelected = { cat ->
                        selectedCategory = if (selectedCategory == cat) null else cat
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (activeTab == InAppPickerTab.FOLDERS) {
                FolderBreadcrumbStrip(
                    rootDir = storageRootDir,
                    currentDir = currentFolder,
                    onSelectDir = { target -> currentFolder = target },
                )
            }

            // 5. Quick Select All / Clear Row
            val activeSelectableCount = if (activeTab == InAppPickerTab.FOLDERS) {
                visibleFolderNodes.count { !it.isDirectory }
            } else {
                visibleEntries.size
            }
            if (activeSelectableCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlasmioText(
                        text = if (activeTab == InAppPickerTab.FOLDERS) {
                            "${currentFolder.name.ifBlank { stringResource(R.string.picker_folder_root_label) }} (${visibleFolderNodes.size})"
                        } else {
                            "${stringResource(R.string.picker_source_in_app_section)} (${visibleEntries.size})"
                        },
                        style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.secondaryContent,
                    )
                    val targetFileKeys = if (activeTab == InAppPickerTab.FOLDERS) {
                        visibleFolderNodes.filter { !it.isDirectory }.map { Uri.fromFile(it.file).toString() }
                    } else {
                        visibleEntries.map { it.key }
                    }
                    val allVisibleSelected = targetFileKeys.isNotEmpty() && targetFileKeys.all { it in selectedKeys }
                    GlasmioButton(
                        onClick = {
                            if (allVisibleSelected) {
                                targetFileKeys.forEach { selectedKeys.remove(it) }
                            } else {
                                targetFileKeys.forEach { key ->
                                    if (key !in selectedKeys) selectedKeys.add(key)
                                }
                            }
                        },
                        emphasis = GlassEmphasis.Regular,
                        size = GlasmioButtonSize.Small,
                    ) {
                        GlasmioText(
                            text = if (allVisibleSelected) {
                                stringResource(R.string.picker_clear_all)
                            } else {
                                stringResource(R.string.picker_select_all)
                            },
                            style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                            color = semantic.accentBlue,
                        )
                    }
                }
            }

            // 6. Content area: In-app Folder Explorer, In-app Grid for MEDIA, or In-app List for FILES
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (activeTab) {
                    InAppPickerTab.FOLDERS -> {
                        if (isLoadingFolder && visibleFolderNodes.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                GlasmioActivityIndicator(
                                    size = 28.dp,
                                    strokeWidth = 2.6.dp,
                                    color = semantic.accentBlue,
                                )
                            }
                        } else if (visibleFolderNodes.isEmpty()) {
                            PickerEmptyStateCard(
                                isMediaMode = false,
                                hasAllFilesPermission = hasAllFilesAccess,
                                onGrantAllFilesPermission = requestAllFilesAccess,
                                onRefreshInApp = { catalogRefreshTick++ },
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (canNavigateUpInFolder) {
                                    item(key = "parent_dir_up") {
                                        ParentFolderUpRow(
                                            parentName = currentFolder.parentFile?.name
                                                ?.takeIf { it.isNotBlank() }
                                                ?: stringResource(R.string.picker_folder_root_label),
                                            onClick = {
                                                currentFolder.parentFile?.let { currentFolder = it }
                                            },
                                        )
                                    }
                                }
                                items(visibleFolderNodes, key = { it.file.absolutePath }) { node ->
                                    if (node.isDirectory) {
                                        DirectoryFolderItemRow(
                                            node = node,
                                            onClick = { currentFolder = node.file },
                                        )
                                    } else {
                                        val fileKey = Uri.fromFile(node.file).toString()
                                        val isSelected = fileKey in selectedKeys
                                        FilePickerListItemRow(
                                            entry = SelectablePickerEntry(
                                                key = fileKey,
                                                uri = Uri.fromFile(node.file),
                                                name = node.name,
                                                sizeBytes = node.sizeBytes,
                                                mimeType = node.mimeType,
                                                category = node.category,
                                                modifiedAtEpochSec = node.modifiedAtEpochSec,
                                                parentFolderName = currentFolder.name,
                                            ),
                                            isSelected = isSelected,
                                            onToggle = {
                                                if (isSelected) selectedKeys.remove(fileKey)
                                                else selectedKeys.add(fileKey)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    InAppPickerTab.MEDIA -> {
                        if (isLoadingCatalog && visibleEntries.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                GlasmioActivityIndicator(
                                    size = 28.dp,
                                    strokeWidth = 2.6.dp,
                                    color = semantic.accentBlue,
                                )
                            }
                        } else if (visibleEntries.isEmpty()) {
                            PickerEmptyStateCard(
                                isMediaMode = true,
                                hasAllFilesPermission = hasAllFilesAccess,
                                onGrantAllFilesPermission = requestAllFilesAccess,
                                onRefreshInApp = { catalogRefreshTick++ },
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 104.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(visibleEntries, key = { it.key }) { entry ->
                                    val isSelected = entry.key in selectedKeys
                                    MediaGridThumbnailCell(
                                        entry = entry,
                                        isSelected = isSelected,
                                        onToggle = {
                                            if (isSelected) selectedKeys.remove(entry.key)
                                            else selectedKeys.add(entry.key)
                                        },
                                    )
                                }
                            }
                        }
                    }

                    InAppPickerTab.FILES -> {
                        if (isLoadingCatalog && visibleEntries.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                GlasmioActivityIndicator(
                                    size = 28.dp,
                                    strokeWidth = 2.6.dp,
                                    color = semantic.accentBlue,
                                )
                            }
                        } else if (visibleEntries.isEmpty()) {
                            PickerEmptyStateCard(
                                isMediaMode = false,
                                hasAllFilesPermission = hasAllFilesAccess,
                                onGrantAllFilesPermission = requestAllFilesAccess,
                                onRefreshInApp = { catalogRefreshTick++ },
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(visibleEntries, key = { it.key }) { entry ->
                                    val isSelected = entry.key in selectedKeys
                                    FilePickerListItemRow(
                                        entry = entry,
                                        isSelected = isSelected,
                                        onToggle = {
                                            if (isSelected) selectedKeys.remove(entry.key)
                                            else selectedKeys.add(entry.key)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. Bottom Selection Summary & Confirm Action Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (selectedEntries.isNotEmpty()) {
                    GlasmioText(
                        text = stringResource(
                            R.string.picker_selected_summary,
                            selectedEntries.size,
                            FormatUtils.formatFileSize(totalSelectedBytes),
                        ),
                        style = GlasmioTheme.textStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                        color = semantic.accentBlue,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlasmioButton(
                        onClick = onDismiss,
                        emphasis = GlassEmphasis.Regular,
                        size = GlasmioButtonSize.Medium,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("in_app_picker_cancel_button"),
                    ) {
                        GlasmioText(
                            text = stringResource(R.string.cancel),
                            style = GlasmioTheme.textStyles.callout.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.content,
                        )
                    }
                    GlasmioButton(
                        onClick = {
                            val uris = selectedEntries.map { it.uri }
                            onConfirmSelection(uris)
                            onDismiss()
                        },
                        enabled = selectedEntries.isNotEmpty(),
                        emphasis = GlassEmphasis.Prominent,
                        navigationSurfaceColor = semantic.accentBlue,
                        size = GlasmioButtonSize.Medium,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("in_app_picker_confirm_button"),
                    ) {
                        GlasmioText(
                            text = stringResource(R.string.picker_confirm_selection, selectedEntries.size),
                            style = GlasmioTheme.textStyles.callout.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderBreadcrumbStrip(
    rootDir: File,
    currentDir: File,
    onSelectDir: (File) -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val scrollState = rememberScrollState()

    val segments = remember(rootDir, currentDir) {
        val list = mutableListOf<File>()
        var cursor: File? = currentDir
        val rootPath = rootDir.absolutePath
        while (cursor != null) {
            list.add(0, cursor)
            if (cursor.absolutePath == rootPath) break
            cursor = cursor.parentFile
        }
        if (list.isEmpty()) list.add(rootDir)
        list
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        segments.forEachIndexed { index, dir ->
            val isLast = index == segments.lastIndex
            val label = if (index == 0) {
                stringResource(R.string.picker_folder_root_label)
            } else {
                dir.name.ifBlank { dir.absolutePath }
            }
            Box(
                modifier = Modifier
                    .clip(ContinuousCapsule)
                    .background(
                        if (isLast) {
                            semantic.accentBlue.copy(alpha = if (colors.isDark) 0.24f else 0.14f)
                        } else {
                            colors.secondaryContent.copy(alpha = if (colors.isDark) 0.16f else 0.09f)
                        },
                    )
                    .clickable(
                        role = Role.Button,
                        onClick = { onSelectDir(dir) },
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                GlasmioText(
                    text = label,
                    style = GlasmioTheme.textStyles.caption1.copy(
                        fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = if (isLast) semantic.accentBlue else colors.secondaryContent,
                    maxLines = 1,
                )
            }
            if (!isLast) {
                SfIcon(
                    systemName = ThazhSymbols.ChevronRight,
                    contentDescription = null,
                    tint = colors.tertiaryContent,
                    size = 12.dp,
                )
            }
        }
    }
}

@Composable
private fun ParentFolderUpRow(
    parentName: String,
    onClick: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic

    GlasmioCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThazhSquircleIconBox(
                symbol = ThazhSymbols.Back,
                accentColor = semantic.accentCyan,
                boxSize = 40.dp,
                iconSize = 18.dp,
                cornerRadius = 12.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                GlasmioText(
                    text = stringResource(R.string.picker_folder_up_label),
                    style = GlasmioTheme.textStyles.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.content,
                )
                GlasmioText(
                    text = parentName,
                    style = GlasmioTheme.textStyles.caption1,
                    color = colors.secondaryContent,
                )
            }
        }
    }
}

@Composable
private fun DirectoryFolderItemRow(
    node: DirectoryNodeEntry,
    onClick: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic

    GlasmioCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThazhSquircleIconBox(
                symbol = ThazhSymbols.FolderOpen,
                accentColor = semantic.accentBlue,
                boxSize = 42.dp,
                iconSize = 20.dp,
                cornerRadius = 12.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                GlasmioText(
                    text = node.name,
                    style = GlasmioTheme.textStyles.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                GlasmioText(
                    text = stringResource(R.string.picker_folder_items_count, node.childCount),
                    style = GlasmioTheme.textStyles.caption1,
                    color = colors.secondaryContent,
                )
            }
            SfIcon(
                systemName = ThazhSymbols.ChevronRight,
                contentDescription = null,
                tint = colors.tertiaryContent,
                size = 16.dp,
            )
        }
    }
}

@Composable
private fun MediaGridThumbnailCell(
    entry: SelectablePickerEntry,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val context = LocalContext.current
    val shape = ContinuousRoundedRectangle(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(colors.secondaryContent.copy(alpha = if (colors.isDark) 0.16f else 0.09f))
            .border(
                width = if (isSelected) 2.2.dp else 0.8.dp,
                color = if (isSelected) semantic.accentBlue else colors.separator.copy(alpha = 0.35f),
                shape = shape,
            )
            .clickable(role = Role.Checkbox, onClick = onToggle),
    ) {
        if (entry.category == FileCategory.IMAGE) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(entry.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = entry.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                semantic.categoryVideo.copy(alpha = 0.28f),
                                semantic.accentBlue.copy(alpha = 0.16f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                SfIcon(
                    systemName = ThazhSymbols.FileVideo,
                    contentDescription = entry.name,
                    tint = semantic.categoryVideo,
                    size = 34.dp,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(24.dp)
                .clip(ContinuousCapsule)
                .background(
                    if (isSelected) semantic.accentBlue else Color.Black.copy(alpha = 0.42f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            SfIcon(
                systemName = if (isSelected) ThazhSymbols.CheckCircle else ThazhSymbols.RadioUnchecked,
                contentDescription = null,
                tint = Color.White,
                size = 16.dp,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                    ),
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            GlasmioText(
                text = entry.name,
                style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            GlasmioText(
                text = FormatUtils.formatFileSize(entry.sizeBytes),
                style = GlasmioTheme.textStyles.caption2,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FilePickerListItemRow(
    entry: SelectablePickerEntry,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic

    GlasmioCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox, onClick = onToggle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) {
                        semantic.accentBlue.copy(alpha = if (colors.isDark) 0.18f else 0.10f)
                    } else {
                        Color.Transparent
                    },
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileTypeBadge(
                category = entry.category,
                size = 42.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                GlasmioText(
                    text = entry.name,
                    style = GlasmioTheme.textStyles.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                val folderPart = entry.parentFolderName.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()
                GlasmioText(
                    text = "${stringResource(entry.category.labelRes())} • ${FormatUtils.formatFileSize(entry.sizeBytes)}$folderPart",
                    style = GlasmioTheme.textStyles.caption1,
                    color = colors.secondaryContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            SfIcon(
                systemName = if (isSelected) ThazhSymbols.CheckCircle else ThazhSymbols.RadioUnchecked,
                contentDescription = null,
                tint = if (isSelected) semantic.accentBlue else colors.secondaryContent.copy(alpha = 0.55f),
                size = 22.dp,
            )
        }
    }
}

@Composable
private fun PickerEmptyStateCard(
    isMediaMode: Boolean,
    hasAllFilesPermission: Boolean,
    onGrantAllFilesPermission: () -> Unit,
    onRefreshInApp: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic

    GlasmioCard(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ThazhSquircleIconBox(
                symbol = if (!hasAllFilesPermission) {
                    ThazhSymbols.FolderOpen
                } else if (isMediaMode) {
                    ThazhSymbols.PhotoLibrary
                } else {
                    ThazhSymbols.AttachFile
                },
                accentColor = semantic.accentBlue,
                boxSize = 54.dp,
                iconSize = 26.dp,
                cornerRadius = 16.dp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            GlasmioText(
                text = stringResource(
                    if (!hasAllFilesPermission) {
                        R.string.picker_all_files_permission_title
                    } else if (isMediaMode) {
                        R.string.picker_empty_media_title
                    } else {
                        R.string.picker_empty_files_title
                    },
                ),
                style = GlasmioTheme.textStyles.headline.copy(fontWeight = FontWeight.SemiBold),
                color = colors.content,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            GlasmioText(
                text = stringResource(
                    if (!hasAllFilesPermission) {
                        R.string.picker_all_files_permission_body
                    } else if (isMediaMode) {
                        R.string.picker_empty_media_body
                    } else {
                        R.string.picker_empty_files_body
                    },
                ),
                style = GlasmioTheme.textStyles.footnote,
                color = colors.secondaryContent,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            GlasmioButton(
                onClick = if (!hasAllFilesPermission) onGrantAllFilesPermission else onRefreshInApp,
                emphasis = GlassEmphasis.Prominent,
                navigationSurfaceColor = semantic.accentBlue,
                size = GlasmioButtonSize.Medium,
                modifier = Modifier.testTag("in_app_picker_empty_action_button"),
            ) {
                SfIcon(
                    systemName = if (!hasAllFilesPermission) ThazhSymbols.ShieldCheck else ThazhSymbols.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    size = 16.dp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                GlasmioText(
                    text = stringResource(
                        if (!hasAllFilesPermission) {
                            R.string.picker_all_files_permission_button
                        } else {
                            R.string.browser_action_reload
                        },
                    ),
                    style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }
    }
}
