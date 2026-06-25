package com.genesiscruz.adwarehuli.ui.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesiscruz.adwarehuli.data.repository.BatteryUsageRepository
import com.genesiscruz.adwarehuli.domain.model.AppBatteryInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BatteryUiState(
    val isScanning: Boolean = false,
    val showSystemApps: Boolean = false,
    val allApps: List<AppBatteryInfo> = emptyList()
) {
    val visibleApps: List<AppBatteryInfo>
        get() = allApps
            .filter { showSystemApps || !it.isSystemApp }
            .filter { it.foregroundTimeMs > 0L }
            .sortedByDescending { it.impactScore }
}

class BatteryViewModel(private val repository: BatteryUsageRepository) : ViewModel() {

    private val isScanning = MutableStateFlow(false)
    private val showSystemApps = MutableStateFlow(false)

    val state: StateFlow<BatteryUiState> = combine(
        isScanning,
        showSystemApps,
        repository.lastScan
    ) { scanning, showSystem, apps ->
        BatteryUiState(scanning, showSystem, apps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BatteryUiState())

    fun setShowSystemApps(show: Boolean) {
        showSystemApps.value = show
    }

    fun runScan() {
        if (isScanning.value) return
        viewModelScope.launch {
            isScanning.value = true
            try {
                repository.runScan()
            } finally {
                isScanning.value = false
            }
        }
    }
}
