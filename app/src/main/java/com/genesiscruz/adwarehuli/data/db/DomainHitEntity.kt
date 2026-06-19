package com.genesiscruz.adwarehuli.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "domain_hits")
data class DomainHitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val uid: Int,
    val domain: String,
    val category: String,
    val isFlagged: Boolean,
    val protocol: String,
    val timestamp: Long
)
