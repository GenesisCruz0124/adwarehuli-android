package com.genesiscruz.adwarehuli.ui.appdetail

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesiscruz.adwarehuli.data.repository.AppRiskRepository
import com.genesiscruz.adwarehuli.data.repository.RedirectEventRepository
import com.genesiscruz.adwarehuli.domain.model.AppRiskInfo
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
    val redirectHistory: List<RedirectEvent> = emptyList()
)

class AppDetailViewModel(
    packageName: String,
    packageManager: PackageManager,
    appRiskRepository: AppRiskRepository,
    redirectEventRepository: RedirectEventRepository
) : ViewModel() {

    private val packageInfo: PackageInfo? = try {
        packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    val state: StateFlow<AppDetailState> = combine(
        appRiskRepository.observeForPackage(packageName),
        redirectEventRepository.observeForPackage(packageName)
    ) { riskInfo, history ->
        AppDetailState(
            label = riskInfo?.label ?: packageInfo?.applicationInfo?.let {
                packageManager.getApplicationLabel(it).toString()
            } ?: packageName,
            packageName = packageName,
            versionName = packageInfo?.versionName,
            requestedPermissions = packageInfo?.requestedPermissions?.toList() ?: emptyList(),
            riskInfo = riskInfo,
            redirectHistory = history
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppDetailState(packageName = packageName)
    )
}
