package com.genesiscruz.adwarehuli.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRiskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<AppRiskEntity>)

    @Query("DELETE FROM app_risk")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(apps: List<AppRiskEntity>) {
        clearAll()
        upsertAll(apps)
    }

    @Query("SELECT * FROM app_risk ORDER BY riskScore DESC")
    fun observeAll(): Flow<List<AppRiskEntity>>

    @Query("SELECT * FROM app_risk WHERE packageName = :packageName")
    fun observeForPackage(packageName: String): Flow<AppRiskEntity?>

    @Query("SELECT * FROM app_risk WHERE packageName = :packageName")
    suspend fun getForPackage(packageName: String): AppRiskEntity?

    @Query("SELECT MAX(lastScanned) FROM app_risk")
    fun observeLastScanTime(): Flow<Long?>
}
