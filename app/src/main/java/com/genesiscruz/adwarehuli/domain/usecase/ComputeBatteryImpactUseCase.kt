package com.genesiscruz.adwarehuli.domain.usecase

import com.genesiscruz.adwarehuli.domain.Constants
import com.genesiscruz.adwarehuli.domain.model.AppBatteryInfo
import com.genesiscruz.adwarehuli.domain.model.BatteryBand
import com.genesiscruz.adwarehuli.domain.model.BatteryReason
import com.genesiscruz.adwarehuli.domain.model.RawBatterySignals
import java.util.concurrent.TimeUnit

/**
 * Pure scoring function: turns raw per-app facts into a weighted
 * battery-impact score, a band, and the ordered list of reasons that
 * contributed to it.
 */
class ComputeBatteryImpactUseCase {

    operator fun invoke(signals: RawBatterySignals): AppBatteryInfo {
        val reasons = mutableListOf<BatteryReason>()

        val highForegroundThresholdMs = TimeUnit.MINUTES.toMillis(Constants.BATTERY_HIGH_FOREGROUND_MINUTES)
        if (signals.foregroundTimeMs >= highForegroundThresholdMs) reasons += BatteryReason.HIGH_FOREGROUND_TIME
        if (!signals.isIgnoringBatteryOptimizations) reasons += BatteryReason.NOT_OPTIMIZED
        if (signals.hasBootReceiver) reasons += BatteryReason.BOOT_AUTOSTART

        reasons.sortByDescending { it.weight }
        val score = reasons.sumOf { it.weight }.coerceAtMost(100)
        val band = when {
            score >= Constants.BATTERY_BAND_HIGH_THRESHOLD -> BatteryBand.HIGH
            score >= Constants.BATTERY_BAND_MEDIUM_THRESHOLD -> BatteryBand.MEDIUM
            else -> BatteryBand.LOW
        }

        return AppBatteryInfo(
            packageName = signals.packageName,
            label = signals.label,
            isSystemApp = signals.isSystemApp,
            foregroundTimeMs = signals.foregroundTimeMs,
            impactScore = score,
            band = band,
            reasons = reasons
        )
    }
}
