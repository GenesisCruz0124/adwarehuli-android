package com.genesiscruz.adwarehuli.domain.model

/**
 * Raw, Android-framework-derived facts used to estimate an app's battery
 * impact. Real per-app mAh consumption requires the privileged
 * android.permission.BATTERY_STATS, which is unavailable to third-party
 * apps, so this is a heuristic proxy built from data we can legitimately
 * read (UsageStatsManager, PowerManager).
 */
data class RawBatterySignals(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val foregroundTimeMs: Long,
    val isIgnoringBatteryOptimizations: Boolean,
    val hasBootReceiver: Boolean
)
