package com.thazh.share.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.thazh.share.R
import com.thazh.share.data.AppThemeMode
import com.thazh.share.data.DeviceVisibilityMode
import com.thazh.share.service.WifiDirectService
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import com.thazh.share.util.BackgroundOptimizationHelper
import com.thazh.share.util.FormatUtils
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlassEmphasis
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
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioGroupedTableFooter
import io.github.neoruaa.glasmio.ui.GlasmioSegmentedControl

@Composable
fun SettingsScreen(
    currentDisplayName: String,
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = GlasmioTheme.colors
    val appearance = GlasmioTheme.appearance
    val semantic = ThazhTheme.semantic
    val manufacturer = remember {
        Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    }
    var showClearSavedDevicesDialog by remember { mutableStateOf(false) }

    val appVersion = remember(context) {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)?.versionName
        }.getOrNull() ?: "1.1.0"
    }

    GlasmioPinnedListPage(
        title = stringResource(R.string.settings_title),
        modifier = modifier.fillMaxSize(),
        onNavigateBack = onBack,
        navigateBackContentDescription = stringResource(R.string.back),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        backgroundColor = colors.groupedBackground.copy(
            alpha = if (appearance.reduceTransparency) 1f else 0.78f,
        ),
    ) {
        item(key = "visibility_title") {
            CenteredSettingWidth {
                GlasmioPreferenceGroupTitle(title = stringResource(R.string.settings_section_visibility))
            }
        }

        item(key = "visibility_group") {
            CenteredSettingWidth {
                Column {
                    GlasmioGroupedList {
                        GlasmioListPreference(
                            title = { Text(stringResource(R.string.settings_current_visibility)) },
                            description = when (visibilityMode) {
                                DeviceVisibilityMode.HIDDEN -> stringResource(R.string.visibility_mode_hidden_long)
                                DeviceVisibilityMode.TEMPORARY_10_MIN -> stringResource(
                                    R.string.visibility_desc_10min,
                                    WifiDirectService.formatCountdown(visibilityRemainingSeconds ?: 600),
                                )
                                DeviceVisibilityMode.ALWAYS_VISIBLE -> stringResource(R.string.visibility_mode_always_long)
                            },
                            icon = {
                                SfIcon(
                                    systemName = when (visibilityMode) {
                                        DeviceVisibilityMode.HIDDEN -> ThazhSymbols.VisibilityOff
                                        DeviceVisibilityMode.TEMPORARY_10_MIN -> ThazhSymbols.Timer
                                        DeviceVisibilityMode.ALWAYS_VISIBLE -> ThazhSymbols.AllInclusive
                                    },
                                    contentDescription = null,
                                    tint = when (visibilityMode) {
                                        DeviceVisibilityMode.HIDDEN -> colors.secondaryContent
                                        DeviceVisibilityMode.TEMPORARY_10_MIN -> semantic.accentBlue
                                        DeviceVisibilityMode.ALWAYS_VISIBLE -> semantic.accentGreen
                                    },
                                    size = 22.dp,
                                )
                            },
                            selectedValue = visibilityMode,
                            values = DeviceVisibilityMode.entries,
                            valueText = { visibilityModeLabel(it) },
                            onValueSelected = onSelectVisibilityMode,
                            showTopSeparator = false,
                            modifier = Modifier.testTag("settings_current_visibility_pref"),
                        )

                        GlasmioListPreference(
                            title = { Text(stringResource(R.string.settings_default_visibility)) },
                            description = visibilityModeLabel(defaultVisibilityMode),
                            icon = {
                                SfIcon(
                                    systemName = ThazhSymbols.VisibilityOn,
                                    contentDescription = null,
                                    tint = semantic.accentCyan,
                                    size = 22.dp,
                                )
                            },
                            selectedValue = defaultVisibilityMode,
                            values = DeviceVisibilityMode.entries,
                            valueText = { visibilityModeLabel(it) },
                            onValueSelected = onSelectDefaultVisibilityMode,
                            showTopSeparator = true,
                            modifier = Modifier.testTag("settings_default_visibility_pref"),
                        )
                    }
                    GlasmioGroupedTableFooter(
                        text = stringResource(R.string.settings_visibility_footer),
                    )
                }
            }
        }

        item(key = "transfer_security_title") {
            CenteredSettingWidth {
                GlasmioPreferenceGroupTitle(title = stringResource(R.string.settings_section_transfer_security))
            }
        }

        item(key = "transfer_security_group") {
            CenteredSettingWidth {
                GlasmioGroupedList {
                    GlasmioSwitchPreference(
                        title = { Text(stringResource(R.string.settings_confirm_incoming)) },
                        description = stringResource(R.string.settings_confirm_incoming_desc),
                        icon = {
                            SfIcon(
                                systemName = ThazhSymbols.ShieldCheck,
                                contentDescription = null,
                                tint = semantic.accentGreen,
                                size = 22.dp,
                            )
                        },
                        checked = confirmIncomingConnection,
                        onCheckedChange = onToggleConfirmIncomingConnection,
                        showTopSeparator = false,
                        modifier = Modifier.testTag("settings_confirm_incoming_toggle"),
                    )

                    GlasmioSwitchPreference(
                        title = { Text(stringResource(R.string.settings_background_prompt)) },
                        description = stringResource(R.string.settings_background_prompt_desc),
                        icon = {
                            SfIcon(
                                systemName = ThazhSymbols.NotificationsActive,
                                contentDescription = null,
                                tint = if (confirmIncomingConnection) semantic.accentBlue else colors.tertiaryContent,
                                size = 22.dp,
                            )
                        },
                        checked = backgroundConnectionDialog && confirmIncomingConnection,
                        onCheckedChange = onToggleBackgroundConnectionDialog,
                        isEnabled = confirmIncomingConnection,
                        showTopSeparator = true,
                        modifier = Modifier.testTag("settings_background_prompt_toggle"),
                    )

                    GlasmioSwitchPreference(
                        title = { Text(stringResource(R.string.settings_auto_save_downloads)) },
                        description = stringResource(R.string.settings_auto_save_downloads_desc),
                        icon = {
                            SfIcon(
                                systemName = ThazhSymbols.SaveAlt,
                                contentDescription = null,
                                tint = semantic.accentCyan,
                                size = 22.dp,
                            )
                        },
                        checked = autoSaveToDownloads,
                        onCheckedChange = onToggleAutoSaveToDownloads,
                        showTopSeparator = true,
                        modifier = Modifier.testTag("settings_auto_save_toggle"),
                    )

                    GlasmioSwitchPreference(
                        title = { Text(stringResource(R.string.settings_sound_vibration)) },
                        description = stringResource(R.string.settings_sound_vibration_desc),
                        icon = {
                            SfIcon(
                                systemName = ThazhSymbols.NotificationsActive,
                                contentDescription = null,
                                tint = semantic.warningOrange,
                                size = 22.dp,
                            )
                        },
                        checked = soundVibrationEnabled,
                        onCheckedChange = onToggleSoundVibration,
                        showTopSeparator = true,
                        modifier = Modifier.testTag("settings_sound_vibration_toggle"),
                    )
                }
            }
        }

        item(key = "appearance_title") {
            CenteredSettingWidth {
                GlasmioPreferenceGroupTitle(title = stringResource(R.string.settings_section_appearance))
            }
        }

        item(key = "appearance_group") {
            CenteredSettingWidth {
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

                Column {
                    GlasmioGroupedList {
                        GlasmioPreferenceEntry(
                            title = { Text(stringResource(R.string.settings_theme_mode)) },
                            description = when (themeMode) {
                                AppThemeMode.SYSTEM -> stringResource(R.string.settings_theme_mode_desc_system)
                                AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_mode_desc_light)
                                AppThemeMode.DARK -> stringResource(R.string.settings_theme_mode_desc_dark)
                            },
                            icon = {
                                SfIcon(
                                    systemName = when (themeMode) {
                                        AppThemeMode.SYSTEM -> ThazhSymbols.ThemeSystem
                                        AppThemeMode.LIGHT -> ThazhSymbols.ThemeLight
                                        AppThemeMode.DARK -> ThazhSymbols.ThemeDark
                                    },
                                    contentDescription = null,
                                    tint = when (themeMode) {
                                        AppThemeMode.SYSTEM -> semantic.accentBlue
                                        AppThemeMode.LIGHT -> semantic.warningOrange
                                        AppThemeMode.DARK -> semantic.categoryDocument
                                    },
                                    size = 22.dp,
                                )
                            },
                            content = {
                                Spacer(modifier = Modifier.height(10.dp))
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
                                        Text(
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
                            title = { Text(stringResource(R.string.settings_reduce_transparency)) },
                            description = stringResource(R.string.settings_reduce_transparency_desc),
                            icon = {
                                SfIcon(
                                    systemName = ThazhSymbols.ReduceTransparency,
                                    contentDescription = null,
                                    tint = semantic.accentBlue,
                                    size = 22.dp,
                                )
                            },
                            checked = reduceTransparency,
                            onCheckedChange = onToggleReduceTransparency,
                            showTopSeparator = true,
                            modifier = Modifier.testTag("reduce_transparency_toggle"),
                        )

                        GlasmioSwitchPreference(
                            title = { Text(stringResource(R.string.settings_liquid_glass)) },
                            description = stringResource(R.string.settings_liquid_glass_desc),
                            icon = {
                                SfIcon(
                                    systemName = ThazhSymbols.LiquidGlass,
                                    contentDescription = null,
                                    tint = if (!reduceTransparency) semantic.accentCyan else colors.tertiaryContent,
                                    size = 22.dp,
                                )
                            },
                            checked = liquidGlassEnabled && !reduceTransparency,
                            onCheckedChange = onToggleLiquidGlass,
                            isEnabled = !reduceTransparency,
                            showTopSeparator = true,
                            modifier = Modifier.testTag("liquid_glass_toggle"),
                        )
                    }
                    GlasmioGroupedTableFooter(
                        text = if (reduceTransparency) {
                            stringResource(R.string.settings_reduce_transparency_desc)
                        } else {
                            stringResource(R.string.settings_liquid_glass_desc)
                        },
                    )
                }
            }
        }

        item(key = "device_title") {
            CenteredSettingWidth {
                GlasmioPreferenceGroupTitle(title = stringResource(R.string.settings_section_device))
            }
        }

        item(key = "device_group") {
            CenteredSettingWidth {
                Column {
                    GlasmioGroupedList {
                        GlasmioEditTextPreference(
                            title = { Text(stringResource(R.string.settings_display_name)) },
                            icon = {
                                SfIcon(
                                    systemName = ThazhSymbols.Person,
                                    contentDescription = null,
                                    tint = semantic.accentBlue,
                                    size = 22.dp,
                                )
                            },
                            value = currentDisplayName.ifBlank { stringResource(R.string.settings_display_name_hint) },
                            onValueChange = { next ->
                                val trimmed = next.trim()
                                if (trimmed.isNotEmpty()) {
                                    onUpdateDisplayName(trimmed)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.save),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                            isInputValid = { it.trim().isNotEmpty() },
                            showTopSeparator = false,
                            modifier = Modifier.testTag("display_name_input"),
                        )
                    }
                    GlasmioGroupedTableFooter(
                        text = stringResource(R.string.settings_display_name_desc),
                    )
                }
            }
        }

        item(key = "connectivity_title") {
            CenteredSettingWidth {
                GlasmioPreferenceGroupTitle(title = stringResource(R.string.settings_section_connectivity))
            }
        }

        item(key = "connectivity_group") {
            CenteredSettingWidth {
                GlasmioGroupedList {
                    GlasmioSwitchPreference(
                        title = { Text(stringResource(R.string.settings_ble_fast_discovery)) },
                        description = if (isBleSupported) {
                            stringResource(R.string.settings_ble_fast_discovery_desc)
                        } else {
                            stringResource(R.string.settings_ble_not_supported)
                        },
                        icon = {
                            SfIcon(
                                systemName = ThazhSymbols.Bluetooth,
                                contentDescription = null,
                                tint = if (isBleSupported) semantic.accentBlue else colors.tertiaryContent,
                                size = 22.dp,
                            )
                        },
                        checked = bleFastDiscoveryEnabled && isBleSupported,
                        onCheckedChange = onToggleBleFastDiscovery,
                        isEnabled = isBleSupported,
                        showTopSeparator = false,
                        modifier = Modifier.testTag("ble_fast_discovery_toggle"),
                    )

                    GlasmioPreferenceEntry(
                        title = { Text(stringResource(R.string.nfc_settings_title)) },
                        description = when {
                            !isNfcSupported -> stringResource(R.string.nfc_settings_unsupported_desc)
                            isNfcEnabled -> stringResource(R.string.nfc_settings_ready_desc)
                            else -> stringResource(R.string.nfc_settings_disabled_desc)
                        },
                        icon = {
                            SfIcon(
                                systemName = ThazhSymbols.Nfc,
                                contentDescription = null,
                                tint = when {
                                    !isNfcSupported -> colors.tertiaryContent
                                    isNfcEnabled -> semantic.accentGreen
                                    else -> semantic.warningOrange
                                },
                                size = 22.dp,
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
                                        Text(
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
                                        Text(
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
        }

        item(key = "storage_about_title") {
            CenteredSettingWidth {
                GlasmioPreferenceGroupTitle(title = stringResource(R.string.settings_section_storage_about))
            }
        }

        item(key = "storage_about_group") {
            CenteredSettingWidth {
                GlasmioGroupedList {
                    GlasmioPreferenceEntry(
                        title = { Text(stringResource(R.string.settings_received_storage_title)) },
                        description = stringResource(
                            R.string.settings_received_storage_summary,
                            receivedFilesCount,
                            FormatUtils.formatFileSize(receivedFilesTotalBytes),
                        ),
                        icon = {
                            SfIcon(
                                systemName = ThazhSymbols.Storage,
                                contentDescription = null,
                                tint = semantic.accentBlue,
                                size = 22.dp,
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
                        title = { Text(stringResource(R.string.settings_clear_temp_cache)) },
                        description = stringResource(R.string.settings_clear_temp_cache_desc),
                        icon = {
                            SfIcon(
                                systemName = ThazhSymbols.Cleaning,
                                contentDescription = null,
                                tint = semantic.accentCyan,
                                size = 22.dp,
                            )
                        },
                        onClick = onClearTempStagingCache,
                        showTopSeparator = true,
                        modifier = Modifier.testTag("settings_clear_temp_cache_entry"),
                    )

                    if (savedDevicesCount > 0) {
                        GlasmioPreferenceEntry(
                            title = {
                                Text(
                                    text = stringResource(R.string.settings_clear_saved_devices),
                                    color = colors.destructive,
                                )
                            },
                            description = stringResource(R.string.settings_clear_saved_devices_desc, savedDevicesCount),
                            icon = {
                                SfIcon(
                                    systemName = ThazhSymbols.DeleteSweep,
                                    contentDescription = null,
                                    tint = colors.destructive,
                                    size = 22.dp,
                                )
                            },
                            onClick = { showClearSavedDevicesDialog = true },
                            showTopSeparator = true,
                            modifier = Modifier.testTag("settings_clear_saved_devices_entry"),
                        )
                    }

                    GlasmioPreferenceEntry(
                        title = { Text(stringResource(R.string.settings_about_title)) },
                        description = stringResource(R.string.settings_about_desc, appVersion),
                        icon = {
                            SfIcon(
                                systemName = ThazhSymbols.Info,
                                contentDescription = null,
                                tint = colors.secondaryContent,
                                size = 22.dp,
                            )
                        },
                        showTopSeparator = true,
                    )
                }
            }
        }

        item(key = "system_title") {
            CenteredSettingWidth {
                GlasmioPreferenceGroupTitle(title = stringResource(R.string.settings_section_system))
            }
        }

        item(key = "system_group") {
            CenteredSettingWidth {
                GlasmioGroupedList {
                    GlasmioPreferenceEntry(
                        title = { Text(stringResource(R.string.settings_background_optimization)) },
                        description = stringResource(R.string.settings_background_optimization_desc, manufacturer),
                        icon = {
                            SfIcon(
                                systemName = ThazhSymbols.BatterySaver,
                                contentDescription = null,
                                tint = semantic.accentGreen,
                                size = 22.dp,
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
                        showTopSeparator = false,
                        modifier = Modifier.testTag("open_background_optimization_button"),
                    )
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
