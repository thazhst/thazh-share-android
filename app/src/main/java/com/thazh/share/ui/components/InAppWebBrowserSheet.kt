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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.thazh.share.R
import com.thazh.share.ui.auth.findActivity
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import com.thazh.share.util.DeviceOptimizationManager
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.GlassSurfaceStyle
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioBottomToolbar
import io.github.neoruaa.glasmio.ui.GlasmioBottomToolbarStyle
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioSearchBar
import io.github.neoruaa.glasmio.ui.GlasmioTopBar
import io.github.neoruaa.glasmio.ui.GlasmioTopBarStyle

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
 * Full Glasmio Liquid Glass In-App Web Browser using authentic GlasmioUI components:
 * - [GlasmioTopBar] (`GlasmioTopBarStyle.TwoLine`) with [GlasmioIconButton] navigation & reload controls
 * - [GlasmioSearchBar] for URL / cloud link entry
 * - [GlasmioBottomToolbar] floating Liquid Glass capsule dock for Back, Forward, Copy, Share, and External Open
 * - [GlasmioCard] & [GlasmioButton] for resilient recovery
 * - Hardware-adaptive WebView tuning via [DeviceOptimizationManager] for Google Pixel, Oppo, and Redmi A Series.
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
    var isEditingUrl by remember { mutableStateOf(false) }
    var pageTitle by remember(initialTitle) { mutableStateOf(initialTitle.orEmpty()) }
    var progress by remember { mutableFloatStateOf(0.1f) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var webViewGeneration by remember { mutableIntStateOf(0) }
    var rendererCrashed by remember { mutableStateOf(false) }
    var loadErrorDescription by remember { mutableStateOf<String?>(null) }

    val navigateToInputUrl: (String) -> Unit = { rawUrl ->
        val target = normalizeWebUrl(rawUrl)
        currentUrl = target
        urlQuery = TextFieldValue(target)
        isEditingUrl = false
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
        if (isEditingUrl) {
            isEditingUrl = false
        } else if (!rendererCrashed && wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onDismiss()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val wv = webViewRef
            webViewRef = null
            destroyWebViewSafely(wv)
        }
    }

    val hostSubtitle = remember(currentUrl) {
        val host = runCatching { Uri.parse(currentUrl).host?.removePrefix("www.") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: currentUrl
        if (currentUrl.startsWith("https://", ignoreCase = true)) "🔒 $host" else host
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
            .clickable(enabled = true, onClick = {})
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("in_app_web_browser_modal"),
    ) {
        // 1. Authentic GlasmioTopBar + GlasmioSearchBar Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.groupedBackground)
                .padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GlasmioTopBar(
                title = pageTitle.ifBlank { stringResource(R.string.browser_title_default) },
                subtitle = hostSubtitle,
                style = GlasmioTopBarStyle.TwoLine,
                navigation = {
                    GlasmioIconButton(
                        onClick = onDismiss,
                        style = GlassSurfaceStyle.Navigation,
                        modifier = Modifier.testTag("in_app_browser_close_button"),
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = colors.content,
                        )
                    }
                },
                actions = {
                    GlasmioIconButton(
                        onClick = reloadOrRecoverWebView,
                        style = GlassSurfaceStyle.Navigation,
                        modifier = Modifier.testTag("in_app_browser_reload_button"),
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.Refresh,
                            contentDescription = stringResource(R.string.browser_action_reload),
                            tint = colors.content,
                        )
                    }
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlasmioSearchBar(
                    query = urlQuery,
                    onQueryChange = { urlQuery = it },
                    active = isEditingUrl,
                    onActiveChange = { isEditingUrl = it },
                    onClose = {
                        urlQuery = TextFieldValue(currentUrl)
                        isEditingUrl = false
                    },
                    onSearch = { entered -> navigateToInputUrl(entered) },
                    placeholder = stringResource(R.string.browser_url_hint),
                    closeContentDescription = stringResource(R.string.close),
                    controlHeight = 42.dp,
                    searchIconSize = 18.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("in_app_browser_url_input"),
                )

                GlasmioButton(
                    onClick = { navigateToInputUrl(urlQuery.text) },
                    emphasis = GlassEmphasis.Prominent,
                    navigationSurfaceColor = semantic.accentBlue,
                    size = GlasmioButtonSize.Small,
                    modifier = Modifier.testTag("in_app_browser_go_button"),
                ) {
                    GlasmioText(
                        text = stringResource(R.string.link_share_browse_go),
                        style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                }
            }

            AnimatedVisibility(
                visible = isLoading && !rendererCrashed,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                GlasmioLinearProgressBar(
                    progress = progress,
                    color = semantic.accentBlue,
                    height = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        // 2. Main WebView Viewport + Floating GlasmioBottomToolbar
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(colors.groupedBackground),
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
                key(webViewGeneration) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            WebView(activityContext).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                // On low-RAM devices (e.g. Redmi A Series), avoid heavy GPU layer caching in WebView
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
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: Bitmap?,
                                    ) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                        loadErrorDescription = null
                                        if (!url.isNullOrBlank()) {
                                            currentUrl = url
                                            if (!isEditingUrl) urlQuery = TextFieldValue(url)
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
                                            if (!isEditingUrl) urlQuery = TextFieldValue(url)
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

            // 3. Authentic Floating GlasmioBottomToolbar Dock
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                GlasmioBottomToolbar(
                    style = GlasmioBottomToolbarStyle.Symbols,
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .testTag("in_app_browser_bottom_toolbar"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlasmioIconButton(
                            onClick = { webViewRef?.goBack() },
                            enabled = canGoBack && !rendererCrashed,
                            style = GlassSurfaceStyle.Navigation,
                            size = 42.dp,
                            modifier = Modifier.testTag("in_app_browser_back_button"),
                        ) {
                            SfIcon(
                                systemName = ThazhSymbols.Back,
                                contentDescription = stringResource(R.string.browser_action_back),
                                tint = if (canGoBack && !rendererCrashed) colors.content else colors.secondaryContent.copy(alpha = 0.35f),
                                size = 18.dp,
                            )
                        }

                        GlasmioIconButton(
                            onClick = { webViewRef?.goForward() },
                            enabled = canGoForward && !rendererCrashed,
                            style = GlassSurfaceStyle.Navigation,
                            size = 42.dp,
                            modifier = Modifier.testTag("in_app_browser_forward_button"),
                        ) {
                            SfIcon(
                                systemName = ThazhSymbols.ChevronRight,
                                contentDescription = stringResource(R.string.browser_action_forward),
                                tint = if (canGoForward && !rendererCrashed) colors.content else colors.secondaryContent.copy(alpha = 0.35f),
                                size = 18.dp,
                            )
                        }

                        GlasmioIconButton(
                            onClick = {
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("URL", currentUrl))
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.link_share_copied_toast),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            style = GlassSurfaceStyle.Navigation,
                            size = 42.dp,
                            modifier = Modifier.testTag("in_app_browser_copy_button"),
                        ) {
                            SfIcon(
                                systemName = ThazhSymbols.Copy,
                                contentDescription = stringResource(R.string.browser_action_copy_url),
                                tint = colors.content,
                                size = 18.dp,
                            )
                        }

                        GlasmioIconButton(
                            onClick = {
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
                            style = GlassSurfaceStyle.Navigation,
                            size = 42.dp,
                            modifier = Modifier.testTag("in_app_browser_share_button"),
                        ) {
                            SfIcon(
                                systemName = ThazhSymbols.Share,
                                contentDescription = stringResource(R.string.browser_action_share),
                                tint = colors.content,
                                size = 18.dp,
                            )
                        }

                        GlasmioIconButton(
                            onClick = {
                                runCatching {
                                    activityContext.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)),
                                    )
                                }
                            },
                            style = GlassSurfaceStyle.Navigation,
                            size = 42.dp,
                            modifier = Modifier.testTag("in_app_browser_external_button"),
                        ) {
                            SfIcon(
                                systemName = ThazhSymbols.OpenInNew,
                                contentDescription = stringResource(R.string.browser_action_open_external),
                                tint = colors.accent,
                                size = 18.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}
