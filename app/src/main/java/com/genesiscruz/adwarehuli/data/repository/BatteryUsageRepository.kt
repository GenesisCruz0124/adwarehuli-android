package com.genesiscruz.adwarehuli.data.repository

import com.genesiscruz.adwarehuli.data.pm.BatteryUsageScanner
import com.genesiscruz.adwarehuli.domain.model.AppBatteryInfo
import com.genesiscruz.adwarehuli.domain.usecase.ComputeBatteryImpactUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Battery impact is a live, on-demand scan rather than stored history, so
 * this just caches the last scan result in memory for the UI to observe.
 */
class BatteryUsageRepository(
    private val scanner: BatteryUsageScanner,
    private val computeBatteryImpact: ComputeBatteryImpactUseCase = ComputeBatteryImpactUseCase()
) {

    private val _lastScan = MutableStateFlow<List<AppBatteryInfo>>(emptyList())
    val lastScan: StateFlow<List<AppBatteryInfo>> = _lastScan.asStateFlow()

    suspend fun runScan(): List<AppBatteryInfo> {
        val results = scanner.scanAll().map { signals -> computeBatteryImpact(signals) }
        _lastScan.value = results
        return results
    }
}
