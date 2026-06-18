package com.genesiscruz.adwarehuli.ui.components

import com.genesiscruz.adwarehuli.R
import com.genesiscruz.adwarehuli.domain.model.RiskReason

fun RiskReason.labelRes(): Int = when (this) {
    RiskReason.HIDDEN_APP -> R.string.reason_hidden
    RiskReason.OVERLAY -> R.string.reason_overlay
    RiskReason.ACCESSIBILITY -> R.string.reason_accessibility
    RiskReason.SIDELOADED -> R.string.reason_sideloaded
    RiskReason.RECENT_INSTALL -> R.string.reason_recent_install
    RiskReason.BOOT_AUTOSTART -> R.string.reason_boot_autostart
    RiskReason.AD_NETWORK_PERMS -> R.string.reason_ad_network_perms
}
