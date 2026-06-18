package com.genesiscruz.adwarehuli.data.pm

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.genesiscruz.adwarehuli.domain.Constants

/**
 * Identifies browser / Custom-Tab host packages: the known-package allowlist
 * plus a heuristic for anything else that can resolve an http(s) ACTION_VIEW
 * intent (covers OEM browsers and custom-tab providers we don't know by name).
 */
class BrowserDetector(context: Context) {

    private val packageManager: PackageManager = context.applicationContext.packageManager
    private val resolvedBrowsers: Set<String> by lazy { resolveBrowserPackages() }

    fun isBrowser(packageName: String): Boolean {
        return packageName in Constants.KNOWN_BROWSER_PACKAGES || packageName in resolvedBrowsers
    }

    private fun resolveBrowserPackages(): Set<String> {
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        val resolveInfos = packageManager.queryIntentActivities(viewIntent, PackageManager.MATCH_ALL)
        return resolveInfos.map { it.activityInfo.packageName }.toSet()
    }
}
