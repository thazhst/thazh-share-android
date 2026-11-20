package com.thazh.share.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.network.NativeCoreEngine
import com.thazh.share.ui.components.GlasmioText
import com.thazh.share.ui.components.ThazhAppIconBadge
import com.thazh.share.ui.components.ThazhSquircleIconBox
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceEntry
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceGroupTitle
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList

private const val GLASMIO_GITHUB_URL = "https://github.com/NEORUAA/Glasmio"
private const val GLASMIO_LICENSE_URL = "https://github.com/NEORUAA/Glasmio/blob/main/LICENSE"
private const val CATBOX_URL = "https://catbox.moe"
private const val BUNDLED_GLASMIO_LICENSE_ASSET = "licenses/GLASMIO_LICENSE.txt"

@Composable
fun AboutAppScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = GlasmioTheme.colors
    val appearance = GlasmioTheme.appearance
    val semantic = ThazhTheme.semantic

    val appName = stringResource(R.string.app_name)
    val packageName = remember(context) { context.packageName }
    val versionDisplay = remember(context) {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val vName = info.versionName ?: "1.2.3"
            val vCode = info.longVersionCode
            "$vName ($vCode)"
        }.getOrDefault("1.2.3 (4)")
    }

    val attachedApacheLicenseText = remember(context) {
        runCatching {
            context.assets.open(BUNDLED_GLASMIO_LICENSE_ASSET).bufferedReader(Charsets.UTF_8).use {
                it.readText().trim()
            }
        }.getOrElse { FALLBACK_APACHE_2_LICENSE }
    }

    var showApacheLicenseDialog by rememberSaveable { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "about_hero_pulse")
    val haloScaleState = pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "about_halo_scale",
    )

    GlasmioPinnedListPage(
        title = stringResource(R.string.about_app_title),
        modifier = modifier
            .fillMaxSize()
            .testTag("about_app_screen"),
        onNavigateBack = onBack,
        navigateBackContentDescription = stringResource(R.string.back),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        backgroundColor = colors.groupedBackground.copy(
            alpha = if (appearance.reduceTransparency) 1f else 0.78f,
        ),
    ) {
        // 1. Compact Hero Header Card showcasing Apple HIG App Icon
        item(key = "about_hero_card") {
            CenteredAboutWidth {
                GlasmioCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .graphicsLayer {
                                        val s = haloScaleState.value
                                        scaleX = s
                                        scaleY = s
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                semantic.accentBlue.copy(alpha = if (colors.isDark) 0.28f else 0.16f),
                                                Color.Transparent,
                                            ),
                                        ),
                                    ),
                            )
                            ThazhAppIconBadge(
                                size = 62.dp,
                                cornerRadius = 15.dp,
                                contentDescription = appName,
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            GlasmioText(
                                text = appName,
                                style = GlasmioTheme.textStyles.title3.copy(fontWeight = FontWeight.Bold),
                                color = colors.content,
                            )
                            GlasmioText(
                                text = stringResource(R.string.about_app_tagline),
                                style = GlasmioTheme.textStyles.footnote,
                                color = colors.secondaryContent,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(ContinuousCapsule)
                                        .background(semantic.accentBlue.copy(alpha = if (colors.isDark) 0.20f else 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    GlasmioText(
                                        text = packageName,
                                        style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.SemiBold),
                                        color = semantic.accentBlue,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(ContinuousCapsule)
                                        .background(semantic.accentCyan.copy(alpha = if (colors.isDark) 0.20f else 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    GlasmioText(
                                        text = "v$versionDisplay",
                                        style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.SemiBold),
                                        color = semantic.accentCyan,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Basic App Info (Tên ứng dụng, Package name, Phiên bản)
        item(key = "about_info_title") {
            CenteredAboutWidth {
                GlasmioPreferenceGroupTitle(title = stringResource(R.string.about_app_section_info))
            }
        }

        item(key = "about_info_group") {
            CenteredAboutWidth {
                GlasmioGroupedList {
                    GlasmioPreferenceEntry(
                        title = { GlasmioText(stringResource(R.string.about_app_field_name)) },
                        description = appName,
                        icon = {
                            ThazhSquircleIconBox(
                                symbol = ThazhSymbols.SendPaperPlane,
                                accentColor = semantic.accentBlue,
                            )
                        },
                        onClick = { copyTextToClipboard(context, appName, appName) },
                        showTopSeparator = false,
                        modifier = Modifier.testTag("about_app_name_row"),
                    )

                    GlasmioPreferenceEntry(
                        title = { GlasmioText(stringResource(R.string.about_app_field_package)) },
                        description = packageName,
                        icon = {
                            ThazhSquircleIconBox(
                                symbol = ThazhSymbols.FileApk,
                                accentColor = semantic.accentCyan,
                            )
                        },
                        trailingContent = {
                            SfIcon(
                                systemName = ThazhSymbols.Copy,
                                contentDescription = null,
                                tint = colors.tertiaryContent,
                                size = 16.dp,
                            )
                        },
                        onClick = { copyTextToClipboard(context, "Package name", packageName) },
                        showTopSeparator = true,
                        modifier = Modifier.testTag("about_app_package_row"),
                    )

                    GlasmioPreferenceEntry(
                        title = { GlasmioText(stringResource(R.string.about_app_field_version)) },
                        description = versionDisplay,
                        icon = {
                            ThazhSquircleIconBox(
                                symbol = ThazhSymbols.ShieldCheck,
                                accentColor = semantic.accentGreen,
                            )
                        },
                        trailingContent = {
                            SfIcon(
                                systemName = ThazhSymbols.Copy,
                                contentDescription = null,
                                tint = colors.tertiaryContent,
                                size = 16.dp,
                            )
                        },
                        onClick = { copyTextToClipboard(context, "Version", versionDisplay) },
                        showTopSeparator = true,
                        modifier = Modifier.testTag("about_app_version_row"),
                    )

                    val engineDesc = remember { NativeCoreEngine.engineDescriptor }
                    GlasmioPreferenceEntry(
                        title = { GlasmioText("Lõi truyền tải & Tín hiệu") },
                        description = engineDesc,
                        icon = {
                            ThazhSquircleIconBox(
                                symbol = ThazhSymbols.LiquidGlass,
                                accentColor = semantic.accentBlue,
                            )
                        },
                        trailingContent = {
                            SfIcon(
                                systemName = ThazhSymbols.Copy,
                                contentDescription = null,
                                tint = colors.tertiaryContent,
                                size = 16.dp,
                            )
                        },
                        onClick = { copyTextToClipboard(context, "Core Engine", engineDesc) },
                        showTopSeparator = true,
                        modifier = Modifier.testTag("about_app_engine_row"),
                    )
                }
            }
        }

        // 3. Concise Introduction
        item(key = "about_intro_title") {
            CenteredAboutWidth {
                GlasmioPreferenceGroupTitle(title = stringResource(R.string.about_app_section_intro))
            }
        }

        item(key = "about_intro_card") {
            CenteredAboutWidth {
                GlasmioCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_app_intro_card"),
                ) {
                    GlasmioText(
                        text = stringResource(R.string.about_app_intro_body),
                        style = GlasmioTheme.textStyles.subheadline.copy(lineHeight = 21.sp),
                        color = colors.content,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
            }
        }

        // 4. Credit & Open Source License Row (tapping opens Apache 2.0 Dialog)
        item(key = "about_credits_title") {
            CenteredAboutWidth {
                GlasmioPreferenceGroupTitle(title = stringResource(R.string.about_app_section_credits))
            }
        }

        item(key = "about_credits_group") {
            CenteredAboutWidth {
                GlasmioGroupedList {
                    GlasmioPreferenceEntry(
                        title = { GlasmioText(stringResource(R.string.about_app_credit_glasmio_title)) },
                        description = stringResource(R.string.about_app_credit_glasmio_desc),
                        icon = {
                            ThazhSquircleIconBox(
                                symbol = ThazhSymbols.LiquidGlass,
                                accentColor = semantic.accentCyan,
                            )
                        },
                        trailingContent = {
                            SfIcon(
                                systemName = ThazhSymbols.OpenInNew,
                                contentDescription = null,
                                tint = colors.tertiaryContent,
                                size = 18.dp,
                            )
                        },
                        onClick = { openExternalLink(context, GLASMIO_GITHUB_URL) },
                        showTopSeparator = false,
                        modifier = Modifier.testTag("about_credit_glasmio_row"),
                    )

                    GlasmioPreferenceEntry(
                        title = { GlasmioText(stringResource(R.string.about_app_credit_catbox_title)) },
                        description = stringResource(R.string.about_app_credit_catbox_desc),
                        icon = {
                            ThazhSquircleIconBox(
                                symbol = ThazhSymbols.CloudUpload,
                                accentColor = semantic.accentBlue,
                            )
                        },
                        trailingContent = {
                            SfIcon(
                                systemName = ThazhSymbols.OpenInNew,
                                contentDescription = null,
                                tint = colors.tertiaryContent,
                                size = 18.dp,
                            )
                        },
                        onClick = { openExternalLink(context, CATBOX_URL) },
                        showTopSeparator = true,
                        modifier = Modifier.testTag("about_credit_catbox_row"),
                    )

                    GlasmioPreferenceEntry(
                        title = { GlasmioText(stringResource(R.string.about_app_section_license)) },
                        description = stringResource(R.string.about_app_license_row_desc),
                        icon = {
                            ThazhSquircleIconBox(
                                symbol = ThazhSymbols.FileDocument,
                                accentColor = semantic.warningOrange,
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
                        onClick = { showApacheLicenseDialog = true },
                        showTopSeparator = true,
                        modifier = Modifier.testTag("about_open_source_license_row"),
                    )
                }
            }
        }
    }

    // Apache 2.0 License Dialog (only shown when tapping "Giấy phép nguồn mở")
    GlasmioAlertDialog(
        visible = showApacheLicenseDialog,
        onDismissRequest = { showApacheLicenseDialog = false },
        title = stringResource(R.string.about_app_license_glasmio_title),
        message = stringResource(R.string.about_app_license_glasmio_subtitle),
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.about_app_open_github_button),
                onClick = { openExternalLink(context, GLASMIO_GITHUB_URL) },
                role = GlasmioAlertButtonRole.Default,
            ),
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.about_app_license_copy_button),
                onClick = {
                    copyTextToClipboard(context, "Apache License 2.0", attachedApacheLicenseText)
                },
                role = GlasmioAlertButtonRole.Default,
            ),
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.close),
                onClick = { showApacheLicenseDialog = false },
                role = GlasmioAlertButtonRole.Cancel,
            ),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LicenseUrlPill(
                label = stringResource(R.string.about_app_license_github_label),
                url = GLASMIO_GITHUB_URL,
                accentColor = semantic.accentBlue,
                onClick = { openExternalLink(context, GLASMIO_GITHUB_URL) },
                onCopy = { copyTextToClipboard(context, "Glasmio GitHub", GLASMIO_GITHUB_URL) },
                testTag = "about_glasmio_github_link",
            )

            LicenseUrlPill(
                label = stringResource(R.string.about_app_license_file_label),
                url = GLASMIO_LICENSE_URL,
                accentColor = semantic.accentCyan,
                onClick = { openExternalLink(context, GLASMIO_LICENSE_URL) },
                onCopy = { copyTextToClipboard(context, "Glasmio LICENSE URL", GLASMIO_LICENSE_URL) },
                testTag = "about_glasmio_license_link",
            )

            val verticalScrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 240.dp)
                    .clip(ContinuousRoundedRectangle(12.dp))
                    .background(colors.secondaryContent.copy(alpha = if (colors.isDark) 0.16f else 0.08f))
                    .border(
                        width = 1.dp,
                        color = colors.secondaryContent.copy(alpha = 0.20f),
                        shape = ContinuousRoundedRectangle(12.dp),
                    )
                    .padding(10.dp)
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState)
                    .testTag("about_attached_apache_license_box"),
            ) {
            GlasmioText(
                text = attachedApacheLicenseText,
                style = GlasmioTheme.textStyles.caption2.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp,
                ),
                color = colors.secondaryContent,
            )
        }
        }
    }
}

@Composable
private fun LicenseUrlPill(
    label: String,
    url: String,
    accentColor: Color,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    testTag: String,
) {
    val colors = GlasmioTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(10.dp))
            .background(accentColor.copy(alpha = if (colors.isDark) 0.16f else 0.09f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SfIcon(
            systemName = ThazhSymbols.Link,
            contentDescription = null,
            tint = accentColor,
            size = 15.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            GlasmioText(
                text = label,
                style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.SemiBold),
                color = colors.secondaryContent,
            )
            GlasmioText(
                text = url,
                style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onCopy),
            contentAlignment = Alignment.Center,
        ) {
            SfIcon(
                systemName = ThazhSymbols.Copy,
                contentDescription = stringResource(R.string.link_share_copy_button),
                tint = accentColor,
                size = 14.dp,
            )
        }
        SfIcon(
            systemName = ThazhSymbols.OpenInNew,
            contentDescription = null,
            tint = accentColor,
            size = 15.dp,
        )
    }
}

@Composable
private fun CenteredAboutWidth(content: @Composable () -> Unit) {
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

private fun copyTextToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(
        context,
        context.getString(R.string.about_app_copied_toast, value.take(48)),
        Toast.LENGTH_SHORT,
    ).show()
}

private fun openExternalLink(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private const val FALLBACK_APACHE_2_LICENSE = """Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License."""
