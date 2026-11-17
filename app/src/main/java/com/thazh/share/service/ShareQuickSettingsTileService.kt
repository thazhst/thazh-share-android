package com.thazh.share.service

import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.thazh.share.MainActivity
import com.thazh.share.R
import com.thazh.share.ShareApplication
import com.thazh.share.data.DeviceVisibilityMode
import com.thazh.share.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ControlCenterTileAddResult {
    ADDED,
    ALREADY_ADDED,
    NOT_ADDED,
    ERROR,
}

class ShareQuickSettingsTileService : TileService() {

    private val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        if (!isServiceRunning) {
            tileScope.launch {
                val app = applicationContext as? ShareApplication
                val savedMode = runCatching {
                    app?.preferencesRepository?.defaultVisibilityMode?.first()
                }.getOrNull()
                if (savedMode != null && !isServiceRunning) {
                    currentVisibilityMode = savedMode
                }
                refreshTileUi()
            }
        } else {
            refreshTileUi()
        }
    }

    override fun onClick() {
        super.onClick()
        if (!PermissionUtils.hasAllRequiredPermissions(this)) {
            openMainActivityFromTile()
            return
        }

        val nextMode = when (currentVisibilityMode) {
            DeviceVisibilityMode.HIDDEN -> DeviceVisibilityMode.TEMPORARY_10_MIN
            DeviceVisibilityMode.TEMPORARY_10_MIN -> DeviceVisibilityMode.ALWAYS_VISIBLE
            DeviceVisibilityMode.ALWAYS_VISIBLE -> DeviceVisibilityMode.HIDDEN
        }
        currentVisibilityMode = nextMode
        refreshTileUi()

        val serviceIntent = Intent(this, WifiDirectService::class.java).apply {
            action = WifiDirectService.ACTION_SET_VISIBILITY_FROM_TILE
            putExtra(WifiDirectService.EXTRA_VISIBILITY_MODE, nextMode.storageKey)
        }
        runCatching {
            if (nextMode.isVisible) {
                ContextCompat.startForegroundService(this, serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }.onFailure {
            openMainActivityFromTile()
            return
        }

        val modeToast = when (nextMode) {
            DeviceVisibilityMode.HIDDEN -> getString(R.string.qs_tile_toast_hidden)
            DeviceVisibilityMode.TEMPORARY_10_MIN -> getString(R.string.qs_tile_toast_10min)
            DeviceVisibilityMode.ALWAYS_VISIBLE -> getString(R.string.qs_tile_toast_always)
        }
        Toast.makeText(applicationContext, modeToast, Toast.LENGTH_SHORT).show()
    }

    private fun openMainActivityFromTile() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(launchIntent)
        }
    }

    private fun refreshTileUi() {
        val tile = qsTile ?: return
        val hasPermissions = PermissionUtils.hasAllRequiredPermissions(this)
        val mode = currentVisibilityMode

        tile.label = getString(R.string.qs_tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_notification)

        if (!hasPermissions) {
            tile.state = Tile.STATE_INACTIVE
            tile.subtitle = getString(R.string.qs_tile_subtitle_setup)
        } else {
            when (mode) {
                DeviceVisibilityMode.HIDDEN -> {
                    tile.state = Tile.STATE_INACTIVE
                    tile.subtitle = getString(R.string.visibility_mode_hidden)
                }
                DeviceVisibilityMode.TEMPORARY_10_MIN -> {
                    tile.state = Tile.STATE_ACTIVE
                    tile.subtitle = getString(R.string.visibility_mode_10min)
                }
                DeviceVisibilityMode.ALWAYS_VISIBLE -> {
                    tile.state = Tile.STATE_ACTIVE
                    tile.subtitle = getString(R.string.visibility_mode_always)
                }
            }
        }
        runCatching { tile.updateTile() }
    }

    companion object {
        @Volatile
        var currentVisibilityMode: DeviceVisibilityMode = DeviceVisibilityMode.TEMPORARY_10_MIN

        @Volatile
        var isServiceRunning: Boolean = false

        fun notifyVisibilityChanged(
            context: Context,
            mode: DeviceVisibilityMode,
            running: Boolean = true,
        ) {
            currentVisibilityMode = mode
            isServiceRunning = running
            runCatching {
                requestListeningState(
                    context.applicationContext,
                    ComponentName(context.applicationContext, ShareQuickSettingsTileService::class.java),
                )
            }
        }

        fun requestAddControlCenterTile(
            context: Context,
            onResult: (ControlCenterTileAddResult) -> Unit,
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                onResult(ControlCenterTileAddResult.ERROR)
                return
            }
            val statusBarManager = context.getSystemService(StatusBarManager::class.java)
            if (statusBarManager == null) {
                onResult(ControlCenterTileAddResult.ERROR)
                return
            }
            runCatching {
                statusBarManager.requestAddTileService(
                    ComponentName(context.applicationContext, ShareQuickSettingsTileService::class.java),
                    context.getString(R.string.qs_tile_label),
                    Icon.createWithResource(context, R.drawable.ic_notification),
                    ContextCompat.getMainExecutor(context),
                ) { resultCode ->
                    val mapped = when (resultCode) {
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> ControlCenterTileAddResult.ADDED
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> ControlCenterTileAddResult.ALREADY_ADDED
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> ControlCenterTileAddResult.NOT_ADDED
                        else -> ControlCenterTileAddResult.ERROR
                    }
                    onResult(mapped)
                }
            }.onFailure {
                onResult(ControlCenterTileAddResult.ERROR)
            }
        }
    }
}
