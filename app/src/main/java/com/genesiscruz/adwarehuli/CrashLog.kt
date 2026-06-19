package com.genesiscruz.adwarehuli

import android.content.Context

/**
 * Tiny crash-log persistence so a stack trace from a sideloaded build (no
 * adb access) can be read back and shown in-app instead of just disappearing
 * into the system's "app keeps stopping" dialog.
 */
object CrashLog {
    private const val PREFS = "crash_log"
    private const val KEY_LAST_CRASH = "last_crash"
    private const val KEY_BREADCRUMBS = "breadcrumbs"
    private const val MAX_BREADCRUMBS = 30

    fun record(context: Context, throwable: Throwable) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_CRASH, throwable.stackTraceToString())
            .apply()
    }

    fun lastCrash(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST_CRASH, null)

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_LAST_CRASH).apply()
    }

    /**
     * Synchronously committed (not apply()) so the write is durable on disk
     * before this call returns — needed to find the last step reached when
     * the process gets killed in a way that skips the uncaught-exception
     * handler entirely (e.g. a system-level foreground-service kill).
     */
    fun breadcrumb(context: Context, message: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_BREADCRUMBS, "").orEmpty()
        val updated = (existing.lines().filter { it.isNotEmpty() } + "${System.currentTimeMillis()}: $message")
            .takeLast(MAX_BREADCRUMBS)
            .joinToString("\n")
        prefs.edit().putString(KEY_BREADCRUMBS, updated).commit()
    }

    fun lastBreadcrumbs(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_BREADCRUMBS, null)
            ?.takeIf { it.isNotEmpty() }

    fun clearBreadcrumbs(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_BREADCRUMBS).commit()
    }
}
