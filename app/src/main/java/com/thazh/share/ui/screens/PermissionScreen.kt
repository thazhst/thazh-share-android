package com.thazh.share.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thazh.share.R
import com.thazh.share.ui.cupertino.CupertinoIcon
import com.thazh.share.ui.cupertino.CupertinoText
import com.thazh.share.ui.cupertino.LiquidButton
import com.thazh.share.ui.cupertino.LiquidCard
import com.thazh.share.ui.theme.CupertinoTheme
import com.thazh.share.util.PermissionUtils

@Composable
fun PermissionRationaleScreen(
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CupertinoTheme.colors
    var deniedOnce by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (PermissionUtils.hasAllRequiredPermissions(context)) {
            onPermissionsGranted()
        } else {
            deniedOnce = true
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "perm_pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidCard(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(116.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = this.center
                        val maxR = size.minDimension / 2f
                        for (ring in 0..1) {
                            val phase = (pulse + ring * 0.5f) % 1f
                            val radius = maxR * (0.42f + 0.58f * phase)
                            val alpha = (1f - phase) * 0.35f
                            drawCircle(
                                color = colors.accentBlue.copy(alpha = alpha),
                                radius = radius,
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        drawCircle(
                            color = colors.accentBlue.copy(alpha = 0.14f),
                            radius = maxR * 0.44f,
                            center = center
                        )
                    }
                    CupertinoIcon(
                        imageVector = Icons.Rounded.WifiTethering,
                        contentDescription = null,
                        tint = colors.accentBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                CupertinoText(
                    text = stringResource(R.string.permission_required_title),
                    style = CupertinoTheme.typography.title1,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                CupertinoText(
                    text = stringResource(R.string.permission_required_body),
                    style = CupertinoTheme.typography.subheadline,
                    color = colors.secondaryLabel,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                LiquidButton(
                    onClick = {
                        launcher.launch(PermissionUtils.requiredPermissions())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_permission_button")
                ) {
                    CupertinoText(
                        text = stringResource(R.string.permission_grant),
                        style = CupertinoTheme.typography.headline,
                        color = Color.White
                    )
                }

                if (deniedOnce) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LiquidButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_app_settings_button"),
                        tint = Color.Unspecified,
                        surfaceColor = colors.fillTertiary,
                        contentColor = colors.accentBlue
                    ) {
                        CupertinoText(
                            text = stringResource(R.string.permission_open_settings),
                            style = CupertinoTheme.typography.headline.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.accentBlue
                        )
                    }
                }
            }
        }
    }
}
