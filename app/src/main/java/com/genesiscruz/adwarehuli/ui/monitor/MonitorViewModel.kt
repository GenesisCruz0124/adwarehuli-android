package com.genesiscruz.adwarehuli.ui.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesiscruz.adwarehuli.data.repository.RedirectEventRepository
import com.genesiscruz.adwarehuli.domain.model.CulpritTally
import com.genesiscruz.adwarehuli.domain.model.RedirectEvent
import com.genesiscruz.adwarehuli.service.CulpritMonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MonitorState(
    val isMonitoring: Boolean = false,
    val eventsThisSession: Int = 0,
    val leaderboard: List<CulpritTally> = emptyList(),
    val history: List<RedirectEvent> = emptyList()
)

class MonitorViewModel(redirectEventRepository: RedirectEventRepository) : ViewModel() {

    val state: StateFlow<MonitorState> = combine(
        CulpritMonitorService.isRunning,
        CulpritMonitorService.eventsThisSession,
        redirectEventRepository.observeLeaderboard(),
        redirectEventRepository.observeAll()
    ) { isMonitoring, eventsThisSession, leaderboard, history ->
        MonitorState(isMonitoring, eventsThisSession, leaderboard, history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonitorState())
}
