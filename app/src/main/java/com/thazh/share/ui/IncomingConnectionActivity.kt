package com.thazh.share.ui

import android.content.ComponentName
import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.MainActivity
import com.thazh.share.R
import com.thazh.share.ShareApplication
import com.thazh.share.data.AppThemeMode
import com.thazh.share.data.DeviceVisibilityMode
import com.thazh.share.data.IncomingConnectionRequest
import com.thazh.share.service.WifiDirectService
import com.thazh.share.ui.components.GlasmioText
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
 * Translucent dialog-styled Activity presented over other apps when the user
 * receives an incoming file-transfer connection request while device visibility
 * (10 minutes or Unlimited) is active in Thazh Share.
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
            val visibilityMode = boundService?.visibilityMode
                ?.collectAsStateWithLifecycle(initialValue = DeviceVisibilityMode.TEMPORARY_10_MIN)?.value
                ?: DeviceVisibilityMode.TEMPORARY_10_MIN
            val remainingSeconds = boundService?.visibilityRemainingSeconds
                ?.collectAsStateWithLifecycle(initialValue = 600)?.value

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
                        visibilityMode = visibilityMode,
                        remainingSeconds = remainingSeconds,
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
                        onAcceptAndOpenApp = {
                            boundService?.acceptIncomingConnection()
                            runCatching {
                                startActivity(
                                    Intent(this@IncomingConnectionActivity, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    },
                                )
                            }
                            finish()
                        },
                        onDecline = {
                            boundService?.declineIncomingConnection()
                            Toast.makeText(
                                this@IncomingConnectionActivity,
                                getString(R.string.incoming_conn_declined_toast),
                                Toast.LENGTH_SHORT,
                            ).show()
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
    visibilityMode: DeviceVisibilityMode,
    remainingSeconds: Int?,
    onAccept: () -> Unit,
    onAcceptAndOpenApp: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val badgeShape = ContinuousRoundedRectangle(12.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.content.copy(alpha = if (colors.isDark) 0.36f else 0.24f))
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

        val visibilityBadgeColor = when (visibilityMode) {
            DeviceVisibilityMode.ALWAYS_VISIBLE -> semantic.accentGreen
            else -> semantic.accentBlue
        }
        val visibilityBadgeText = when (visibilityMode) {
            DeviceVisibilityMode.ALWAYS_VISIBLE -> stringResource(R.string.incoming_conn_visibility_always)
            else -> stringResource(
                R.string.incoming_conn_visibility_10min,
                WifiDirectService.formatCountdown(remainingSeconds ?: 600),
            )
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
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .clip(ContinuousCapsule)
                        .background(
                            visibilityBadgeColor.copy(alpha = if (colors.isDark) 0.22f else 0.14f),
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SfIcon(
                        systemName = if (visibilityMode == DeviceVisibilityMode.ALWAYS_VISIBLE) {
                            ThazhSymbols.AllInclusive
                        } else {
                            ThazhSymbols.Timer
                        },
                        contentDescription = null,
                        tint = visibilityBadgeColor,
                        size = 14.dp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    GlasmioText(
                        text = visibilityBadgeText,
                        style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                        color = visibilityBadgeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

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
                        GlasmioText(
                            text = stringResource(R.string.incoming_conn_device_address, request.peerAddress),
                            style = GlasmioTheme.textStyles.footnote,
                            color = colors.secondaryContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (request.pendingFileNames.isNotEmpty()) {
                    GlasmioText(
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(badgeShape)
                        .clickable(role = Role.Button, onClick = onAcceptAndOpenApp)
                        .padding(vertical = 6.dp)
                        .testTag("background_incoming_accept_open_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    GlasmioText(
                        text = stringResource(R.string.incoming_conn_accept_open),
                        style = GlasmioTheme.textStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                        color = semantic.accentBlue,
                        textAlign = TextAlign.Center,
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
