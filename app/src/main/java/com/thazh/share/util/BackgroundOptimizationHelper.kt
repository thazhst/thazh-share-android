package com.thazh.share.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Samsung (One UI), Oppo (ColorOS) and Xiaomi (MIUI/HyperOS) all layer their
 * own, more aggressive background-process/battery management on top of
 * stock Android Doze — a correctly-built foreground service (which this app
 * already uses for the P2P connection) can still get throttled or blocked
 * from auto-starting by these.
 *
 * There is no public, documented API for any of this: the OEM screens below
 * are reached through component names the Android developer community has
 * catalogued by testing (see dontkillmyapp.com) rather than anything Google
 * guarantees stable, and they can shift between OEM skin versions — this
 * was written with Samsung OneUI 3.1+, Oppo ColorOS 15+ and Xiaomi HyperOS
 * 3+ in mind, but isn't something that can be verified without those exact
 * devices. Every attempt here is wrapped and falls back, in order: next OEM
 * guess → the standard Android "ignore battery optimizations" request →
 * the app's own settings page, which always exists — so a stale component
 * name degrades to a slightly less direct path rather than doing nothing.
 */
object BackgroundOptimizationHelper {
    private const val TAG = "BackgroundOptHelper"

    enum class Oem { SAMSUNG, OPPO, XIAOMI, OTHER }

    fun detectOem(): Oem {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return when {
            manufacturer.contains("samsung") -> Oem.SAMSUNG
            manufacturer.contains("oppo") || brand.contains("oppo") -> Oem.OPPO
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
                brand.contains("redmi") || brand.contains("poco") -> Oem.XIAOMI
            else -> Oem.OTHER
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** The one part of this file that's a fully standard, stable Android API — identical on every OEM. */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "requestIgnoreBatteryOptimizations failed: ${e.message}")
            openAppSettingsFallback(context)
        }
    }

    /**
     * Tries this OEM's own autostart/background-permission screen first
     * (several candidate component names per OEM, since the exact one
     * varies by skin version), falling back to [requestIgnoreBatteryOptimizations]
     * and then the app's own settings page. Always ends by starting
     * *something* useful rather than silently doing nothing.
     */
    fun openAutostartSettings(context: Context) {
        val candidates: List<Intent> = when (detectOem()) {
            Oem.SAMSUNG -> listOf(
                componentIntent("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
                componentIntent("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")
            )
            Oem.XIAOMI -> listOf(
                Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT),
                componentIntent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            )
            Oem.OPPO -> listOf(
                componentIntent("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                componentIntent("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                componentIntent("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
            )
            Oem.OTHER -> emptyList()
        }

        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Log.w(TAG, "OEM autostart intent ${intent.component} failed: ${e.message}")
            }
        }

        // No OEM-specific screen resolved (unrecognized OEM, or the
        // component changed on this OS version) — fall back to the
        // standard, always-available path.
        requestIgnoreBatteryOptimizations(context)
    }

    private fun componentIntent(packageName: String, className: String): Intent =
        Intent().setComponent(ComponentName(packageName, className))

    private fun openAppSettingsFallback(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "openAppSettingsFallback failed: ${e.message}")
        }
    }
}
