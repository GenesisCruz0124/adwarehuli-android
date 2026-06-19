package com.genesiscruz.adwarehuli.data.repository

import com.genesiscruz.adwarehuli.data.db.DomainHitDao
import com.genesiscruz.adwarehuli.data.db.DomainHitEntity
import com.genesiscruz.adwarehuli.domain.Constants
import com.genesiscruz.adwarehuli.domain.model.DomainCategory
import com.genesiscruz.adwarehuli.domain.model.DomainHit
import com.genesiscruz.adwarehuli.domain.model.DomainRedirectCorrelation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DomainHitRepository(private val dao: DomainHitDao) {

    suspend fun record(
        packageName: String,
        uid: Int,
        domain: String,
        category: DomainCategory,
        isFlagged: Boolean,
        protocol: String,
        timestamp: Long
    ) {
        dao.insert(
            DomainHitEntity(
                packageName = packageName,
                uid = uid,
                domain = domain,
                category = category.name,
                isFlagged = isFlagged,
                protocol = protocol,
                timestamp = timestamp
            )
        )
    }

    fun observeRecent(limit: Int = 200): Flow<List<DomainHit>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    fun observeForPackage(packageName: String): Flow<List<DomainHit>> =
        dao.observeForPackage(packageName).map { rows -> rows.map { it.toDomain() } }

    fun observeFlagged(): Flow<List<DomainHit>> =
        dao.observeFlagged().map { rows -> rows.map { it.toDomain() } }

    fun observeFlaggedGroupedByPackage(): Flow<Map<String, Int>> =
        dao.observeFlaggedGroupedByPackage().map { rows ->
            rows.groupBy { it.packageName }.mapValues { (_, hits) -> hits.sumOf { it.hitCount } }
        }

    fun observeCorrelations(windowMs: Long = Constants.CORRELATION_WINDOW_MS): Flow<List<DomainRedirectCorrelation>> =
        dao.observeCorrelations(windowMs).map { rows -> rows.map { it.toDomain() } }

    fun observeCorrelationsForPackage(
        packageName: String,
        windowMs: Long = Constants.CORRELATION_WINDOW_MS
    ): Flow<List<DomainRedirectCorrelation>> =
        dao.observeCorrelationsForPackage(packageName, windowMs).map { rows -> rows.map { it.toDomain() } }

    suspend fun prune(olderThan: Long) = dao.pruneOlderThan(olderThan)

    private fun DomainHitEntity.toDomain() = DomainHit(
        id = id,
        packageName = packageName,
        uid = uid,
        domain = domain,
        category = DomainCategory.valueOf(category),
        isFlagged = isFlagged,
        protocol = protocol,
        timestamp = timestamp
    )

    private fun com.genesiscruz.adwarehuli.data.db.DomainCorrelationRow.toDomain() = DomainRedirectCorrelation(
        packageName = packageName,
        domain = domain,
        category = DomainCategory.valueOf(category),
        hitTimestamp = hitTimestamp,
        redirectTimestamp = redirectTimestamp
    )
}
