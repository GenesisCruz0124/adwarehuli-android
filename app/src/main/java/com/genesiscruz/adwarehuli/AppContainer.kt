package com.genesiscruz.adwarehuli

import android.content.Context
import com.genesiscruz.adwarehuli.data.db.AppDatabase
import com.genesiscruz.adwarehuli.data.pm.BrowserDetector
import com.genesiscruz.adwarehuli.data.pm.InstalledAppsScanner
import com.genesiscruz.adwarehuli.data.pm.LauncherDetector
import com.genesiscruz.adwarehuli.data.pm.PackageInfoProvider
import com.genesiscruz.adwarehuli.data.pm.PermissionChecker
import com.genesiscruz.adwarehuli.data.repository.AppRiskRepository
import com.genesiscruz.adwarehuli.data.repository.RedirectEventRepository
import com.genesiscruz.adwarehuli.domain.usecase.ComputeVerdictsUseCase

/**
 * Hand-rolled dependency container. The app is small enough that a DI
 * framework would add more ceremony than it saves.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val database by lazy { AppDatabase.getInstance(appContext) }

    val packageInfoProvider by lazy { PackageInfoProvider(appContext) }
    val permissionChecker by lazy { PermissionChecker(appContext) }
    val browserDetector by lazy { BrowserDetector(appContext) }
    val launcherDetector by lazy { LauncherDetector(appContext) }
    private val installedAppsScanner by lazy { InstalledAppsScanner(appContext) }

    val redirectEventRepository by lazy { RedirectEventRepository(database.redirectEventDao()) }
    val appRiskRepository by lazy { AppRiskRepository(database.appRiskDao(), installedAppsScanner) }

    val computeVerdictsUseCase by lazy { ComputeVerdictsUseCase() }
}
