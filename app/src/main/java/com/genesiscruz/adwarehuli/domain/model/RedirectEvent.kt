package com.genesiscruz.adwarehuli.domain.model

data class RedirectEvent(
    val id: Long = 0,
    val suspectPackage: String,
    val browserPackage: String,
    val timestamp: Long
)

data class CulpritTally(
    val suspectPackage: String,
    val redirectCount: Int,
    val lastSeen: Long
)
