package com.genesiscruz.adwarehuli.domain.model

data class AppBatteryInfo(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val foregroundTimeMs: Long,
    val impactScore: Int,
    val band: BatteryBand,
    val reasons: List<BatteryReason>
)
