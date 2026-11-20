package com.thazh.share.ui.components

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.DownloadListener
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.capsule.ContinuousCapsule
import com.thazh.share.R
import com.thazh.share.ui.auth.findActivity
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import com.thazh.share.util.DeviceOptimizationManager
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.glass.GlassSurfaceStyle
import io.github.neoruaa.glasmio.glass.LocalBlurBackdrop
import io.github.neoruaa.glasmio.glass.LocalGlassBackdrop
import io.github.neoruaa.glasmio.glass.glassBackdropSource
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberCombinedBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.glass.trackBackdropPosition
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioBottomSheet
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioMenu
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem
import io.github.neoruaa.glasmio.ui.GlasmioSearchBar
import io.github.neoruaa.glasmio.ui.GlasmioSheetToolbarButton
import io.github.neoruaa.glasmio.ui.GlasmioSheetTopToolbar
import io.github.neoruaa.glasmio.ui.LocalGlasmioSheetContentPadding
import kotlinx.coroutines.yield

fun normalizeWebUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return "https://catbox.moe"
    return if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

private fun extractDisplayDomain(url: String): String {
    val host = runCatching {
        Uri.parse(url).host?.removePrefix("www.")
    }.getOrNull()?.takeIf { it.isNotBlank() }
    if (host != null) return host
    return url
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .substringBefore('/')
        .ifBlank { url }
}

private fun destroyWebViewSafely(webView: WebView?) {
    if (webView == null) return
    runCatching {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.stopLoading()
        webView.onPause()
        webView.clearHistory()
        webView.removeAllViews()
        webView.destroy()
    }
}

/**
 * Full-screen iOS 26 Safari / Glasmio Liquid Glass In-App WebView matching uploaded references `1.png` and `2.png`:
 *
 * - **Full-bleed WebView canvas** (no bulky top navigation header).
 * - **Expanded State (`1.png` — when not scrolled / at top, or when scrolling from bottom to top / pulling content up)**:
 *   1. Left floating circular Liquid Glass button (`<`): navigates back in WebView history (or closes modal if at root).
 *   2. Center floating Liquid Glass capsule bar:
 *      - Left menu button (3 horizontal lines with uneven bottom bar, matching `1.png`): opens options menu (Forward, Copy Link, Share Link, Open in External Browser, Edit URL).
 *      - Center bold domain label (e.g., `thazh.site`) — tapping opens the URL editor sheet.
 *      - Right clockwise reload button (`↻`): reloads or recovers the page.
 *   3. Right floating circular Liquid Glass button (overlapping squares icon): closes the browser modal / returns to tabs.
 * - **Collapsed State (`2.png` — when scrolling down the page / finger swiping from bottom to top to read lower content)**:
 *   - Smoothly morphs into a compact centered floating Liquid Glass pill showing only the domain name (`thazh.site`).
 *   - Tapping the compact pill expands the full toolbar immediately.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppWebBrowserModal(
    initialUrl: String,
    initialTitle: String? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val activityContext = remember(context) {
        runCatching { context.findActivity() }.getOrElse { context }
    }
    val deviceProfile = remember(context) {
        DeviceOptimizationManager.getProfile(context)
    }
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic

    var currentUrl by remember(initialUrl) { mutableStateOf(normalizeWebUrl(initialUrl)) }
    var urlQuery by remember(initialUrl) {
        mutableStateOf(TextFieldValue(normalizeWebUrl(initialUrl)))
    }
    var showUrlEditSheet by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    var pageTitle by remember(initialTitle) { mutableStateOf(initialTitle.orEmpty()) }
    var progress by remember { mutableFloatStateOf(0.1f) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Scroll-driven bottom bar state:
    // false -> Image 1.png (Expanded: Left Circle '<' + Center Capsule '[≡  domain  ↻]' + Right Circle '[⧉]')
    // true  -> Image 2.png (Collapsed: Compact centered capsule '[domain]')
    var isToolbarCollapsed by remember { mutableStateOf(false) }

    var webViewGeneration by remember { mutableIntStateOf(0) }
    var rendererCrashed by remember { mutableStateOf(false) }
    var loadErrorDescription by remember { mutableStateOf<String?>(null) }

    val displayDomain = remember(currentUrl) {
        extractDisplayDomain(currentUrl)
    }

    val navigateToInputUrl: (String) -> Unit = { rawUrl ->
        val target = normalizeWebUrl(rawUrl)
        currentUrl = target
        urlQuery = TextFieldValue(target)
        showUrlEditSheet = false
        isToolbarCollapsed = false
        loadErrorDescription = null
        if (rendererCrashed) {
            rendererCrashed = false
            webViewGeneration++
        } else {
            webViewRef?.loadUrl(target)
        }
    }

    val reloadOrRecoverWebView: () -> Unit = {
        loadErrorDescription = null
        if (rendererCrashed || webViewRef == null) {
            rendererCrashed = false
            webViewGeneration++
        } else {
            webViewRef?.reload()
        }
    }

    BackHandler {
        val wv = webViewRef
        when {
            showUrlEditSheet -> showUrlEditSheet = false
            showOptionsMenu -> showOptionsMenu = false
            !rendererCrashed && wv != null && wv.canGoBack() -> wv.goBack()
            else -> onDismiss()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val wv = webViewRef
            webViewRef = null
            destroyWebViewSafely(wv)
        }
    }

    val hostBackdrop = LocalGlassBackdrop.current
    val webContentBackdrop = rememberLayerBackdrop()
    val floatingBarBackdrop = rememberCombinedBackdrop(hostBackdrop, webContentBackdrop)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
            .clickable(enabled = true, onClick = {})
            .testTag("in_app_web_browser_modal"),
    ) {
        // 1. Full-Screen WebView Viewport (captured as glass backdrop source for the floating bottom bar)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassBackdropSource(webContentBackdrop)
                .trackBackdropPosition(webContentBackdrop),
        ) {
            if (rendererCrashed || loadErrorDescription != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    GlasmioCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ThazhSquircleIconBox(
                                symbol = ThazhSymbols.Globe,
                                accentColor = semantic.accentBlue,
                                boxSize = 54.dp,
                                iconSize = 26.dp,
                                cornerRadius = 16.dp,
                            )
                            GlasmioText(
                                text = pageTitle.ifBlank {
                                    stringResource(R.string.browser_title_default)
                                },
                                style = GlasmioTheme.textStyles.headline.copy(fontWeight = FontWeight.Bold),
                                color = colors.content,
                                textAlign = TextAlign.Center,
                            )
                            GlasmioText(
                                text = currentUrl,
                                style = GlasmioTheme.textStyles.footnote,
                                color = semantic.accentBlue,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!loadErrorDescription.isNullOrBlank()) {
                                GlasmioText(
                                    text = loadErrorDescription.orEmpty(),
                                    style = GlasmioTheme.textStyles.caption1,
                                    color = colors.secondaryContent,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                GlasmioButton(
                                    onClick = reloadOrRecoverWebView,
                                    emphasis = GlassEmphasis.Regular,
                                    size = GlasmioButtonSize.Medium,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    SfIcon(
                                        systemName = ThazhSymbols.Refresh,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        size = 16.dp,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    GlasmioText(
                                        text = stringResource(R.string.browser_action_reload),
                                        style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.accent,
                                    )
                                }

                                GlasmioButton(
                                    onClick = {
                                        runCatching {
                                            activityContext.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)),
                                            )
                                        }
                                    },
                                    emphasis = GlassEmphasis.Prominent,
                                    navigationSurfaceColor = semantic.accentBlue,
                                    size = GlasmioButtonSize.Medium,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    SfIcon(
                                        systemName = ThazhSymbols.OpenInNew,
                                        contentDescription = null,
                                        tint = Color.White,
                                        size = 16.dp,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    GlasmioText(
                                        text = stringResource(R.string.browser_action_open_external),
                                        style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                var isWebViewReadyToMount by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    yield()
                    isWebViewReadyToMount = true
                }
                if (isWebViewReadyToMount) {
                    key(webViewGeneration) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding(),
                            factory = {
                                WebView(activityContext).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                    )
                                    if (deviceProfile.useLightweightWebView) {
                                        setLayerType(View.LAYER_TYPE_NONE, null)
                                    }
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        loadsImagesAutomatically = true
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        setSupportZoom(true)
                                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                        cacheMode = if (deviceProfile.isLowRamDevice) {
                                            WebSettings.LOAD_NO_CACHE
                                        } else {
                                            WebSettings.LOAD_DEFAULT
                                        }
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        setRendererPriorityPolicy(
                                            WebView.RENDERER_PRIORITY_IMPORTANT,
                                            true,
                                        )
                                    }

                                    // Scroll listener driving the transition between 1.png (Expanded) and 2.png (Collapsed)
                                    var accumulatedDy = 0
                                    setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                                        val dy = scrollY - oldScrollY
                                        if (scrollY <= 24) {
                                            // At top of page -> always Expanded (1.png)
                                            accumulatedDy = 0
                                            if (isToolbarCollapsed) {
                                                isToolbarCollapsed = false
                                            }
                                        } else if (dy > 0) {
                                            // Scrolling down the page -> collapse into mini domain pill (2.png)
                                            accumulatedDy = if (accumulatedDy > 0) accumulatedDy + dy else dy
                                            if (accumulatedDy > 18 && !isToolbarCollapsed) {
                                                isToolbarCollapsed = true
                                            }
                                        } else if (dy < 0) {
                                            // Scrolling back up toward top -> expand full 3-part bar (1.png)
                                            accumulatedDy = if (accumulatedDy < 0) accumulatedDy + dy else dy
                                            if (accumulatedDy < -18 && isToolbarCollapsed) {
                                                isToolbarCollapsed = false
                                            }
                                        }
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(
                                            view: WebView?,
                                            url: String?,
                                            favicon: Bitmap?,
                                        ) {
                                            super.onPageStarted(view, url, favicon)
                                            isLoading = true
                                            isToolbarCollapsed = false
                                            loadErrorDescription = null
                                            if (!url.isNullOrBlank()) {
                                                currentUrl = url
                                                urlQuery = TextFieldValue(url)
                                            }
                                            canGoBack = view?.canGoBack() == true
                                            canGoForward = view?.canGoForward() == true
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isLoading = false
                                            progress = 1f
                                            if (!url.isNullOrBlank()) {
                                                currentUrl = url
                                                urlQuery = TextFieldValue(url)
                                            }
                                            val title = view?.title?.takeIf { it.isNotBlank() }
                                            if (title != null) {
                                                pageTitle = title
                                            }
                                            canGoBack = view?.canGoBack() == true
                                            canGoForward = view?.canGoForward() == true
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: WebResourceError?,
                                        ) {
                                            super.onReceivedError(view, request, error)
                                            if (request?.isForMainFrame == true) {
                                                isLoading = false
                                            }
                                        }

                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                        ): Boolean {
                                            val uri = request?.url ?: return false
                                            val scheme = uri.scheme?.lowercase().orEmpty()
                                            if (scheme == "http" || scheme == "https") {
                                                return false
                                            }
                                            runCatching {
                                                activityContext.startActivity(
                                                    Intent(Intent.ACTION_VIEW, uri),
                                                )
                                            }
                                            return true
                                        }

                                        override fun onRenderProcessGone(
                                            view: WebView?,
                                            detail: RenderProcessGoneDetail?,
                                        ): Boolean {
                                            isLoading = false
                                            canGoBack = false
                                            canGoForward = false
                                            if (webViewRef === view) {
                                                webViewRef = null
                                            }
                                            destroyWebViewSafely(view)
                                            rendererCrashed = true
                                            return true
                                        }
                                    }
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(
                                            view: WebView?,
                                            newProgress: Int,
                                        ) {
                                            progress = (newProgress / 100f).coerceIn(0.05f, 1f)
                                            isLoading = newProgress < 100
                                        }

                                        override fun onReceivedTitle(view: WebView?, title: String?) {
                                            if (!title.isNullOrBlank()) {
                                                pageTitle = title
                                            }
                                        }
                                    }
                                    setDownloadListener(
                                        DownloadListener { url, _, _, _, _ ->
                                            runCatching {
                                                activityContext.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                                )
                                            }
                                        },
                                    )
                                    webViewRef = this
                                    runCatching {
                                        loadUrl(currentUrl)
                                    }.onFailure { err ->
                                        loadErrorDescription = err.localizedMessage
                                    }
                                }
                            },
                            update = { wv ->
                                webViewRef = wv
                                canGoBack = wv.canGoBack()
                                canGoForward = wv.canGoForward()
                            },
                        )
                    }
                }
            }
        }

        // Subtle top thin progress line while loading
        AnimatedVisibility(
            visible = isLoading && !rendererCrashed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth(),
        ) {
            GlasmioLinearProgressBar(
                progress = progress,
                color = semantic.accentBlue,
                height = 2.5.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // 2. Bottom Floating Safari Liquid Glass Bar matching 1.png (Expanded) and 2.png (Collapsed)
        CompositionLocalProvider(LocalBlurBackdrop provides floatingBarBackdrop) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 14.dp)
                    .testTag("in_app_browser_bottom_toolbar"),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = isToolbarCollapsed,
                    transitionSpec = {
                        (
                            fadeIn(tween(210)) +
                                scaleIn(
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                    initialScale = 0.90f,
                                )
                            ) togetherWith (
                            fadeOut(tween(150)) +
                                scaleOut(tween(150), targetScale = 0.90f)
                            ) using SizeTransform(clip = false)
                    },
                    label = "safari_floating_toolbar_state",
                ) { collapsed ->
                    if (collapsed) {
                        // STATE 2 (2.png): Compact centered floating domain capsule
                        CollapsedSafariDomainPill(
                            domain = displayDomain,
                            onClick = { isToolbarCollapsed = false },
                        )
                    } else {
                        // STATE 1 (1.png): 3-part floating Liquid Glass bar
                        ExpandedSafariFloatingToolbar(
                            domain = displayDomain,
                            canGoBack = canGoBack && !rendererCrashed,
                            canGoForward = canGoForward && !rendererCrashed,
                            showOptionsMenu = showOptionsMenu,
                            onShowOptionsMenuChange = { showOptionsMenu = it },
                            onBackClick = {
                                val wv = webViewRef
                                if (!rendererCrashed && wv != null && wv.canGoBack()) {
                                    wv.goBack()
                                } else {
                                    onDismiss()
                                }
                            },
                            onForwardClick = {
                                webViewRef?.goForward()
                            },
                            onDomainClick = {
                                urlQuery = TextFieldValue(currentUrl)
                                showUrlEditSheet = true
                            },
                            onReloadClick = reloadOrRecoverWebView,
                            onCopyUrlClick = {
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("URL", currentUrl))
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.link_share_copied_toast),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            onShareUrlClick = {
                                runCatching {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, currentUrl)
                                    }
                                    activityContext.startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            pageTitle.ifBlank { currentUrl },
                                        ),
                                    )
                                }
                            },
                            onOpenExternalClick = {
                                runCatching {
                                    activityContext.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)),
                                    )
                                }
                            },
                            onTabsDismissClick = onDismiss,
                        )
                    }
                }
            }
        }
    }

    // URL Editor Bottom Sheet when tapping the center domain text or "Edit Address"
    if (showUrlEditSheet) {
        var isSearchActive by remember { mutableStateOf(true) }
        GlasmioBottomSheet(
            visible = true,
            onDismissRequest = { showUrlEditSheet = false },
            fitContent = true,
            title = stringResource(R.string.browser_title_default),
            topBar = {
                GlasmioSheetTopToolbar(
                    title = pageTitle.ifBlank { displayDomain },
                    actions = {
                        GlasmioSheetToolbarButton(
                            onClick = { showUrlEditSheet = false },
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
                    .padding(
                        top = sheetPadding.calculateTopPadding() + 12.dp,
                        bottom = sheetPadding.calculateBottomPadding() + 20.dp,
                    )
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlasmioSearchBar(
                        query = urlQuery,
                        onQueryChange = { urlQuery = it },
                        active = isSearchActive,
                        onActiveChange = { isSearchActive = it },
                        onClose = {
                            urlQuery = TextFieldValue(currentUrl)
                            showUrlEditSheet = false
                        },
                        onSearch = { entered -> navigateToInputUrl(entered) },
                        placeholder = stringResource(R.string.browser_url_hint),
                        closeContentDescription = stringResource(R.string.close),
                        controlHeight = 44.dp,
                        searchIconSize = 18.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("in_app_browser_url_input"),
                    )

                    GlasmioButton(
                        onClick = { navigateToInputUrl(urlQuery.text) },
                        emphasis = GlassEmphasis.Prominent,
                        navigationSurfaceColor = semantic.accentBlue,
                        size = GlasmioButtonSize.Medium,
                        modifier = Modifier.testTag("in_app_browser_go_button"),
                    ) {
                        GlasmioText(
                            text = stringResource(R.string.link_share_browse_go),
                            style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Image `1.png` — Expanded 3-part floating Safari Liquid Glass bar:
 * `[ ( < ) ]   [  ≡     thazh.site     ↻  ]   [ ( ⧉ ) ]`
 */
@Composable
private fun ExpandedSafariFloatingToolbar(
    domain: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    showOptionsMenu: Boolean,
    onShowOptionsMenuChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onDomainClick: () -> Unit,
    onReloadClick: () -> Unit,
    onCopyUrlClick: () -> Unit,
    onShareUrlClick: () -> Unit,
    onOpenExternalClick: () -> Unit,
    onTabsDismissClick: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val pillBackground = if (colors.isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.84f)
    } else {
        Color.White.copy(alpha = 0.88f)
    }
    val pillBorderColor = if (colors.isDark) {
        Color.White.copy(alpha = 0.16f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    val iconColor = if (colors.isDark) Color.White else Color(0xFF111111)
    val mutedChevronColor = if (canGoBack) {
        iconColor
    } else {
        iconColor.copy(alpha = 0.34f)
    }

    Row(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Left Circular Glass Button: `<` (Back)
        GlassSurface(
            shape = CircleShape,
            style = GlassSurfaceStyle.Navigation,
            nativeNavigationFlex = true,
            onClick = onBackClick,
            modifier = Modifier
                .size(52.dp)
                .testTag("in_app_browser_back_button"),
        ) {
            SfIcon(
                systemName = ThazhSymbols.Back,
                contentDescription = stringResource(R.string.browser_action_back),
                tint = mutedChevronColor,
                size = 21.dp,
            )
        }

        // 2. Center Capsule Glass Bar: `[ ≡    thazh.site    ↻ ]`
        GlassSurface(
            shape = ContinuousCapsule,
            style = GlassSurfaceStyle.Navigation,
            nativeNavigationFlex = true,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left Menu Icon (`≡` uneven 3 lines matching 1.png) with GlasmioMenu
                GlasmioMenu(
                    expanded = showOptionsMenu,
                    onExpandedChange = onShowOptionsMenuChange,
                    itemCount = if (canGoForward) 5 else 4,
                    anchor = { openMenu ->
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = openMenu,
                                )
                                .testTag("in_app_browser_menu_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            SafariPageMenuGlyph(
                                color = iconColor,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    },
                ) { _, close ->
                    if (canGoForward) {
                        GlasmioMenuItem(
                            title = stringResource(R.string.browser_action_forward),
                            systemName = ThazhSymbols.ChevronRight,
                            onClick = {
                                close()
                                onForwardClick()
                            },
                        )
                    }
                    GlasmioMenuItem(
                        title = stringResource(R.string.browser_url_hint),
                        systemName = ThazhSymbols.Search,
                        onClick = {
                            close()
                            onDomainClick()
                        },
                    )
                    GlasmioMenuItem(
                        title = stringResource(R.string.browser_action_copy_url),
                        systemName = ThazhSymbols.Copy,
                        onClick = {
                            close()
                            onCopyUrlClick()
                        },
                    )
                    GlasmioMenuItem(
                        title = stringResource(R.string.browser_action_share),
                        systemName = ThazhSymbols.Share,
                        onClick = {
                            close()
                            onShareUrlClick()
                        },
                    )
                    GlasmioMenuItem(
                        title = stringResource(R.string.browser_action_open_external),
                        systemName = ThazhSymbols.OpenInNew,
                        onClick = {
                            close()
                            onOpenExternalClick()
                        },
                    )
                }

                // Center Domain Name (`thazh.site`)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(ContinuousCapsule)
                        .clickable(
                            role = Role.Button,
                            onClick = onDomainClick,
                        )
                        .padding(horizontal = 6.dp)
                        .testTag("in_app_browser_domain_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    GlasmioText(
                        text = domain,
                        style = GlasmioTheme.textStyles.headline.copy(fontWeight = FontWeight.SemiBold),
                        color = iconColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }

                // Right Reload Button (`↻`)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = onReloadClick,
                        )
                        .testTag("in_app_browser_reload_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.Refresh,
                        contentDescription = stringResource(R.string.browser_action_reload),
                        tint = iconColor,
                        size = 19.dp,
                    )
                }
            }
        }

        // 3. Right Circular Glass Button: Overlapping Squares (`⧉` Tabs / Close button matching 1.png)
        GlassSurface(
            shape = CircleShape,
            style = GlassSurfaceStyle.Navigation,
            nativeNavigationFlex = true,
            onClick = onTabsDismissClick,
            modifier = Modifier
                .size(52.dp)
                .testTag("in_app_browser_close_button"),
        ) {
            SafariOverlappingSquaresGlyph(
                color = iconColor,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * Image `2.png` — Collapsed compact centered floating domain pill when scrolling down:
 * `[   thazh.site   ]`
 */
@Composable
private fun CollapsedSafariDomainPill(
    domain: String,
    onClick: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val textColor = if (colors.isDark) Color.White else Color(0xFF111111)

    GlassSurface(
        shape = ContinuousCapsule,
        style = GlassSurfaceStyle.Navigation,
        nativeNavigationFlex = true,
        onClick = onClick,
        modifier = Modifier
            .height(38.dp)
            .widthIn(min = 114.dp, max = 240.dp)
            .testTag("in_app_browser_collapsed_domain_pill"),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlasmioText(
                text = domain,
                style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Custom Canvas glyph reproducing the exact 3-line page menu icon on the left of the capsule in `1.png`
 * (two full-width top/middle horizontal lines and a shorter left-aligned bottom line).
 */
@Composable
private fun SafariPageMenuGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokePx = 2.0.dp.toPx()
        val leftX = size.width * 0.16f
        val fullRightX = size.width * 0.84f
        val shortRightX = size.width * 0.58f

        val topY = size.height * 0.27f
        val midY = size.height * 0.50f
        val botY = size.height * 0.73f

        drawLine(
            color = color,
            start = Offset(leftX, topY),
            end = Offset(fullRightX, topY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(leftX, midY),
            end = Offset(fullRightX, midY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(leftX, botY),
            end = Offset(shortRightX, botY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * Custom Canvas glyph reproducing the exact overlapping rounded squares icon (`square.on.square`)
 * inside the right circular button in `1.png`.
 */
@Composable
private fun SafariOverlappingSquaresGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokePx = 1.9.dp.toPx()
        val cornerPx = 3.2.dp.toPx()
        val boxSide = size.minDimension * 0.52f

        // Back top-left rounded square
        val backTopLeft = Offset(size.width * 0.16f, size.height * 0.16f)
        drawRoundRect(
            color = color,
            topLeft = backTopLeft,
            size = Size(boxSide, boxSide),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style = Stroke(width = strokePx),
        )

        // Front bottom-right rounded square
        val frontTopLeft = Offset(size.width * 0.34f, size.height * 0.34f)
        drawRoundRect(
            color = color,
            topLeft = frontTopLeft,
            size = Size(boxSide, boxSide),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style = Stroke(width = strokePx),
        )
    }
}
