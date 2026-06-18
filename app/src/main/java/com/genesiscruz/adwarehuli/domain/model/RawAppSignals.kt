package com.genesiscruz.adwarehuli.domain.model

/**
 * Raw, Android-framework-derived facts about one installed app, gathered by
 * the data layer. Kept separate from [AppRiskInfo] so the scoring logic in
 * [com.genesiscruz.adwarehuli.domain.usecase.ComputeRiskScoreUseCase] is a
 * pure function over plain data and easy to unit test.
 */
data class RawAppSignals(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val requestsOverlay: Boolean,
    val hasAccessibilityService: Boolean,
    val isHiddenFromLauncher: Boolean,
    val isSideloaded: Boolean,
    val installSource: String?,
    val firstInstallTime: Long,
    val hasBootReceiver: Boolean,
    val hasAdNetworkPermCombo: Boolean
)
