package com.thazh.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.thazh.share.network.NfcConnectionManager
import com.thazh.share.ui.ShareApp
import com.thazh.share.ui.ShareViewModel
import com.thazh.share.ui.cupertino.CupertinoBackdropHost
import com.thazh.share.ui.screens.PermissionRationaleScreen
import com.thazh.share.ui.theme.ThazhShareTheme
import com.thazh.share.util.PermissionUtils

class MainActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels()
    private val nfcConnectionManager by lazy { NfcConnectionManager(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            ThazhShareTheme {
                CupertinoBackdropHost {
                    AppRoot(viewModel = viewModel)
                }
            }
        }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
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
            }
        )
    }
}
