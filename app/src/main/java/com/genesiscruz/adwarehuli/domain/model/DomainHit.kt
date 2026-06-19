package com.genesiscruz.adwarehuli.domain.model

data class DomainHit(
    val id: Long = 0,
    val packageName: String,
    val uid: Int,
    val domain: String,
    val category: DomainCategory,
    val isFlagged: Boolean,
    val protocol: String,
    val timestamp: Long
)

data class DomainHitTally(
    val packageName: String,
    val domain: String,
    val category: DomainCategory,
    val hitCount: Int,
    val lastSeen: Long
)

/**
 * A [DomainHit] that occurred within [Constants.CORRELATION_WINDOW_MS] of a
 * Phase 1 redirect event for the same package — i.e. the lookup that most
 * likely caused that redirect.
 */
data class DomainRedirectCorrelation(
    val packageName: String,
    val domain: String,
    val category: DomainCategory,
    val hitTimestamp: Long,
    val redirectTimestamp: Long
) {
    val gapMs: Long get() = redirectTimestamp - hitTimestamp
}
