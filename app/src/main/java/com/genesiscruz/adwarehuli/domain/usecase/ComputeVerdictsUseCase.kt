package com.genesiscruz.adwarehuli.domain.usecase

import com.genesiscruz.adwarehuli.domain.Constants
import com.genesiscruz.adwarehuli.domain.model.AppRiskInfo
import com.genesiscruz.adwarehuli.domain.model.CulpritTally
import com.genesiscruz.adwarehuli.domain.model.CulpritVerdict
import com.genesiscruz.adwarehuli.domain.model.RiskReason

/**
 * Cross-references redirect leaderboard data with risk-scan data to decide
 * which apps are CONFIRMED CULPRITs.
 *
 * An app is confirmed when it has >=1 redirect event AND a high-weight risk
 * signal (overlay / accessibility / hidden / sideloaded).
 */
class ComputeVerdictsUseCase {

    operator fun invoke(
        leaderboard: List<CulpritTally>,
        riskByPackage: Map<String, AppRiskInfo>
    ): List<CulpritVerdict> {
        return leaderboard.map { tally ->
            val risk = riskByPackage[tally.suspectPackage]
            val isHighRisk = risk != null && risk.reasons.any { it.weight in Constants.HIGH_RISK_SIGNAL_WEIGHTS }
            val isConfirmed = tally.redirectCount >= Constants.CONFIRMED_CULPRIT_MIN_REDIRECTS && isHighRisk

            CulpritVerdict(
                packageName = tally.suspectPackage,
                label = risk?.label ?: tally.suspectPackage,
                redirectCount = tally.redirectCount,
                lastSeen = tally.lastSeen,
                riskInfo = risk,
                isConfirmed = isConfirmed,
                explanation = buildExplanation(tally.redirectCount, risk?.reasons.orEmpty())
            )
        }.sortedWith(compareByDescending<CulpritVerdict> { it.isConfirmed }.thenByDescending { it.redirectCount })
    }

    private fun buildExplanation(redirectCount: Int, reasons: List<RiskReason>): String {
        val topReason = reasons.firstOrNull { it.weight in Constants.HIGH_RISK_SIGNAL_WEIGHTS }
        val reasonPhrase = when (topReason) {
            RiskReason.OVERLAY -> "can draw over other apps"
            RiskReason.ACCESSIBILITY -> "has accessibility access"
            RiskReason.HIDDEN_APP -> "is hidden from the app list"
            RiskReason.SIDELOADED -> "was sideloaded from an unknown source"
            else -> "shows other risky behavior"
        }
        return "Triggered the browser ${redirectCount}x and $reasonPhrase — likely the source of the ads."
    }
}
