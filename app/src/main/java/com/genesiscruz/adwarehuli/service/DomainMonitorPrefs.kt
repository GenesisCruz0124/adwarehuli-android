package com.genesiscruz.adwarehuli.service

import android.content.Context
import androidx.core.content.edit

/** Small SharedPreferences-backed toggle store for the Domain Monitor's optional blocking mode. */
object DomainMonitorPrefs {
    private const val PREFS_NAME = "adwarehuli_prefs"
    private const val PREF_BLOCKING_ENABLED = "domain_blocking_enabled"

    fun isBlockingEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_BLOCKING_ENABLED, com.genesiscruz.adwarehuli.domain.Constants.DOMAIN_BLOCKING_DEFAULT_ENABLED)
    }

    fun setBlockingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(PREF_BLOCKING_ENABLED, enabled)
        }
    }
}
