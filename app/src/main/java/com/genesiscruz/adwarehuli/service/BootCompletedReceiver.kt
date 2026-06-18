package com.genesiscruz.adwarehuli.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.edit

/**
 * Resumes the Culprit Monitor after a reboot, but only if the technician
 * opted in via the "Resume After Restart" toggle on the Monitor screen.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (isResumeOnBootEnabled(context)) {
            CulpritMonitorService.start(context)
        }
    }

    companion object {
        private const val PREFS_NAME = "adwarehuli_prefs"
        private const val PREF_RESUME_ON_BOOT = "resume_monitor_on_boot"

        fun setResumeOnBoot(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putBoolean(PREF_RESUME_ON_BOOT, enabled)
            }
        }

        fun isResumeOnBootEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(PREF_RESUME_ON_BOOT, false)
        }
    }
}
