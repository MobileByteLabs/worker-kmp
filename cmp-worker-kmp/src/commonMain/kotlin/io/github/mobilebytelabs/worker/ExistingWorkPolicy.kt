package io.github.mobilebytelabs.worker

/**
 * Conflict resolution policy for [WorkManager.enqueueUniqueWork] when a one-time work
 * request with the same uniqueness name already exists. Mirrors AndroidX
 * `androidx.work.ExistingWorkPolicy`.
 *
 * Added by cross-platform-worker-parity-audit sub-plan 03 (closes G2 — Android's
 * `enqueueUniqueWork` with conflict policies wasn't surfaced through worker-kmp's
 * commonMain). The sibling [ExistingPeriodicWorkPolicy] handles periodic work; this
 * enum handles one-time work.
 */
public enum class ExistingWorkPolicy {
    /**
     * Cancel any existing work with this name and enqueue the new request. Equivalent
     * to AndroidX `ExistingWorkPolicy.REPLACE`.
     */
    REPLACE,

    /**
     * Keep the existing work; the new request is dropped silently. Equivalent to
     * AndroidX `ExistingWorkPolicy.KEEP`.
     */
    KEEP,

    /**
     * Append the new request to the existing chain. New work runs after the existing
     * unique-named work completes. Equivalent to AndroidX `ExistingWorkPolicy.APPEND`.
     *
     * Behaviour when the existing work has FAILED or been CANCELLED is platform-specific:
     * AndroidX leaves the new appended work BLOCKED; iOS/Desktop/Web treat the existing
     * work as terminal and run the new work immediately. See per-actual docs.
     */
    APPEND,

    /**
     * Like [APPEND], but if the existing work is in a FAILED or CANCELLED terminal
     * state, behave as [REPLACE] (drop the terminal work and enqueue fresh). Equivalent
     * to AndroidX `ExistingWorkPolicy.APPEND_OR_REPLACE`.
     */
    APPEND_OR_REPLACE,
}
