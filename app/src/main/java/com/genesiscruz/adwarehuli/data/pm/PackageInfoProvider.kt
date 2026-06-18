package com.genesiscruz.adwarehuli.data.pm

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Caches app labels and icons so list/detail screens don't repeatedly hit
 * PackageManager (which involves disk I/O) on the main thread.
 */
class PackageInfoProvider(context: Context) {

    private val packageManager: PackageManager = context.applicationContext.packageManager
    private val labelCache = LruCache<String, String>(500)
    private val iconCache = LruCache<String, Drawable>(200)

    suspend fun getLabel(packageName: String): String = withContext(Dispatchers.IO) {
        labelCache.get(packageName) ?: run {
            val label = try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                packageName
            }
            labelCache.put(packageName, label)
            label
        }
    }

    suspend fun getIcon(packageName: String): Drawable? = withContext(Dispatchers.IO) {
        iconCache.get(packageName) ?: try {
            val icon = packageManager.getApplicationIcon(packageName)
            iconCache.put(packageName, icon)
            icon
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun invalidate(packageName: String) {
        labelCache.remove(packageName)
        iconCache.remove(packageName)
    }
}
