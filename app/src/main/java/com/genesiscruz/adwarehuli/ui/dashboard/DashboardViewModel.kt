package com.genesiscruz.adwarehuli.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesiscruz.adwarehuli.data.repository.AppRiskRepository
import com.genesiscruz.adwarehuli.data.repository.RedirectEventRepository
import com.genesiscruz.adwarehuli.domain.model.CulpritVerdict
import com.genesiscruz.adwarehuli.domain.usecase.ComputeVerdictsUseCase
import com.genesiscruz.adwarehuli.service.CulpritMonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardState(
    val isMonitoring: Boolean = false,
    val lastScanTime: Long? = null,
    val verdicts: List<CulpritVerdict> = emptyList()
) {
    val confirmedCulprits: List<CulpritVerdict> get() = verdicts.filter { it.isConfirmed }
}

class DashboardViewModel(
    redirectEventRepository: RedirectEventRepository,
    appRiskRepository: AppRiskRepository,
    computeVerdicts: ComputeVerdictsUseCase
) : ViewModel() {

    val state: StateFlow<DashboardState> = combine(
        CulpritMonitorService.isRunning,
        appRiskRepository.observeLastScanTime(),
        redirectEventRepository.observeLeaderboard(),
        appRiskRepository.observeAll()
    ) { isRunning, lastScan, leaderboard, riskList ->
        val riskByPackage = riskList.associateBy { it.packageName }
        DashboardState(
            isMonitoring = isRunning,
            lastScanTime = lastScan,
            verdicts = computeVerdicts(leaderboard, riskByPackage)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())
}
