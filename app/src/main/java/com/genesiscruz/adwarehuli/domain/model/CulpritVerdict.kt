package com.genesiscruz.adwarehuli.domain.model

/**
 * Cross-referenced result combining Culprit Monitor evidence with Risk Scanner
 * evidence for a single package.
 */
data class CulpritVerdict(
    val packageName: String,
    val label: String,
    val redirectCount: Int,
    val lastSeen: Long,
    val riskInfo: AppRiskInfo?,
    val isConfirmed: Boolean,
    val explanation: String
)
