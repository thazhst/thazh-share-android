package com.thazh.share.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.data.AppThemeMode
import com.thazh.share.data.DeviceVisibilityMode
import com.thazh.share.service.WifiDirectService
import com.thazh.share.ui.auth.GoogleAuthHelper
import com.thazh.share.ui.auth.findActivity
import com.thazh.share.ui.components.GlasmioActivityIndicator
import com.thazh.share.ui.components.GlasmioText
import com.thazh.share.ui.components.IosSwipeBackContainer
import com.thazh.share.ui.components.ThazhSquircleIconBox
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import com.thazh.share.util.BackgroundOptimizationHelper
import com.thazh.share.util.FormatUtils
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.swipeBackExcluded
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.GlassSurfaceStyle
import io.github.neoruaa.glasmio.glass.LocalBlurBackdrop
import io.github.neoruaa.glasmio.glass.LocalGlassBackdrop
import io.github.neoruaa.glasmio.glass.glassBackdropSource
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberCombinedBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.glass.trackBackdropPosition
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.preference.GlasmioEditTextPreference
import io.github.neoruaa.glasmio.preference.GlasmioListPreference
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceEntry
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceGroupTitle
import io.github.neoruaa.glasmio.preference.GlasmioSwitchPreference
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioSearchBar
import io.github.neoruaa.glasmio.ui.GlasmioSegmentedControl
import kotlinx.coroutines.launch

enum class SettingsCategory {
    ACCOUNT_DEVICE,
    VISIBILITY_CONNECTIVITY,
    TRANSFER_SECURITY,
    APPEARANCE,
    DATA,
    ABOUT,
}

@Composable
fun SettingsScreen(
    currentDisplayName: String,
    syncDeviceNameWithAccount: Boolean = true,
    visibilityMode: DeviceVisibilityMode,
    visibilityRemainingSeconds: Int?,
    defaultVisibilityMode: DeviceVisibilityMode,
    confirmIncomingConnection: Boolean,
    backgroundConnectionDialog: Boolean,
    autoSaveToDownloads: Boolean,
    soundVibrationEnabled: Boolean,
    bleFastDiscoveryEnabled: Boolean,
    isBleSupported: Boolean,
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    themeMode: AppThemeMode,
    reduceTransparency: Boolean,
    liquidGlassEnabled: Boolean,
    receivedFilesCount: Int,
    receivedFilesTotalBytes: Long,
    savedDevicesCount: Int,
    onUpdateDisplayName: (String) -> Unit,
    onToggleSyncDeviceNameWithAccount: (Boolean) -> Unit = {},
    onSelectVisibilityMode: (DeviceVisibilityMode) -> Unit,
    onSelectDefaultVisibilityMode: (DeviceVisibilityMode) -> Unit,
    onToggleConfirmIncomingConnection: (Boolean) -> Unit,
    onToggleBackgroundConnectionDialog: (Boolean) -> Unit,
    onToggleAutoSaveToDownloads: (Boolean) -> Unit,
    onToggleSoundVibration: (Boolean) -> Unit,
    onToggleBleFastDiscovery: (Boolean) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleReduceTransparency: (Boolean) -> Unit,
    onToggleLiquidGlass: (Boolean) -> Unit,
    onOpenNfcSettings: () -> Unit,
    onOpenReceivedFiles: () -> Unit,
    onClearTempStagingCache: () -> Unit,
    onClearSavedDevices: () -> Unit,
    onOpenAboutApp: () -> Unit = {},
    onSubPageActiveChange: (Boolean) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val manufacturer = remember {
        Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    }
    var showClearSavedDevicesDialog by remember { mutableStateOf(false) }

    // Reactive Firebase Auth state for Account section & Device Name sync
    var currentUser by remember {
        mutableStateOf<FirebaseUser?>(runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull())
    }

    DisposableEffect(Unit) {
        val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth?.addAuthStateListener(listener)
        onDispose {
            auth?.removeAuthStateListener(listener)
            onSubPageActiveChange(false)
        }
    }

    val appVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)?.versionName
        }.getOrNull() ?: "1.2.0"
    }

    // Sub-page navigation state inside Settings (each of the 6 categories has its own dedicated page)
    var selectedCategory by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }

    LaunchedEffect(selectedCategory) {
        onSubPageActiveChange(selectedCategory != null)
    }

    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    val slideSpec = remember {
        spring<IntOffset>(dampingRatio = 0.88f, stiffness = 420f)
    }

    @Composable
    fun RenderMainSettingsHub(interactive: Boolean) {
        SettingsHubPage(
            currentDisplayName = currentDisplayName,
            syncDeviceNameWithAccount = syncDeviceNameWithAccount,
            currentUser = currentUser,
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
            receivedFilesCount = receivedFilesCount,
            receivedFilesTotalBytes = receivedFilesTotalBytes,
            savedDevicesCount = savedDevicesCount,
            manufacturer = manufacturer,
            appVersion = appVersion,
            interactive = interactive,
            onSelectCategory = { category -> selectedCategory = category },
            onUpdateDisplayName = onUpdateDisplayName,
            onToggleSyncDeviceNameWithAccount = onToggleSyncDeviceNameWithAccount,
            onSelectVisibilityMode = onSelectVisibilityMode,
            onSelectDefaultVisibilityMode = onSelectDefaultVisibilityMode,
            onToggleConfirmIncomingConnection = onToggleConfirmIncomingConnection,
            onToggleBackgroundConnectionDialog = onToggleBackgroundConnectionDialog,
            onToggleAutoSaveToDownloads = onToggleAutoSaveToDownloads,
            onToggleSoundVibration = onToggleSoundVibration,
            onToggleBleFastDiscovery = onToggleBleFastDiscovery,
            onSelectThemeMode = onSelectThemeMode,
            onToggleReduceTransparency = onToggleReduceTransparency,
            onToggleLiquidGlass = onToggleLiquidGlass,
            onOpenNfcSettings = onOpenNfcSettings,
            onOpenReceivedFiles = onOpenReceivedFiles,
            onClearTempStagingCache = onClearTempStagingCache,
            onRequestClearSavedDevices = { showClearSavedDevicesDialog = true },
            onBack = onBack,
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = selectedCategory,
            transitionSpec = {
                if (targetState != null && initialState == null) {
                    slideInHorizontally(slideSpec) { it } + fadeIn(tween(180)) togetherWith
                        slideOutHorizontally(slideSpec) { -(it * 0.28f).toInt() } + fadeOut(tween(150))
                } else {
                    slideInHorizontally(slideSpec) { -(it * 0.28f).toInt() } + fadeIn(tween(180)) togetherWith
                        slideOutHorizontally(slideSpec) { it } + fadeOut(tween(150))
                }
            },
            label = "settings_category_page_transition",
        ) { category ->
            if (category == null) {
                RenderMainSettingsHub(interactive = true)
            } else {
                IosSwipeBackContainer(
                    enabled = true,
                    onBack = { selectedCategory = null },
                    previousContent = { RenderMainSettingsHub(interactive = false) },
                ) {
                    when (category) {
                        SettingsCategory.ACCOUNT_DEVICE -> {
                            AccountDeviceSettingsPage(
                                currentDisplayName = currentDisplayName,
                                syncDeviceNameWithAccount = syncDeviceNameWithAccount,
                                currentUser = currentUser,
                                onUpdateDisplayName = onUpdateDisplayName,
                                onToggleSyncDeviceNameWithAccount = onToggleSyncDeviceNameWithAccount,
                                onBack = { selectedCategory = null },
                            )
                        }

                        SettingsCategory.VISIBILITY_CONNECTIVITY -> {
                            VisibilityConnectivitySettingsPage(
                                visibilityMode = visibilityMode,
                                visibilityRemainingSeconds = visibilityRemainingSeconds,
                                defaultVisibilityMode = defaultVisibilityMode,
                                bleFastDiscoveryEnabled = bleFastDiscoveryEnabled,
                                isBleSupported = isBleSupported,
                                isNfcSupported = isNfcSupported,
                                isNfcEnabled = isNfcEnabled,
                                onSelectVisibilityMode = onSelectVisibilityMode,
                                onSelectDefaultVisibilityMode = onSelectDefaultVisibilityMode,
                                onToggleBleFastDiscovery = onToggleBleFastDiscovery,
                                onOpenNfcSettings = onOpenNfcSettings,
                                onBack = { selectedCategory = null },
                            )
                        }

                        SettingsCategory.TRANSFER_SECURITY -> {
                            TransferSecuritySettingsPage(
                                confirmIncomingConnection = confirmIncomingConnection,
                                backgroundConnectionDialog = backgroundConnectionDialog,
                                autoSaveToDownloads = autoSaveToDownloads,
                                soundVibrationEnabled = soundVibrationEnabled,
                                onToggleConfirmIncomingConnection = onToggleConfirmIncomingConnection,
                                onToggleBackgroundConnectionDialog = onToggleBackgroundConnectionDialog,
                                onToggleAutoSaveToDownloads = onToggleAutoSaveToDownloads,
                                onToggleSoundVibration = onToggleSoundVibration,
                                onBack = { selectedCategory = null },
                            )
                        }

                        SettingsCategory.APPEARANCE -> {
                            AppearanceSettingsPage(
                                themeMode = themeMode,
                                reduceTransparency = reduceTransparency,
                                liquidGlassEnabled = liquidGlassEnabled,
                                onSelectThemeMode = onSelectThemeMode,
                                onToggleReduceTransparency = onToggleReduceTransparency,
                                onToggleLiquidGlass = onToggleLiquidGlass,
                                onBack = { selectedCategory = null },
                            )
                        }

                        SettingsCategory.DATA -> {
                            DataSettingsPage(
                                receivedFilesCount = receivedFilesCount,
                                receivedFilesTotalBytes = receivedFilesTotalBytes,
                                savedDevicesCount = savedDevicesCount,
                                manufacturer = manufacturer,
                                onOpenReceivedFiles = onOpenReceivedFiles,
                                onClearTempStagingCache = onClearTempStagingCache,
                                onRequestClearSavedDevices = { showClearSavedDevicesDialog = true },
                                onBack = { selectedCategory = null },
                            )
                        }

                        SettingsCategory.ABOUT -> {
                            AboutAppScreen(
                                onBack = { selectedCategory = null },
                            )
                        }
                    }
                }
            }
        }
    }

    GlasmioAlertDialog(
        visible = showClearSavedDevicesDialog,
        onDismissRequest = { showClearSavedDevicesDialog = false },
        title = stringResource(R.string.settings_clear_saved_devices),
        message = stringResource(R.string.settings_clear_saved_devices_confirm, savedDevicesCount),
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.cancel),
                onClick = { showClearSavedDevicesDialog = false },
                role = GlasmioAlertButtonRole.Cancel,
            ),
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.delete),
                onClick = {
                    showClearSavedDevicesDialog = false
                    onClearSavedDevices()
                },
                role = GlasmioAlertButtonRole.Destructive,
            ),
        ),
    )
}

@Composable
private fun SettingsHubPage(
    currentDisplayName: String,
    syncDeviceNameWithAccount: Boolean,
    currentUser: FirebaseUser?,
    visibilityMode: DeviceVisibilityMode,
    visibilityRemainingSeconds: Int?,
    defaultVisibilityMode: DeviceVisibilityMode,
    confirmIncomingConnection: Boolean,
    backgroundConnectionDialog: Boolean,
    autoSaveToDownloads: Boolean,
    soundVibrationEnabled: Boolean,
    bleFastDiscoveryEnabled: Boolean,
    isBleSupported: Boolean,
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    themeMode: AppThemeMode,
    reduceTransparency: Boolean,
    liquidGlassEnabled: Boolean,
    receivedFilesCount: Int,
    receivedFilesTotalBytes: Long,
    savedDevicesCount: Int,
    manufacturer: String,
    appVersion: String,
    interactive: Boolean,
    onSelectCategory: (SettingsCategory) -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onToggleSyncDeviceNameWithAccount: (Boolean) -> Unit,
    onSelectVisibilityMode: (DeviceVisibilityMode) -> Unit,
    onSelectDefaultVisibilityMode: (DeviceVisibilityMode) -> Unit,
    onToggleConfirmIncomingConnection: (Boolean) -> Unit,
    onToggleBackgroundConnectionDialog: (Boolean) -> Unit,
    onToggleAutoSaveToDownloads: (Boolean) -> Unit,
    onToggleSoundVibration: (Boolean) -> Unit,
    onToggleBleFastDiscovery: (Boolean) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleReduceTransparency: (Boolean) -> Unit,
    onToggleLiquidGlass: (Boolean) -> Unit,
    onOpenNfcSettings: () -> Unit,
    onOpenReceivedFiles: () -> Unit,
    onClearTempStagingCache: () -> Unit,
    onRequestClearSavedDevices: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val appearance = GlasmioTheme.appearance
    val semantic = ThazhTheme.semantic
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    // Floating Search Bar state
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val normalizedQuery = remember(searchQuery.text) { searchQuery.text.trim().lowercase() }

    fun matchesSearch(vararg keywords: String): Boolean {
        if (normalizedQuery.isEmpty()) return true
        return keywords.any { it.lowercase().contains(normalizedQuery) }
    }

    val matchAccountCategory = matchesSearch(
        "tài khoản", "thiết bị", "google", "đăng nhập", "đăng xuất", "tên thiết bị",
        "đồng bộ tên", "account", "device", currentDisplayName, currentUser?.email.orEmpty(),
    )
    val matchVisibilityCategory = matchesSearch(
        "hiển thị", "kết nối", "ẩn", "10 phút", "không giới hạn", "mặc định",
        "bluetooth", "ble", "quét nhanh", "nfc", "chạm", "radar", "wifi direct",
    )
    val matchTransferSecurityCategory = matchesSearch(
        "nhận tệp", "bảo mật", "xác nhận", "hộp thoại", "chạy nền",
        "tự lưu", "tải xuống", "âm báo", "rung", "chuông",
    )
    val matchAppearanceCategory = matchesSearch(
        "giao diện", "chế độ màu", "sáng", "tối", "hệ thống",
        "liquid glass", "thủy tinh", "trong suốt", "theme", "dark", "light",
    )
    val matchDataCategory = matchesSearch(
        "dữ liệu", "tệp đã nhận", "lưu trữ", "dọn", "bộ nhớ tạm", "cache",
        "xóa thiết bị", "đã lưu", "chạy nền", "pin", manufacturer,
    )
    val matchAboutCategory = matchesSearch(
        "giới thiệu", "thông tin", "phiên bản", "thazh share", "package",
        "credit", "glasmio", "catbox", "giấy phép", "apache", "license",
    )

    val hasAnyResult = matchAccountCategory ||
        matchVisibilityCategory ||
        matchTransferSecurityCategory ||
        matchAppearanceCategory ||
        matchDataCategory ||
        matchAboutCategory

    val accountSummaryLabel = currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser?.email
        ?: stringResource(R.string.settings_account_sign_in_short)

    val currentVisibilitySummary = when (visibilityMode) {
        DeviceVisibilityMode.HIDDEN -> stringResource(R.string.visibility_mode_hidden_long)
        DeviceVisibilityMode.TEMPORARY_10_MIN -> {
            val rem = visibilityRemainingSeconds
            if (rem != null && rem > 0) {
                stringResource(
                    R.string.visibility_desc_10min,
                    WifiDirectService.formatCountdown(rem),
                )
            } else {
                stringResource(R.string.visibility_mode_10min_long)
            }
        }
        DeviceVisibilityMode.ALWAYS_VISIBLE -> stringResource(R.string.visibility_mode_always_long)
    }

    val themeModeSummary = when (themeMode) {
        AppThemeMode.SYSTEM -> stringResource(R.string.settings_theme_mode_system)
        AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_mode_light)
        AppThemeMode.DARK -> stringResource(R.string.settings_theme_mode_dark)
    }

    val hostBackdrop = LocalGlassBackdrop.current
    val pageLayerBackdrop = rememberLayerBackdrop()
    val floatingSearchBackdrop = rememberCombinedBackdrop(hostBackdrop, pageLayerBackdrop)

    val floatingBottomInsets = WindowInsets.navigationBars
        .only(WindowInsetsSides.Bottom)
        .union(WindowInsets.ime.only(WindowInsetsSides.Bottom))
        .add(WindowInsets(bottom = 14.dp))
        .asPaddingValues()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassBackdropSource(pageLayerBackdrop)
                .trackBackdropPosition(pageLayerBackdrop),
        ) {
            GlasmioPinnedListPage(
                title = stringResource(R.string.settings_title),
                modifier = Modifier.fillMaxSize(),
                onNavigateBack = onBack,
                navigateBackContentDescription = stringResource(R.string.back),
                bottomPadding = 96.dp,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                backgroundColor = colors.groupedBackground.copy(
                    alpha = if (appearance.reduceTransparency) 1f else 0.78f,
                ),
                actions = {
                    GlasmioIconButton(
                        onClick = {
                            searchActive = true
                            searchFocusRequester.requestFocus()
                            keyboard?.show()
                        },
                        modifier = Modifier.testTag("settings_top_search_button"),
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.Search,
                            contentDescription = stringResource(R.string.settings_search_placeholder),
                        )
                    }
                },
            ) {
                if (!hasAnyResult) {
                    item(key = "settings_search_empty") {
                        CenteredSettingWidth {
                            GlasmioCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    SfIcon(
                                        systemName = ThazhSymbols.Search,
                                        contentDescription = null,
                                        tint = colors.tertiaryContent,
                                        size = 30.dp,
                                    )
                                    GlasmioText(
                                        text = stringResource(R.string.settings_search_empty),
                                        style = GlasmioTheme.textStyles.subheadline,
                                        color = colors.secondaryContent,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item(key = "settings_categories_header") {
                        CenteredSettingWidth {
                            GlasmioPreferenceGroupTitle(
                                title = stringResource(
                                    if (normalizedQuery.isEmpty()) {
                                        R.string.settings_categories_header
                                    } else {
                                        R.string.settings_search_results_header
                                    }
                                )
                            )
                        }
                    }

                    // 6 Main Category Entries (each opens its own dedicated page)
                    item(key = "settings_categories_group") {
                        CenteredSettingWidth {
                            GlasmioGroupedList(
                                modifier = Modifier.animateContentSize(
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                ),
                            ) {
                                var hasRenderedCategory = false

                                if (matchAccountCategory) {
                                    hasRenderedCategory = true
                                    CategoryMenuRow(
                                        title = stringResource(R.string.settings_category_account_device_title),
                                        description = stringResource(
                                            R.string.settings_category_account_device_desc,
                                            currentDisplayName,
                                            accountSummaryLabel,
                                        ),
                                        symbol = ThazhSymbols.AccountCircle,
                                        badgeColor = semantic.accentBlue,
                                        showTopSeparator = false,
                                        onClick = { onSelectCategory(SettingsCategory.ACCOUNT_DEVICE) },
                                        testTag = "settings_category_account_device",
                                    )
                                }

                                if (matchVisibilityCategory) {
                                    CategoryMenuRow(
                                        title = stringResource(R.string.settings_category_visibility_title),
                                        description = stringResource(
                                            R.string.settings_category_visibility_desc,
                                            currentVisibilitySummary,
                                        ),
                                        symbol = ThazhSymbols.WifiTethering,
                                        badgeColor = semantic.accentGreen,
                                        showTopSeparator = hasRenderedCategory,
                                        onClick = { onSelectCategory(SettingsCategory.VISIBILITY_CONNECTIVITY) },
                                        testTag = "settings_category_visibility_connectivity",
                                    )
                                    hasRenderedCategory = true
                                }

                                if (matchTransferSecurityCategory) {
                                    CategoryMenuRow(
                                        title = stringResource(R.string.settings_category_transfer_security_title),
                                        description = stringResource(R.string.settings_category_transfer_security_desc),
                                        symbol = ThazhSymbols.ShieldCheck,
                                        badgeColor = semantic.warningOrange,
                                        showTopSeparator = hasRenderedCategory,
                                        onClick = { onSelectCategory(SettingsCategory.TRANSFER_SECURITY) },
                                        testTag = "settings_category_transfer_security",
                                    )
                                    hasRenderedCategory = true
                                }

                                if (matchAppearanceCategory) {
                                    CategoryMenuRow(
                                        title = stringResource(R.string.settings_category_appearance_title),
                                        description = stringResource(
                                            R.string.settings_category_appearance_desc,
                                            themeModeSummary,
                                        ),
                                        symbol = ThazhSymbols.LiquidGlass,
                                        badgeColor = semantic.accentCyan,
                                        showTopSeparator = hasRenderedCategory,
                                        onClick = { onSelectCategory(SettingsCategory.APPEARANCE) },
                                        testTag = "settings_category_appearance",
                                    )
                                    hasRenderedCategory = true
                                }

                                if (matchDataCategory) {
                                    CategoryMenuRow(
                                        title = stringResource(R.string.settings_category_data_title),
                                        description = stringResource(
                                            R.string.settings_category_data_desc,
                                            receivedFilesCount,
                                            FormatUtils.formatFileSize(receivedFilesTotalBytes),
                                        ),
                                        symbol = ThazhSymbols.Storage,
                                        badgeColor = semantic.accentBlue,
                                        showTopSeparator = hasRenderedCategory,
                                        onClick = { onSelectCategory(SettingsCategory.DATA) },
                                        testTag = "settings_category_data",
                                    )
                                    hasRenderedCategory = true
                                }

                                if (matchAboutCategory) {
                                    CategoryMenuRow(
                                        title = stringResource(R.string.settings_category_about_title),
                                        description = stringResource(R.string.settings_category_about_desc, appVersion),
                                        symbol = ThazhSymbols.Info,
                                        badgeColor = semantic.categoryDocument,
                                        showTopSeparator = hasRenderedCategory,
                                        onClick = { onSelectCategory(SettingsCategory.ABOUT) },
                                        testTag = "settings_about_entry",
                                    )
                                }
                            }
                        }
                    }

                    // If user is actively typing a search query in the floating search bar,
                    // also show matching direct settings below so they can toggle immediately!
                    if (normalizedQuery.isNotEmpty()) {
                        if (matchAccountCategory) {
                            item(key = "search_direct_account") {
                                CenteredSettingWidth {
                                    AccountDeviceSettingsCardContent(
                                        currentDisplayName = currentDisplayName,
                                        syncDeviceNameWithAccount = syncDeviceNameWithAccount,
                                        currentUser = currentUser,
                                        onUpdateDisplayName = onUpdateDisplayName,
                                        onToggleSyncDeviceNameWithAccount = onToggleSyncDeviceNameWithAccount,
                                    )
                                }
                            }
                        }

                        if (matchVisibilityCategory) {
                            item(key = "search_direct_visibility") {
                                CenteredSettingWidth {
                                    VisibilityConnectivityCardContent(
                                        visibilityMode = visibilityMode,
                                        visibilityRemainingSeconds = visibilityRemainingSeconds,
                                        defaultVisibilityMode = defaultVisibilityMode,
                                        bleFastDiscoveryEnabled = bleFastDiscoveryEnabled,
                                        isBleSupported = isBleSupported,
                                        isNfcSupported = isNfcSupported,
                                        isNfcEnabled = isNfcEnabled,
                                        onSelectVisibilityMode = onSelectVisibilityMode,
                                        onSelectDefaultVisibilityMode = onSelectDefaultVisibilityMode,
                                        onToggleBleFastDiscovery = onToggleBleFastDiscovery,
                                        onOpenNfcSettings = onOpenNfcSettings,
                                    )
                                }
                            }
                        }

                        if (matchTransferSecurityCategory) {
                            item(key = "search_direct_transfer") {
                                CenteredSettingWidth {
                                    TransferSecurityCardContent(
                                        confirmIncomingConnection = confirmIncomingConnection,
                                        backgroundConnectionDialog = backgroundConnectionDialog,
                                        autoSaveToDownloads = autoSaveToDownloads,
                                        soundVibrationEnabled = soundVibrationEnabled,
                                        onToggleConfirmIncomingConnection = onToggleConfirmIncomingConnection,
                                        onToggleBackgroundConnectionDialog = onToggleBackgroundConnectionDialog,
                                        onToggleAutoSaveToDownloads = onToggleAutoSaveToDownloads,
                                        onToggleSoundVibration = onToggleSoundVibration,
                                    )
                                }
                            }
                        }

                        if (matchAppearanceCategory) {
                            item(key = "search_direct_appearance") {
                                CenteredSettingWidth {
                                    AppearanceCardContent(
                                        themeMode = themeMode,
                                        reduceTransparency = reduceTransparency,
                                        liquidGlassEnabled = liquidGlassEnabled,
                                        onSelectThemeMode = onSelectThemeMode,
                                        onToggleReduceTransparency = onToggleReduceTransparency,
                                        onToggleLiquidGlass = onToggleLiquidGlass,
                                    )
                                }
                            }
                        }

                        if (matchDataCategory) {
                            item(key = "search_direct_data") {
                                CenteredSettingWidth {
                                    DataSettingsCardContent(
                                        receivedFilesCount = receivedFilesCount,
                                        receivedFilesTotalBytes = receivedFilesTotalBytes,
                                        savedDevicesCount = savedDevicesCount,
                                        manufacturer = manufacturer,
                                        onOpenReceivedFiles = onOpenReceivedFiles,
                                        onClearTempStagingCache = onClearTempStagingCache,
                                        onRequestClearSavedDevices = onRequestClearSavedDevices,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // iOS & Glasmio Floating Liquid Glass Search Bar (Thanh tìm kiếm nổi)
        if (interactive) {
            CompositionLocalProvider(
                LocalGlassBackdrop provides floatingSearchBackdrop,
                LocalBlurBackdrop provides floatingSearchBackdrop,
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(floatingBottomInsets)
                        .padding(horizontal = 16.dp)
                        .swipeBackExcluded(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 520.dp)
                            .fillMaxWidth()
                            .shadow(
                                elevation = 14.dp,
                                shape = ContinuousCapsule,
                                ambientColor = Color.Black.copy(alpha = 0.18f),
                                spotColor = Color.Black.copy(alpha = 0.22f),
                            )
                            .clip(ContinuousCapsule)
                            .background(
                                colors.elevatedBackground.copy(
                                    alpha = if (appearance.reduceTransparency) 0.96f else 0.76f
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = colors.content.copy(alpha = if (colors.isDark) 0.16f else 0.10f),
                                shape = ContinuousCapsule,
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    ) {
                        GlasmioSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            active = searchActive,
                            onActiveChange = { searchActive = it },
                            onClose = {
                                searchQuery = TextFieldValue("")
                                searchActive = false
                                focusManager.clearFocus()
                                keyboard?.hide()
                            },
                            onSearch = {
                                focusManager.clearFocus()
                                keyboard?.hide()
                            },
                            placeholder = stringResource(R.string.settings_search_placeholder),
                            closeContentDescription = stringResource(R.string.close),
                            backdrop = floatingSearchBackdrop,
                            style = GlassSurfaceStyle.Navigation,
                            controlHeight = 46.dp,
                            focusRequester = searchFocusRequester,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_search_bar"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryMenuRow(
    title: String,
    description: String,
    symbol: String,
    badgeColor: Color,
    showTopSeparator: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    val colors = GlasmioTheme.colors
    GlasmioPreferenceEntry(
        title = { GlasmioText(title) },
        description = description,
        icon = {
            ThazhSquircleIconBox(
                symbol = symbol,
                accentColor = badgeColor,
            )
        },
        trailingContent = {
            SfIcon(
                systemName = ThazhSymbols.ChevronRight,
                contentDescription = null,
                tint = colors.tertiaryContent,
                size = 18.dp,
            )
        },
        onClick = onClick,
        showTopSeparator = showTopSeparator,
        modifier = Modifier.testTag(testTag),
    )
}

// ============================================================================
// 1. TÀI KHOẢN & THIẾT BỊ SUB-PAGE
// ============================================================================

@Composable
private fun AccountDeviceSettingsPage(
    currentDisplayName: String,
    syncDeviceNameWithAccount: Boolean,
    currentUser: FirebaseUser?,
    onUpdateDisplayName: (String) -> Unit,
    onToggleSyncDeviceNameWithAccount: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val appearance = GlasmioTheme.appearance

    GlasmioPinnedListPage(
        title = stringResource(R.string.settings_category_account_device_title),
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_page_account_device"),
        onNavigateBack = onBack,
        navigateBackContentDescription = stringResource(R.string.back),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        backgroundColor = colors.groupedBackground.copy(
            alpha = if (appearance.reduceTransparency) 1f else 0.78f,
        ),
    ) {
        item(key = "account_device_card") {
            CenteredSettingWidth {
                AccountDeviceSettingsCardContent(
                    currentDisplayName = currentDisplayName,
                    syncDeviceNameWithAccount = syncDeviceNameWithAccount,
                    currentUser = currentUser,
                    onUpdateDisplayName = onUpdateDisplayName,
                    onToggleSyncDeviceNameWithAccount = onToggleSyncDeviceNameWithAccount,
                )
            }
        }
    }
}

@Composable
private fun AccountDeviceSettingsCardContent(
    currentDisplayName: String,
    syncDeviceNameWithAccount: Boolean,
    currentUser: FirebaseUser?,
    onUpdateDisplayName: (String) -> Unit,
    onToggleSyncDeviceNameWithAccount: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    var isSigningIn by remember { mutableStateOf(false) }
    val user = currentUser
    val accountDisplayName = user?.displayName?.trim()?.takeIf { it.isNotEmpty() }

    GlasmioGroupedList {
        if (user == null) {
            GlasmioPreferenceEntry(
                title = { GlasmioText(stringResource(R.string.settings_account_title)) },
                description = stringResource(R.string.settings_account_signed_out_desc),
                icon = {
                    ThazhSquircleIconBox(
                        symbol = ThazhSymbols.AccountCircle,
                        accentColor = semantic.accentBlue,
                    )
                },
                trailingContent = {
                    GlasmioButton(
                        onClick = {
                            if (isSigningIn) return@GlasmioButton
                            isSigningIn = true
                            coroutineScope.launch {
                                try {
                                    val activity = context.findActivity()
                                    GoogleAuthHelper.onGoogleSignInClicked(
                                        activityContext = activity,
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        },
                                    )
                                } finally {
                                    isSigningIn = false
                                }
                            }
                        },
                        emphasis = GlassEmphasis.Prominent,
                        navigationSurfaceColor = semantic.accentBlue,
                        size = GlasmioButtonSize.Small,
                        modifier = Modifier.testTag("settings_google_sign_in_button"),
                    ) {
                        if (isSigningIn) {
                            GlasmioActivityIndicator(
                                size = 14.dp,
                                color = Color.White,
                                strokeWidth = 1.8.dp,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        GlasmioText(
                            text = stringResource(R.string.settings_account_sign_in_short),
                            style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                },
                showTopSeparator = false,
                modifier = Modifier.testTag("settings_account_entry"),
            )
        } else {
            GlasmioPreferenceEntry(
                title = {
                    GlasmioText(
                        text = accountDisplayName
                            ?: user.email
                            ?: stringResource(R.string.link_share_signed_in_default),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                description = user.email ?: stringResource(R.string.link_share_cloud_sync_active),
                icon = {
                    ThazhSquircleIconBox(
                        symbol = ThazhSymbols.CloudDone,
                        accentColor = semantic.accentGreen,
                    )
                },
                trailingContent = {
                    GlasmioButton(
                        onClick = {
                            coroutineScope.launch {
                                GoogleAuthHelper.signOut(context)
                            }
                        },
                        emphasis = GlassEmphasis.Regular,
                        size = GlasmioButtonSize.Small,
                        modifier = Modifier.testTag("settings_google_sign_out_button"),
                    ) {
                        GlasmioText(
                            text = stringResource(R.string.link_share_sign_out),
                            style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.destructive,
                            maxLines = 1,
                        )
                    }
                },
                showTopSeparator = false,
                modifier = Modifier.testTag("settings_account_entry"),
            )
        }

        GlasmioEditTextPreference(
            title = { GlasmioText(stringResource(R.string.settings_display_name)) },
            value = currentDisplayName.ifBlank { stringResource(R.string.settings_display_name_desc) },
            onValueChange = { newName ->
                val trimmed = newName.trim()
                if (trimmed.isNotEmpty()) {
                    onUpdateDisplayName(trimmed)
                }
            },
            icon = {
                ThazhSquircleIconBox(
                    symbol = ThazhSymbols.Person,
                    accentColor = semantic.accentBlue,
                )
            },
            showTopSeparator = true,
            modifier = Modifier.testTag("display_name_input"),
        )

        GlasmioSwitchPreference(
            title = { GlasmioText(stringResource(R.string.settings_sync_device_name)) },
            description = if (user != null && syncDeviceNameWithAccount) {
                stringResource(R.string.settings_sync_device_name_active_desc)
            } else {
                stringResource(R.string.settings_sync_device_name_desc)
            },
            icon = {
                ThazhSquircleIconBox(
                    symbol = ThazhSymbols.CloudUpload,
                    accentColor = if (syncDeviceNameWithAccount) semantic.accentCyan else colors.secondaryContent,
                )
            },
            checked = syncDeviceNameWithAccount,
            onCheckedChange = onToggleSyncDeviceNameWithAccount,
            showTopSeparator = true,
            modifier = Modifier.testTag("sync_device_name_toggle"),
        )

        if (accountDisplayName != null && accountDisplayName != currentDisplayName) {
            GlasmioPreferenceEntry(
                title = {
                    GlasmioText(
                        text = stringResource(R.string.settings_use_account_name, accountDisplayName),
                        color = semantic.accentBlue,
                    )
                },
                icon = {
                    ThazhSquircleIconBox(
                        symbol = ThazhSymbols.Refresh,
                        accentColor = semantic.accentBlue,
                    )
                },
                onClick = {
                    onUpdateDisplayName(accountDisplayName)
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_use_account_name_toast, accountDisplayName),
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                showTopSeparator = true,
                modifier = Modifier.testTag("settings_use_account_name_entry"),
            )
        }
    }
}

// ============================================================================
// 2. HIỂN THỊ & KẾT NỐI SUB-PAGE
// ============================================================================

@Composable
private fun VisibilityConnectivitySettingsPage(
    visibilityMode: DeviceVisibilityMode,
    visibilityRemainingSeconds: Int?,
    defaultVisibilityMode: DeviceVisibilityMode,
    bleFastDiscoveryEnabled: Boolean,
    isBleSupported: Boolean,
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    onSelectVisibilityMode: (DeviceVisibilityMode) -> Unit,
    onSelectDefaultVisibilityMode: (DeviceVisibilityMode) -> Unit,
    onToggleBleFastDiscovery: (Boolean) -> Unit,
    onOpenNfcSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val appearance = GlasmioTheme.appearance

    GlasmioPinnedListPage(
        title = stringResource(R.string.settings_category_visibility_title),
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_page_visibility_connectivity"),
        onNavigateBack = onBack,
        navigateBackContentDescription = stringResource(R.string.back),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        backgroundColor = colors.groupedBackground.copy(
            alpha = if (appearance.reduceTransparency) 1f else 0.78f,
        ),
    ) {
        item(key = "visibility_connectivity_card") {
            CenteredSettingWidth {
                VisibilityConnectivityCardContent(
                    visibilityMode = visibilityMode,
                    visibilityRemainingSeconds = visibilityRemainingSeconds,
                    defaultVisibilityMode = defaultVisibilityMode,
                    bleFastDiscoveryEnabled = bleFastDiscoveryEnabled,
                    isBleSupported = isBleSupported,
                    isNfcSupported = isNfcSupported,
                    isNfcEnabled = isNfcEnabled,
                    onSelectVisibilityMode = onSelectVisibilityMode,
                    onSelectDefaultVisibilityMode = onSelectDefaultVisibilityMode,
                    onToggleBleFastDiscovery = onToggleBleFastDiscovery,
                    onOpenNfcSettings = onOpenNfcSettings,
                )
            }
        }
    }
}

@Composable
private fun VisibilityConnectivityCardContent(
    visibilityMode: DeviceVisibilityMode,
    visibilityRemainingSeconds: Int?,
    defaultVisibilityMode: DeviceVisibilityMode,
    bleFastDiscoveryEnabled: Boolean,
    isBleSupported: Boolean,
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    onSelectVisibilityMode: (DeviceVisibilityMode) -> Unit,
    onSelectDefaultVisibilityMode: (DeviceVisibilityMode) -> Unit,
    onToggleBleFastDiscovery: (Boolean) -> Unit,
    onOpenNfcSettings: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic

    GlasmioGroupedList {
        val currentVisibilityDesc = when (visibilityMode) {
            DeviceVisibilityMode.HIDDEN -> stringResource(R.string.visibility_desc_hidden)
            DeviceVisibilityMode.TEMPORARY_10_MIN -> {
                val rem = visibilityRemainingSeconds
                if (rem != null && rem > 0) {
                    stringResource(
                        R.string.visibility_desc_10min,
                        WifiDirectService.formatCountdown(rem),
                    )
                } else {
                    stringResource(R.string.visibility_mode_10min_long)
                }
            }
            DeviceVisibilityMode.ALWAYS_VISIBLE -> stringResource(R.string.visibility_desc_always)
        }

        GlasmioListPreference(
            title = { GlasmioText(stringResource(R.string.settings_current_visibility)) },
            description = currentVisibilityDesc,
            values = DeviceVisibilityMode.entries,
            selectedValue = visibilityMode,
            onValueSelected = onSelectVisibilityMode,
            valueText = { mode -> visibilityModeLabel(mode) },
            icon = {
                val (symbol, tint) = when (visibilityMode) {
                    DeviceVisibilityMode.HIDDEN -> ThazhSymbols.VisibilityOff to colors.secondaryContent
                    DeviceVisibilityMode.TEMPORARY_10_MIN -> ThazhSymbols.Timer to semantic.accentBlue
                    DeviceVisibilityMode.ALWAYS_VISIBLE -> ThazhSymbols.VisibilityOn to semantic.accentGreen
                }
                ThazhSquircleIconBox(
                    symbol = symbol,
                    accentColor = tint,
                )
            },
            showTopSeparator = false,
            modifier = Modifier.testTag("settings_current_visibility_pref"),
        )

        GlasmioListPreference(
            title = { GlasmioText(stringResource(R.string.settings_default_visibility)) },
            values = DeviceVisibilityMode.entries,
            selectedValue = defaultVisibilityMode,
            onValueSelected = onSelectDefaultVisibilityMode,
            valueText = { mode -> visibilityModeLabel(mode) },
            icon = {
                ThazhSquircleIconBox(
                    symbol = ThazhSymbols.AllInclusive,
                    accentColor = semantic.accentCyan,
                )
            },
            showTopSeparator = true,
            modifier = Modifier.testTag("settings_default_visibility_pref"),
        )

        GlasmioSwitchPreference(
            title = { GlasmioText(stringResource(R.string.settings_ble_fast_discovery)) },
            description = if (isBleSupported) {
                stringResource(R.string.settings_ble_fast_discovery_desc)
            } else {
                stringResource(R.string.settings_ble_not_supported)
            },
            icon = {
                ThazhSquircleIconBox(
                    symbol = ThazhSymbols.Bluetooth,
                    accentColor = if (isBleSupported) semantic.accentCyan else colors.secondaryContent,
                )
            },
            checked = bleFastDiscoveryEnabled && isBleSupported,
            onCheckedChange = onToggleBleFastDiscovery,
            isEnabled = isBleSupported,
            showTopSeparator = true,
            modifier = Modifier.testTag("ble_fast_discovery_toggle"),
        )

        GlasmioPreferenceEntry(
            title = { GlasmioText(stringResource(R.string.nfc_settings_title)) },
            description = when {
                !isNfcSupported -> stringResource(R.string.nfc_settings_unsupported_desc)
                isNfcEnabled -> stringResource(R.string.nfc_settings_ready_desc)
                else -> stringResource(R.string.nfc_settings_disabled_desc)
            },
            icon = {
                ThazhSquircleIconBox(
                    symbol = ThazhSymbols.Nfc,
                    accentColor = when {
                        !isNfcSupported -> colors.secondaryContent
                        isNfcEnabled -> semantic.accentGreen
                        else -> semantic.warningOrange
                    },
                )
            },
            trailingContent = when {
                isNfcSupported && !isNfcEnabled -> {
                    {
                        GlasmioButton(
                            onClick = onOpenNfcSettings,
                            emphasis = GlassEmphasis.Prominent,
                            navigationSurfaceColor = semantic.warningOrange,
                            size = GlasmioButtonSize.Small,
                            modifier = Modifier.testTag("settings_open_nfc_button"),
                        ) {
                            GlasmioText(
                                text = stringResource(R.string.nfc_open_settings),
                                style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1,
                            )
                        }
                    }
                }
                isNfcSupported && isNfcEnabled -> {
                    {
                        Box(
                            modifier = Modifier
                                .clip(ContinuousCapsule)
                                .background(semantic.accentGreen.copy(alpha = if (colors.isDark) 0.24f else 0.16f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            GlasmioText(
                                text = stringResource(R.string.nfc_status_ready),
                                style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                                color = semantic.accentGreen,
                                maxLines = 1,
                            )
                        }
                    }
                }
                else -> null
            },
            onClick = if (isNfcSupported && !isNfcEnabled) onOpenNfcSettings else null,
            showTopSeparator = true,
        )
    }
}

// ============================================================================
// 3. NHẬN TỆP & BẢO MẬT SUB-PAGE
// ============================================================================

@Composable
private fun TransferSecuritySettingsPage(
    confirmIncomingConnection: Boolean,
    backgroundConnectionDialog: Boolean,
    autoSaveToDownloads: Boolean,
    soundVibrationEnabled: Boolean,
    onToggleConfirmIncomingConnection: (Boolean) -> Unit,
    onToggleBackgroundConnectionDialog: (Boolean) -> Unit,
    onToggleAutoSaveToDownloads: (Boolean) -> Unit,
    onToggleSoundVibration: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val appearance = GlasmioTheme.appearance

    GlasmioPinnedListPage(
        title = stringResource(R.string.settings_category_transfer_security_title),
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_page_transfer_security"),
        onNavigateBack = onBack,
        navigateBackContentDescription = stringResource(R.string.back),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        backgroundColor = colors.groupedBackground.copy(
            alpha = if (appearance.reduceTransparency) 1f else 0.78f,
        ),
    ) {
        item(key = "transfer_security_card") {
            CenteredSettingWidth {
                TransferSecurityCardContent(
                    confirmIncomingConnection = confirmIncomingConnection,
                    backgroundConnectionDialog = backgroundConnectionDialog,
                    autoSaveToDownloads = autoSaveToDownloads,
                    soundVibrationEnabled = soundVibrationEnabled,
                    onToggleConfirmIncomingConnection = onToggleConfirmIncomingConnection,
                    onToggleBackgroundConnectionDialog = onToggleBackgroundConnectionDialog,
                    onToggleAutoSaveToDownloads = onToggleAutoSaveToDownloads,
                    onToggleSoundVibration = onToggleSoundVibration,
                )
            }
        }
    }
}

@Composable
private fun TransferSecurityCardContent(
    confirmIncomingConnection: Boolean,
    backgroundConnectionDialog: Boolean,
    autoSaveToDownloads: Boolean,
    soundVibrationEnabled: Boolean,
    onToggleConfirmIncomingConnection: (Boolean) -> Unit,
    onToggleBackgroundConnectionDialog: (Boolean) -> Unit,
    onToggleAutoSaveToDownloads: (Boolean) -> Unit,
    onToggleSoundVibration: (Boolean) -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic

    GlasmioGroupedList {
        GlasmioSwitchPreference(
            title = { GlasmioText(stringResource(R.string.settings_confirm_incoming)) },
            description = stringResource(R.string.settings_confirm_incoming_desc),
            icon = {
                ThazhSquircleIconBox(
                    symbol = ThazhSymbols.ShieldCheck,
                    accentColor = semantic.accentGreen,
                )
            },
            checked = confirmIncomingConnection,
            onCheckedChange = onToggleConfirmIncomingConnection,
            showTopSeparator = false,
            modifier = Modifier.testTag("settings_confirm_incoming_toggle"),
        )

        GlasmioSwitchPreference(
            title = { GlasmioText(stringResource(R.string.settings_background_prompt)) },
            description = stringResource(R.string.settings_background_prompt_desc),
            icon = {
                ThazhSquircleIconBox(
                    symbol = ThazhSymbols.OverlayPrompt,
                    accentColor = if (confirmIncomingConnection) semantic.accentBlue else colors.secondaryContent,
                )
            },
            checked = backgroundConnectionDialog && confirmIncomingConnection,
            onCheckedChange = onToggleBackgroundConnectionDialog,
            isEnabled = confirmIncomingConnection,
            showTopSeparator = true,
            modifier = Modifier.testTag("settings_background_prompt_toggle"),
        )

        GlasmioSwitchPreference(
            title = { GlasmioText(stringResource(R.string.settings_auto_save_downloads)) },
            description = stringResource(R.string.settings_auto_save_downloads_desc),
            icon = {
                ThazhSquircleIconBox(
                    symbol = ThazhSymbols.SaveAlt,
                    accentColor = semantic.accentCyan,
                )
            },
            checked = autoSaveToDownloads,
            onCheckedChange = onToggleAutoSaveToDownloads,
            showTopSeparator = true,
            modifier = Modifier.testTag("settings_auto_save_toggle"),
        )

        GlasmioSwitchPreference(
            title = { GlasmioText(stringResource(R.string.settings_sound_vibration)) },
            description = stringResource(R.string.settings_sound_vibration_desc),
            icon = {
                ThazhSquircleIconBox(
                    symbol = ThazhSymbols.NotificationsActive,
                    accentColor = semantic.warningOrange,
                )
            },
            checked = soundVibrationEnabled,
            onCheckedChange = onToggleSoundVibration,
            showTopSeparator = true,
            modifier = Modifier.testTag("settings_sound_vibration_toggle"),
        )
    }
}

// ============================================================================
// 4. GIAO DIỆN SUB-PAGE
// ============================================================================

@Composable
private fun AppearanceSettingsPage(
    themeMode: AppThemeMode,
    reduceTransparency: Boolean,
    liquidGlassEnabled: Boolean,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleReduceTransparency: (Boolean) -> Unit,
    onToggleLiquidGlass: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val appearance = GlasmioTheme.appearance

    GlasmioPinnedListPage(
        title = stringResource(R.string.settings_category_appearance_title),
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_page_appearance"),
        onNavigateBack = onBack,
        navigateBackContentDescription = stringResource(R.string.back),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        backgroundColor = colors.groupedBackground.copy(
            alpha = if (appearance.reduceTransparency) 1f else 0.78f,
        ),
    ) {
        item(key = "appearance_card") {
            CenteredSettingWidth {
                AppearanceCardContent(
                    themeMode = themeMode,
                    reduceTransparency = reduceTransparency,
                    liquidGlassEnabled = liquidGlassEnabled,
                    onSelectThemeMode = onSelectThemeMode,
                    onToggleReduceTransparency = onToggleReduceTransparency,
                    onToggleLiquidGlass = onToggleLiquidGlass,
                )
            }
        }
    }
}

@Composable
private fun AppearanceCardContent(
    themeMode: AppThemeMode,
    reduceTransparency: Boolean,
    liquidGlassEnabled: Boolean,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleReduceTransparency: (Boolean) -> Unit,
    onToggleLiquidGlass: (Boolean) -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val systemLabel = stringResource(R.string.settings_theme_mode_system)
    val lightLabel = stringResource(R.string.settings_theme_mode_light)
    val darkLabel = stringResource(R.string.settings_theme_mode_dark)
    val themeItems = remember(systemLabel, lightLabel, darkLabel) {
        listOf(
            AppThemeMode.SYSTEM to systemLabel,
            AppThemeMode.LIGHT to lightLabel,
            AppThemeMode.DARK to darkLabel,
        )
    }

    GlasmioGroupedList {
        GlasmioPreferenceEntry(
            title = { GlasmioText(stringResource(R.string.settings_theme_mode)) },
            icon = {
                ThazhSquircleIconBox(
                    symbol = when (themeMode) {
                        AppThemeMode.SYSTEM -> ThazhSymbols.ThemeSystem
                        AppThemeMode.LIGHT -> ThazhSymbols.ThemeLight
                        AppThemeMode.DARK -> ThazhSymbols.ThemeDark
                    },
                    accentColor = when (themeMode) {
                        AppThemeMode.SYSTEM -> semantic.accentBlue
                        AppThemeMode.LIGHT -> semantic.warningOrange
                        AppThemeMode.DARK -> semantic.categoryDocument
                    },
                )
            },
            content = {
                Spacer(modifier = Modifier.height(8.dp))
                GlasmioSegmentedControl(
                    items = themeItems,
                    selected = themeMode,
                    onSelected = onSelectThemeMode,
                    height = 38.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_mode_segmented_control"),
                ) { value, isSelected ->
                    val symbol = when (value) {
                        AppThemeMode.SYSTEM -> ThazhSymbols.ThemeSystem
                        AppThemeMode.LIGHT -> ThazhSymbols.ThemeLight
                        AppThemeMode.DARK -> ThazhSymbols.ThemeDark
                    }
                    val label = when (value) {
                        AppThemeMode.SYSTEM -> systemLabel
                        AppThemeMode.LIGHT -> lightLabel
                        AppThemeMode.DARK -> darkLabel
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        SfIcon(
                            systemName = symbol,
                            contentDescription = null,
                            tint = if (isSelected) colors.content else colors.secondaryContent,
                            size = 14.dp,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        GlasmioText(
                            text = label,
                            style = GlasmioTheme.textStyles.caption1.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                            color = if (isSelected) colors.content else colors.secondaryContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            showTopSeparator = false,
            modifier = Modifier.testTag("theme_mode_preference"),
        )

        GlasmioSwitchPreference(
            title = { GlasmioText(stringResource(R.string.settings_liquid_glass)) },
            description = stringResource(R.string.settings_liquid_glass_desc),
            icon = {
                ThazhSquircleIconBox(
                    symbol = ThazhSymbols.LiquidGlass,
                    accentColor = if (!reduceTransparency) semantic.accentCyan else colors.secondaryContent,
                )
            },
            checked = liquidGlassEnabled && !reduceTransparency,
            onCheckedChange = onToggleLiquidGlass,
            isEnabled = !reduceTransparency,
            showTopSeparator = true,
            modifier = Modifier.testTag("liquid_glass_toggle"),
        )

        GlasmioSwitchPreference(
            title = { GlasmioText(stringResource(R.string.settings_reduce_transparency)) },
            description = stringResource(R.string.settings_reduce_transparency_desc),
            icon = {
                ThazhSquircleIconBox(
                    symbol = ThazhSymbols.ReduceTransparency,
                    accentColor = semantic.accentBlue,
                )
            },
            checked = reduceTransparency,
            onCheckedChange = onToggleReduceTransparency,
            showTopSeparator = true,
            modifier = Modifier.testTag("reduce_transparency_toggle"),
        )
    }
}

// ============================================================================
// 5. DỮ LIỆU SUB-PAGE
// ============================================================================

@Composable
private fun DataSettingsPage(
    receivedFilesCount: Int,
    receivedFilesTotalBytes: Long,
    savedDevicesCount: Int,
    manufacturer: String,
    onOpenReceivedFiles: () -> Unit,
    onClearTempStagingCache: () -> Unit,
    onRequestClearSavedDevices: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val appearance = GlasmioTheme.appearance

    GlasmioPinnedListPage(
        title = stringResource(R.string.settings_category_data_title),
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_page_data"),
        onNavigateBack = onBack,
        navigateBackContentDescription = stringResource(R.string.back),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        backgroundColor = colors.groupedBackground.copy(
            alpha = if (appearance.reduceTransparency) 1f else 0.78f,
        ),
    ) {
        item(key = "data_card") {
            CenteredSettingWidth {
                DataSettingsCardContent(
                    receivedFilesCount = receivedFilesCount,
                    receivedFilesTotalBytes = receivedFilesTotalBytes,
                    savedDevicesCount = savedDevicesCount,
                    manufacturer = manufacturer,
                    onOpenReceivedFiles = onOpenReceivedFiles,
                    onClearTempStagingCache = onClearTempStagingCache,
                    onRequestClearSavedDevices = onRequestClearSavedDevices,
                )
            }
        }
    }
}

@Composable
private fun DataSettingsCardContent(
    receivedFilesCount: Int,
    receivedFilesTotalBytes: Long,
    savedDevicesCount: Int,
    manufacturer: String,
    onOpenReceivedFiles: () -> Unit,
    onClearTempStagingCache: () -> Unit,
    onRequestClearSavedDevices: () -> Unit,
) {
    val context = LocalContext.current
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic

    GlasmioGroupedList {
        GlasmioPreferenceEntry(
            title = { GlasmioText(stringResource(R.string.settings_received_storage_title)) },
            description = stringResource(
                R.string.settings_received_storage_summary,
                receivedFilesCount,
                FormatUtils.formatFileSize(receivedFilesTotalBytes),
            ),
            icon = {
                ThazhSquircleIconBox(
                    symbol = ThazhSymbols.Storage,
                    accentColor = semantic.accentBlue,
                )
            },
            trailingContent = {
                SfIcon(
                    systemName = ThazhSymbols.ChevronRight,
                    contentDescription = null,
                    tint = colors.tertiaryContent,
                    size = 18.dp,
                )
            },
            onClick = onOpenReceivedFiles,
            showTopSeparator = false,
            modifier = Modifier.testTag("settings_received_storage_entry"),
        )

        GlasmioPreferenceEntry(
            title = { GlasmioText(stringResource(R.string.settings_clear_temp_cache)) },
            description = stringResource(R.string.settings_clear_temp_cache_desc),
            icon = {
                ThazhSquircleIconBox(
                    symbol = ThazhSymbols.Cleaning,
                    accentColor = semantic.accentCyan,
                )
            },
            onClick = onClearTempStagingCache,
            showTopSeparator = true,
            modifier = Modifier.testTag("settings_clear_temp_cache_entry"),
        )

        if (savedDevicesCount > 0) {
            GlasmioPreferenceEntry(
                title = {
                    GlasmioText(
                        text = stringResource(R.string.settings_clear_saved_devices),
                        color = colors.destructive,
                    )
                },
                description = stringResource(R.string.settings_clear_saved_devices_desc, savedDevicesCount),
                icon = {
                    ThazhSquircleIconBox(
                        symbol = ThazhSymbols.DeleteSweep,
                        accentColor = colors.destructive,
                    )
                },
                onClick = onRequestClearSavedDevices,
                showTopSeparator = true,
                modifier = Modifier.testTag("settings_clear_saved_devices_entry"),
            )
        }

        GlasmioPreferenceEntry(
            title = { GlasmioText(stringResource(R.string.settings_background_optimization)) },
            description = stringResource(R.string.settings_background_optimization_desc, manufacturer),
            icon = {
                ThazhSquircleIconBox(
                    symbol = ThazhSymbols.BatterySaver,
                    accentColor = semantic.accentGreen,
                )
            },
            trailingContent = {
                SfIcon(
                    systemName = ThazhSymbols.ChevronRight,
                    contentDescription = null,
                    tint = colors.tertiaryContent,
                    size = 18.dp,
                )
            },
            onClick = { BackgroundOptimizationHelper.openAutostartSettings(context) },
            showTopSeparator = true,
            modifier = Modifier.testTag("open_background_optimization_button"),
        )
    }
}

@Composable
private fun visibilityModeLabel(mode: DeviceVisibilityMode): String = when (mode) {
    DeviceVisibilityMode.HIDDEN -> stringResource(R.string.visibility_mode_hidden_long)
    DeviceVisibilityMode.TEMPORARY_10_MIN -> stringResource(R.string.visibility_mode_10min_long)
    DeviceVisibilityMode.ALWAYS_VISIBLE -> stringResource(R.string.visibility_mode_always_long)
}

@Composable
private fun CenteredSettingWidth(content: @Composable () -> Unit) {
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
