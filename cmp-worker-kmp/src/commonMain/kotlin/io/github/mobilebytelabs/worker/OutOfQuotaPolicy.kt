package io.github.mobilebytelabs.worker

/**
 * Behavior when Android 12+ expedited-work quota is exhausted.
 *
 * Added in v3.0.0-alpha04 (Phase 7).
 *
 * Mirrors `androidx.work.OutOfQuotaPolicy`. No-op on iOS/Desktop/Web (those
 * platforms have no expedited quota concept; the request runs as ordinary
 * background work).
 */
public enum class OutOfQuotaPolicy {
    RUN_AS_NON_EXPEDITED_WORK_REQUEST,
    DROP_WORK_REQUEST,
}
