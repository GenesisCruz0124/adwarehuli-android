package com.genesiscruz.adwarehuli.data.repository

import com.genesiscruz.adwarehuli.data.db.AppRiskDao
import com.genesiscruz.adwarehuli.data.db.AppRiskEntity
import com.genesiscruz.adwarehuli.data.pm.InstalledAppsScanner
import com.genesiscruz.adwarehuli.domain.model.AppRiskInfo
import com.genesiscruz.adwarehuli.domain.model.RiskBand
import com.genesiscruz.adwarehuli.domain.model.RiskReason
import com.genesiscruz.adwarehuli.domain.usecase.ComputeRiskScoreUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

class AppRiskRepository(
    private val dao: AppRiskDao,
    private val scanner: InstalledAppsScanner,
    private val computeRiskScore: ComputeRiskScoreUseCase = ComputeRiskScoreUseCase()
) {

    suspend fun runScan(): List<AppRiskInfo> {
        val now = System.currentTimeMillis()
        val results = scanner.scanAll().map { signals -> computeRiskScore(signals, now) }
        dao.replaceAll(results.map { it.toEntity() })
        return results
    }

    fun observeAll(): Flow<List<AppRiskInfo>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observeForPackage(packageName: String): Flow<AppRiskInfo?> =
        dao.observeForPackage(packageName).map { it?.toDomain() }

    suspend fun getForPackage(packageName: String): AppRiskInfo? = dao.getForPackage(packageName)?.toDomain()

    fun observeLastScanTime(): Flow<Long?> = dao.observeLastScanTime()

    private fun AppRiskInfo.toEntity() = AppRiskEntity(
        packageName = packageName,
        label = label,
        isSystemApp = isSystemApp,
        riskScore = riskScore,
        band = band.name,
        reasonsJson = JSONArray(reasons.map { it.name }).toString(),
        installSource = installSource,
        firstInstallTime = firstInstallTime,
        isHidden = isHidden,
        lastScanned = lastScanned
    )

    private fun AppRiskEntity.toDomain(): AppRiskInfo {
        val reasonNames = buildList {
            val json = JSONArray(reasonsJson)
            for (i in 0 until json.length()) add(json.getString(i))
        }
        return AppRiskInfo(
            packageName = packageName,
            label = label,
            isSystemApp = isSystemApp,
            riskScore = riskScore,
            band = RiskBand.valueOf(band),
            reasons = reasonNames.map { RiskReason.valueOf(it) },
            installSource = installSource,
            firstInstallTime = firstInstallTime,
            isHidden = isHidden,
            lastScanned = lastScanned
        )
    }
}
