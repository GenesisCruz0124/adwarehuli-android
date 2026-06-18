package com.genesiscruz.adwarehuli.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "redirect_events")
data class RedirectEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val suspectPackage: String,
    val browserPackage: String,
    val timestamp: Long
)
