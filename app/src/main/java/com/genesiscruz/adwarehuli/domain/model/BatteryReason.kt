package com.genesiscruz.adwarehuli.domain.model

import com.genesiscruz.adwarehuli.domain.Constants

/**
 * A single contributing factor to an app's battery-impact score. Weights are
 * sourced from [Constants.BatteryImpactWeights] so tuning stays in one place.
 */
enum class BatteryReason(val weight: Int) {
    HIGH_FOREGROUND_TIME(Constants.BatteryImpactWeights.HIGH_FOREGROUND_TIME),
    NOT_OPTIMIZED(Constants.BatteryImpactWeights.NOT_OPTIMIZED),
    BOOT_AUTOSTART(Constants.BatteryImpactWeights.BOOT_AUTOSTART)
}
