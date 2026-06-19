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
}
