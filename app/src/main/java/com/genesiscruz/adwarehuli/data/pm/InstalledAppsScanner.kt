package com.genesiscruz.adwarehuli.data.pm

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.genesiscruz.adwarehuli.domain.model.RawAppSignals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val KNOWN_STORE_INSTALLER = "com.android.vending"

/**
 * Enumerates installed apps and extracts the raw signals the risk scorer
 * needs. All PackageManager work happens off the main thread.
 */
class InstalledAppsScanner(context: Context) {

    private val context = context.applicationContext
    private val packageManager: PackageManager = this.context.packageManager

    suspend fun scanAll(): List<RawAppSignals> = withContext(Dispatchers.IO) {
        val flags = PackageManager.GET_PERMISSIONS or
            PackageManager.GET_RECEIVERS or
            PackageManager.GET_SERVICES
        @Suppress("DEPRECATION")
        val packages: List<PackageInfo> = packageManager.getInstalledPackages(flags)

        packages.map { pkgInfo -> toSignals(pkgInfo) }
    }

    private fun toSignals(pkgInfo: PackageInfo): RawAppSignals {
        val appInfo = pkgInfo.applicationInfo
        val packageName = pkgInfo.packageName
        val isSystemApp = appInfo?.let { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: false

        val requestedPermissions = pkgInfo.requestedPermissions?.toList() ?: emptyList()
        val requestsOverlay = android.Manifest.permission.SYSTEM_ALERT_WINDOW in requestedPermissions
        val hasAccessibilityService = pkgInfo.services?.any { service ->
            service.permission == android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE
        } ?: false
        val hasBootReceiver = hasBootCompletedReceiver(packageName)

        val isHidden = !hasLauncherActivity(packageName)
        val installSource = resolveInstallSource(packageName)
        val isSideloaded = installSource != null && installSource != KNOWN_STORE_INSTALLER

        val hasAdNetworkPermCombo = listOf(
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_NETWORK_STATE
        ).all { it in requestedPermissions } &&
            (android.Manifest.permission.FOREGROUND_SERVICE in requestedPermissions)

        return RawAppSignals(
            packageName = packageName,
            label = appInfo?.let { packageManager.getApplicationLabel(it).toString() } ?: packageName,
            isSystemApp = isSystemApp,
            requestsOverlay = requestsOverlay,
            hasAccessibilityService = hasAccessibilityService,
            isHiddenFromLauncher = isHidden,
            isSideloaded = isSideloaded,
            installSource = installSource,
            firstInstallTime = pkgInfo.firstInstallTime,
            hasBootReceiver = hasBootReceiver,
            hasAdNetworkPermCombo = hasAdNetworkPermCombo
        )
    }

    private fun hasBootCompletedReceiver(packageName: String): Boolean {
        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED).setPackage(packageName)
        return packageManager.queryBroadcastReceivers(bootIntent, 0).isNotEmpty()
    }

    private fun hasLauncherActivity(packageName: String): Boolean {
        val launchIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(packageName)
        return packageManager.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL).isNotEmpty()
    }

    private fun resolveInstallSource(packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).initiatingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
