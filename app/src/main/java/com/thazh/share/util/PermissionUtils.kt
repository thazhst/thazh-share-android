package com.thazh.share.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class OnboardingPermissionStep {
    WIFI_DIRECT,
    BLUETOOTH_BLE,
    NOTIFICATIONS,
}

object PermissionUtils {

    fun permissionsForStep(step: OnboardingPermissionStep): Array<String> {
        return when (step) {
            OnboardingPermissionStep.WIFI_DIRECT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
                } else {
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            OnboardingPermissionStep.BLUETOOTH_BLE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT,
                    )
                } else {
                    emptyArray()
                }
            }
            OnboardingPermissionStep.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    emptyArray()
                }
            }
        }
    }

    fun isStepGranted(context: Context, step: OnboardingPermissionStep): Boolean {
        val perms = permissionsForStep(step)
        if (perms.isEmpty()) return true
        return perms.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun allSteps(): List<OnboardingPermissionStep> = OnboardingPermissionStep.entries

    fun firstUngrantedStepIndex(context: Context): Int {
        val steps = allSteps()
        val index = steps.indexOfFirst { !isStepGranted(context, it) }
        return if (index >= 0) index else steps.lastIndex
    }

    fun requiredPermissions(): Array<String> {
        return allSteps()
            .flatMap { permissionsForStep(it).toList() }
            .distinct()
            .toTypedArray()
    }

    fun hasAllRequiredPermissions(context: Context): Boolean {
        return allSteps().all { step -> isStepGranted(context, step) }
    }

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
