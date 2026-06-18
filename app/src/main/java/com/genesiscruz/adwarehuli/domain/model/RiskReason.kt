package com.genesiscruz.adwarehuli.domain.model

import com.genesiscruz.adwarehuli.domain.Constants

/**
 * A single contributing factor to an app's risk score. Weights are sourced
 * from [Constants.RiskWeights] so tuning stays in one place.
 */
enum class RiskReason(val weight: Int) {
    HIDDEN_APP(Constants.RiskWeights.HIDDEN_APP),
    OVERLAY(Constants.RiskWeights.OVERLAY),
    ACCESSIBILITY(Constants.RiskWeights.ACCESSIBILITY),
    SIDELOADED(Constants.RiskWeights.SIDELOADED),
    RECENT_INSTALL(Constants.RiskWeights.RECENT_INSTALL),
    BOOT_AUTOSTART(Constants.RiskWeights.BOOT_AUTOSTART),
    AD_NETWORK_PERMS(Constants.RiskWeights.AD_NETWORK_PERMS)
}
