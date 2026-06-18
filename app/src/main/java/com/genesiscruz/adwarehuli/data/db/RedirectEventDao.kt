package com.genesiscruz.adwarehuli.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class SuspectTallyRow(
    val suspectPackage: String,
    val redirectCount: Int,
    val lastSeen: Long
)

@Dao
interface RedirectEventDao {

    @Insert
    suspend fun insert(event: RedirectEventEntity): Long

    @Query("SELECT * FROM redirect_events ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<RedirectEventEntity>>

    @Query("SELECT * FROM redirect_events WHERE suspectPackage = :packageName ORDER BY timestamp DESC")
    fun observeForPackage(packageName: String): Flow<List<RedirectEventEntity>>

    @Query(
        """
        SELECT suspectPackage, COUNT(*) AS redirectCount, MAX(timestamp) AS lastSeen
        FROM redirect_events
        GROUP BY suspectPackage
        ORDER BY redirectCount DESC
        """
    )
    fun observeTallyBySuspect(): Flow<List<SuspectTallyRow>>

    @Query("SELECT COUNT(*) FROM redirect_events WHERE suspectPackage = :packageName")
    suspend fun countForPackage(packageName: String): Int
}
