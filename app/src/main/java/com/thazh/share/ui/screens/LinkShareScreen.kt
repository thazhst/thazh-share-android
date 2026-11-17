package com.thazh.share.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.data.FileCategory
import com.thazh.share.data.SharedLinkItem
import com.thazh.share.network.CatboxExpiry
import com.thazh.share.network.CatboxUploadProgress
import com.thazh.share.network.CatboxUploadResult
import com.thazh.share.ui.auth.GoogleAuthHelper
import com.thazh.share.ui.auth.findActivity
import com.thazh.share.ui.components.FileTypeBadge
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import com.thazh.share.util.FormatUtils
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioAlertFieldGroup
import io.github.neoruaa.glasmio.ui.GlasmioAlertTextField
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioSectionTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.security.MessageDigest

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LinkShareScreen(
    selectedExpiry: CatboxExpiry,
    isUploading: Boolean,
    uploadProgress: CatboxUploadProgress?,
    uploadError: String?,
    sessionUploadedLinks: List<CatboxUploadResult>,
    observeCloudLinks: (String) -> Flow<List<SharedLinkItem>>,
    bottomBarPadding: Dp = 96.dp,
    onSelectExpiry: (CatboxExpiry) -> Unit,
    onUploadUris: (List<Uri>, CatboxExpiry, String) -> Unit,
    onUpdateCloudLinkNote: (String, String, String) -> Unit,
    onDeleteCloudLink: (String, String) -> Unit,
    onClearSessionLinks: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = GlasmioTheme.colors
    val appearance = GlasmioTheme.appearance
    val semantic = ThazhTheme.semantic

    // Reactive Firebase Auth state listener
    var currentUser by remember { mutableStateOf<FirebaseUser?>(FirebaseAuth.getInstance().currentUser) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var showAuthHelpDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            runCatching {
                val activity = context.findActivity()
                GoogleAuthHelper.attemptAutoSignIn(activity)
            }
        }
    }

    var uploadNoteDraft by remember { mutableStateOf("") }
    var qrPreviewTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // (fileName, url)
    var lastQrTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    if (qrPreviewTarget != null) {
        lastQrTarget = qrPreviewTarget
    }

    var linkToEditNote by remember { mutableStateOf<SharedLinkItem?>(null) }
    var lastLinkToEditNote by remember { mutableStateOf<SharedLinkItem?>(null) }
    if (linkToEditNote != null) {
        lastLinkToEditNote = linkToEditNote
    }
    var editNoteValue by remember { mutableStateOf(TextFieldValue("")) }

    val filesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onUploadUris(uris, selectedExpiry, uploadNoteDraft)
            uploadNoteDraft = ""
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onUploadUris(uris, selectedExpiry, uploadNoteDraft)
            uploadNoteDraft = ""
        }
    }

    val cardShape = ContinuousRoundedRectangle(GlasmioTheme.dimensions.regularCornerRadius)

    GlasmioPinnedListPage(
        title = stringResource(R.string.link_share_title),
        subtitle = stringResource(R.string.link_share_subtitle),
        bottomPadding = bottomBarPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        backgroundColor = colors.groupedBackground.copy(
            alpha = if (appearance.reduceTransparency) 1f else 0.78f,
        ),
        modifier = modifier.fillMaxSize(),
        actions = {
            GlasmioIconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("link_share_open_settings_button"),
            ) {
                SfIcon(
                    systemName = ThazhSymbols.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                )
            }
        },
    ) {
        // 1. Hero Upload Card (Catbox.moe / Litterbox)
        item(key = "catbox_upload_card") {
            CenteredLinkContent {
                GlasmioCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(ContinuousRoundedRectangle(14.dp))
                                    .background(semantic.accentBlue.copy(alpha = if (colors.isDark) 0.24f else 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                SfIcon(
                                    systemName = ThazhSymbols.CloudUpload,
                                    contentDescription = null,
                                    tint = semantic.accentBlue,
                                    size = 24.dp,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.link_share_hero_title),
                                    style = GlasmioTheme.textStyles.headline.copy(fontWeight = FontWeight.Bold),
                                    color = colors.content,
                                )
                                Text(
                                    text = stringResource(R.string.link_share_hero_desc),
                                    style = GlasmioTheme.textStyles.footnote,
                                    color = colors.secondaryContent,
                                )
                            }
                        }

                        // Retention / Expiry selector chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.link_share_expiry_label),
                                style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.secondaryContent,
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CatboxExpiry.entries.forEach { expiry ->
                                    val selected = expiry == selectedExpiry
                                    val chipColor = if (selected) semantic.accentBlue else colors.secondaryContent
                                    Box(
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 36.dp)
                                            .clip(ContinuousCapsule)
                                            .background(
                                                chipColor.copy(
                                                    alpha = if (selected) (if (colors.isDark) 0.26f else 0.16f) else 0.08f
                                                )
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = chipColor.copy(alpha = if (selected) 0.55f else 0.16f),
                                                shape = ContinuousCapsule,
                                            )
                                            .clickable(
                                                enabled = !isUploading,
                                                role = Role.RadioButton,
                                                onClick = { onSelectExpiry(expiry) },
                                            )
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                            .testTag("expiry_chip_${expiry.code}"),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = expiryDisplayLabel(expiry),
                                            style = GlasmioTheme.textStyles.caption1.copy(
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (selected) semantic.accentBlue else colors.content,
                                        )
                                    }
                                }
                            }
                        }

                        // Upload progress or Pick buttons
                        if (isUploading && uploadProgress != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(ContinuousRoundedRectangle(14.dp))
                                    .background(semantic.accentBlue.copy(alpha = if (colors.isDark) 0.16f else 0.08f))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = stringResource(R.string.link_share_uploading_file, uploadProgress.fileName),
                                        style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.content,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${(uploadProgress.fraction * 100).toInt()}%",
                                        style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.Bold),
                                        color = semantic.accentBlue,
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { uploadProgress.fraction },
                                    color = semantic.accentBlue,
                                    trackColor = semantic.accentBlue.copy(alpha = 0.18f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(ContinuousCapsule),
                                )
                                val statsText = buildList {
                                    add("${FormatUtils.formatFileSize(uploadProgress.bytesUploaded)} / ${FormatUtils.formatFileSize(uploadProgress.totalBytes)}")
                                    if (uploadProgress.speedBytesPerSec > 0L) {
                                        add(FormatUtils.formatSpeed(uploadProgress.speedBytesPerSec))
                                    }
                                }.joinToString(" • ")
                                Text(
                                    text = statsText,
                                    style = GlasmioTheme.textStyles.caption2,
                                    color = colors.secondaryContent,
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                GlasmioButton(
                                    onClick = { filesPickerLauncher.launch("*/*") },
                                    emphasis = GlassEmphasis.Prominent,
                                    navigationSurfaceColor = semantic.accentBlue,
                                    size = GlasmioButtonSize.Medium,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("link_share_pick_files_button"),
                                ) {
                                    SfIcon(
                                        systemName = ThazhSymbols.AttachFile,
                                        contentDescription = null,
                                        tint = Color.White,
                                        size = 18.dp,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.transfer_pick_files),
                                        style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White,
                                    )
                                }

                                GlasmioButton(
                                    onClick = {
                                        mediaPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    emphasis = GlassEmphasis.Regular,
                                    size = GlasmioButtonSize.Medium,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("link_share_pick_media_button"),
                                ) {
                                    SfIcon(
                                        systemName = ThazhSymbols.PhotoLibrary,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        size = 18.dp,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.transfer_pick_media),
                                        style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.accent,
                                    )
                                }
                            }
                        }

                        if (!uploadError.isNullOrBlank()) {
                            Text(
                                text = uploadError,
                                style = GlasmioTheme.textStyles.footnote,
                                color = colors.destructive,
                            )
                        }
                    }
                }
            }
        }

        // 2. Instant Session Uploaded Links (Available without login!)
        if (sessionUploadedLinks.isNotEmpty() && currentUser == null) {
            item(key = "session_links_header") {
                CenteredLinkContent {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        GlasmioSectionTitle(
                            title = stringResource(R.string.link_share_session_section_title),
                            modifier = Modifier.weight(1f),
                        )
                        GlasmioButton(
                            onClick = onClearSessionLinks,
                            emphasis = GlassEmphasis.Regular,
                            size = GlasmioButtonSize.Small,
                        ) {
                            Text(
                                text = stringResource(R.string.transfer_clear_finished),
                                style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.accent,
                            )
                        }
                    }
                }
            }

            items(sessionUploadedLinks, key = { it.url + it.uploadedAtEpochMs }) { item ->
                CenteredLinkContent {
                    SharedLinkRowCard(
                        fileName = item.fileName,
                        fileSizeBytes = item.fileSizeBytes,
                        mimeType = item.mimeType,
                        url = item.url,
                        expiryCode = item.expiry.code,
                        note = item.note,
                        timestampEpochMs = item.uploadedAtEpochMs,
                        onCopy = { copyUrlToClipboard(context, item.url) },
                        onShare = { shareUrlIntent(context, item.fileName, item.url) },
                        onShowQr = { qrPreviewTarget = item.fileName to item.url },
                        onOpenBrowser = { openUrlInBrowser(context, item.url) },
                        onEditNote = null,
                        onDelete = null,
                    )
                }
            }
        }

        // 3. Optional Account & Cloud History Section
        item(key = "cloud_account_card") {
            CenteredLinkContent {
                val user = currentUser
                if (user == null) {
                    GlasmioCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(ContinuousRoundedRectangle(12.dp))
                                        .background(semantic.accentCyan.copy(alpha = if (colors.isDark) 0.22f else 0.14f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    SfIcon(
                                        systemName = ThazhSymbols.AccountCircle,
                                        contentDescription = null,
                                        tint = semantic.accentCyan,
                                        size = 24.dp,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.link_share_auth_title),
                                            style = GlasmioTheme.textStyles.headline.copy(fontWeight = FontWeight.Bold),
                                            color = colors.content,
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(ContinuousCapsule)
                                                .background(colors.secondaryContent.copy(alpha = 0.14f))
                                                .padding(horizontal = 7.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                text = stringResource(R.string.link_share_auth_optional_badge),
                                                style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.SemiBold),
                                                color = colors.secondaryContent,
                                            )
                                        }
                                    }
                                    Text(
                                        text = stringResource(R.string.link_share_auth_desc),
                                        style = GlasmioTheme.textStyles.footnote,
                                        color = colors.secondaryContent,
                                    )
                                }
                                GlasmioIconButton(
                                    onClick = { showAuthHelpDialog = true },
                                    size = 40.dp,
                                    modifier = Modifier.testTag("link_share_auth_info_button"),
                                ) {
                                    SfIcon(
                                        systemName = ThazhSymbols.Info,
                                        contentDescription = stringResource(R.string.link_share_auth_info_title),
                                        tint = colors.secondaryContent,
                                        size = 18.dp,
                                    )
                                }
                            }

                            if (!authErrorMessage.isNullOrBlank()) {
                                Text(
                                    text = authErrorMessage!!,
                                    style = GlasmioTheme.textStyles.footnote,
                                    color = colors.destructive,
                                )
                            }

                            GlasmioButton(
                                onClick = {
                                    if (isSigningIn) return@GlasmioButton
                                    authErrorMessage = null
                                    isSigningIn = true
                                    coroutineScope.launch {
                                        try {
                                            val activity = context.findActivity()
                                            GoogleAuthHelper.onGoogleSignInClicked(
                                                activityContext = activity,
                                                onError = { err -> authErrorMessage = err },
                                            )
                                        } finally {
                                            isSigningIn = false
                                        }
                                    }
                                },
                                emphasis = GlassEmphasis.Prominent,
                                navigationSurfaceColor = semantic.accentBlue,
                                size = GlasmioButtonSize.Medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("google_sign_in_button"),
                            ) {
                                if (isSigningIn) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    SfIcon(
                                        systemName = ThazhSymbols.AccountCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        size = 18.dp,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = stringResource(R.string.link_share_sign_in_google),
                                    style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                )
                            }
                        }
                    }
                } else {
                    // Signed-in User Header Card
                    GlasmioCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    semantic.accentGreen.copy(alpha = if (colors.isDark) 0.16f else 0.10f),
                                    cardShape,
                                )
                                .border(1.dp, semantic.accentGreen.copy(alpha = 0.30f), cardShape)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(semantic.accentGreen.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                SfIcon(
                                    systemName = ThazhSymbols.CloudDone,
                                    contentDescription = null,
                                    tint = semantic.accentGreen,
                                    size = 20.dp,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.displayName?.takeIf { it.isNotBlank() }
                                        ?: user.email
                                        ?: stringResource(R.string.link_share_signed_in_default),
                                    style = GlasmioTheme.textStyles.headline,
                                    color = colors.content,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = user.email ?: stringResource(R.string.link_share_cloud_sync_active),
                                    style = GlasmioTheme.textStyles.footnote,
                                    color = colors.secondaryContent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            GlasmioIconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        GoogleAuthHelper.signOut(context)
                                    }
                                },
                                size = 44.dp,
                                modifier = Modifier.testTag("google_sign_out_button"),
                            ) {
                                SfIcon(
                                    systemName = ThazhSymbols.Logout,
                                    contentDescription = stringResource(R.string.link_share_sign_out),
                                    tint = colors.destructive,
                                    size = 19.dp,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Real-Time Cloud Synced Links (Scoped to key(user.uid))
        val signedInUser = currentUser
        if (signedInUser != null) {
            item(key = "cloud_history_section_${signedInUser.uid}") {
                key(signedInUser.uid) {
                    val cloudLinksFlow = remember(signedInUser.uid) {
                        observeCloudLinks(signedInUser.uid)
                    }
                    val cloudLinks by cloudLinksFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                    CenteredLinkContent {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlasmioSectionTitle(
                                title = stringResource(R.string.link_share_cloud_history_title, cloudLinks.size),
                            )

                            if (cloudLinks.isEmpty()) {
                                GlasmioCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        SfIcon(
                                            systemName = ThazhSymbols.Link,
                                            contentDescription = null,
                                            tint = colors.tertiaryContent,
                                            size = 32.dp,
                                        )
                                        Text(
                                            text = stringResource(R.string.link_share_cloud_empty_title),
                                            style = GlasmioTheme.textStyles.headline,
                                            color = colors.content,
                                            textAlign = TextAlign.Center,
                                        )
                                        Text(
                                            text = stringResource(R.string.link_share_cloud_empty_body),
                                            style = GlasmioTheme.textStyles.subheadline,
                                            color = colors.secondaryContent,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            } else {
                                cloudLinks.forEach { linkItem ->
                                    SharedLinkRowCard(
                                        fileName = linkItem.fileName,
                                        fileSizeBytes = linkItem.fileSizeBytes,
                                        mimeType = linkItem.mimeType,
                                        url = linkItem.url,
                                        expiryCode = linkItem.expiryLabel,
                                        note = linkItem.note,
                                        timestampEpochMs = linkItem.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                                        onCopy = { copyUrlToClipboard(context, linkItem.url) },
                                        onShare = { shareUrlIntent(context, linkItem.fileName, linkItem.url) },
                                        onShowQr = { qrPreviewTarget = linkItem.fileName to linkItem.url },
                                        onOpenBrowser = { openUrlInBrowser(context, linkItem.url) },
                                        onEditNote = {
                                            editNoteValue = TextFieldValue(
                                                linkItem.note,
                                                TextRange(linkItem.note.length),
                                            )
                                            linkToEditNote = linkItem
                                        },
                                        onDelete = {
                                            onDeleteCloudLink(signedInUser.uid, linkItem.id)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // QR Code Preview Dialog
    val qrTarget = lastQrTarget
    GlasmioAlertDialog(
        visible = qrPreviewTarget != null && qrTarget != null,
        onDismissRequest = { qrPreviewTarget = null },
        title = qrTarget?.first ?: stringResource(R.string.link_share_qr_title),
        message = qrTarget?.second,
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.link_share_copy_button),
                onClick = {
                    qrTarget?.second?.let { copyUrlToClipboard(context, it) }
                    qrPreviewTarget = null
                },
                role = GlasmioAlertButtonRole.Default,
            ),
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.close),
                onClick = { qrPreviewTarget = null },
                role = GlasmioAlertButtonRole.Cancel,
            ),
        ),
    ) {
        if (qrTarget != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                StylizedQrMatrixBox(
                    url = qrTarget.second,
                    modifier = Modifier
                        .size(184.dp)
                        .clip(ContinuousRoundedRectangle(18.dp))
                        .background(Color.White)
                        .padding(14.dp),
                )
            }
        }
    }

    // Edit Note Dialog
    val editTarget = lastLinkToEditNote
    val currentUid = currentUser?.uid
    GlasmioAlertDialog(
        visible = linkToEditNote != null && editTarget != null,
        onDismissRequest = { linkToEditNote = null },
        title = stringResource(R.string.link_share_edit_note_title),
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.cancel),
                onClick = { linkToEditNote = null },
                role = GlasmioAlertButtonRole.Cancel,
            ),
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.save),
                onClick = {
                    if (currentUid != null && editTarget != null) {
                        onUpdateCloudLinkNote(currentUid, editTarget.id, editNoteValue.text)
                    }
                    linkToEditNote = null
                },
                role = GlasmioAlertButtonRole.Default,
            ),
        ),
    ) {
        GlasmioAlertFieldGroup {
            GlasmioAlertTextField(
                value = editNoteValue,
                onValueChange = { editNoteValue = it },
                placeholder = stringResource(R.string.link_share_note_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (currentUid != null && editTarget != null) {
                            onUpdateCloudLinkNote(currentUid, editTarget.id, editNoteValue.text)
                        }
                        linkToEditNote = null
                    }
                ),
            )
        }
    }

    // Info dialog explaining optional Cloud Auth & Email/Password vs Google Sign-In
    GlasmioAlertDialog(
        visible = showAuthHelpDialog,
        onDismissRequest = { showAuthHelpDialog = false },
        title = stringResource(R.string.link_share_auth_info_title),
        message = stringResource(R.string.link_share_auth_info_body),
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.close),
                onClick = { showAuthHelpDialog = false },
                role = GlasmioAlertButtonRole.Default,
            ),
        ),
    )
}

@Composable
private fun SharedLinkRowCard(
    fileName: String,
    fileSizeBytes: Long,
    mimeType: String,
    url: String,
    expiryCode: String,
    note: String,
    timestampEpochMs: Long,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onShowQr: () -> Unit,
    onOpenBrowser: () -> Unit,
    onEditNote: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val category = remember(fileName, mimeType) { FileCategory.from(mimeType, fileName) }

    GlasmioCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FileTypeBadge(category = category, size = 44.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = GlasmioTheme.textStyles.headline,
                        color = colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = buildList {
                        add(FormatUtils.formatFileSize(fileSizeBytes))
                        add(expiryCodeToBadgeText(expiryCode))
                        add(FormatUtils.formatRelativeTime(timestampEpochMs))
                    }.joinToString(" • ")
                    Text(
                        text = meta,
                        style = GlasmioTheme.textStyles.caption1,
                        color = colors.secondaryContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (note.isNotBlank()) {
                Text(
                    text = note,
                    style = GlasmioTheme.textStyles.footnote,
                    color = colors.content.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Clickable URL pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ContinuousRoundedRectangle(12.dp))
                    .background(semantic.accentBlue.copy(alpha = if (colors.isDark) 0.16f else 0.09f))
                    .clickable(role = Role.Button, onClick = onCopy)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SfIcon(
                    systemName = ThazhSymbols.Link,
                    contentDescription = null,
                    tint = semantic.accentBlue,
                    size = 16.dp,
                )
                Text(
                    text = url,
                    style = GlasmioTheme.textStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = semantic.accentBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                SfIcon(
                    systemName = ThazhSymbols.Copy,
                    contentDescription = stringResource(R.string.link_share_copy_button),
                    tint = semantic.accentBlue,
                    size = 16.dp,
                )
            }

            // Action toolbar row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlasmioButton(
                    onClick = onCopy,
                    emphasis = GlassEmphasis.Prominent,
                    navigationSurfaceColor = semantic.accentBlue,
                    size = GlasmioButtonSize.Small,
                    modifier = Modifier.weight(1f),
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.Copy,
                        contentDescription = null,
                        tint = Color.White,
                        size = 15.dp,
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.link_share_copy_button),
                        style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                }

                GlasmioButton(
                    onClick = onShare,
                    emphasis = GlassEmphasis.Regular,
                    size = GlasmioButtonSize.Small,
                    modifier = Modifier.weight(1f),
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.Share,
                        contentDescription = null,
                        tint = colors.accent,
                        size = 15.dp,
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.received_files_share),
                        style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent,
                    )
                }

                GlasmioIconButton(
                    onClick = onShowQr,
                    size = 40.dp,
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.QrCode,
                        contentDescription = stringResource(R.string.link_share_qr_title),
                        tint = colors.content,
                        size = 18.dp,
                    )
                }

                GlasmioIconButton(
                    onClick = onOpenBrowser,
                    size = 40.dp,
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.OpenInNew,
                        contentDescription = stringResource(R.string.received_files_open),
                        tint = colors.content,
                        size = 18.dp,
                    )
                }

                if (onEditNote != null) {
                    GlasmioIconButton(
                        onClick = onEditNote,
                        size = 40.dp,
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.Edit,
                            contentDescription = stringResource(R.string.link_share_edit_note_title),
                            tint = colors.secondaryContent,
                            size = 17.dp,
                        )
                    }
                }

                if (onDelete != null) {
                    GlasmioIconButton(
                        onClick = onDelete,
                        size = 40.dp,
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = colors.destructive,
                            size = 18.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StylizedQrMatrixBox(
    url: String,
    modifier: Modifier = Modifier,
) {
    val digest = remember(url) {
        MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
    }
    Canvas(modifier = modifier) {
        val grid = 21
        val cell = size.minDimension / grid.toFloat()

        fun isFinderPattern(r: Int, c: Int): Boolean {
            val topLeft = r in 0..6 && c in 0..6
            val topRight = r in 0..6 && c in (grid - 7) until grid
            val bottomLeft = r in (grid - 7) until grid && c in 0..6
            return topLeft || topRight || bottomLeft
        }

        fun drawFinder(startR: Int, startC: Int) {
            for (dr in 0..6) {
                for (dc in 0..6) {
                    val border = dr == 0 || dr == 6 || dc == 0 || dc == 6
                    val inner = dr in 2..4 && dc in 2..4
                    if (border || inner) {
                        drawRect(
                            color = Color(0xFF10141E),
                            topLeft = Offset((startC + dc) * cell, (startR + dr) * cell),
                            size = Size(cell * 0.96f, cell * 0.96f),
                        )
                    }
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, grid - 7)
        drawFinder(grid - 7, 0)

        var bitIdx = 0
        for (r in 0 until grid) {
            for (c in 0 until grid) {
                if (isFinderPattern(r, c)) continue
                if (r == 7 || c == 7) continue
                val byteVal = digest[(bitIdx / 8) % digest.size].toInt() and 0xFF
                val on = ((byteVal shr (bitIdx % 8)) and 1) == 1
                bitIdx++
                if (on) {
                    drawRect(
                        color = Color(0xFF10141E),
                        topLeft = Offset(c * cell, r * cell),
                        size = Size(cell * 0.92f, cell * 0.92f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredLinkContent(content: @Composable () -> Unit) {
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

@Composable
private fun expiryDisplayLabel(expiry: CatboxExpiry): String = when (expiry) {
    CatboxExpiry.PERMANENT -> stringResource(R.string.link_share_expiry_permanent)
    CatboxExpiry.ONE_HOUR -> stringResource(R.string.link_share_expiry_1h)
    CatboxExpiry.TWELVE_HOURS -> stringResource(R.string.link_share_expiry_12h)
    CatboxExpiry.ONE_DAY -> stringResource(R.string.link_share_expiry_24h)
    CatboxExpiry.THREE_DAYS -> stringResource(R.string.link_share_expiry_72h)
}

@Composable
private fun expiryCodeToBadgeText(code: String): String = when (code) {
    "1H" -> stringResource(R.string.link_share_expiry_1h)
    "12H" -> stringResource(R.string.link_share_expiry_12h)
    "24H" -> stringResource(R.string.link_share_expiry_24h)
    "72H" -> stringResource(R.string.link_share_expiry_72h)
    else -> stringResource(R.string.link_share_expiry_permanent)
}

private fun copyUrlToClipboard(context: Context, url: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Catbox Link", url))
    Toast.makeText(
        context,
        context.getString(R.string.link_share_copied_toast),
        Toast.LENGTH_SHORT,
    ).show()
}

private fun shareUrlIntent(context: Context, fileName: String, url: String) {
    runCatching {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            putExtra(Intent.EXTRA_TEXT, "$fileName: $url")
        }
        val chooser = Intent.createChooser(sendIntent, context.getString(R.string.received_files_share_chooser)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

private fun openUrlInBrowser(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
