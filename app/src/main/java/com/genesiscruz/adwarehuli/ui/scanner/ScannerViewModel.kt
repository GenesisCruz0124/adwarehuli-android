package com.genesiscruz.adwarehuli.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesiscruz.adwarehuli.data.repository.AppRiskRepository
import com.genesiscruz.adwarehuli.domain.model.AppRiskInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScannerUiState(
    val isScanning: Boolean = false,
    val showSystemApps: Boolean = false,
    val allApps: List<AppRiskInfo> = emptyList()
) {
    val visibleApps: List<AppRiskInfo>
        get() = allApps
            .filter { showSystemApps || !it.isSystemApp }
            .sortedByDescending { it.riskScore }
}

class ScannerViewModel(private val repository: AppRiskRepository) : ViewModel() {

    private val isScanning = MutableStateFlow(false)
    private val showSystemApps = MutableStateFlow(false)

    val state: StateFlow<ScannerUiState> = combine(
        isScanning,
        showSystemApps,
        repository.observeAll()
    ) { scanning, showSystem, apps ->
        ScannerUiState(scanning, showSystem, apps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScannerUiState())

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
