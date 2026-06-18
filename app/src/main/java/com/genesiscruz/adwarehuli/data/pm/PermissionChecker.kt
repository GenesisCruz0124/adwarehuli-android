package com.genesiscruz.adwarehuli.data.pm

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat

/**
 * Central place to ask "do we have permission X" without scattering
 * AppOpsManager / PackageManager calls across the UI layer.
 */
class PermissionChecker(private val context: Context) {

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasQueryAllPackages(): Boolean {
        // QUERY_ALL_PACKAGES only exists as a manifest permission from API 30 on.
        // Below that, package visibility is unrestricted, so treat it as granted.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        return context.checkSelfPermission(android.Manifest.permission.QUERY_ALL_PACKAGES) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun hasPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBootCompleted(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.RECEIVE_BOOT_COMPLETED) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun canScan(): Boolean = hasUsageAccess() && hasQueryAllPackages()
}
