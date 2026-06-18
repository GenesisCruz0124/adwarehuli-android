package com.genesiscruz.adwarehuli.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_risk")
data class AppRiskEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val riskScore: Int,
    val band: String,
    val reasonsJson: String,
    val installSource: String?,
    val firstInstallTime: Long,
    val isHidden: Boolean,
    val lastScanned: Long
)
