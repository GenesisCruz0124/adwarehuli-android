package com.genesiscruz.adwarehuli.ui.appdetail

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesiscruz.adwarehuli.data.repository.AppRiskRepository
import com.genesiscruz.adwarehuli.data.repository.DomainHitRepository
import com.genesiscruz.adwarehuli.data.repository.RedirectEventRepository
import com.genesiscruz.adwarehuli.domain.model.AppRiskInfo
import com.genesiscruz.adwarehuli.domain.model.DomainHit
import com.genesiscruz.adwarehuli.domain.model.DomainRedirectCorrelation
import com.genesiscruz.adwarehuli.domain.model.RedirectEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AppDetailState(
    val label: String = "",
    val packageName: String = "",
    val versionName: String? = null,
    val requestedPermissions: List<String> = emptyList(),
    val riskInfo: AppRiskInfo? = null,
    val redirectHistory: List<RedirectEvent> = emptyList(),
    val domainHits: List<DomainHit> = emptyList(),
    val domainCorrelations: List<DomainRedirectCorrelation> = emptyList()
)

private data class RiskAndRedirectData(
    val riskInfo: AppRiskInfo?,
    val redirectHistory: List<RedirectEvent>
)

class AppDetailViewModel(
    packageName: String,
    packageManager: PackageManager,
    appRiskRepository: AppRiskRepository,
    redirectEventRepository: RedirectEventRepository,
    domainHitRepository: DomainHitRepository
) : ViewModel() {

    private val packageInfo: PackageInfo? = try {
        packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private val riskAndRedirectData = combine(
        appRiskRepository.observeForPackage(packageName),
        redirectEventRepository.observeForPackage(packageName)
    ) { riskInfo, history -> RiskAndRedirectData(riskInfo, history) }

    val state: StateFlow<AppDetailState> = combine(
        riskAndRedirectData,
        domainHitRepository.observeForPackage(packageName),
        domainHitRepository.observeCorrelationsForPackage(packageName)
    ) { data, domainHits, correlations ->
        AppDetailState(
            label = data.riskInfo?.label ?: packageInfo?.applicationInfo?.let {
                packageManager.getApplicationLabel(it).toString()
            } ?: packageName,
            packageName = packageName,
            versionName = packageInfo?.versionName,
            requestedPermissions = packageInfo?.requestedPermissions?.toList() ?: emptyList(),
            riskInfo = data.riskInfo,
            redirectHistory = data.redirectHistory,
            domainHits = domainHits.sortedByDescending { it.isFlagged },
            domainCorrelations = correlations
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppDetailState(packageName = packageName)
    )
}
