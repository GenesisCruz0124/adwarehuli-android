package com.genesiscruz.adwarehuli.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesiscruz.adwarehuli.data.repository.AppRiskRepository
import com.genesiscruz.adwarehuli.data.repository.DomainHitRepository
import com.genesiscruz.adwarehuli.data.repository.RedirectEventRepository
import com.genesiscruz.adwarehuli.domain.model.AppRiskInfo
import com.genesiscruz.adwarehuli.domain.model.CulpritTally
import com.genesiscruz.adwarehuli.domain.model.CulpritVerdict
import com.genesiscruz.adwarehuli.domain.model.DomainRedirectCorrelation
import com.genesiscruz.adwarehuli.domain.usecase.ComputeVerdictsUseCase
import com.genesiscruz.adwarehuli.service.CulpritMonitorService
import com.genesiscruz.adwarehuli.service.DomainMonitorVpnService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardState(
    val isMonitoring: Boolean = false,
    val isDomainMonitoring: Boolean = false,
    val lastScanTime: Long? = null,
    val verdicts: List<CulpritVerdict> = emptyList()
) {
    val confirmedCulprits: List<CulpritVerdict> get() = verdicts.filter { it.isConfirmed }
}

private data class MonitorAndRiskData(
    val isRunning: Boolean,
    val isDomainMonitoring: Boolean,
    val lastScan: Long?,
    val leaderboard: List<CulpritTally>,
    val riskList: List<AppRiskInfo>
)

class DashboardViewModel(
    redirectEventRepository: RedirectEventRepository,
    appRiskRepository: AppRiskRepository,
    domainHitRepository: DomainHitRepository,
    computeVerdicts: ComputeVerdictsUseCase
) : ViewModel() {

    private val monitorAndRiskData = combine(
        CulpritMonitorService.isRunning,
        DomainMonitorVpnService.isRunning,
        appRiskRepository.observeLastScanTime(),
        redirectEventRepository.observeLeaderboard(),
        appRiskRepository.observeAll()
    ) { isRunning, isDomainMonitoring, lastScan, leaderboard, riskList ->
        MonitorAndRiskData(isRunning, isDomainMonitoring, lastScan, leaderboard, riskList)
    }

    val state: StateFlow<DashboardState> = combine(
        monitorAndRiskData,
        domainHitRepository.observeCorrelations()
    ) { data, correlations ->
        val riskByPackage = data.riskList.associateBy { it.packageName }
        val latestCorrelationByPackage: Map<String, DomainRedirectCorrelation> = correlations
            .groupBy { it.packageName }
            .mapValues { (_, hits) -> hits.maxBy { it.redirectTimestamp } }

        DashboardState(
            isMonitoring = data.isRunning,
            isDomainMonitoring = data.isDomainMonitoring,
            lastScanTime = data.lastScan,
            verdicts = computeVerdicts(data.leaderboard, riskByPackage, latestCorrelationByPackage)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())
}
