package io.github.mobilebytelabs.worker.storeflow.policy

/**
 * Controls whether a Store-backed flow reads from cache, hits the network, or both.
 *
 * Lifted from `kmp-project-template/core-base/store/screen/FetchPolicy.kt` with attribution.
 * Worker-anchored adaptation: the FetchPolicy enum is value-identical; the consumer-side
 * decisioning logic that uses it has been re-implemented in WorkScheduledOfflineSubmitSyncer
 * and related worker-anchored types.
 *
 * Added in v3.0.0-alpha03 (Phase 3 of worker-kmp v3.0.0 epic).
 */
public enum class FetchPolicy {
    /** Emit cached data immediately (if present), then trigger a background network fetch. Default. */
    CACHE_THEN_NETWORK,
    /** Skip cache entirely; always fetch from network. */
    NETWORK_ONLY,
    /** Read only from cache; never perform a network request. */
    CACHE_ONLY,
}
