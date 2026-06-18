package com.genesiscruz.adwarehuli.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import com.genesiscruz.adwarehuli.data.pm.PermissionChecker
import com.genesiscruz.adwarehuli.service.BootCompletedReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OnboardingState(
    val hasUsageAccess: Boolean = false,
    val hasQueryAllPackages: Boolean = false,
    val hasNotifications: Boolean = false,
    val resumeOnBoot: Boolean = false
) {
    val canProceed: Boolean get() = hasUsageAccess && hasQueryAllPackages
}

class OnboardingViewModel(
    private val permissionChecker: PermissionChecker,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun refresh() {
        _state.value = OnboardingState(
            hasUsageAccess = permissionChecker.hasUsageAccess(),
            hasQueryAllPackages = permissionChecker.hasQueryAllPackages(),
            hasNotifications = permissionChecker.hasPostNotifications(),
            resumeOnBoot = BootCompletedReceiver.isResumeOnBootEnabled(appContext)
        )
    }

    fun setResumeOnBoot(enabled: Boolean) {
        BootCompletedReceiver.setResumeOnBoot(appContext, enabled)
        refresh()
    }
}
