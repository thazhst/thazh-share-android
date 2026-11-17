package com.thazh.share.ui

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.ShareApplication
import com.thazh.share.data.AppThemeMode
import com.thazh.share.data.IncomingConnectionRequest
import com.thazh.share.service.WifiDirectService
import com.thazh.share.ui.theme.ThazhIconResolver
import com.thazh.share.ui.theme.ThazhShareTheme
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.render.backdrops.emptyBackdrop
import io.github.neoruaa.glasmio.icons.GlasmioIconProvider
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioAlertButton
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertSurface

/**
 * Translucent dialog-styled Activity presented when the user is in another app
 * and interacts with an incoming connection prompt while device visibility is active.
 */
class IncomingConnectionActivity : ComponentActivity() {

    private val serviceState = mutableStateOf<WifiDirectService?>(null)
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? WifiDirectService.LocalBinder ?: return
            serviceState.value = localBinder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceState.value = null
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ShareApplication
        WifiDirectService.bind(this, serviceConnection)
        isBound = true

        setContent {
            val themeMode by app.preferencesRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)
            val reduceTransparency by app.preferencesRepository.reduceTransparency
                .collectAsStateWithLifecycle(initialValue = false)
            val liquidGlassEnabled by app.preferencesRepository.liquidGlassEnabled
                .collectAsStateWithLifecycle(initialValue = true)
            val darkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            val boundService by serviceState
            val request = boundService?.incomingConnectionRequest
                ?.collectAsStateWithLifecycle(initialValue = null)?.value

            var hadRequest by remember { mutableStateOf(false) }
            LaunchedEffect(boundService, request) {
                if (request != null) {
                    hadRequest = true
                } else if (boundService != null && hadRequest) {
                    finish()
                }
            }

            GlasmioIconProvider(resolver = ThazhIconResolver) {
                ThazhShareTheme(
                    darkTheme = darkTheme,
                    reduceTransparency = reduceTransparency,
                    liquidGlassEnabled = liquidGlassEnabled,
                ) {
                    IncomingConnectionOverlayDialog(
                        request = request,
                        onAccept = {
                            val peer = request?.peerName.orEmpty()
                            boundService?.acceptIncomingConnection()
                            if (peer.isNotBlank()) {
                                Toast.makeText(
                                    this@IncomingConnectionActivity,
                                    getString(R.string.incoming_conn_accepted_toast, peer),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            finish()
                        },
                        onDecline = {
                            boundService?.declineIncomingConnection()
                            finish()
                        },
                        onDismiss = {
                            finish()
                        },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        if (isBound) {
            runCatching { unbindService(serviceConnection) }
            isBound = false
        }
        super.onDestroy()
    }
}

@Composable
private fun IncomingConnectionOverlayDialog(
    request: IncomingConnectionRequest?,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val badgeShape = ContinuousRoundedRectangle(12.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (request == null) return@Box

        val message = if (request.pendingFileCount > 0) {
            stringResource(R.string.incoming_conn_message_files, request.peerName, request.pendingFileCount)
        } else {
            stringResource(R.string.incoming_conn_message, request.peerName)
        }

        GlasmioAlertSurface(
            backdrop = emptyBackdrop(),
            title = stringResource(R.string.incoming_conn_title),
            message = message,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .testTag("background_incoming_connection_dialog"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (request.peerAddress.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(badgeShape)
                            .background(
                                colors.secondaryContent.copy(alpha = if (colors.isDark) 0.14f else 0.08f),
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.Devices,
                            contentDescription = null,
                            tint = semantic.accentBlue,
                            size = 18.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.incoming_conn_device_address, request.peerAddress),
                            style = GlasmioTheme.textStyles.footnote,
                            color = colors.secondaryContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (request.pendingFileNames.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            R.string.incoming_conn_pending_files,
                            request.pendingFileCount,
                            request.pendingFileNames.take(3).joinToString(", "),
                        ),
                        style = GlasmioTheme.textStyles.footnote.copy(fontWeight = FontWeight.Medium),
                        color = colors.content,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlasmioAlertButton(
                    text = stringResource(R.string.incoming_conn_decline),
                    onClick = onDecline,
                    role = GlasmioAlertButtonRole.Destructive,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("background_incoming_decline_button"),
                )
                GlasmioAlertButton(
                    text = stringResource(R.string.incoming_conn_accept),
                    onClick = onAccept,
                    role = GlasmioAlertButtonRole.Default,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("background_incoming_accept_button"),
                )
            }
        }
    }
}
