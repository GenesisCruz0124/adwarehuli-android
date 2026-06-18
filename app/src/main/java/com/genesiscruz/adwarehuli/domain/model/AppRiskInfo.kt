package com.genesiscruz.adwarehuli.domain.model

data class AppRiskInfo(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val riskScore: Int,
    val band: RiskBand,
    val reasons: List<RiskReason>,
    val installSource: String?,
    val firstInstallTime: Long,
    val isHidden: Boolean,
    val lastScanned: Long
)
