package com.genesiscruz.adwarehuli.domain.usecase

import com.genesiscruz.adwarehuli.domain.Constants
import com.genesiscruz.adwarehuli.domain.model.AppRiskInfo
import com.genesiscruz.adwarehuli.domain.model.CulpritTally
import com.genesiscruz.adwarehuli.domain.model.CulpritVerdict
import com.genesiscruz.adwarehuli.domain.model.DomainCategory
import com.genesiscruz.adwarehuli.domain.model.DomainRedirectCorrelation
import com.genesiscruz.adwarehuli.domain.model.RiskReason

/**
 * Cross-references redirect leaderboard data with risk-scan data (Phase 1)
 * and, when available, Domain Monitor correlations (Phase 2) to decide
 * which apps are CONFIRMED CULPRITs.
 *
 * An app is confirmed when EITHER:
 *  - it has >=1 redirect event AND a high-weight risk signal (overlay /
 *    accessibility / hidden / sideloaded), OR
 *  - a flagged domain lookup was observed within [Constants.CORRELATION_WINDOW_MS]
 *    of one of its redirects — direct evidence that's treated as confirming
 *    on its own.
 */
class ComputeVerdictsUseCase {

    operator fun invoke(
        leaderboard: List<CulpritTally>,
        riskByPackage: Map<String, AppRiskInfo>,
        latestCorrelationByPackage: Map<String, DomainRedirectCorrelation> = emptyMap()
    ): List<CulpritVerdict> {
        return leaderboard.map { tally ->
            val risk = riskByPackage[tally.suspectPackage]
            val correlation = latestCorrelationByPackage[tally.suspectPackage]
            val isHighRisk = risk != null && risk.reasons.any { it.weight in Constants.HIGH_RISK_SIGNAL_WEIGHTS }
            val isConfirmedByRisk = tally.redirectCount >= Constants.CONFIRMED_CULPRIT_MIN_REDIRECTS && isHighRisk
            val isConfirmed = isConfirmedByRisk || correlation != null

            CulpritVerdict(
                packageName = tally.suspectPackage,
                label = risk?.label ?: tally.suspectPackage,
                redirectCount = tally.redirectCount,
                lastSeen = tally.lastSeen,
                riskInfo = risk,
                isConfirmed = isConfirmed,
                explanation = buildExplanation(tally.redirectCount, risk?.reasons.orEmpty(), correlation),
                domainCorrelation = correlation
            )
        }.sortedWith(compareByDescending<CulpritVerdict> { it.isConfirmed }.thenByDescending { it.redirectCount })
    }

    private fun buildExplanation(
        redirectCount: Int,
        reasons: List<RiskReason>,
        correlation: DomainRedirectCorrelation?
    ): String {
        if (correlation != null) {
            val seconds = "%.1f".format(correlation.gapMs / 1000f)
            val category = categoryPhrase(correlation.category)
            return "Looked up ${correlation.domain} ($category) ${seconds}s before the browser opened — confirmed source of the ads."
        }
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

    private fun categoryPhrase(category: DomainCategory): String = when (category) {
        DomainCategory.GAMBLING -> "gambling"
        DomainCategory.AD_NETWORK -> "ad network"
        DomainCategory.MALVERTISING -> "malvertising"
        DomainCategory.UNCATEGORIZED -> "flagged"
    }
}
