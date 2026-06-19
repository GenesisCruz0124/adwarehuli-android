package com.genesiscruz.adwarehuli.data.net

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import java.net.InetSocketAddress

data class AppAttribution(val uid: Int, val packageName: String)

/**
 * Maps a captured UDP flow to the app that owns it via
 * [ConnectivityManager.getConnectionOwnerUid], which requires API 29+.
 * On older devices we can't attribute reliably, so callers should treat a
 * null result as "unknown app" rather than failing the whole capture.
 */
class ConnectionAttributor(context: Context) {

    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val packageManager = appContext.packageManager

    fun attribute(protocol: Int, local: InetSocketAddress, remote: InetSocketAddress): AppAttribution? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val uid = try {
            connectivityManager.getConnectionOwnerUid(protocol, local, remote)
        } catch (e: SecurityException) {
            return null
        }
        if (uid <= 0 || uid == Process.INVALID_UID) return null

        val packageName = try {
            packageManager.getPackagesForUid(uid)?.firstOrNull()
        } catch (e: SecurityException) {
            null
        } ?: return AppAttribution(uid, UNKNOWN_PACKAGE)

        return AppAttribution(uid, packageName)
    }

    companion object {
        const val UNKNOWN_PACKAGE = "unknown"
    }
}
