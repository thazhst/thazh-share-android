package com.thazh.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thazh.share.network.NfcConnectionManager
import com.thazh.share.ui.ShareApp
import com.thazh.share.ui.ShareViewModel
import com.thazh.share.ui.screens.PermissionRationaleScreen
import com.thazh.share.ui.theme.ThazhIconResolver
import com.thazh.share.ui.theme.ThazhShareTheme
import com.thazh.share.ui.theme.ThazhTheme
import com.thazh.share.util.PermissionUtils
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.icons.GlasmioIconProvider

class MainActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels()
    private val nfcConnectionManager by lazy { NfcConnectionManager(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val reduceTransparency by viewModel.reduceTransparency.collectAsStateWithLifecycle()
            val liquidGlassEnabled by viewModel.liquidGlassEnabled.collectAsStateWithLifecycle()

            GlasmioIconProvider(resolver = ThazhIconResolver) {
                ThazhShareTheme(
                    reduceTransparency = reduceTransparency,
                    liquidGlassEnabled = liquidGlassEnabled,
                ) {
                    GlasmioBackdropHost(
                        modifier = Modifier.fillMaxSize(),
                        sampledContent = { AmbientGlasmioBackground() },
                        overlayContent = { AppRoot(viewModel = viewModel) },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.setUiInForeground(true)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshNfcState()
        nfcConnectionManager.enableForegroundReader(this)
    }

    override fun onPause() {
        nfcConnectionManager.disableForegroundReader(this)
        super.onPause()
    }

    override fun onStop() {
        viewModel.setUiInForeground(false)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        viewModel.setUiInForeground(false)
        viewModel.unbindService(this)
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_RECEIVED_FILES, false) == true) {
            intent.removeExtra(EXTRA_OPEN_RECEIVED_FILES)
            viewModel.requestOpenReceivedFiles()
        }
    }

    companion object {
        const val EXTRA_OPEN_RECEIVED_FILES = "com.thazh.share.extra.OPEN_RECEIVED_FILES"
    }
}

@Composable
private fun AmbientGlasmioBackground() {
    val colors = GlasmioTheme.colors
    val appearance = GlasmioTheme.appearance
    val semantic = ThazhTheme.semantic

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.groupedBackground),
    ) {
        if (!appearance.reduceTransparency) {
            val topOrbAlpha = if (colors.isDark) 0.22f else 0.14f
            val midOrbAlpha = if (colors.isDark) 0.16f else 0.11f
            val bottomOrbAlpha = if (colors.isDark) 0.14f else 0.09f

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(semantic.accentBlue.copy(alpha = topOrbAlpha), Color.Transparent),
                        center = Offset(width * 0.16f, height * 0.14f),
                        radius = width * 0.68f,
                    ),
                    radius = width * 0.68f,
                    center = Offset(width * 0.16f, height * 0.14f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(semantic.accentCyan.copy(alpha = midOrbAlpha), Color.Transparent),
                        center = Offset(width * 0.88f, height * 0.36f),
                        radius = width * 0.64f,
                    ),
                    radius = width * 0.64f,
                    center = Offset(width * 0.88f, height * 0.36f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(semantic.accentGreen.copy(alpha = bottomOrbAlpha), Color.Transparent),
                        center = Offset(width * 0.42f, height * 0.88f),
                        radius = width * 0.72f,
                    ),
                    radius = width * 0.72f,
                    center = Offset(width * 0.42f, height * 0.88f),
                )
            }
        }
    }
}

@Composable
private fun AppRoot(viewModel: ShareViewModel) {
    val context = LocalContext.current
    var hasPermissions by remember {
        mutableStateOf(PermissionUtils.hasAllRequiredPermissions(context))
    }

    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            viewModel.bindService(context)
        }
    }

    if (hasPermissions) {
        ShareApp(viewModel = viewModel)
    } else {
        PermissionRationaleScreen(
            onPermissionsGranted = {
                hasPermissions = PermissionUtils.hasAllRequiredPermissions(context)
            },
        )
    }
}
