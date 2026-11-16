package com.thazh.share.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.thazh.share.R
import com.thazh.share.ui.cupertino.CupertinoIcon
import com.thazh.share.ui.cupertino.CupertinoText
import com.thazh.share.ui.cupertino.CupertinoTextField
import com.thazh.share.ui.cupertino.CupertinoTopBar
import com.thazh.share.ui.cupertino.LiquidButton
import com.thazh.share.ui.cupertino.LiquidCard
import com.thazh.share.ui.cupertino.LiquidToggle
import com.thazh.share.ui.theme.CupertinoTheme
import com.thazh.share.ui.theme.LocalAppBackdrop
import com.thazh.share.util.BackgroundOptimizationHelper

@Composable
fun SettingsScreen(
    currentDisplayName: String,
    bleFastDiscoveryEnabled: Boolean,
    isBleSupported: Boolean,
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    onUpdateDisplayName: (String) -> Unit,
    onToggleBleFastDiscovery: (Boolean) -> Unit,
    onOpenNfcSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CupertinoTheme.colors
    var nameInput by remember(currentDisplayName) { mutableStateOf(currentDisplayName) }

    LaunchedEffect(currentDisplayName) {
        if (nameInput.isBlank() && currentDisplayName.isNotBlank()) {
            nameInput = currentDisplayName
        }
    }

    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    val topPadding = if (topBarHeightPx > 0) {
        with(density) { topBarHeightPx.toDp() } + 16.dp
    } else {
        112.dp
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
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "display_name_card") {
                    LiquidCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CupertinoIcon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = colors.accentBlue
                            )
                            CupertinoText(
                                text = stringResource(R.string.settings_display_name),
                                style = CupertinoTheme.typography.headline
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        CupertinoText(
                            text = stringResource(R.string.settings_display_name_desc),
                            style = CupertinoTheme.typography.footnote,
                            color = colors.secondaryLabel
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CupertinoTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            placeholder = stringResource(R.string.settings_display_name_hint),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                val trimmed = nameInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    onUpdateDisplayName(trimmed)
                                    Toast.makeText(context, context.getString(R.string.save), Toast.LENGTH_SHORT).show()
                                }
                            }),
                            modifier = Modifier.testTag("display_name_input")
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        LiquidButton(
                            onClick = {
                                val trimmed = nameInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    onUpdateDisplayName(trimmed)
                                    Toast.makeText(context, context.getString(R.string.save), Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = nameInput.trim().isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_display_name_button")
                        ) {
                            CupertinoText(
                                text = stringResource(R.string.settings_save),
                                style = CupertinoTheme.typography.headline,
                                color = Color.White
                            )
                        }
                    }
                }

                item(key = "ble_card") {
                    LiquidCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CupertinoIcon(
                                        imageVector = Icons.Rounded.BluetoothSearching,
                                        contentDescription = null,
                                        tint = colors.accentBlue
                                    )
                                    CupertinoText(
                                        text = stringResource(R.string.settings_ble_fast_discovery),
                                        style = CupertinoTheme.typography.headline
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                CupertinoText(
                                    text = if (isBleSupported) {
                                        stringResource(R.string.settings_ble_fast_discovery_desc)
                                    } else {
                                        stringResource(R.string.settings_ble_not_supported)
                                    },
                                    style = CupertinoTheme.typography.footnote,
                                    color = colors.secondaryLabel
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            LiquidToggle(
                                checked = bleFastDiscoveryEnabled && isBleSupported,
                                onCheckedChange = onToggleBleFastDiscovery,
                                enabled = isBleSupported,
                                modifier = Modifier.testTag("ble_fast_discovery_toggle")
                            )
                        }
                    }
                }

                item(key = "nfc_card") {
                    LiquidCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CupertinoIcon(
                                imageVector = Icons.Rounded.Nfc,
                                contentDescription = null,
                                tint = when {
                                    !isNfcSupported -> colors.tertiaryLabel
                                    isNfcEnabled -> colors.accentGreen
                                    else -> colors.warningOrange
                                }
                            )
                            CupertinoText(
                                text = stringResource(R.string.nfc_settings_title),
                                style = CupertinoTheme.typography.headline
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        CupertinoText(
                            text = when {
                                !isNfcSupported -> stringResource(R.string.nfc_settings_unsupported_desc)
                                isNfcEnabled -> stringResource(R.string.nfc_settings_ready_desc)
                                else -> stringResource(R.string.nfc_settings_disabled_desc)
                            },
                            style = CupertinoTheme.typography.footnote,
                            color = colors.secondaryLabel
                        )
                        if (isNfcSupported && !isNfcEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            LiquidButton(
                                onClick = onOpenNfcSettings,
                                tint = colors.warningOrange,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_open_nfc_button")
                            ) {
                                CupertinoText(
                                    text = stringResource(R.string.nfc_open_settings),
                                    style = CupertinoTheme.typography.headline,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                item(key = "background_opt_card") {
                    val manufacturer = remember {
                        Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
                    }
                    LiquidCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CupertinoIcon(
                                imageVector = Icons.Rounded.BatterySaver,
                                contentDescription = null,
                                tint = colors.accentGreen
                            )
                            CupertinoText(
                                text = stringResource(R.string.settings_background_optimization),
                                style = CupertinoTheme.typography.headline
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        CupertinoText(
                            text = stringResource(R.string.settings_background_optimization_desc, manufacturer),
                            style = CupertinoTheme.typography.footnote,
                            color = colors.secondaryLabel
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        LiquidButton(
                            onClick = { BackgroundOptimizationHelper.openAutostartSettings(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_background_optimization_button"),
                            tint = Color.Unspecified,
                            surfaceColor = colors.fillTertiary,
                            contentColor = colors.accentBlue
                        ) {
                            CupertinoText(
                                text = stringResource(R.string.settings_background_optimization_button),
                                style = CupertinoTheme.typography.headline.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.accentBlue
                            )
                        }
                    }
                }
            }
        }

        CompositionLocalProvider(LocalAppBackdrop provides barBackdrop) {
            CupertinoTopBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
                backTestTag = "settings_back_button",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged { topBarHeightPx = it.height }
            )
        }
    }
}
