package com.genesiscruz.adwarehuli.data.repository

import com.genesiscruz.adwarehuli.data.db.RedirectEventDao
import com.genesiscruz.adwarehuli.data.db.RedirectEventEntity
import com.genesiscruz.adwarehuli.domain.model.CulpritTally
import com.genesiscruz.adwarehuli.domain.model.RedirectEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RedirectEventRepository(private val dao: RedirectEventDao) {

    suspend fun recordRedirect(suspectPackage: String, browserPackage: String, timestamp: Long) {
        dao.insert(
            RedirectEventEntity(
                suspectPackage = suspectPackage,
                browserPackage = browserPackage,
                timestamp = timestamp
            )
        )
    }

    fun observeAll(): Flow<List<RedirectEvent>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observeForPackage(packageName: String): Flow<List<RedirectEvent>> =
        dao.observeForPackage(packageName).map { rows -> rows.map { it.toDomain() } }

    fun observeLeaderboard(): Flow<List<CulpritTally>> = dao.observeTallyBySuspect().map { rows ->
        rows.map { CulpritTally(it.suspectPackage, it.redirectCount, it.lastSeen) }
    }

    suspend fun countForPackage(packageName: String): Int = dao.countForPackage(packageName)

    private fun RedirectEventEntity.toDomain() = RedirectEvent(id, suspectPackage, browserPackage, timestamp)
}
