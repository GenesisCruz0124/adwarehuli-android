package com.genesiscruz.adwarehuli.data.pm

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Identifies the home-screen launcher package(s) so a launcher -> browser
 * transition (the user tapping a bookmark) isn't mistaken for a redirect.
 */
class LauncherDetector(context: Context) {

    private val packageManager: PackageManager = context.applicationContext.packageManager
    private val launcherPackages: Set<String> by lazy { resolveLauncherPackages() }

    fun isLauncher(packageName: String): Boolean = packageName in launcherPackages

    private fun resolveLauncherPackages(): Set<String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfos = packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_ALL)
        return resolveInfos.map { it.activityInfo.packageName }.toSet()
    }
}
