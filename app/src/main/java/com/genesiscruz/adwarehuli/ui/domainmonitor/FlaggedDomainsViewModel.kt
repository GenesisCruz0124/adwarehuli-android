package com.genesiscruz.adwarehuli.ui.domainmonitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesiscruz.adwarehuli.data.repository.DomainHitRepository
import com.genesiscruz.adwarehuli.domain.model.DomainHit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FlaggedAppGroup(val packageName: String, val hits: List<DomainHit>)

data class FlaggedDomainsState(val groups: List<FlaggedAppGroup> = emptyList())

class FlaggedDomainsViewModel(domainHitRepository: DomainHitRepository) : ViewModel() {

    val state: StateFlow<FlaggedDomainsState> = domainHitRepository.observeFlagged()
        .map { hits ->
            val groups = hits.groupBy { it.packageName }
                .map { (packageName, groupHits) -> FlaggedAppGroup(packageName, groupHits) }
                .sortedByDescending { it.hits.size }
            FlaggedDomainsState(groups)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FlaggedDomainsState())
}
