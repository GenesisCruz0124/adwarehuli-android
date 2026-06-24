package com.genesiscruz.adwarehuli.data.pm

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.PowerManager
import com.genesiscruz.adwarehuli.domain.Constants
import com.genesiscruz.adwarehuli.domain.model.RawBatterySignals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Estimates per-app battery impact from data a third-party app is actually
 * allowed to read. Real per-app mAh consumption requires the privileged
 * android.permission.BATTERY_STATS, which is signature|privileged-only and
 * unavailable here, so this uses foreground usage time (UsageStatsManager,
 * already granted via PACKAGE_USAGE_STATS) and battery-optimization
 * exemption (PowerManager) as a heuristic proxy.
 */
class BatteryUsageScanner(context: Context) {

    private val context = context.applicationContext
    private val packageManager: PackageManager = this.context.packageManager
    private val usageStatsManager = this.context.getSystemService(UsageStatsManager::class.java)
    private val powerManager = this.context.getSystemService(PowerManager::class.java)

    suspend fun scanAll(): List<RawBatterySignals> = withContext(Dispatchers.IO) {
        @Suppress("DEPRECATION")
        val packages: List<PackageInfo> = packageManager.getInstalledPackages(0)
        val foregroundTimeByPackage = queryForegroundTime()

        packages.map { pkgInfo -> toSignals(pkgInfo, foregroundTimeByPackage) }
    }

    private fun toSignals(
        pkgInfo: PackageInfo,
        foregroundTimeByPackage: Map<String, Long>
    ): RawBatterySignals {
        val appInfo = pkgInfo.applicationInfo
        val packageName = pkgInfo.packageName
        val isSystemApp = appInfo?.let { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: false

        return RawBatterySignals(
            packageName = packageName,
            label = appInfo?.let { packageManager.getApplicationLabel(it).toString() } ?: packageName,
            isSystemApp = isSystemApp,
            foregroundTimeMs = foregroundTimeByPackage[packageName] ?: 0L,
            isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(packageName) ?: true,
            hasBootReceiver = hasBootCompletedReceiver(packageName)
        )
    }

    private fun queryForegroundTime(): Map<String, Long> {
        val manager = usageStatsManager ?: return emptyMap()
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(Constants.BATTERY_USAGE_WINDOW_DAYS.toLong())
        val stats = manager.queryAndAggregateUsageStats(start, end)
        return stats.mapValues { (_, usageStats) -> usageStats.totalTimeInForeground }
    }

    private fun hasBootCompletedReceiver(packageName: String): Boolean {
        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED).setPackage(packageName)
        return packageManager.queryBroadcastReceivers(bootIntent, 0).isNotEmpty()
    }
}
