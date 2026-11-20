package com.thazh.share.ui.components

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
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
}

private data class SelectablePickerEntry(
    val key: String,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val category: FileCategory,
    val modifiedAtEpochSec: Long,
)

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

private fun hasAnyMediaCatalogPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val img = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val vid = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        val aud = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        img || vid || aud
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Queries on-device media, documents, downloads, archives, APKs, and in-app received files
 * directly inside the application so the user can pick files and photos in-app without
 * redirecting to an external system picker activity.
 */
private fun loadOnDeviceCatalogEntries(
    context: Context,
    inAppReceivedFiles: List<ReceivedFile>,
): List<SelectablePickerEntry> {
    val entriesMap = LinkedHashMap<String, SelectablePickerEntry>()

    // 1. In-app received files on disk
    inAppReceivedFiles.forEach { rf ->
        val f = File(rf.filePath)
        if (f.isFile && f.length() > 0L) {
            val uri = Uri.fromFile(f)
            val key = uri.toString()
            entriesMap[key] = SelectablePickerEntry(
                key = key,
                uri = uri,
                name = rf.fileName,
                sizeBytes = f.length().coerceAtLeast(rf.sizeBytes),
                mimeType = rf.mimeType,
                category = rf.category,
                modifiedAtEpochSec = (rf.receivedAtEpochMs / 1000L).coerceAtLeast(f.lastModified() / 1000L),
            )
        }
    }

    // 2. App-owned directories & public Downloads/Documents directories accessible via File API
    val candidateFolders = buildList {
        add(File(context.filesDir, "share_files"))
        context.getExternalFilesDir(null)?.let { add(it) }
        runCatching {
            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            add(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Thazh Share"))
            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))
            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
            add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))
        }
    }
    candidateFolders.forEach { folder ->
        runCatching {
            if (folder.isDirectory && folder.canRead()) {
                folder.listFiles()?.take(80)?.forEach { f ->
                    if (f.isFile && f.length() > 0L && !f.name.startsWith(".")) {
                        val uri = Uri.fromFile(f)
                        val key = uri.toString()
                        if (!entriesMap.containsKey(key)) {
                            val mime = com.thazh.share.util.FileNameUtils.guessMimeType(f.name)
                            entriesMap[key] = SelectablePickerEntry(
                                key = key,
                                uri = uri,
                                name = f.name,
                                sizeBytes = f.length(),
                                mimeType = mime,
                                category = FileCategory.from(mime, f.name),
                                modifiedAtEpochSec = f.lastModified() / 1000L,
                            )
                        }
                    }
                }
            }
        }
    }

    // 3. MediaStore queries (Photos, Videos, Audio, Downloads, and General Files)
    val resolver = context.contentResolver
    val projection = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.DATE_MODIFIED,
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

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val rawName = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    if (size <= 0L || rawName.isNullOrBlank() || rawName.startsWith(".")) continue

                    val rawMime = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                    val mime = rawMime?.takeIf { it.isNotBlank() }
                        ?: defaultMimePrefix
                        ?: com.thazh.share.util.FileNameUtils.guessMimeType(rawName)
                    val modifiedSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L

                    val contentUri = ContentUris.withAppendedId(collectionUri, id)
                    val key = contentUri.toString()
                    if (!entriesMap.containsKey(key)) {
                        entriesMap[key] = SelectablePickerEntry(
                            key = key,
                            uri = contentUri,
                            name = rawName,
                            sizeBytes = size,
                            mimeType = mime,
                            category = FileCategory.from(mime, rawName),
                            modifiedAtEpochSec = modifiedSec,
                        )
                        count++
                    }
                }
            }
        }
    }

    queryCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*", limit = 120)
    queryCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*", limit = 80)
    queryCollection(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio/*", limit = 80)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        queryCollection(MediaStore.Downloads.EXTERNAL_CONTENT_URI, null, limit = 100)
    }
    queryCollection(MediaStore.Files.getContentUri("external"), null, limit = 140)

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
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val scope = rememberCoroutineScope()

    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<FileCategory?>(null) }

    var catalogRefreshTick by remember { mutableIntStateOf(0) }
    var hasMediaPermission by remember {
        mutableStateOf(hasAnyMediaCatalogPermission(context))
    }
    var isLoadingCatalog by remember { mutableStateOf(true) }
    var deviceCatalogEntries by remember { mutableStateOf<List<SelectablePickerEntry>>(emptyList()) }

    val stagedSystemEntries = remember { mutableStateListOf<SelectablePickerEntry>() }
    val selectedKeys = remember { mutableStateListOf<String>() }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        hasMediaPermission = hasAnyMediaCatalogPermission(context)
        catalogRefreshTick++
    }

    // Request media read permissions automatically on first open if not yet granted
    // so photos, videos, audio, and files appear right inside the in-app picker.
    LaunchedEffect(Unit) {
        if (!hasMediaPermission) {
            runCatching {
                mediaPermissionLauncher.launch(requiredMediaCatalogPermissions())
            }
        }
    }

    LaunchedEffect(inAppReceivedFiles, hasMediaPermission, catalogRefreshTick) {
        isLoadingCatalog = true
        val loaded = withContext(Dispatchers.IO) {
            loadOnDeviceCatalogEntries(context, inAppReceivedFiles)
        }
        deviceCatalogEntries = loaded
        isLoadingCatalog = false
    }

    // Optional secondary fallback system picker in top bar only if user wants external SAF provider
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

    val allEntries = remember(deviceCatalogEntries, stagedSystemEntries.toList()) {
        val map = LinkedHashMap<String, SelectablePickerEntry>()
        stagedSystemEntries.forEach { map[it.key] = it }
        deviceCatalogEntries.forEach { if (!map.containsKey(it.key)) map[it.key] = it }
        map.values.toList()
    }

    val visibleEntries = remember(allEntries, activeTab, searchQuery.text, selectedCategory) {
        val q = searchQuery.text.trim().lowercase()
        allEntries.filter { entry ->
            val matchesTab = when (activeTab) {
                InAppPickerTab.MEDIA -> entry.category == FileCategory.IMAGE || entry.category == FileCategory.VIDEO
                InAppPickerTab.FILES -> selectedCategory == null || entry.category == selectedCategory
            }
            val matchesQuery = q.isEmpty() || entry.name.lowercase().contains(q)
            matchesTab && matchesQuery
        }
    }

    val selectedEntries = remember(allEntries, selectedKeys.toList()) {
        allEntries.filter { it.key in selectedKeys }
    }
    val totalSelectedBytes = remember(selectedEntries) {
        selectedEntries.sumOf { it.sizeBytes.coerceAtLeast(0L) }
    }

    val sheetTitle = if (activeTab == InAppPickerTab.MEDIA) {
        stringResource(R.string.picker_media_title)
    } else {
        stringResource(R.string.picker_files_title)
    }

    val categoryTabItems = remember(
        stringResource(R.string.received_files_filter_all),
    ) {
        null
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
                .heightIn(min = 450.dp, max = 610.dp)
                .padding(
                    top = sheetPadding.calculateTopPadding() + 8.dp,
                    bottom = sheetPadding.calculateBottomPadding() + 16.dp,
                )
                .padding(horizontal = 16.dp)
                .testTag("in_app_file_media_picker_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 1. GlasmioSegmentedControl: Files vs Photos & Videos
            GlasmioSegmentedControl(
                items = listOf(
                    InAppPickerTab.FILES to stringResource(R.string.picker_tab_files),
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SfIcon(
                        systemName = if (tab == InAppPickerTab.MEDIA) {
                            ThazhSymbols.PhotoLibrary
                        } else {
                            ThazhSymbols.AttachFile
                        },
                        contentDescription = null,
                        tint = if (isSelected) colors.accent else colors.secondaryContent,
                        size = 15.dp,
                    )
                    GlasmioText(
                        text = if (tab == InAppPickerTab.MEDIA) {
                            stringResource(R.string.picker_tab_media)
                        } else {
                            stringResource(R.string.picker_tab_files)
                        },
                        style = GlasmioTheme.textStyles.footnote.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (isSelected) colors.content else colors.secondaryContent,
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

            // 3. Authentic GlasmioScrollableTabRow for File Category filtering
            if (activeTab == InAppPickerTab.FILES) {
                GlasmioScrollableTabRow(
                    items = filterTabs,
                    selected = selectedCategory,
                    onSelected = { cat ->
                        selectedCategory = if (selectedCategory == cat) null else cat
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 4. Inline Media Permission Banner if permission not yet granted
            if (!hasMediaPermission) {
                GlasmioCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.PhotoLibrary,
                            contentDescription = null,
                            tint = semantic.accentBlue,
                            size = 20.dp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            GlasmioText(
                                text = stringResource(R.string.picker_permission_banner_title),
                                style = GlasmioTheme.textStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.content,
                            )
                            GlasmioText(
                                text = stringResource(R.string.picker_permission_banner_body),
                                style = GlasmioTheme.textStyles.caption2,
                                color = colors.secondaryContent,
                            )
                        }
                        GlasmioButton(
                            onClick = {
                                mediaPermissionLauncher.launch(requiredMediaCatalogPermissions())
                            },
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

            // 5. Quick Select All / Clear Row
            if (visibleEntries.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlasmioText(
                        text = "${stringResource(R.string.picker_source_in_app_section)} (${visibleEntries.size})",
                        style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.secondaryContent,
                    )
                    val allVisibleSelected = visibleEntries.all { it.key in selectedKeys }
                    GlasmioButton(
                        onClick = {
                            if (allVisibleSelected) {
                                visibleEntries.forEach { selectedKeys.remove(it.key) }
                            } else {
                                visibleEntries.forEach {
                                    if (it.key !in selectedKeys) selectedKeys.add(it.key)
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

            // 6. Content area: In-app Grid for MEDIA, In-app List for FILES
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
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
                        isMediaMode = activeTab == InAppPickerTab.MEDIA,
                        hasPermission = hasMediaPermission,
                        onGrantPermission = {
                            mediaPermissionLauncher.launch(requiredMediaCatalogPermissions())
                        },
                        onRefreshInApp = {
                            catalogRefreshTick++
                        },
                    )
                } else if (activeTab == InAppPickerTab.MEDIA) {
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
                GlasmioText(
                    text = "${stringResource(entry.category.labelRes())} • ${FormatUtils.formatFileSize(entry.sizeBytes)}",
                    style = GlasmioTheme.textStyles.caption1,
                    color = colors.secondaryContent,
                    maxLines = 1,
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
    hasPermission: Boolean,
    onGrantPermission: () -> Unit,
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
                symbol = if (isMediaMode) ThazhSymbols.PhotoLibrary else ThazhSymbols.AttachFile,
                accentColor = semantic.accentBlue,
                boxSize = 54.dp,
                iconSize = 26.dp,
                cornerRadius = 16.dp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            GlasmioText(
                text = stringResource(
                    if (!hasPermission) {
                        R.string.picker_permission_banner_title
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
                    if (!hasPermission) {
                        R.string.picker_permission_banner_body
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
                onClick = if (!hasPermission) onGrantPermission else onRefreshInApp,
                emphasis = GlassEmphasis.Prominent,
                navigationSurfaceColor = semantic.accentBlue,
                size = GlasmioButtonSize.Medium,
                modifier = Modifier.testTag("in_app_picker_empty_action_button"),
            ) {
                SfIcon(
                    systemName = if (!hasPermission) ThazhSymbols.ShieldCheck else ThazhSymbols.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    size = 16.dp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                GlasmioText(
                    text = stringResource(
                        if (!hasPermission) R.string.permission_grant else R.string.browser_action_reload,
                    ),
                    style = GlasmioTheme.textStyles.callout.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }
    }
}
