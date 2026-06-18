package com.genesiscruz.adwarehuli.domain.usecase

import com.genesiscruz.adwarehuli.domain.Constants
import com.genesiscruz.adwarehuli.domain.model.AppRiskInfo
import com.genesiscruz.adwarehuli.domain.model.RawAppSignals
import com.genesiscruz.adwarehuli.domain.model.RiskBand
import com.genesiscruz.adwarehuli.domain.model.RiskReason
import java.util.concurrent.TimeUnit

/**
 * Pure scoring function: turns raw per-app facts into a weighted score, a
 * band, and the ordered list of reasons that contributed to it.
 */
class ComputeRiskScoreUseCase {

    operator fun invoke(signals: RawAppSignals, now: Long = System.currentTimeMillis()): AppRiskInfo {
        val reasons = mutableListOf<RiskReason>()

        if (signals.isHiddenFromLauncher) reasons += RiskReason.HIDDEN_APP
        if (signals.requestsOverlay) reasons += RiskReason.OVERLAY
        if (signals.hasAccessibilityService) reasons += RiskReason.ACCESSIBILITY
        if (signals.isSideloaded) reasons += RiskReason.SIDELOADED

        val installAgeMs = now - signals.firstInstallTime
        val recentInstallDaysMs = TimeUnit.DAYS.toMillis(Constants.RECENT_INSTALL_DAYS.toLong())
        if (installAgeMs in 0..recentInstallDaysMs) reasons += RiskReason.RECENT_INSTALL

        if (signals.hasBootReceiver) reasons += RiskReason.BOOT_AUTOSTART
        if (signals.hasAdNetworkPermCombo) reasons += RiskReason.AD_NETWORK_PERMS

        reasons.sortByDescending { it.weight }
        val score = reasons.sumOf { it.weight }.coerceAtMost(100)
        val band = when {
            score >= Constants.RISK_BAND_RED_THRESHOLD -> RiskBand.RED
            score >= Constants.RISK_BAND_YELLOW_THRESHOLD -> RiskBand.YELLOW
            else -> RiskBand.GREEN
        }

        return AppRiskInfo(
            packageName = signals.packageName,
            label = signals.label,
            isSystemApp = signals.isSystemApp,
            riskScore = score,
            band = band,
            reasons = reasons,
            installSource = signals.installSource,
            firstInstallTime = signals.firstInstallTime,
            isHidden = signals.isHiddenFromLauncher,
            lastScanned = now
        )
    }
}
