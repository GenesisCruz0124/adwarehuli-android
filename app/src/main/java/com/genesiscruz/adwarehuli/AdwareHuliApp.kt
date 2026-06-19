package com.genesiscruz.adwarehuli

import android.app.Application

class AdwareHuliApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        installCrashLogger()
    }

    /**
     * There's no adb access to a tester's physical device, so the only way to
     * see why a crash happened is to persist it ourselves and surface it in
     * the UI (see CrashLog.kt) for the user to screenshot.
     */
    private fun installCrashLogger() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { CrashLog.record(this, throwable) }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
