package com.genesiscruz.adwarehuli.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class DomainTallyRow(
    val packageName: String,
    val domain: String,
    val category: String,
    val hitCount: Int,
    val lastSeen: Long
)

data class DomainCorrelationRow(
    val packageName: String,
    val domain: String,
    val category: String,
    val hitTimestamp: Long,
    val redirectTimestamp: Long
)

@Dao
interface DomainHitDao {

    @Insert
    suspend fun insert(hit: DomainHitEntity): Long

    @Query("SELECT * FROM domain_hits ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<DomainHitEntity>>

    @Query("SELECT * FROM domain_hits WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun observeForPackage(packageName: String): Flow<List<DomainHitEntity>>

    @Query("SELECT * FROM domain_hits WHERE isFlagged = 1 ORDER BY timestamp DESC")
    fun observeFlagged(): Flow<List<DomainHitEntity>>

    @Query(
        """
        SELECT packageName, domain, category, COUNT(*) AS hitCount, MAX(timestamp) AS lastSeen
        FROM domain_hits
        WHERE isFlagged = 1
        GROUP BY packageName, domain
        ORDER BY hitCount DESC
        """
    )
    fun observeFlaggedGroupedByPackage(): Flow<List<DomainTallyRow>>

    /**
     * Joins flagged domain hits against Phase 1 redirect events: a hit counts
     * as the likely cause of a redirect when it's for the same package and
     * happened no more than [windowMs] before the redirect timestamp.
     */
    @Query(
        """
        SELECT dh.packageName AS packageName, dh.domain AS domain, dh.category AS category,
               dh.timestamp AS hitTimestamp, re.timestamp AS redirectTimestamp
        FROM domain_hits dh
        INNER JOIN redirect_events re
            ON dh.packageName = re.suspectPackage
            AND dh.timestamp <= re.timestamp
            AND (re.timestamp - dh.timestamp) <= :windowMs
        WHERE dh.isFlagged = 1
        ORDER BY re.timestamp DESC
        """
    )
    fun observeCorrelations(windowMs: Long): Flow<List<DomainCorrelationRow>>

    @Query(
        """
        SELECT dh.packageName AS packageName, dh.domain AS domain, dh.category AS category,
               dh.timestamp AS hitTimestamp, re.timestamp AS redirectTimestamp
        FROM domain_hits dh
        INNER JOIN redirect_events re
            ON dh.packageName = re.suspectPackage
            AND dh.timestamp <= re.timestamp
            AND (re.timestamp - dh.timestamp) <= :windowMs
        WHERE dh.isFlagged = 1 AND dh.packageName = :packageName
        ORDER BY re.timestamp DESC
        """
    )
    fun observeCorrelationsForPackage(packageName: String, windowMs: Long): Flow<List<DomainCorrelationRow>>

    @Query("DELETE FROM domain_hits WHERE timestamp < :olderThan")
    suspend fun pruneOlderThan(olderThan: Long)
}
