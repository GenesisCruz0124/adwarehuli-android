package com.genesiscruz.adwarehuli.ui.domainmonitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesiscruz.adwarehuli.data.repository.DomainHitRepository
import com.genesiscruz.adwarehuli.domain.model.DomainHit
import com.genesiscruz.adwarehuli.service.DomainMonitorVpnService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DomainMonitorState(
    val isRunning: Boolean = false,
    val hitsThisSession: Int = 0,
    val lastError: DomainMonitorVpnService.ErrorReason? = null,
    val recentHits: List<DomainHit> = emptyList()
)

class DomainMonitorViewModel(domainHitRepository: DomainHitRepository) : ViewModel() {

    val state: StateFlow<DomainMonitorState> = combine(
        DomainMonitorVpnService.isRunning,
        DomainMonitorVpnService.hitsThisSession,
        DomainMonitorVpnService.lastError,
        domainHitRepository.observeRecent()
    ) { isRunning, hitsThisSession, lastError, recentHits ->
        DomainMonitorState(isRunning, hitsThisSession, lastError, recentHits)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DomainMonitorState())
}
